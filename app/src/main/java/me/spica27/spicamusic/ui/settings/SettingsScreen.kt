package me.spica27.spicamusic.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.rounded.Light
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.utils.navSharedBounds
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 设置页面
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    val viewModel: SettingsViewModel = koinViewModel()

    // 定义设置项列表，每次 recompose 时重建以支持语言切换
    val settingsItems =
        buildList {
            // 外观设置
            add(SettingsItem.GroupHeader(title = stringResource(R.string.settings_appearance)))
            add(
                SettingsItem.SwitchItem(
                    title = stringResource(R.string.setting_dark_mode),
                    subtitle = stringResource(R.string.settings_dark_mode_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Light,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    },
                    key = "dark_mode",
                    valueFlow = viewModel.darkMode,
                    onValueChange = { viewModel.setDarkMode(it) },
                ),
            )

            // 播放设置
            add(SettingsItem.GroupHeader(title = stringResource(R.string.settings_playback)))
            add(
                SettingsItem.NavigationItem(
                    modifier = Modifier.navSharedBounds(Screen.AudioEffects),
                    title = stringResource(R.string.setting_eq),
                    subtitle = stringResource(R.string.settings_sound_effects_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        backStack.add(Screen.AudioEffects)
                    },
                ),
            )
            add(
                SettingsItem.SwitchItem(
                    title = stringResource(R.string.settings_keep_screen_on),
                    subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    },
                    key = "keep_screen_on",
                    valueFlow = viewModel.keepScreenOn,
                    onValueChange = { viewModel.setKeepScreenOn(it) },
                ),
            )
            add(
                SettingsItem.SelectItem(
                    title = stringResource(R.string.settings_dynamic_spectrum),
                    subtitle = null,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    },
                    key = "dynamic_spectrum_background",
                    options =
                        DynamicSpectrumBackground.presets.map {
                            SelectOption(
                                label = it.name,
                                value = it.value,
                            )
                        },
                    valueFlow = viewModel.dynamicSpectrumBackground,
                    onValueChange = { viewModel.setDynamicSpectrumBackground(it) },
                ),
            )
            add(
                SettingsItem.SelectItem(
                    title = stringResource(R.string.settings_dynamic_cover),
                    subtitle = stringResource(R.string.settings_dynamic_cover_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    },
                    key = "dynamic_cover_type",
                    options =
                        DynamicCoverType.presets.map {
                            SelectOption(
                                label = it.name,
                                value = it.value,
                            )
                        },
                    valueFlow = viewModel.dynamicCoverType,
                    onValueChange = { viewModel.setDynamicCoverType(it) },
                ),
            )

            // 媒体库设置
            add(SettingsItem.GroupHeader(title = stringResource(R.string.settings_media_library)))
            add(
                SettingsItem.NavigationItem(
                    modifier = Modifier.navSharedBounds(Screen.MediaLibrarySource),
                    title = stringResource(R.string.media_library_source_title),
                    subtitle = stringResource(R.string.settings_media_library_source_subtitle),
                    icon = {
                        Icon(
                            tint = MiuixTheme.colorScheme.onSurface,
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        backStack.add(Screen.MediaLibrarySource)
                    },
                ),
            )
        }

    val scrollerBehavior = MiuixScrollBehavior()

    Scaffold(modifier = modifier.fillMaxSize(), popupHost = { MiuixPopupHost() }, topBar = {
        TopAppBar(
            title = stringResource(R.string.title_settings),
            scrollBehavior = scrollerBehavior,
        )
    }) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                    .nestedScroll(scrollerBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            items(
                items = settingsItems,
                key = { item ->
                    when (item) {
                        is SettingsItem.GroupHeader -> "header_${item.title}"
                        is SettingsItem.SwitchItem -> "switch_${item.key}"
                        is SettingsItem.SelectItem -> "select_${item.key}"
                        is SettingsItem.NavigationItem -> "nav_${item.title}"
                    }
                },
            ) { item ->
                SettingsItemView(item = item)
            }
            item {
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }
}
