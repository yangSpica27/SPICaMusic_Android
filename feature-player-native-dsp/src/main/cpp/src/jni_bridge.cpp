#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <new>

#include "spica/dsp/dsp_engine.h"

namespace {

using spica::dsp::DspEngine;
using spica::dsp::DspParameters;
using spica::dsp::PcmEncoding;

// Media3 C.ENCODING_PCM_* values. They intentionally do not match the
// internal enum values: the bridge is the single place where platform
// encoding constants are translated, keeping the native core platform-free.
PcmEncoding toPcmEncoding(jint encoding) {
    switch (encoding) {
        case 3:          return PcmEncoding::Pcm8;
        case 2:          return PcmEncoding::Pcm16;
        case 268435456:  return PcmEncoding::Pcm16BigEndian;
        case 21:         return PcmEncoding::Pcm24;
        case 1342177280: return PcmEncoding::Pcm24BigEndian;
        case 22:         return PcmEncoding::Pcm32;
        case 1610612736: return PcmEncoding::Pcm32BigEndian;
        case 4:          return PcmEncoding::PcmFloat;
        default:         return static_cast<PcmEncoding>(0);
    }
}
DspEngine* engine(jlong handle) {
    return reinterpret_cast<DspEngine*>(static_cast<std::uintptr_t>(handle));
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeCreate(
    JNIEnv*, jobject) {
    auto* value = new (std::nothrow) DspEngine();
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(value));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeConfigure(
    JNIEnv*, jobject, jlong handle, jint sampleRate, jint channels,
    jint encoding, jint maxFrames) {
    auto* value = engine(handle);
    if (value == nullptr) return JNI_FALSE;
    const auto pcmEncoding = toPcmEncoding(encoding);
    if (static_cast<int>(pcmEncoding) == 0) return JNI_FALSE;
    return value->configure(sampleRate, channels, pcmEncoding, maxFrames)
        ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeProcess(
    JNIEnv* env, jobject, jlong handle, jobject inputBuffer, jobject outputBuffer,
    jint inputPosition, jint outputPosition, jint byteCount) {
    auto* value = engine(handle);
    if (value == nullptr || inputBuffer == nullptr || outputBuffer == nullptr ||
        inputPosition < 0 || outputPosition < 0 || byteCount < 0) {
        return -1;
    }
    auto* input = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(inputBuffer));
    auto* output = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(outputBuffer));
    const jlong inputCapacity = env->GetDirectBufferCapacity(inputBuffer);
    const jlong outputCapacity = env->GetDirectBufferCapacity(outputBuffer);
    if (input == nullptr || output == nullptr || inputPosition > inputCapacity ||
        outputPosition > outputCapacity || byteCount > inputCapacity - inputPosition ||
        byteCount > outputCapacity - outputPosition) {
        return -2;
    }
    return value->process(input + inputPosition, static_cast<std::size_t>(byteCount),
                          output + outputPosition,
                          static_cast<std::size_t>(outputCapacity - outputPosition));
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetParameters(
    JNIEnv* env, jobject, jlong handle, jfloatArray gains, jboolean eqEnabled,
    jboolean loudnessEnabled, jfloat targetLufs, jboolean fftEnabled) {
    auto* value = engine(handle);
    if (value == nullptr) return;

    DspParameters parameters;
    parameters.eqEnabled = eqEnabled == JNI_TRUE;
    parameters.loudnessEnabled = loudnessEnabled == JNI_TRUE;
    parameters.fftEnabled = fftEnabled == JNI_TRUE;
    parameters.targetLufs = targetLufs;
    if (gains != nullptr) {
        const jsize count = std::min<jsize>(env->GetArrayLength(gains),
                                            static_cast<jsize>(parameters.eqGainsDb.size()));
        env->GetFloatArrayRegion(gains, 0, count, parameters.eqGainsDb.data());
    }
    value->setParameters(parameters);
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetFftEnabled(
    JNIEnv*, jobject, jlong handle, jboolean enabled) {
    if (auto* value = engine(handle)) value->setFftEnabled(enabled == JNI_TRUE);
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetPlaybackActive(
    JNIEnv*, jobject, jlong handle, jboolean active) {
    if (auto* value = engine(handle)) value->setPlaybackActive(active == JNI_TRUE);
}

extern "C" JNIEXPORT jlong JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeReadBands(
    JNIEnv* env, jobject, jlong handle, jfloatArray output) {
    auto* value = engine(handle);
    if (value == nullptr || output == nullptr ||
        env->GetArrayLength(output) < DspEngine::kBandCount) return 0;
    jfloat bands[DspEngine::kBandCount]{};
    const std::uint64_t sequence = value->readBands(bands, DspEngine::kBandCount);
    env->SetFloatArrayRegion(output, 0, DspEngine::kBandCount, bands);
    return static_cast<jlong>(sequence);
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeReset(
    JNIEnv*, jobject, jlong handle) {
    if (auto* value = engine(handle)) value->reset();
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeResetFft(
    JNIEnv*, jobject, jlong handle) {
    // reset() currently resets all state. Keeping a separate bridge method
    // leaves room for an FFT-only reset without changing the Kotlin API.
    if (auto* value = engine(handle)) value->reset();
}

extern "C" JNIEXPORT void JNICALL
Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeRelease(
    JNIEnv*, jobject, jlong handle) {
    delete engine(handle);
}
