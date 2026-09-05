package me.spica27.spicamusic.audioeffects

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases

/** Applies persisted audio-effect settings to the native player engine. */
class AudioEffectsApplier(
    private val settings: SettingsUseCases,
    private val player: PlayerUseCases,
) {
    private val defaultEqBands = List(10) { 0f }

    /** Starts collecting settings; call once from Application.onCreate. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            settings
                .getBoolean(SettingsUseCases.Keys.EQ_ENABLED, false)
                .collect { player.setEQEnabled(it) }
        }

        scope.launch {
            settings
                .getFloatList(SettingsUseCases.Keys.EQ_BANDS, defaultEqBands)
                .distinctUntilChanged()
                .collect { player.setAllEQBands(it.toFloatArray()) }
        }

        scope.launch {
            settings
                .getBoolean(SettingsUseCases.Keys.LOUDNESS_NORMALIZATION_ENABLED, false)
                .collect { player.setLoudnessNormalizationEnabled(it) }
        }

        scope.launch {
            settings
                .getFloat(SettingsUseCases.Keys.LOUDNESS_TARGET_LUFS, -14f)
                .distinctUntilChanged()
                .collect { player.setLoudnessTargetLufs(it) }
        }
    }
}
