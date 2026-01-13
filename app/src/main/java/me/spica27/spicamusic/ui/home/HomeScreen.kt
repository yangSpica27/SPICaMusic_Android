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
import me.spica27.spicamusic.ui.player.LocalBottomPaddingState
import me.spica27.spicamusic.ui.player.SetBottomPadding
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * 首页界面
 * 底部NavigationBar + 内容区域
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = koinActivityViewModel(),
) {
    // 当前选中的页面索引
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    // 获取全局底部 padding 状态
    val bottomPaddingState = LocalBottomPaddingState.current

    // 记录 NavigationBar 高度
    var navBarHeight by remember { mutableFloatStateOf(0f) }

    // 每次进入页面时恢复 padding（如果已经测量过）
    if (navBarHeight > 0f) {
        SetBottomPadding(navBarHeight)
    }

    // Scaffold 布局
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier =
                    Modifier.onSizeChanged {
                        // 记录高度并更新全局底部 padding
                        navBarHeight = it.height.toFloat()
                        bottomPaddingState.floatValue = it.height.toFloat()
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
        // 内容区域
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
