package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.image.LandscapistImage

/**
 * 歌曲/专辑封面。
 * [uri] 加载失败时先尝试 [fallbackUri]（歌曲本体无内嵌封面时回退专辑图），仍失败才渲染 [placeHolder]。
 */
@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
    uri: Uri? = null,
    fallbackUri: Uri? = null,
    progressiveEnabled: Boolean = false,
    onPainterReady: (Painter) -> Unit = {},
    onPainterFailed: () -> Unit = {},
) {
    LandscapistImage(
        modifier = modifier,
        requestBuilder = {
            this
                .model(uri)
                .tag(uri.toString())
                .progressiveEnabled(progressiveEnabled)
                .build()
        },
        imageModel = { uri },
        success = { _, painter ->
            LaunchedEffect(painter) {
                onPainterReady(painter)
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            LaunchedEffect(uri, fallbackUri) {
                onPainterFailed()
            }
            if (fallbackUri != null && fallbackUri != uri) {
                CoverFallback(
                    fallbackUri = fallbackUri,
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = placeHolder,
                )
            } else {
                placeHolder()
            }
        },
    )
}

/**
 * Keeps the last decoded cover visible while a new URI is loading.
 *
 * The loader is always composed above the retained painter, so retaining a painter never prevents
 * the next cover request from starting. This avoids exposing a theme-coloured placeholder for a
 * frame during track changes.
 */
@Composable
fun StableAudioCover(
    uri: Uri?,
    retainedPainter: Painter?,
    modifier: Modifier = Modifier,
    onPainterReady: (Painter) -> Unit = {},
    onPainterFailed: () -> Unit = {},
    placeHolder: @Composable () -> Unit = {},
) {
    val previousPainter = remember(uri) { retainedPainter }
    var incomingPainter by remember(uri) { mutableStateOf<Painter?>(null) }
    val incomingAlpha = remember(uri) { Animatable(0f) }

    LaunchedEffect(uri, incomingPainter) {
        val painter = incomingPainter ?: return@LaunchedEffect
        if (retainedPainter == null) {
            incomingAlpha.snapTo(1f)
        } else {
            incomingAlpha.snapTo(0f)
            incomingAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = COVER_CROSSFADE_DURATION_MS),
            )
        }
    }

    Box(modifier = modifier) {
        val basePainter =
            if (uri == null) {
                retainedPainter
            } else {
                previousPainter ?: retainedPainter
            }
        if (basePainter != null) {
            Image(
                painter = basePainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            placeHolder()
        }

        incomingPainter?.let { painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = incomingAlpha.value },
            )
        }

        if (uri != null) {
            key(uri) {
                AudioCover(
                    uri = uri,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0f },
                    onPainterReady = { painter ->
                        incomingPainter = painter
                        onPainterReady(painter)
                    },
                    onPainterFailed = onPainterFailed,
                    placeHolder = {},
                )
            }
        }
    }
}

private const val COVER_CROSSFADE_DURATION_MS = 180

/**
 * 播放器各尺寸封面共用的无封面占位图。
 *
 * 图标尺寸随封面平滑增长并限制在 16–64dp，确保迷你播放器、飞行过渡层和完整播放页
 * 始终使用同一个矢量图标，不会在拖动展开时突然换成 emoji 或跳变尺寸。
 */
@Composable
fun MusicCoverPlaceholder(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentDescription: String? = null,
) {
    BoxWithConstraints(
        modifier = modifier.background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        val iconSize =
            (minOf(maxWidth, maxHeight) * 0.36f)
                .coerceIn(16.dp, 64.dp)
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 封面兜底层：加载 [fallbackUri]，失败（或为 null）时渲染 [placeHolder]。
 * 供 AudioCover 与各处直接使用 LandscapistImage 的 failure 槽复用。
 */
@Composable
fun CoverFallback(
    fallbackUri: Uri?,
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
) {
    if (fallbackUri == null) {
        placeHolder()
        return
    }
    LandscapistImage(
        modifier = modifier,
        requestBuilder = {
            this
                .model(fallbackUri)
                .tag(fallbackUri.toString())
                .progressiveEnabled(false)
                .build()
        },
        imageModel = { fallbackUri },
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            placeHolder()
        },
    )
}
