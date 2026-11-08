#include "HarmonicExciter.h"
#include <algorithm>
#include <cmath>

HarmonicExciter::HarmonicExciter()
    : sampleRate_(48000.0f), enabled_(false), intensity_(0.35f), drive_(1.5f) {
    init(48000.0f);
}

void HarmonicExciter::init(float sampleRate) {
    sampleRate_ = sampleRate;

    // Bandpass centered at 12 kHz, Q = 1.8 (taps ~9 kHz - 15 kHz)
    bpLeft_.configure(FilterType::BAND_PASS, 12000.0f, sampleRate_, 0.0f, 1.8f);
    bpRight_.configure(FilterType::BAND_PASS, 12000.0f, sampleRate_, 0.0f, 1.8f);

    // Highpass cutoff at 15 kHz to isolate synthesized high harmonics
    hpLeft_.configure(FilterType::HIGH_PASS, 15000.0f, sampleRate_, 0.0f, 0.707f);
    hpRight_.configure(FilterType::HIGH_PASS, 15000.0f, sampleRate_, 0.0f, 0.707f);
}

void HarmonicExciter::setEnabled(bool enabled) {
    enabled_ = enabled;
}

void HarmonicExciter::setIntensity(float intensity) {
    intensity_ = std::clamp(intensity, 0.0f, 1.0f);
}

void HarmonicExciter::setHarmonicDrive(float drive) {
    drive_ = std::clamp(drive, 0.5f, 3.0f);
}

void HarmonicExciter::reset() {
    bpLeft_.reset();
    bpRight_.reset();
    hpLeft_.reset();
    hpRight_.reset();
}

void HarmonicExciter::processStereo(float* left, float* right, int numFrames) {
    if (!enabled_ || intensity_ <= 0.001f) return;

    float wetGain = intensity_ * 0.45f;

    for (int i = 0; i < numFrames; ++i) {
        float l = left[i];
        float r = right[i];

        // 1. Extract bandpass region
        float bpL = bpLeft_.process(l) * drive_;
        float bpR = bpRight_.process(r) * drive_;

        // 2. Non-linear harmonic generation: combination of 2nd (warmth/air) & 3rd (presence) harmonics
        // Soft-saturation / polynomial expansion
        float harmL = 0.5f * (bpL * bpL) + 0.3f * (bpL * bpL * bpL);
        float harmR = 0.5f * (bpR * bpR) + 0.3f * (bpR * bpR * bpR);

        // 3. High-pass filter above 15 kHz
        float sheenL = hpLeft_.process(harmL);
        float sheenR = hpRight_.process(harmR);

        // 4. Blend back into original signal
        left[i] = l + sheenL * wetGain;
        right[i] = r + sheenR * wetGain;
    }
}

