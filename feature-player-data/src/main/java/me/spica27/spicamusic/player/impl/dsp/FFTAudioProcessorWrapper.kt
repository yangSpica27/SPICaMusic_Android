package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Media3 AudioProcessor 包装器
 * 将音频数据传递给 FFT 处理器进行频谱分析
 */
@UnstableApi
class FFTAudioProcessorWrapper(
    private val fftProcessor: FFTAudioProcessor,
) : AudioProcessor {

    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var isActive = false

    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // 预分配音频数据缓冲区，避免每帧分配新 ByteArray（GC 压力来源）
    private var audioDataBuffer = ByteArray(0)

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // 支持所有线性 PCM 编码（16/24/32-bit 整型及浮点，兼容 Hi-Res 音源）
        if (!PcmMonoDecoder.isSupported(inputAudioFormat.encoding)) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.isActive = true

        return outputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        // 将音频数据传递给 FFT 处理器进行频谱分析
        // isSamplingActive = 应用在前台 && 正在播放：
        // 后台或暂停（暂停中 seek/切歌仍会预缓冲）时直接透传，不做任何拷贝
        if (isActive && inputBuffer.hasRemaining() && fftProcessor.isSamplingActive) {
            val position = inputBuffer.position()
            val size = inputBuffer.remaining()

            // 复用缓冲区，容量不足时才重新分配（避免每帧 ByteArray 分配导致 GC 压力）
            if (audioDataBuffer.size < size) {
                audioDataBuffer = ByteArray(size)
            }
            inputBuffer.get(audioDataBuffer, 0, size)

            // 重置 position 以便后续播放使用
            inputBuffer.position(position)

            // 异步处理 FFT（不阻塞音频流），传入实际有效字节数
            fftProcessor.process(
                audioData = audioDataBuffer,
                sampleRate = inputAudioFormat.sampleRate,
                channelCount = inputAudioFormat.channelCount,
                encoding = inputAudioFormat.encoding,
                audioDataSize = size,
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
        audioDataBuffer = ByteArray(0)
    }
}
