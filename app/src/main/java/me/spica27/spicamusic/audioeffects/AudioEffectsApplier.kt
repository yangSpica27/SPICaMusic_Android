package me.spica27.spicamusic.audioeffects

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases

/**
 * 应用级音效应用器。
 */
class AudioEffectsApplier(
    private val settings: SettingsUseCases,
    private val player: PlayerUseCases,
) {
    // EQ 默认频段（10 段，全 0）
    private val defaultEqBands = List(10) { 0f }

    /** 启动收集；App.onCreate 调用一次即可。 */
    fun start(scope: CoroutineScope) {
        // EQ 开关
        scope.launch {
            settings
                .getBoolean(SettingsUseCases.Keys.EQ_ENABLED, false)
                .collect { player.setEQEnabled(it) }
        }

        // EQ 频段增益
        // 注意：getFloatList 内部没有 distinctUntilChanged，任何无关偏好写入都会让
        // dataStore.data 重新发射，导致重复 setAllEQBands → 重新发布快照，
        // 音频线程按引用比较判定“变了”后重算系数并 reset 滤波器，产生细微爆音，故此处必须去重。
        scope.launch {
            settings
                .getFloatList(SettingsUseCases.Keys.EQ_BANDS, defaultEqBands)
                .distinctUntilChanged()
                .collect { player.setAllEQBands(it.toFloatArray()) }
        }

        // 混响开关
        scope.launch {
            settings
                .getBoolean(SettingsUseCases.Keys.REVERB_ENABLED, false)
                .collect { player.setReverbEnabled(it) }
        }

        // 混响参数（强度 + 房间大小）
        scope.launch {
            combine(
                settings.getFloat(SettingsUseCases.Keys.REVERB_LEVEL, 0.3f),
                settings.getFloat(SettingsUseCases.Keys.REVERB_ROOM_SIZE, 0.5f),
            ) { level, roomSize -> level to roomSize }
                .distinctUntilChanged()
                .collect { (level, roomSize) -> player.setReverb(level, roomSize) }
        }

        // 响度归一化开关
        scope.launch {
            settings
                .getBoolean(SettingsUseCases.Keys.LOUDNESS_NORMALIZATION_ENABLED, false)
                .collect { player.setLoudnessNormalizationEnabled(it) }
        }
    }
}
