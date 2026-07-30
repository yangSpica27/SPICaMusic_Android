package me.spica27.spicamusic.ui.theme

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Compose-native adaptation of CircularRevealSwitch's screenshot/reveal idea. The current app
 * frame remains above the newly composed theme while an expanding transparent circle uncovers it.
 * This avoids Activity recreation and keeps the player/navigation state intact.
 */
@Stable
class ThemeRevealController internal constructor() {
    private var pendingOrigin: Offset? = null
    private var armedAtMs: Long = 0L

    fun arm(originInWindow: Offset) {
        if (!originInWindow.x.isFinite() || !originInWindow.y.isFinite()) return
        pendingOrigin = originInWindow
        armedAtMs = SystemClock.uptimeMillis()
    }

    internal fun consumeOrigin(): Offset? {
        val origin = pendingOrigin
        pendingOrigin = null
        return origin?.takeIf {
            SystemClock.uptimeMillis() - armedAtMs <= ORIGIN_VALIDITY_MS
        }
    }

    private companion object {
        const val ORIGIN_VALIDITY_MS = 15_000L
    }
}

val LocalThemeRevealController =
    staticCompositionLocalOf<ThemeRevealController> {
        ThemeRevealController()
    }

@Stable
class ThemeRevealOriginState internal constructor(
    private val controller: ThemeRevealController,
) {
    private var coordinates: LayoutCoordinates? = null

    internal fun update(value: LayoutCoordinates) {
        coordinates = value
    }

    fun armFromCenter() {
        val bounds = coordinates?.boundsInWindow() ?: return
        controller.arm(bounds.center)
    }
}

@Composable
fun rememberThemeRevealOriginState(): ThemeRevealOriginState {
    val controller = LocalThemeRevealController.current
    return remember(controller) { ThemeRevealOriginState(controller) }
}

fun Modifier.themeRevealOrigin(state: ThemeRevealOriginState): Modifier = onGloballyPositioned(state::update)

@Composable
fun CircularRevealThemeHost(
    targetDarkTheme: Boolean,
    targetThemeColor: Color,
    content: @Composable (darkTheme: Boolean, themeColor: Color) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val controller = remember { ThemeRevealController() }
    var displayedDarkTheme by remember { mutableStateOf(targetDarkTheme) }
    var displayedThemeColor by remember { mutableStateOf(targetThemeColor) }
    var oldFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var revealOrigin by remember { mutableStateOf(Offset.Zero) }
    val progress = remember { Animatable(1f) }

    androidx.compose.runtime.LaunchedEffect(targetDarkTheme, targetThemeColor) {
        val darkChanged = displayedDarkTheme != targetDarkTheme
        val colorChanged = displayedThemeColor != targetThemeColor
        if (!darkChanged && !colorChanged) return@LaunchedEffect

        val armedOrigin = controller.consumeOrigin()
        val shouldReveal = darkChanged || armedOrigin != null
        if (!shouldReveal || view.width <= 0 || view.height <= 0) {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        val snapshot =
            runCatching {
                view.drawToBitmap(android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
            }.getOrNull()
        if (snapshot == null) {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        val defaultInset = with(density) { 48f * density.density }
        revealOrigin =
            armedOrigin
                ?: Offset(
                    x = (view.width - defaultInset).coerceAtLeast(0f),
                    y = defaultInset.coerceAtMost(view.height.toFloat()),
                )
        oldFrame = snapshot
        progress.snapTo(0f)
        displayedDarkTheme = targetDarkTheme
        displayedThemeColor = targetThemeColor
        // Wait for the new Material color scheme to be drawn underneath the retained frame.
        withFrameNanos { }
        withFrameNanos { }
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = if (darkChanged) 680 else 560,
                    easing = FastOutSlowInEasing,
                ),
        )
        oldFrame = null
    }

    CompositionLocalProvider(LocalThemeRevealController provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(displayedDarkTheme, displayedThemeColor)
            oldFrame?.let { frame ->
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            },
                ) {
                    drawIntoCanvas { canvas ->
                        val source =
                            android.graphics.Rect(
                                0,
                                0,
                                frame.width,
                                frame.height,
                            )
                        val destination =
                            android.graphics.Rect(
                                0,
                                0,
                                size.width.roundToInt(),
                                size.height.roundToInt(),
                            )
                        canvas.nativeCanvas.drawBitmap(
                            frame.asAndroidBitmap(),
                            source,
                            destination,
                            null,
                        )
                    }
                    val maximumRadius =
                        maxOf(
                            hypot(revealOrigin.x, revealOrigin.y),
                            hypot(size.width - revealOrigin.x, revealOrigin.y),
                            hypot(revealOrigin.x, size.height - revealOrigin.y),
                            hypot(size.width - revealOrigin.x, size.height - revealOrigin.y),
                        )
                    drawCircle(
                        color = Color.Transparent,
                        radius = maximumRadius * progress.value,
                        center = revealOrigin,
                        blendMode = BlendMode.Clear,
                    )
                }
            }
        }
    }
}
