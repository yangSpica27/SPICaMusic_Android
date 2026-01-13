package me.spica27.spicamusic.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import me.spica27.spicamusic.ui.home.pages.LibraryPage
import me.spica27.spicamusic.ui.home.pages.SearchPage
import me.spica27.spicamusic.ui.home.pages.SettingsPage
import me.spica27.spicamusic.ui.player.DraggablePlayerSheet
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * 首页界面
 * 底部NavigationBar + 内容区域 + 可拖拽播放器
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = koinActivityViewModel(),
) {
    // 当前选中的页面索引
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    // 获取全局 PlayerViewModel
    val playerViewModel = LocalPlayerViewModel.current

    var bottomPadding by remember { mutableFloatStateOf(0f) }

    // 使用 DraggablePlayerSheet 包裹整个界面
    DraggablePlayerSheet(
        viewModel = playerViewModel,
        bottomPadding = bottomPadding,
    ) {
        // Scaffold 布局
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier =
                        Modifier.onSizeChanged {
                            bottomPadding = it.height.toFloat()
                        },
                    items =
                        listOf(
                            NavigationItem("媒体库", Icons.Outlined.Home),
                            NavigationItem("搜索", Icons.Outlined.Search),
                            NavigationItem("设置", Icons.Outlined.Menu),
                        ),
                    selected = selectedIndex,
                    onClick = { index -> selectedIndex = index },
                )
            },
        ) { paddingValues ->
            // 内容区域 - 如果有播放内容，为播放条留出空间
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                when (selectedIndex) {
                    0 -> LibraryPage()
                    1 -> SearchPage()
                    2 -> SettingsPage()
                }
            }
        }
    }
}
