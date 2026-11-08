#include "FftAnalyzer.h"
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

FftAnalyzer::FftAnalyzer()
    : inputWritePos_(0) {
    inputBuffer_.resize(FFT_SIZE, 0.0f);
    window_.resize(FFT_SIZE);
    smoothMagnitudes_.resize(NUM_BINS, 0.0f);

    // Precalculate Hann window
    for (int i = 0; i < FFT_SIZE; ++i) {
        window_[i] = 0.5f * (1.0f - std::cos(2.0f * static_cast<float>(M_PI) * i / (FFT_SIZE - 1)));
    }
}

void FftAnalyzer::pushSamples(const float* left, const float* right, int numFrames) {
    std::lock_guard<std::mutex> lock(mutex_);
    for (int i = 0; i < numFrames; ++i) {
        // Mono sum for spectrum
        inputBuffer_[inputWritePos_] = (left[i] + right[i]) * 0.5f;
        inputWritePos_++;
        if (inputWritePos_ >= FFT_SIZE) {
            computeFft();
            inputWritePos_ = 0;
        }
    }
}

// In-place Cooley-Tukey Radix-2 FFT
static void fftRadix2(std::vector<float>& real, std::vector<float>& imag) {
    int n = static_cast<int>(real.size());
    int j = 0;
    for (int i = 0; i < n - 1; ++i) {
        if (i < j) {
            std::swap(real[i], real[j]);
            std::swap(imag[i], imag[j]);
        }
        int k = n / 2;
        while (k <= j) {
            j -= k;
            k /= 2;
        }
        j += k;
    }

    for (int len = 2; len <= n; len <<= 1) {
        float angle = -2.0f * static_cast<float>(M_PI) / len;
        float wlen_r = std::cos(angle);
        float wlen_i = std::sin(angle);
        for (int i = 0; i < n; i += len) {
            float w_r = 1.0f;
            float w_i = 0.0f;
            for (int k = 0; k < len / 2; ++k) {
                float u_r = real[i + k];
                float u_i = imag[i + k];
                float v_r = real[i + k + len / 2] * w_r - imag[i + k + len / 2] * w_i;
                float v_i = real[i + k + len / 2] * w_i + imag[i + k + len / 2] * w_r;
                real[i + k] = u_r + v_r;
                imag[i + k] = u_i + v_i;
                real[i + k + len / 2] = u_r - v_r;
                imag[i + k + len / 2] = u_i - v_i;

                float next_w_r = w_r * wlen_r - w_i * wlen_i;
                float next_w_i = w_r * wlen_i + w_i * wlen_r;
                w_r = next_w_r;
                w_i = next_w_i;
            }
        }
    }
}

void FftAnalyzer::computeFft() {
    std::vector<float> real(FFT_SIZE);
    std::vector<float> imag(FFT_SIZE, 0.0f);

    for (int i = 0; i < FFT_SIZE; ++i) {
        real[i] = inputBuffer_[i] * window_[i];
    }

    fftRadix2(real, imag);

    // Group FFT bins logarithmically into NUM_BINS
    int halfSize = FFT_SIZE / 2;
    for (int b = 0; b < NUM_BINS; ++b) {
        // Logarithmic frequency distribution
        float p0 = static_cast<float>(b) / NUM_BINS;
        float p1 = static_cast<float>(b + 1) / NUM_BINS;
        int k0 = std::clamp(static_cast<int>(std::pow(halfSize, p0) - 1.0f), 0, halfSize - 1);
        int k1 = std::clamp(static_cast<int>(std::pow(halfSize, p1)), k0 + 1, halfSize);

        float sumMag = 0.0f;
        for (int k = k0; k < k1; ++k) {
            float mag = std::sqrt(real[k] * real[k] + imag[k] * imag[k]);
            sumMag += mag;
        }
        float avgMag = sumMag / (k1 - k0);

        // Normalize to 0.0 .. 1.0 range with log perceptual scaling
        float normalized = std::clamp((std::log10(avgMag + 1e-4f) + 4.0f) / 4.0f, 0.0f, 1.0f);

        // Smooth decay
        if (normalized >= smoothMagnitudes_[b]) {
            smoothMagnitudes_[b] = normalized; // instant attack
        } else {
            smoothMagnitudes_[b] = smoothMagnitudes_[b] * 0.82f + normalized * 0.18f; // smooth decay
        }
    }
}

void FftAnalyzer::getMagnitudes(float* outMagnitudes, int numOutBins) {
    std::lock_guard<std::mutex> lock(mutex_);
    int count = std::min(numOutBins, NUM_BINS);
    for (int i = 0; i < count; ++i) {
        outMagnitudes[i] = smoothMagnitudes_[i];
    }
}

