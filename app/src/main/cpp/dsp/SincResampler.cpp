#include "SincResampler.h"
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

SincResampler::SincResampler()
    : inRate_(44100), outRate_(192000), mode_(ResamplerFilterMode::LINEAR_PHASE),
      timeRatio_(44100.0 / 192000.0), subSamplePos_(0.0), histIndex_(0) {
    histLeft_.resize(SINC_HALF_WIDTH * 2, 0.0f);
    histRight_.resize(SINC_HALF_WIDTH * 2, 0.0f);
    generateSincTable();
}

void SincResampler::configure(int inSampleRate, int outSampleRate, ResamplerFilterMode mode) {
    if (inSampleRate <= 0) inSampleRate = 44100;
    if (outSampleRate <= 0) outSampleRate = 192000;

    inRate_ = inSampleRate;
    outRate_ = outSampleRate;
    mode_ = mode;
    timeRatio_ = static_cast<double>(inRate_) / static_cast<double>(outRate_);
    reset();
    generateSincTable();
}

void SincResampler::reset() {
    subSamplePos_ = 0.0;
    histIndex_ = 0;
    std::fill(histLeft_.begin(), histLeft_.end(), 0.0f);
    std::fill(histRight_.begin(), histRight_.end(), 0.0f);
}

void SincResampler::generateSincTable() {
    int totalEntries = SINC_TABLE_STEPS * SINC_HALF_WIDTH * 2;
    sincTable_.resize(totalEntries);

    // Blackman-Harris windowed sinc
    double cutoff = std::min(1.0, static_cast<double>(outRate_) / static_cast<double>(inRate_)) * 0.95;

    for (int step = 0; step < SINC_TABLE_STEPS; ++step) {
        double frac = static_cast<double>(step) / static_cast<double>(SINC_TABLE_STEPS);
        double sumWeights = 0.0;
        int stepOffset = step * (SINC_HALF_WIDTH * 2);

        for (int i = -SINC_HALF_WIDTH; i < SINC_HALF_WIDTH; ++i) {
            double t = (static_cast<double>(i) - frac);
            double window = 1.0;

            // Blackman-Harris 4-term window
            double wT = (t + SINC_HALF_WIDTH) / (2.0 * SINC_HALF_WIDTH);
            if (wT >= 0.0 && wT <= 1.0) {
                const double a0 = 0.35875;
                const double a1 = 0.48829;
                const double a2 = 0.14128;
                const double a3 = 0.01168;
                window = a0 - a1 * std::cos(2.0 * M_PI * wT) + a2 * std::cos(4.0 * M_PI * wT) - a3 * std::cos(6.0 * M_PI * wT);
            } else {
                window = 0.0;
            }

            // Minimum phase exponential decay shaping if requested
            if (mode_ == ResamplerFilterMode::MINIMUM_PHASE && t < 0) {
                window *= std::exp(t * 0.25);
            }

            double sincVal;
            if (std::abs(t) < 1e-7) {
                sincVal = 1.0;
            } else {
                double x = M_PI * t * cutoff;
                sincVal = std::sin(x) / x;
            }

            float w = static_cast<float>(sincVal * window);
            sincTable_[stepOffset + (i + SINC_HALF_WIDTH)] = w;
            sumWeights += w;
        }

        // Normalize DC gain to 1.0
        if (std::abs(sumWeights) > 1e-6) {
            float invSum = static_cast<float>(1.0 / sumWeights);
            for (int i = 0; i < SINC_HALF_WIDTH * 2; ++i) {
                sincTable_[stepOffset + i] *= invSum;
            }
        }
    }
}

int SincResampler::processStereo(const float* inLeft, const float* inRight, int inFrames,
                                float* outLeft, float* outRight, int maxOutFrames) {
    if (inRate_ == outRate_) {
        // Bit-perfect passthrough
        int framesToCopy = std::min(inFrames, maxOutFrames);
        for (int i = 0; i < framesToCopy; ++i) {
            outLeft[i] = inLeft[i];
            outRight[i] = inRight[i];
        }
        return framesToCopy;
    }

    int inPos = 0;
    int outPos = 0;
    int ringSize = SINC_HALF_WIDTH * 2;

    while (outPos < maxOutFrames) {
        while (subSamplePos_ >= 1.0 && inPos < inFrames) {
            histLeft_[histIndex_] = inLeft[inPos];
            histRight_[histIndex_] = inRight[inPos];
            histIndex_ = (histIndex_ + 1) % ringSize;
            inPos++;
            subSamplePos_ -= 1.0;
        }

        if (inPos >= inFrames && subSamplePos_ >= 1.0) {
            // Need more input frames
            break;
        }

        // Subsample phase
        int step = static_cast<int>(subSamplePos_ * SINC_TABLE_STEPS);
        step = std::clamp(step, 0, SINC_TABLE_STEPS - 1);
        int tableOffset = step * ringSize;

        float sumL = 0.0f;
        float sumR = 0.0f;

        for (int k = 0; k < ringSize; ++k) {
            int histIdx = (histIndex_ - ringSize + k + ringSize) % ringSize;
            float weight = sincTable_[tableOffset + k];
            sumL += histLeft_[histIdx] * weight;
            sumR += histRight_[histIdx] * weight;
        }

        outLeft[outPos] = sumL;
        outRight[outPos] = sumR;
        outPos++;

        subSamplePos_ += timeRatio_;
    }

    return outPos;
}

