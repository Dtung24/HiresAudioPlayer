package com.hiresaudioplayer.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.hiresaudioplayer.app.data.MusicTrack
import com.hiresaudioplayer.app.dsp.DspManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

class HiResAudioPlayer(
    private val context: Context,
    private val dspManager: DspManager
) {

    private val playerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var playbackJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _activeOutputSampleRate = MutableStateFlow(192000)
    val activeOutputSampleRate: StateFlow<Int> = _activeOutputSampleRate.asStateFlow()

    private val _visualizerBins = MutableStateFlow(FloatArray(64) { 0.0f })
    val visualizerBins: StateFlow<FloatArray> = _visualizerBins.asStateFlow()

    @Volatile
    private var isPlaying = false
    @Volatile
    private var isPaused = false
    @Volatile
    private var seekRequestedMs = -1L

    private var audioTrack: AudioTrack? = null

    init {
        // Visualizer poll loop
        playerScope.launch {
            val bins = FloatArray(64)
            while (isActive) {
                if (isPlaying && !isPaused) {
                    NativeAudioEngine.getFftMagnitudes(bins)
                    _visualizerBins.value = bins.clone()
                } else if (!isPlaying) {
                    _visualizerBins.value = FloatArray(64) { 0.0f }
                }
                delay(33) // ~30 fps visualizer refresh
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        stop()
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L

        // Codec specific DSP auto-activation
        if (track.codec.equals("AAC", ignoreCase = true) || track.codec.equals("MP3", ignoreCase = true)) {
            NativeAudioEngine.setAacEnhancement(
                dspManager.aacProfile.value.dseeHighFrequencyRecovery,
                dspManager.aacProfile.value.harmonicIntensity
            )
            NativeAudioEngine.setFlacCrossfeed(false, 0f)
        } else {
            // FLAC / Lossless mode
            NativeAudioEngine.setAacEnhancement(false, 0f)
            NativeAudioEngine.setFlacCrossfeed(
                dspManager.flacProfile.value.crossfeedEnabled,
                dspManager.flacProfile.value.crossfeedStrength
            )
        }

        playbackJob = playerScope.launch(Dispatchers.IO) {
            runAudioPipeline(track)
        }
    }

    fun pause() {
        if (isPlaying && !isPaused) {
            isPaused = true
            audioTrack?.pause()
            _playbackState.value = PlaybackState.PAUSED
        }
    }

    fun resume() {
        if (isPlaying && isPaused) {
            isPaused = false
            audioTrack?.play()
            _playbackState.value = PlaybackState.PLAYING
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            pause()
        } else if (_playbackState.value == PlaybackState.PAUSED) {
            resume()
        } else if (_currentTrack.value != null) {
            playTrack(_currentTrack.value!!)
        }
    }

    fun seekTo(positionMs: Long) {
        seekRequestedMs = positionMs
        _currentPositionMs.value = positionMs
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        NativeAudioEngine.reset()
        _playbackState.value = PlaybackState.IDLE
    }

    private suspend fun runAudioPipeline(track: MusicTrack) {
        _playbackState.value = PlaybackState.BUFFERING
        isPlaying = true
        isPaused = false

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, track.uri, null)
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || inputFormat == null) {
                _playbackState.value = PlaybackState.ERROR
                return
            }

            extractor.selectTrack(audioTrackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/flac"
            val inputSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Determine target upsample output rate (upper limit up to 192000)
            val targetRate = dspManager.targetUpsampleRate.value
            _activeOutputSampleRate.value = targetRate

            NativeAudioEngine.init(inputSampleRate, targetRate)
            NativeAudioEngine.setInputSampleRate(inputSampleRate)
            dspManager.syncAllToNative()

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            // Setup 32-bit Float PCM AudioTrack at upsampled rate
            val minBufSize = AudioTrack.getMinBufferSize(
                targetRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            val bufferSize = (minBufSize * 2).coerceAtLeast(16384)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(targetRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack?.play()
            _playbackState.value = PlaybackState.PLAYING

            val info = MediaCodec.BufferInfo()
            var isEos = false

            val floatInputBuffer = FloatArray(8192)
            val floatOutputBuffer = FloatArray(32768)

            while (isPlaying && (!isEos || info.size > 0)) {
                if (isPaused) {
                    delay(50)
                    continue
                }

                // Handle Seek
                if (seekRequestedMs >= 0) {
                    val targetUs = seekRequestedMs * 1000L
                    extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    codec.flush()
                    NativeAudioEngine.reset()
                    seekRequestedMs = -1L
                }

                // Feed input to decoder
                if (!isEos) {
                    val inIndex = codec.dequeueInputBuffer(5000)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)
                        if (inBuffer != null) {
                            val sampleSize = extractor.readSampleData(inBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEos = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // Retrieve decoded PCM
                val outIndex = codec.dequeueOutputBuffer(info, 5000)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (outBuffer != null && info.size > 0) {
                        outBuffer.position(info.offset)
                        outBuffer.limit(info.offset + info.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        // Convert 16-bit or 24-bit PCM into Float PCM
                        val numFrames = (info.size / 2) / channelCount // assuming 16-bit PCM default from decoder
                        val inStereoFrames = numFrames.coerceAtMost(floatInputBuffer.size / 2)

                        for (f in 0 until inStereoFrames) {
                            if (outBuffer.remaining() >= 2 * channelCount) {
                                val sL = outBuffer.short / 32768.0f
                                val sR = if (channelCount > 1) outBuffer.short / 32768.0f else sL
                                floatInputBuffer[f * 2] = sL
                                floatInputBuffer[f * 2 + 1] = sR
                            }
                        }

                        // Process through C++ Native DSP: EQ + Codec Enhancer + 192 kHz Sinc Resampler
                        val maxOutFrames = (floatOutputBuffer.size / 2)
                        val outSamplesCount = NativeAudioEngine.processAudio(
                            floatInputBuffer,
                            inStereoFrames,
                            floatOutputBuffer,
                            maxOutFrames
                        )

                        // Write to 192 kHz Float AudioTrack
                        if (outSamplesCount > 0) {
                            audioTrack?.write(
                                floatOutputBuffer,
                                0,
                                outSamplesCount,
                                AudioTrack.WRITE_BLOCKING
                            )
                        }

                        _currentPositionMs.value = info.presentationTimeUs / 1000L
                    }

                    codec.releaseOutputBuffer(outIndex, false)

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }

            if (isPlaying) {
                _playbackState.value = PlaybackState.COMPLETED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.ERROR
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}

