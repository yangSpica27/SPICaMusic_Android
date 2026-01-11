package me.spica27.spicamusic.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.spica27.spicamusic.ui.home.pages.AudioEffectPage
import me.spica27.spicamusic.ui.home.pages.LibraryPage
import me.spica27.spicamusic.ui.home.pages.SearchPage
import me.spica27.spicamusic.ui.home.pages.SettingsPage
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

    // 使用Material3的Scaffold布局
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                items =
                    listOf(
                        NavigationItem("媒体库", Icons.Outlined.Home),
                        NavigationItem("音效", Icons.Outlined.ThumbUp),
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (selectedIndex) {
                0 -> LibraryPage()
                1 -> AudioEffectPage()
                2 -> SearchPage()
                3 -> SettingsPage()
            }
        }
    }
}
