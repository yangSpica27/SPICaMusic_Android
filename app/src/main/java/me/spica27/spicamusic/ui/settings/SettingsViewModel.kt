package me.spica27.spicamusic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.utils.PreferencesManager

/**
 * 设置页面 ViewModel
 */
class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {
    // 暗色模式
    val darkMode =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.DARK_MODE, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBoolean(PreferencesManager.Keys.DARK_MODE, enabled)
        }
    }

    // 屏幕常亮
    val keepScreenOn =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.KEEP_SCREEN_ON, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBoolean(PreferencesManager.Keys.KEEP_SCREEN_ON, enabled)
        }
    }

    // 动态频谱
    val dynamicSpectrumBackground =
        preferencesManager
            .getString(
                PreferencesManager.Keys.DYNAMIC_SPECTRUM_BACKGROUND,
                DynamicSpectrumBackground.TopGlow.value,
            ).stateIn(viewModelScope, SharingStarted.Eagerly, DynamicSpectrumBackground.TopGlow.value)

    fun setDynamicSpectrumBackground(value: String) {
        viewModelScope.launch {
            preferencesManager.setString(PreferencesManager.Keys.DYNAMIC_SPECTRUM_BACKGROUND, value)
        }
    }
}
