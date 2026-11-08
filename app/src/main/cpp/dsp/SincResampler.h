#ifndef SINC_RESAMPLER_H
#define SINC_RESAMPLER_H

#include <vector>

enum class ResamplerFilterMode {
    LINEAR_PHASE = 0,
    MINIMUM_PHASE = 1,
    FAST_CUBIC = 2
};

class SincResampler {
public:
    SincResampler();
    ~SincResampler() = default;

    void configure(int inSampleRate, int outSampleRate, ResamplerFilterMode mode = ResamplerFilterMode::LINEAR_PHASE);
    void reset();

    // Resamples stereo float audio. Returns number of output frames produced.
    int processStereo(const float* inLeft, const float* inRight, int inFrames,
                      float* outLeft, float* outRight, int maxOutFrames);

    int getInSampleRate() const { return inRate_; }
    int getOutSampleRate() const { return outRate_; }

private:
    void generateSincTable();

    int inRate_;
    int outRate_;
    ResamplerFilterMode mode_;
    double timeRatio_; // inRate / outRate
    double subSamplePos_;

    // Ring buffer history for sinc convolution
    static constexpr int SINC_HALF_WIDTH = 16;
    static constexpr int SINC_TABLE_STEPS = 64; // Polyphase phases
    std::vector<float> sincTable_;

    std::vector<float> histLeft_;
    std::vector<float> histRight_;
    int histIndex_;
};

#endif // SINC_RESAMPLER_H

