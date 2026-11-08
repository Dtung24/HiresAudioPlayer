#include "ParametricEqualizer.h"
#include <cmath>

ParametricEqualizer::ParametricEqualizer()
    : sampleRate_(48000.0f), preampLinear_(1.0f) {
    init(48000.0f, 7);
}

void ParametricEqualizer::init(float sampleRate, int numBands) {
    sampleRate_ = sampleRate;
    configs_.resize(numBands);
    leftFilters_.resize(numBands);
    rightFilters_.resize(numBands);

    // Default 7-band parametric audiophile distribution
    const float defaultFreqs[7] = { 60.0f, 150.0f, 450.0f, 1000.0f, 2500.0f, 6000.0f, 12000.0f };
    const FilterType defaultTypes[7] = {
        FilterType::LOW_SHELF,
        FilterType::PEAK,
        FilterType::PEAK,
        FilterType::PEAK,
        FilterType::PEAK,
        FilterType::PEAK,
        FilterType::HIGH_SHELF
    };

    for (int i = 0; i < numBands; ++i) {
        configs_[i].enabled = true;
        configs_[i].type = (i < 7) ? defaultTypes[i] : FilterType::PEAK;
        configs_[i].frequency = (i < 7) ? defaultFreqs[i] : 1000.0f * (i + 1);
        configs_[i].gainDb = 0.0f;
        configs_[i].q = 1.0f;

        leftFilters_[i].configure(configs_[i].type, configs_[i].frequency, sampleRate_, 0.0f, configs_[i].q);
        rightFilters_[i].configure(configs_[i].type, configs_[i].frequency, sampleRate_, 0.0f, configs_[i].q);
    }
}

void ParametricEqualizer::setBand(int bandIndex, FilterType type, float frequency, float gainDb, float q, bool enabled) {
    if (bandIndex < 0 || bandIndex >= static_cast<int>(configs_.size())) return;

    configs_[bandIndex].enabled = enabled;
    configs_[bandIndex].type = type;
    configs_[bandIndex].frequency = frequency;
    configs_[bandIndex].gainDb = gainDb;
    configs_[bandIndex].q = q;

    float effectiveGain = enabled ? gainDb : 0.0f;
    leftFilters_[bandIndex].configure(type, frequency, sampleRate_, effectiveGain, q);
    rightFilters_[bandIndex].configure(type, frequency, sampleRate_, effectiveGain, q);
}

void ParametricEqualizer::setPreampGain(float preampDb) {
    preampLinear_ = std::pow(10.0f, preampDb / 20.0f);
}

void ParametricEqualizer::setSampleRate(float sampleRate) {
    sampleRate_ = sampleRate;
    for (size_t i = 0; i < configs_.size(); ++i) {
        float effectiveGain = configs_[i].enabled ? configs_[i].gainDb : 0.0f;
        leftFilters_[i].configure(configs_[i].type, configs_[i].frequency, sampleRate_, effectiveGain, configs_[i].q);
        rightFilters_[i].configure(configs_[i].type, configs_[i].frequency, sampleRate_, effectiveGain, configs_[i].q);
    }
}

void ParametricEqualizer::reset() {
    for (auto& f : leftFilters_) f.reset();
    for (auto& f : rightFilters_) f.reset();
}

void ParametricEqualizer::processStereo(float* left, float* right, int numFrames) {
    for (int i = 0; i < numFrames; ++i) {
        float l = left[i] * preampLinear_;
        float r = right[i] * preampLinear_;

        for (size_t b = 0; b < configs_.size(); ++b) {
            if (configs_[b].enabled && std::abs(configs_[b].gainDb) > 0.01f) {
                l = leftFilters_[b].process(l);
                r = rightFilters_[b].process(r);
            }
        }

        left[i] = l;
        right[i] = r;
    }
}

