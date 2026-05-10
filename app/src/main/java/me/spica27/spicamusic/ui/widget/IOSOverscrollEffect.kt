package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class IOSOverscrollEffect(
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val onOverscrollOffset: (value: Float) -> Unit = {},
) : OverscrollEffect {
    private val overscrollAmountAnimatable = Animatable(0f)
    private var length = 1f

    private val cubicEasing = CubicBezierEasing(0.5f, 0.5f, 1.0f, 0.25f)

    private val animationSpec: SpringSpec<Float> = spring(stiffness = Spring.StiffnessLow)

    init {
        scope.launch {
            snapshotFlow { overscrollAmountAnimatable.value }.collect {
                onOverscrollOffset(
                    cubicEasing.transform(it / (length * 1.5f)) * length,
                )
            }
        }
    }

    private fun transformOverscroll(value: Float): Float = cubicEasing.transform(value / (length * 1.5f)) * length

    private fun getRelevantDelta(offset: Offset): Float =
        when (orientation) {
            Orientation.Vertical -> offset.y
            Orientation.Horizontal -> offset.x
        }

    private fun getRelevantVelocity(velocity: Velocity): Float =
        when (orientation) {
            Orientation.Vertical -> velocity.y
            Orientation.Horizontal -> velocity.x
        }

    private fun calculateOverscroll(available: Float): Float {
        val previous = overscrollAmountAnimatable.value
        val newValue = previous + available
        return when {
            previous > 0 -> newValue.coerceAtLeast(0f)
            previous < 0 -> newValue.coerceAtMost(0f)
            else -> newValue
        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val relevantDelta = getRelevantDelta(delta)
        val currentOverscroll = overscrollAmountAnimatable.value

        if (currentOverscroll != 0f && source != NestedScrollSource.SideEffect) {
            val newOverscroll = calculateOverscroll(relevantDelta)
            scope.launch {
                overscrollAmountAnimatable.snapTo(newOverscroll)
            }
            return delta
        }

        val consumedByScroll = performScroll(delta)
        val remaining = delta - consumedByScroll
        val relevantRemaining = getRelevantDelta(remaining)

        if (abs(relevantRemaining) > 0.01f && source == NestedScrollSource.UserInput) {
            val newOverscroll = calculateOverscroll(relevantRemaining)
            scope.launch {
                overscrollAmountAnimatable.snapTo(newOverscroll)
            }
        }

        return consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        val relevantVelocity = getRelevantVelocity(velocity)
        val currentOverscroll = overscrollAmountAnimatable.value

        if (currentOverscroll != 0f && relevantVelocity != 0f) {
            val previousSign = currentOverscroll.sign

            val predictedEndValue =
                exponentialDecay<Float>().calculateTargetValue(
                    initialValue = currentOverscroll,
                    initialVelocity = relevantVelocity,
                )

            if (predictedEndValue.sign == previousSign) {
                overscrollAmountAnimatable.animateTo(
                    targetValue = 0f,
                    initialVelocity = relevantVelocity,
                    animationSpec = animationSpec,
                )
            } else {
                try {
                    overscrollAmountAnimatable.animateDecay(
                        initialVelocity = relevantVelocity,
                        animationSpec = exponentialDecay(),
                    ) {
                        if (value.sign != previousSign) {
                            scope.launch {
                                overscrollAmountAnimatable.snapTo(0f)
                            }
                        }
                    }
                } catch (_: Exception) {
                    overscrollAmountAnimatable.animateTo(0f, animationSpec = animationSpec)
                }
            }
            return
        }

        val consumed = performFling(velocity)
        val available = velocity - consumed
        val availableRelevant = getRelevantVelocity(available)

        overscrollAmountAnimatable.animateTo(
            targetValue = 0f,
            initialVelocity = availableRelevant,
            animationSpec = animationSpec,
        )
    }

    override val isInProgress: Boolean
        get() = false

    override val node: DelegatableNode =
        object : Modifier.Node(), LayoutModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)

                length =
                    when (orientation) {
                        Orientation.Vertical -> placeable.height.toFloat()
                        Orientation.Horizontal -> placeable.width.toFloat()
                    }

                return layout(placeable.width, placeable.height) {
                    val transformedOffset = transformOverscroll(overscrollAmountAnimatable.value)

                    val offsetValue =
                        when (orientation) {
                            Orientation.Vertical ->
                                IntOffset(
                                    x = 0,
                                    y = transformedOffset.roundToInt(),
                                )

                            Orientation.Horizontal ->
                                IntOffset(
                                    x = transformedOffset.roundToInt(),
                                    y = 0,
                                )
                        }
                    placeable.placeRelativeWithLayer(offsetValue.x, offsetValue.y)
                }
            }
        }
}

@Composable
fun rememberIOSOverScrollEffect(
    orientation: Orientation,
    scope: CoroutineScope = rememberCoroutineScope(),
    onOverscrollOffset: (value: Float) -> Unit = {},
): OverscrollEffect =
    remember(
        scope,
        orientation,
        onOverscrollOffset,
    ) {
        IOSOverscrollEffect(scope, orientation, onOverscrollOffset)
    }
