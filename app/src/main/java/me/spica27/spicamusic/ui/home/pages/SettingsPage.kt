package me.spica27.spicamusic.ui.home.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Light
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.settings.SelectOption
import me.spica27.spicamusic.ui.settings.SettingsItem
import me.spica27.spicamusic.ui.settings.SettingsItemView
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost

/**
 * 设置页面
 */
@Composable
fun SettingsPage(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    val viewModel: SettingsViewModel = koinViewModel()

    // 定义设置项列表 - 只构建一次，每个项目通过 StateFlow.collectAsState() 响应变化
    val settingsItems =
        remember {
            buildList {
                // 外观设置
                add(SettingsItem.GroupHeader(title = "外观"))
                add(
                    SettingsItem.SwitchItem(
                        title = "暗色模式",
                        subtitle = "启用深色主题",
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Light,
                                contentDescription = null,
                            )
                        },
                        key = "dark_mode",
                        valueFlow = viewModel.darkMode,
                        onValueChange = { viewModel.setDarkMode(it) },
                    ),
                )

                // 播放设置
                add(SettingsItem.GroupHeader(title = "播放"))
                add(
                    SettingsItem.SwitchItem(
                        title = "屏幕常亮",
                        subtitle = "播放时保持屏幕常亮",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                            )
                        },
                        key = "keep_screen_on",
                        valueFlow = viewModel.keepScreenOn,
                        onValueChange = { viewModel.setKeepScreenOn(it) },
                    ),
                )
                add(
                    SettingsItem.SelectItem(
                        title = "动态频谱背景",
                        subtitle = null,
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
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

                // 媒体库设置
                add(SettingsItem.GroupHeader(title = "媒体库"))
                add(
                    SettingsItem.NavigationItem(
                        title = "媒体库来源",
                        subtitle = "管理音乐扫描路径",
                        icon = {
                            Icon(
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
        }

    val scrollerBehavior = MiuixScrollBehavior()

    Scaffold(modifier = modifier.fillMaxSize(), popupHost = { MiuixPopupHost() }, topBar = {
        TopAppBar(
            title = "设置",
            scrollBehavior = scrollerBehavior,
        )
    }) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
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
        }
    }
}
