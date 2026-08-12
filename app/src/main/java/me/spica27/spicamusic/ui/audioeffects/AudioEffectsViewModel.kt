package me.spica27.spicamusic.ui.audioeffects

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases

/**
 * 音效配置 ViewModel
 *
 * 管理 EQ 和混响效果的状态，并持久化到 DataStore
 */
@Stable
class AudioEffectsViewModel(
    private val settingsUseCases: SettingsUseCases,
) : ViewModel() {
    companion object {
        /** 默认目标响度（LUFS），与 Spotify/YouTube 一致 */
        const val DEFAULT_TARGET_LUFS = -14f

        /** 可选目标响度：流媒体 / ReplayGain 传统参考 / EBU R128 广播标准 */
        val TARGET_LUFS_OPTIONS = listOf(-14f, -18f, -23f)
    }

    // EQ 默认频段（10段）
    private val defaultEqBands = List(10) { 0f }

    // EQ 开关
    val eqEnabled: StateFlow<Boolean> =
        settingsUseCases
            .getBoolean(SettingsUseCases.Keys.EQ_ENABLED, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // EQ 频段增益值 (-12dB to +12dB)
    val eqBands: StateFlow<List<Float>> =
        settingsUseCases
            .getFloatList(SettingsUseCases.Keys.EQ_BANDS, defaultEqBands)
            .stateIn(viewModelScope, SharingStarted.Eagerly, defaultEqBands)

    // 混响开关
    val reverbEnabled: StateFlow<Boolean> =
        settingsUseCases
            .getBoolean(SettingsUseCases.Keys.REVERB_ENABLED, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 混响强度 (0.0 - 1.0)
    val reverbLevel: StateFlow<Float> =
        settingsUseCases
            .getFloat(SettingsUseCases.Keys.REVERB_LEVEL, 0.3f)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.3f)

    // 混响房间大小 (0.0 - 1.0)
    val reverbRoomSize: StateFlow<Float> =
        settingsUseCases
            .getFloat(SettingsUseCases.Keys.REVERB_ROOM_SIZE, 0.5f)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    // 响度归一化开关
    val loudnessNormalizationEnabled: StateFlow<Boolean> =
        settingsUseCases
            .getBoolean(SettingsUseCases.Keys.LOUDNESS_NORMALIZATION_ENABLED, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 设置 EQ 开关
     * 只更新 DataStore，通过 Flow 监听自动应用到播放器
     */
    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBoolean(SettingsUseCases.Keys.EQ_ENABLED, enabled)
        }
    }

    /**
     * 设置单个 EQ 频段增益
     * @param band 频段索引 (0-9)
     * @param gainDb 增益值 (-12.0 to +12.0)
     */
    fun setEqBandGain(
        band: Int,
        gainDb: Float,
    ) {
        if (band !in 0..9) return

        viewModelScope.launch {
            val newBands = eqBands.value.toMutableList()
            newBands[band] = gainDb.coerceIn(-12f, 12f)
            settingsUseCases.setFloatList(SettingsUseCases.Keys.EQ_BANDS, newBands)
        }
    }

    /**
     * 设置所有 EQ 频段增益
     */
    fun setAllEqBands(bands: List<Float>) {
        if (bands.size != 10) return

        viewModelScope.launch {
            val clampedBands = bands.map { it.coerceIn(-12f, 12f) }
            settingsUseCases.setFloatList(SettingsUseCases.Keys.EQ_BANDS, clampedBands)
        }
    }

    /**
     * 应用预设配置
     */
    fun applyPreset(preset: Preset) {
        viewModelScope.launch {
            val bands =
                when (preset) {
                    Preset.POP -> listOf(1f, 2f, 3f, 2f, 0f, -1f, -2f, -2f, 1f, 2f)
                    Preset.ROCK -> listOf(4f, 3f, 2f, 0f, -2f, -1f, 1f, 3f, 4f, 5f)
                    Preset.CLASSICAL -> listOf(3f, 2f, 0f, 0f, -2f, -2f, 0f, 1f, 2f, 3f)
                    Preset.JAZZ -> listOf(2f, 1f, 0f, 1f, 2f, 2f, 1f, 0f, 1f, 2f)
                }
            setAllEqBands(bands)
        }
    }

    /**
     * 设置混响开关
     */
    fun setReverbEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBoolean(SettingsUseCases.Keys.REVERB_ENABLED, enabled)
        }
    }

    /**
     * 设置混响强度
     */
    fun setReverbLevel(level: Float) {
        viewModelScope.launch {
            val clampedLevel = level.coerceIn(0f, 1f)
            settingsUseCases.setFloat(SettingsUseCases.Keys.REVERB_LEVEL, clampedLevel)
        }
    }

    /**
     * 设置混响房间大小
     */
    fun setReverbRoomSize(roomSize: Float) {
        viewModelScope.launch {
            val clampedSize = roomSize.coerceIn(0f, 1f)
            settingsUseCases.setFloat(SettingsUseCases.Keys.REVERB_ROOM_SIZE, clampedSize)
        }
    }

    /**
     * 设置响度归一化开关
     */
    fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBoolean(SettingsUseCases.Keys.LOUDNESS_NORMALIZATION_ENABLED, enabled)
        }
    }

    /**
     * 重置所有音效到默认值
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            _isLoading.value = true

            // 重置 EQ
            setEqEnabled(false)
            setAllEqBands(defaultEqBands)

            // 重置混响
            setReverbEnabled(false)
            settingsUseCases.setFloat(SettingsUseCases.Keys.REVERB_LEVEL, 0.3f)
            settingsUseCases.setFloat(SettingsUseCases.Keys.REVERB_ROOM_SIZE, 0.5f)

            // 重置响度归一化
            setLoudnessNormalizationEnabled(false)
            settingsUseCases.setFloat(
                SettingsUseCases.Keys.LOUDNESS_TARGET_LUFS,
                DEFAULT_TARGET_LUFS,
            )

            _isLoading.value = false
        }
    }

    /**
     * 预设类型
     */
    enum class Preset {
        POP, // 流行
        ROCK, // 摇滚
        CLASSICAL, // 古典
        JAZZ, // 爵士
    }
}
