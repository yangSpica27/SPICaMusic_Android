package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 底栏垂直拖拽手势的运算层。
 *
 * 只负责把像素拖拽换算成 0..1 的容器形变进度；播放器重内容的启停由
 * [BottomBarV2State] 在拖拽开始和吸附完成时统一处理。
 */
internal class VerticalDragGestureHandler(
    private val scope: CoroutineScope,
    private val fraction: Animatable<Float, AnimationVector1D>,
    private val snapSpec: AnimationSpec<Float>,
    private val onDragStarted: () -> Unit = {},
    private val onSettled: (expanded: Boolean) -> Unit = {},
) {
    /** 收起态到展开态的总像素距离（由 Layout 在每次测量时写入）。 */
    var dragDistancePx: Float = 1f

    private var dragSnapJob: Job? = null
    private var startFraction = 0f
    private var accumulatedDrag = 0f

    fun onDragStart() {
        dragSnapJob?.cancel()
        onDragStarted()
        dragSnapJob = scope.launch { fraction.stop() }
        startFraction = fraction.value
        accumulatedDrag = 0f
    }

    fun onDrag(dragAmount: Float) {
        accumulatedDrag += dragAmount
        val distance = dragDistancePx.coerceAtLeast(1f)
        // 向上拖（dragAmount < 0）使进度增大。
        val target = (startFraction - accumulatedDrag / distance).coerceIn(0f, 1f)
        dragSnapJob = scope.launch { fraction.snapTo(target) }
    }

    fun onDragEnd(
        velocity: Float,
        distanceThresholdPx: Float,
        velocityThresholdPx: Float,
    ) {
        val target =
            when {
                abs(accumulatedDrag) > distanceThresholdPx -> if (accumulatedDrag < 0f) 1f else 0f
                abs(velocity) > velocityThresholdPx -> if (velocity < 0f) 1f else 0f
                else -> if (fraction.value > 0.5f) 1f else 0f
            }
        val distance = dragDistancePx.coerceAtLeast(1f)
        val initialFractionVelocity = (-velocity / distance).coerceIn(-8f, 8f)
        dragSnapJob =
            scope.launch {
                fraction.animateTo(
                    targetValue = target,
                    animationSpec = snapSpec,
                    initialVelocity = initialFractionVelocity,
                )
                onSettled(target >= 1f)
            }
    }
}
