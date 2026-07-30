package me.spica27.spicamusic.ui.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.common.entity.ProgressBarStyle
import me.spica27.spicamusic.common.entity.ThemeColorStyle
import me.spica27.spicamusic.common.entity.ThemeMode
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases

/**
 * 设置页面 ViewModel
 */
@Stable
class SettingsViewModel(
    private val settingsUseCases: SettingsUseCases,
) : ViewModel() {
    // 主题模式；旧版本只有 DARK_MODE 布尔值，首次读取时自动兼容。
    val themeMode =
        combine(
            settingsUseCases.getString(SettingsUseCases.Keys.THEME_MODE, ""),
            settingsUseCases.getBoolean(SettingsUseCases.Keys.DARK_MODE, false),
        ) { savedMode, legacyDarkMode ->
            if (savedMode.isBlank()) {
                if (legacyDarkMode) ThemeMode.DARK.value else ThemeMode.SYSTEM.value
            } else {
                ThemeMode.fromString(savedMode).value
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM.value)

    fun setThemeMode(value: String) {
        viewModelScope.launch {
            val mode = ThemeMode.fromString(value)
            settingsUseCases.setString(SettingsUseCases.Keys.THEME_MODE, mode.value)
            // 保留旧键，便于旧组件或降级安装继续读取。
            settingsUseCases.setBoolean(SettingsUseCases.Keys.DARK_MODE, mode == ThemeMode.DARK)
        }
    }

    // 主题色风格
    val themeColorStyle =
        settingsUseCases
            .getString(
                SettingsUseCases.Keys.THEME_COLOR_STYLE,
                ThemeColorStyle.Textured.value,
            ).stateIn(viewModelScope, SharingStarted.Eagerly, ThemeColorStyle.Textured.value)

    fun setThemeColorStyle(value: String) {
        viewModelScope.launch {
            settingsUseCases.setString(SettingsUseCases.Keys.THEME_COLOR_STYLE, value)
        }
    }

    // 屏幕常亮
    val keepScreenOn =
        settingsUseCases
            .getBoolean(SettingsUseCases.Keys.KEEP_SCREEN_ON, false)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBoolean(SettingsUseCases.Keys.KEEP_SCREEN_ON, enabled)
        }
    }

    // 动态频谱
    val dynamicSpectrumBackground =
        settingsUseCases
            .getString(
                SettingsUseCases.Keys.DYNAMIC_SPECTRUM_BACKGROUND,
                DynamicSpectrumBackground.FluidWarp.value,
            ).stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                DynamicSpectrumBackground.FluidWarp.value,
            )

    fun setDynamicSpectrumBackground(value: String) {
        viewModelScope.launch {
            settingsUseCases.setString(SettingsUseCases.Keys.DYNAMIC_SPECTRUM_BACKGROUND, value)
        }
    }

    // 动态封面
    val dynamicCoverType =
        settingsUseCases
            .getString(
                SettingsUseCases.Keys.DYNAMIC_COVER_TYPE,
                DynamicCoverType.ShiningStars.value,
            ).stateIn(viewModelScope, SharingStarted.Eagerly, DynamicCoverType.ShiningStars.value)

    fun setDynamicCoverType(value: String) {
        viewModelScope.launch {
            settingsUseCases.setString(SettingsUseCases.Keys.DYNAMIC_COVER_TYPE, value)
        }
    }

    // 进度条样式
    val progressBarStyle =
        settingsUseCases
            .getString(
                SettingsUseCases.Keys.PROGRESS_BAR_STYLE,
                ProgressBarStyle.ExpressiveWavy.value,
            ).stateIn(viewModelScope, SharingStarted.Eagerly, ProgressBarStyle.ExpressiveWavy.value)

    fun setProgressBarStyle(value: String) {
        viewModelScope.launch {
            settingsUseCases.setString(SettingsUseCases.Keys.PROGRESS_BAR_STYLE, value)
        }
    }
}
