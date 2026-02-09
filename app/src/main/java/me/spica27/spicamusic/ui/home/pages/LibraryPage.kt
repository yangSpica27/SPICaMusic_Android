package me.spica27.spicamusic.ui.home.pages

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.random.Random

/**
 * 媒体库页面
 */
@Composable
fun LibraryPage(modifier: Modifier = Modifier) {
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberHazeState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "媒体库",
                largeTitle = "媒体库", // If not specified, title value will be used
                scrollBehavior = scrollBehavior,
                color = Color.Transparent,
                modifier =
                    Modifier.hazeEffect(
                        state = hazeState,
                        style =
                            HazeMaterials.ultraThick(
                                MiuixTheme.colorScheme.surface,
                            ),
                    ) {
                        progressive =
                            HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f,
                            )
                    },
            )
        },
    ) { paddingValues ->
        LibraryContent(
            modifier =
                Modifier
                    .fillMaxSize(),
            scrollBehavior,
            paddingValues,
            hazeState,
        )
    }
}

/**
 * 媒体库内容列表
 */
@Composable
private fun LibraryContent(
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior,
    paddingValues: PaddingValues,
    hazeState: HazeState,
) {
    val backStack = LocalNavBackStack.current

    // 媒体库列表项数据
    val libraryItems =
        listOf(
            LibraryItem("所有歌曲", Icons.Default.Home, Screen.AllSongs),
            LibraryItem("歌单", Icons.AutoMirrored.Filled.List, Screen.Playlists),
            LibraryItem("专辑", Icons.Default.Star, Screen.Albums),
            LibraryItem("艺术家", Icons.Default.Person, Screen.Artists),
            LibraryItem("最近添加", Icons.Default.Add, Screen.RecentlyAdded),
            LibraryItem("最常播放", Icons.Default.Favorite, Screen.MostPlayed),
            LibraryItem("播放历史", Icons.AutoMirrored.Filled.PlaylistPlay, Screen.PlayHistory),
            LibraryItem("文件夹", Icons.Default.Home, Screen.Folders),
        )

    LazyVerticalGrid(
        modifier =
            modifier
                .hazeSource(hazeState)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(
            span = { GridItemSpan(2) },
        ) {
            SmallTitle(
                text = "快速浏览",
                insideMargin = PaddingValues(start = 16.dp, top = 8.dp),
            )
        }
        items(libraryItems) { item ->
            LibraryItemCard(
                title = item.title,
                icon = item.icon,
                onClick = {
                    backStack.add(item.screen)
                },
            )
        }
    }
}

/**
 * 媒体库列表项卡片
 */
@Composable
private fun LibraryItemCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val delayMillis = remember(title, icon) { Random.nextInt(0, 220) }
    val durationMillis = remember(title, icon) { Random.nextInt(260, 720) }

    val animationSpec =
        remember(delayMillis, durationMillis) {
            tween<Float>(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
            )
        }

    ShowOnIdleContent(
        true,
        modifier =
            modifier
                .fillMaxWidth()
                .height(80.dp),
        enter =
            fadeIn(
                animationSpec = animationSpec,
            ) + scaleIn(animationSpec = animationSpec, initialScale = 0f),
        exit = fadeOut(),
    ) {
        Card(
            onClick = onClick,
            pressFeedbackType = PressFeedbackType.Tilt,
            cornerRadius = 10.dp,
            modifier =
                modifier
                    .fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Text(
                    text = title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp,
                            ),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body2,
                )

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(64.dp)
                            .rotate(45f)
                            .graphicsLayer {
                                translationX = 16.dp.toPx()
                                translationY = 10.dp.toPx()
                            }.background(
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            MiuixTheme.colorScheme.primaryVariant,
                                            MiuixTheme.colorScheme.primary,
                                            MiuixTheme.colorScheme.primaryContainer,
                                        ),
                                ),
                                shape = Shapes.SmallCornerBasedShape,
                            ),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimaryContainer,
                        modifier =
                            Modifier
                                .size(32.dp),
                    )
                }
            }
        }
    }
}

/**
 * 媒体库列表项数据类
 */
private data class LibraryItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen,
)
