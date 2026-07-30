package me.spica27.navkit.popup

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import me.spica27.navkit.motion.EaseOutEmphasized
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import kotlin.math.roundToInt

/**
 * 锚定弹出菜单场景：绑定前一个页面中的某个组件（锚点），打开时锚点组件
 * **原地过渡成菜单**，背景页面模糊（由 [me.spica27.navkit.stack.NavigationStack]
 * 对下层 StackScene 施加 BlurEffect）并变暗（本场景的 scrim），只保留菜单清晰突出焦点。
 *
 * ## 动画模型
 * 复用 [DialogScene] 的 [enterProgress] 生命周期，改为菜单专用的 emphasized 缓动。
 * 单一进度同时驱动四层效果（全部在 Layout/Draw 阶段读取，零重组）：
 * 1. **容器 morph**：矩形从锚点冻结矩形插值到菜单目标矩形，圆角从锚点圆角插值到 [menuCornerRadius]
 * 2. **幽灵交接**：锚点组件隐藏后，[AnchorGhostContent] 复制品钉在锚点原位（容器之上），
 *    进度前段淡出；[MenuContent] 中后段淡入——图标"原地"过渡成菜单
 * 3. **背景聚焦**：scrim 变暗到 [scrimMaxAlpha]（模糊由 NavigationStack 自动叠加）
 * 4. **投影**：随进度浮起到 [menuElevation]
 *
 * ## 布局策略
 * 菜单内容始终以**自然尺寸**测量（约束每帧相同，命中测量缓存，避免文字每帧重排），
 * 容器上报插值后的小尺寸并靠 graphicsLayer clip 做揭示，等价 Material 容器变换。
 * 目标位置优先与锚点同角对齐（右半屏锚点右缘对齐、向下展开），放不下时翻转，
 * 最终 clamp 进 [screenMargin] 安全区。
 *
 * ## 使用示例
 * ```kotlin
 * // 前一个页面：
 * val sortAnchor = rememberPopupMenuAnchorState()
 * Icon(..., modifier = Modifier
 *     .popupMenuAnchor(sortAnchor)
 *     .clip(CircleShape).background(...)
 *     .clickable { if (!sortAnchor.isOpen) path.push(MySortMenuScene(sortAnchor, ...)) })
 *
 * // 菜单场景：
 * class MySortMenuScene(anchor: PopupMenuAnchorState) : PopupMenuScene(anchor) {
 *     @Composable override fun MenuContent() { /* 菜单项列表 */ }
 *     @Composable override fun AnchorGhostContent() { /* 锚点图标的复制品 */ }
 * }
 * ```
 */
abstract class PopupMenuScene(
    val anchorState: PopupMenuAnchorState,
) : DialogScene() {

    // ──────────────────────────────────────────────────────────────────────
    // 可覆写的样式参数
    // ──────────────────────────────────────────────────────────────────────

    /** 菜单完全展开态的容器圆角 */
    open val menuCornerRadius: Dp = 20.dp

    /** 菜单与屏幕边缘的最小安全间距 */
    open val screenMargin: Dp = 16.dp

    /** 菜单完全展开态的投影高度 */
    open val menuElevation: Dp = 12.dp

    /** 遮罩最大不透明度（配合背景模糊压暗前一个页面） */
    open val scrimMaxAlpha: Float = 0.35f

    override val enterAnimationSpec: AnimationSpec<Float>
        get() = tween(ENTER_DURATION_MILLIS, easing = EaseOutEmphasized)

    override val exitAnimationSpec: AnimationSpec<Float>
        get() = tween(EXIT_DURATION_MILLIS, easing = POPUP_EXIT_EASING)

    /** 锚点态容器圆角（px）；默认取短边一半（圆形/胶囊锚点） */
    protected open fun anchorCornerRadiusPx(density: Density, size: Size): Float =
        size.minDimension / 2f

    /** 菜单展开态的容器背景色 */
    @Composable
    protected open fun menuContainerColor(): Color =
        MaterialTheme.colorScheme.surfaceContainerHigh

    /** 锚点态的容器背景色（morph 起点，应与锚点组件的背景一致） */
    @Composable
    protected open fun anchorContainerColor(): Color = menuContainerColor()

    // ──────────────────────────────────────────────────────────────────────
    // 抽象内容
    // ──────────────────────────────────────────────────────────────────────

    /** 菜单内容（以自然尺寸测量；超高时子类自行加滚动） */
    @Composable
    protected abstract fun MenuContent()

    /**
     * 锚点组件的视觉复制品（不含背景，背景由容器色 [anchorContainerColor] 提供）。
     * 打开瞬间原组件隐藏，由它在同一位置接管显示，保证过渡无缝。
     */
    @Composable
    protected abstract fun AnchorGhostContent()

    // ──────────────────────────────────────────────────────────────────────
    // 生命周期：占有/释放锚点
    // ──────────────────────────────────────────────────────────────────────

    /** push 时占有锚点：冻结锚点矩形（源组件此时仍可见，等幽灵上屏再隐藏） */
    override suspend fun onPush() {
        anchorState.owner.value = this
        super.onPush()
    }

    /**
     * 进场动画启动点。此时 waitAppear 已确认场景首帧完成布局——
     * 幽灵复制品已钉在锚点原位并绘制，隐藏源组件实现零闪烁交接。
     */
    override suspend fun onAppear() {
        anchorState.sourceHidden.value = true
        super.onAppear()
    }

    /** pop 完成后释放锚点：源组件恢复显示、矩形恢复刷新 */
    override suspend fun onPop() {
        super.onPop()
        if (anchorState.owner.value === this) {
            anchorState.sourceHidden.value = false
            anchorState.owner.value = null
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Composable 内容
    // ──────────────────────────────────────────────────────────────────────

    @Composable
    final override fun DialogContent() = Unit

    @Composable
    final override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val density = LocalDensity.current

        val menuColor = menuContainerColor()
        val anchorColor = anchorContainerColor()
        val menuRadiusPx = with(density) { menuCornerRadius.toPx() }
        val marginPx = with(density) { screenMargin.toPx() }
        val elevationPx = with(density) { menuElevation.toPx() }
        // edge-to-edge 下场景容器铺满整个窗口，目标矩形需避开系统栏
        val systemBarInsets = WindowInsets.systemBars

        // 锚点矩形在 onPush 时已冻结，组合期读取即为稳定值
        val anchorRect = anchorState.anchorRect.value
        val ghostWidth = with(density) { anchorRect.width.toDp() }
        val ghostHeight = with(density) { anchorRect.height.toDp() }

        val geometry = remember { PopupMenuGeometry() }

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize()
        ) {
            // 遮罩：随进度压暗背景（模糊由 NavigationStack 叠加），点击关闭
            val scrimInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = enterProgress.value * scrimMaxAlpha }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null
                    ) { path.pop(scene) }
            )

            // morph 容器：锚点矩形 ⇄ 菜单矩形
            Box(
                modifier = Modifier
                    // 位置：放置阶段读取插值矩形（不触发重组）
                    .absoluteOffset {
                        val bounds = geometry.currentBounds(
                            enterProgress.value,
                            anchorState.anchorRect.value
                        )
                        IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
                    }
                    // 圆角/裁剪/投影：Draw 阶段读取进度
                    .graphicsLayer {
                        val p = enterProgress.value
                        val bounds = geometry.currentBounds(p, anchorState.anchorRect.value)
                        val radius = lerp(
                            anchorCornerRadiusPx(this, bounds.size),
                            menuRadiusPx,
                            p
                        )
                        shape = RoundedCornerShape(radius)
                        clip = true
                        shadowElevation = p * elevationPx
                    }
                    // 容器背景：锚点色 → 菜单面色
                    .drawBehind {
                        drawRect(lerp(anchorColor, menuColor, enterProgress.value))
                    }
                    // 尺寸：内容始终按自然尺寸测量（约束恒定命中缓存），容器上报插值尺寸
                    .layout { measurable, constraints ->
                        val screenW = constraints.maxWidth
                        val screenH = constraints.maxHeight
                        // 安全区 = 系统栏 inset + screenMargin
                        val safeLeft = systemBarInsets.getLeft(this, layoutDirection) + marginPx
                        val safeTop = systemBarInsets.getTop(this) + marginPx
                        val safeRight = screenW - systemBarInsets.getRight(this, layoutDirection) - marginPx
                        val safeBottom = screenH - systemBarInsets.getBottom(this) - marginPx
                        val maxMenuW = (safeRight - safeLeft).roundToInt().coerceAtLeast(1)
                        val maxMenuH = (safeBottom - safeTop).roundToInt().coerceAtLeast(1)
                        val inner = measurable.measure(
                            Constraints(maxWidth = maxMenuW, maxHeight = maxMenuH)
                        )

                        val anchor = anchorState.anchorRect.value
                        val menuW = inner.width.toFloat()
                        val menuH = inner.height.toFloat()
                        // 展开方向：与锚点同角对齐，放不下再翻转，最终 clamp 进安全区
                        val alignRight = anchor.center.x > screenW / 2f
                        val growDown = anchor.top + menuH <= safeBottom
                        // 上界先与下界取 max，防止菜单占满安全区时区间因舍入倒挂
                        val maxLeft = (safeRight - menuW).coerceAtLeast(safeLeft)
                        val maxTop = (safeBottom - menuH).coerceAtLeast(safeTop)
                        val left = (if (alignRight) anchor.right - menuW else anchor.left)
                            .coerceIn(safeLeft, maxLeft)
                        val top = (if (growDown) anchor.top else anchor.bottom - menuH)
                            .coerceIn(safeTop, maxTop)

                        geometry.targetRect = Rect(Offset(left, top), Size(menuW, menuH))

                        val bounds = geometry.currentBounds(enterProgress.value, anchor)
                        val boundsW = bounds.width.roundToInt().coerceAtLeast(1)
                        val boundsH = bounds.height.roundToInt().coerceAtLeast(1)
                        layout(boundsW, boundsH) {
                            // 内容钉在展开原点角，随容器生长逐步揭示
                            val dx = if (alignRight) boundsW - inner.width else 0
                            val dy = if (growDown) 0 else boundsH - inner.height
                            inner.place(dx, dy)
                        }
                    }
            ) {
                Box(
                    Modifier.graphicsLayer {
                        alpha = menuContentAlpha(enterProgress.value)
                    }
                ) {
                    MenuContent()
                }
            }

            // 幽灵：钉在锚点原位（容器之上），进度前段可见——图标"原地"过渡成菜单。
            // 独立于容器摆放，目标矩形被安全区 clamp 时也不会漂移。
            Box(
                Modifier
                    .absoluteOffset {
                        val anchor = anchorState.anchorRect.value
                        IntOffset(anchor.left.roundToInt(), anchor.top.roundToInt())
                    }
                    .requiredSize(ghostWidth, ghostHeight)
                    .graphicsLayer { alpha = ghostAlpha(enterProgress.value) }
            ) {
                AnchorGhostContent()
            }
        }
    }

    private fun ghostAlpha(progress: Float): Float =
        1f - (progress / GHOST_FADE_END).coerceIn(0f, 1f)

    private fun menuContentAlpha(progress: Float): Float =
        ((progress - MENU_FADE_START) / (MENU_FADE_END - MENU_FADE_START)).coerceIn(0f, 1f)

    companion object {
        private const val ENTER_DURATION_MILLIS = 340
        private const val EXIT_DURATION_MILLIS = 220

        /** Material emphasized-accelerate：收回时迅速离场 */
        private val POPUP_EXIT_EASING = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

        /** 幽灵淡出结束进度 */
        private const val GHOST_FADE_END = 0.35f

        /** 菜单内容淡入起止进度 */
        private const val MENU_FADE_START = 0.15f
        private const val MENU_FADE_END = 0.6f
    }
}

/**
 * morph 几何的帧间共享数据：测量阶段写入目标矩形，
 * 放置/绘制阶段读取插值边界。同一帧内测量先于放置/绘制执行，无竞争。
 */
private class PopupMenuGeometry {
    var targetRect: Rect = Rect.Zero

    /** 锚点矩形与目标矩形按进度插值；异常态（矩形未记录）退化为目标位置渐显 */
    fun currentBounds(progress: Float, anchor: Rect): Rect {
        val start = if (anchor == Rect.Zero) targetRect else anchor
        val end = if (targetRect == Rect.Zero) start else targetRect
        return Rect(
            left = lerp(start.left, end.left, progress),
            top = lerp(start.top, end.top, progress),
            right = lerp(start.right, end.right, progress),
            bottom = lerp(start.bottom, end.bottom, progress),
        )
    }
}
