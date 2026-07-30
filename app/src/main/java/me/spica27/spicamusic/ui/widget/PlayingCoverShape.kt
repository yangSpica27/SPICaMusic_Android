package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.isActive
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun rememberPlayingCoverShape(
    isPlaying: Boolean,
    lockToTransitionShape: Boolean = false,
): Shape {
    val playProgress = remember { Animatable(if (isPlaying) 1f else 0f) }
    val polygonProgress = remember { Animatable(0f) }
    val rotationProgress = remember { Animatable(0f) }

    LaunchedEffect(isPlaying, lockToTransitionShape) {
        if (lockToTransitionShape) {
            playProgress.animateTo(
                targetValue = if (isPlaying) 1f else 0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            )
            polygonProgress.animateTo(
                // 过渡固定形状使用三叶圆角斜三角，而不是四叶圆角十字。
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
            return@LaunchedEffect
        }

        if (isPlaying) {
            // Finish circle -> triangle before starting the polygon loop. Starting both
            // clocks at composition time made the first loop frame jump to an arbitrary
            // triangle -> quadrilateral progress.
            polygonProgress.snapTo(0f)
            playProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            )
            while (isActive) {
                polygonProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                )
                polygonProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                )
            }
        } else {
            // Return to the triangle first, then morph triangle -> circle. This also keeps
            // pause transitions continuous when interrupted halfway through the loop.
            polygonProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
            playProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(isPlaying, lockToTransitionShape) {
        if (!isPlaying || lockToTransitionShape) {
            val targetRotation =
                if (isPlaying && lockToTransitionShape) {
                    // 三角形每 120° 等价；固定到最接近当前角度的 30° 斜向姿态，
                    // 避免为了归位突然倒转一大圈。
                    ((rotationProgress.value - DIAGONAL_TRIANGLE_ROTATION) * 3f).roundToInt() / 3f +
                        DIAGONAL_TRIANGLE_ROTATION
                } else {
                    rotationProgress.value
                }
            rotationProgress.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
            return@LaunchedEffect
        }
        while (isActive) {
            val nextFullTurn = floor(rotationProgress.value) + 1f
            rotationProgress.animateTo(
                targetValue = nextFullTurn,
                animationSpec =
                    tween(
                        durationMillis =
                            (6200f * (nextFullTurn - rotationProgress.value))
                                .roundToInt()
                                .coerceAtLeast(1),
                        easing = LinearEasing,
                    ),
            )
        }
    }

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

    return remember(
        circleToTriangle,
        triangleToQuadrilateral,
        playProgress.value,
        polygonProgress.value,
        rotationProgress.value,
    ) {
        PlayingCoverShape(
            circleToTriangle = circleToTriangle,
            triangleToQuadrilateral = triangleToQuadrilateral,
            playProgress = playProgress.value,
            polygonProgress = polygonProgress.value,
            rotationProgress = rotationProgress.value,
        )
    }
}

private const val DIAGONAL_TRIANGLE_ROTATION = 1f / 24f

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
