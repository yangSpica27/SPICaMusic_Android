package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** 原播放条的形变参数；位移使用 tanh 限幅，回弹统一使用 0.6 / 200 的弹簧。 */
@Immutable
data class ElasticDragSpec(
    val pressedScale: Float = 1.1f,
    val maxStretch: Float = 0.1f,
    val maxTranslation: Dp = 16.dp,
) {
    init {
        require(pressedScale.isFinite() && pressedScale >= 1f)
        require(maxStretch.isFinite() && maxStretch >= 0f)
        require(maxTranslation.value.isFinite() && maxTranslation >= 0.dp)
    }
}

object ElasticDragDefaults {
    val Default = ElasticDragSpec()

    /** 对话框比播放条宽大，减小形变量，保留相同的阻力曲线和回弹节奏。 */
    val Dialog = ElasticDragSpec(pressedScale = 1.03f, maxStretch = 0.03f, maxTranslation = 8.dp)
}

/**
 * 触摸放大、方向性拉伸和松手回弹。只改变绘制图层，不改变布局占位。
 */
@Stable
fun Modifier.elasticDrag(
    spec: ElasticDragSpec = ElasticDragDefaults.Default,
    enabled: Boolean = true,
): Modifier = then(ElasticDragElement(spec, enabled))

private data class ElasticDragElement(
    val spec: ElasticDragSpec,
    val enabled: Boolean,
) : ModifierNodeElement<ElasticDragNode>() {
    override fun create() = ElasticDragNode(spec, enabled)

    override fun update(node: ElasticDragNode) = node.update(spec, enabled)

    override fun InspectorInfo.inspectableProperties() {
        name = "elasticDrag"
        properties["spec"] = spec
        properties["enabled"] = enabled
    }
}

private class ElasticDragNode(
    private var spec: ElasticDragSpec,
    private var enabled: Boolean,
) : Modifier.Node(),
    LayoutModifierNode,
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {
    private var pressProgress = Animatable(0f)
    private var dragOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var pressJob: Job? = null
    private var dragJob: Job? = null
    private var activePointer: PointerId? = null
    private var blockedUntilUp = false
    private var accumulatedDrag = Offset.Zero

    // 指针节点留在未变换的坐标系，避免容器自身的位移再次进入手指位移计算。
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val reducedMotion = currentValueOf(LocalReducedMotion)
        val progress = if (enabled && !reducedMotion) pressProgress.value else 0f
        val drag = if (enabled && !reducedMotion) dragOffset.value else Offset.Zero
        val baseScale = 1f + (spec.pressedScale - 1f) * progress
        val angle = atan2(drag.y, drag.x)
        val stretch = tanh(drag.getDistance() * 0.002f) * spec.maxStretch
        translationX = spec.maxTranslation.toPx() * tanh(drag.x * 0.002f)
        translationY = spec.maxTranslation.toPx() * tanh(drag.y * 0.002f)
        scaleX = baseScale * (1f + abs(cos(angle)) * stretch)
        scaleY = baseScale * (1f + abs(sin(angle)) * stretch)
        transformOrigin =
            TransformOrigin(
                0.5f - cos(angle) * stretch * 1.25f,
                0.5f - sin(angle) * stretch * 1.25f,
            )
    }

    override val shouldAutoInvalidate = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Final) return
        if (!enabled || currentValueOf(LocalReducedMotion)) {
            if (activePointer != null) reset()
            return
        }

        val pressedCount = pointerEvent.changes.count { it.pressed }
        if (blockedUntilUp) {
            if (pressedCount == 0) blockedUntilUp = false
            return
        }
        if (pressedCount > 1) {
            release()
            blockedUntilUp = true
            return
        }

        val pointer = activePointer
        if (pointer == null) {
            val down = pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return
            if (down.position.x !in 0f..bounds.width.toFloat() ||
                down.position.y !in 0f..bounds.height.toFloat()
            ) {
                return
            }
            activePointer = down.id
            // 再次触摸正在回弹的容器时，从当前形变接手，避免跳回原点。
            dragJob?.cancel()
            accumulatedDrag = dragOffset.value
            animatePress(1f)
            return
        }

        val change = pointerEvent.changes.firstOrNull { it.id == pointer }
        if (change == null || !change.pressed) {
            release()
            return
        }
        val delta = change.positionChangeIgnoreConsumed()
        if (delta != Offset.Zero && change.isConsumed) {
            release()
            blockedUntilUp = true
        } else if (delta != Offset.Zero) {
            accumulatedDrag += delta
            val target = accumulatedDrag
            dragJob?.cancel()
            dragJob =
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    dragOffset.snapTo(target)
                }
        }
    }

    override fun onCancelPointerInput() {
        release()
        blockedUntilUp = false
    }

    override fun onDetach() = reset()

    override fun onReset() = reset()

    fun update(
        spec: ElasticDragSpec,
        enabled: Boolean,
    ) {
        this.spec = spec
        this.enabled = enabled
        if (!enabled) reset()
        if (isAttached) updateLayerBlock(layerBlock)
    }

    private fun animatePress(target: Float) {
        pressJob?.cancel()
        pressJob =
            coroutineScope.launch {
                pressProgress.animateTo(target, spring(dampingRatio = 0.6f, stiffness = 200f))
            }
    }

    private fun release() {
        if (activePointer == null) return
        activePointer = null
        accumulatedDrag = Offset.Zero
        animatePress(0f)
        dragJob?.cancel()
        dragJob =
            coroutineScope.launch {
                dragOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.6f, stiffness = 200f))
            }
    }

    private fun reset() {
        pressJob?.cancel()
        dragJob?.cancel()
        pressJob = null
        dragJob = null
        activePointer = null
        blockedUntilUp = false
        accumulatedDrag = Offset.Zero
        pressProgress = Animatable(0f)
        dragOffset = Animatable(Offset.Zero, Offset.VectorConverter)
        if (isAttached) updateLayerBlock(layerBlock)
    }
}
