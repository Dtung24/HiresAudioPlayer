package com.hiresaudioplayer.app.dsp

import com.hiresaudioplayer.app.audio.NativeAudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DspManager {

    companion object {
        val SAMPLING_RATES = listOf(44100, 48000, 88200, 96000, 176400, 192000)
        val EQ_FREQUENCIES = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    }

    // Upsampling upper limit
    private val _targetUpsampleRate = MutableStateFlow(192000)
    val targetUpsampleRate: StateFlow<Int> = _targetUpsampleRate.asStateFlow()

    // 0 = Linear Phase, 1 = Minimum Phase
    private val _resamplerFilterMode = MutableStateFlow(0)
    val resamplerFilterMode: StateFlow<Int> = _resamplerFilterMode.asStateFlow()

    // Graphic EQ
    private val _graphicEqEnabled = MutableStateFlow(true)
    val graphicEqEnabled: StateFlow<Boolean> = _graphicEqEnabled.asStateFlow()

    private val _graphicEqGains = MutableStateFlow(FloatArray(10) { 0.0f })
    val graphicEqGains: StateFlow<FloatArray> = _graphicEqGains.asStateFlow()

    private val _currentPresetName = MutableStateFlow("Flat")
    val currentPresetName: StateFlow<String> = _currentPresetName.asStateFlow()

    // Parametric EQ
    private val _peqEnabled = MutableStateFlow(false)
    val peqEnabled: StateFlow<Boolean> = _peqEnabled.asStateFlow()

    private val _peqPreampDb = MutableStateFlow(0.0f)
    val peqPreampDb: StateFlow<Float> = _peqPreampDb.asStateFlow()

    private val _peqBands = MutableStateFlow(
        listOf(
            PeqBand(0, 1, 60.0f, 0.0f, 0.71f),
            PeqBand(1, 0, 150.0f, 0.0f, 1.41f),
            PeqBand(2, 0, 450.0f, 0.0f, 1.41f),
            PeqBand(3, 0, 1000.0f, 0.0f, 1.41f),
            PeqBand(4, 0, 2500.0f, 0.0f, 1.41f),
            PeqBand(5, 0, 6000.0f, 0.0f, 1.41f),
            PeqBand(6, 2, 12000.0f, 0.0f, 0.71f)
        )
    )
    val peqBands: StateFlow<List<PeqBand>> = _peqBands.asStateFlow()

    // Codec Profiles
    private val _flacProfile = MutableStateFlow(FlacProfile())
    val flacProfile: StateFlow<FlacProfile> = _flacProfile.asStateFlow()

    private val _aacProfile = MutableStateFlow(AacProfile())
    val aacProfile: StateFlow<AacProfile> = _aacProfile.asStateFlow()

    init {
        // Initialize default native engine state
        syncAllToNative()
    }

    fun syncAllToNative() {
        NativeAudioEngine.setUpsamplingTarget(_targetUpsampleRate.value, _resamplerFilterMode.value)
        NativeAudioEngine.setGraphicEqEnabled(_graphicEqEnabled.value)

        val gains = _graphicEqGains.value
        for (i in gains.indices) {
            NativeAudioEngine.setGraphicEqBand(i, gains[i])
        }

        NativeAudioEngine.setParametricEqPreamp(_peqPreampDb.value)
        for (b in _peqBands.value) {
            NativeAudioEngine.setParametricEqBand(
                b.id, b.type, b.frequency, b.gainDb, b.q, b.enabled && _peqEnabled.value
            )
        }

        val flac = _flacProfile.value
        NativeAudioEngine.setFlacCrossfeed(flac.crossfeedEnabled, flac.crossfeedStrength)
        NativeAudioEngine.setFlacRollOffFilter(flac.rollOffFilterEnabled, flac.rollOffCutoffHz)

        val aac = _aacProfile.value
        NativeAudioEngine.setAacEnhancement(aac.dseeHighFrequencyRecovery, aac.harmonicIntensity)
    }

    fun setTargetUpsampleRate(rate: Int) {
        _targetUpsampleRate.value = rate
        NativeAudioEngine.setUpsamplingTarget(rate, _resamplerFilterMode.value)
    }

    fun setResamplerFilterMode(mode: Int) {
        _resamplerFilterMode.value = mode
        NativeAudioEngine.setUpsamplingTarget(_targetUpsampleRate.value, mode)
    }

    fun setGraphicEqEnabled(enabled: Boolean) {
        _graphicEqEnabled.value = enabled
        NativeAudioEngine.setGraphicEqEnabled(enabled)
    }

    fun setGraphicEqBandGain(bandIndex: Int, gainDb: Float) {
        val current = _graphicEqGains.value.copyOf()
        if (bandIndex in current.indices) {
            current[bandIndex] = gainDb
            _graphicEqGains.value = current
            NativeAudioEngine.setGraphicEqBand(bandIndex, gainDb)
            _currentPresetName.value = "Custom"
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        _currentPresetName.value = preset.name
        val newGains = preset.gains.copyOf()
        _graphicEqGains.value = newGains
        for (i in newGains.indices) {
            NativeAudioEngine.setGraphicEqBand(i, newGains[i])
        }
        setPeqPreamp(preset.preampDb)
    }

    fun setPeqEnabled(enabled: Boolean) {
        _peqEnabled.value = enabled
        for (b in _peqBands.value) {
            NativeAudioEngine.setParametricEqBand(
                b.id, b.type, b.frequency, b.gainDb, b.q, b.enabled && enabled
            )
        }
    }

    fun setPeqPreamp(preampDb: Float) {
        _peqPreampDb.value = preampDb
        NativeAudioEngine.setParametricEqPreamp(preampDb)
    }

    fun updatePeqBand(bandIndex: Int, freq: Float, gainDb: Float, q: Float, type: Int, enabled: Boolean) {
        val list = _peqBands.value.toMutableList()
        if (bandIndex in list.indices) {
            val updated = PeqBand(bandIndex, type, freq, gainDb, q, enabled)
            list[bandIndex] = updated
            _peqBands.value = list
            NativeAudioEngine.setParametricEqBand(bandIndex, type, freq, gainDb, q, enabled && _peqEnabled.value)
        }
    }

    // FLAC Controls
    fun setFlacBitPerfect(enabled: Boolean) {
        _flacProfile.value = _flacProfile.value.copy(bitPerfectMode = enabled)
        if (enabled) {
            // In bit-perfect mode, bypass upsampling and crossfeed
            NativeAudioEngine.setUpsamplingTarget(44100, 0)
            NativeAudioEngine.setFlacCrossfeed(false, 0f)
            NativeAudioEngine.setGraphicEqEnabled(false)
        } else {
            syncAllToNative()
        }
    }

    fun setFlacCrossfeed(enabled: Boolean, strength: Float) {
        _flacProfile.value = _flacProfile.value.copy(crossfeedEnabled = enabled, crossfeedStrength = strength)
        NativeAudioEngine.setFlacCrossfeed(enabled, strength)
    }

    fun setFlacRollOff(enabled: Boolean, cutoffHz: Float = 22000.0f) {
        _flacProfile.value = _flacProfile.value.copy(rollOffFilterEnabled = enabled, rollOffCutoffHz = cutoffHz)
        NativeAudioEngine.setFlacRollOffFilter(enabled, cutoffHz)
    }

    // AAC Controls
    fun setAacDseeRecovery(enabled: Boolean, intensity: Float) {
        _aacProfile.value = _aacProfile.value.copy(dseeHighFrequencyRecovery = enabled, harmonicIntensity = intensity)
        NativeAudioEngine.setAacEnhancement(enabled, intensity)
    }

    /**
     * Import Parametric EQ profile from standard AutoEQ text format
     */
    fun importAutoEqText(text: String): Boolean {
        try {
            val lines = text.lines()
            val parsedBands = mutableListOf<PeqBand>()
            var idCounter = 0

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.startsWith("Preamp:", ignoreCase = true)) {
                    val match = Regex("Preamp:\\s*([+-]?\\d*\\.?\\d+)", RegexOption.IGNORE_CASE).find(line)
                    match?.groupValues?.get(1)?.toFloatOrNull()?.let {
                        setPeqPreamp(it)
                    }
                } else if (line.startsWith("Filter", ignoreCase = true)) {
                    val isEnabled = !line.contains("OFF", ignoreCase = true)
                    val type = when {
                        line.contains("LSC", ignoreCase = true) -> 1 // Low Shelf
                        line.contains("HSC", ignoreCase = true) -> 2 // High Shelf
                        line.contains("LP", ignoreCase = true) -> 3 // Low Pass
                        line.contains("HP", ignoreCase = true) -> 4 // High Pass
                        line.contains("BP", ignoreCase = true) -> 5 // Band Pass
                        line.contains("NO", ignoreCase = true) -> 6 // Notch
                        else -> 0 // Peaking
                    }

                    val fcMatch = Regex("Fc\\s+([0-9.]+)", RegexOption.IGNORE_CASE).find(line)
                    val gainMatch = Regex("Gain\\s+([+-]?[0-9.]+)", RegexOption.IGNORE_CASE).find(line)
                    val qMatch = Regex("Q\\s+([0-9.]+)", RegexOption.IGNORE_CASE).find(line)

                    val freq = fcMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1000f
                    val gain = gainMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                    val q = qMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 1.41f

                    parsedBands.add(PeqBand(idCounter++, type, freq, gain, q, isEnabled))
                }
            }

            if (parsedBands.isNotEmpty()) {
                _peqBands.value = parsedBands
                setPeqEnabled(true)
                syncAllToNative()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}

