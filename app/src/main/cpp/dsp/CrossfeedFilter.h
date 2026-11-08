#ifndef CROSSFEED_FILTER_H
#define CROSSFEED_FILTER_H

#include "BiquadFilter.h"

class CrossfeedFilter {
public:
    CrossfeedFilter();
    ~CrossfeedFilter() = default;

    void init(float sampleRate);
    void setEnabled(bool enabled);
    void setStrength(float strength); // 0.0 (off) to 1.0 (strong Meier crossfeed)
    void reset();

    void processStereo(float* left, float* right, int numFrames);

    bool isEnabled() const { return enabled_; }
    float getStrength() const { return strength_; }

private:
    float sampleRate_;
    bool enabled_;
    float strength_;

    // Low-pass crosstalk filters (approx 700 Hz cutoff)
    BiquadFilter lpLeftToRight_;
    BiquadFilter lpRightToLeft_;
};

#endif // CROSSFEED_FILTER_H

