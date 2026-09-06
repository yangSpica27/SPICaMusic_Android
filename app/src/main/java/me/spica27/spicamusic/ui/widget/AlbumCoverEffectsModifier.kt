package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DEFAULT_MAX_TILT_DEGREES = 6f
private const val CAMERA_DISTANCE_MULTIPLIER = 4f

private class HolographicCoverNode(
    var enabled: Boolean,
    var maxTiltDegrees: Float,
    var cornerRadius: Dp,
    var highlightColor: Color,
    var spectralColor: Color,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    PointerInputModifierNode {
    private var pointerX = 0f
    private var pointerY = 0f
    private var highlightStrength = 0f
    private var pointerActive = false
    private var settleJob: Job? = null

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        rotationX = -pointerY * maxTiltDegrees
        rotationY = pointerX * maxTiltDegrees
        cameraDistance =
            (maxOf(size.width, size.height) * CAMERA_DISTANCE_MULTIPLIER)
                .coerceAtLeast(1f)
        transformOrigin = TransformOrigin.Center
    }

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!enabled || highlightStrength <= 0.001f) return

        val center =
            Offset(
                x = size.width * (0.5f - pointerX * 0.32f),
                y = size.height * (0.5f - pointerY * 0.32f),
            )
        val brush =
            Brush.radialGradient(
                colorStops =
                    arrayOf(
                        0f to highlightColor,
                        0.24f to spectralColor,
                        0.58f to highlightColor.copy(alpha = 0.35f),
                        1f to Color.Transparent,
                    ),
                center = center,
                radius = size.maxDimension * 0.78f,
            )
        val radius = cornerRadius.toPx()
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(radius, radius),
            alpha = 0.24f * highlightStrength.coerceIn(0f, 1f),
            blendMode = BlendMode.Screen,
        )
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Main || !enabled) return

        val pressedChange = pointerEvent.changes.firstOrNull { it.pressed }
        if (pressedChange != null && bounds.width > 0 && bounds.height > 0) {
            settleJob?.cancel()
            settleJob = null
            pointerActive = true
            pointerX = ((pressedChange.position.x / bounds.width) * 2f - 1f).coerceIn(-1f, 1f)
            pointerY = ((pressedChange.position.y / bounds.height) * 2f - 1f).coerceIn(-1f, 1f)
            highlightStrength = 1f
            invalidateEffects()
        } else if (pointerActive) {
            pointerActive = false
            settleToRest()
        }
    }

    override fun onCancelPointerInput() {
        if (pointerActive) {
            pointerActive = false
            settleToRest()
        }
    }

    override fun onDetach() {
        settleJob?.cancel()
        settleJob = null
        clearEffects()
    }

    override fun onReset() {
        settleJob?.cancel()
        settleJob = null
        clearEffects()
        if (isAttached) invalidateEffects()
    }

    fun update(
        enabled: Boolean,
        maxTiltDegrees: Float,
        cornerRadius: Dp,
        highlightColor: Color,
        spectralColor: Color,
    ) {
        this.enabled = enabled
        this.maxTiltDegrees = maxTiltDegrees
        this.cornerRadius = cornerRadius
        this.highlightColor = highlightColor
        this.spectralColor = spectralColor

        if (!isAttached) return
        if (!enabled) {
            settleJob?.cancel()
            settleJob = null
            clearEffects()
        }
        invalidateEffects()
    }

    private fun settleToRest() {
        if (!isAttached) {
            clearEffects()
            return
        }

        val startX = pointerX
        val startY = pointerY
        val startHighlight = highlightStrength
        settleJob?.cancel()
        settleJob =
            coroutineScope.launch {
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                ) { value, _ ->
                    pointerX = startX * value
                    pointerY = startY * value
                    highlightStrength = startHighlight * value.coerceIn(0f, 1f)
                    invalidateEffects()
                }
                clearEffects()
                invalidateEffects()
            }
    }

    private fun clearEffects() {
        pointerActive = false
        pointerX = 0f
        pointerY = 0f
        highlightStrength = 0f
    }

    private fun invalidateEffects() {
        if (!isAttached) return
        updateLayerBlock(layerBlock)
        invalidateDraw()
    }
}

private data class HolographicCoverElement(
    val enabled: Boolean,
    val maxTiltDegrees: Float,
    val cornerRadius: Dp,
    val highlightColor: Color,
    val spectralColor: Color,
) : ModifierNodeElement<HolographicCoverNode>() {
    override fun create(): HolographicCoverNode =
        HolographicCoverNode(
            enabled = enabled,
            maxTiltDegrees = maxTiltDegrees,
            cornerRadius = cornerRadius,
            highlightColor = highlightColor,
            spectralColor = spectralColor,
        )

    override fun update(node: HolographicCoverNode) {
        node.update(enabled, maxTiltDegrees, cornerRadius, highlightColor, spectralColor)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "holographicCoverTilt"
        properties["enabled"] = enabled
        properties["maxTiltDegrees"] = maxTiltDegrees
    }
}

/** 触摸倾斜与反向移动高光。 */
@Stable
fun Modifier.holographicCoverTilt(
    enabled: Boolean = true,
    maxTiltDegrees: Float = DEFAULT_MAX_TILT_DEGREES,
    cornerRadius: Dp = 16.dp,
    highlightColor: Color = Color.White,
    spectralColor: Color = Color.Cyan,
): Modifier =
    then(
        HolographicCoverElement(
            enabled = enabled,
            maxTiltDegrees = maxTiltDegrees.coerceIn(0f, 12f),
            cornerRadius = cornerRadius,
            highlightColor = highlightColor,
            spectralColor = spectralColor,
        ),
    )
