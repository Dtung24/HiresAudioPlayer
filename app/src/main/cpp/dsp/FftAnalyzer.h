#ifndef FFT_ANALYZER_H
#define FFT_ANALYZER_H

#include <vector>
#include <mutex>

class FftAnalyzer {
public:
    static constexpr int FFT_SIZE = 512;
    static constexpr int NUM_BINS = 64;

    FftAnalyzer();
    ~FftAnalyzer() = default;

    void pushSamples(const float* left, const float* right, int numFrames);
    void getMagnitudes(float* outMagnitudes, int numOutBins);

private:
    void computeFft();

    std::vector<float> inputBuffer_;
    int inputWritePos_;
    std::vector<float> window_;
    std::vector<float> smoothMagnitudes_;
    std::mutex mutex_;
};

#endif // FFT_ANALYZER_H

