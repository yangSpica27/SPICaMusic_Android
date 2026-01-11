package me.spica27.spicamusic.ui.home.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.player.api.IAudioEffectProcessor
import me.spica27.spicamusic.player.api.IMusicPlayer

/**
 * 音效页面 ViewModel
 */
class AudioEffectViewModel(
    private val player: IMusicPlayer,
) : ViewModel() {
    private val audioEffect: IAudioEffectProcessor = player.audioEffectProcessor

    // EQ 状态
    val eqEnabled: StateFlow<Boolean> =
        audioEffect.eqEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val eqBands: StateFlow<FloatArray> =
        audioEffect.eqBands.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = FloatArray(IAudioEffectProcessor.EQ_BAND_COUNT),
        )

    // 低音增强状态
    val bassBoostEnabled: StateFlow<Boolean> =
        audioEffect.bassBoostEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val bassBoostStrength: StateFlow<Float> =
        audioEffect.bassBoostStrength.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0f,
        )

    // 混响状态
    val reverbEnabled: StateFlow<Boolean> =
        audioEffect.reverbEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    // 音量标准化状态
    val normalizerEnabled: StateFlow<Boolean> =
        audioEffect.normalizerEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    /**
     * 切换 EQ 启用状态
     */
    fun toggleEq() {
        audioEffect.setEqEnabled(!eqEnabled.value)
    }

    /**
     * 设置 EQ 频段增益
     */
    fun setEqBandGain(
        bandIndex: Int,
        gainDb: Float,
    ) {
        audioEffect.setEqBandGain(bandIndex, gainDb)
    }

    /**
     * 重置 EQ
     */
    fun resetEq() {
        audioEffect.resetEq()
    }

    /**
     * 切换低音增强
     */
    fun toggleBassBoost() {
        audioEffect.setBassBoostEnabled(!bassBoostEnabled.value)
    }

    /**
     * 设置低音增强强度
     */
    fun setBassBoostStrength(strength: Float) {
        audioEffect.setBassBoostStrength(strength)
    }

    /**
     * 切换混响
     */
    fun toggleReverb() {
        audioEffect.setReverbEnabled(!reverbEnabled.value)
    }

    /**
     * 切换音量标准化
     */
    fun toggleNormalizer() {
        audioEffect.setNormalizerEnabled(!normalizerEnabled.value)
    }

    /**
     * 获取 EQ 频段频率
     */
    fun getEqBandFrequency(bandIndex: Int): Int = audioEffect.getEqBandFrequency(bandIndex)
}
