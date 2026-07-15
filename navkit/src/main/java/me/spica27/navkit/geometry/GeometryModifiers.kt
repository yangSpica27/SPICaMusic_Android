package me.spica27.navkit.geometry

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

// ──────────────────────────────────────────────────────────────────────────
// 公共扩展函数
// ──────────────────────────────────────────────────────────────────────────

/**
 * 将此可组合元素标记为几何过渡的**源节点**，布局完成时自动把边界写入 [transition.sourceRect]。
 *
 * 典型用法：在列表行上附加此 modifier，当用户点击后触发导航，
 * NavigationStack 会以 [transition] 做共享元素过渡。
 *
 * 仅在静止（Source）阶段记录：飞行中/被上层场景覆盖期间，
 * 祖先场景的过渡变换（背景压缩、位移）会污染窗口坐标，
 * 冻结矩形保证返回飞行始终落在源节点的静止位置。
 *
 * @param transition 要关联的 [GeometryTransition] 实例
 */
fun Modifier.geometrySource(transition: GeometryTransition): Modifier =
    this.onGloballyPositioned { coords ->
        if (transition.phase.value == GeometryTransition.GeometryPhase.Source) {
            transition.sourceRect.value = coords.unclippedBoundsInWindow()
            transition.sourceVisibleRect.value = coords.boundsInWindow()
        }
    }

/**
 * 将此可组合元素标记为几何过渡的**目标节点**，布局完成时自动把边界写入 [transition.targetRect]。
 *
 * 典型用法：在目标页面的英雄图或标题上附加此 modifier。
 *
 * @param transition 要关联的 [GeometryTransition] 实例
 */
fun Modifier.geometryTarget(transition: GeometryTransition): Modifier =
    this.onGloballyPositioned { coords ->
        transition.targetRect.value = coords.unclippedBoundsInWindow()
        transition.targetVisibleRect.value = coords.boundsInWindow()
    }

/**
 * 将此可组合元素标记为几何过渡的**遮挡物**（如底部播放条这类悬浮在列表上方的兄弟浮层）。
 *
 * 遮挡物不是源节点的布局祖先，`boundsInWindow` 的祖先裁剪无法感知它；
 * 通过此 modifier 把它的窗口矩形登记到 [GeometryOccluders]，
 * 飞行浮层会在起飞侧裁剪框中扣除这些区域，避免被遮住的封面部分突然显示在最顶层。
 *
 * @param key 此遮挡物的唯一标识；节点从组合树移除时自动注销
 */
fun Modifier.geometryOccluder(key: Any): Modifier = this.then(GeometryOccluderElement(key))

/**
 * 计算节点在窗口坐标系中的**未裁剪**边界。
 *
 * [androidx.compose.ui.layout.boundsInWindow] 会按祖先（LazyColumn/LazyGrid 视口、窗口等）
 * 裁剪结果：当封面被 header 或 BottomPlayerBar 遮挡而部分滚出视口时，
 * 记录到的矩形被裁掉一截，导致飞行动画起点/终点错位变形。
 * 这里改用 positionInWindow（不裁剪的左上角坐标）+ 实际布局尺寸还原完整矩形。
 */
private fun LayoutCoordinates.unclippedBoundsInWindow(): Rect =
    Rect(offset = positionInWindow(), size = size.toSize())

// ──────────────────────────────────────────────────────────────────────────
// 高性能节点实现（Modifier.Node — Draw 阶段读取，避免 Composition 开销）
// ──────────────────────────────────────────────────────────────────────────

/**
 * [GeometryStateNode]：同时实现 [DrawModifierNode] 和 [GlobalPositionAwareModifierNode]。
 *
 * - [GlobalPositionAwareModifierNode.onGloballyPositioned]：在 Layout 阶段完成后记录边界，
 *   并写入 [transition.sourceRect] 或 [transition.targetRect]（由 [isSource] 决定）。
 * - [DrawModifierNode.draw]：直接透传，不绘制任何内容；
 *   DrawModifierNode 的存在使节点在绘制相关状态变化时可被重绘。
 *
 * 继承 [Modifier.Node] 而非 `Modifier.composed`，
 * 避免每次重组时重新创建实例（性能优化）。
 */
internal class GeometryStateNode(
    var transition: GeometryTransition,
    var isSource: Boolean
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override val shouldAutoInvalidate: Boolean get() = false

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // 与 Modifier.geometrySource 保持一致：源矩形仅在静止阶段记录
        if (isSource && transition.phase.value != GeometryTransition.GeometryPhase.Source) return
        val bounds: Rect = coordinates.unclippedBoundsInWindow()
        val visibleBounds: Rect = coordinates.boundsInWindow()
        if (isSource) {
            transition.sourceRect.value = bounds
            transition.sourceVisibleRect.value = visibleBounds
        } else {
            transition.targetRect.value = bounds
            transition.targetVisibleRect.value = visibleBounds
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
    }
}

/**
 * [GeometryStateElement]：[GeometryStateNode] 的 [ModifierNodeElement] 工厂。
 *
 * 必须是 data class，以便 Compose 通过结构相等性判断是否需要更新节点，
 * 避免不必要的节点重建。
 */
internal data class GeometryStateElement(
    val transition: GeometryTransition,
    val isSource: Boolean
) : ModifierNodeElement<GeometryStateNode>() {

    override fun create(): GeometryStateNode =
        GeometryStateNode(transition, isSource)

    override fun update(node: GeometryStateNode) {
        node.transition = transition
        node.isSource = isSource
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "geometryState"
        properties["key"] = transition.key
        properties["isSource"] = isSource
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 遮挡物登记
// ──────────────────────────────────────────────────────────────────────────

/**
 * 全局遮挡物登记表：key → 窗口坐标矩形。
 *
 * 由 [geometryOccluder] 写入/注销，[me.spica27.navkit.stack.NavigationStack]
 * 的飞行浮层读取，用于在源侧裁剪框中扣除被悬浮 UI 遮挡的区域。
 * 使用 mutableStateMapOf 保证矩形变化（如播放条展开/收起）时浮层自动重绘。
 */
object GeometryOccluders {
    internal val rects = mutableStateMapOf<Any, Rect>()

    /** 当前所有遮挡矩形的快照 */
    fun current(): List<Rect> = rects.values.toList()
}

/** 记录遮挡物窗口矩形的节点；从组合树移除时自动注销 */
internal class GeometryOccluderNode(
    var occluderKey: Any
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        GeometryOccluders.rects[occluderKey] = coordinates.boundsInWindow()
    }

    override fun onDetach() {
        GeometryOccluders.rects.remove(occluderKey)
    }
}

internal data class GeometryOccluderElement(
    val occluderKey: Any
) : ModifierNodeElement<GeometryOccluderNode>() {

    override fun create(): GeometryOccluderNode = GeometryOccluderNode(occluderKey)

    override fun update(node: GeometryOccluderNode) {
        if (node.occluderKey != occluderKey) {
            GeometryOccluders.rects.remove(node.occluderKey)
            node.occluderKey = occluderKey
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "geometryOccluder"
        properties["key"] = occluderKey
    }
}
