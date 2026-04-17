package me.spica27.spicamusic.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * FFT 频谱分析结果监听器
 */
fun interface FFTListener {
    /**
     * 接收频谱数据
     * @param bands 31个频段的响度值，范围 0.0 - 1.0
     */
    fun onFFTData(bands: FloatArray)
}

/**
 * FFT 音频处理器接口
 * 实时分析音频频谱数据
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
     * 是否启用 FFT 分析
     */
    val isEnabled: StateFlow<Boolean>

    /**
     * 启用 FFT 分析
     */
    fun enable()

    /**
     * 禁用 FFT 分析
     */
    fun disable()

    /**
     * 添加监听器
     */
    fun addListener(listener: FFTListener)

    /**
     * 移除监听器
     */
    fun removeListener(listener: FFTListener)

    /**
     * 处理音频数据
     * @param audioData PCM 音频数据
     * @param sampleRate 采样率
     * @param channelCount 声道数
     */
    fun process(audioData: ByteArray, sampleRate: Int, channelCount: Int)

    /**
     * 重置处理器状态
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
