#ifndef PARAMETRIC_EQUALIZER_H
#define PARAMETRIC_EQUALIZER_H

#include "BiquadFilter.h"
#include <vector>

struct PeqBandConfig {
    bool enabled = true;
    FilterType type = FilterType::PEAK;
    float frequency = 1000.0f;
    float gainDb = 0.0f;
    float q = 1.0f;
};

class ParametricEqualizer {
public:
    ParametricEqualizer();
    ~ParametricEqualizer() = default;

    void init(float sampleRate, int numBands = 7);
    void setBand(int bandIndex, FilterType type, float frequency, float gainDb, float q, bool enabled = true);
    void setPreampGain(float preampDb);
    void setSampleRate(float sampleRate);
    void reset();

    void processStereo(float* left, float* right, int numFrames);

private:
    float sampleRate_;
    float preampLinear_;
    std::vector<PeqBandConfig> configs_;
    std::vector<BiquadFilter> leftFilters_;
    std::vector<BiquadFilter> rightFilters_;
};

#endif // PARAMETRIC_EQUALIZER_H

