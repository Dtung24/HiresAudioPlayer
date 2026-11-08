package com.hiresaudioplayer.app.dsp

data class FlacProfile(
    val bitPerfectMode: Boolean = false,
    val crossfeedEnabled: Boolean = true,
    val crossfeedStrength: Float = 0.5f, // 0.0 .. 1.0
    val rollOffFilterEnabled: Boolean = false,
    val rollOffCutoffHz: Float = 22000.0f
)

data class AacProfile(
    val dseeHighFrequencyRecovery: Boolean = true,
    val harmonicIntensity: Float = 0.45f, // 0.0 .. 1.0
    val antiDistortionLimiter: Boolean = true
)

data class PeqBand(
    val id: Int,
    val type: Int = 0, // 0: PEAK, 1: LOW_SHELF, 2: HIGH_SHELF, 3: LOW_PASS, 4: HIGH_PASS, 5: BAND_PASS, 6: NOTCH
    val frequency: Float,
    val gainDb: Float = 0.0f,
    val q: Float = 1.0f,
    val enabled: Boolean = true
)

