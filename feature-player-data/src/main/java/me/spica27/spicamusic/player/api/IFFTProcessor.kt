package me.spica27.spicamusic.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * FFT 音频处理器接口
 * 实时分析音频频谱数据
 *
 * 消费方式：收集 [bands] StateFlow（无监听器注册机制）。
 * 采样开关由应用前后台生命周期驱动：前台 [enable]，后台 [disable] 以降低功耗。
 */
interface IFFTProcessor {
    /**
     * 31 个频段的响度值 (0.0 - 1.0)
     * 频段: 20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
     *       800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
     *       12500, 16000, 20000 Hz
     */
    val bands: StateFlow<FloatArray>

    /**
     * 是否启用 FFT 采样分析
     */
    val isEnabled: StateFlow<Boolean>

    /**
     * 启用 FFT 采样（应用进入前台时调用）
     */
    fun enable()

    /**
     * 禁用 FFT 采样并清空频谱数据（应用进入后台时调用）
     */
    fun disable()

    /**
     * 处理音频数据
     * @param audioData 交错线性 PCM 音频数据
     * @param sampleRate 采样率
     * @param channelCount 声道数
     * @param encoding PCM 编码，取值为 androidx.media3.common.C.ENCODING_PCM_*
     *                 （支持 8/16/24/32-bit 整型与 32-bit 浮点，含大端变体）
     * @param audioDataSize 有效字节数，默认为 audioData.size；传入复用缓冲区时需指定实际大小
     */
    fun process(
        audioData: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        audioDataSize: Int = audioData.size,
    )

    /**
     * 重置处理器状态并清空频谱数据
     */
    fun reset()

    companion object {
        /**
         * 31 个标准频段 (Hz)
         */
        val FREQUENCY_BANDS = floatArrayOf(
            20f, 25f, 32f, 40f, 50f, 63f, 80f, 100f, 125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f,
            800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f,
            12500f, 16000f, 20000f
        )

        /**
         * 频段数量
         */
        const val BAND_COUNT = 31
    }
}
