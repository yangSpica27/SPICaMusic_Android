package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun rememberClickHighlightIndication(
    color: Color = MaterialTheme.colorScheme.onSurface,
    focusedAlpha: Float = 0.1f,
    backgroundAlpha: Float = 0.0875f,
    rippleAlpha: Float = 0.14f,
): Indication =
    remember(color, focusedAlpha, backgroundAlpha, rippleAlpha) {
        ClickHighlightIndication(
            color = color,
            focusedAlpha = focusedAlpha,
            backgroundAlpha = backgroundAlpha,
            rippleAlpha = rippleAlpha,
        )
    }

@Stable
fun Modifier.clickHighlight(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier =
    composed {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            interactionSource = source,
            indication = rememberClickHighlightIndication(),
            onClick = onClick,
        )
    }

private data class ClickHighlightIndication(
    private val color: Color,
    private val focusedAlpha: Float,
    private val backgroundAlpha: Float,
    private val rippleAlpha: Float,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ClickHighlightNode(
            interactionSource = interactionSource,
            color = color,
            focusedAlpha = focusedAlpha,
            backgroundAlpha = backgroundAlpha,
            rippleAlpha = rippleAlpha,
        )
}

private class ClickHighlightNode(
    private var interactionSource: InteractionSource,
    private var color: Color,
    private var focusedAlpha: Float,
    private var backgroundAlpha: Float,
    private var rippleAlpha: Float,
) : Modifier.Node(),
    DrawModifierNode {
    private val progress = Animatable(0f)
    private val presses = mutableSetOf<PressInteraction.Press>()
    private val hovers = mutableSetOf<HoverInteraction.Enter>()
    private val focuses = mutableSetOf<FocusInteraction.Focus>()
    private var animationJob: Job? = null
    private var pressPosition = Offset.Unspecified
    private var rippleActive = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        presses += interaction
                        pressPosition = interaction.pressPosition
                        animatePressed()
                    }

                    is PressInteraction.Release -> {
                        presses -= interaction.press
                        if (presses.isEmpty()) {
                            animateReleased()
                        }
                    }

                    is PressInteraction.Cancel -> {
                        presses -= interaction.press
                        if (presses.isEmpty()) {
                            animateReleased()
                        }
                    }

                    is HoverInteraction.Enter -> {
                        hovers += interaction
                        invalidateDraw()
                    }

                    is HoverInteraction.Exit -> {
                        hovers -= interaction.enter
                        invalidateDraw()
                    }

                    is FocusInteraction.Focus -> {
                        focuses += interaction
                        invalidateDraw()
                    }

                    is FocusInteraction.Unfocus -> {
                        focuses -= interaction.focus
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        animationJob?.cancel()
        presses.clear()
        hovers.clear()
        focuses.clear()
        rippleActive = false
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        if (hovers.isNotEmpty() || focuses.isNotEmpty()) {
            drawRect(color = color.copy(alpha = focusedAlpha))
        }

        if (!rippleActive) {
            return
        }

        val value = progress.value.coerceIn(0f, 1f)
        if (value <= 0f || value >= 1f) {
            return
        }

        val center =
            if (pressPosition.isSpecified) {
                Offset(
                    x = pressPosition.x.coerceIn(0f, size.width),
                    y = pressPosition.y.coerceIn(0f, size.height),
                )
            } else {
                Offset(size.width / 2f, size.height / 2f)
            }

        val backgroundIntensity = 1f - kotlin.math.abs(0.5f - value) * 2f
        if (backgroundIntensity > 0f) {
            drawRect(color = color.copy(alpha = backgroundAlpha * backgroundIntensity))
        }

        val radius = maxDistanceToCorner(center) * (0.25f + value * 0.85f)
        val rippleIntensity = (1f - value).coerceIn(0f, 1f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    colorStops =
                        arrayOf(
                            0f to color.copy(alpha = rippleAlpha * rippleIntensity),
                            0.6f to color.copy(alpha = rippleAlpha * 0.45f * rippleIntensity),
                            1f to Color.Transparent,
                        ),
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
    }

    private fun animatePressed() {
        animationJob?.cancel()
        animationJob =
            coroutineScope.launch {
                rippleActive = true
                progress.snapTo(0f)
                invalidateDraw()
                progress.animateTo(
                    targetValue = 0.5f,
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing),
                ) {
                    invalidateDraw()
                }
            }
    }

    private fun animateReleased() {
        animationJob?.cancel()
        animationJob =
            coroutineScope.launch {
                val duration = ((1f - progress.value) * 500).roundToInt().coerceAtLeast(1)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = duration, easing = LinearEasing),
                ) {
                    invalidateDraw()
                }
                rippleActive = false
                progress.snapTo(0f)
                invalidateDraw()
            }
    }

    private fun ContentDrawScope.maxDistanceToCorner(center: Offset): Float =
        maxOf(
            hypot(center.x, center.y),
            hypot(size.width - center.x, center.y),
            hypot(center.x, size.height - center.y),
            hypot(size.width - center.x, size.height - center.y),
        )
}
