#include "spica/dsp/dsp_engine.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstring>
#include <limits>

namespace spica::dsp {

namespace {

constexpr float kPi = 3.14159265358979323846f;
constexpr float kMinLufs = -70.0f;
constexpr float kLimiterCeiling = 0.98f;
constexpr float kEqBandwidthOctaves = 1.0f;
constexpr float kEqSmoothingSeconds = 0.03f;
constexpr float kAttackSeconds = 0.8f;
constexpr float kReleaseSeconds = 0.2f;
constexpr float kLimiterReleaseSeconds = 0.15f;

inline float clampUnit(float value) {
    if (!std::isfinite(value)) return 0.0f;
    return std::max(-1.0f, std::min(1.0f, value));
}

inline float smoothingAmount(int frames, int sampleRate, float seconds) {
    if (frames <= 0 || sampleRate <= 0) return 1.0f;
    const float samples = std::max(1.0f, seconds * static_cast<float>(sampleRate));
    return 1.0f - std::exp(-static_cast<float>(frames) / samples);
}

}  // namespace

// ----- FftAnalyzer ---------------------------------------------------------

FftAnalyzer::FftAnalyzer() {
    for (int i = 0; i < kFftSize; ++i) {
        hammingWindow_[i] = 0.54f - 0.46f *
            std::cos(2.0f * kPi * static_cast<float>(i) /
                     static_cast<float>(kFftSize - 1));
    }
}

FftAnalyzer::~FftAnalyzer() {
    stop_.store(true, std::memory_order_release);
    if (worker_.joinable()) worker_.join();
    if (setup_ != nullptr) {
        pffft_destroy_setup(setup_);
        setup_ = nullptr;
    }
}

bool FftAnalyzer::configure(int sampleRate) {
    if (sampleRate <= 0) return false;

    // Reconfiguration is performed from Media3's audio thread. Stop the
    // previous worker before replacing the PFFFT setup so no worker can use a
    // destroyed setup during a format change.
    stop_.store(true, std::memory_order_release);
    if (worker_.joinable()) worker_.join();

    decimationFactor_ = std::max(1, (sampleRate + 48000 - 1) / 48000);
    // The ring buffer contains one averaged sample for each decimation
    // window. Keep the effective rate in sync with those samples; using the
    // negotiated 96/192 kHz rate here would shift every FFT peak by 2x/4x.
    analysisSampleRate_ = std::max(1, sampleRate / decimationFactor_);
    decimationAccumulator_ = 0.0f;
    decimationCount_ = 0;

    if (setup_ != nullptr) {
        pffft_destroy_setup(setup_);
        setup_ = nullptr;
    }
    setup_ = pffft_new_setup(kFftSize, PFFFT_REAL);
    if (setup_ == nullptr) return false;

    // The worker is stopped above, so the ring can be reinitialized safely
    // during a format change. Runtime reset() deliberately leaves indices
    // untouched until the worker observes the new generation (see below).
    writeIndex_.store(0, std::memory_order_release);
    readIndex_.store(0, std::memory_order_release);
    reset();
    stop_.store(false, std::memory_order_release);
    if (!worker_.joinable()) {
        worker_ = std::thread(&FftAnalyzer::workerLoop, this);
    }
    return true;
}

void FftAnalyzer::setEnabled(bool enabled) {
    enabled_.store(enabled, std::memory_order_release);
    if (!enabled) reset();
}

void FftAnalyzer::setPlaybackActive(bool active) {
    playbackActive_.store(active, std::memory_order_release);
    if (!active) reset();
}

void FftAnalyzer::push(const float* mono, std::size_t frames) {
    if (mono == nullptr || frames == 0 || setup_ == nullptr ||
        !enabled_.load(std::memory_order_acquire) ||
        !playbackActive_.load(std::memory_order_acquire)) {
        return;
    }

    // reset() may be requested from the UI thread. Keep decimation state
    // audio-thread-owned and clear it lazily when the generation changes.
    const int generation = generation_.load(std::memory_order_acquire);
    if (generation != observedGeneration_) {
        decimationAccumulator_ = 0.0f;
        decimationCount_ = 0;
        observedGeneration_ = generation;
    }

    std::size_t write = writeIndex_.load(std::memory_order_relaxed);
    const std::size_t read = readIndex_.load(std::memory_order_acquire);

    for (std::size_t i = 0; i < frames; ++i) {
        decimationAccumulator_ += mono[i];
        ++decimationCount_;
        if (decimationCount_ < decimationFactor_) continue;

        const float sample = decimationAccumulator_ /
            static_cast<float>(decimationFactor_);
        decimationAccumulator_ = 0.0f;
        decimationCount_ = 0;

        if (write - read >= kRingSize - 1) {
            // FFT is an observation path. Never block the audio thread when
            // the worker is behind; dropping the newest sample bounds latency.
            break;
        }
        ring_[write & kRingMask] = sample;
        ++write;
    }
    writeIndex_.store(write, std::memory_order_release);
}

void FftAnalyzer::reset() {
    generation_.fetch_add(1, std::memory_order_acq_rel);

    std::lock_guard<std::mutex> lock(bandsMutex_);
    const int next = activeBandBuffer_.load(std::memory_order_relaxed) ^ 1;
    bandBuffers_[next].fill(0.0f);
    bandsSequence_.fetch_add(1, std::memory_order_acq_rel);
    activeBandBuffer_.store(next, std::memory_order_release);
    bandsSequence_.fetch_add(1, std::memory_order_release);
}

std::uint64_t FftAnalyzer::readBands(float* out, std::size_t count) const {
    if (out == nullptr || count < kBandCount) return 0;
    std::lock_guard<std::mutex> lock(bandsMutex_);
    const int active = activeBandBuffer_.load(std::memory_order_relaxed);
    std::copy(bandBuffers_[active].begin(), bandBuffers_[active].end(), out);
    return bandsSequence_.load(std::memory_order_relaxed);
}

void FftAnalyzer::workerLoop() {
    while (!stop_.load(std::memory_order_acquire)) {
        const std::size_t write = writeIndex_.load(std::memory_order_acquire);
        std::size_t read = readIndex_.load(std::memory_order_relaxed);
        const int currentGeneration = generation_.load(std::memory_order_acquire);
        if (currentGeneration != workerGeneration_) {
            workerGeneration_ = currentGeneration;
            readIndex_.store(write, std::memory_order_release);
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
            continue;
        }
        if (write - read < kFftSize || setup_ == nullptr ||
            !enabled_.load(std::memory_order_acquire) ||
            !playbackActive_.load(std::memory_order_acquire)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(2));
            continue;
        }

        const int generation = generation_.load(std::memory_order_acquire);
        for (int i = 0; i < kFftSize; ++i) {
            fftInput_[i] = ring_[(read + static_cast<std::size_t>(i)) & kRingMask] *
                           hammingWindow_[i];
        }
        read += kFftSize;
        readIndex_.store(read, std::memory_order_release);

        pffft_transform_ordered(setup_, fftInput_.data(), fftOutput_.data(),
                                fftWork_.data(), PFFFT_FORWARD);

        // A reset may arrive while the worker is transforming an old window.
        // Discard that window and realign the consumer to the current producer
        // position so stale data cannot delay the next generation.
        if (generation != generation_.load(std::memory_order_acquire)) {
            readIndex_.store(writeIndex_.load(std::memory_order_acquire),
                             std::memory_order_release);
            continue;
        }

        for (int i = 0; i < kFftSize / 2; ++i) {
            float real;
            float imag;
            if (i == 0) {
                real = fftOutput_[0];
                imag = 0.0f;
            } else {
                real = fftOutput_[2 * i];
                imag = fftOutput_[2 * i + 1];
            }
            magnitudes_[i] = std::sqrt(real * real + imag * imag);
        }

        std::array<float, kBandCount> values{};
        mapToBands(magnitudes_.data(), values.data());
        if (generation == generation_.load(std::memory_order_acquire) &&
            enabled_.load(std::memory_order_acquire) &&
            playbackActive_.load(std::memory_order_acquire)) {
            publishBands(values.data(), generation);
        }
    }
}

void FftAnalyzer::publishBands(const float* values, int generation) {
    std::lock_guard<std::mutex> lock(bandsMutex_);
    if (generation != generation_.load(std::memory_order_acquire) ||
        !enabled_.load(std::memory_order_acquire) ||
        !playbackActive_.load(std::memory_order_acquire)) {
        return;
    }
    bandsSequence_.fetch_add(1, std::memory_order_acq_rel);
    const int next = activeBandBuffer_.load(std::memory_order_relaxed) ^ 1;
    std::copy(values, values + kBandCount, bandBuffers_[next].begin());
    activeBandBuffer_.store(next, std::memory_order_release);
    bandsSequence_.fetch_add(1, std::memory_order_release);
}

void FftAnalyzer::mapToBands(const float* magnitudes, float* result) const {
    static constexpr std::array<float, kBandCount> frequencies = {
        20.0f, 25.0f, 32.0f, 40.0f, 50.0f, 63.0f, 80.0f, 100.0f,
        125.0f, 160.0f, 200.0f, 250.0f, 315.0f, 400.0f, 500.0f,
        630.0f, 800.0f, 1000.0f, 1250.0f, 1600.0f, 2000.0f,
        2500.0f, 3150.0f, 4000.0f, 5000.0f, 6300.0f, 8000.0f,
        10000.0f, 12500.0f, 16000.0f, 20000.0f,
    };

    const float frequencyResolution =
        static_cast<float>(analysisSampleRate_) / kFftSize;
    const float nyquist = static_cast<float>(analysisSampleRate_) / 2.0f;
    for (int band = 0; band < kBandCount; ++band) {
        const float center = frequencies[band];
        const float low = band == 0 ? 16.0f :
            std::sqrt(center * frequencies[band - 1]);
        const float high = band == kBandCount - 1 ? 22000.0f :
            std::sqrt(center * frequencies[band + 1]);

        if (low >= nyquist) {
            result[band] = 0.0f;
            continue;
        }
        const int lowBin = std::max(1, static_cast<int>(low / frequencyResolution));
        const int highBin = std::max(lowBin, std::min(
            kFftSize / 2 - 1,
            static_cast<int>(std::min(high, nyquist) / frequencyResolution)));

        float sum = 0.0f;
        int count = 0;
        for (int bin = lowBin; bin <= highBin; ++bin) {
            sum += magnitudes[bin];
            ++count;
        }
        const float avg = count > 0 ? sum / static_cast<float>(count) : 0.0f;
        const float db = avg > 0.0f ? 20.0f * std::log10(avg) : 0.0f;
        result[band] = std::max(0.0f, std::min(1.0f, db / 60.0f));
    }
}

// ----- EqProcessor ---------------------------------------------------------

constexpr std::array<float, EqProcessor::kBandCount> EqProcessor::kFrequencies;

bool EqProcessor::configure(int sampleRate, int channelCount, int maxFrames) {
    if (sampleRate <= 0 || channelCount <= 0 ||
        channelCount > kMaxChannels || maxFrames <= 0) {
        configured_ = false;
        return false;
    }

    sampleRate_ = sampleRate;
    channelCount_ = channelCount;
    maxFrames_ = maxFrames;
    configured_ = true;

    for (int band = 0; band < kBandCount; ++band) {
        usableBands_[band] = kFrequencies[band] < sampleRate_ * 0.48f;
        for (int channel = 0; channel < channelCount_; ++channel) {
            const float frequency = std::min(
                kFrequencies[band], sampleRate_ * 0.45f);
            filters_[band][channel].setup(
                sampleRate_, frequency, currentGains_[band], kEqBandwidthOctaves);
            filters_[band][channel].reset();
        }
    }
    return true;
}

void EqProcessor::setEnabled(bool enabled) {
    enabled_ = enabled;
}

void EqProcessor::setTargetGains(const float* gains, std::size_t count) {
    for (int i = 0; i < kBandCount; ++i) {
        const float value = gains != nullptr && static_cast<std::size_t>(i) < count
            ? gains[i]
            : 0.0f;
        targetGains_[i] = std::max(-12.0f, std::min(12.0f, value));
    }
}

void EqProcessor::updateCoefficients(int frames) {
    const float amount = smoothingAmount(frames, sampleRate_, kEqSmoothingSeconds);
    for (int band = 0; band < kBandCount; ++band) {
        const float diff = targetGains_[band] - currentGains_[band];
        if (std::abs(diff) < 0.0001f) {
            currentGains_[band] = targetGains_[band];
        } else {
            currentGains_[band] += diff * amount;
        }

        if (!usableBands_[band]) continue;
        const float frequency = std::min(
            kFrequencies[band], sampleRate_ * 0.45f);
        for (int channel = 0; channel < channelCount_; ++channel) {
            filters_[band][channel].setup(
                sampleRate_, frequency, currentGains_[band], kEqBandwidthOctaves);
        }
    }
}

void EqProcessor::process(float* const* channels, int frames) {
    if (!configured_ || !enabled_ || channels == nullptr || frames <= 0) return;
    updateCoefficients(frames);

    for (int band = 0; band < kBandCount; ++band) {
        if (!usableBands_[band] || std::abs(currentGains_[band]) < 0.0001f) continue;
        for (int channel = 0; channel < channelCount_; ++channel) {
            float* channelArray[1] = {channels[channel]};
            filters_[band][channel].process(frames, channelArray);
        }
    }
}

void EqProcessor::reset() {
    for (int band = 0; band < kBandCount; ++band) {
        for (int channel = 0; channel < channelCount_; ++channel) {
            filters_[band][channel].reset();
        }
    }
}

// ----- LoudnessProcessor ---------------------------------------------------

bool LoudnessProcessor::configure(int sampleRate, int channelCount, int maxFrames) {
    if (sampleRate <= 0 || channelCount <= 0 || channelCount > 64 || maxFrames <= 0) {
        return false;
    }
    destroyMeter();
    sampleRate_ = sampleRate;
    channelCount_ = channelCount;
    maxFrames_ = maxFrames;
    interleaved_.assign(static_cast<std::size_t>(maxFrames_) * channelCount_, 0.0f);
    currentGain_ = 1.0f;
    limiterGain_ = 1.0f;
    return createMeter();
}

void LoudnessProcessor::destroyMeter() {
    if (meter_ != nullptr) {
        ebur128_destroy(&meter_);
        meter_ = nullptr;
    }
}

bool LoudnessProcessor::createMeter() {
    meter_ = ebur128_init(
        static_cast<unsigned int>(channelCount_),
        static_cast<unsigned long>(sampleRate_),
        EBUR128_MODE_M | EBUR128_MODE_S | EBUR128_MODE_SAMPLE_PEAK);
    if (meter_ == nullptr) return false;
    // libebur128 supplies the standard channel map (including LFE exclusion)
    // for mono/stereo/surround layouts. Keep it intact so 4/5-channel music
    // contributes surround energy instead of silently reducing to L/R only.
    return true;
}

void LoudnessProcessor::setEnabled(bool enabled) {
    if (enabled_ == enabled) return;
    enabled_ = enabled;
    if (!enabled_) {
        currentGain_ = 1.0f;
        limiterGain_ = 1.0f;
    } else {
        reset();
    }
}

void LoudnessProcessor::setTargetLufs(float targetLufs) {
    targetLufs_ = std::max(-40.0f, std::min(0.0f, targetLufs));
}

void LoudnessProcessor::process(float* const* channels, int frames) {
    if (!enabled_ || meter_ == nullptr || channels == nullptr || frames <= 0 ||
        frames > maxFrames_) return;

    for (int frame = 0; frame < frames; ++frame) {
        for (int channel = 0; channel < channelCount_; ++channel) {
            interleaved_[static_cast<std::size_t>(frame) * channelCount_ + channel] =
                channels[channel][frame];
        }
    }
    if (ebur128_add_frames_float(meter_, interleaved_.data(),
                                 static_cast<std::size_t>(frames)) != EBUR128_SUCCESS) {
        return;
    }

    double loudness = -std::numeric_limits<double>::infinity();
    ebur128_loudness_momentary(meter_, &loudness);

    float targetGain = currentGain_;
    if (std::isfinite(loudness) && loudness > kMinLufs) {
        const double gainDb = static_cast<double>(targetLufs_) - loudness;
        targetGain = static_cast<float>(std::pow(10.0, gainDb / 20.0));
        targetGain = std::max(0.25f, std::min(4.0f, targetGain));
    }

    const float smoothingSeconds = targetGain > currentGain_
        ? kAttackSeconds : kReleaseSeconds;
    const float blockAmount = smoothingAmount(frames, sampleRate_, smoothingSeconds);
    const float nextGain = currentGain_ + (targetGain - currentGain_) * blockAmount;
    const float limiterRelease = smoothingAmount(1, sampleRate_, kLimiterReleaseSeconds);

    for (int frame = 0; frame < frames; ++frame) {
        const float t = frames > 1
            ? static_cast<float>(frame) / static_cast<float>(frames - 1)
            : 1.0f;
        const float gain = currentGain_ + (nextGain - currentGain_) * t;
        float peak = 0.0f;
        for (int channel = 0; channel < channelCount_; ++channel) {
            peak = std::max(peak, std::abs(channels[channel][frame] * gain));
        }

        const float desiredLimiter = peak > kLimiterCeiling
            ? kLimiterCeiling / peak
            : 1.0f;
        if (desiredLimiter < limiterGain_) {
            limiterGain_ = desiredLimiter;
        } else {
            limiterGain_ += (1.0f - limiterGain_) * limiterRelease;
        }

        for (int channel = 0; channel < channelCount_; ++channel) {
            channels[channel][frame] = std::max(
                -1.0f, std::min(1.0f,
                    channels[channel][frame] * gain * limiterGain_));
        }
    }
    currentGain_ = nextGain;
}

void LoudnessProcessor::reset() {
    currentGain_ = 1.0f;
    limiterGain_ = 1.0f;
    if (meter_ != nullptr) {
        destroyMeter();
        createMeter();
    }
}

// ----- DspEngine -----------------------------------------------------------

DspEngine::DspEngine() {
    for (auto& gain : pendingEqGainsDb_) {
        gain.store(0.0f, std::memory_order_relaxed);
    }
}

DspEngine::~DspEngine() = default;

int DspEngine::bytesPerSample() const {
    switch (encoding_) {
        case PcmEncoding::Pcm8: return 1;
        case PcmEncoding::Pcm16:
        case PcmEncoding::Pcm16BigEndian: return 2;
        case PcmEncoding::Pcm24:
        case PcmEncoding::Pcm24BigEndian: return 3;
        case PcmEncoding::Pcm32:
        case PcmEncoding::Pcm32BigEndian:
        case PcmEncoding::PcmFloat: return 4;
    }
    return 0;
}

bool DspEngine::configure(int sampleRate, int channelCount,
                          PcmEncoding encoding, int maxFrames) {
    if (sampleRate <= 0 || channelCount <= 0 ||
        channelCount > kMaxChannels || maxFrames <= 0) {
        configured_ = false;
        return false;
    }

    sampleRate_ = sampleRate;
    channelCount_ = channelCount;
    encoding_ = encoding;
    maxFrames_ = maxFrames;
    if (bytesPerSample() == 0) {
        configured_ = false;
        return false;
    }

    for (int channel = 0; channel < channelCount_; ++channel) {
        channelBuffers_[channel].assign(maxFrames_, 0.0f);
        channelPointers_[channel] = channelBuffers_[channel].data();
    }
    fftMonoBuffer_.assign(maxFrames_, 0.0f);
    if (!fft_.configure(sampleRate_) ||
        !eq_.configure(sampleRate_, channelCount_, maxFrames_) ||
        !loudness_.configure(sampleRate_, channelCount_, maxFrames_)) {
        configured_ = false;
        return false;
    }
    configured_ = true;
    appliedParameterSequence_ = ~std::uint64_t{0};
    applyParameterSnapshot();
    return true;
}

void DspEngine::setParameters(const DspParameters& parameters) {
    std::lock_guard<std::mutex> lock(parameterWriteMutex_);
    // Odd sequence means a writer is publishing. The audio thread only
    // consumes a snapshot when the same even sequence surrounds all loads,
    // avoiding a mutex/priority inversion on the realtime path.
    parameterSequence_.fetch_add(1, std::memory_order_acq_rel);
    pendingEqEnabled_.store(parameters.eqEnabled, std::memory_order_relaxed);
    pendingLoudnessEnabled_.store(parameters.loudnessEnabled,
                                  std::memory_order_relaxed);
    const float targetLufs = std::isfinite(parameters.targetLufs)
        ? std::max(-40.0f, std::min(0.0f, parameters.targetLufs))
        : -14.0f;
    pendingTargetLufs_.store(targetLufs, std::memory_order_relaxed);
    for (int band = 0; band < kEqBandCount; ++band) {
        const float requested = parameters.eqGainsDb[band];
        const float gain = std::isfinite(requested)
            ? std::max(-12.0f, std::min(12.0f, requested))
            : 0.0f;
        pendingEqGainsDb_[band].store(gain, std::memory_order_relaxed);
    }
    parameterSequence_.fetch_add(1, std::memory_order_release);
    fftEnabled_.store(parameters.fftEnabled, std::memory_order_release);
    fft_.setEnabled(parameters.fftEnabled);
}

void DspEngine::setFftEnabled(bool enabled) {
    fftEnabled_.store(enabled, std::memory_order_release);
    fft_.setEnabled(enabled);
}

void DspEngine::setPlaybackActive(bool active) {
    fft_.setPlaybackActive(active);
}

std::uint64_t DspEngine::readBands(float* out, std::size_t count) const {
    return fft_.readBands(out, count);
}

void DspEngine::applyParameterSnapshot() {
    const std::uint64_t sequenceBefore =
        parameterSequence_.load(std::memory_order_acquire);
    if ((sequenceBefore & 1u) != 0u || sequenceBefore == appliedParameterSequence_) return;

    DspParameters parameters;
    parameters.eqEnabled = pendingEqEnabled_.load(std::memory_order_relaxed);
    parameters.loudnessEnabled =
        pendingLoudnessEnabled_.load(std::memory_order_relaxed);
    parameters.targetLufs = pendingTargetLufs_.load(std::memory_order_relaxed);
    for (int band = 0; band < kEqBandCount; ++band) {
        parameters.eqGainsDb[band] =
            pendingEqGainsDb_[band].load(std::memory_order_relaxed);
    }

    const std::uint64_t sequenceAfter =
        parameterSequence_.load(std::memory_order_acquire);
    if (sequenceBefore != sequenceAfter || (sequenceAfter & 1u) != 0u) return;

    eq_.setEnabled(parameters.eqEnabled);
    eq_.setTargetGains(parameters.eqGainsDb.data(), parameters.eqGainsDb.size());
    loudness_.setEnabled(parameters.loudnessEnabled);
    loudness_.setTargetLufs(parameters.targetLufs);
    effectsEnabled_ = parameters.eqEnabled || parameters.loudnessEnabled;
    appliedParameterSequence_ = sequenceAfter;
}

float DspEngine::decodeSample(const std::uint8_t* source, PcmEncoding encoding) {
    switch (encoding) {
        case PcmEncoding::Pcm8:
            return (static_cast<int>(*source) - 128) / 128.0f;
        case PcmEncoding::Pcm16: {
            const std::int16_t value = static_cast<std::int16_t>(
                static_cast<std::uint16_t>(source[0]) |
                (static_cast<std::uint16_t>(source[1]) << 8));
            return static_cast<float>(value) / 32768.0f;
        }
        case PcmEncoding::Pcm16BigEndian: {
            const std::int16_t value = static_cast<std::int16_t>(
                (static_cast<std::uint16_t>(source[0]) << 8) |
                static_cast<std::uint16_t>(source[1]));
            return static_cast<float>(value) / 32768.0f;
        }
        case PcmEncoding::Pcm24: {
            std::int32_t value = static_cast<std::int32_t>(source[0]) |
                (static_cast<std::int32_t>(source[1]) << 8) |
                (static_cast<std::int32_t>(source[2]) << 16);
            if (value & 0x800000) value |= ~0xFFFFFF;
            return static_cast<float>(value) / 8388608.0f;
        }
        case PcmEncoding::Pcm24BigEndian: {
            std::int32_t value = (static_cast<std::int32_t>(source[0]) << 16) |
                (static_cast<std::int32_t>(source[1]) << 8) |
                static_cast<std::int32_t>(source[2]);
            if (value & 0x800000) value |= ~0xFFFFFF;
            return static_cast<float>(value) / 8388608.0f;
        }
        case PcmEncoding::Pcm32: {
            const std::uint32_t bits = static_cast<std::uint32_t>(source[0]) |
                (static_cast<std::uint32_t>(source[1]) << 8) |
                (static_cast<std::uint32_t>(source[2]) << 16) |
                (static_cast<std::uint32_t>(source[3]) << 24);
            return static_cast<float>(static_cast<std::int32_t>(bits)) / 2147483648.0f;
        }
        case PcmEncoding::Pcm32BigEndian: {
            const std::uint32_t bits = (static_cast<std::uint32_t>(source[0]) << 24) |
                (static_cast<std::uint32_t>(source[1]) << 16) |
                (static_cast<std::uint32_t>(source[2]) << 8) |
                static_cast<std::uint32_t>(source[3]);
            return static_cast<float>(static_cast<std::int32_t>(bits)) / 2147483648.0f;
        }
        case PcmEncoding::PcmFloat: {
            std::uint32_t bits = static_cast<std::uint32_t>(source[0]) |
                (static_cast<std::uint32_t>(source[1]) << 8) |
                (static_cast<std::uint32_t>(source[2]) << 16) |
                (static_cast<std::uint32_t>(source[3]) << 24);
            float value;
            std::memcpy(&value, &bits, sizeof(value));
            return clampUnit(value);
        }
    }
    return 0.0f;
}

void DspEngine::encodeSample(std::uint8_t* destination,
                             PcmEncoding encoding, float value) {
    value = clampUnit(value);
    switch (encoding) {
        case PcmEncoding::Pcm8: {
            const int sample = std::max(0, std::min(255,
                static_cast<int>(std::lround(value * 128.0f)) + 128));
            destination[0] = static_cast<std::uint8_t>(sample);
            break;
        }
        case PcmEncoding::Pcm16:
        case PcmEncoding::Pcm16BigEndian: {
            const auto sample = static_cast<std::int16_t>(std::max(
                -32768L, std::min(32767L, std::lround(value * 32768.0f))));
            const auto bits = static_cast<std::uint16_t>(sample);
            if (encoding == PcmEncoding::Pcm16) {
                destination[0] = static_cast<std::uint8_t>(bits);
                destination[1] = static_cast<std::uint8_t>(bits >> 8);
            } else {
                destination[0] = static_cast<std::uint8_t>(bits >> 8);
                destination[1] = static_cast<std::uint8_t>(bits);
            }
            break;
        }
        case PcmEncoding::Pcm24:
        case PcmEncoding::Pcm24BigEndian: {
            const auto sample = static_cast<std::int32_t>(std::max(
                -8388608L, std::min(8388607L, std::lround(value * 8388608.0f))));
            const auto bits = static_cast<std::uint32_t>(sample);
            if (encoding == PcmEncoding::Pcm24) {
                destination[0] = static_cast<std::uint8_t>(bits);
                destination[1] = static_cast<std::uint8_t>(bits >> 8);
                destination[2] = static_cast<std::uint8_t>(bits >> 16);
            } else {
                destination[0] = static_cast<std::uint8_t>(bits >> 16);
                destination[1] = static_cast<std::uint8_t>(bits >> 8);
                destination[2] = static_cast<std::uint8_t>(bits);
            }
            break;
        }
        case PcmEncoding::Pcm32:
        case PcmEncoding::Pcm32BigEndian: {
            const auto sample = static_cast<std::int32_t>(std::max<std::int64_t>(
                -2147483648LL, std::min<std::int64_t>(2147483647LL,
                                                       std::llround(value * 2147483648.0))));
            const auto bits = static_cast<std::uint32_t>(sample);
            if (encoding == PcmEncoding::Pcm32) {
                destination[0] = static_cast<std::uint8_t>(bits);
                destination[1] = static_cast<std::uint8_t>(bits >> 8);
                destination[2] = static_cast<std::uint8_t>(bits >> 16);
                destination[3] = static_cast<std::uint8_t>(bits >> 24);
            } else {
                destination[0] = static_cast<std::uint8_t>(bits >> 24);
                destination[1] = static_cast<std::uint8_t>(bits >> 16);
                destination[2] = static_cast<std::uint8_t>(bits >> 8);
                destination[3] = static_cast<std::uint8_t>(bits);
            }
            break;
        }
        case PcmEncoding::PcmFloat: {
            std::memcpy(destination, &value, sizeof(value));
            break;
        }
    }
}

bool DspEngine::decode(const std::uint8_t* input, std::size_t inputBytes, int frames) {
    const int sampleBytes = bytesPerSample();
    const std::size_t frameBytes = static_cast<std::size_t>(sampleBytes) * channelCount_;
    if (input == nullptr || frameBytes == 0 || frames < 0 ||
        static_cast<std::size_t>(frames) * frameBytes > inputBytes) return false;

    for (int frame = 0; frame < frames; ++frame) {
        const std::uint8_t* source = input + static_cast<std::size_t>(frame) * frameBytes;
        for (int channel = 0; channel < channelCount_; ++channel) {
            channelBuffers_[channel][frame] = decodeSample(
                source + static_cast<std::size_t>(channel) * sampleBytes, encoding_);
        }
    }
    return true;
}

void DspEngine::pushFftAnalysis(int frames) {
    if (frames <= 0 || frames > maxFrames_ || channelCount_ <= 0) return;

    // FFT is an observation path. Mix every interleaved channel into a
    // mono analysis buffer so a silent/quiet first channel cannot hide the
    // spectrum of a Hi-Res multichannel recording. The original channel
    // buffers remain untouched for EQ/loudness and output encoding.
    const float scale = 1.0f / static_cast<float>(channelCount_);
    for (int frame = 0; frame < frames; ++frame) {
        float sum = 0.0f;
        for (int channel = 0; channel < channelCount_; ++channel) {
            sum += channelBuffers_[channel][frame];
        }
        fftMonoBuffer_[frame] = sum * scale;
    }
    fft_.push(fftMonoBuffer_.data(), static_cast<std::size_t>(frames));
}

int DspEngine::process(const std::uint8_t* input, std::size_t inputBytes,
                       std::uint8_t* output, std::size_t outputCapacity) {
    if (!configured_ || input == nullptr || output == nullptr) return -1;
    if (outputCapacity < inputBytes) return -2;

    const int sampleBytes = bytesPerSample();
    const std::size_t frameBytes = static_cast<std::size_t>(sampleBytes) * channelCount_;
    if (frameBytes == 0) return -3;
    const int frames = static_cast<int>(inputBytes / frameBytes);
    if (frames > maxFrames_) return -4;

    applyParameterSnapshot();
    const bool fftEnabled = fftEnabled_.load(std::memory_order_acquire);
    const bool anyEffects = effectsEnabled_;

    if (!fftEnabled && !anyEffects) {
        std::memcpy(output, input, inputBytes);
        return static_cast<int>(inputBytes);
    }

    if (!anyEffects) {
        if (fftEnabled) {
            if (!decode(input, inputBytes, frames)) return -5;
            pushFftAnalysis(frames);
        }
        std::memcpy(output, input, inputBytes);
        return static_cast<int>(inputBytes);
    }

    if (!decode(input, inputBytes, frames)) return -5;

    if (fftEnabled) {
        pushFftAnalysis(frames);
    }

    if (eq_.isConfigured()) {
        eq_.process(channelPointers_.data(), frames);
    }
    if (loudness_.isConfigured()) {
        loudness_.process(channelPointers_.data(), frames);
    }

    for (int frame = 0; frame < frames; ++frame) {
        std::uint8_t* destination = output + static_cast<std::size_t>(frame) * frameBytes;
        for (int channel = 0; channel < channelCount_; ++channel) {
            encodeSample(destination + static_cast<std::size_t>(channel) * sampleBytes,
                         encoding_, channelBuffers_[channel][frame]);
        }
    }
    const std::size_t processedBytes = static_cast<std::size_t>(frames) * frameBytes;
    if (processedBytes < inputBytes) {
        std::memcpy(output + processedBytes, input + processedBytes,
                    inputBytes - processedBytes);
    }
    return static_cast<int>(inputBytes);
}

void DspEngine::reset() {
    fft_.reset();
    eq_.reset();
    loudness_.reset();
}

}  // namespace spica::dsp
