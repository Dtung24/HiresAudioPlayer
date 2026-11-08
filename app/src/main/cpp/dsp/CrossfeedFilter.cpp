#include "CrossfeedFilter.h"
#include <algorithm>

CrossfeedFilter::CrossfeedFilter()
    : sampleRate_(48000.0f), enabled_(false), strength_(0.5f) {
    init(48000.0f);
}

void CrossfeedFilter::init(float sampleRate) {
    sampleRate_ = sampleRate;
    // Jan Meier crossfeed standard: ~700 Hz cutoff low-shelf / low-pass
    lpLeftToRight_.configure(FilterType::LOW_PASS, 700.0f, sampleRate_, 0.0f, 0.5f);
    lpRightToLeft_.configure(FilterType::LOW_PASS, 700.0f, sampleRate_, 0.0f, 0.5f);
}

void CrossfeedFilter::setEnabled(bool enabled) {
    enabled_ = enabled;
}

void CrossfeedFilter::setStrength(float strength) {
    strength_ = std::clamp(strength, 0.0f, 1.0f);
}

void CrossfeedFilter::reset() {
    lpLeftToRight_.reset();
    lpRightToLeft_.reset();
}

void CrossfeedFilter::processStereo(float* left, float* right, int numFrames) {
    if (!enabled_ || strength_ <= 0.001f) return;

    // Crossfeed factor: typically up to ~0.35 max mix to preserve stereo separation
    float blend = strength_ * 0.32f;
    float directGain = 1.0f - (blend * 0.5f);

    for (int i = 0; i < numFrames; ++i) {
        float l = left[i];
        float r = right[i];

        // Crossed signals filtered through head acoustic model
        float crossL = lpRightToLeft_.process(r) * blend;
        float crossR = lpLeftToRight_.process(l) * blend;

        left[i] = (l * directGain) + crossL;
        right[i] = (r * directGain) + crossR;
    }
}

