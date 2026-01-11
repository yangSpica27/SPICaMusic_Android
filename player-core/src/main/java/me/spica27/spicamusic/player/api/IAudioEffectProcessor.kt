package me.spica27.spicamusic.player.api

import kotlinx.coroutines.flow.StateFlow

/**
 * 音效处理器接口
 */
interface IAudioEffectProcessor {

    companion object {
        /** EQ 频段数量 */
        const val EQ_BAND_COUNT = 10

        /** EQ 增益范围 (-15dB ~ +15dB) */
        const val EQ_GAIN_MIN = -15f
        const val EQ_GAIN_MAX = 15f

        /** 低音增强范围 (0 ~ 10) */
        const val BASS_BOOST_MIN = 0f
        const val BASS_BOOST_MAX = 10f
    }

    /**
     * EQ 是否启用
     */
    val eqEnabled: StateFlow<Boolean>

    /**
     * EQ 各频段增益值 (dB)
     * 10个频段对应频率: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
     */
    val eqBands: StateFlow<FloatArray>

    /**
     * 低音增强是否启用
     */
    val bassBoostEnabled: StateFlow<Boolean>

    /**
     * 低音增强强度 (0-10)
     */
    val bassBoostStrength: StateFlow<Float>

    /**
     * 混响是否启用
     */
    val reverbEnabled: StateFlow<Boolean>

    /**
     * 音量标准化是否启用
     */
    val normalizerEnabled: StateFlow<Boolean>

    /**
     * 设置 EQ 启用状态
     */
    fun setEqEnabled(enabled: Boolean)

    /**
     * 设置 EQ 某个频段的增益
     * @param bandIndex 频段索引 (0-9)
     * @param gainDb 增益值 (dB, -15 ~ +15)
     */
    fun setEqBandGain(bandIndex: Int, gainDb: Float)

    /**
     * 设置所有 EQ 频段增益
     */
    fun setEqBands(gains: FloatArray)

    /**
     * 重置 EQ 到默认值
     */
    fun resetEq()

    /**
     * 设置低音增强启用状态
     */
    fun setBassBoostEnabled(enabled: Boolean)

    /**
     * 设置低音增强强度
     * @param strength 强度值 (0-10)
     */
    fun setBassBoostStrength(strength: Float)

    /**
     * 设置混响启用状态
     */
    fun setReverbEnabled(enabled: Boolean)

    /**
     * 设置音量标准化启用状态
     */
    fun setNormalizerEnabled(enabled: Boolean)

    /**
     * 获取 EQ 频段对应的频率 (Hz)
     */
    fun getEqBandFrequency(bandIndex: Int): Int

    /**
     * 获取用于 ExoPlayer 的 AudioProcessor
     */
    fun getAudioProcessor(): androidx.media3.common.audio.AudioProcessor
}
