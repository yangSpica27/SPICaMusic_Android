@file:Suppress("FunctionName", "ktlint:standard:package-name")

package me.spica27.spicamusic.ui.widget.audio_seekbar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

/**
 * XiangsuPlayerHQ 风格的可拖动波浪进度条。
 *
 * 已播放部分为连续正弦波，未播放部分保持直线；播放时波形向前流动，
 * 暂停或拖动时平滑收束为直线。拇指在拖动期间由圆形变为竖向圆角短条。
 */
@Composable
fun ExpressiveWavySlider(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color = activeColor,
    strokeWidth: Dp = 5.dp,
    wavelength: Dp = 30.dp,
    amplitude: Dp = 4.dp,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val wavelengthPx = with(density) { wavelength.toPx() }
    val amplitudePx = with(density) { amplitude.toPx() }
    val thumbRadiusPx = with(density) { 8.dp.toPx() }
    val latestOnProgressChange by rememberUpdatedState(onProgressChange)
    val latestOnProgressChangeFinished by rememberUpdatedState(onProgressChangeFinished)

    var dragging by remember { mutableStateOf(false) }
    var gestureProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress, dragging) {
        if (!dragging) gestureProgress = progress.coerceIn(0f, 1f)
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive) {
            val start = phase
            androidx.compose.animation.core
                .Animatable(start)
                .animateTo(
                    targetValue = start + 1f,
                    animationSpec = tween(durationMillis = 1350, easing = LinearEasing),
                ) {
                    phase = value
                }
            phase -= phase.toInt()
        }
    }

    val waveStrength by animateFloatAsState(
        targetValue = if (isPlaying && !dragging) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "wavySliderAmplitude",
    )
    val interactionFraction by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "wavySliderThumb",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(Unit) {
                    fun progressForX(x: Float): Float {
                        val edge = thumbRadiusPx
                        return ((x - edge) / (size.width - edge * 2f).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        dragging = true
                        down.consume()
                        gestureProgress = progressForX(down.position.x)
                        latestOnProgressChange(gestureProgress)
                        try {
                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change =
                                    event.changes.firstOrNull { it.id == pointerId }
                                        ?: event.changes.firstOrNull { it.pressed }
                                        ?: break
                                pointerId = change.id
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                    gestureProgress = progressForX(change.position.x)
                                    latestOnProgressChange(gestureProgress)
                                }
                            }
                        } finally {
                            dragging = false
                            latestOnProgressChangeFinished()
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackStart = thumbRadiusPx
            val trackEnd = size.width - thumbRadiusPx
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(1f)
            val thumbX = trackStart + trackWidth * gestureProgress
            val gap = 7.dp.toPx()
            val activeEnd = (thumbX - gap).coerceAtLeast(trackStart)
            val inactiveStart = (thumbX + gap).coerceAtMost(trackEnd)
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

            if (inactiveStart < trackEnd) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(inactiveStart, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round,
                )
            }

            if (activeEnd > trackStart) {
                val path = Path()
                var x = trackStart
                var first = true
                while (x <= activeEnd) {
                    val angle =
                        ((x - trackStart) / wavelengthPx * (2f * PI).toFloat()) -
                            phase * (2f * PI).toFloat()
                    val y = centerY + sin(angle.toDouble()).toFloat() * amplitudePx * waveStrength
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    x += 2f
                }
                path.lineTo(
                    activeEnd,
                    centerY +
                        sin(
                            (
                                (activeEnd - trackStart) / wavelengthPx * (2f * PI).toFloat() -
                                    phase * (2f * PI).toFloat()
                            ).toDouble(),
                        ).toFloat() * amplitudePx * waveStrength,
                )
                drawPath(path = path, color = activeColor, style = stroke)
            }

            val thumbWidth = thumbRadiusPx * 2f - interactionFraction * thumbRadiusPx
            val thumbHeight = thumbRadiusPx * 2f + interactionFraction * thumbRadiusPx
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - thumbWidth / 2f, centerY - thumbHeight / 2f),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2f),
            )
        }
    }
}
