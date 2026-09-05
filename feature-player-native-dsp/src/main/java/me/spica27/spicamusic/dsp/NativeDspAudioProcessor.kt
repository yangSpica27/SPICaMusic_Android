package me.spica27.spicamusic.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * Media3 adapter for [NativeDspEngine]. The negotiated AudioFormat is returned
 * unchanged, including 24/32-bit and floating-point Hi-Res PCM formats.
 */
@UnstableApi
class NativeDspAudioProcessor(
    private val engine: NativeDspEngine,
) : AudioProcessor {

    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var cachedOutputBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)
    private var inputEnded = false
    private var configured = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (!engine.isAvailable || !isSupportedEncoding(inputAudioFormat.encoding) ||
            inputAudioFormat.sampleRate <= 0 || inputAudioFormat.channelCount <= 0
        ) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            configured = false
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // A Media3 buffer normally contains fewer than 8k frames. 32k leaves
        // headroom for large renderer buffers without allocating per block.
        if (!engine.configure(
                inputAudioFormat.sampleRate,
                inputAudioFormat.channelCount,
                inputAudioFormat.encoding,
            )
        ) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            configured = false
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        configured = true
        inputEnded = false
        return outputAudioFormat
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!configured || !inputBuffer.hasRemaining()) return

        val size = inputBuffer.remaining()
        ensureOutputCapacity(size)
        val start = inputBuffer.position()
        cachedOutputBuffer.clear().limit(size)

        // Native processing is all-or-nothing for a block. On an unexpected
        // direct-buffer/JNI failure, copy the original bytes so playback keeps
        // running at the negotiated quality rather than dropping audio.
        val processed = if (inputBuffer.isDirect) {
            engine.process(inputBuffer, cachedOutputBuffer, size)
        } else {
            -1
        }

        if (processed == size) {
            inputBuffer.position(start + size)
            cachedOutputBuffer.position(0).limit(size)
            outputBuffer = cachedOutputBuffer
        } else {
            cachedOutputBuffer.clear().limit(size)
            val duplicate = inputBuffer.duplicate()
            duplicate.position(start).limit(start + size)
            cachedOutputBuffer.put(duplicate)
            cachedOutputBuffer.flip()
            inputBuffer.position(start + size)
            outputBuffer = cachedOutputBuffer
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val result = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return result
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        if (configured) engine.reset()
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        configured = false
        cachedOutputBuffer = ByteBuffer.allocateDirect(0)
    }

    private fun ensureOutputCapacity(size: Int) {
        if (cachedOutputBuffer.capacity() < size) {
            cachedOutputBuffer = ByteBuffer.allocateDirect(size)
        }
    }

    private fun isSupportedEncoding(encoding: Int): Boolean = when (encoding) {
        C.ENCODING_PCM_8BIT,
        C.ENCODING_PCM_16BIT,
        C.ENCODING_PCM_16BIT_BIG_ENDIAN,
        C.ENCODING_PCM_24BIT,
        C.ENCODING_PCM_24BIT_BIG_ENDIAN,
        C.ENCODING_PCM_32BIT,
        C.ENCODING_PCM_32BIT_BIG_ENDIAN,
        C.ENCODING_PCM_FLOAT,
        -> true
        else -> false
    }
}
