package me.spica27.spicamusic.ui.home.pages

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollHorizontal
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.random.Random

// 媒体库快捷入口列表（静态数据，提取到顶层避免每次重组重建）
private val libraryItems =
    listOf(
        LibraryItem("所有歌曲", Icons.Default.Home, Screen.AllSongs),
        LibraryItem("歌单", Icons.AutoMirrored.Filled.List, Screen.Playlists),
        LibraryItem("专辑", Icons.Default.Star, Screen.Albums),
        LibraryItem("艺术家", Icons.Default.Person, Screen.Artists),
        LibraryItem("最近添加", Icons.Default.Add, Screen.RecentlyAdded),
        LibraryItem("最常播放", Icons.Default.AllInbox, Screen.MostPlayed),
        LibraryItem("我喜爱的", Icons.Default.Favorite, Screen.Favorite),
        LibraryItem("文件夹", Icons.Default.Home, Screen.Folders),
    )

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
    viewModel: LibraryPageViewModel = koinActivityViewModel(),
) {
    val backStack = LocalNavBackStack.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())

    LazyVerticalGrid(
        modifier =
            modifier
                .hazeSource(hazeState)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 歌单分区：显示已有歌单或引导创建
        item(span = { GridItemSpan(2) }) {
            Title(
                text = "歌单",
                summary = "浏览你创建的歌单",
                rightWidget = { ViewAllButton { backStack.add(Screen.Playlists) } },
            )
        }

        if (playlists.isEmpty()) {
            // 未创建歌单时展示占位卡片，点击进入歌单页面去创建
            item(span = { GridItemSpan(2) }) {
                EmptyPlaylistCard(
                    onClick = { backStack.add(Screen.Playlists) },
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        } else {
            item(
                span = { GridItemSpan(2) },
            ) {
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .overScrollHorizontal(),
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 显示最多 4 个歌单作为预览
                    val display = playlists.take(4)
                    this@LazyRow.items(display, key = { it.playlistId ?: 0L }) { playlist ->
                        PlaylistMiniCard(
                            playlist = playlist,
                            onClick = {
                                playlist.playlistId?.let { id ->
                                    backStack.add(
                                        Screen.PlaylistDetail(
                                            id,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
        librarySection(
            title = "专辑",
            summary = "浏览你的专辑收藏",
            placeholderText = "（专辑列表暂未实现，敬请期待）",
            rightWidget = { ViewAllButton { backStack.add(Screen.Albums) } },
        )
        librarySection(
            title = "为你推荐",
            summary = "基于你的听歌习惯推荐的歌单和专辑",
            placeholderText = "（推荐功能暂未实现，敬请期待）",
            rightWidget = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .background(
                                MiuixTheme.colorScheme.primaryContainer,
                                shape = Shapes.LargeCornerBasedShape,
                            ).padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放推荐",
                        tint = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "播放",
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                        style = MiuixTheme.textStyles.subtitle,
                    )
                }
            },
        )
        librarySection(
            title = "听歌统计",
            summary = "查看你的听歌习惯和历史数据",
            placeholderText = "（听歌统计功能暂未实现，敬请期待）",
            rightWidget = { ViewAllButton { } },
        )
        item(span = { GridItemSpan(2) }) {
            Text(
                "快捷入口",
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                style = MiuixTheme.textStyles.subtitle,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(libraryItems.size) { index ->
            LibraryItemCard(
                modifier =
                    Modifier.padding(
                        start = ((1 - index % 2) * 16).dp, // 左边距偶数项为0，奇数项为10dp
                        end = ((index % 2) * 16).dp, // 右边距偶数项为10dp，奇数项为0
                    ),
                title = libraryItems[index].title,
                icon = libraryItems[index].icon,
                onClick = { backStack.add(libraryItems[index].screen) },
            )
        }
        item {
            Spacer(modifier = Modifier.height(280.dp))
        }
    }
}

/**
 * 通用 section 扩展：包含标题行和占位提示文本
 */
private fun LazyGridScope.librarySection(
    title: String,
    summary: String,
    placeholderText: String,
    rightWidget: @Composable () -> Unit = {},
) {
    item(span = { GridItemSpan(2) }) {
        Title(text = title, summary = summary, rightWidget = rightWidget)
    }
    item(span = { GridItemSpan(2) }) {
        Text(
            placeholderText,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            style = MiuixTheme.textStyles.subtitle,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

/**
 * 通用"查看全部"按钮
 */
@Composable
private fun ViewAllButton(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .clip(Shapes.LargeCornerBasedShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            "查看全部",
            color = MiuixTheme.colorScheme.onTertiaryContainer,
            style = MiuixTheme.textStyles.subtitle,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = MiuixIcons.Basic.ArrowRight,
            contentDescription = "more",
            tint = MiuixTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/**
 * 歌单占位卡片 - 当用户没有歌单时显示
 */
@Composable
private fun EmptyPlaylistCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Tilt,
        cornerRadius = 10.dp,
        modifier =
            modifier
                .fillMaxWidth()
                .height(160.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "还没有歌单",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "创建您的第一个歌单",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * 小型歌单卡片 (用于 LibraryPage 列表展示)
 */
@Composable
private fun PlaylistMiniCard(
    playlist: me.spica27.spicamusic.common.entity.Playlist,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Tilt,
        cornerRadius = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .size(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MiuixTheme.colorScheme.tertiaryContainer,
                                        MiuixTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                )
            }

            Text(
                text = playlist.playlistName,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * 媒体库列表项卡片
 */
@Composable
private fun LibraryItemCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val durationMillis = remember(title, icon) { Random.nextInt(260, 720) }

    val animationSpec =
        remember(durationMillis) {
            tween<Float>(
                durationMillis = durationMillis,
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
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun Title(
    text: String,
    summary: String,
    rightWidget: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.W600,
            )
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                style = MiuixTheme.textStyles.body2,
            )
        }
        Spacer(
            modifier = Modifier.width(8.dp),
        )
        rightWidget()
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
