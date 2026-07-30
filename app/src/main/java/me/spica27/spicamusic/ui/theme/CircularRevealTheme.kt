package me.spica27.spicamusic.ui.theme

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.PathInterpolator
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

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
        // Preference restoration and cold-start player metadata can update the target shortly
        // after the first frame. Only a real user gesture is allowed to start a reveal; otherwise
        // a fixed app theme on a differently themed system would animate from the fallback corner.
        val shouldReveal = armedOrigin != null
        if (!shouldReveal || view.width <= 0 || view.height <= 0) {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        val oldFrame =
            runCatching {
                captureWindowFrame(view)
            }.onFailure {
                Log.e(REVEAL_LOG_TAG, "Unable to capture old frame", it)
            }.getOrNull()
        if (oldFrame == null) {
            displayedDarkTheme = targetDarkTheme
            displayedThemeColor = targetThemeColor
            return@LaunchedEffect
        }

        val revealOrigin = requireNotNull(armedOrigin)
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
                    revealMode = revealMode,
                )
            }.onFailure {
                Log.e(REVEAL_LOG_TAG, "Unable to attach reveal transition", it)
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
                RevealMode.Expand ->
                    transition.revealNewContent(durationMillis = durationMillis)

                RevealMode.Shrink ->
                    transition.shrinkOldFrame(durationMillis = durationMillis)
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
                    }.pointerInput(controller) {
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

private const val REVEAL_LOG_TAG = "SPICaThemeReveal"

private enum class RevealMode {
    Expand,
    Shrink,
}

/**
 * Screenshot layer matching CircularRevealSwitch's implementation strategy.
 *
 * The retained old frame is inserted into DecorView beside `android.R.id.content`, matching the
 * reference library. For EXPAND it sits underneath the temporarily hidden live content, then the
 * live new theme is circular-revealed. For SHRINK it sits above the already-updated live content
 * and shrinks away.
 */
private class NativeRevealTransition private constructor(
    private val decorView: ViewGroup,
    private val contentView: View,
    private val oldFrame: Bitmap,
    private val originInDecor: Offset,
    private val originInContent: Offset,
    private val revealMode: RevealMode,
) {
    private val originalContentVisibility = contentView.visibility
    private val originalContentBackground = contentView.background
    private val installedContentBackground =
        revealMode == RevealMode.Expand && originalContentBackground == null
    private val oldLayer =
        createOldFrameLayer().also { layer ->
            val contentIndex = decorView.indexOfChild(contentView)
            val insertIndex =
                when (revealMode) {
                    RevealMode.Expand -> contentIndex.takeIf { it >= 0 } ?: 0
                    RevealMode.Shrink ->
                        if (contentIndex >= 0) contentIndex + 1 else decorView.childCount
                }
            decorView.addView(layer, insertIndex)
        }

    init {
        if (installedContentBackground) {
            contentView.background = decorView.background
        }
        if (revealMode == RevealMode.Expand) {
            // Compose can recompose while invisible; the retained frame remains visible underneath.
            contentView.visibility = View.INVISIBLE
        }
    }

    suspend fun revealNewContent(durationMillis: Long) {
        require(revealMode == RevealMode.Expand)
        // Give the new Compose theme time to commit before making the live view visible.
        withFrameNanos { }
        val animator =
            createRevealAnimator(
                layer = contentView,
                center = originInContent,
                startRadius = 0f,
                endRadius = maximumRadius(contentView, originInContent),
            )
        animator.duration = durationMillis
        contentView.visibility = View.VISIBLE
        animator.awaitEnd()
    }

    suspend fun shrinkOldFrame(durationMillis: Long) {
        require(revealMode == RevealMode.Shrink)
        val animator =
            createRevealAnimator(
                layer = oldLayer,
                center = originInDecor,
                startRadius = maximumRadius(oldLayer, originInDecor),
                endRadius = 0f,
            )
        animator.duration = durationMillis
        animator.awaitEnd()
    }

    fun close() {
        contentView.visibility = originalContentVisibility
        if (installedContentBackground) {
            contentView.background = originalContentBackground
        }
        oldLayer.setImageDrawable(null)
        (oldLayer.parent as? ViewGroup)?.removeView(oldLayer)
        if (!oldFrame.isRecycled) oldFrame.recycle()
    }

    private fun createOldFrameLayer(): ImageView =
        ImageView(contentView.context).apply {
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            scaleType = ImageView.ScaleType.FIT_XY
            setImageBitmap(oldFrame)
            isClickable = true
            isFocusable = true
        }

    private fun createRevealAnimator(
        layer: View,
        center: Offset,
        startRadius: Float,
        endRadius: Float,
    ): Animator =
        ViewAnimationUtils
            .createCircularReveal(
                layer,
                center.x.toInt(),
                center.y.toInt(),
                startRadius,
                endRadius,
            ).apply {
                interpolator = REFERENCE_INTERPOLATOR
            }

    private fun maximumRadius(
        target: View,
        center: Offset,
    ): Float {
        val x = center.x
        val y = center.y
        return maxOf(
            hypot(x, y),
            hypot(target.width - x, y),
            hypot(x, target.height - y),
            hypot(target.width - x, target.height - y),
        )
    }

    companion object {
        private val REFERENCE_INTERPOLATOR = PathInterpolator(0.455f, 0.03f, 0.515f, 0.955f)

        fun attach(
            anchor: View,
            oldFrame: Bitmap,
            originInWindow: Offset,
            revealMode: RevealMode,
        ): NativeRevealTransition {
            require(anchor.width > 0 && anchor.height > 0)
            val decorView =
                anchor.rootView as? ViewGroup
                    ?: error("Window DecorView is not a ViewGroup")
            val contentView =
                decorView.findViewById<View>(android.R.id.content)
                    ?: error("Activity content view is unavailable")
            val decorLocation = IntArray(2).also(decorView::getLocationInWindow)
            val contentLocation = IntArray(2).also(contentView::getLocationInWindow)
            val originInDecor =
                Offset(
                    x =
                        (originInWindow.x - decorLocation[0])
                            .coerceIn(0f, decorView.width.toFloat()),
                    y =
                        (originInWindow.y - decorLocation[1])
                            .coerceIn(0f, decorView.height.toFloat()),
                )
            val originInContent =
                Offset(
                    x =
                        (originInWindow.x - contentLocation[0])
                            .coerceIn(0f, contentView.width.toFloat()),
                    y =
                        (originInWindow.y - contentLocation[1])
                            .coerceIn(0f, contentView.height.toFloat()),
                )
            return NativeRevealTransition(
                decorView = decorView,
                contentView = contentView,
                oldFrame = oldFrame,
                originInDecor = originInDecor,
                originInContent = originInContent,
                revealMode = revealMode,
            )
        }
    }
}

/**
 * Captures the composed window through SurfaceFlinger so hardware-backed album art can be copied.
 * Drawing DecorView into a software canvas throws as soon as a hardware bitmap is visible.
 */
private suspend fun captureWindowFrame(view: View): Bitmap? {
    val activity = view.context.findActivity() ?: return null
    val root = activity.window.decorView.rootView
    if (root.width <= 0 || root.height <= 0) return null
    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    return suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            activity.window,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    if (continuation.isActive) {
                        continuation.resume(bitmap)
                    } else if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } else {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    if (continuation.isActive) continuation.resume(null)
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
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
