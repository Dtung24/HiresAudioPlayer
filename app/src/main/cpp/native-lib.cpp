#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include "dsp/AudioPipeline.h"

#define TAG "NativeAudioEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::unique_ptr<AudioPipeline> g_pipeline;

extern "C" {

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeInit(
        JNIEnv* env, jobject thiz, jint inSampleRate, jint outSampleRate) {
    if (!g_pipeline) {
        g_pipeline = std::make_unique<AudioPipeline>();
    }
    g_pipeline->init(inSampleRate, outSampleRate);
    LOGD("NativeAudioEngine initialized with inRate=%d, outRate=%d", inSampleRate, outSampleRate);
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetInputSampleRate(
        JNIEnv* env, jobject thiz, jint inRate) {
    if (g_pipeline) {
        g_pipeline->setInputSampleRate(inRate);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetUpsamplingTarget(
        JNIEnv* env, jobject thiz, jint targetRate, jint filterMode) {
    if (g_pipeline) {
        g_pipeline->setUpsamplingTarget(targetRate, static_cast<ResamplerFilterMode>(filterMode));
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetGraphicEqBand(
        JNIEnv* env, jobject thiz, jint bandIndex, jfloat gainDb) {
    if (g_pipeline) {
        g_pipeline->setGraphicEqBand(bandIndex, gainDb);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetGraphicEqEnabled(
        JNIEnv* env, jobject thiz, jboolean enabled) {
    if (g_pipeline) {
        g_pipeline->setGraphicEqEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetParametricEqBand(
        JNIEnv* env, jobject thiz, jint bandIndex, jint type, jfloat freq, jfloat gainDb, jfloat q, jboolean enabled) {
    if (g_pipeline) {
        g_pipeline->setParametricEqBand(bandIndex, type, freq, gainDb, q, enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetParametricEqPreamp(
        JNIEnv* env, jobject thiz, jfloat preampDb) {
    if (g_pipeline) {
        g_pipeline->setParametricEqPreamp(preampDb);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetAacEnhancement(
        JNIEnv* env, jobject thiz, jboolean enabled, jfloat intensity) {
    if (g_pipeline) {
        g_pipeline->setAacEnhancement(enabled, intensity);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetFlacCrossfeed(
        JNIEnv* env, jobject thiz, jboolean enabled, jfloat strength) {
    if (g_pipeline) {
        g_pipeline->setFlacCrossfeed(enabled, strength);
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeSetFlacRollOffFilter(
        JNIEnv* env, jobject thiz, jboolean enabled, jfloat cutoffHz) {
    if (g_pipeline) {
        g_pipeline->setFlacRollOffFilter(enabled, cutoffHz);
    }
}

JNIEXPORT jint JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeProcess(
        JNIEnv* env, jobject thiz, jfloatArray inArray, jint inFrames, jfloatArray outArray, jint maxOutFrames) {
    if (!g_pipeline || inFrames <= 0) return 0;

    jfloat* inPtr = env->GetFloatArrayElements(inArray, nullptr);
    jfloat* outPtr = env->GetFloatArrayElements(outArray, nullptr);

    int samplesWritten = g_pipeline->processInterleaved(inPtr, inFrames, outPtr, maxOutFrames);

    env->ReleaseFloatArrayElements(inArray, inPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(outArray, outPtr, 0);

    return samplesWritten;
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeGetFftMagnitudes(
        JNIEnv* env, jobject thiz, jfloatArray outBinsArray, jint numBins) {
    if (!g_pipeline || numBins <= 0) return;

    jfloat* outBins = env->GetFloatArrayElements(outBinsArray, nullptr);
    g_pipeline->getFftMagnitudes(outBins, numBins);
    env->ReleaseFloatArrayElements(outBinsArray, outBins, 0);
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeReset(
        JNIEnv* env, jobject thiz) {
    if (g_pipeline) {
        g_pipeline->reset();
    }
}

JNIEXPORT void JNICALL
Java_com_hiresaudioplayer_app_audio_NativeAudioEngine_nativeRelease(
        JNIEnv* env, jobject thiz) {
    g_pipeline.reset();
}

} // extern "C"

