package me.spica27.spicamusic.ui.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.hypot
import kotlin.coroutines.resume

/**
 * CircularRevealSwitch-style theme transition without recreating the Activity.
 *
 * The old Compose frame is retained in the window while the new frame is revealed with Android's
 * native [ViewAnimationUtils.createCircularReveal]. Keeping the transition in the decor layer
 * produces the same sharp circular edge as the reference implementation while preserving the
 * player and navigation state.
 */
@Stable
class ThemeRevealController internal constructor() {
    private var pendingOrigin: Offset? = null
    private var armedAtMs: Long = 0L
    private var recentPointerOrigin: Offset? = null
    private var pointerRecordedAtMs: Long = 0L

    fun arm(originInWindow: Offset) {
        if (!originInWindow.x.isFinite() || !originInWindow.y.isFinite()) return
        pendingOrigin = originInWindow
        armedAtMs = SystemClock.uptimeMillis()
    }

    internal fun recordPointer(originInWindow: Offset) {
        if (!originInWindow.x.isFinite() || !originInWindow.y.isFinite()) return
        recentPointerOrigin = originInWindow
        pointerRecordedAtMs = SystemClock.uptimeMillis()
    }

    internal fun consumeOrigin(): Offset? {
        val now = SystemClock.uptimeMillis()
        val explicitlyArmedOrigin = pendingOrigin
        pendingOrigin = null
        explicitlyArmedOrigin
            ?.takeIf { now - armedAtMs <= EXPLICIT_ORIGIN_VALIDITY_MS }
            ?.let { return it }

        val pointerOrigin = recentPointerOrigin
        recentPointerOrigin = null
        return pointerOrigin?.takeIf { now - pointerRecordedAtMs <= POINTER_ORIGIN_VALIDITY_MS }
    }

    private companion object {
        const val EXPLICIT_ORIGIN_VALIDITY_MS = 15_000L
        const val POINTER_ORIGIN_VALIDITY_MS = 8_000L
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
    val controller = remember { ThemeRevealController() }
    var displayedDarkTheme by remember { mutableStateOf(targetDarkTheme) }
    var displayedThemeColor by remember { mutableStateOf(targetThemeColor) }
    var hostOriginInWindow by remember { mutableStateOf(Offset.Zero) }

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

        val oldFrame =
            runCatching {
                view.drawToBitmap(Bitmap.Config.ARGB_8888)
            }.getOrNull()
        if (oldFrame == null) {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        val defaultInset = 48f * view.resources.displayMetrics.density
        val revealOrigin =
            armedOrigin
                ?: Offset(
                    x = (view.width - defaultInset).coerceAtLeast(0f),
                    y = defaultInset.coerceAtMost(view.height.toFloat()),
                )
        val revealMode =
            if (darkChanged && !targetDarkTheme) {
                RevealMode.Shrink
            } else {
                RevealMode.Expand
            }
        val durationMillis = if (darkChanged) 520L else 480L
        val transition =
            runCatching {
                NativeRevealTransition.attach(
                    anchor = view,
                    oldFrame = oldFrame,
                    originInWindow = revealOrigin,
                )
            }.getOrNull()

        if (transition == null) {
            oldFrame.recycle()
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        try {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            // Draw the new Material color scheme behind the retained old frame.
            withFrameNanos { }
            withFrameNanos { }

            when (revealMode) {
                RevealMode.Expand -> {
                    runCatching {
                        view.drawToBitmap(Bitmap.Config.ARGB_8888)
                    }.getOrNull()?.let { newFrame ->
                        transition.expandNewFrame(
                            newFrame = newFrame,
                            durationMillis = durationMillis,
                        )
                    }
                }

                RevealMode.Shrink -> {
                    transition.shrinkOldFrame(durationMillis = durationMillis)
                }
            }
        } finally {
            transition.close()
        }
    }

    CompositionLocalProvider(LocalThemeRevealController provides controller) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        hostOriginInWindow = coordinates.boundsInWindow().topLeft
                    }
                    .pointerInput(controller) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            controller.recordPointer(hostOriginInWindow + down.position)
                        }
                    },
        ) {
            content(displayedDarkTheme, displayedThemeColor)
        }
    }
}

private enum class RevealMode {
    Expand,
    Shrink,
}

/**
 * Window-level screenshot layers matching CircularRevealSwitch's implementation strategy.
 *
 * The old frame is attached before Compose receives the new theme, preventing a one-frame flash.
 * For expand transitions a screenshot of the new frame is revealed above it. For shrink
 * transitions the old screenshot itself shrinks while the live new UI remains underneath.
 */
private class NativeRevealTransition private constructor(
    private val root: ViewGroup,
    private val anchor: View,
    private val oldFrame: Bitmap,
    private val originInAnchor: Offset,
) {
    private val ownedLayers = mutableListOf<ImageView>()
    private val ownedBitmaps = mutableListOf(oldFrame)
    private val oldLayer =
        createLayer(oldFrame, initiallyVisible = true).also { layer ->
            ownedLayers += layer
        }

    suspend fun expandNewFrame(
        newFrame: Bitmap,
        durationMillis: Long,
    ) {
        ownedBitmaps += newFrame
        val newLayer = createLayer(newFrame, initiallyVisible = false)
        ownedLayers += newLayer

        // Give DecorView one frame to measure and position the new screenshot layer.
        withFrameNanos { }
        val animator = createRevealAnimator(newLayer, startRadius = 0f, endRadius = maximumRadius())
        animator.duration = durationMillis
        newLayer.visibility = View.VISIBLE
        animator.awaitEnd()
    }

    suspend fun shrinkOldFrame(durationMillis: Long) {
        val animator = createRevealAnimator(oldLayer, startRadius = maximumRadius(), endRadius = 0f)
        animator.duration = durationMillis
        animator.awaitEnd()
    }

    fun close() {
        ownedLayers.asReversed().forEach { layer ->
            layer.setImageDrawable(null)
            (layer.parent as? ViewGroup)?.removeView(layer)
        }
        ownedLayers.clear()
        ownedBitmaps.forEach { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
        ownedBitmaps.clear()
    }

    private fun createLayer(
        bitmap: Bitmap,
        initiallyVisible: Boolean,
    ): ImageView {
        val anchorLocation = IntArray(2).also(anchor::getLocationInWindow)
        val rootLocation = IntArray(2).also(root::getLocationInWindow)
        val params =
            FrameLayout.LayoutParams(anchor.width, anchor.height).apply {
                leftMargin = anchorLocation[0] - rootLocation[0]
                topMargin = anchorLocation[1] - rootLocation[1]
            }
        val layer =
            ImageView(anchor.context).apply {
                layoutParams = params
                scaleType = ImageView.ScaleType.FIT_XY
                setImageBitmap(bitmap)
                isClickable = true
                isFocusable = true
                visibility = if (initiallyVisible) View.VISIBLE else View.INVISIBLE
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        root.addView(layer)
        layer.bringToFront()
        return layer
    }

    private fun createRevealAnimator(
        layer: View,
        startRadius: Float,
        endRadius: Float,
    ): Animator =
        ViewAnimationUtils
            .createCircularReveal(
                layer,
                originInAnchor.x.toInt(),
                originInAnchor.y.toInt(),
                startRadius,
                endRadius,
            ).apply {
                interpolator = REFERENCE_INTERPOLATOR
            }

    private fun maximumRadius(): Float {
        val x = originInAnchor.x
        val y = originInAnchor.y
        return maxOf(
            hypot(x, y),
            hypot(anchor.width - x, y),
            hypot(x, anchor.height - y),
            hypot(anchor.width - x, anchor.height - y),
        )
    }

    companion object {
        private val REFERENCE_INTERPOLATOR = PathInterpolator(0.455f, 0.03f, 0.515f, 0.955f)

        fun attach(
            anchor: View,
            oldFrame: Bitmap,
            originInWindow: Offset,
        ): NativeRevealTransition {
            require(anchor.width > 0 && anchor.height > 0)
            val root = anchor.rootView as? ViewGroup ?: error("Decor root is not a ViewGroup")
            val anchorLocation = IntArray(2).also(anchor::getLocationInWindow)
            val localOrigin =
                Offset(
                    x = (originInWindow.x - anchorLocation[0]).coerceIn(0f, anchor.width.toFloat()),
                    y = (originInWindow.y - anchorLocation[1]).coerceIn(0f, anchor.height.toFloat()),
                )
            return NativeRevealTransition(
                root = root,
                anchor = anchor,
                oldFrame = oldFrame,
                originInAnchor = localOrigin,
            )
        }
    }
}

private suspend fun Animator.awaitEnd() {
    suspendCancellableCoroutine { continuation ->
        var finished = false
        addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (finished) return
                    finished = true
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (finished) return
                    finished = true
                    if (continuation.isActive) continuation.resume(Unit)
                }
            },
        )
        continuation.invokeOnCancellation { cancel() }
        start()
    }
}
