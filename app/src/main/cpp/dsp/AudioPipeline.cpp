#include "AudioPipeline.h"
#include <algorithm>
#include <cmath>

AudioPipeline::AudioPipeline()
    : inSampleRate_(44100), outSampleRate_(192000), filterMode_(ResamplerFilterMode::LINEAR_PHASE),
      flacRollOffEnabled_(false) {
    init(44100, 192000);
}

void AudioPipeline::init(int inputSampleRate, int targetUpsampleRate) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    inSampleRate_ = inputSampleRate;
    outSampleRate_ = targetUpsampleRate;

    graphicEq_.init(static_cast<float>(inSampleRate_));
    peq_.init(static_cast<float>(inSampleRate_), 7);
    harmonicExciter_.init(static_cast<float>(inSampleRate_));
    crossfeed_.init(static_cast<float>(inSampleRate_));
    resampler_.configure(inSampleRate_, outSampleRate_, filterMode_);

    flacLpLeft_.configure(FilterType::LOW_PASS, 20000.0f, static_cast<float>(inSampleRate_), 0.0f, 0.707f);
    flacLpRight_.configure(FilterType::LOW_PASS, 20000.0f, static_cast<float>(inSampleRate_), 0.0f, 0.707f);

    planarInL_.resize(4096);
    planarInR_.resize(4096);
    planarOutL_.resize(16384);
    planarOutR_.resize(16384);
}

void AudioPipeline::setInputSampleRate(int inRate) {
    if (inRate == inSampleRate_ || inRate <= 0) return;
    std::lock_guard<std::mutex> lock(dspMutex_);
    inSampleRate_ = inRate;
    graphicEq_.setSampleRate(static_cast<float>(inSampleRate_));
    peq_.setSampleRate(static_cast<float>(inSampleRate_));
    harmonicExciter_.init(static_cast<float>(inSampleRate_));
    crossfeed_.init(static_cast<float>(inSampleRate_));
    resampler_.configure(inSampleRate_, outSampleRate_, filterMode_);
}

void AudioPipeline::setUpsamplingTarget(int targetRate, ResamplerFilterMode filterMode) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    outSampleRate_ = targetRate;
    filterMode_ = filterMode;
    resampler_.configure(inSampleRate_, outSampleRate_, filterMode_);
}

void AudioPipeline::setAacEnhancement(bool enabled, float intensity) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    harmonicExciter_.setEnabled(enabled);
    harmonicExciter_.setIntensity(intensity);
}

void AudioPipeline::setFlacCrossfeed(bool enabled, float strength) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    crossfeed_.setEnabled(enabled);
    crossfeed_.setStrength(strength);
}

void AudioPipeline::setFlacRollOffFilter(bool enabled, float cutoffHz) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    flacRollOffEnabled_ = enabled;
    if (enabled) {
        flacLpLeft_.configure(FilterType::LOW_PASS, cutoffHz, static_cast<float>(inSampleRate_), 0.0f, 0.707f);
        flacLpRight_.configure(FilterType::LOW_PASS, cutoffHz, static_cast<float>(inSampleRate_), 0.0f, 0.707f);
    }
}

void AudioPipeline::setGraphicEqBand(int bandIndex, float gainDb) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    graphicEq_.setBandGain(bandIndex, gainDb);
}

void AudioPipeline::setGraphicEqEnabled(bool enabled) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    graphicEq_.setEnabled(enabled);
}

void AudioPipeline::setParametricEqBand(int bandIndex, int type, float freq, float gainDb, float q, bool enabled) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    peq_.setBand(bandIndex, static_cast<FilterType>(type), freq, gainDb, q, enabled);
}

void AudioPipeline::setParametricEqPreamp(float preampDb) {
    std::lock_guard<std::mutex> lock(dspMutex_);
    peq_.setPreampGain(preampDb);
}

void AudioPipeline::getFftMagnitudes(float* outBins, int numBins) {
    fftAnalyzer_.getMagnitudes(outBins, numBins);
}

void AudioPipeline::reset() {
    std::lock_guard<std::mutex> lock(dspMutex_);
    graphicEq_.reset();
    peq_.reset();
    harmonicExciter_.reset();
    crossfeed_.reset();
    resampler_.reset();
    flacLpLeft_.reset();
    flacLpRight_.reset();
}

int AudioPipeline::processInterleaved(const float* inInterleaved, int inFrames,
                                     float* outInterleaved, int maxOutFrames) {
    std::lock_guard<std::mutex> lock(dspMutex_);

    if (inFrames <= 0) return 0;

    // Ensure capacity
    if (static_cast<int>(planarInL_.size()) < inFrames) {
        planarInL_.resize(inFrames * 2);
        planarInR_.resize(inFrames * 2);
    }

    // 1. De-interleave input
    for (int i = 0; i < inFrames; ++i) {
        planarInL_[i] = inInterleaved[i * 2];
        planarInR_[i] = inInterleaved[i * 2 + 1];
    }

    // 2. Codec-specific DSP stage
    // A) AAC High Frequency Harmonics Recovery
    if (harmonicExciter_.isEnabled()) {
        harmonicExciter_.processStereo(planarInL_.data(), planarInR_.data(), inFrames);
    }

    // B) FLAC Meier Binaural Crossfeed
    if (crossfeed_.isEnabled()) {
        crossfeed_.processStereo(planarInL_.data(), planarInR_.data(), inFrames);
    }

    // C) FLAC Smooth Roll-Off Filter
    if (flacRollOffEnabled_) {
        for (int i = 0; i < inFrames; ++i) {
            planarInL_[i] = flacLpLeft_.process(planarInL_[i]);
            planarInR_[i] = flacLpRight_.process(planarInR_[i]);
        }
    }

    // 3. Graphic Equalizer
    graphicEq_.processStereo(planarInL_.data(), planarInR_.data(), inFrames);

    // 4. Parametric Equalizer & Preamp
    peq_.processStereo(planarInL_.data(), planarInR_.data(), inFrames);

    // 5. Hi-Res Upsampling (Windowed Sinc up to 192 kHz)
    int estimatedOut = static_cast<int>(inFrames * (static_cast<double>(outSampleRate_) / inSampleRate_)) + 32;
    if (static_cast<int>(planarOutL_.size()) < estimatedOut) {
        planarOutL_.resize(estimatedOut * 2);
        planarOutR_.resize(estimatedOut * 2);
    }

    int producedFrames = resampler_.processStereo(
        planarInL_.data(), planarInR_.data(), inFrames,
        planarOutL_.data(), planarOutR_.data(), maxOutFrames
    );

    // 6. Transparent Soft-Clipping Limiter (tanh saturation safeguard)
    for (int i = 0; i < producedFrames; ++i) {
        float l = planarOutL_[i];
        float r = planarOutR_[i];

        if (std::abs(l) > 0.95f) {
            l = std::copysign(0.95f + 0.05f * std::tanh((std::abs(l) - 0.95f) / 0.05f), l);
        }
        if (std::abs(r) > 0.95f) {
            r = std::copysign(0.95f + 0.05f * std::tanh((std::abs(r) - 0.95f) / 0.05f), r);
        }

        planarOutL_[i] = l;
        planarOutR_[i] = r;
    }

    // 7. Push to FFT Visualizer
    fftAnalyzer_.pushSamples(planarOutL_.data(), planarOutR_.data(), producedFrames);

    // 8. Re-interleave output
    for (int i = 0; i < producedFrames; ++i) {
        outInterleaved[i * 2] = planarOutL_[i];
        outInterleaved[i * 2 + 1] = planarOutR_[i];
    }

    return producedFrames * 2; // return total float samples
}

