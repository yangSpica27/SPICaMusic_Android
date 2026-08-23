package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.flow.Flow
import kotlin.math.cos
import kotlin.math.sin

private const val COMBINATION_COVER_COUNT = 5
private const val COMBINATION_ROTATION_DEGREES = -30f
private const val COMBINATION_SCALE = 2.15f
private const val COMBINATION_FOCUS_OFFSET_X = -0.485f
private const val COMBINATION_FOCUS_OFFSET_Y = -0.21f

private fun albumCoverUri(albumId: Long): Uri = "content://media/external/audio/albumart/$albumId".toUri()

/**
 * 歌单组合封面。
 *
 * 五张或更多封面使用 2+3+空位的倾斜布局；少于五张时回退到第一张封面。
 * 图片由 LandscapistImage 独立加载，不预先合成 Bitmap。
 *
 * 使用方式（歌单列表 item 示例）：
 * ```kotlin
 * val coverIds by viewModel.getPlaylistCoverAlbumIds(playlist.playlistId!!).collectAsState(emptyList())
 * PlaylistCoverView(albumIds = coverIds, modifier = Modifier.size(56.dp))
 * ```
 * 或直接传入 Flow：
 * ```kotlin
 * PlaylistCoverView(
 *     albumIdsFlow = playlistUseCases.getPlaylistCoverAlbumIds(playlistId),
 *     modifier = Modifier.size(56.dp),
 * )
 * ```
 *
 * @param albumIds   最多 5 个专辑 ID，长度决定渲染策略（0/1-4/5）
 * @param iconSize   空歌单占位音符图标大小，默认 32.dp
 */
@Composable
fun PlaylistCoverView(
    albumIds: List<Long>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    Box(modifier = modifier) {
        ShowOnIdleContent(
            true,
            enter = materialSharedAxisYIn(true),
            exit = materialSharedAxisYOut(true),
        ) {
            when {
                albumIds.isEmpty() -> EmptyPlaylistCover(Modifier.fillMaxSize(), iconSize)
                albumIds.size < COMBINATION_COVER_COUNT ->
                    SingleAlbumCover(albumIds.first(), Modifier.fillMaxSize())
                else ->
                    FinalPerfectCollage(
                        covers = albumIds.take(COMBINATION_COVER_COUNT).map(::albumCoverUri),
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

/**
 * 接受 Flow 版本，内部 collectAsState；适合直接传 use case 的 Flow 而不想在外部 collect 的场景。
 */
@Composable
fun PlaylistCoverView(
    albumIdsFlow: Flow<List<Long>>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    val albumIds by albumIdsFlow.collectAsState(initial = emptyList())
    PlaylistCoverView(albumIds = albumIds, modifier = modifier, iconSize = iconSize)
}

// ─────────────────────────────────────────────────────────────────────────────
// 内部渲染分支
// ─────────────────────────────────────────────────────────────────────────────

/** 歌单为空时的占位图（音符图标居中） */
@Composable
private fun EmptyPlaylistCover(
    modifier: Modifier,
    iconSize: Dp,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 歌单少于 5 首时，直接显示第一首的专辑封面 */
@Composable
private fun SingleAlbumCover(
    albumId: Long,
    modifier: Modifier,
) {
    PlaylistCoverImage(
        uri = albumCoverUri(albumId),
        modifier = modifier,
    )
}

@Composable
private fun PlaylistCoverImage(
    uri: Uri,
    modifier: Modifier,
) {
    LandscapistImage(
        imageModel = { uri },
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = { CoverImagePlaceholder(Modifier.fillMaxSize()) },
    )
}

@Composable
private fun CoverImagePlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier,
    ) {
        // Keep failed tiles visually quiet while the remaining covers continue to load.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }
}

/**
 * Renders the MeiloX-style grid:
 *
 * [0] [1]
 * [2] [3] [4] [ ]
 *
 * The collage is laid out at its natural 4:3 ratio, then rotated and scaled so the first
 * image becomes the focal point of the square viewport.
 */
@Composable
private fun FinalPerfectCollage(
    covers: List<Uri>,
    modifier: Modifier = Modifier,
) {
    if (covers.size < COMBINATION_COVER_COUNT) return

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val density = LocalDensity.current
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportHeightPx = with(density) { maxHeight.toPx() }

        // The actual grid is 1/2 W high on top and 1/4 W high on the bottom.
        val collageWidthPx = viewportWidthPx
        val topRowHeightPx = collageWidthPx / 2f
        val bottomRowHeightPx = collageWidthPx / 4f
        val collageHeightPx = topRowHeightPx + bottomRowHeightPx

        val collageCenterX = collageWidthPx / 2f
        val collageCenterY = collageHeightPx / 2f
        val firstImageCenterX = collageWidthPx / 4f
        val firstImageCenterY = topRowHeightPx / 2f

        val angleRad = Math.toRadians(COMBINATION_ROTATION_DEGREES.toDouble())
        val vectorX = (firstImageCenterX - collageCenterX) * COMBINATION_SCALE
        val vectorY = (firstImageCenterY - collageCenterY) * COMBINATION_SCALE
        val rotatedVectorX = vectorX * cos(angleRad).toFloat() - vectorY * sin(angleRad).toFloat()
        val rotatedVectorY = vectorX * sin(angleRad).toFloat() + vectorY * cos(angleRad).toFloat()

        // Move the focal tile slightly toward the upper-left so the enlarged collage fully
        // bleeds past the viewport edges instead of exposing a transparent corner.
        val focalPointX = viewportWidthPx * (0.5f + COMBINATION_FOCUS_OFFSET_X)
        val focalPointY = viewportHeightPx * (0.5f + COMBINATION_FOCUS_OFFSET_Y)
        val computedTranslationX = focalPointX - (collageCenterX + rotatedVectorX)
        val computedTranslationY = focalPointY - (collageCenterY + rotatedVectorY)

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = computedTranslationX
                        translationY = computedTranslationY
                        rotationZ = COMBINATION_ROTATION_DEGREES
                        scaleX = COMBINATION_SCALE
                        scaleY = COMBINATION_SCALE
                    },
        ) {
            ImageCollageContent(covers = covers, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ImageCollageContent(
    covers: List<Uri>,
    modifier: Modifier = Modifier,
) {
    if (covers.size < COMBINATION_COVER_COUNT) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            PlaylistCoverImage(
                uri = covers[0],
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            PlaylistCoverImage(
                uri = covers[1],
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            PlaylistCoverImage(
                uri = covers[2],
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            PlaylistCoverImage(
                uri = covers[3],
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            PlaylistCoverImage(
                uri = covers[4],
                modifier = Modifier.weight(1f).aspectRatio(1f),
            )
            Spacer(Modifier.weight(1f).aspectRatio(1f))
        }
    }
}
