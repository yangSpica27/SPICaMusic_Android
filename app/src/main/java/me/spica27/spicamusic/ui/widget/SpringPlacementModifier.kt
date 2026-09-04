package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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

/** 歌词行位移曲线，先缓后快再减速。 */
internal val LyricLineMoveEasing = CubicBezierEasing(0.4f, 0.1f, 0f, 1f)

/**
 * 处理前瞻布局后的歌词行位移。
 * 列表先立即更新滚动位置，各行再按时长和错峰延迟补间；手动滚动时直接贴合手指。
 */
private class LinePlacementModifierNode(
    var lookaheadScope: LookaheadScope,
    var itemKey: Any,
    var isManualScrolling: Boolean,
    var durationMillis: Int,
    var delayMillis: Int,
    var initialOffsetY: Int,
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
        updatePlacementTarget(target)
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
            val animatedOffset = updatePlacementTarget(target)

            val placementOffset =
                with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                }
            val delta = animatedOffset - placementOffset
            placeable.place(delta.x, delta.y)
        }
    }

    private fun animationSpec(): FiniteAnimationSpec<IntOffset> =
        if (isManualScrolling) {
            snap()
        } else {
            tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = LyricLineMoveEasing,
            )
        }

    /** 首次组合的底部歌词先下移，再补间到目标位置。 */
    private fun updatePlacementTarget(target: IntOffset): IntOffset {
        if (isFirstFrame) {
            isFirstFrame = false
            if (initialOffsetY != 0 && !isManualScrolling) {
                offsetAnimation.updateTarget(
                    target + IntOffset(0, initialOffsetY),
                    coroutineScope,
                    snap(),
                )
                return offsetAnimation.updateTarget(target, coroutineScope, animationSpec())
            }
            return offsetAnimation.updateTarget(target, coroutineScope, snap())
        }
        return offsetAnimation.updateTarget(target, coroutineScope, animationSpec())
    }

    override fun onReset() {
        resetAnimation()
    }

    fun updateState(
        newScope: LookaheadScope,
        newKey: Any,
        newIsManualScrolling: Boolean,
        newDurationMillis: Int,
        newDelayMillis: Int,
        newInitialOffsetY: Int,
    ) {
        lookaheadScope = newScope
        isManualScrolling = newIsManualScrolling
        durationMillis = newDurationMillis
        delayMillis = newDelayMillis
        initialOffsetY = newInitialOffsetY
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

private data class LinePlacementNodeElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val durationMillis: Int,
    val delayMillis: Int,
    val initialOffsetY: Int,
) : ModifierNodeElement<LinePlacementModifierNode>() {
    override fun update(node: LinePlacementModifierNode) {
        node.updateState(
            lookaheadScope,
            itemKey,
            isManualScrolling,
            durationMillis,
            delayMillis,
            initialOffsetY,
        )
    }

    override fun create(): LinePlacementModifierNode =
        LinePlacementModifierNode(
            lookaheadScope,
            itemKey,
            isManualScrolling,
            durationMillis,
            delayMillis,
            initialOffsetY,
        )
}

/** 添加歌词行位移动画，支持时长和错峰延迟。 */
internal fun Modifier.linePlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    durationMillis: Int,
    delayMillis: Int,
    initialOffsetY: Int = 0,
): Modifier =
    then(
        LinePlacementNodeElement(
            lookaheadScope = lookaheadScope,
            itemKey = itemKey,
            isManualScrolling = isManualScrolling,
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            initialOffsetY = initialOffsetY,
        ),
    )
