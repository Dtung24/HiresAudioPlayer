package com.hiresaudioplayer.app.audio

object NativeAudioEngine {

    init {
        try {
            System.loadLibrary("aether_dsp")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    // Native JNI functions
    private external fun nativeInit(inSampleRate: Int, outSampleRate: Int)
    private external fun nativeSetInputSampleRate(inRate: Int)
    private external fun nativeSetUpsamplingTarget(targetRate: Int, filterMode: Int)
    private external fun nativeSetGraphicEqBand(bandIndex: Int, gainDb: Float)
    private external fun nativeSetGraphicEqEnabled(enabled: Boolean)
    private external fun nativeSetParametricEqBand(bandIndex: Int, type: Int, freq: Float, gainDb: Float, q: Float, enabled: Boolean)
    private external fun nativeSetParametricEqPreamp(preampDb: Float)
    private external fun nativeSetAacEnhancement(enabled: Boolean, intensity: Float)
    private external fun nativeSetFlacCrossfeed(enabled: Boolean, strength: Float)
    private external fun nativeSetFlacRollOffFilter(enabled: Boolean, cutoffHz: Float)
    private external fun nativeProcess(inSamples: FloatArray, inFrames: Int, outSamples: FloatArray, maxOutFrames: Int): Int
    private external fun nativeGetFftMagnitudes(outBins: FloatArray, numBins: Int)
    private external fun nativeReset()
    private external fun nativeRelease()

    fun init(inSampleRate: Int, outSampleRate: Int) {
        try {
            nativeInit(inSampleRate, outSampleRate)
        } catch (_: Throwable) {}
    }

    fun setInputSampleRate(inRate: Int) {
        try {
            nativeSetInputSampleRate(inRate)
        } catch (_: Throwable) {}
    }

    fun setUpsamplingTarget(targetRate: Int, filterMode: Int) {
        try {
            nativeSetUpsamplingTarget(targetRate, filterMode)
        } catch (_: Throwable) {}
    }

    fun setGraphicEqBand(bandIndex: Int, gainDb: Float) {
        try {
            nativeSetGraphicEqBand(bandIndex, gainDb)
        } catch (_: Throwable) {}
    }

    fun setGraphicEqEnabled(enabled: Boolean) {
        try {
            nativeSetGraphicEqEnabled(enabled)
        } catch (_: Throwable) {}
    }

    fun setParametricEqBand(bandIndex: Int, type: Int, freq: Float, gainDb: Float, q: Float, enabled: Boolean) {
        try {
            nativeSetParametricEqBand(bandIndex, type, freq, gainDb, q, enabled)
        } catch (_: Throwable) {}
    }

    fun setParametricEqPreamp(preampDb: Float) {
        try {
            nativeSetParametricEqPreamp(preampDb)
        } catch (_: Throwable) {}
    }

    fun setAacEnhancement(enabled: Boolean, intensity: Float) {
        try {
            nativeSetAacEnhancement(enabled, intensity)
        } catch (_: Throwable) {}
    }

    fun setFlacCrossfeed(enabled: Boolean, strength: Float) {
        try {
            nativeSetFlacCrossfeed(enabled, strength)
        } catch (_: Throwable) {}
    }

    fun setFlacRollOffFilter(enabled: Boolean, cutoffHz: Float) {
        try {
            nativeSetFlacRollOffFilter(enabled, cutoffHz)
        } catch (_: Throwable) {}
    }

    fun processAudio(inSamples: FloatArray, inFrames: Int, outSamples: FloatArray, maxOutFrames: Int): Int {
        return try {
            nativeProcess(inSamples, inFrames, outSamples, maxOutFrames)
        } catch (_: Throwable) {
            // Fallback passthrough
            val toCopy = inSamples.size.coerceAtMost(outSamples.size)
            System.arraycopy(inSamples, 0, outSamples, 0, toCopy)
            toCopy
        }
    }

    fun getFftMagnitudes(outBins: FloatArray) {
        try {
            nativeGetFftMagnitudes(outBins, outBins.size)
        } catch (_: Throwable) {}
    }

    fun reset() {
        try {
            nativeReset()
        } catch (_: Throwable) {}
    }

    fun release() {
        try {
            nativeRelease()
        } catch (_: Throwable) {}
    }
}

