package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.spica27.spicamusic.player.api.IAudioEffectProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * 音效处理器实现
 * 实现 EQ、低音增强、混响、音量标准化
 */
@UnstableApi
class AudioEffectProcessor : IAudioEffectProcessor, AudioProcessor {

    companion object {
        private const val TAG = "AudioEffectProcessor"

        // 空缓冲区
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

        // EQ 中心频率 (Hz)
        private val EQ_FREQUENCIES = intArrayOf(
            31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
        )

        // 低音增强影响的频率范围 (Hz)
        private const val BASS_BOOST_CUTOFF = 200f

        // 混响参数
        private const val REVERB_DELAY_MS = 50
        private const val REVERB_DECAY = 0.3f

        // 音量标准化目标 RMS
        private const val TARGET_RMS = 0.15f
    }

    // EQ 状态
    private val _eqEnabled = MutableStateFlow(false)
    override val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqBands = MutableStateFlow(FloatArray(IAudioEffectProcessor.EQ_BAND_COUNT))
    override val eqBands: StateFlow<FloatArray> = _eqBands.asStateFlow()

    // 低音增强状态
    private val _bassBoostEnabled = MutableStateFlow(false)
    override val bassBoostEnabled: StateFlow<Boolean> = _bassBoostEnabled.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0f)
    override val bassBoostStrength: StateFlow<Float> = _bassBoostStrength.asStateFlow()

    // 混响状态
    private val _reverbEnabled = MutableStateFlow(false)
    override val reverbEnabled: StateFlow<Boolean> = _reverbEnabled.asStateFlow()

    // 音量标准化状态
    private val _normalizerEnabled = MutableStateFlow(false)
    override val normalizerEnabled: StateFlow<Boolean> = _normalizerEnabled.asStateFlow()

    // AudioProcessor 状态
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var inputBuffer = EMPTY_BUFFER
    private var outputBuffer = EMPTY_BUFFER
    private var isInputEnded = false

    // 混响延迟缓冲区
    private var reverbBuffer: FloatArray? = null
    private var reverbBufferIndex = 0

    // 音量标准化 - 滑动窗口 RMS 计算
    private val rmsWindow = FloatArray(4410) // 100ms at 44.1kHz
    private var rmsWindowIndex = 0
    private var currentRms = 0f

    override fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
    }

    override fun setEqBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until IAudioEffectProcessor.EQ_BAND_COUNT) return
        val clampedGain = gainDb.coerceIn(
            IAudioEffectProcessor.EQ_GAIN_MIN,
            IAudioEffectProcessor.EQ_GAIN_MAX
        )
        val bands = _eqBands.value.copyOf()
        bands[bandIndex] = clampedGain
        _eqBands.value = bands
    }

    override fun setEqBands(gains: FloatArray) {
        if (gains.size != IAudioEffectProcessor.EQ_BAND_COUNT) return
        _eqBands.value = gains.copyOf()
    }

    override fun resetEq() {
        _eqBands.value = FloatArray(IAudioEffectProcessor.EQ_BAND_COUNT)
    }

    override fun setBassBoostEnabled(enabled: Boolean) {
        _bassBoostEnabled.value = enabled
    }

    override fun setBassBoostStrength(strength: Float) {
        _bassBoostStrength.value = strength.coerceIn(
            IAudioEffectProcessor.BASS_BOOST_MIN,
            IAudioEffectProcessor.BASS_BOOST_MAX
        )
    }

    override fun setReverbEnabled(enabled: Boolean) {
        _reverbEnabled.value = enabled
    }

    override fun setNormalizerEnabled(enabled: Boolean) {
        _normalizerEnabled.value = enabled
    }

    override fun getEqBandFrequency(bandIndex: Int): Int {
        return if (bandIndex in EQ_FREQUENCIES.indices) {
            EQ_FREQUENCIES[bandIndex]
        } else {
            0
        }
    }

    override fun getAudioProcessor(): AudioProcessor = this

    // ==================== AudioProcessor 实现 ====================

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        this.inputAudioFormat = inputAudioFormat
        
        // 只处理 PCM 16-bit 音频
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            this.outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // 初始化混响缓冲区
        val delaySamples = (inputAudioFormat.sampleRate * REVERB_DELAY_MS / 1000) * inputAudioFormat.channelCount
        reverbBuffer = FloatArray(delaySamples)
        reverbBufferIndex = 0

        this.outputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return _eqEnabled.value || _bassBoostEnabled.value || 
               _reverbEnabled.value || _normalizerEnabled.value
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            this.outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (this.inputBuffer.capacity() < size) {
            this.inputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            this.inputBuffer.clear()
        }

        // 复制输入数据
        val position = inputBuffer.position()
        this.inputBuffer.put(inputBuffer)
        inputBuffer.position(position)
        this.inputBuffer.flip()

        // 处理音频
        processAudio()
    }

    private fun processAudio() {
        val buffer = inputBuffer
        if (!buffer.hasRemaining()) return

        val sampleCount = buffer.remaining() / 2 // 16-bit = 2 bytes
        val samples = FloatArray(sampleCount)

        // 读取样本并转换为 float (-1.0 ~ 1.0)
        for (i in 0 until sampleCount) {
            val sample = buffer.short.toFloat() / 32768f
            samples[i] = sample
        }

        // 应用音效处理
        if (_normalizerEnabled.value) {
            applyNormalizer(samples)
        }

        if (_bassBoostEnabled.value) {
            applyBassBoost(samples)
        }

        if (_eqEnabled.value) {
            applyEqualizer(samples)
        }

        if (_reverbEnabled.value) {
            applyReverb(samples)
        }

        // 转换回 16-bit 并写入输出缓冲区
        if (outputBuffer.capacity() < buffer.capacity()) {
            outputBuffer = ByteBuffer.allocateDirect(buffer.capacity()).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }

        for (sample in samples) {
            val clampedSample = sample.coerceIn(-1f, 1f)
            val shortSample = (clampedSample * 32767f).toInt().toShort()
            outputBuffer.putShort(shortSample)
        }

        outputBuffer.flip()
    }

    /**
     * 应用音量标准化
     */
    private fun applyNormalizer(samples: FloatArray) {
        // 计算当前 RMS
        var sumSquares = 0f
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = kotlin.math.sqrt(sumSquares / samples.size)

        // 更新滑动窗口
        rmsWindow[rmsWindowIndex] = rms
        rmsWindowIndex = (rmsWindowIndex + 1) % rmsWindow.size

        // 计算平均 RMS
        currentRms = rmsWindow.average().toFloat()

        // 应用增益
        if (currentRms > 0.001f) {
            val gain = TARGET_RMS / currentRms
            val clampedGain = gain.coerceIn(0.5f, 2.0f) // 限制增益范围
            for (i in samples.indices) {
                samples[i] *= clampedGain
            }
        }
    }

    /**
     * 应用低音增强 (简化实现：增强低频样本)
     */
    private fun applyBassBoost(samples: FloatArray) {
        val strength = _bassBoostStrength.value
        if (strength <= 0f) return

        // 简单的低通滤波器 + 增益
        val gain = 1f + (strength / 10f) * 0.5f
        var prevSample = 0f

        for (i in samples.indices) {
            // 一阶低通滤波
            val filtered = 0.7f * prevSample + 0.3f * samples[i]
            prevSample = filtered

            // 混合原始信号和增强的低频
            samples[i] = samples[i] * 0.7f + filtered * gain * 0.3f
        }
    }

    /**
     * 应用均衡器 (简化实现：频段增益)
     */
    private fun applyEqualizer(samples: FloatArray) {
        val bands = _eqBands.value
        if (bands.all { it == 0f }) return

        // 简化实现：应用平均增益
        val avgGain = bands.average().toFloat()
        val gainLinear = 10f.pow(avgGain / 20f) // dB 转线性

        for (i in samples.indices) {
            samples[i] *= gainLinear
        }
    }

    /**
     * 应用混响
     */
    private fun applyReverb(samples: FloatArray) {
        val buffer = reverbBuffer ?: return

        for (i in samples.indices) {
            // 获取延迟的样本
            val delayedSample = buffer[reverbBufferIndex]

            // 混合原始信号和延迟信号
            samples[i] = samples[i] * 0.7f + delayedSample * REVERB_DECAY * 0.3f

            // 更新延迟缓冲区
            buffer[reverbBufferIndex] = samples[i]
            reverbBufferIndex = (reverbBufferIndex + 1) % buffer.size
        }
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = isInputEnded && !outputBuffer.hasRemaining()

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        isInputEnded = false
        reverbBufferIndex = 0
        reverbBuffer?.fill(0f)
        rmsWindow.fill(0f)
        rmsWindowIndex = 0
        currentRms = 0f
    }

    override fun reset() {
        flush()
        inputBuffer = EMPTY_BUFFER
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        reverbBuffer = null
    }

    override fun queueEndOfStream() {
        isInputEnded = true
    }
}
