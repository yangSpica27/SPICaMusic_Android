package me.spica27.spicamusic.ui.player

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import me.spica27.spicamusic.ui.widget.AudioCover
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.util.lerp as floatLerp

/**
 * Geometry state for the artwork shared by the player and lyrics pager pages.
 *
 * Pager children report moving coordinates. The overlay removes the pager's
 * page translation before interpolating, keeping both endpoints stable while
 * the artwork itself follows the drag fraction.
 */
@Stable
class PlayerLyricsMorphState internal constructor() {
    private var rootCoordinates: LayoutCoordinates? = null
    private var sourceCoordinates: LayoutCoordinates? = null
    private var targetCoordinates: LayoutCoordinates? = null
    private var sourceProgress = 0f
    private var targetProgress = 0f

    var sourceBounds by mutableStateOf(Rect.Zero)
        private set

    var targetBounds by mutableStateOf(Rect.Zero)
        private set

    var pageWidthPx by mutableStateOf(0f)
        private set

    internal fun updateRoot(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
        pageWidthPx = coordinates.size.width.toFloat()
        recalculate()
    }

    internal fun updateSource(
        coordinates: LayoutCoordinates,
        progress: Float,
    ) {
        sourceCoordinates = coordinates
        sourceProgress = progress.coerceIn(0f, 1f)
        recalculate()
    }

    internal fun updateTarget(
        coordinates: LayoutCoordinates,
        progress: Float,
    ) {
        targetCoordinates = coordinates
        targetProgress = progress.coerceIn(0f, 1f)
        recalculate()
    }

    private fun recalculate() {
        val root = rootCoordinates?.takeIf { it.isAttached } ?: return
        sourceCoordinates?.takeIf { it.isAttached }?.let { source ->
            val movingTopLeft = root.localPositionOf(source, Offset.Zero)
            val topLeft =
                movingTopLeft.copy(
                    x = movingTopLeft.x + sourceProgress * pageWidthPx,
                )
            sourceBounds = Rect(topLeft, Size(source.size.width.toFloat(), source.size.height.toFloat()))
        }
        targetCoordinates?.takeIf { it.isAttached }?.let { target ->
            val movingTopLeft = root.localPositionOf(target, Offset.Zero)
            val topLeft =
                movingTopLeft.copy(
                    x = movingTopLeft.x - (1f - targetProgress) * pageWidthPx,
                )
            targetBounds = Rect(topLeft, Size(target.size.width.toFloat(), target.size.height.toFloat()))
        }
    }

    val isReady: Boolean
        get() =
            pageWidthPx > 1f &&
                sourceBounds.isUsable() &&
                targetBounds.isUsable()
}

@Composable
fun rememberPlayerLyricsMorphState(): PlayerLyricsMorphState = remember { PlayerLyricsMorphState() }

fun Modifier.playerLyricsMorphRoot(state: PlayerLyricsMorphState): Modifier = onGloballyPositioned(state::updateRoot)

fun Modifier.playerLyricsMorphSource(
    state: PlayerLyricsMorphState,
    progressProvider: () -> Float,
): Modifier =
    onGloballyPositioned { coordinates ->
        state.updateSource(coordinates, progressProvider())
    }

fun Modifier.playerLyricsMorphTarget(
    state: PlayerLyricsMorphState,
    progressProvider: () -> Float,
): Modifier =
    onGloballyPositioned { coordinates ->
        state.updateTarget(coordinates, progressProvider())
    }

@Composable
fun PlayerLyricsArtworkMorphOverlay(
    state: PlayerLyricsMorphState,
    artworkUri: Uri?,
    artworkPainter: Painter?,
    progressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    if (!state.isReady) return

    val density = LocalDensity.current
    val target = state.targetBounds
    val targetWidth = target.width.coerceAtLeast(1f)
    val targetHeight = target.height.coerceAtLeast(1f)
    val shadowColor = Color.Black
    val dynamicShape =
        remember(state, progressProvider) {
            LyricsArtworkShape(state, progressProvider)
        }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            x = target.left.roundToInt(),
                            y = target.top.roundToInt(),
                        )
                    }.size(
                        width = with(density) { targetWidth.toDp() },
                        height = with(density) { targetHeight.toDp() },
                    ).zIndex(40f)
                    .graphicsLayer {
                        val progress = progressProvider().coerceIn(0f, 1f)
                        val left = floatLerp(state.sourceBounds.left, state.targetBounds.left, progress)
                        val top = floatLerp(state.sourceBounds.top, state.targetBounds.top, progress)
                        val width = floatLerp(state.sourceBounds.width, state.targetBounds.width, progress)
                        val height = floatLerp(state.sourceBounds.height, state.targetBounds.height, progress)
                        val currentScaleX = (width / targetWidth).coerceAtLeast(0.01f)
                        val currentScaleY = (height / targetHeight).coerceAtLeast(0.01f)

                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = left - target.left
                        translationY = top - target.top
                        scaleX = currentScaleX
                        scaleY = currentScaleY
                        shape = dynamicShape
                        clip = true
                        // The cover lifts only while it is morphing and settles
                        // flush at both endpoints. Compensating for the changing
                        // cover scale keeps the visible shadow depth stable.
                        val lift = sin(progress * PI).toFloat().coerceAtLeast(0f)
                        shadowElevation =
                            lift * 18.dp.toPx() / maxOf(currentScaleX, currentScaleY)
                        ambientShadowColor = shadowColor.copy(alpha = 0.28f)
                        spotShadowColor = shadowColor.copy(alpha = 0.36f)
                        alpha =
                            if (
                                progress > ARTWORK_OVERLAY_MIN &&
                                progress < ARTWORK_OVERLAY_MAX
                            ) {
                                1f
                            } else {
                                0f
                            }
                    }.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkPainter != null) {
                Image(
                    painter = artworkPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AudioCover(
                    uri = artworkUri,
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }
    }
}

private class LyricsArtworkShape(
    private val state: PlayerLyricsMorphState,
    private val progressProvider: () -> Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val progress = progressProvider().coerceIn(0f, 1f)
        val currentArtworkWidth =
            floatLerp(state.sourceBounds.width, state.targetBounds.width, progress)
                .coerceAtLeast(1f)
        val targetLayerWidth = state.targetBounds.width.coerceAtLeast(1f)
        val layerScale = currentArtworkWidth / targetLayerWidth
        // Match the real endpoint shapes exactly. The 48.dp lyrics thumbnail
        // uses a proportionally smaller radius so it keeps the same visual
        // corner treatment as the much larger player cover.
        val visibleRadiusDp =
            floatLerp(
                PLAYER_ARTWORK_CORNER_DP,
                LYRICS_ARTWORK_CORNER_DP,
                progress,
            )
        val layerRadiusDp = visibleRadiusDp / layerScale.coerceAtLeast(0.01f)
        return RoundedCornerShape(layerRadiusDp.dp)
            .createOutline(size, layoutDirection, density)
    }
}

private fun Rect.isUsable(): Boolean =
    this != Rect.Zero &&
        left.isFinite() &&
        top.isFinite() &&
        width > 1f &&
        height > 1f

private const val ARTWORK_OVERLAY_MIN = 0.001f
private const val ARTWORK_OVERLAY_MAX = 0.999f
private const val PLAYER_ARTWORK_CORNER_DP = 16f
private const val LYRICS_ARTWORK_CORNER_DP = 4f
