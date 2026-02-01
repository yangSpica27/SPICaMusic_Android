package me.spica27.spicamusic.ui.audioeffects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.utils.PreferencesManager

/**
 * 音效配置 ViewModel
 *
 * 管理 EQ 和混响效果的状态，并持久化到 DataStore
 */
class AudioEffectsViewModel(
    private val preferencesManager: PreferencesManager,
    private val player: IMusicPlayer,
) : ViewModel() {
    // EQ 默认频段（10段）
    private val defaultEqBands = List(10) { 0f }

    // EQ 开关
    val eqEnabled: StateFlow<Boolean> =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.EQ_ENABLED, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // EQ 频段增益值 (-12dB to +12dB)
    val eqBands: StateFlow<List<Float>> =
        preferencesManager
            .getFloatList(PreferencesManager.Keys.EQ_BANDS, defaultEqBands)
            .stateIn(viewModelScope, SharingStarted.Eagerly, defaultEqBands)

    // 混响开关
    val reverbEnabled: StateFlow<Boolean> =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.REVERB_ENABLED, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 混响强度 (0.0 - 1.0)
    val reverbLevel: StateFlow<Float> =
        preferencesManager
            .getFloat(PreferencesManager.Keys.REVERB_LEVEL, 0.3f)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.3f)

    // 混响房间大小 (0.0 - 1.0)
    val reverbRoomSize: StateFlow<Float> =
        preferencesManager
            .getFloat(PreferencesManager.Keys.REVERB_ROOM_SIZE, 0.5f)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.5f)

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            eqEnabled.collect { enabled ->
                player.setEQEnabled(enabled)
            }
        }

        viewModelScope.launch {
            eqBands.collect { bands ->
                player.setAllEQBands(bands.toFloatArray())
            }
        }

        viewModelScope.launch {
            reverbEnabled.collect { enabled ->
                player.setReverbEnabled(enabled)
            }
        }

        viewModelScope.launch {
            combine(reverbLevel, reverbRoomSize) { level, roomSize ->
                Pair(level, roomSize)
            }.collect { (level, roomSize) ->
                player.setReverb(level, roomSize)
            }
        }
    }

    /**
     * 设置 EQ 开关
     */
    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBoolean(PreferencesManager.Keys.EQ_ENABLED, enabled)
            player.setEQEnabled(enabled)
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
            preferencesManager.setFloatList(PreferencesManager.Keys.EQ_BANDS, newBands)
            player.setEQBandGain(band, gainDb)
        }
    }

    /**
     * 设置所有 EQ 频段增益
     */
    fun setAllEqBands(bands: List<Float>) {
        if (bands.size != 10) return

        viewModelScope.launch {
            val clampedBands = bands.map { it.coerceIn(-12f, 12f) }
            preferencesManager.setFloatList(PreferencesManager.Keys.EQ_BANDS, clampedBands)
            player.setAllEQBands(clampedBands.toFloatArray())
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
            preferencesManager.setBoolean(PreferencesManager.Keys.REVERB_ENABLED, enabled)
            player.setReverbEnabled(enabled)
        }
    }

    /**
     * 设置混响强度
     */
    fun setReverbLevel(level: Float) {
        viewModelScope.launch {
            val clampedLevel = level.coerceIn(0f, 1f)
            preferencesManager.setFloat(PreferencesManager.Keys.REVERB_LEVEL, clampedLevel)
            player.setReverb(clampedLevel, reverbRoomSize.value)
        }
    }

    /**
     * 设置混响房间大小
     */
    fun setReverbRoomSize(roomSize: Float) {
        viewModelScope.launch {
            val clampedSize = roomSize.coerceIn(0f, 1f)
            preferencesManager.setFloat(PreferencesManager.Keys.REVERB_ROOM_SIZE, clampedSize)
            player.setReverb(reverbLevel.value, clampedSize)
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
            preferencesManager.setFloat(PreferencesManager.Keys.REVERB_LEVEL, 0.3f)
            preferencesManager.setFloat(PreferencesManager.Keys.REVERB_ROOM_SIZE, 0.5f)

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
