package me.spica27.spicamusic.player.impl.dsp

import android.media.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * 混响效果处理器
 * 使用延迟效果模拟空间混响
 */
@UnstableApi
class ReverbAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "ReverbAudioProcessor"
        private const val MAX_DELAY_MS = 200 // 最大延迟 200ms
    }

    private var enabled = false
    private var reverbLevel = 0.3f  // 0.0 - 1.0
    private var roomSize = 0.5f     // 0.0 - 1.0
    
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    
    // 延迟缓冲区
    private var delayBuffer: ShortArray? = null
    private var delayBufferIndex = 0
    private var sampleRate = 44100

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate
        
        // 初始化延迟缓冲区
        val maxDelaySamples = (sampleRate * MAX_DELAY_MS / 1000) * inputAudioFormat.channelCount
        delayBuffer = ShortArray(maxDelaySamples)
        delayBufferIndex = 0
        
        return inputAudioFormat
    }

    // 始终保持活跃（已配置即纳入管线），enabled 判断在 queueInput 内部处理
    // 这样切换开关可以即时生效，无需等待 ExoPlayer 重新 configure
    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!enabled) {
            outputBuffer = inputBuffer
            return
        }

        val buffer = delayBuffer
        if (buffer == null) {
            outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        val output = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        
        // 计算延迟参数
        val delayTime = (roomSize * MAX_DELAY_MS).toInt() // 延迟时间(ms)
        val delaySamples = (sampleRate * delayTime / 1000 * inputAudioFormat.channelCount).toInt()
        val feedback = reverbLevel * 0.5f // 反馈系数
        val wet = reverbLevel // 湿声比例
        val dry = 1f - wet * 0.5f // 干声比例
        
        // 处理音频数据
        while (inputBuffer.hasRemaining()) {
            val inputSample = inputBuffer.short
            
            // 读取延迟的样本
            val delayedSample = if (delaySamples > 0 && buffer.isNotEmpty()) {
                val delayIndex = (delayBufferIndex - delaySamples + buffer.size) % buffer.size
                buffer[delayIndex]
            } else {
                0.toShort()
            }
            
            // 混合原始信号和延迟信号
            val mixed = (inputSample * dry + delayedSample * wet).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            
            // 将混合后的信号写入延迟缓冲区（带反馈）
            val feedbackSample = (inputSample + delayedSample * feedback).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            
            buffer[delayBufferIndex] = feedbackSample
            delayBufferIndex = (delayBufferIndex + 1) % buffer.size
            
            output.putShort(mixed)
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
        delayBuffer?.fill(0)
        delayBufferIndex = 0
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        delayBuffer = null
    }

    /**
     * 设置混响开关
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        Timber.tag(TAG).d("Reverb enabled: $enabled")
    }

    /**
     * 设置混响参数
     * @param level 混响强度 (0.0 - 1.0)
     * @param roomSize 房间大小 (0.0 - 1.0)
     */
    fun setReverb(level: Float, roomSize: Float) {
        this.reverbLevel = level.coerceIn(0f, 1f)
        this.roomSize = roomSize.coerceIn(0f, 1f)
        
        Timber.tag(TAG).d("Set reverb - level: $reverbLevel, roomSize: $roomSize")
    }

    /**
     * 获取当前混响参数
     */
    fun getReverbLevel(): Float = reverbLevel
    fun getRoomSize(): Float = roomSize
}
