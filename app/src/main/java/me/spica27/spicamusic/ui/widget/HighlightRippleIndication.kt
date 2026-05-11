package me.spica27.spicamusic.ui.widget

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// AGSL shader source
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ripple shape shader (API 33 / Android 13+).
 *
 * Center-based expanding disc with pre-computed alpha passed as a uniform.
 *
 * Uniforms:
 *   size            – component bounds in px (float2)
 *   time            – normalized animation clock 0.0..0.5 s (float)
 *   backgroundAlpha – pre-computed alpha: triangle peak 0.0875 at time=0.25 (float)
 */
private const val RIPPLE_AGSL = """
uniform float2 size;
uniform float  time;
uniform float  backgroundAlpha;

float easeOutCubic(float t) {
    float inv = 1.0 - t;
    return 1.0 - inv * inv * inv;
}

half4 main(float2 coord) {
    // pressProgress reaches 1.0 at time = 0.25 s (raw 250)
    float pressProgress = clamp(time / 0.25, 0.0, 1.0);
    float2 center = size * 0.5;
    float maxR = length(center) * 1.1;
    float radius = maxR * easeOutCubic(pressProgress);

    float dist = distance(coord, center);
    float softEdge = 1.0 - smoothstep(radius - 12.0, radius, dist);

    return half4(1.0, 1.0, 1.0, backgroundAlpha * softEdge);
}
"""

// ─────────────────────────────────────────────────────────────────────────────
// Blend modes
// ─────────────────────────────────────────────────────────────────────────────

/** ADD: brightens the destination – ideal for light/dark backgrounds where a glow is desired. */
private val LightRippleXfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)

/** SRC_OVER: standard alpha-composite – used when ADD would over-saturate. */
private val DarkRippleXfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)

// ─────────────────────────────────────────────────────────────────────────────
// Indication node
// ─────────────────────────────────────────────────────────────────────────────

/**
 * [DrawModifierNode] that draws an expanding white disc ripple on press/hover/focus.
 *
 * Matches testApp2's RippleIndicationNode design:
 *   - Ripple expands from component **center** (not touch point).
 *   - Alpha is a symmetric triangle: 0 → 0.0875 peak at raw time 250 → 0.
 *   - Frame loop driven by [withFrameNanos]; [shouldAutoInvalidate] = false.
 *   - [MutatorMutex] cancels in-flight animation on the next interaction.
 *   - API 33+: AGSL [android.graphics.RuntimeShader] with ADD/SrcOver xfermode.
 *   - API < 33: plain [ContentDrawScope.drawCircle] fallback with same alpha formula.
 */
private class SpicaRippleIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color = Color(0x1A000000),
    private val isLightRipple: Boolean = true,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private val mutatorMutex = MutatorMutex()
    private val composePaint = Paint()

    /** RuntimeShader cached between frames; nulled after release to free GPU resources. */
    private var rippleShaderRef: Any? = null // android.graphics.RuntimeShader

    /** Raw animation clock: 0 = idle, 1..249 = pressing, 250..499 = releasing. */
    private var rawTime = 0f
    private var isFocused = false
    private var isHovered = false

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        coroutineScope.launch {
                            mutatorMutex.mutate {
                                rawTime = 0f
                                animateTo(target = 250f)
                            }
                        }
                    }

                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> {
                        coroutineScope.launch {
                            mutatorMutex.mutate {
                                animateTo(target = 500f)
                                rawTime = 0f
                                rippleShaderRef = null
                                invalidateDraw()
                            }
                        }
                    }

                    is HoverInteraction.Enter -> {
                        isHovered = true
                        invalidateDraw()
                    }
                    is HoverInteraction.Exit -> {
                        isHovered = false
                        invalidateDraw()
                    }
                    is FocusInteraction.Focus -> {
                        isFocused = true
                        invalidateDraw()
                    }
                    is FocusInteraction.Unfocus -> {
                        isFocused = false
                        invalidateDraw()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        rippleShaderRef = null
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    /**
     * Linearly animates [rawTime] from its current value to [target] at a rate of
     * 1000 units/second (mirrors testApp2: `duration = (target - current) / 1000`).
     * Calls [invalidateDraw] on every frame so [draw] is re-invoked.
     */
    private suspend fun animateTo(target: Float) {
        val start = rawTime
        val durationSec = (target - start) / 1000f
        if (durationSec <= 0f) return
        val startNanos = withFrameNanos { it }
        var done = false
        while (!done) {
            withFrameNanos { frameNanos ->
                val elapsed = (frameNanos - startNanos) / 1_000_000_000f
                val t = (elapsed / durationSec).coerceIn(0f, 1f)
                rawTime = start + (target - start) * t
                invalidateDraw()
                done = t >= 1f
            }
        }
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    override fun ContentDrawScope.draw() {
        drawContent()

        if (isHovered || isFocused) {
            drawRect(color = color)
        }

        val time = rawTime.toInt()
        if (time <= 0 || time >= 500) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawShaderRipple(time)
        } else {
            drawCanvasRipple(time)
        }
    }

    /**
     * AGSL shader ripple (API 33+).
     *
     * Sets all three uniforms each frame: [size] (may change on layout), and the
     * per-frame [backgroundAlpha] / [time] values. Uses ADD xfermode for light
     * ripple (matches testApp2 LightRippleXfermode), SrcOver for dark.
     */
    @Suppress("NewApi")
    private fun ContentDrawScope.drawShaderRipple(time: Int) {
        if (rippleShaderRef == null) {
            rippleShaderRef = android.graphics.RuntimeShader(RIPPLE_AGSL)
        }
        val runtimeShader = rippleShaderRef as android.graphics.RuntimeShader
        val f = time.toFloat()
        runtimeShader.setFloatUniform("size", size.width, size.height)
        runtimeShader.setFloatUniform("backgroundAlpha", (1f - abs(0.5f - f / 500f) * 2f) * 0.0875f)
        runtimeShader.setFloatUniform("time", f / 1000f)
        composePaint.asFrameworkPaint().apply {
            shader = runtimeShader
            xfermode = if (isLightRipple) LightRippleXfermode else DarkRippleXfermode
        }
        drawIntoCanvas { canvas ->
            canvas.drawRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                paint = composePaint,
            )
        }
    }

    /**
     * Fallback canvas ripple (API < 33).
     *
     * Replicates the same center-based expansion and triangle-alpha formula as the
     * AGSL path, without requiring RuntimeShader.
     */
    private fun ContentDrawScope.drawCanvasRipple(time: Int) {
        val f = time.toFloat()
        val backgroundAlpha = (1f - abs(0.5f - f / 500f) * 2f) * 0.0875f
        val pressProgress = (f / 250f).coerceIn(0f, 1f)
        val eased = 1f - (1f - pressProgress).pow(3f)
        val center =
            androidx.compose.ui.geometry
                .Offset(size.width / 2f, size.height / 2f)
        val maxRadius = sqrt((size.width / 2f).pow(2) + (size.height / 2f).pow(2)) * 1.1f
        drawCircle(
            color = Color.White.copy(alpha = backgroundAlpha),
            radius = maxRadius * eased,
            center = center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

object HighlightRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node =
        SpicaRippleIndicationNode(interactionSource, isLightRipple = false)

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)
}

object PrimaryRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node =
        SpicaRippleIndicationNode(interactionSource, Color(0x870a32ff), isLightRipple = true)

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)
}

@Composable
fun Modifier.highLightClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier =
    this.clickable(
        enabled = enabled,
        indication = HighlightRippleIndication,
        interactionSource = interactionSource,
        onClick = onClick,
    )

@Composable
fun Modifier.primaryClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier =
    this.clickable(
        enabled = enabled,
        indication = PrimaryRippleIndication,
        interactionSource = interactionSource,
        onClick = onClick,
    )
