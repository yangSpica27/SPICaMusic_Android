package me.spica27.navkit.geometry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 几何过渡动画的状态容器，用于实现"共享元素"式的位置/尺寸过渡。
 *
 * ## 工作原理
 * 1. 触发方调用 [start]，将源矩形和目标矩形分别存入 [sourceRect] / [targetRect]
 * 2. 动画通过 [progress]（Animatable 0f→1f）插值当前边界
 * 3. [getBounds] 在每帧根据 [progress.value] 计算插值后的 [Rect]
 *
 * @param key 标识此过渡的唯一字符串，供 NavigationStack 匹配源/目标节点
 * @param sourceClipRadius 源节点侧的圆角裁剪半径；飞行中按进度向 [targetClipRadius] 插值，
 *   两端与源/目标节点的真实圆角一致即可避免交接瞬间的圆角突变。文本类内容传 0.dp 避免裁掉字形边缘
 * @param targetClipRadius 目标节点侧的圆角裁剪半径，默认与 [sourceClipRadius] 相同
 */
class GeometryTransition(
    val key: String,
    val sourceClipRadius: Dp = 16.dp,
    val targetClipRadius: Dp = sourceClipRadius,
) {
    /**
     * 共享元素当前的可见所有权阶段。
     *
     * - [Source]：源节点显示，浮层隐藏
     * - [Forward]：正向飞行中，仅浮层显示
     * - [Target]：目标节点显示，浮层隐藏
     * - [Reverse]：反向飞行中，仅浮层显示
     */
    enum class GeometryPhase {
        Source,
        Forward,
        Target,
        Reverse,
    }

    /** 过渡起始矩形（源节点的全局位置和尺寸） */
    val sourceRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /** 过渡目标矩形（目标节点的全局位置和尺寸） */
    val targetRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /**
     * 源节点经祖先裁剪后的**可见**矩形（boundsInWindow）。
     * 当封面部分滚出 Lazy 视口（被 header 区域裁掉）时，此矩形小于 [sourceRect]。
     * [Rect.Zero] 表示未记录，视为与 [sourceRect] 相同。
     */
    val sourceVisibleRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /** 目标节点经祖先裁剪后的可见矩形，语义同 [sourceVisibleRect] */
    val targetVisibleRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /** 当前共享元素处于哪个显示阶段 */
    val phase: MutableState<GeometryPhase> = mutableStateOf(GeometryPhase.Source)

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
        phase.value = GeometryPhase.Forward
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy,
                visibilityThreshold = PROGRESS_VISIBILITY_THRESHOLD
            )
        )
        phase.value = GeometryPhase.Target
    }

    /**
     * 反向动画从当前进度回到 0f（用于 pop 时复原）。
     */
    suspend fun animateReverse() {
        phase.value = GeometryPhase.Reverse
        progress.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy,
                visibilityThreshold = PROGRESS_VISIBILITY_THRESHOLD
            )
        )
        phase.value = GeometryPhase.Source
    }

    /** 将进度 snap 到 0f（无动画重置） */
    suspend fun reset() {
        phase.value = GeometryPhase.Source
        progress.snapTo(0f)
    }

    /**
     * 源节点的显示策略：
     * - Source：始终显示
     * - Forward：在动画最开始的极短时间内继续显示，避免 overlay 首次接管时闪空
     * - Reverse / Target：隐藏
     */
    fun shouldShowSource(): Boolean =
        when (phase.value) {
            GeometryPhase.Source -> true
            GeometryPhase.Forward -> progress.value <= SOURCE_HANDOFF_PROGRESS
            GeometryPhase.Target, GeometryPhase.Reverse -> false
        }

    /**
     * 目标节点的显示策略：
     * - 仅在 Target 阶段显示
     * - Forward / Reverse 期间交由 overlay 接管
     */
    fun shouldShowTarget(): Boolean =
        when (phase.value) {
            GeometryPhase.Target -> true
            GeometryPhase.Source, GeometryPhase.Forward, GeometryPhase.Reverse -> false
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

    /**
     * 计算当前帧浮层内容的**裁剪矩形**（窗口坐标系）。
     *
     * 在 [sourceVisibleRect] 与 [targetVisibleRect] 之间按 [progress] 插值：
     * 起飞时裁剪框与源节点真实可见区域一致（被 header 裁掉 / 被浮层遮挡的部分不显示），
     * 飞行过程中逐渐过渡到目标可见区域，避免被遮挡部分突然"弹"到最顶层。
     *
     * @param sourceOcclusions 源侧的额外遮挡矩形（如 BottomPlayerBar 这类兄弟浮层，
     *   不参与祖先裁剪，需要显式扣除）
     */
    fun getClipBounds(sourceOcclusions: List<Rect> = emptyList()): Rect {
        val t = progress.value
        var src = sourceVisibleRect.value.takeIf { it != Rect.Zero } ?: sourceRect.value
        sourceOcclusions.forEach { src = src.subtractOccluder(it) }
        val dst = targetVisibleRect.value.takeIf { it != Rect.Zero } ?: targetRect.value
        return Rect(
            left = lerp(src.left, dst.left, t),
            top = lerp(src.top, dst.top, t),
            right = lerp(src.right, dst.right, t),
            bottom = lerp(src.bottom, dst.bottom, t)
        )
    }

    /**
     * 从矩形中扣除一个遮挡矩形，结果仍以矩形近似（按被完全跨越的方向收缩边缘）。
     * 典型场景：底部播放条横向铺满屏幕、遮住封面下缘，此时将 bottom 收缩到遮挡物上缘。
     */
    private fun Rect.subtractOccluder(o: Rect): Rect {
        if (!overlaps(o)) return this
        val coversHorizontally = o.left <= left && o.right >= right
        val coversVertically = o.top <= top && o.bottom >= bottom
        return when {
            coversHorizontally && coversVertically -> Rect(left, top, left, top)
            coversHorizontally -> when {
                o.top <= top && o.bottom >= bottom -> Rect(left, top, left, top)
                o.top <= top -> copy(top = o.bottom)
                o.bottom >= bottom -> copy(bottom = o.top)
                else -> this // 遮挡物嵌在中间，无法用单矩形表达，保持原样
            }
            coversVertically -> when {
                o.left <= left && o.right >= right -> Rect(left, top, left, top)
                o.left <= left -> copy(left = o.right)
                o.right >= right -> copy(right = o.left)
                else -> this
            }
            else -> this // 仅遮住一角，矩形裁剪无法表达，保持原样
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction

    companion object {
        /** 几何过渡弹簧刚度（250 ≈ 略软于导航弹簧 300，视觉上更流畅） */
        private const val SPRING_STIFFNESS = 250f

        /**
         * 飞行进度弹簧的收敛阈值。
         *
         * 进度是 0..1 的归一化值，最终会放大到近千像素的飞行距离：
         * 默认阈值 0.01 意味着弹簧在离终点还差约 1%（可达 ~10px）时即判定完成，
         * 最后一帧直接吸附到终点值，交接瞬间产生可见跳变。
         * 收紧到 0.0005（≈0.5px）后落点在亚像素级，交接无感。
         */
        private const val PROGRESS_VISIBILITY_THRESHOLD = 0.0005f

        /**
         * 正向飞行开始时源节点延迟交棒的进度阈值。
         * 让 overlay 有一两个绘制帧完成图片接管，避免点击瞬间闪烁。
         */
        private const val SOURCE_HANDOFF_PROGRESS = 0.08f
    }
}
