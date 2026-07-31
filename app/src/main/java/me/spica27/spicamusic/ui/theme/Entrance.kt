package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 首屏入场动画（本应用的标志性动效）。
 *
 */

/** 相邻元素之间的入场间隔。30–80ms 是体感最自然的区间，再长就显得拖沓 */
const val ENTRANCE_STAGGER_MILLIS = 55L

/**
 * 入场闸门时长：整条瀑布跑完所需的时间上界。
 *
 */
const val ENTRANCE_GATE_MILLIS = 1000L

/** 入场上浮距离 */
private const val ENTRANCE_TRANSLATION_DP = 28f

/** 入场弹簧：轻微回弹，约 400–500ms 落定 */
private const val ENTRANCE_DAMPING = Spring.DampingRatioLowBouncy
private const val ENTRANCE_STIFFNESS = 380f

/**
 * 记住一个入场进度（0f → 1f）。
 *
 */
@Composable
fun rememberEntrance(
    order: Int,
    play: Boolean = true,
): Animatable<Float, AnimationVector1D> {
    val entrance = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        // 已就位则不重播：避免重组/重访时元素再次跳一遍
        if (entrance.value < 1f) {
            delay(order * ENTRANCE_STAGGER_MILLIS)
            entrance.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = ENTRANCE_DAMPING,
                        stiffness = ENTRANCE_STIFFNESS,
                    ),
            )
        }
    }
    return entrance
}

/**
 * 入场位移 + 淡入。动画值全部在 Draw 阶段读取，入场期间零重组。
 *
 * 刻意不含 blur：`Modifier.blur` 的半径参数在组合期求值，会让每一帧都重组
 * 整条修饰符链并重建 RenderEffect（曾是 PlaylistDetailScreen 的性能问题）。
 */
fun Modifier.entranceGraphics(entrance: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        val enter = entrance.value
        alpha = enter
        translationY = (1f - enter) * ENTRANCE_TRANSLATION_DP.dp.toPx()
    }
