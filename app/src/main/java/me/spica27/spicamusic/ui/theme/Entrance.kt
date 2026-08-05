package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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

/** 降级动效下的入场淡入时长：只留这一次短淡入，不排队不位移 */
private const val ENTRANCE_REDUCED_FADE_MILLIS = 120

/**
 * 入场状态：把"进度"与"是否允许位移"打包成一个持有者。
 */
@Stable
class EntranceState internal constructor(
    internal val progress: Animatable<Float, AnimationVector1D>,
    internal val translate: Boolean,
) {
    /**
     * 淡入进度 0f → 1f。**永远是真实进度**，降级时也不例外——
     * 降级要去掉的是位移，不是透明度。在 Draw 阶段读取，不触发重组。
     */
    val alpha: Float
        get() = progress.value

    /**
     * 上浮位移的剩余比例 1f → 0f，乘上位移距离即可用。
     * 降级时恒为 0f，元素直接就位、只保留 [alpha] 的淡入。
     */
    val translateFraction: Float
        get() = if (translate) 1f - progress.value else 0f
}

/**
 * 记住一个入场进度（0f → 1f）。
 *
 */
@Composable
fun rememberEntrance(
    order: Int,
    play: Boolean = true,
): EntranceState {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (play) 0f else 1f) }
    val state = remember(reduced) { EntranceState(progress = progress, translate = !reduced) }
    LaunchedEffect(reduced) {
        // 已就位则不重播：避免重组/重访时元素再次跳一遍
        if (progress.value < 1f) {
            if (reduced) {
                // 降级：不排队、不位移，只留一次短淡入（降级是"更少更柔"，不是"归零"）
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        tween(
                            durationMillis = ENTRANCE_REDUCED_FADE_MILLIS,
                            easing = EaseOutEmphasized,
                        ),
                )
            } else {
                delay(order * ENTRANCE_STAGGER_MILLIS)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec =
                        spring(
                            dampingRatio = ENTRANCE_DAMPING,
                            stiffness = ENTRANCE_STIFFNESS,
                        ),
                )
            }
        }
    }
    return state
}

/**
 * 入场位移 + 淡入。动画值全部在 Draw 阶段读取，入场期间零重组。
 *
 * 刻意不含 blur：`Modifier.blur` 的半径参数在组合期求值，会让每一帧都重组
 * 整条修饰符链并重建 RenderEffect（曾是 PlaylistDetailScreen 的性能问题）。
 */
fun Modifier.entranceGraphics(entrance: EntranceState): Modifier =
    graphicsLayer {
        alpha = entrance.alpha
        // 降级时只淡入不上浮：位移是无障碍设置要求去掉的部分
        translationY = entrance.translateFraction * ENTRANCE_TRANSLATION_DP.dp.toPx()
    }
