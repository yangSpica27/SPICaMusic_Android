#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

#include "DspFilters/Dsp.h"
#include "ebur128.h"
#include "pffft/pffft.h"

namespace spica::dsp {

enum class PcmEncoding : int {
    Pcm8 = 1,
    Pcm16 = 2,
    Pcm16BigEndian = 3,
    Pcm24 = 4,
    Pcm24BigEndian = 5,
    Pcm32 = 6,
    Pcm32BigEndian = 7,
    PcmFloat = 8,
};

struct DspParameters {
    bool fftEnabled = false;
    bool eqEnabled = false;
    bool loudnessEnabled = false;
    float targetLufs = -14.0f;
    std::array<float, 10> eqGainsDb{};
};

class FftAnalyzer {
public:
    static constexpr int kFftSize = 4096;
    static constexpr int kBandCount = 31;

    FftAnalyzer();
    ~FftAnalyzer();

    FftAnalyzer(const FftAnalyzer&) = delete;
    FftAnalyzer& operator=(const FftAnalyzer&) = delete;

    bool configure(int sampleRate);
    void setEnabled(bool enabled);
    void setPlaybackActive(bool active);
    void push(const float* mono, std::size_t frames);
    void reset();
    std::uint64_t readBands(float* out, std::size_t count) const;

private:
    void workerLoop();
    void publishBands(const float* values, int generation);
    void mapToBands(const float* magnitudes, float* result) const;

    static constexpr std::size_t kRingSize = 1u << 15;
    static constexpr std::size_t kRingMask = kRingSize - 1;

    std::atomic<bool> stop_{false};
    std::atomic<bool> enabled_{false};
    std::atomic<bool> playbackActive_{false};
    std::atomic<std::size_t> writeIndex_{0};
    std::atomic<std::size_t> readIndex_{0};
    std::atomic<int> generation_{0};
    std::atomic<std::uint64_t> bandsSequence_{0};
    std::atomic<int> activeBandBuffer_{0};

    std::array<float, kRingSize> ring_{};
    std::array<std::array<float, kBandCount>, 2> bandBuffers_{};
    mutable std::mutex bandsMutex_;
    alignas(16) std::array<float, kFftSize> fftInput_{};
    alignas(16) std::array<float, kFftSize> fftOutput_{};
    alignas(16) std::array<float, kFftSize> fftWork_{};
    alignas(16) std::array<float, kFftSize / 2> magnitudes_{};
    std::array<float, kFftSize> hammingWindow_{};

    PFFFT_Setup* setup_ = nullptr;
    int sampleRate_ = 44100;
    int decimationFactor_ = 1;
    float decimationAccumulator_ = 0.0f;
    int decimationCount_ = 0;
    int observedGeneration_ = 0;
    int workerGeneration_ = 0;
    std::thread worker_;
};

class EqProcessor {
public:
    static constexpr int kBandCount = 10;
    static constexpr int kMaxChannels = 16;

    bool configure(int sampleRate, int channelCount, int maxFrames);
    void setEnabled(bool enabled);
    void setTargetGains(const float* gains, std::size_t count);
    void process(float* const* channels, int frames);
    void reset();
    bool isConfigured() const { return configured_; }

private:
    using Filter = Dsp::SimpleFilter<Dsp::RBJ::BandShelf, 1>;

    void updateCoefficients(int frames);

    static constexpr std::array<float, kBandCount> kFrequencies = {
        31.0f, 62.0f, 125.0f, 250.0f, 500.0f,
        1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f,
    };

    std::array<std::array<Filter, kMaxChannels>, kBandCount> filters_{};
    std::array<float, kBandCount> currentGains_{};
    std::array<float, kBandCount> targetGains_{};
    std::array<bool, kBandCount> usableBands_{};
    int sampleRate_ = 44100;
    int channelCount_ = 2;
    int maxFrames_ = 0;
    bool enabled_ = false;
    bool configured_ = false;
};

class LoudnessProcessor {
public:
    bool configure(int sampleRate, int channelCount, int maxFrames);
    void setEnabled(bool enabled);
    void setTargetLufs(float targetLufs);
    void process(float* const* channels, int frames);
    void reset();
    bool isConfigured() const { return meter_ != nullptr; }

private:
    void destroyMeter();
    bool createMeter();

    ebur128_state* meter_ = nullptr;
    std::vector<float> interleaved_;
    int sampleRate_ = 44100;
    int channelCount_ = 2;
    int maxFrames_ = 0;
    bool enabled_ = false;
    float targetLufs_ = -14.0f;
    float currentGain_ = 1.0f;
    float limiterGain_ = 1.0f;
};

class DspEngine {
public:
    static constexpr int kBandCount = 31;
    static constexpr int kEqBandCount = 10;

    DspEngine();
    ~DspEngine();

    DspEngine(const DspEngine&) = delete;
    DspEngine& operator=(const DspEngine&) = delete;

    bool configure(int sampleRate, int channelCount, PcmEncoding encoding, int maxFrames);
    int process(const std::uint8_t* input, std::size_t inputBytes,
                std::uint8_t* output, std::size_t outputCapacity);

    void setParameters(const DspParameters& parameters);
    void setFftEnabled(bool enabled);
    void setPlaybackActive(bool active);
    std::uint64_t readBands(float* out, std::size_t count) const;
    void reset();

    bool isConfigured() const { return configured_; }
    int bytesPerSample() const;
    int channelCount() const { return channelCount_; }

private:
    bool decode(const std::uint8_t* input, std::size_t inputBytes, int frames);
    bool decodeFirstChannel(const std::uint8_t* input, std::size_t inputBytes, int frames);
    void applyParameterSnapshot();
    static float decodeSample(const std::uint8_t* source, PcmEncoding encoding);
    static void encodeSample(std::uint8_t* destination, PcmEncoding encoding, float value);

    static constexpr int kMaxChannels = 16;

    std::array<std::vector<float>, kMaxChannels> channelBuffers_;
    std::array<float*, kMaxChannels> channelPointers_{};
    FftAnalyzer fft_;
    EqProcessor eq_;
    LoudnessProcessor loudness_;

    std::array<std::atomic<float>, kEqBandCount> pendingEqGainsDb_{};
    std::atomic<bool> pendingEqEnabled_{false};
    std::atomic<bool> pendingLoudnessEnabled_{false};
    std::atomic<float> pendingTargetLufs_{-14.0f};
    std::atomic<std::uint64_t> parameterSequence_{0};
    std::mutex parameterWriteMutex_;
    std::atomic<bool> fftEnabled_{false};
    std::uint64_t appliedParameterSequence_ = ~std::uint64_t{0};

    int sampleRate_ = 44100;
    int channelCount_ = 2;
    PcmEncoding encoding_ = PcmEncoding::Pcm16;
    int maxFrames_ = 0;
    bool configured_ = false;
    bool effectsEnabled_ = false;
};

}  // namespace spica::dsp
