#ifndef AUDIO_PIPELINE_H
#define AUDIO_PIPELINE_H

#include "BiquadFilter.h"
#include "ParametricEqualizer.h"
#include "GraphicEqualizer.h"
#include "SincResampler.h"
#include "HarmonicExciter.h"
#include "CrossfeedFilter.h"
#include "FftAnalyzer.h"
#include <vector>
#include <mutex>

class AudioPipeline {
public:
    AudioPipeline();
    ~AudioPipeline() = default;

    void init(int inputSampleRate, int targetUpsampleRate);
    void setInputSampleRate(int inRate);
    void setUpsamplingTarget(int targetRate, ResamplerFilterMode filterMode);

    // Codec-specific DSP toggles
    void setAacEnhancement(bool enabled, float intensity);
    void setFlacCrossfeed(bool enabled, float strength);
    void setFlacRollOffFilter(bool enabled, float cutoffHz);

    // EQ controls
    void setGraphicEqBand(int bandIndex, float gainDb);
    void setGraphicEqEnabled(bool enabled);
    void setParametricEqBand(int bandIndex, int type, float freq, float gainDb, float q, bool enabled);
    void setParametricEqPreamp(float preampDb);

    // FFT visualizer query
    void getFftMagnitudes(float* outBins, int numBins);

    // Main DSP processing function. Takes interleaved stereo float input [L0, R0, L1, R1...]
    // Produces interleaved stereo float output at upsampled rate [L0, R0, L1, R1...].
    // Returns total number of output samples written (frames * 2).
    int processInterleaved(const float* inInterleaved, int inFrames,
                           float* outInterleaved, int maxOutFrames);

    void reset();

private:
    int inSampleRate_;
    int outSampleRate_;
    ResamplerFilterMode filterMode_;

    GraphicEqualizer graphicEq_;
    ParametricEqualizer peq_;
    HarmonicExciter harmonicExciter_;
    CrossfeedFilter crossfeed_;
    SincResampler resampler_;
    FftAnalyzer fftAnalyzer_;

    // Flac Roll-Off lowpass
    bool flacRollOffEnabled_;
    BiquadFilter flacLpLeft_;
    BiquadFilter flacLpRight_;

    // Intermediate planar buffers
    std::vector<float> planarInL_;
    std::vector<float> planarInR_;
    std::vector<float> planarOutL_;
    std::vector<float> planarOutR_;

    std::mutex dspMutex_;
};

#endif // AUDIO_PIPELINE_H

