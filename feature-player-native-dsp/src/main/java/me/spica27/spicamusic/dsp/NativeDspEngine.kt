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
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 基于 Rust/OxiMedia 的 FFT、EQ 和响度处理引擎。
 *
 * 输入和输出保持 Media3 协商的交错 PCM 格式。仅在原生代码内部转换为 f64，
 * 处理后按原位深和大小端顺序重新编码。动态库加载失败时，[isAvailable] 为 false，
 * 上层 Media3 适配器直接旁路，继续传递原始 PCM 数据。
 */
@UnstableApi
class NativeDspEngine : Closeable, PcmDspProcessor {

    companion object {
        private const val BAND_WAIT_TIMEOUT_MS = 250
        private const val BAND_COUNT = 31

        /** 当前 ABI 对应的 DSP 动态库加载成功时为 true。 */
        val isNativeLibraryLoaded: Boolean = runCatching {
            System.loadLibrary("spica_native_dsp")
        }.isSuccess
    }

    private val handle = AtomicLong(
        if (isNativeLibraryLoaded) nativeCreate() else 0L,
    )

    private val _bands = MutableStateFlow(FloatArray(BAND_COUNT))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private val _spectrumGeneration = MutableStateFlow(0L)
    /** 频谱片段的代次，绘制层据此替换 seek 或切歌前的分析目标。 */
    val spectrumGeneration: StateFlow<Long> = _spectrumGeneration.asStateFlow()

    private val _isFftEnabled = MutableStateFlow(false)
    val isFftEnabled: StateFlow<Boolean> = _isFftEnabled.asStateFlow()

    override val isAvailable: Boolean
        get() = handle.get() != 0L

    private val stateLock = Any()
    // 音频处理和 UI 参数发布可以并发进行。
    // 释放资源时独占写锁，等待所有 JNI 读操作结束后再销毁句柄。
    private val nativeLifecycle = ReentrantReadWriteLock()
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

    // 必须声明在 bandReader 之前，否则其初始化会覆盖线程工厂写入的值。
    @Volatile private var bandReaderThread: Thread? = null
    private val bandReader: ExecutorService? = if (isAvailable) {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "Native-DSP-Bands").apply {
                priority = Thread.NORM_PRIORITY
                bandReaderThread = this
            }
        }.also { executor ->
            executor.execute(::readBandsLoop)
        }
    } else {
        null
    }

    /** 配置新的 Media3 PCM 音频流，必须在调用 [process] 前完成。 */
    override fun configure(
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        maxFrames: Int,
        channelMask: Int,
    ): Boolean {
        // 重建原生引擎会停止旧 FFT 线程并分配缓冲区，放在 stateLock 之外，
        // 避免主线程的 EQ 和频谱操作在切歌时被阻塞；生命周期读锁保证句柄不被释放。
        val success = nativeLifecycle.read {
            val currentHandle = handle.get()
            currentHandle != 0L && nativeConfigure(
                currentHandle, sampleRate, channelCount, encoding,
                maxFrames.coerceAtLeast(1),
                channelMask,
            )
        }
        synchronized(stateLock) {
            bandStateGeneration++
            configured = success && handle.get() != 0L
            clearSpectrumLocked()
            if (configured) wakeBandReader()
            return configured
        }
    }

    /** 将一块交错 PCM 数据处理后写入 [output]，输入和输出都必须是直接缓冲区。 */
    override fun process(input: ByteBuffer, output: ByteBuffer, byteCount: Int): Int {
        nativeLifecycle.read {
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
        wakeBandReader()
    }

    fun disableFft() {
        _isFftEnabled.value = false
        synchronized(stateLock) {
            bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeSetFftEnabled(it, false) }
            clearSpectrumLocked()
        }
    }

    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        synchronized(stateLock) {
            if (!active) bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeSetPlaybackActive(it, active) }
            if (!active) {
                clearSpectrumLocked()
            }
        }
        if (active) wakeBandReader()
    }

    override fun reset() {
        synchronized(stateLock) {
            bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeReset(it) }
            clearSpectrumLocked()
        }
    }

    /** 只清理频谱，播放器事件不能在音频线程外重置 EQ 或响度状态。 */
    fun resetFft() {
        synchronized(stateLock) {
            bandStateGeneration++
            handle.get().takeIf { it != 0L }?.let { nativeResetFft(it) }
            clearSpectrumLocked()
        }
    }

    /** 先发布空频谱，再发布代次；消费者读取新代次时不会看到旧片段数据。 */
    private fun clearSpectrumLocked() {
        lastBandSequence = 0L
        _bands.value = FloatArray(BAND_COUNT)
        _spectrumGeneration.value = bandStateGeneration
    }

    private fun wakeBandReader() {
        bandReaderThread?.let(LockSupport::unpark)
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
                // 空闲时不轮询：configure、enableFft、setPlaybackActive(true) 会 unpark，
                // close 的 shutdownNow 会中断 park。先到的 unpark 留有许可，不会丢失。
                LockSupport.park(this)
                continue
            }

            val generation = bandStateGeneration
            val observedSequence = lastBandSequence
            val sequence = nativeLifecycle.read {
                if (handle.get() != currentHandle) {
                    0L
                } else {
                    nativeAwaitBands(
                        currentHandle,
                        observedSequence,
                        pollBuffer,
                        BAND_WAIT_TIMEOUT_MS,
                    )
                }
            }
            if (sequence == 0L || sequence == observedSequence) continue

            synchronized(stateLock) {
                if (closing || handle.get() != currentHandle ||
                    generation != bandStateGeneration || !configured ||
                    !_isFftEnabled.value || !playbackActive ||
                    sequence == lastBandSequence
                ) return@synchronized

                lastBandSequence = sequence
                // StateFlow 消费方可能持有数组引用，因此发布副本，
                // 避免下一次原生等待复用 pollBuffer 时修改已发布的数据。
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
                clearSpectrumLocked()
                handle.getAndSet(0L).also { value ->
                    // 关闭 FFT 会重置并唤醒原生等待，
                    // 使读取线程立即感知关闭状态，无需等到超时。
                    if (value != 0L) nativeSetFftEnabled(value, false)
                }
            }
        bandReader?.shutdownNow()
        bandReader?.awaitTermination(
            BAND_WAIT_TIMEOUT_MS.toLong() + 250L,
            TimeUnit.MILLISECONDS,
        )
        nativeLifecycle.write {
            if (currentHandle != 0L) nativeRelease(currentHandle)
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeConfigure(
        handle: Long, sampleRate: Int, channels: Int, encoding: Int, maxFrames: Int, channelMask: Int,
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
    private external fun nativeResetFft(handle: Long)
    private external fun nativeRelease(handle: Long)
}
