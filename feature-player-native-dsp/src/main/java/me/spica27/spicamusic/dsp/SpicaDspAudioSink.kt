package me.spica27.spicamusic.dsp

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 在 DefaultAudioSink 进行整数或浮点格式转换前执行 DSP。
 * 下游暂时无法继续接收数据时保留已处理的数据块，重试时直接复用，避免重复处理。
 */
@UnstableApi
class SpicaDspAudioSink(
    sink: AudioSink,
    private val engine: PcmDspProcessor,
) : ForwardingAudioSink(sink) {
    private var configured = false
    private var sampleRate = 0
    private var frameSize = 0
    private var source: ByteBuffer? = null
    private var sourceStart = 0
    private var sourceChunkEnd = 0
    private var chunkPending = false
    private var chunkTimestampUs = 0L
    private var output = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

    override fun supportsFormat(format: Format): Boolean =
        getFormatSupport(format) != AudioSink.SINK_FORMAT_UNSUPPORTED

    override fun getFormatSupport(format: Format): Int =
        if (format.sampleMimeType == MimeTypes.AUDIO_RAW) {
            super.getFormatSupport(format)
        } else {
            // 压缩音频直通会绕过公共 PCM 处理链，因此要求先解码为 PCM。
            AudioSink.SINK_FORMAT_UNSUPPORTED
        }

    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport =
        AudioOffloadSupport.DEFAULT_UNSUPPORTED

    override fun setOffloadMode(offloadMode: Int) {
        super.setOffloadMode(AudioSink.OFFLOAD_MODE_DISABLED)
    }

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        super.configure(audioSinkConfig)
        clearPending()
        val format = audioSinkConfig.format
        val linearPcm = format.sampleMimeType == MimeTypes.AUDIO_RAW &&
            Util.isEncodingLinearPcm(format.pcmEncoding) && format.channelCount > 0
        sampleRate = format.sampleRate
        frameSize = if (linearPcm) Util.getPcmFrameSize(format.pcmEncoding, format.channelCount) else 0
        // 每次最多提前处理约 20 ms，避免大块 PCM 在等待写出时一直保留旧音效参数。
        val chunkFrames = (sampleRate / CHUNKS_PER_SECOND).coerceIn(1, MAX_FRAMES)
        // 原生层只接受能逐样本编解码的编码（不含大端浮点和 double），其余由 configure 拒绝后直接旁路。
        configured = linearPcm && engine.isAvailable &&
            engine.configure(format.sampleRate, format.channelCount, format.pcmEncoding, chunkFrames, format.channelMask)
        if (configured) {
            output = ByteBuffer.allocateDirect(chunkFrames * frameSize).order(ByteOrder.nativeOrder())
            output.limit(0)
        }
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        if (!configured) return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        if (source == null) {
            source = buffer
            sourceStart = buffer.position()
        } else {
            check(source === buffer) { "Retry must retain the same input buffer" }
        }
        while (true) {
            if (chunkPending) {
                if (!super.handleBuffer(output, chunkTimestampUs, encodedAccessUnitCount)) return false
                if (output.hasRemaining()) {
                    // DefaultAudioSink 重置卡住的 AudioTrack 时会在内部 flush，丢弃当前输入后返回 true。
                    // 这里同样丢弃整块输入，并重置 DSP 状态以对齐下游的不连续点。
                    buffer.position(buffer.limit())
                    clearPending()
                    engine.reset()
                    return true
                }
                buffer.position(sourceChunkEnd)
                chunkPending = false
            }
            if (!buffer.hasRemaining()) {
                source = null
                return true
            }
            val size = minOf(buffer.remaining(), output.capacity())
            output.clear().limit(size)
            val processed = if (buffer.isDirect) engine.process(buffer, output, size) else -1
            if (processed != size) {
                // 原生处理按整块报告成功或失败；失败时复制原始字节，保证播放继续。
                output.clear().limit(size)
                val original = buffer.duplicate()
                original.limit(original.position() + size)
                output.put(original)
            }
            output.position(0).limit(size)
            chunkPending = true
            sourceChunkEnd = buffer.position() + size
            chunkTimestampUs = presentationTimeUs +
                (buffer.position() - sourceStart).toLong() / frameSize * 1_000_000L / sampleRate
        }
    }

    override fun flush() {
        clearPending()
        engine.reset()
        super.flush()
    }

    override fun handleDiscontinuity() {
        engine.reset()
        super.handleDiscontinuity()
    }

    override fun hasPendingData(): Boolean = chunkPending || super.hasPendingData()

    override fun isEnded(): Boolean = source == null && super.isEnded()

    override fun reset() {
        clearPending()
        configured = false
        engine.reset()
        super.reset()
    }

    override fun release() {
        clearPending()
        configured = false
        engine.reset()
        super.release()
    }

    private fun clearPending() {
        source = null
        chunkPending = false
        output.position(0).limit(0)
    }

    private companion object {
        const val MAX_FRAMES = 32 * 1024
        const val CHUNKS_PER_SECOND = 50
    }
}
