package me.spica27.spicamusic.ui.home.player_bar

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.spica27.spicamusic.ui.home.HomePage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** 验证玻璃指示器变化及绘制边界。 */
@RunWith(AndroidJUnit4::class)
class HomeNavigationRenderingInstrumentedTest {
    @get:Rule
    val activity = ActivityScenarioRule(ComponentActivity::class.java)

    private val bounds = AtomicReference<Rect>()
    private val density = AtomicReference(1f)

    @Test
    fun pressedAndMovingLensStaysInsideTheNavigationContainer() {
        activity.scenario.onActivity { host ->
            host.setContent {
                MaterialTheme {
                    CompositionLocalProvider {
                        var selectedPage by remember { mutableStateOf(HomePage.Music) }
                        density.set(LocalDensity.current.density)
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawRect(Brush.linearGradient(listOf(Color(0xFFFFDBC8), Color(0xFFCCE3EF))))
                            }
                            HomeNavigationBar(
                                selectedPage = selectedPage,
                                glassEnabled = true,
                                onPageSelected = { selectedPage = it },
                                modifier =
                                    Modifier.width(320.dp).onGloballyPositioned {
                                        bounds.set(it.boundsInWindow())
                                    },
                            )
                        }
                    }
                }
            }
        }
        SystemClock.sleep(1000)
        val navigation = checkNotNull(bounds.get())
        val tabWidth = navigation.width / 3f
        val x = navigation.left + 1.5f * tabWidth
        val y = navigation.center.y
        val downTime = SystemClock.uptimeMillis()
        val rest = screenshot("navigation-floating-rest.png")
        injectTouch(MotionEvent.ACTION_DOWN, x, y, downTime)
        try {
            SystemClock.sleep(700)
            val pressed = screenshot("navigation-floating-pressed.png")
            assertLensIsContained(rest, pressed, navigation, x)
            injectTouch(MotionEvent.ACTION_MOVE, x + tabWidth * 0.5f, y, downTime)
            SystemClock.sleep(400)
            val moving = screenshot("navigation-floating-moving.png")
            assertLensIsContained(rest, moving, navigation, x + tabWidth * 0.4f)
        } finally {
            injectTouch(MotionEvent.ACTION_UP, x + tabWidth * 0.5f, y, downTime)
        }
    }

    private fun injectTouch(
        action: Int,
        x: Float,
        y: Float,
        downTime: Long,
    ) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            assertTrue(InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private fun screenshot(name: String): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()).copy(Bitmap.Config.ARGB_8888, false)
        val navigation = checkNotNull(bounds.get())
        val margin = (32f * density.get()).toInt()
        val left = (navigation.left.toInt() - margin).coerceAtLeast(0)
        val top = (navigation.top.toInt() - margin).coerceAtLeast(0)
        val right = (navigation.right.toInt() + margin).coerceAtMost(bitmap.width)
        val bottom = (navigation.bottom.toInt() + margin).coerceAtMost(bitmap.height)
        val preview = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        instrumentation.targetContext.cacheDir.resolve(name).outputStream().use {
            preview.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return bitmap
    }

    private fun assertLensIsContained(
        rest: Bitmap,
        active: Bitmap,
        navigation: Rect,
        centerX: Float,
    ) {
        val margin = density.get()
        val halfWidth = 12f * margin
        val left = (centerX - halfWidth).toInt().coerceAtLeast(0)
        val right = (centerX + halfWidth).toInt().coerceAtMost(rest.width)

        fun changes(
            top: Int,
            bottom: Int,
        ): Int {
            var count = 0
            for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(rest.height)) {
                for (x in left until right) {
                    if (rest.getPixel(x, y) != active.getPixel(x, y)) count++
                }
            }
            return count
        }
        assertTrue(
            "Pressing and dragging must visibly change the glass lens",
            changes((navigation.top + 8f * margin).toInt(), (navigation.bottom - 8f * margin).toInt()) > 10,
        )
        assertTrue(
            "The lens must not paint over the mini player",
            changes((navigation.top - 6f * margin).toInt(), (navigation.top - margin).toInt()) == 0,
        )
        assertTrue(
            "The lens must not paint over the system navigation area",
            changes((navigation.bottom + margin).toInt(), (navigation.bottom + 6f * margin).toInt()) == 0,
        )
    }
}
