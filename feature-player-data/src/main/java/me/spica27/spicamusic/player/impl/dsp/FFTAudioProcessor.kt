package me.spica27.spicamusic.player.impl.dsp

import be.tarsos.dsp.util.fft.FFT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.player.api.FFTListener
import me.spica27.spicamusic.player.api.IFFTProcessor
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * FFT 音频处理器实现
 * 使用 TarsosDSP 进行实时傅里叶变换，分析 31 个频段的响度
 * 使用独立线程异步处理，避免阻塞音频流
 */
class FFTAudioProcessor : IFFTProcessor {

    companion object {
        private const val TAG = "FFTAudioProcessor"

        // FFT 窗口大小 (2的幂次)
        private const val FFT_SIZE = 4096

        // 平滑因子 (0-1, 越大越平滑)
        private const val SMOOTHING_FACTOR = 0.7f

        // 最小分贝值
        private const val MIN_DB = 0f

        // 最大分贝值
        private const val MAX_DB = 60f

        // 预计算汉宁窗系数（固定 FFT_SIZE，初始化一次即可）
        private val HANNING_WINDOW = FloatArray(FFT_SIZE) { i ->
            (0.5 * (1 - kotlin.math.cos(2 * Math.PI * i / (FFT_SIZE - 1)))).toFloat()
        }
    }

    private val listeners = CopyOnWriteArrayList<FFTListener>()

    private val _bands = MutableStateFlow(FloatArray(IFFTProcessor.BAND_COUNT))
    override val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // 上一帧的平滑值
    private val smoothedBands = FloatArray(IFFTProcessor.BAND_COUNT)

    // FFT 实例 (延迟初始化)
    private var fft: FFT? = null
    private var currentSampleRate = 44100

    // 音频缓冲区
    private val audioBuffer = FloatArray(FFT_SIZE)
    private var bufferIndex = 0

    // 预分配复用缓冲区，避免热路径中重复分配 FloatArray
    // 用于 bytesToFloats 的转换输出（最大可容纳一次 process 调用的样本数）
    private var convertBuffer = FloatArray(FFT_SIZE)
    // 用于传递给 performFFT 的独立拷贝（防止 audioBuffer 被下次 process 覆盖）
    private val processBuffer = FloatArray(FFT_SIZE)
    // 预分配 FFT 幅度缓冲区，避免 performFFT 每次分配（只在 FFT 线程中使用，无需同步）
    private val magnitudesBuffer = FloatArray(FFT_SIZE / 2)
    // 预分配频段映射结果缓冲区
    private val bandValuesBuffer = FloatArray(IFFTProcessor.BAND_COUNT)
    // 预分配 _bands 发射缓冲区（双缓冲避免外部持有引用时被覆盖）
    private val bandResultBuffers = arrayOf(
        FloatArray(IFFTProcessor.BAND_COUNT),
        FloatArray(IFFTProcessor.BAND_COUNT),
    )
    private var activeBandResultIndex = 0

    // 处理标志 - 使用原子布尔值确保线程安全
    private val isProcessing = AtomicBoolean(false)
    
    /**
     * 检查是否正在处理，用于外部快速判断
     */
    fun isBusy(): Boolean = isProcessing.get()

    // 独立的FFT处理线程
    private val fftExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "FFT-Processor").apply {
            priority = Thread.MIN_PRIORITY // 设置为最低优先级，避免影响音频播放
        }
    }
    private val fftDispatcher = fftExecutor.asCoroutineDispatcher()
    private val fftScope = CoroutineScope(fftDispatcher + SupervisorJob())

    override fun enable() {
        _isEnabled.value = true
    }

    override fun disable() {
        _isEnabled.value = false
        reset()
    }

    /**
     * 释放资源
     */
    fun release() {
        fftScope.cancel()
        fftExecutor.shutdown()
    }

    override fun addListener(listener: FFTListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: FFTListener) {
        listeners.remove(listener)
    }

    /**
     * 处理音频数据
     * 如果正在处理FFT，丢弃当前音频数据，避免阻塞
     */
    override fun process(audioData: ByteArray, sampleRate: Int, channelCount: Int, audioDataSize: Int) {
        // 如果正在处理FFT，丢弃当前音频数据，避免阻塞
        if (isProcessing.get()) {
            return
        }

        // 更新采样率
        if (sampleRate != currentSampleRate) {
            currentSampleRate = sampleRate
            fft = FFT(FFT_SIZE)
        }

        if (fft == null) {
            fft = FFT(FFT_SIZE)
        }

        // 将字节数组转换为浮点数组 (16-bit PCM)，复用 convertBuffer 避免分配
        val sampleCount = audioDataSize / 2 / channelCount
        if (convertBuffer.size < sampleCount) {
            convertBuffer = FloatArray(sampleCount)
        }
        bytesToFloats(audioData, channelCount, convertBuffer, sampleCount)

        // 填充缓冲区
        for (i in 0 until sampleCount) {
            audioBuffer[bufferIndex] = convertBuffer[i]
            bufferIndex++

            // 缓冲区满时异步执行 FFT 分析
            if (bufferIndex >= FFT_SIZE) {
                bufferIndex = 0

                // 将 audioBuffer 拷贝到 processBuffer，避免异步执行时被主路径覆盖
                if (isProcessing.compareAndSet(false, true)) {
                    audioBuffer.copyInto(processBuffer)
                    // 在独立线程中异步处理FFT
                    fftScope.launch {
                        try {
                            performFFT(processBuffer)
                        } catch (e: Exception) {
                            Timber.e(e, "FFT processing error")
                        } finally {
                            // 处理完成，允许接收新数据
                            isProcessing.set(false)
                        }
                    }
                }
            }
        }
    }

    override fun reset() {
        bufferIndex = 0
        audioBuffer.fill(0f)
        smoothedBands.fill(0f)
        _bands.value = FloatArray(IFFTProcessor.BAND_COUNT)
        isProcessing.set(false)
    }

    /**
     * 执行 FFT 分析（在独立线程中执行）
     * 所有中间缓冲区均为预分配字段，零堆分配热路径。
     * @param fftData 已填充的音频数据
     */
    private fun performFFT(fftData: FloatArray) {
        val fftInstance = fft ?: return

        // 应用汉宁窗
        applyHanningWindow(fftData)

        // 执行 FFT
        fftInstance.forwardTransform(fftData)

        // 计算频谱幅度（复用预分配的 magnitudesBuffer，零分配）
        for (i in 0 until FFT_SIZE / 2) {
            val real = fftData[2 * i]
            val imag = fftData[2 * i + 1]
            magnitudesBuffer[i] = sqrt(real * real + imag * imag)
        }

        // 映射到 31 个频段（结果写入 bandValuesBuffer，零分配）
        mapToBands(magnitudesBuffer, currentSampleRate, bandValuesBuffer)

        // 平滑处理
        for (i in 0 until IFFTProcessor.BAND_COUNT) {
            smoothedBands[i] = smoothedBands[i] * SMOOTHING_FACTOR +
                    bandValuesBuffer[i] * (1 - SMOOTHING_FACTOR)
        }

        // 双缓冲：切换到下一个结果缓冲区，避免外部持有引用时被覆盖
        val nextIndex = activeBandResultIndex xor 1
        val result = bandResultBuffers[nextIndex]
        smoothedBands.copyInto(result)
        activeBandResultIndex = nextIndex

        // 更新状态（发出已复制的缓冲区引用，零额外分配）
        _bands.value = result

        // 通知监听器
        for (listener in listeners) {
            try {
                listener.onFFTData(result)
            } catch (e: Exception) {
                Timber.e(e, "FFT listener error")
            }
        }
    }

    /**
     * 应用汉宁窗函数（使用预计算系数，避免每帧重复计算三角函数）
     */
    private fun applyHanningWindow(data: FloatArray) {
        for (i in data.indices) {
            data[i] *= HANNING_WINDOW[i]
        }
    }

    /**
     * 将 FFT 结果映射到 31 个频段，结果写入 result（复用，零分配）
     */
    private fun mapToBands(magnitudes: FloatArray, sampleRate: Int, result: FloatArray) {
        val frequencyResolution = sampleRate.toFloat() / FFT_SIZE

        for (bandIndex in 0 until IFFTProcessor.BAND_COUNT) {
            val centerFreq = IFFTProcessor.FREQUENCY_BANDS[bandIndex]

            // 计算频段范围 (对数间隔)
            val lowFreq = if (bandIndex == 0) {
                16f // 最低频率
            } else {
                sqrt(centerFreq * IFFTProcessor.FREQUENCY_BANDS[bandIndex - 1])
            }

            val highFreq = if (bandIndex == IFFTProcessor.BAND_COUNT - 1) {
                22000f // 最高频率
            } else {
                sqrt(centerFreq * IFFTProcessor.FREQUENCY_BANDS[bandIndex + 1])
            }

            // 计算 FFT bin 范围
            val lowBin = max(0, (lowFreq / frequencyResolution).toInt())
            val highBin = min(magnitudes.size - 1, (highFreq / frequencyResolution).toInt())

            // 计算该频段的平均能量
            var sum = 0f
            var count = 0
            for (bin in lowBin..highBin) {
                sum += magnitudes[bin]
                count++
            }

            val avgMagnitude = if (count > 0) sum / count else 0f

            // 转换为分贝并归一化到 0-1
            val db = if (avgMagnitude > 0) {
                20 * kotlin.math.log10(avgMagnitude.toDouble()).toFloat()
            } else {
                MIN_DB
            }

            // 归一化到 0-1 范围
            val normalized = (db - MIN_DB) / (MAX_DB - MIN_DB)
            result[bandIndex] = max(0f, min(1f, normalized))
        }

        return
    }

    /**
     * 将 PCM 字节数组转换为浮点数组，结果写入 out（复用，不分配新数组）
     * @param data 16-bit PCM 数据
     * @param channelCount 声道数
     * @param out 输出目标数组
     * @param sampleCount 有效样本数
     */
    private fun bytesToFloats(data: ByteArray, channelCount: Int, out: FloatArray, sampleCount: Int) {
        var byteIndex = 0
        for (i in 0 until sampleCount) {
            // 读取第一个声道的样本 (16-bit little-endian)
            val low = data[byteIndex].toInt() and 0xFF
            val high = data[byteIndex + 1].toInt()
            val sample = (high shl 8) or low

            // 归一化到 -1.0 到 1.0
            out[i] = sample / 32768f

            // 跳过其他声道
            byteIndex += 2 * channelCount
        }
    }
}
