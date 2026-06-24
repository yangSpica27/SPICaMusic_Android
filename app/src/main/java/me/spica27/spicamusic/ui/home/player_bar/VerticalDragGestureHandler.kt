package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 底栏垂直拖拽手势的“运算大脑”。
 *
 * 思路完全对齐 PixelPlayer：用一个 [Animatable] 的 [展开进度][fraction]（0 = 收起的迷你条，
 * 1 = 全屏播放器）来驱动整个过渡。手指的像素位移按“全程收起→展开的总距离”折算成进度增量，
 * 松手时按 距离 → 速度 → 最近锚点 的优先级决定吸附到收起还是展开。
 *
 * 该类只负责数学与动画，几何摆放交给 [BottomBarV2] 的自定义 Layout。
 */
internal class VerticalDragGestureHandler(
    private val scope: CoroutineScope,
    private val fraction: Animatable<Float, AnimationVector1D>,
    private val snapSpec: AnimationSpec<Float>,
) {
    /** 收起态到展开态的总像素距离（由 Layout 在每次测量时写入）。 */
    var dragDistancePx: Float = 1f

    private var dragSnapJob: Job? = null

    /** 拖拽起点处的进度，用于把绝对位移换算成进度。 */
    private var startFraction = 0f

    /** 自本次拖拽开始累计的纵向位移（向上为负）。 */
    private var accumulatedDrag = 0f

    fun onDragStart() {
        dragSnapJob?.cancel()
        // 立刻停住正在进行的吸附动画，使拖拽接管手感不打架
        dragSnapJob = scope.launch { fraction.stop() }
        startFraction = fraction.value
        accumulatedDrag = 0f
    }

    fun onDrag(dragAmount: Float) {
        accumulatedDrag += dragAmount
        val distance = dragDistancePx.coerceAtLeast(1f)
        // 向上拖（dragAmount < 0）使进度增大
        val target = (startFraction - accumulatedDrag / distance).coerceIn(0f, 1f)
        dragSnapJob = scope.launch { fraction.snapTo(target) }
    }

    /**
     * @param velocity   松手瞬间的纵向速度（px/s，向上为负）
     * @param distanceThresholdPx 触发“按方向吸附”的最小位移
     * @param velocityThresholdPx 触发“按速度吸附”的最小速度
     */
    fun onDragEnd(
        velocity: Float,
        distanceThresholdPx: Float,
        velocityThresholdPx: Float,
    ) {
        val target =
            when {
                // 1. 位移足够大：按拖拽方向决定（向上展开，向下收起）
                abs(accumulatedDrag) > distanceThresholdPx -> if (accumulatedDrag < 0f) 1f else 0f
                // 2. 否则看速度：快速滑动按速度方向决定
                abs(velocity) > velocityThresholdPx -> if (velocity < 0f) 1f else 0f
                // 3. 否则吸附到最近的锚点
                else -> if (fraction.value > 0.5f) 1f else 0f
            }
        val distance = dragDistancePx.coerceAtLeast(1f)
        // 把像素速度换算成“进度/秒”，让吸附动画衔接松手时的惯性
        val initialFractionVelocity = -velocity / distance
        dragSnapJob =
            scope.launch {
                fraction.animateTo(
                    targetValue = target,
                    animationSpec = snapSpec,
                    initialVelocity = initialFractionVelocity,
                )
            }
    }
}
