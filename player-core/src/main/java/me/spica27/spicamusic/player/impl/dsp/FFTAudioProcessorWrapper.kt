package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.player.api.IFFTProcessor
import java.nio.ByteBuffer

/**
 * Media3 AudioProcessor 包装器
 * 将音频数据传递给 FFT 处理器进行频谱分析
 */
@UnstableApi
class FFTAudioProcessorWrapper(
    private val fftProcessor: IFFTProcessor,
) : AudioProcessor {

    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var isActive = false

    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // 只处理 16-bit PCM 数据
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.isActive = true

        return outputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        // 将音频数据传递给 FFT 处理器进行频谱分析
        if (isActive && inputBuffer.hasRemaining() && fftProcessor.isEnabled.value) {
            val position = inputBuffer.position()
            val limit = inputBuffer.limit()
            val size = limit - position
            
            // 提取音频数据
            val audioData = ByteArray(size)
            inputBuffer.get(audioData)
            
            // 重置 position 以便后续播放使用
            inputBuffer.position(position)
            
            // 异步处理 FFT（不阻塞音频流）
            fftProcessor.process(
                audioData = audioData,
                sampleRate = inputAudioFormat.sampleRate,
                channelCount = inputAudioFormat.channelCount
            )
        }
        
        // 将缓冲区传递给输出（透传模式，不修改音频数据）
        outputBuffer = inputBuffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        fftProcessor.reset()
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        isActive = false
    }
}
