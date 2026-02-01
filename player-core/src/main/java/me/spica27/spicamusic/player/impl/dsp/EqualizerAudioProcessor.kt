package me.spica27.spicamusic.player.impl.dsp

import android.media.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

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
    
    // 线性增益 (从 dB 转换)
    private val linearGains = FloatArray(BAND_COUNT) { 1f }
    
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
        return inputAudioFormat
    }

    override fun isActive(): Boolean = enabled

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        // 创建输出缓冲区
        val output = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        
        // 处理音频数据 (16-bit PCM)
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val avgGain = linearGains.average().toFloat()
            val processed = (sample * avgGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            
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
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    }

    /**
     * 设置均衡器开关
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
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
        
        // 转换为线性增益: gain_linear = 10^(gain_dB / 20)
        linearGains[band] = 10.0.pow(clampedGain / 20.0).toFloat()
        
        Timber.tag(TAG).d("Set band $band gain: ${clampedGain}dB (linear: ${linearGains[band]})")
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
}
