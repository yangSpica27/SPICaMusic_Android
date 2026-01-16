package me.spica27.spicamusic.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.home.pages.LibraryPage
import me.spica27.spicamusic.ui.home.pages.SearchPage
import me.spica27.spicamusic.ui.home.pages.SettingsPage
import me.spica27.spicamusic.ui.player.LocalBottomPaddingState
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    // 记住 NavigationBar 的高度
    var navBarHeight by remember { mutableFloatStateOf(0f) }

    // 获取导航栈
    val backStack = LocalNavBackStack.current
    val isHomeScreen = backStack.lastOrNull() == Screen.Home

    // 监听路由变化和高度变化，自动更新 padding
    LaunchedEffect(isHomeScreen, navBarHeight) {
        bottomPaddingState.floatValue =
            if (isHomeScreen && navBarHeight > 0f) {
                navBarHeight
            } else {
                0f
            }
    }

    // Scaffold 布局
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                showDivider = false,
                modifier =
                    Modifier.onSizeChanged {
                        // 记录高度并更新全局底部 padding
                        val height = it.height.toFloat()
                        navBarHeight = height
                    },
                items =
                    listOf(
                        NavigationItem("媒体库", Icons.Outlined.Home),
                        NavigationItem("搜索", Icons.Outlined.Search),
                        NavigationItem("设置", Icons.Outlined.Menu),
                    ),
                selected = selectedIndex,
                onClick = { index -> selectedIndex = index },
                color = MiuixTheme.colorScheme.surfaceContainer,
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
