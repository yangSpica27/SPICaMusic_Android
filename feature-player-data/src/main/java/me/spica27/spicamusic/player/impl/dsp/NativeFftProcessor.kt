package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.StateFlow
import me.spica27.spicamusic.dsp.NativeDspEngine
import me.spica27.spicamusic.player.api.IFFTProcessor

/**
 * 将原生引擎的异步频谱分析能力适配为 [IFFTProcessor]。
 * PCM 只由音频输出层送入引擎，频谱片段也只在其 PCM 边界重置，这里不提供额外入口。
 */
@UnstableApi
class NativeFftProcessor(
    private val engine: NativeDspEngine,
) : IFFTProcessor {

    override val bands: StateFlow<FloatArray> = engine.bands
    override val generation: StateFlow<Long> = engine.spectrumGeneration
    override val isEnabled: StateFlow<Boolean> = engine.isFftEnabled

    override fun enable() = engine.enableFft()

    override fun disable() = engine.disableFft()

    fun setPlaybackActive(active: Boolean) = engine.setPlaybackActive(active)

    fun release() = engine.close()
}
