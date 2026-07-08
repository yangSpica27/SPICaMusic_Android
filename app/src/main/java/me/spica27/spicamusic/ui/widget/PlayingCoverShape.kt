package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

@Composable
fun rememberPlayingCoverShape(isPlaying: Boolean): Shape {
    val playProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "playingCoverPlayProgress",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "playingCoverMorph")
    val polygonProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "playingCoverPolygonProgress",
    )

    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "playingCoverRotationProgress",
    )

    val circle =
        remember {
            RoundedPolygon(
                numVertices = 16,
                rounding = CornerRounding(radius = 1f),
            )
        }
    val shape1 =
        remember {
            RoundedPolygon.star(
                3,
                rounding = CornerRounding(smoothing = 0.3f, radius = .2f),
            )
        }
    val shape2 =
        remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(smoothing = 0.3f, radius = .2f),
            )
        }
    val circleToTriangle = remember(circle, shape1) { Morph(circle, shape1) }
    val triangleToQuadrilateral = remember(shape1, shape2) { Morph(shape1, shape2) }

    return remember(circleToTriangle, triangleToQuadrilateral, playProgress, polygonProgress) {
        PlayingCoverShape(
            circleToTriangle = circleToTriangle,
            triangleToQuadrilateral = triangleToQuadrilateral,
            playProgress = playProgress,
            polygonProgress = polygonProgress,
            rotationProgress = rotationProgress,
        )
    }
}

private class PlayingCoverShape(
    private val circleToTriangle: Morph,
    private val triangleToQuadrilateral: Morph,
    private val playProgress: Float,
    private val polygonProgress: Float,
    private val rotationProgress: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val normalizedPlayProgress = playProgress.coerceIn(0f, 1f)
        val morph =
            if (normalizedPlayProgress < 0.999f) {
                circleToTriangle.toPath(progress = normalizedPlayProgress)
            } else {
                triangleToQuadrilateral.toPath(progress = polygonProgress.coerceIn(0f, 1f))
            }
        val path = morph.asComposePath()
        val matrix =
            Matrix().apply {
                scale(size.width / 2f, size.height / 2f)
                translate(1f, 1f)
                rotateZ(rotationProgress * 360f)
            }
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
