package me.spica27.spicamusic.player.impl.dsp

import android.media.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 10段均衡器处理器
 * 使用简单的增益调节实现频段均衡
 * 
 * 频段: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16kHz
 */
@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "EqualizerAudioProcessor"
        private const val BAND_COUNT = 10
    }

    // 10个频段的增益值 (-12dB 到 +12dB)
    private val bandGains = FloatArray(BAND_COUNT) { 0f }

    // 10段中心频率 (Hz)
    private val bandFrequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    // 每个频段的 Q 值（带宽）
    private val bandQ = 1.0f

    private var sampleRate = 44100f
    private var channelCount = 2

    // [band][channel] 过滤器阵列
    private var filters: Array<Array<BiquadFilter>>? = null
    
    private var enabled = false
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate.toFloat()
        this.channelCount = inputAudioFormat.channelCount

        filters = Array(BAND_COUNT) {
            Array(channelCount) { BiquadFilter() }
        }
        updateAllFilters()
        return inputAudioFormat
    }

    // 始终保持活跃（已配置即纳入管线），enabled 判断在 queueInput 内部处理
    // 这样切换开关可以即时生效，无需等待 ExoPlayer 重新 configure
    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!enabled || bandGains.all { it == 0f }) {
            outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        // 创建输出缓冲区
        val output = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        
        val filterBank = filters
        if (filterBank == null) {
            outputBuffer = inputBuffer
            return
        }

        val totalSamples = size / 2

        // 处理音频数据 (16-bit PCM, interleaved)
        for (i in 0 until totalSamples) {
            val sample = inputBuffer.short
            val channel = if (channelCount > 0) i % channelCount else 0

            var x = sample / 32768f
            // 应用 10 段均衡器（peaking EQ）
            for (band in 0 until BAND_COUNT) {
                if (bandGains[band] != 0f) {
                    x = filterBank[band][channel].process(x)
                }
            }

            val processed = (x * 32768f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(processed.toShort())
        }
        
        output.flip()
        outputBuffer = output
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        resetFilters()
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        filters = null
    }

    /**
     * 设置均衡器开关
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            resetFilters()
        }
        Timber.tag(TAG).d("EQ enabled: $enabled")
    }

    /**
     * 设置单个频段增益
     * @param band 频段索引 (0-9)
     * @param gainDb 增益值 (-12.0 to +12.0 dB)
     */
    fun setBandGain(band: Int, gainDb: Float) {
        if (band !in 0 until BAND_COUNT) {
            Timber.tag(TAG).w("Invalid band index: $band")
            return
        }
        
        val clampedGain = gainDb.coerceIn(-12f, 12f)
        bandGains[band] = clampedGain

        updateBandFilter(band)

        Timber.tag(TAG).d("Set band $band gain: ${clampedGain}dB")
    }

    /**
     * 设置所有频段增益
     */
    fun setAllBands(gains: FloatArray) {
        if (gains.size != BAND_COUNT) {
            Timber.tag(TAG).w("Invalid bands array size: ${gains.size}, expected $BAND_COUNT")
            return
        }
        
        gains.forEachIndexed { index, gain ->
            setBandGain(index, gain)
        }
    }

    /**
     * 获取当前频段增益
     */
    fun getBandGains(): FloatArray = bandGains.copyOf()

    private fun updateAllFilters() {
        for (band in 0 until BAND_COUNT) {
            updateBandFilter(band)
        }
    }

    private fun updateBandFilter(band: Int) {
        val filterBank = filters ?: return
        val gainDb = bandGains[band]
        val freq = bandFrequencies[band]
        for (ch in 0 until channelCount) {
            filterBank[band][ch].setPeakingEQ(sampleRate, freq, bandQ, gainDb)
            filterBank[band][ch].reset()
        }
    }

    private fun resetFilters() {
        val filterBank = filters ?: return
        for (band in 0 until BAND_COUNT) {
            for (ch in 0 until channelCount) {
                filterBank[band][ch].reset()
            }
        }
    }

    /**
     * 经典二阶 Biquad（Transposed Direct Form II）
     * 参考: Audio EQ Cookbook (peaking EQ)
     */
    private class BiquadFilter {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f

        private var z1 = 0f
        private var z2 = 0f

        fun setPeakingEQ(
            sampleRate: Float,
            frequency: Float,
            q: Float,
            gainDb: Float,
        ) {
            val a = 10.0.pow((gainDb / 40.0).toDouble()).toFloat()
            val w0 = (2.0 * PI * frequency / sampleRate).toFloat()
            val alpha = (sin(w0) / (2.0 * q)).toFloat()
            val cosW0 = cos(w0)

            val b0 = 1 + alpha * a
            val b1 = -2 * cosW0
            val b2 = 1 - alpha * a
            val a0 = 1 + alpha / a
            val a1 = -2 * cosW0
            val a2 = 1 - alpha / a

            this.b0 = (b0 / a0).toFloat()
            this.b1 = (b1 / a0).toFloat()
            this.b2 = (b2 / a0).toFloat()
            this.a1 = (a1 / a0).toFloat()
            this.a2 = (a2 / a0).toFloat()
        }

        fun process(input: Float): Float {
            val output = b0 * input + z1
            z1 = b1 * input - a1 * output + z2
            z2 = b2 * input - a2 * output
            return output
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }
    }
}
