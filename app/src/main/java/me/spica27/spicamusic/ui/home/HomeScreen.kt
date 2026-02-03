package me.spica27.spicamusic.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.ui.home.pages.LibraryPage
import me.spica27.spicamusic.ui.home.pages.SearchPage
import me.spica27.spicamusic.ui.home.pages.SettingsPage
import me.spica27.spicamusic.ui.widget.materialSharedAxisZIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
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

    val hazeState = rememberHazeState()

    // Scaffold 布局
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                showDivider = false,
                color = Color.Transparent,
                modifier =
                    Modifier
                        .hazeEffect(
                            hazeState,
                            style =
                                HazeMaterials.thick(
                                    MiuixTheme.colorScheme.surfaceContainer,
                                ),
                        ),
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
            modifier =
                Modifier
                    .hazeSource(hazeState)
                    .fillMaxSize(),
        ) {
            AnimatedContent(selectedIndex, contentKey = { it }, transitionSpec = {
                materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
            }) {
                when (it) {
                    0 -> LibraryPage()
                    1 -> SearchPage()
                    2 -> SettingsPage()
                }
            }
        }
    }
}
