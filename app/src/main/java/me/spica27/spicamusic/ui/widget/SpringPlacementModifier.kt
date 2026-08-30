package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

/**
 * Animates an item's placement after a lookahead layout changes.
 *
 * The list can update its scroll position immediately, while each lyric line
 * approaches its new position with an independent spring. Manual scrolling
 * uses snap placement so the content stays directly under the user's finger.
 */
private class SpringPlacementModifierNode(
    var lookaheadScope: LookaheadScope,
    var itemKey: Any,
    var isManualScrolling: Boolean,
    var stiffness: Float,
) : Modifier.Node(),
    ApproachLayoutModifierNode {
    private var offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
    private var isFirstFrame = true

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(lookaheadCoordinates: LayoutCoordinates): Boolean {
        val target =
            with(lookaheadScope) {
                lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
            }
        offsetAnimation.updateTarget(target, coroutineScope, animationSpec())
        return !offsetAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val coordinates = coordinates
            if (coordinates == null) {
                placeable.place(0, 0)
                return@layout
            }

            val target =
                with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
                }
            val animatedOffset =
                offsetAnimation.updateTarget(
                    target,
                    coroutineScope,
                    animationSpec(),
                )
            isFirstFrame = false

            val placementOffset =
                with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                }
            val delta = animatedOffset - placementOffset
            placeable.place(delta.x, delta.y)
        }
    }

    private fun animationSpec(): FiniteAnimationSpec<IntOffset> =
        if (isFirstFrame || isManualScrolling) {
            snap()
        } else {
            spring(dampingRatio = 0.95f, stiffness = stiffness)
        }

    override fun onReset() {
        resetAnimation()
    }

    fun updateState(
        newScope: LookaheadScope,
        newKey: Any,
        newIsManualScrolling: Boolean,
        newStiffness: Float,
    ) {
        lookaheadScope = newScope
        isManualScrolling = newIsManualScrolling
        stiffness = newStiffness
        if (itemKey != newKey) {
            itemKey = newKey
            resetAnimation()
        }
    }

    private fun resetAnimation() {
        offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
        isFirstFrame = true
    }
}

private data class SpringPlacementNodeElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val stiffness: Float,
) : ModifierNodeElement<SpringPlacementModifierNode>() {
    override fun update(node: SpringPlacementModifierNode) {
        node.updateState(lookaheadScope, itemKey, isManualScrolling, stiffness)
    }

    override fun create(): SpringPlacementModifierNode = SpringPlacementModifierNode(lookaheadScope, itemKey, isManualScrolling, stiffness)
}

internal fun Modifier.springPlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float,
): Modifier =
    then(
        SpringPlacementNodeElement(
            lookaheadScope = lookaheadScope,
            itemKey = itemKey,
            isManualScrolling = isManualScrolling,
            stiffness = stiffness,
        ),
    )
