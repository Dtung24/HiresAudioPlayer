#ifndef GRAPHIC_EQUALIZER_H
#define GRAPHIC_EQUALIZER_H

#include "BiquadFilter.h"
#include <array>

class GraphicEqualizer {
public:
    static constexpr int NUM_BANDS = 10;
    static const float BAND_FREQUENCIES[NUM_BANDS];

    GraphicEqualizer();
    ~GraphicEqualizer() = default;

    void init(float sampleRate);
    void setBandGain(int bandIndex, float gainDb);
    float getBandGain(int bandIndex) const;
    void setSampleRate(float sampleRate);
    void setEnabled(bool enabled);
    bool isEnabled() const { return enabled_; }
    void reset();

    void processStereo(float* left, float* right, int numFrames);

private:
    float sampleRate_;
    bool enabled_;
    std::array<float, NUM_BANDS> gainsDb_;
    std::array<BiquadFilter, NUM_BANDS> leftFilters_;
    std::array<BiquadFilter, NUM_BANDS> rightFilters_;
};

#endif // GRAPHIC_EQUALIZER_H

