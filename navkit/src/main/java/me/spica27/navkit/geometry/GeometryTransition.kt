package me.spica27.navkit.geometry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect

/**
 * 几何过渡动画的状态容器，用于实现"共享元素"式的位置/尺寸过渡。
 *
 * ## 工作原理
 * 1. 触发方调用 [start]，将源矩形和目标矩形分别存入 [sourceRect] / [targetRect]
 * 2. 动画通过 [progress]（Animatable 0f→1f）插值当前边界
 * 3. [getBounds] 在每帧根据 [progress.value] 计算插值后的 [Rect]
 *
 * @param key 标识此过渡的唯一字符串，供 NavigationStack 匹配源/目标节点
 */
class GeometryTransition(val key: String) {

    /** 过渡起始矩形（源节点的全局位置和尺寸） */
    val sourceRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /** 过渡目标矩形（目标节点的全局位置和尺寸） */
    val targetRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /**
     * 过渡进度：0f = 完全在源位置，1f = 完全在目标位置。
     * 使用弹簧动画（刚度 250，无弹跳）以获得自然感。
     */
    val progress = Animatable(initialValue = 0f)

    /** 是否正在过渡中（progress 不等于目标终点时为 true） */
    val isRunning: Boolean get() = progress.isRunning

    /**
     * 启动从 0f 到 1f 的过渡动画（在调用方的协程中执行）。
     * @param onComplete 动画结束后的回调（可选）
     */
    suspend fun animateForward() {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    }

    /**
     * 反向动画从当前进度回到 0f（用于 pop 时复原）。
     */
    suspend fun animateReverse() {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    }

    /** 将进度 snap 到 0f（无动画重置） */
    suspend fun reset() {
        progress.snapTo(0f)
    }

    /**
     * 根据当前 [progress.value] 在 [sourceRect] 和 [targetRect] 之间插值，
     * 返回当前帧的边界矩形。
     *
     * 使用线性插值（lerp）；如需非线性可在外部对 progress 应用 easing。
     */
    fun getBounds(): Rect {
        val t = progress.value
        val src = sourceRect.value
        val dst = targetRect.value
        return Rect(
            left = lerp(src.left, dst.left, t),
            top = lerp(src.top, dst.top, t),
            right = lerp(src.right, dst.right, t),
            bottom = lerp(src.bottom, dst.bottom, t)
        )
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    companion object {
        /** 几何过渡弹簧刚度（250 ≈ 略软于导航弹簧 300，视觉上更流畅） */
        private const val SPRING_STIFFNESS = 250f
    }
}
