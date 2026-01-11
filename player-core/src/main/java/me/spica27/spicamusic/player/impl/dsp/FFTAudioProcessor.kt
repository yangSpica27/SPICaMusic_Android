package me.spica27.spicamusic.player.impl.dsp

import be.tarsos.dsp.util.fft.FFT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.spica27.spicamusic.player.api.FFTListener
import me.spica27.spicamusic.player.api.IFFTProcessor
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * FFT 音频处理器实现
 * 使用 TarsosDSP 进行实时傅里叶变换，分析 31 个频段的响度
 */
class FFTAudioProcessor : IFFTProcessor {

    companion object {
        private const val TAG = "FFTAudioProcessor"

        // FFT 窗口大小 (2的幂次)
        private const val FFT_SIZE = 4096

        // 平滑因子 (0-1, 越大越平滑)
        private const val SMOOTHING_FACTOR = 0.7f

        // 最小分贝值
        private const val MIN_DB = -60f

        // 最大分贝值
        private const val MAX_DB = 0f
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

    override fun enable() {
        _isEnabled.value = true
    }

    override fun disable() {
        _isEnabled.value = false
        reset()
    }

    override fun addListener(listener: FFTListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: FFTListener) {
        listeners.remove(listener)
    }

    override fun process(audioData: ByteArray, sampleRate: Int, channelCount: Int) {
        if (!_isEnabled.value) return

        // 更新采样率
        if (sampleRate != currentSampleRate) {
            currentSampleRate = sampleRate
            fft = FFT(FFT_SIZE)
        }

        if (fft == null) {
            fft = FFT(FFT_SIZE)
        }

        // 将字节数组转换为浮点数组 (16-bit PCM)
        val samples = bytesToFloats(audioData, channelCount)

        // 填充缓冲区
        for (sample in samples) {
            audioBuffer[bufferIndex] = sample
            bufferIndex++

            // 缓冲区满时进行 FFT 分析
            if (bufferIndex >= FFT_SIZE) {
                performFFT()
                bufferIndex = 0
            }
        }
    }

    override fun reset() {
        bufferIndex = 0
        audioBuffer.fill(0f)
        smoothedBands.fill(0f)
        _bands.value = FloatArray(IFFTProcessor.BAND_COUNT)
    }

    /**
     * 执行 FFT 分析
     */
    private fun performFFT() {
        val fftInstance = fft ?: return

        // 复制数据用于 FFT (避免修改原始缓冲区)
        val fftData = audioBuffer.copyOf()

        // 应用汉宁窗
        applyHanningWindow(fftData)

        // 执行 FFT
        fftInstance.forwardTransform(fftData)

        // 计算频谱幅度
        val magnitudes = FloatArray(FFT_SIZE / 2)
        for (i in 0 until FFT_SIZE / 2) {
            val real = fftData[2 * i]
            val imag = fftData[2 * i + 1]
            magnitudes[i] = sqrt(real * real + imag * imag)
        }

        // 映射到 31 个频段
        val bandValues = mapToBands(magnitudes, currentSampleRate)

        // 平滑处理
        for (i in 0 until IFFTProcessor.BAND_COUNT) {
            smoothedBands[i] = smoothedBands[i] * SMOOTHING_FACTOR +
                    bandValues[i] * (1 - SMOOTHING_FACTOR)
        }

        // 更新状态
        val result = smoothedBands.copyOf()
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
     * 应用汉宁窗函数
     */
    private fun applyHanningWindow(data: FloatArray) {
        for (i in data.indices) {
            val multiplier = 0.5 * (1 - kotlin.math.cos(2 * Math.PI * i / (data.size - 1)))
            data[i] = (data[i] * multiplier).toFloat()
        }
    }

    /**
     * 将 FFT 结果映射到 31 个频段
     */
    private fun mapToBands(magnitudes: FloatArray, sampleRate: Int): FloatArray {
        val result = FloatArray(IFFTProcessor.BAND_COUNT)
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

        return result
    }

    /**
     * 将 PCM 字节数组转换为浮点数组
     * @param data 16-bit PCM 数据
     * @param channelCount 声道数
     * @return 单声道浮点数组 (-1.0 to 1.0)
     */
    private fun bytesToFloats(data: ByteArray, channelCount: Int): FloatArray {
        val samples = data.size / 2 / channelCount
        val result = FloatArray(samples)

        var byteIndex = 0
        for (i in 0 until samples) {
            // 读取第一个声道的样本 (16-bit little-endian)
            val low = data[byteIndex].toInt() and 0xFF
            val high = data[byteIndex + 1].toInt()
            val sample = (high shl 8) or low

            // 归一化到 -1.0 到 1.0
            result[i] = sample / 32768f

            // 跳过其他声道
            byteIndex += 2 * channelCount
        }

        return result
    }
}
