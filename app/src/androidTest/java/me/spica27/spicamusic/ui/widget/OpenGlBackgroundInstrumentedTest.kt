package me.spica27.spicamusic.ui.widget

import android.opengl.GLES20
import android.os.SystemClock
import android.util.Size
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class OpenGlBackgroundInstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun hazeCapturesUpdatingGlFramesInThePageAndDialog() {
        val renderer = SplitColorRenderer()
        val sampleBounds = AtomicReference<Rect?>(null)
        val showDialog = mutableStateOf(false)
        lateinit var owner: TestLifecycleOwner
        activityRule.scenario.onActivity { activity ->
            owner = TestLifecycleOwner()
            activity.setContent {
                val hazeState = rememberHazeState()
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        OpenGlBackground(renderer, Modifier.fillMaxSize().hazeSource(hazeState))
                        if (showDialog.value) {
                            Dialog(
                                onDismissRequest = {},
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    BlurSample(hazeState, sampleBounds)
                                }
                            }
                        } else {
                            BlurSample(hazeState, sampleBounds)
                        }
                    }
                }
            }
        }

        awaitBlurredBoundary(sampleBounds, green = false)
        // These updates are outside Compose state; only TextureView's frame notification changes.
        renderer.green = true
        awaitBlurredBoundary(sampleBounds, green = true)

        activityRule.scenario.onActivity {
            sampleBounds.set(null)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            showDialog.value = true
        }
        awaitBlurredBoundary(sampleBounds, green = true)
        renderer.green = false
        awaitBlurredBoundary(sampleBounds, green = false)
    }

    @Test
    fun pauseResizeReattachAndReleaseKeepTheRendererUsable() {
        val renderer = SplitColorRenderer()
        val initialThreadCount = glThreadCount()
        lateinit var owner: TestLifecycleOwner
        lateinit var view: OpenGlTextureView
        lateinit var parent: FrameLayout
        activityRule.scenario.onActivity { activity ->
            owner = TestLifecycleOwner()
            view = OpenGlTextureView(activity, renderer) {}
            view.updateLifecycle(owner.lifecycle, enabled = true)
            parent = FrameLayout(activity)
            parent.addView(view, FrameLayout.LayoutParams(200, 200))
            activity.setContentView(parent)
        }
        await { renderer.frames.get() >= 3 }

        val beforeDialog = renderer.frames.get()
        activityRule.scenario.onActivity { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        await { renderer.frames.get() >= beforeDialog + 3 }

        activityRule.scenario.onActivity { view.updateLifecycle(owner.lifecycle, enabled = false) }
        assertRenderingStops(renderer)
        activityRule.scenario.onActivity {
            val bitmap = checkNotNull(view.bitmap)
            assertTrue(android.graphics.Color.red(bitmap.getPixel(25, 25)) > 200)
            bitmap.recycle()
        }
        val beforeResume = renderer.frames.get()
        activityRule.scenario.onActivity { view.updateLifecycle(owner.lifecycle, enabled = true) }
        await { renderer.frames.get() >= beforeResume + 3 }
        assertEquals(1, renderer.contexts.get())

        activityRule.scenario.onActivity { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        assertRenderingStops(renderer)
        val beforeStart = renderer.frames.get()
        activityRule.scenario.onActivity { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        await { renderer.frames.get() >= beforeStart + 3 }

        activityRule.scenario.onActivity { view.layoutParams = FrameLayout.LayoutParams(240, 160) }
        await { renderer.size.get() == Size(240, 160) }
        activityRule.scenario.onActivity { parent.removeView(view) }
        assertRenderingStops(renderer)
        val beforeReattach = renderer.frames.get()
        activityRule.scenario.onActivity { parent.addView(view, FrameLayout.LayoutParams(180, 180)) }
        await { renderer.contexts.get() == 2 && renderer.frames.get() >= beforeReattach + 3 }

        activityRule.scenario.onActivity {
            // Exercise onRelease before detachment; the opposite order is covered by reattachment.
            view.release()
            parent.removeView(view)
        }
        assertRenderingStops(renderer)
        await { glThreadCount() <= initialThreadCount }
    }

    private fun awaitBlurredBoundary(
        bounds: AtomicReference<Rect?>,
        green: Boolean,
    ) {
        await {
            val center = bounds.get()?.center ?: return@await false
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return@await false
            try {
                val pixel = screenshot.getPixel(center.x.toInt(), center.y.toInt())
                val red = android.graphics.Color.red(pixel)
                val blue = android.graphics.Color.blue(pixel)
                val greenChannel = android.graphics.Color.green(pixel)
                // Unblurred GL has a hard red/blue (or green/blue) edge. Mixed channels prove that
                // Haze captured and blurred it, rather than just showing a transparent overlay.
                blue > 40 && if (green) greenChannel > 40 && red < 30 else red > 40 && greenChannel < 30
            } finally {
                screenshot.recycle()
            }
        }
    }

    private fun assertRenderingStops(renderer: SplitColorRenderer) {
        SystemClock.sleep(200) // Allow an already submitted GPU frame to finish.
        val frames = renderer.frames.get()
        SystemClock.sleep(150)
        assertEquals(frames, renderer.frames.get())
    }

    private fun await(predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (!predicate()) {
            assertTrue("Timed out waiting for GL rendering or Haze capture", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(50)
        }
    }

    private fun glThreadCount(): Int = Thread.getAllStackTraces().keys.count { it.isAlive && it.name == "Background-GL" }
}

@Composable
private fun BlurSample(
    hazeState: HazeState,
    bounds: AtomicReference<Rect?>,
) {
    val view = LocalView.current
    val style = remember { HazeBlurStyle { blurRadius(20.dp) } }
    Box(
        Modifier
            .size(160.dp)
            .onGloballyPositioned { coordinates ->
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                bounds.set(coordinates.boundsInRoot().translate(Offset(location[0].toFloat(), location[1].toFloat())))
            }.hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = style,
                performanceMode = HazePerformanceMode.Quality,
            ),
    )
}

private class TestLifecycleOwner : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
}

private class SplitColorRenderer : OpenGlBackgroundRenderer {
    val frames = AtomicInteger()
    val contexts = AtomicInteger()
    val size = AtomicReference(Size(1, 1))

    @Volatile var green = false

    override fun onSurfaceCreated() {
        contexts.incrementAndGet()
    }

    override fun onSurfaceChanged(
        width: Int,
        height: Int,
    ) {
        size.set(Size(width, height))
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame() {
        val currentSize = size.get()
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glClearColor(0f, 0f, 1f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(0, 0, currentSize.width / 2, currentSize.height)
        GLES20.glClearColor(if (green) 0f else 1f, if (green) 1f else 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        frames.incrementAndGet()
    }
}
