package me.spica27.spicamusic.ui.albumdetail

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.home.page.CoverPlaceholder
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entranceGraphics
import me.spica27.spicamusic.ui.theme.rememberEntrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.OtherAlbumsShelf
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.calculateLuminance
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit

// ── 布局尺寸常量 ──────────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp // 固定顶栏内容区高度（不含状态栏）
private val COVER_EXPANDED_MAX = 236.dp // 封面展开尺寸上限（矮窗口按可用高度 34% 钳制）
private val COVER_COLLAPSED = 38.dp // 封面折叠尺寸（顶栏内容区内垂直居中）
private val COVER_COLLAPSED_START = 56.dp // 封面折叠后距屏幕左缘距离（返回按钮之后）

private val BOTTOM_PLAYER_RESERVED = 200.dp // 悬浮迷你播放器底部预留（全项目惯例值）

/**
 * 专辑详情页。
 */
@Composable
fun AlbumDetailScreen(album: Album) {
    val path = LocalNavigationPath.current
    val viewModel: AlbumDetailViewModel =
        koinViewModel(key = "AlbumDetailViewModel_${album.id}") { parametersOf(album.id) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val otherAlbums by viewModel.otherAlbums.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val playerViewModel = LocalPlayerViewModel.current
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val playingMediaId = currentMediaItem?.mediaId
    val playingFromThisAlbum =
        remember(playingMediaId, songs) {
            playingMediaId != null && playingMediaId in songs.map { it.mediaStoreId.toString() }
        }

    // 写入歌单成功/失败提示（Toast 由 ViewModel 递出，消费后清除）
    LaunchedEffect(toastMessage) {
        val message = toastMessage ?: return@LaunchedEffect
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT).show()
        viewModel.clearToast()
    }

    val coverUri = remember(album) { album.getCoverUri() }
    val dominantColor =
        rememberDominantColorFromUri(uri = coverUri, fallbackColor = Color(0xFF1E1E2E))
    val animatedDominantColor =
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec = spring(stiffness = 200f),
            label = "dominantColor",
        )
    val luminance = remember(dominantColor) { calculateLuminance(dominantColor) }
    val onDominantColor = if (luminance > 0.65f) Color.Black else Color.White

    val lazyListState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 展开封面：横屏/分屏矮窗口时按可用高度 34% 钳制
        val coverExpanded = COVER_EXPANDED_MAX.coerceAtMost(maxHeight * 0.34f)

        // 封面占位块高度 == 折叠量程：滚过 item 0 时 progress 恰好到 1，边界零断层
        val coverBlock = Spacing.Small + coverExpanded + Spacing.Medium
        val coverStartExpanded = (maxWidth - coverExpanded) / 2

        // 首屏入场瀑布：与歌单详情页同款节奏
        val headerEntrance = rememberEntrance(order = 1)
        val actionRowEntrance = rememberEntrance(order = 2)
        val listEntrancePlay = remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(55)
            listEntrancePlay.value = false
        }

        // 折叠进度：只在 draw/graphicsLayer 阶段调用 → 滚动全程零重组
        val coverBlockPx =
            with(androidx.compose.ui.platform.LocalDensity.current) {
                coverBlock.toPx()
            }

        // 简化的折叠进度计算：始终使用 coverBlockPx 作为分母
        // 通过在列表底部添加足够的空白来确保短内容也能滚动完整距离
        val collapseProgress: Density.() -> Float =
            remember(lazyListState, coverBlockPx) {
                {
                    if (lazyListState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (lazyListState.firstVisibleItemScrollOffset / coverBlockPx)
                            .coerceIn(0f, 1f)
                    }
                }
            }

        // ── 主色调渐变背景（随折叠淡出）──────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT + coverExpanded + 140.dp)
                .graphicsLayer { alpha = 1f - collapseProgress() }
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to animatedDominantColor.value.copy(alpha = 0.92f),
                            0.3f to animatedDominantColor.value.copy(alpha = 0.65f),
                            0.4f to animatedDominantColor.value.copy(alpha = 0.65f),
                            0.6f to animatedDominantColor.value.copy(alpha = 0.55f),
                            0.8f to animatedDominantColor.value.copy(alpha = 0.45f),
                            0.85f to animatedDominantColor.value.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                        ),
                    )
                },
        )

        // ── 歌曲列表 ────────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + HEADER_HEIGHT,
                    bottom = BOTTOM_PLAYER_RESERVED,
                ),
        ) {
            // 封面占位：留白属于列表自身
            item(key = "cover_space", contentType = "cover_space") {
                Spacer(Modifier.height(coverBlock))
            }

            // 专辑名 + 歌手 + meta 行（随滚动淡出）
            item(key = "album_header", contentType = "header") {
                AlbumHeader(
                    album = album,
                    songCount = songs.size,
                    totalDurationMs = songs.sumOf { it.duration },
                    onDominantColor = onDominantColor,
                    collapseProgress = collapseProgress,
                    modifier = Modifier.entranceGraphics(headerEntrance),
                )
            }

            // 播放 / 随机 + 设置 操作行
            item(key = "action_row", contentType = "actions") {
                AlbumActionRow(
                    isPlaying = isPlaying && playingFromThisAlbum,
                    onPlayAll = {
                        if (playingFromThisAlbum) viewModel.togglePlayPause() else viewModel.playAll()
                    },
                    onShuffle = viewModel::playAll,
                    modifier = Modifier.entranceGraphics(actionRowEntrance),
                )
            }

            items(
                count = songs.size,
                key = { index -> songs[index].mediaStoreId },
                contentType = { "song" },
            ) { index ->
                val entrance =
                    rememberEntrance(Math.min(3 + index, 8), play = listEntrancePlay.value)
                val song = songs[index]
                AlbumSongRow(
                    index = index + 1,
                    song = song,
                    isPlaying = playingMediaId == song.mediaStoreId.toString(),
                    onClick = { viewModel.playSongInList(song) },
                    onMore = { path.push(SongMenuScene(song)) },
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold,
                                    ),
                                fadeOutSpec = ListItemFadeOutSpec,
                            ).entranceGraphics(entrance),
                )
            }
            // 来自同一歌手的其他专辑
            if (otherAlbums.isNotEmpty()) {
                item(key = "other_albums", contentType = "other_albums") {
                    OtherAlbumsShelf(
                        artistName = album.artist,
                        albums = otherAlbums,
                        onAlbumClick = { path.push(AlbumDetailScene(it)) },
                        modifier = Modifier.padding(top = Spacing.ExtraLarge),
                    )
                }
            }
            item(
                key = "bottom_spacer",
                contentType = "bottom_spacer",
            ) {
                Spacer(Modifier.height(coverBlock + COVER_EXPANDED_MAX))
            }
        }

        // ── 浮动封面（滚动直接映射，全部运动收敛在一个 graphicsLayer）──────
        val collapsedScale = COVER_COLLAPSED / coverExpanded
        Box(
            Modifier
                .padding(
                    start = coverStartExpanded,
                    top = statusBarTop + HEADER_HEIGHT + Spacing.Small,
                ).size(coverExpanded)
                .graphicsLayer {
                    val p = collapseProgress()
                    val s = lerp(1f, collapsedScale, p)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = s
                    scaleY = s
                    translationX = lerp(0f, (COVER_COLLAPSED_START - coverStartExpanded).toPx(), p)
                    // 折叠终点在顶栏内容区内垂直居中（状态栏高度在展开/折叠位中相消）
                    translationY =
                        lerp(
                            0f,
                            ((HEADER_HEIGHT - COVER_COLLAPSED) / 2 - HEADER_HEIGHT - Spacing.Small)
                                .toPx(),
                            p,
                        )
                    clip = true
                    shape = Shapes.LargeCornerBasedShape
                }.dropShadow(
                    shape = RoundedCornerShape(16.dp),
                    shadow =
                        Shadow(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                            radius = 12.dp,
                            spread = 2.dp,
                        ),
                ),
        ) {
            AudioCover(
                uri = coverUri,
                placeHolder = { CoverPlaceholder() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── [L3] 固定顶栏（背景/标题在折叠尾段浮现）────────────────────────────
        val topBarBg = MaterialTheme.colorScheme.background
        val hairlineColor = MaterialTheme.colorScheme.outlineVariant
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT)
                .align(Alignment.TopStart)
                .drawBehind {
                    val scrollAlpha = ((collapseProgress() - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    drawRect(topBarBg.copy(alpha = scrollAlpha))
                    if (scrollAlpha > 0f) {
                        drawRect(
                            color = hairlineColor.copy(alpha = 0.14f * scrollAlpha),
                            topLeft =
                                androidx.compose.ui.geometry
                                    .Offset(0f, size.height - 1.dp.toPx()),
                            size = Size(size.width, 1.dp.toPx()),
                        )
                    }
                },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .align(Alignment.BottomCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { path.popTop() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = album.title,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = Spacing.ExtraSmall)
                            .graphicsLayer {
                                val a = ((collapseProgress() - 0.6f) / 0.3f).coerceIn(0f, 1f)
                                alpha = a
                                translationY = (1f - a) * 4.dp.toPx()
                            },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { path.push(AlbumMenuScene(album)) }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

// ── 专辑头部：标题 + 歌手 + meta ─────────────────────────────────────────────

@Composable
private fun AlbumHeader(
    album: Album,
    songCount: Int,
    totalDurationMs: Long,
    onDominantColor: Color,
    collapseProgress: Density.() -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large)
            .graphicsLayer {
                // 使用更激进的淡出：在进度 0.5 时完全透明
                // 短内容时也能确保 header 完全消失，不留白
                alpha = (1f - collapseProgress() * 2f).coerceIn(0f, 1f)
            },
    ) {
        Text(
            text = album.title,
            modifier = Modifier,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onDominantColor,
        )
        Spacer(Modifier.height(Spacing.ExtraSmall))
        Text(
            text = album.artist.ifBlank { stringResource(R.string.unknown_artist) },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = onDominantColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(Spacing.ExtraSmall))
        Text(
            text = albumMetaText(album, songCount, totalDurationMs),
            style = MaterialTheme.typography.bodySmall,
            color = onDominantColor.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** meta 行：年份 · N 首 · 总时长 */
@Composable
private fun albumMetaText(
    album: Album,
    songCount: Int,
    totalDurationMs: Long,
): String {
    val yearPart = if (album.year > 0) album.year.toString() else null
    val countPart = stringResource(R.string.songs_count, songCount)
    val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs) % 60
    val durationPart =
        when {
            totalDurationMs <= 0L -> null
            hours > 0 -> stringResource(R.string.hours_minutes, hours, minutes)
            minutes > 0 -> stringResource(R.string.minutes, minutes)
            else -> stringResource(R.string.less_than_1_minute)
        }
    return listOfNotNull(yearPart, countPart, durationPart).joinToString(" · ")
}

// ── 操作行：播放 / 随机 / 设置 ───────────────────────────────────────────────

@Composable
private fun AlbumActionRow(
    isPlaying: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large)
                .padding(top = Spacing.Small, bottom = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AlbumActionChip(
            text = stringResource(R.string.play_all),
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            onClick = onPlayAll,
            primary = true,
            modifier = Modifier.weight(1f),
        )
        AlbumActionChip(
            text = stringResource(R.string.shuffle_play),
            icon = Icons.Default.Shuffle,
            onClick = onShuffle,
            primary = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AlbumActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(
                    if (primary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ).clickHighlight(onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(Spacing.ExtraSmall, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint =
                if (primary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color =
                if (primary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            maxLines = 1,
        )
    }
}

// ── 歌曲行：音序号 / 播放均衡器 + 标题 + 时长 + 更多 ──────────────────────────

@Composable
private fun AlbumSongRow(
    index: Int,
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium)
                .clip(Shapes.MediumCornerBasedShape)
                .background(
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                    } else {
                        Color.Transparent
                    },
                ).clickHighlight(onClick = onClick)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                PlayingBarsIndicator()
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.getFormattedDuration(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 播放中指示：三根错相起伏的均衡器竖条（全局空态浮动图标同款节奏） */
@Composable
private fun PlayingBarsIndicator() {
    val transition = rememberInfiniteTransition(label = "albumPlayingBars")
    val barHeights =
        List(3) { i ->
            transition.animateFloat(
                initialValue = 4f,
                targetValue = 14f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = 420 + i * 130,
                                easing = FastOutSlowInEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "bar$i",
            )
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        barHeights.forEach { heightAnim ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(heightAnim.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
