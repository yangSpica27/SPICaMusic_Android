package me.spica27.spicamusic.player.impl.dsp

import kotlinx.coroutines.flow.StateFlow
import me.spica27.spicamusic.dsp.NativeDspEngine
import me.spica27.spicamusic.player.api.IFFTProcessor
import java.nio.ByteBuffer

/** IFFTProcessor facade backed by the native engine's asynchronous analyzer. */
class NativeFftProcessor(
    private val engine: NativeDspEngine,
) : IFFTProcessor {

    override val bands: StateFlow<FloatArray> = engine.bands
    override val isEnabled: StateFlow<Boolean> = engine.isFftEnabled

    @Volatile private var playbackActive = false
    private var lastFormat: Format? = null
    private var inputBuffer = ByteBuffer.allocateDirect(0)
    private var outputBuffer = ByteBuffer.allocateDirect(0)

    override fun enable() = engine.enableFft()

    override fun disable() = engine.disableFft()

    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        engine.setPlaybackActive(active)
    }

    /** Compatibility entry point for callers that still provide ByteArray PCM. */
    override fun process(
        audioData: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        audioDataSize: Int,
    ) {
        if (!isEnabled.value || !playbackActive || audioDataSize <= 0) return
        val size = audioDataSize.coerceAtMost(audioData.size)
        val format = Format(sampleRate, channelCount, encoding)
        if (format != lastFormat) {
            if (!engine.configure(sampleRate, channelCount, encoding)) return
            lastFormat = format
        }
        if (inputBuffer.capacity() < size) inputBuffer = ByteBuffer.allocateDirect(size)
        if (outputBuffer.capacity() < size) outputBuffer = ByteBuffer.allocateDirect(size)
        inputBuffer.clear()
        inputBuffer.limit(size)
        inputBuffer.put(audioData, 0, size)
        inputBuffer.flip()
        outputBuffer.clear().limit(size)
        engine.process(inputBuffer, outputBuffer, size)
    }

    override fun reset() {
        lastFormat = null
        engine.reset()
    }

    fun release() = engine.close()

    private data class Format(val sampleRate: Int, val channels: Int, val encoding: Int)
}
