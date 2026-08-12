package me.spica27.spicamusic.ui.artistdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.widget.CoverFallback
import me.spica27.spicamusic.ui.widget.OtherAlbumsShelf
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.calculateLuminance
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

// ── 折叠段划分（顶栏遮罩/标题在折叠后段淡入，对齐旧的 280..480 相对区间）───────────
private const val HDR_FADE_START = 0.58f

// ── 布局尺寸常量 ──────────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp
private val ART_EXPANDED = 200.dp
private val ART_COLLAPSED = 42.dp

@Composable
fun ArtistDetailScreen(artist: Artist) {
    val path = LocalNavigationPath.current
    val viewModel: ArtistDetailViewModel =
        koinViewModel(key = "ArtistDetailViewModel_${artist.name}") {
            parametersOf(artist.name)
        }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    val coverUri = remember(artist) { artist.getCoverUri() }
    val dominantColor =
        rememberDominantColorFromUri(uri = coverUri, fallbackColor = Color(0xFF1E1E2E))
    // stiffness 50f 要 1.5-2 秒落定；收敛到 200f 并保持 State 形态（只在 draw 读取）
    val animatedDominantColor =
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec = spring(stiffness = 200f),
            label = "dominantColor",
        )
    val luminance = remember(dominantColor) { calculateLuminance(dominantColor) }
    val onDominantColor = if (luminance > 0.65f) Color.Black else Color.White

    val lazyListState = rememberLazyListState()
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidthDp = remember { 375.dp }

    // 折叠量程 = 头像占位块高度。列表 item 0 是一个等高的 Spacer
    val artBlock = ART_EXPANDED + 12.dp
    val artBlockPx = with(LocalDensity.current) { artBlock.toPx() }

    // 全部保持为 State：只在 Layout/Draw 阶段读取，滚动时不重组
    val artProgressState =
        remember(lazyListState, artBlockPx) {
            derivedStateOf {
                if (lazyListState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (lazyListState.firstVisibleItemScrollOffset / artBlockPx).coerceIn(0f, 1f)
                }
            }
        }
    // 顶栏遮罩/标题在折叠后段淡入
    val hdrProgressState =
        remember(artProgressState) {
            derivedStateOf {
                ((artProgressState.value - HDR_FADE_START) / (1f - HDR_FADE_START)).coerceIn(0f, 1f)
            }
        }

    // 几何直接由滚动进度导出，不再经过弹簧（滚动本身是持续手势输入）

    val artTopExpanded = statusBarTopDp + HEADER_HEIGHT + 4.dp
    val artTopCollapsed = statusBarTopDp + (HEADER_HEIGHT - ART_COLLAPSED) / 2f
    val artStartExpanded = (screenWidthDp - ART_EXPANDED) / 2f

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 主色调渐变背景
        val backgroundColor = MaterialTheme.colorScheme.background
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT + ART_EXPANDED + 100.dp)
                // 渐变在 Draw 阶段构建：主色弹簧跑动期间只重绘，不重组
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to animatedDominantColor.value.copy(alpha = 0.90f),
                            1f to backgroundColor.copy(alpha = 0f),
                        ),
                    )
                },
        )

        // 可滚动内容
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            contentPadding =
                PaddingValues(
                    top = statusBarTopDp + HEADER_HEIGHT,
                    bottom = 200.dp,
                ),
        ) {
            // 头像占位：留白归列表自身，其高度即折叠量程（滚过它 progress 恰好到 1）
            item(key = "art_space", contentType = "art_space") {
                Spacer(Modifier.height(artBlock))
            }

            // 歌手信息大字区
            item(key = "artist_header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                        .graphicsLayer {
                            alpha = (1f - artProgressState.value * 2.5f).coerceIn(0f, 1f)
                        },
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = onDominantColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.songs_count, artist.songCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = onDominantColor.copy(alpha = 0.6f),
                    )
                }
            }

            item(key = "play_buttons") {
                ArtistPlayButtons(
                    onPlayAll = viewModel::playAll,
                    onShuffle = viewModel::playAll,
                )
            }

            items(songs, key = { it.mediaStoreId }) { song ->
                ArtistSongRow(
                    song = song,
                    onClick = { viewModel.playSongInList(song) },
                    onMore = { path.push(SongMenuScene(song)) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            // 来自该歌手的其他内容：设备中存在该歌手的专辑时展示横向列表
            if (albums.isNotEmpty()) {
                item(key = "other_albums", contentType = "other_albums") {
                    OtherAlbumsShelf(
                        artistName = artist.name,
                        albums = albums,
                        onAlbumClick = { path.push(AlbumDetailScene(it)) },
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(340.dp)) }
        }

        // 固定顶栏遮罩
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT)
                .align(Alignment.TopStart)
                .drawBehind { drawRect(backgroundColor.copy(alpha = hdrProgressState.value)) },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { path.popTop() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = artist.name,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 64.dp, end = 16.dp)
                            .graphicsLayer {
                                alpha = (hdrProgressState.value * 2f).coerceIn(0f, 1f)
                            },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 圆形歌手头像（浮动层，随滚动折叠为小圆角方形）
        LandscapistImage(
            imageModel = { coverUri },
            modifier =
                Modifier
                    // 位置/尺寸在 Layout 阶段按进度导出，圆角在 Draw 阶段插值：
                    // 滚动时只重新布局与重绘，不重组
                    .layout { measurable, _ ->
                        val p = artProgressState.value
                        val side =
                            lerp(ART_EXPANDED.toPx(), ART_COLLAPSED.toPx(), p)
                                .roundToInt()
                                .coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(side, side))
                        layout(side, side) {
                            placeable.place(
                                x = lerp(artStartExpanded.toPx(), 56.dp.toPx(), p).roundToInt(),
                                y = lerp(artTopExpanded.toPx(), artTopCollapsed.toPx(), p).roundToInt(),
                            )
                        }
                    }.graphicsLayer {
                        // 展开态为圆形（半径 = 尺寸一半），折叠态收敛到 8dp 圆角
                        val p = artProgressState.value
                        shape =
                            RoundedCornerShape(
                                lerp(ART_EXPANDED.toPx() / 2f, 8.dp.toPx(), p),
                            )
                        clip = true
                    },
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            failure = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }
}

@Composable
private fun ArtistPlayButtons(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedButton(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.play_all_songs))
        }
        ElevatedButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.shuffle_play))
        }
    }
}

@Composable
private fun ArtistSongRow(
    song: Song,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 每行缓存封面 Uri：getCoverUri() 每次都拼串 + Uri.parse，长列表 fling 时逐行分配
        val songCoverUri = remember(song.mediaStoreId) { song.getCoverUri() }
        LandscapistImage(
            imageModel = { songCoverUri },
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            failure = {
                CoverFallback(
                    fallbackUri = song.getAlbumCoverUri(),
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
