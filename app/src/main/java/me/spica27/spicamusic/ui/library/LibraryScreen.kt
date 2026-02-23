@file:OptIn(ExperimentalHazeMaterialsApi::class)

package me.spica27.spicamusic.ui.library

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.paging.compose.collectAsLazyPagingItems
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.java.KoinJavaComponent
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
fun LibraryScreen(modifier: Modifier = Modifier) {
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
    val albumsItems = viewModel.albumList.collectAsLazyPagingItems()
    val localNavSharedTransitionScope = LocalNavSharedTransitionScope.current
    val localNavAnimatedContentScope = LocalNavAnimatedContentScope.current
    LazyVerticalGrid(
        modifier =
            modifier
                .hazeSource(hazeState)
                .overScrollVertical()
                .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = rememberLazyGridState(),
    ) {
        // 歌单分区：显示已有歌单或引导创建
        item(span = { GridItemSpan(2) }) {
            Title(
                text = stringResource(R.string.title_playlist),
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
                        with(localNavSharedTransitionScope) {
                            PlaylistMiniCard(
                                modifier =
                                    Modifier.sharedBounds(
                                        sharedContentState =
                                            rememberSharedContentState(
                                                playlist,
                                            ),
                                        animatedVisibilityScope = localNavAnimatedContentScope,
                                    ),
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
        }
        // 专辑分区：展示最多 4 张专辑预览或占位提示去重新扫描
        item(span = { GridItemSpan(2) }) {
            Title(
                text = stringResource(R.string.albums_title),
                summary = "浏览你的专辑收藏",
                rightWidget = { ViewAllButton { backStack.add(Screen.Albums) } },
            )
        }

        if (albumsItems.itemCount == 0) {
            item(span = { GridItemSpan(2) }) {
                EmptyAlbumCard(
                    onRescan = { backStack.add(Screen.MediaLibrarySource) },
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        } else {
            val displayCount = minOf(albumsItems.itemCount, 4)
            items(displayCount) { index ->
                val album = albumsItems[index]
                if (album != null) {
                    with(localNavSharedTransitionScope) {
                        AlbumMiniCard(
                            modifier =
                                Modifier
                                    .padding(
                                        start = if (index % 2 == 0) 16.dp else 0.dp,
                                        end = if (index % 2 == 0) 0.dp else 16.dp,
                                    ).sharedBounds(
                                        sharedContentState =
                                            rememberSharedContentState(
                                                album,
                                            ),
                                        animatedVisibilityScope = localNavAnimatedContentScope,
                                    ),
                            album = album,
                            onClick = { backStack.add(Screen.AlbumDetail(album)) },
                        )
                    }
                } else {
                    AlbumMiniPlaceholder()
                }
            }
        }
        item(
            span = { GridItemSpan(2) },
        ) {
            Title(
                text = "歌曲推荐",
                summary = "基于你的听歌习惯推荐",
                rightWidget = {
                    val recommended by viewModel.recommendedSongs.collectAsStateWithLifecycle(
                        initialValue = emptyList(),
                    )
                    val player: IMusicPlayer =
                        KoinJavaComponent
                            .getKoin()
                            .get()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clip(Shapes.LargeCornerBasedShape)
                                .background(
                                    MiuixTheme.colorScheme.primaryContainer,
                                ).clickable(onClick = {
                                    // 播放全部推荐，从第一个开始
                                    val mediaIds = recommended.map { it.mediaStoreId.toString() }
                                    if (mediaIds.isNotEmpty()) {
                                        player.doAction(
                                            PlayerAction.UpdateList(
                                                mediaIds = mediaIds,
                                                mediaId = mediaIds.first(),
                                                start = true,
                                            ),
                                        )
                                    }
                                })
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play_recommendation),
                            tint = MiuixTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.play),
                            color = MiuixTheme.colorScheme.onPrimaryContainer,
                            style = MiuixTheme.textStyles.subtitle,
                        )
                    }
                },
            )
        }
        // 推荐歌曲列表
        item(span = { GridItemSpan(2) }) {
            val recommended by viewModel.recommendedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
            if (recommended.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_recommendations),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    style = MiuixTheme.textStyles.subtitle,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            } else {
                val player: IMusicPlayer =
                    KoinJavaComponent
                        .getKoin()
                        .get()
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recommended) { song ->
                        Card(
                            onClick = {
                                // 点击某一项，从该项开始播放整个推荐列表
                                val mediaIds = recommended.map { it.mediaStoreId.toString() }
                                player.doAction(
                                    PlayerAction.UpdateList(
                                        mediaIds = mediaIds,
                                        mediaId = song.mediaStoreId.toString(),
                                        start = true,
                                    ),
                                )
                            },
                            pressFeedbackType = PressFeedbackType.Sink,
                            cornerRadius = 10.dp,
                            modifier = Modifier.size(width = 180.dp, height = 100.dp),
                        ) {
                            Box(
                                Modifier.fillMaxSize(),
                            ) {
                                AudioCover(
                                    uri = song.getCoverUri(),
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .hazeEffect(
                                                style =
                                                    HazeMaterials.ultraThick(
                                                        MiuixTheme.colorScheme.surface,
                                                    ),
                                            ) {
                                                progressive =
                                                    HazeProgressive.LinearGradient(
                                                        startIntensity = 0.45f,
                                                        endIntensity = 0f,
                                                    )
                                            },
                                    placeHolder = {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
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
                                            Text(
                                                text = song.displayName,
                                                style = MiuixTheme.textStyles.headline1,
                                                color =
                                                    MiuixTheme.colorScheme.onSurfaceVariantActions.copy(
                                                        alpha = 0.3f,
                                                    ),
                                                modifier =
                                                    Modifier
                                                        .padding(12.dp)
                                                        .scale(2f)
                                                        .rotate(-45f),
                                            )
                                        }
                                    },
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = song.displayName,
                                        style = MiuixTheme.textStyles.body1,
                                        maxLines = 2,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = song.artist,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item(
            span = { GridItemSpan(2) },
        ) {
            Title(
                text = stringResource(R.string.listening_stats),
                summary = "查看你的听歌数据和习惯",
                rightWidget = { ViewAllButton { } },
            )
        }
        // 本周听歌统计卡片：展示本周听歌总时长、播放次数和听过的歌曲数
        item(span = { GridItemSpan(2) }) {
            val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle(initialValue = null)
            WeeklyStatsCard(
                weeklyStats = weeklyStats,
                onRefresh = { viewModel.refreshWeeklyStats() },
            )
        }
        item(span = { GridItemSpan(2) }) {
            Text(
                text = stringResource(R.string.quick_action),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.subtitle,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(libraryItems.size) { index ->
            LibraryItemCard(
                title = libraryItems[index].title,
                icon = libraryItems[index].icon,
                onClick = { backStack.add(libraryItems[index].screen) },
                Modifier.padding(
                    start = if (index % 2 == 0) 16.dp else 0.dp,
                    end = if (index % 2 == 0) 0.dp else 16.dp,
                ),
            )
        }
        item {
            Spacer(modifier = Modifier.height(280.dp))
        }
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
            text = stringResource(R.string.view_all),
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
        pressFeedbackType = PressFeedbackType.Sink,
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
                text = stringResource(R.string.no_playlists),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.create_first_playlist),
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
    modifier: Modifier,
    playlist: Playlist,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        cornerRadius = 10.dp,
        modifier = modifier.fillMaxWidth(),
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
 * 专辑占位卡片 - 当没有专辑时显示
 */
@Composable
private fun EmptyAlbumCard(
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onRescan,
        pressFeedbackType = PressFeedbackType.Sink,
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
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.no_albums),
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.try_rescan_media_library),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun AlbumMiniCard(
    modifier: Modifier,
    album: Album,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        cornerRadius = 10.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .size(120.dp)
                        .clip(Shapes.SmallCornerBasedShape),
                contentAlignment = Alignment.Center,
            ) {
                AudioCover(
                    uri = album.artworkUri?.toUri(),
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors =
                                                listOf(
                                                    MiuixTheme.colorScheme.tertiaryContainer,
                                                    MiuixTheme.colorScheme.surfaceContainerHigh,
                                                ),
                                        ),
                                    ).padding(
                                        12.dp,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = album.title,
                                style = MiuixTheme.textStyles.headline1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                            )
                        }
                    },
                )
            }
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            Text(
                text = album.title,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
            )
            Spacer(
                modifier = Modifier.height(4.dp),
            )
            Text(
                text = album.artist,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(
                modifier = Modifier.height(10.dp),
            )
        }
    }
}

@Composable
private fun AlbumMiniPlaceholder() {
    Card(
        pressFeedbackType = PressFeedbackType.Sink,
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
                                        MiuixTheme.colorScheme.surfaceContainer,
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
            Spacer(modifier = Modifier.height(8.dp))
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
            pressFeedbackType = PressFeedbackType.Sink,
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
                    contentAlignment = Alignment.Center,
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
@Composable
private fun WeeklyStatsCard(
    weeklyStats: PlayStats?,
    onRefresh: () -> Unit,
) {
    Card(
        onClick = {},
        pressFeedbackType = PressFeedbackType.None,
        cornerRadius = 10.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        weeklyStats?.let { formatDuration(it.totalPlayedDuration) } ?: "—",
                        style = MiuixTheme.textStyles.title1,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        "本周听歌时长",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        weeklyStats?.playEventCount?.toString() ?: "—",
                        style = MiuixTheme.textStyles.title1,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        "播放次数",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        weeklyStats?.uniqueSongCount?.toString() ?: "—",
                        style = MiuixTheme.textStyles.title1,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        "听过歌曲数",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "刷新",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    modifier =
                        Modifier
                            .clip(Shapes.MediumCornerBasedShape)
                            .clickable(onClick = onRefresh)
                            .padding(8.dp),
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

private data class LibraryItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen,
)
