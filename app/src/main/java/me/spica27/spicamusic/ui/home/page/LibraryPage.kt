package me.spica27.spicamusic.ui.home.page

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.delay
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.model.PlaylistWithCover
import me.spica27.spicamusic.ui.navigation.FavoriteRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.PlaylistCreatorRoute
import me.spica27.spicamusic.ui.navigation.PlaylistDetailRoute
import me.spica27.spicamusic.ui.navigation.ScannerRoute
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.ScaleEnterFrom
import me.spica27.spicamusic.ui.theme.ScaleExitTo
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 资料库页面
 */

/** 大标题收缩归一化距离的上限（实际取刊头实测滚出高度，见 mastheadCollapse） */
private val MastheadCollapseDistance = 140.dp

/** 参与入场编排的最大歌单卡数（之后的卡片直接呈现） */
private const val ENTRANCE_MAX_CARD = 6

/** 首屏元素在编排中的槽位：刊头=0 操作行=1 收藏区头=2 歌单区头=3 歌单卡从 4 开始 */
private const val ENTRANCE_ORDER_CARD_BASE = 2

/** 收藏预览最多展示的歌曲数 */
private const val FavoritePreviewSongCount = 5

@Composable
fun LibraryPage() {
    val backStack = LocalBackStack.current
    val viewModel: LibraryPageViewModel = koinActivityViewModel()
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current

    val playlists by viewModel.playlistsWithCover.collectAsStateWithLifecycle()
    val favoriteSongs by homeViewModel.favoriteSongs.collectAsStateWithLifecycle()
    val snackbarMessage by homeViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val favoritePreviewSongs =
        remember(favoriteSongs) {
            ImmutableList.copyOf(favoriteSongs.take(FavoritePreviewSongCount))
        }
    val favoritePlaylistName = stringResource(R.string.finder_favorites_playlist_name)

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT).show()
        homeViewModel.clearSnackbar()
    }

    var playEntrance by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            delay(1400)
            playEntrance = false
        }
    }

    val gridState = rememberLazyGridState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(LocalBottomBarScrollConnection.current),
            contentPadding =
                PaddingValues(
                    start = LayoutTokens.MusicHeaderHorizontalPadding,
                    end = LayoutTokens.MusicHeaderHorizontalPadding,
                    top = statusBarTop + 56.dp,
                    bottom = 200.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "masthead", span = { GridItemSpan(maxLineSpan) }, contentType = "masthead") {
                LibraryMasthead(
                    playlistCount = playlists.size,
                    modifier =
                        Modifier
                            .padding(top = Spacing.Large)
                            .entrance(order = 0, play = playEntrance)
                            .graphicsLayer {
                                val t = mastheadCollapse(gridState)
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = 1f - t
                                translationY = -t * 16.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }

            item(key = "actions", span = { GridItemSpan(maxLineSpan) }, contentType = "actions") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Small)
                            .entrance(order = 1, play = playEntrance),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    LibraryCommandPill(
                        text = stringResource(R.string.create_playlist),
                        icon = Icons.Default.Add,
                        container = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { backStack.add(PlaylistCreatorRoute) },
                        modifier = Modifier.weight(1f),
                    )
                    LibraryCommandPill(
                        text = stringResource(R.string.scan_music),
                        icon = Icons.Default.Scanner,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { backStack.add(ScannerRoute) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(
                key = "favorites_header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "section_header",
            ) {
                SectionHeader(
                    title = stringResource(R.string.my_favorites),
                    subtitle = stringResource(R.string.songs_count_format, favoriteSongs.size),
                    actionLabel = stringResource(R.string.finder_more).takeIf { favoriteSongs.isNotEmpty() },
                    onActionClick =
                        {
                            backStack.add(FavoriteRoute)
                            Unit
                        }.takeIf { favoriteSongs.isNotEmpty() },
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = null,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ).padding(top = Spacing.Medium),
                )
            }

            if (favoriteSongs.isEmpty()) {
                item(
                    key = "favorites_empty",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "empty",
                ) {
                    FavoritesEmptyRow(
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = null,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ),
                    )
                }
            } else {
                item(
                    key = "favorites_card",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "favorites",
                ) {
                    FavoritesCard(
                        songs = favoritePreviewSongs,
                        onPlayAll = {
                            playerViewModel.updatePlaylistWithSongs(
                                songs = favoriteSongs,
                                startSong = favoriteSongs.firstOrNull(),
                                autoStart = true,
                            )
                        },
                        onSongClick = { song ->
                            playerViewModel.updatePlaylistWithSongs(
                                songs = favoriteSongs,
                                startSong = song,
                                autoStart = true,
                            )
                        },
                        onSaveAsPlaylist = {
                            homeViewModel.createPlaylistFromSongs(
                                songs = favoriteSongs,
                                playlistName = favoritePlaylistName,
                            )
                        },
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = null,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ),
                    )
                }
            }

            item(
                key = "playlists_header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "section_header",
            ) {
                Text(
                    text = stringResource(R.string.my_playlists),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .padding(top = Spacing.Medium)
                            .entrance(order = 3, play = playEntrance),
                )
            }

            if (playlists.isEmpty()) {
                item(
                    key = "playlists_empty",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "empty",
                ) {
                    PlaylistsEmptyState(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = ListItemFadeInSpec,
                                    placementSpec = null,
                                    fadeOutSpec = ListItemFadeOutSpec,
                                ),
                    )
                }
            } else {
                itemsIndexed(
                    items = playlists,
                    key = { _, item ->
                        item.playlist.playlistId ?: item.playlist.playlistName
                            .hashCode()
                            .toLong()
                    },
                    contentType = { _, _ -> "playlist" },
                ) { index, item ->
                    val cardModifier =
                        Modifier.animateItem(
                            fadeInSpec = ListItemFadeInSpec,
                            placementSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                ),
                            fadeOutSpec = ListItemFadeOutSpec,
                        )
                    val entranceModifier =
                        if (playEntrance && index < ENTRANCE_MAX_CARD) {
                            cardModifier.entrance(
                                order = ENTRANCE_ORDER_CARD_BASE + index,
                                play = true,
                            )
                        } else {
                            cardModifier
                        }
                    PlaylistCard(
                        item = item,
                        onClick = { backStack.add(PlaylistDetailRoute(item.playlist)) },
                        modifier = entranceModifier,
                    )
                }
            }
        }

        LibraryTopBar(
            gridState = gridState,
            onCreateClick = { backStack.add(PlaylistCreatorRoute) },
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

/**
 * 大标题收缩进度：0f=完全展开 1f=完全收进顶栏（在 Draw 阶段读取，滚动零重组）
 */
private fun Density.mastheadCollapse(gridState: LazyGridState): Float {
    if (gridState.firstVisibleItemIndex > 0) return 1f
    val layoutInfo = gridState.layoutInfo
    val masthead = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
    val scrollOutDistance =
        (masthead.size.height + layoutInfo.mainAxisItemSpacing)
            .toFloat()
            .coerceIn(1f, MastheadCollapseDistance.toPx())
    return (gridState.firstVisibleItemScrollOffset / scrollOutDistance).coerceIn(0f, 1f)
}

/** 固定顶栏：背景与标题透明度跟随刊头收缩进度，收起后弹出迷你「新建歌单」药丸 */
@Composable
private fun LibraryTopBar(
    gridState: LazyGridState,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    val solid by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 56.dp)
                .drawBehind {
                    drawRect(color = backgroundColor.copy(alpha = mastheadCollapse(gridState)))
                },
    ) {
        if (solid) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = mastheadCollapse(gridState) },
            )
            AnimatedVisibility(
                visible = solid,
                enter =
                    scaleIn(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        initialScale = ScaleEnterFrom,
                    ) + fadeIn(tween(durationMillis = 160)),
                exit =
                    scaleOut(
                        animationSpec = tween(durationMillis = 140),
                        targetScale = ScaleExitTo,
                    ) + fadeOut(tween(durationMillis = 140)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickHighlight(onClick = onCreateClick)
                            .padding(horizontal = Spacing.Medium, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.create_playlist),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 刊头：大标题 + 歌单计数 meta 行（计数全页唯一） */
@Composable
private fun LibraryMasthead(
    playlistCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(modifier = Modifier.padding(top = 6.dp)) {
            RollingPlaylistCount(
                playlistCount = playlistCount,
            )
        }
    }
}

/** 歌单计数：数字变化时上下滚动切换 */
@Composable
private fun RollingPlaylistCount(
    playlistCount: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = playlistCount,
        transitionSpec = {
            val direction = if (targetState >= initialState) 1 else -1
            (
                slideInVertically { height -> direction * height / 2 } +
                    fadeIn(tween(durationMillis = 240))
            ) togetherWith
                (
                    slideOutVertically { height -> -direction * height / 2 } +
                        fadeOut(tween(durationMillis = 160))
                ) using SizeTransform(clip = false)
        },
        modifier = modifier,
        label = "libraryPlaylistCountRoll",
    ) { count ->
        Text(
            text = stringResource(R.string.library_summary_playlists, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** 命令药丸：52dp 圆胶囊 + 按压回弹（收藏页 HeroCommandPill 同款，无心跳脉冲） */
@Composable
private fun LibraryCommandPill(
    text: String,
    icon: ImageVector,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "libraryCommandPillPressScale",
    )
    Box(
        modifier =
            modifier
                .height(52.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }.clip(CircleShape)
                .background(container)
                .clickHighlight(interactionSource = interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

/** 分区头：标题 + 计数 meta + 可选「更多」胶囊 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier =
                    Modifier
                        .padding(start = Spacing.Small)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickHighlight(onClick = onActionClick)
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 收藏预览卡：静置容器色 */
@Composable
private fun FavoritesCard(
    songs: ImmutableList<Song>,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSaveAsPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
            songs.forEach { song ->
                FavoriteSongRow(
                    song = song,
                    onClick = { onSongClick(song) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            ActionPill(
                text = stringResource(R.string.play_all),
                icon = Icons.Default.PlayArrow,
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
            )
            ActionPill(
                text = stringResource(R.string.finder_save_as_playlist),
                icon = Icons.Default.Add,
                onClick = onSaveAsPlaylist,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 收藏行：封面 + 歌名/歌手 + 时长 */
@Composable
private fun FavoriteSongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.LargeCornerBasedShape)
                .clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        AudioCover(
            uri = song.getCoverUri(),
            fallbackUri = song.getAlbumCoverUri(),
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(Shapes.MediumCornerBasedShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = song.getFormattedDuration(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** 动作药丸：次级容器色 + 按压回弹 */
@Composable
private fun ActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "libraryActionPillPressScale",
    )
    Row(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }.clip(Shapes.MediumCornerBasedShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickHighlight(interactionSource = interactionSource, onClick = onClick)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                Spacing.ExtraSmall,
                Alignment.CenterHorizontally,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 收藏空态行 */
@Composable
private fun FavoritesEmptyRow(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.LargeCornerBasedShape)
                .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.finder_no_favorites_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.finder_no_favorites_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 歌单卡：组合封面 + 名称 + 歌曲数 */
@Composable
private fun PlaylistCard(
    item: PlaylistWithCover,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .clickHighlight(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        PlaylistCoverView(
            albumIds = item.coverAlbumIds,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(Shapes.ExtraLargeCornerBasedShape),
        )
        Column(
            modifier = Modifier.padding(bottom = Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.playlist.playlistName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.songs_count, item.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 歌单空态：开放排版无卡片，音符轻盈浮动 */
@Composable
private fun PlaylistsEmptyState(modifier: Modifier = Modifier) {
    val floatTransition = rememberInfiniteTransition(label = "libraryEmptyFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "libraryEmptyBob",
    )
    Column(
        modifier = modifier.padding(top = Spacing.ExtraLarge, bottom = Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        translationY = bob * 5.dp.toPx()
                        rotationZ = bob * 6f
                    }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.no_playlists_yet),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.create_first_playlist_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
