#ifndef BIQUAD_FILTER_H
#define BIQUAD_FILTER_H

#include <cmath>

enum class FilterType {
    PEAK = 0,
    LOW_SHELF = 1,
    HIGH_SHELF = 2,
    LOW_PASS = 3,
    HIGH_PASS = 4,
    BAND_PASS = 5,
    NOTCH = 6
};

class BiquadFilter {
public:
    BiquadFilter();
    ~BiquadFilter() = default;

    void configure(FilterType type, float frequencyHz, float sampleRate, float gainDb, float qFactor);
    void reset();

    inline float process(float inSample) {
        // Direct Form II Transposed for numerical stability and zero denormal latency
        float out = b0 * inSample + z1;
        z1 = b1 * inSample - a1 * out + z2;
        z2 = b2 * inSample - a2 * out;
        return out;
    }

private:
    float b0, b1, b2;
    float a1, a2;
    float z1, z2;
};

#endif // BIQUAD_FILTER_H

