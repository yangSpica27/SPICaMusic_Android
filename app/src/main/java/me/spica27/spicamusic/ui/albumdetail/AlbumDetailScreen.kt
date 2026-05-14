package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.calculateLuminance
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

// ── Layout constants ──────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp
private val ART_EXPANDED = 200.dp
private val ART_COLLAPSED = 48.dp
private val INFO_HEIGHT = 104.dp

/** Linear interpolation for [Dp] values. */
private fun lerpDp(
    start: Dp,
    stop: Dp,
    fraction: Float,
): Dp = start + (stop - start) * fraction

@Composable
fun AlbumDetailScreen(album: Album) {
    val path = LocalNavigationPath.current
    val viewModel: AlbumDetailViewModel =
        koinViewModel(key = "AlbumDetailViewModel_${album.id}") { parametersOf(album.id) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    val coverUri = remember(album) { album.getCoverUri() }
    val dominantColor =
        rememberDominantColorFromUri(
            uri = coverUri,
            fallbackColor = Color(0xFF1E1E2E),
        )
    val animatedDominantColor by
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec = spring(stiffness = 50f),
            label = "dominantColor",
        )
    val luminance = remember(dominantColor) { calculateLuminance(dominantColor) }
    val onDominantColor = if (luminance > 0.55f) Color.Black else Color.White

    // ── Cumulative scroll tracking ────────────────────────────────────────────
    var rawScrollPx by remember { mutableFloatStateOf(0f) }
    val scrollTracker =
        remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // consumed.y is negative when scrolling down (content moves up)
                    rawScrollPx = (rawScrollPx - consumed.y).coerceAtLeast(0f)
                    return Offset.Zero
                }
            }
        }
    val scrollArtRangePx = with(density) { 280.dp.toPx() }

    // ── Animation progress derived from scroll ────────────────────────────────
    val artProgress by remember {
        derivedStateOf { (rawScrollPx / scrollArtRangePx).coerceIn(0f, 1f) }
    }
    val hdrProgress by remember {
        derivedStateOf {
            (
                (rawScrollPx - scrollArtRangePx * 0.55f) /
                    (scrollArtRangePx * 0.45f)
            ).coerceIn(0f, 1f)
        }
    }

    // Alpha values read in graphicsLayer (draw phase — no recomposition triggered)
    val bigAlpha by remember { derivedStateOf { (1f - artProgress * 2.2f).coerceIn(0f, 1f) } }
    val smallAlpha by remember { derivedStateOf { (hdrProgress * 2f).coerceIn(0f, 1f) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()

        // Pre-compute art position endpoints in px (stable values, no scroll dependency)
        val artExpandedPx = with(density) { ART_EXPANDED.toPx() }
        val artCollapsedPx = with(density) { ART_COLLAPSED.toPx() }
        val headerHeightPx = with(density) { HEADER_HEIGHT.toPx() }
        val artStartExpandedPx = (maxWidthPx - artExpandedPx) / 2f
        val artStartCollapsedPx = headerHeightPx + with(density) { 8.dp.toPx() }
        val artTopExpandedPx = headerHeightPx + with(density) { 16.dp.toPx() }
        val artTopCollapsedPx = (headerHeightPx - artCollapsedPx) / 2f

        // ── Gradient background ───────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to animatedDominantColor.copy(alpha = 0.95f),
                        0.3f to animatedDominantColor.copy(alpha = 0.55f),
                        0.55f to MaterialTheme.colorScheme.surface,
                        1f to MaterialTheme.colorScheme.surface,
                    ),
                ),
        )

        // ── Scrollable song list ──────────────────────────────────────────────
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollTracker),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            contentPadding =
                PaddingValues(
                    top = HEADER_HEIGHT + ART_EXPANDED + INFO_HEIGHT,
                    bottom = 200.dp,
                ),
        ) {
            item {
                PlayButtons(
                    onPlayAll = viewModel::playAll,
                    onShuffle = viewModel::playAll,
                )
            }
            items(songs, key = { it.mediaStoreId }) { song ->
                SongRow(
                    song = song,
                    onClick = { viewModel.playSongInList(song) },
                    onMore = { path.push(SongMenuScene(song)) },
                )
            }
            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
        }

        // ── Fixed header overlay (not scrollable) ─────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT + ART_EXPANDED + INFO_HEIGHT),
        ) {
            // Header bar background — fades in as sticky header appears
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .graphicsLayer { alpha = hdrProgress }
                    .background(animatedDominantColor),
            )

            // Back button (always visible)
            IconButton(
                onClick = { path.popTop() },
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp, start = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = onDominantColor,
                )
            }

            // Mini album title — appears in the sticky header on scroll
            Text(
                text = album.title,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 56.dp, end = 56.dp)
                        .graphicsLayer { alpha = smallAlpha },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = onDominantColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Album art — animates from centered/large to collapsed/left on scroll
            LandscapistImage(
                imageModel = { coverUri },
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                lerp(artStartExpandedPx, artStartCollapsedPx, artProgress).roundToInt(),
                                lerp(artTopExpandedPx, artTopCollapsedPx, artProgress).roundToInt(),
                            )
                        }.size(lerpDp(ART_EXPANDED, ART_COLLAPSED, artProgress))
                        .clip(RoundedCornerShape(lerpDp(20.dp, 8.dp, artProgress))),
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

            // Large info section — fades out as user scrolls down
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .graphicsLayer { alpha = bigAlpha },
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onDominantColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onDominantColor.copy(alpha = 0.8f),
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text =
                        buildString {
                            append("${album.numberOfSongs}首歌曲")
                            if (album.year > 0) append(" · ${album.year}")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = onDominantColor.copy(alpha = 0.6f),
                )
            }
        }
    }
}

// ── Play / Shuffle action row ─────────────────────────────────────────────────

@Composable
private fun PlayButtons(
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
            Text("播放全部")
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
            Text("随机播放")
        }
    }
}

// ── Song row item ─────────────────────────────────────────────────────────────

@Composable
private fun SongRow(
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
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
