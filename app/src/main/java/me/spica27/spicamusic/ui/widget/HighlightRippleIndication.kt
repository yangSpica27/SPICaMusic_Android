package me.spica27.spicamusic.ui.widget

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// AGSL shader source
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ripple shape shader (API 33 / Android 13+).
 *
 * Draws a soft-edged, ease-out expanding disc centred on the touch point using
 * standard SrcOver blending. This avoids the ADD mode that saturates light
 * backgrounds to white.
 *
 * Uniforms:
 *   size   – component bounds in px (float2)
 *   time   – animation clock 0..500: 0..250 = press expanding, 250..500 = release fading
 *   origin – press / touch position in px (float2)
 */
private const val RIPPLE_AGSL = """
uniform float2 size;
uniform float  time;
uniform float2 origin;

float easeOutCubic(float t) {
    float inv = 1.0 - t;
    return 1.0 - inv * inv * inv;
}

half4 main(float2 coord) {
    float pressProgress   = clamp(time / 250.0, 0.0, 1.0);
    float releaseProgress = clamp((time - 250.0) / 250.0, 0.0, 1.0);

    // Radius: ease-out expansion until it covers the farthest corner from origin
    float2 farCorner = max(abs(origin), abs(size - origin));
    float  maxR      = length(farCorner) * 1.1;
    float  radius    = maxR * easeOutCubic(pressProgress);

    // Alpha: peaks at press, fades on release; kept low for SrcOver overlay
    float alpha = pressProgress * (1.0 - releaseProgress) * 0.22;

    // Soft disc boundary (12 px feather)
    float dist     = distance(coord, origin);
    float softEdge = 1.0 - smoothstep(radius - 12.0, radius, dist);

    return half4(1.0, 1.0, 1.0, alpha * softEdge);
}
"""

// ─────────────────────────────────────────────────────────────────────────────
// Indication node
// ─────────────────────────────────────────────────────────────────────────────

/**
 * [DrawModifierNode] that draws an expanding bright ripple on press/hover/focus.
 *
 * Animation timing (mirrors testApp2 FloatAnimator 0→500 range):
 *   - Press:   animatable 0 → 250 over 250 ms (linear)
 *   - Release: animatable current → 500 over remaining ms, then reset to 0
 *
 * Draw pipeline:
 *   - API 33+: custom AGSL [android.graphics.RuntimeShader] disc + ADD xfermode
 *   - API < 33: plain [android.graphics.Canvas.drawCircle] with [BlendMode.Plus]
 *
 * Reading [animatable].value inside [ContentDrawScope.draw] registers a Compose
 * snapshot dependency; the node is automatically re-drawn every frame the
 * animation progresses – no explicit [invalidateDraw] needed for the ripple path.
 */
private class SpicaRippleIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color = Color(0x1A000000),
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    /** Animation clock: 0 = idle, 1..249 = pressing, 250..499 = releasing. */
    private val animatable = Animatable(0f)

    /** Press origin in the component's local coordinate space. */
    private var pressOrigin: Offset = Offset.Zero

    // mutableStateOf so that changes are tracked by Compose's snapshot system:
    // reading these in draw() subscribes the draw scope, and assignment in the
    // coroutine automatically schedules a redraw (no invalidateDraw() needed).
    private var isFocused by mutableStateOf(false)
    private var isHovered by mutableStateOf(false)

    /**
     * Cached RuntimeShader (API 33+) stored as [Any] to avoid NewApi type errors
     * at the call-site; cast is guarded by the runtime SDK check.
     */
    private var rippleShaderRef: Any? = null // android.graphics.RuntimeShader

    /** Reused Compose [Paint] for the shader draw path (avoids per-frame allocation). */
    private val composePaint = Paint()

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressOrigin = interaction.pressPosition
                        animatable.snapTo(0f)
                        // Press phase: 0 → 250 in exactly 250 ms (linear velocity)
                        animatable.animateTo(
                            targetValue = 250f,
                            animationSpec = tween(durationMillis = 250, easing = LinearEasing),
                        )
                    }

                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> {
                        // Release phase: current → 500, duration = remaining distance in ms
                        // (mirrors original: Linear(duration = (500 - current) / 1000 seconds))
                        val remaining = (500f - animatable.value).toInt().coerceAtLeast(1)
                        animatable.animateTo(
                            targetValue = 500f,
                            animationSpec =
                                tween(
                                    durationMillis = remaining,
                                    easing = LinearEasing,
                                ),
                        )
                        animatable.snapTo(0f)
                    }

                    // Assignment to mutableStateOf delegates triggers automatic redraw
                    is HoverInteraction.Enter -> isHovered = true
                    is HoverInteraction.Exit -> isHovered = false
                    is FocusInteraction.Focus -> isFocused = true
                    is FocusInteraction.Unfocus -> isFocused = false
                }
            }
        }
    }

    override fun onDetach() {
        rippleShaderRef = null
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    override fun ContentDrawScope.draw() {
        drawContent()

        // Reading animatable.value here creates a Compose snapshot read dependency.
        // Compose UI will re-invoke draw() automatically whenever the value changes
        // during animation – no explicit invalidateDraw() required.
        val time = animatable.value

        // Hover / focus: 10 % black overlay (matches testApp2 FocusedRippleColor)
        if (isHovered || isFocused) {
            drawRect(color = color)
        }

        // Ripple: only drawn during the active animation window (0 < time < 500)
        if (time > 0f && time < 500f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                drawShaderRipple(time)
            } else {
                drawCanvasRipple(time)
            }
        }
    }

    /**
     * Draws the ripple using an AGSL [android.graphics.RuntimeShader] (API 33+).
     *
     * The shader is cached in [rippleShaderRef] and re-used across frames.
     * Uniforms `size`, `time`, and `origin` are updated every frame.
     * Uses default SrcOver blending — avoids the white blowout caused by ADD mode.
     */
    @Suppress("NewApi") // Caller already guards with Build.VERSION.SDK_INT >= TIRAMISU
    private fun ContentDrawScope.drawShaderRipple(time: Float) {
        if (rippleShaderRef == null) {
            rippleShaderRef = android.graphics.RuntimeShader(RIPPLE_AGSL)
        }
        val shader = rippleShaderRef as? android.graphics.RuntimeShader ?: return

        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setFloatUniform("origin", pressOrigin.x, pressOrigin.y)

        composePaint.asFrameworkPaint().shader = shader

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
     * Fallback ripple drawn with [ContentDrawScope.drawCircle] (API < 33).
     *
     * Uses default SrcOver blending with a semi-transparent white overlay,
     * matching the visual intent without the white blowout caused by ADD mode.
     */
    private fun ContentDrawScope.drawCanvasRipple(time: Float) {
        val pressProgress = (time / 250f).coerceIn(0f, 1f)
        val releaseProgress = ((time - 250f) / 250f).coerceIn(0f, 1f)
        val alpha = pressProgress * (1f - releaseProgress) * 0.22f

        val eased = 1f - (1f - pressProgress).pow(3f)
        val maxRadius =
            sqrt(
                maxOf(pressOrigin.x, size.width - pressOrigin.x).pow(2) +
                    maxOf(pressOrigin.y, size.height - pressOrigin.y).pow(2),
            ) * 1.1f

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = maxRadius * eased,
            center = pressOrigin,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Indication factory that produces an expanding white ripple on every interaction.
 *
 * Use directly as the `indication` parameter of [Modifier.clickable], or use the
 * convenience [Modifier.highLightClickable] extension.
 *
 * Visual behaviour:
 * - **Press**: a white semi-transparent disc expands from the touch point over 250 ms,
 *   blended with SrcOver at ~22% alpha peak. Works correctly on both light and dark
 *   backgrounds (no ADD-mode white blowout).
 * - **Release / cancel**: the disc fades over the remaining duration to 500 ms total.
 * - **Hover / focus**: a 10 % black overlay is drawn while the state is active.
 *
 * The shader path (API 33+) uses a custom AGSL disc whose radius eases out to cover
 * the farthest corner of the component. Devices below API 33 fall back to a plain
 * canvas circle with SrcOver blending.
 */
object HighlightRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node = SpicaRippleIndicationNode(interactionSource)

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)
}

object PrimaryRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): Modifier.Node =
        SpicaRippleIndicationNode(interactionSource, Color(0x870a32ff))

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
