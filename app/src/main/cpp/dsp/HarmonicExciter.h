#ifndef HARMONIC_EXCITER_H
#define HARMONIC_EXCITER_H

#include "BiquadFilter.h"

class HarmonicExciter {
public:
    HarmonicExciter();
    ~HarmonicExciter() = default;

    void init(float sampleRate);
    void setEnabled(bool enabled);
    void setIntensity(float intensity); // 0.0 to 1.0
    void setHarmonicDrive(float drive);   // 0.5 to 2.5
    void reset();

    void processStereo(float* left, float* right, int numFrames);

    bool isEnabled() const { return enabled_; }
    float getIntensity() const { return intensity_; }

private:
    float sampleRate_;
    bool enabled_;
    float intensity_;
    float drive_;

    // Analysis bandpass filters (extract upper mids 10kHz - 15kHz)
    BiquadFilter bpLeft_;
    BiquadFilter bpRight_;

    // Post-harmonics high-pass filter (isolate newly generated 16kHz+ sheen)
    BiquadFilter hpLeft_;
    BiquadFilter hpRight_;
};

#endif // HARMONIC_EXCITER_H

