package me.spica27.spicamusic.ui.albumdetail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.geometry.geometryTarget
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

// ── 滚动驱动常量（像素）────────────────────────────────────────────────────────
private const val SCROLL_ART_RANGE = 520f // 封面动画的滚动像素总范围
private const val SCROLL_HDR_START = 310f // 顶栏开始淡入时对应的滚动偏移量（像素）
private const val SCROLL_HDR_RANGE = 210f // 顶栏从不可见到完全可见经历的滚动范围（像素）

// ── 布局尺寸常量 ───────────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp // 固定顶栏的内容区高度（不含状态栏）
private val ART_EXPANDED = 220.dp // 封面展开尺寸
private val ART_COLLAPSED = 42.dp // 封面折叠尺寸

@Composable
fun AlbumDetailScreen(
    album: Album,
    /** 封面飞行过渡；不为 null 时，封面在过渡完成前隐藏，由浮层接管渲染 */
    geometryTransition: GeometryTransition? = null,
) {
    val path = LocalNavigationPath.current
    val viewModel: AlbumDetailViewModel =
        koinViewModel(key = "AlbumDetailViewModel_${album.id}") { parametersOf(album.id) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    val coverUri = remember(album) { album.getCoverUri() }
    val dominantColor =
        rememberDominantColorFromUri(uri = coverUri, fallbackColor = Color(0xFF1E1E2E))
    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = spring(stiffness = 50f),
        label = "dominantColor",
    )
    val luminance = remember(dominantColor) { calculateLuminance(dominantColor) }
    val onDominantColor = if (luminance > 0.55f) Color.Black else Color.White

    val lazyListState = rememberLazyListState()
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidthDp = remember { 375.dp }

    // ── 基于 LazyListState 的滚动偏移量（像素，上限为 SCROLL_ART_RANGE）─────────
    // 一旦用户滚动超过第一个 item，视为动画已完全结束
    val rawOffset by remember(lazyListState) {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                SCROLL_ART_RANGE
            } else {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    // artProgress：0 = 封面完全展开，1 = 封面完全折叠
    val artProgress by remember {
        derivedStateOf {
            (rawOffset / SCROLL_ART_RANGE).coerceIn(
                0f,
                1f,
            )
        }
    }
    // hdrProgress：0 = 固定顶栏不可见，1 = 固定顶栏完全可见
    val hdrProgress by remember {
        derivedStateOf { ((rawOffset - SCROLL_HDR_START) / SCROLL_HDR_RANGE).coerceIn(0f, 1f) }
    }

    // ── 封面几何属性（弹簧动画跟随滚动目标值变化）────────────────────────────────
    val springDp = remember { spring<Dp>(stiffness = 400f) }
    val springFloat = remember { spring<Float>(stiffness = 400f) }

    val artTopExpanded = statusBarTopDp + HEADER_HEIGHT + 4.dp
    // 折叠态：在 HEADER_HEIGHT 内容区内垂直居中
    val artTopCollapsed = statusBarTopDp + (HEADER_HEIGHT - ART_COLLAPSED) / 2f
    // 展开态：在屏幕上水平居中
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
        targetValue = lerp(16f, 8f, artProgress).dp,
        animationSpec = springDp,
        label = "cornerRad",
    )

    // ── 透明度动画 ─────────────────────────────────────────────────────────────
    // bigAlpha：封面下方的大字信息区 —— 封面移动时迅速淡出
    val bigAlpha by animateFloatAsState(
        targetValue = (1f - artProgress * 2.5f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "bigAlpha",
    )
    // smallAlpha：固定顶栏中的专辑标题 —— 在 hdrProgress 后半段淡入
    val smallAlpha by animateFloatAsState(
        targetValue = (hdrProgress * 2f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "smallAlpha",
    )
    // hdrAlpha：固定顶栏背景透明度
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
        // ── 由专辑主色调染色的渐变背景 ──────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT + ART_EXPANDED + 120.dp)
                .background(
                    Brush.verticalGradient(
                        0f to animatedDominantColor.copy(alpha = 0.92f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                ),
        )

        // ── 可滚动内容 ────────────────────────────────────────────────────────
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
            // 专辑大字信息区 —— 随封面折叠逐渐淡出
            item(key = "album_header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
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
                        overflow = TextOverflow.Ellipsis,
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
            item(key = "play_buttons") {
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
                Spacer(Modifier.height(800.dp))
            }
        }

        // ── 固定顶栏遮罩 ──────────────────────────────────────────────────────
        // 背景随 hdrProgress 淡入；返回按钮始终可见
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
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = album.title,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(64.dp, end = 16.dp)
                            .graphicsLayer { alpha = smallAlpha },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── 封面图（浮动遮罩层，随滚动动画）─────────────────────────────────────
        // 声明在最后，确保渲染层级高于固定顶栏
        // 过渡动画期间 alpha=0（由 GeometryOverlay 浮层接管），完成后淡入
        LandscapistImage(
            imageModel = { coverUri },
            modifier =
                Modifier
                    .padding(start = artStart, top = artTop)
                    .size(artSize)
                    .clip(RoundedCornerShape(cornerRad))
                    // Draw 阶段读取 progress：progress < 1f 时完全隐藏（overlay 飞行中），
                    // progress >= 1f 时才显示目标封面；B→A 反向时同样立即隐藏交还给 overlay
                    .graphicsLayer {
                        alpha = if ((geometryTransition?.progress?.value ?: 1f) >= 1f) 1f else 0f
                    }.then(
                        if (geometryTransition != null) {
                            Modifier.geometryTarget(geometryTransition)
                        } else {
                            Modifier
                        },
                    ),
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

/**
 * 飞行浮层中渲染的专辑封面内容。
 * 与 [me.spica27.spicamusic.ui.home.page.AlbumGridItem] 使用相同的 imageModel，保证视觉连续性。
 * 由 [AlbumDetailScene] 通过 [me.spica27.navkit.scene.Scene.geometryOverlay] 注册。
 */
@Composable
internal fun AlbumCoverContent(album: Album) {
    LandscapistImage(
        imageModel = { album.getCoverUri() },
        modifier = Modifier.fillMaxSize(),
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

// ── 播放 / 随机播放操作行 ──────────────────────────────────────────────────────

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

// ── 歌曲列表行 ────────────────────────────────────────────────────────────────

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
