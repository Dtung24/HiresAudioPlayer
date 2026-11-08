package com.hiresaudioplayer.app.dsp

data class EqualizerPreset(
    val name: String,
    val gains: FloatArray,
    val preampDb: Float = 0.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EqualizerPreset
        return name == other.name && gains.contentEquals(other.gains) && preampDb == other.preampDb
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + gains.contentHashCode()
        result = 31 * result + preampDb.hashCode()
        return result
    }

    companion object {
        val PRESETS = listOf(
            EqualizerPreset("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), 0f),
            EqualizerPreset("Audiophile Pure", floatArrayOf(0.5f, 0.5f, 0f, 0f, 0f, 0f, 0.5f, 1.0f, 1.5f, 2.0f), -1.0f),
            EqualizerPreset("Bass Boost", floatArrayOf(6.0f, 5.0f, 3.5f, 2.0f, 0.5f, 0f, 0f, 0f, 0f, 0f), -2.5f),
            EqualizerPreset("Treble Air", floatArrayOf(-1.0f, 0f, 0f, 0f, 0f, 1.0f, 2.0f, 4.0f, 5.5f, 6.0f), -2.0f),
            EqualizerPreset("Vocal Presence", floatArrayOf(-1.5f, -1.0f, 0f, 1.5f, 3.5f, 4.0f, 3.0f, 1.5f, 0.5f, 0f), -1.5f),
            EqualizerPreset("Acoustic / Jazz", floatArrayOf(2.5f, 1.5f, 1.0f, 0.5f, 0f, 0.5f, 1.5f, 2.5f, 3.0f, 3.0f), -1.0f),
            EqualizerPreset("Rock / Hi-Fi", floatArrayOf(4.5f, 3.0f, 1.5f, -0.5f, -1.0f, 0.5f, 2.0f, 3.5f, 4.5f, 4.0f), -2.0f),
            EqualizerPreset("Electronic / EDM", floatArrayOf(5.5f, 5.0f, 2.0f, 0f, -1.0f, 1.0f, 2.5f, 4.0f, 4.5f, 5.0f), -2.5f)
        )
    }
}

