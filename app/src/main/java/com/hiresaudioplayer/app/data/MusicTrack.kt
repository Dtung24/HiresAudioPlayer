package com.hiresaudioplayer.app.data

import android.net.Uri
import java.io.Serializable

enum class AudioSource {
    LOCAL,
    GOOGLE_DRIVE,
    CLOUD_STREAM
}

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val uriString: String,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val bitRateKbps: Int = 0,
    val codec: String = "FLAC", // FLAC, AAC, WAV, MP3, ALAC
    val fileSize: Long = 0L,
    val source: AudioSource = AudioSource.LOCAL,
    val dateAdded: Long = System.currentTimeMillis()
) : Serializable {

    val uri: Uri
        get() = Uri.parse(uriString)

    val isHiRes: Boolean
        get() = sampleRate >= 88200 || bitDepth >= 24

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val formattedSampleRate: String
        get() = String.format("%.1f kHz", sampleRate / 1000.0)

    val formattedCodecBadge: String
        get() = "$codec ${formattedSampleRate} / ${bitDepth}b"
}

