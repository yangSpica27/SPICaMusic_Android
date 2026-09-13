package me.spica27.spicamusic.dsp

import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Native FFT/EQ/loudness engine.
 *
 * The engine accepts and returns the exact same packed PCM format negotiated by
 * Media3. Conversion to float happens only inside native code and is re-encoded
 * at the original bit depth/endianness after processing. If the
 * native library cannot be loaded, [isAvailable] is false and the Media3
 * adapter bypasses processing so the original PCM stream continues unchanged.
 */
@UnstableApi
class NativeDspEngine : Closeable {

    companion object {
        private const val BAND_WAIT_TIMEOUT_MS = 250
        private const val IDLE_RECHECK_INTERVAL_MS = 16L
        private const val DEFAULT_MAX_FRAMES = 32 * 1024
        private const val BAND_COUNT = 31

        /** True when the ABI contains the native DSP shared object. */
        val isNativeLibraryLoaded: Boolean = runCatching {
            System.loadLibrary("spica_native_dsp")
        }.isSuccess
    }

    private val handle = AtomicLong(
        if (isNativeLibraryLoaded) nativeCreate() else 0L,
    )

    private val _bands = MutableStateFlow(FloatArray(BAND_COUNT))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private val _isFftEnabled = MutableStateFlow(false)
    val isFftEnabled: StateFlow<Boolean> = _isFftEnabled.asStateFlow()

    val isAvailable: Boolean
        get() = handle.get() != 0L

    private val stateLock = Any()
    private val eqGains = FloatArray(10)
    private var eqEnabled = false
    private var loudnessEnabled = false
    private var targetLufs = -14f
    @Volatile private var playbackActive = false
    @Volatile private var configured = false
    @Volatile private var lastBandSequence = 0L
    @Volatile private var bandStateGeneration = 0L
    @Volatile private var closing = false
    private var pollBuffer = FloatArray(BAND_COUNT)

    private val bandReader: ExecutorService? = if (isAvailable) {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "Native-DSP-Bands").apply { priority = Thread.NORM_PRIORITY }
        }.also { executor ->
            executor.execute(::readBandsLoop)
        }
    } else {
        null
    }

    /** Configure a new Media3 PCM stream. Must be called before [process]. */
    fun configure(
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        maxFrames: Int = DEFAULT_MAX_FRAMES,
    ): Boolean {
        synchronized(stateLock) {
            val currentHandle = handle.get()
            if (currentHandle == 0L) return false
            bandStateGeneration++
            val success = nativeConfigure(
                currentHandle, sampleRate, channelCount, encoding,
                maxFrames.coerceAtLeast(1),
            )
            configured = success
            lastBandSequence = 0L
            if (success) _bands.value = FloatArray(BAND_COUNT)
            return success
        }
    }

    /** Process one packed PCM block into [output]. Both buffers must be direct. */
    fun process(input: ByteBuffer, output: ByteBuffer, byteCount: Int): Int {
        synchronized(stateLock) {
            val currentHandle = handle.get()
            if (currentHandle == 0L || !configured || byteCount < 0 ||
                input.position() < 0 || output.position() < 0 ||
                input.remaining() < byteCount || output.remaining() < byteCount ||
                !input.isDirect || !output.isDirect
            ) return -1
            return nativeProcess(
                currentHandle, input, output, input.position(), output.position(), byteCount,
            )
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        synchronized(stateLock) { eqEnabled = enabled; publishParameters() }
    }

    fun setEqBandGain(band: Int, gainDb: Float) {
        if (band !in eqGains.indices) return
        synchronized(stateLock) {
            eqGains[band] = gainDb.coerceIn(-12f, 12f)
            publishParameters()
        }
    }

    fun setAllEqBands(gains: FloatArray) {
        if (gains.size != eqGains.size) return
        synchronized(stateLock) {
            for (i in eqGains.indices) eqGains[i] = gains[i].coerceIn(-12f, 12f)
            publishParameters()
        }
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        synchronized(stateLock) { loudnessEnabled = enabled; publishParameters() }
    }

    fun setTargetLufs(target: Float) {
        synchronized(stateLock) {
            targetLufs = target.coerceIn(-40f, 0f)
            publishParameters()
        }
    }

    fun enableFft() {
        _isFftEnabled.value = true
        synchronized(stateLock) {
            handle.get().takeIf { it != 0L }?.let { nativeSetFftEnabled(it, true) }
        }
    }

    fun disableFft() {
        _isFftEnabled.value = false
        synchronized(stateLock) {
            bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeSetFftEnabled(it, false) }
            lastBandSequence = 0L
            _bands.value = FloatArray(BAND_COUNT)
        }
    }

    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        synchronized(stateLock) {
            if (!active) bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeSetPlaybackActive(it, active) }
            if (!active) {
                lastBandSequence = 0L
                _bands.value = FloatArray(BAND_COUNT)
            }
        }
    }

    fun reset() {
        synchronized(stateLock) {
            bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeReset(it) }
            lastBandSequence = 0L
            _bands.value = FloatArray(BAND_COUNT)
        }
    }

    private fun publishParameters() {
        val currentHandle = handle.get()
        if (currentHandle != 0L) {
            nativeSetParameters(
                currentHandle,
                eqGains,
                eqEnabled,
                loudnessEnabled,
                targetLufs,
                _isFftEnabled.value,
            )
        }
    }

    private fun readBandsLoop() {
        while (!closing && !Thread.currentThread().isInterrupted) {
            val currentHandle = handle.get()
            if (currentHandle == 0L) return

            if (!configured || !_isFftEnabled.value || !playbackActive) {
                try {
                    Thread.sleep(IDLE_RECHECK_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
                continue
            }

            val generation = bandStateGeneration
            val observedSequence = lastBandSequence
            val sequence = nativeAwaitBands(
                currentHandle,
                observedSequence,
                pollBuffer,
                BAND_WAIT_TIMEOUT_MS,
            )
            if (sequence == 0L || sequence == observedSequence) continue

            synchronized(stateLock) {
                if (closing || handle.get() != currentHandle ||
                    generation != bandStateGeneration || !configured ||
                    !_isFftEnabled.value || !playbackActive ||
                    sequence == lastBandSequence
                ) return@synchronized

                lastBandSequence = sequence
                // StateFlow consumers may retain the array; publish a copy
                // before the next native wait reuses pollBuffer.
                _bands.value = pollBuffer.copyOf()
            }
        }
    }

    override fun close() {
        closing = true
        val currentHandle =
            synchronized(stateLock) {
                bandStateGeneration++
                configured = false
                handle.getAndSet(0L).also { value ->
                    // reset() inside setFftEnabled wakes a native await so the
                    // reader can observe closing without waiting for timeout.
                    if (value != 0L) nativeSetFftEnabled(value, false)
                }
            }
        bandReader?.shutdownNow()
        bandReader?.awaitTermination(
            BAND_WAIT_TIMEOUT_MS.toLong() + 250L,
            TimeUnit.MILLISECONDS,
        )
        synchronized(stateLock) {
            if (currentHandle != 0L) nativeRelease(currentHandle)
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeConfigure(
        handle: Long, sampleRate: Int, channels: Int, encoding: Int, maxFrames: Int,
    ): Boolean
    private external fun nativeProcess(
        handle: Long, input: ByteBuffer, output: ByteBuffer,
        inputPosition: Int, outputPosition: Int, byteCount: Int,
    ): Int
    private external fun nativeSetParameters(
        handle: Long, gains: FloatArray?, eqEnabled: Boolean,
        loudnessEnabled: Boolean, targetLufs: Float, fftEnabled: Boolean,
    )
    private external fun nativeSetFftEnabled(handle: Long, enabled: Boolean)
    private external fun nativeSetPlaybackActive(handle: Long, active: Boolean)
    private external fun nativeAwaitBands(
        handle: Long,
        afterSequence: Long,
        output: FloatArray,
        timeoutMs: Int,
    ): Long
    private external fun nativeReset(handle: Long)
    @Suppress("unused")
    private external fun nativeResetFft(handle: Long)
    private external fun nativeRelease(handle: Long)
}
