package me.spica27.navkit.geometry

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
 * @param transition 要关联的 [GeometryTransition] 实例
 */
fun Modifier.geometrySource(transition: GeometryTransition): Modifier =
    this.onGloballyPositioned { coords ->
        transition.sourceRect.value = coords.boundsInWindow()
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
        transition.targetRect.value = coords.boundsInWindow()
    }

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
        val bounds: Rect = coordinates.boundsInWindow()
        if (isSource) {
            transition.sourceRect.value = bounds
        } else {
            transition.targetRect.value = bounds
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
