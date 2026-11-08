#include "BiquadFilter.h"
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

BiquadFilter::BiquadFilter()
    : b0(1.0f), b1(0.0f), b2(0.0f), a1(0.0f), a2(0.0f), z1(0.0f), z2(0.0f) {}

void BiquadFilter::reset() {
    z1 = 0.0f;
    z2 = 0.0f;
}

void BiquadFilter::configure(FilterType type, float frequencyHz, float sampleRate, float gainDb, float qFactor) {
    if (sampleRate <= 0.0f) sampleRate = 48000.0f;

    // Clamp cutoff safely below Nyquist
    float nyquist = sampleRate * 0.495f;
    float f0 = std::clamp(frequencyHz, 10.0f, nyquist);
    float q = std::max(0.1f, qFactor);
    float A = std::pow(10.0f, gainDb / 40.0f); // sqrt of linear gain for shelving/peaking

    float w0 = 2.0f * static_cast<float>(M_PI) * f0 / sampleRate;
    float cosW0 = std::cos(w0);
    float sinW0 = std::sin(w0);
    float alpha = sinW0 / (2.0f * q);

    float a0_val = 1.0f;
    float b0_val = 1.0f, b1_val = 0.0f, b2_val = 0.0f;
    float a1_val = 0.0f, a2_val = 0.0f;

    switch (type) {
        case FilterType::PEAK: {
            b0_val = 1.0f + alpha * A;
            b1_val = -2.0f * cosW0;
            b2_val = 1.0f - alpha * A;
            a0_val = 1.0f + alpha / A;
            a1_val = -2.0f * cosW0;
            a2_val = 1.0f - alpha / A;
            break;
        }
        case FilterType::LOW_SHELF: {
            float twoSqrtA_alpha = 2.0f * std::sqrt(A) * alpha;
            b0_val = A * ((A + 1.0f) - (A - 1.0f) * cosW0 + twoSqrtA_alpha);
            b1_val = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW0);
            b2_val = A * ((A + 1.0f) - (A - 1.0f) * cosW0 - twoSqrtA_alpha);
            a0_val = (A + 1.0f) + (A - 1.0f) * cosW0 + twoSqrtA_alpha;
            a1_val = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosW0);
            a2_val = (A + 1.0f) + (A - 1.0f) * cosW0 - twoSqrtA_alpha;
            break;
        }
        case FilterType::HIGH_SHELF: {
            float twoSqrtA_alpha = 2.0f * std::sqrt(A) * alpha;
            b0_val = A * ((A + 1.0f) + (A - 1.0f) * cosW0 + twoSqrtA_alpha);
            b1_val = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW0);
            b2_val = A * ((A + 1.0f) + (A - 1.0f) * cosW0 - twoSqrtA_alpha);
            a0_val = (A + 1.0f) - (A - 1.0f) * cosW0 + twoSqrtA_alpha;
            a1_val = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosW0);
            a2_val = (A + 1.0f) - (A - 1.0f) * cosW0 - twoSqrtA_alpha;
            break;
        }
        case FilterType::LOW_PASS: {
            b0_val = (1.0f - cosW0) * 0.5f;
            b1_val = 1.0f - cosW0;
            b2_val = (1.0f - cosW0) * 0.5f;
            a0_val = 1.0f + alpha;
            a1_val = -2.0f * cosW0;
            a2_val = 1.0f - alpha;
            break;
        }
        case FilterType::HIGH_PASS: {
            b0_val = (1.0f + cosW0) * 0.5f;
            b1_val = -(1.0f + cosW0);
            b2_val = (1.0f + cosW0) * 0.5f;
            a0_val = 1.0f + alpha;
            a1_val = -2.0f * cosW0;
            a2_val = 1.0f - alpha;
            break;
        }
        case FilterType::BAND_PASS: {
            b0_val = alpha;
            b1_val = 0.0f;
            b2_val = -alpha;
            a0_val = 1.0f + alpha;
            a1_val = -2.0f * cosW0;
            a2_val = 1.0f - alpha;
            break;
        }
        case FilterType::NOTCH: {
            b0_val = 1.0f;
            b1_val = -2.0f * cosW0;
            b2_val = 1.0f;
            a0_val = 1.0f + alpha;
            a1_val = -2.0f * cosW0;
            a2_val = 1.0f - alpha;
            break;
        }
    }

    // Normalize coefficients by a0
    float invA0 = 1.0f / a0_val;
    b0 = b0_val * invA0;
    b1 = b1_val * invA0;
    b2 = b2_val * invA0;
    a1 = a1_val * invA0;
    a2 = a2_val * invA0;
}

