#include "GraphicEqualizer.h"
#include <cmath>

const float GraphicEqualizer::BAND_FREQUENCIES[GraphicEqualizer::NUM_BANDS] = {
    31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
    1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
};

GraphicEqualizer::GraphicEqualizer()
    : sampleRate_(48000.0f), enabled_(true) {
    gainsDb_.fill(0.0f);
    init(48000.0f);
}

void GraphicEqualizer::init(float sampleRate) {
    sampleRate_ = sampleRate;
    // Standard 10-band octave Q factor is ~ 1.414 (bandwidth = 1 octave)
    const float Q_OCTAVE = 1.4142f;

    for (int i = 0; i < NUM_BANDS; ++i) {
        FilterType type = FilterType::PEAK;
        if (i == 0) type = FilterType::LOW_SHELF;
        else if (i == NUM_BANDS - 1) type = FilterType::HIGH_SHELF;

        leftFilters_[i].configure(type, BAND_FREQUENCIES[i], sampleRate_, gainsDb_[i], Q_OCTAVE);
        rightFilters_[i].configure(type, BAND_FREQUENCIES[i], sampleRate_, gainsDb_[i], Q_OCTAVE);
    }
}

void GraphicEqualizer::setBandGain(int bandIndex, float gainDb) {
    if (bandIndex < 0 || bandIndex >= NUM_BANDS) return;
    gainsDb_[bandIndex] = gainDb;

    const float Q_OCTAVE = 1.4142f;
    FilterType type = FilterType::PEAK;
    if (bandIndex == 0) type = FilterType::LOW_SHELF;
    else if (bandIndex == NUM_BANDS - 1) type = FilterType::HIGH_SHELF;

    leftFilters_[bandIndex].configure(type, BAND_FREQUENCIES[bandIndex], sampleRate_, gainDb, Q_OCTAVE);
    rightFilters_[bandIndex].configure(type, BAND_FREQUENCIES[bandIndex], sampleRate_, gainDb, Q_OCTAVE);
}

float GraphicEqualizer::getBandGain(int bandIndex) const {
    if (bandIndex < 0 || bandIndex >= NUM_BANDS) return 0.0f;
    return gainsDb_[bandIndex];
}

void GraphicEqualizer::setSampleRate(float sampleRate) {
    init(sampleRate);
}

void GraphicEqualizer::setEnabled(bool enabled) {
    enabled_ = enabled;
}

void GraphicEqualizer::reset() {
    for (auto& f : leftFilters_) f.reset();
    for (auto& f : rightFilters_) f.reset();
}

void GraphicEqualizer::processStereo(float* left, float* right, int numFrames) {
    if (!enabled_) return;

    for (int i = 0; i < numFrames; ++i) {
        float l = left[i];
        float r = right[i];

        for (int b = 0; b < NUM_BANDS; ++b) {
            if (std::abs(gainsDb_[b]) > 0.01f) {
                l = leftFilters_[b].process(l);
                r = rightFilters_[b].process(r);
            }
        }

        left[i] = l;
        right[i] = r;
    }
}

