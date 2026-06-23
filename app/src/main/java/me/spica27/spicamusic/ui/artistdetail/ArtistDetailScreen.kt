package me.spica27.spicamusic.ui.artistdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.calculateLuminance
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// ── 滚动驱动常量（像素）──────────────────────────────────────────────────────
private const val SCROLL_ART_RANGE = 480f
private const val SCROLL_HDR_START = 280f
private const val SCROLL_HDR_RANGE = 200f

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

    val coverUri = remember(artist) { artist.getCoverUri() }
    val dominantColor =
        rememberDominantColorFromUri(uri = coverUri, fallbackColor = Color(0xFF1E1E2E))
    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = spring(stiffness = 50f),
        label = "dominantColor",
    )
    val luminance = remember(dominantColor) { calculateLuminance(dominantColor) }
    val onDominantColor = if (luminance > 0.65f) Color.Black else Color.White

    val lazyListState = rememberLazyListState()
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidthDp = remember { 375.dp }

    val rawOffset by remember(lazyListState) {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                SCROLL_ART_RANGE
            } else {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    val artProgress by remember {
        derivedStateOf { (rawOffset / SCROLL_ART_RANGE).coerceIn(0f, 1f) }
    }
    val hdrProgress by remember {
        derivedStateOf { ((rawOffset - SCROLL_HDR_START) / SCROLL_HDR_RANGE).coerceIn(0f, 1f) }
    }

    val springDp = remember { spring<Dp>(stiffness = 400f) }
    val springFloat = remember { spring<Float>(stiffness = 400f) }

    val artTopExpanded = statusBarTopDp + HEADER_HEIGHT + 4.dp
    val artTopCollapsed = statusBarTopDp + (HEADER_HEIGHT - ART_COLLAPSED) / 2f
    val artStartExpanded = (screenWidthDp - ART_EXPANDED) / 2f

    val artSize by animateDpAsState(
        targetValue = lerp(ART_EXPANDED.value, ART_COLLAPSED.value, artProgress).dp,
        animationSpec = springDp,
        label = "artSize",
    )
    val artTop by animateDpAsState(
        targetValue = lerp(artTopExpanded.value, artTopCollapsed.value, artProgress).dp,
        animationSpec = springDp,
        label = "artTop",
    )
    val artStart by animateDpAsState(
        targetValue = lerp(artStartExpanded.value, 56f, artProgress).dp,
        animationSpec = springDp,
        label = "artStart",
    )
    val cornerRad by animateDpAsState(
        targetValue = lerp(ART_EXPANDED.value / 2f, 8f, artProgress).dp,
        animationSpec = springDp,
        label = "cornerRad",
    )

    val bigAlpha by animateFloatAsState(
        targetValue = (1f - artProgress * 2.5f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "bigAlpha",
    )
    val smallAlpha by animateFloatAsState(
        targetValue = (hdrProgress * 2f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "smallAlpha",
    )
    val hdrAlpha by animateFloatAsState(
        targetValue = hdrProgress,
        animationSpec = springFloat,
        label = "hdrAlpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 主色调渐变背景
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT + ART_EXPANDED + 100.dp)
                .background(
                    Brush.verticalGradient(
                        0f to animatedDominantColor.copy(alpha = 0.90f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                ),
        )

        // 可滚动内容
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            contentPadding =
                PaddingValues(
                    top = statusBarTopDp + HEADER_HEIGHT + ART_EXPANDED + 12.dp,
                    bottom = 200.dp,
                ),
        ) {
            // 歌手信息大字区
            item(key = "artist_header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                        .graphicsLayer { alpha = bigAlpha },
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

            item { Spacer(Modifier.height(340.dp)) }
        }

        // 固定顶栏遮罩
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.background.copy(alpha = hdrAlpha)),
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
                            .graphicsLayer { alpha = smallAlpha },
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
                    .padding(start = artStart, top = artTop)
                    .size(artSize)
                    .clip(RoundedCornerShape(cornerRad)),
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
        LandscapistImage(
            imageModel = { song.getCoverUri() },
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
