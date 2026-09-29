package me.spica27.spicamusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.widget.elasticDrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElasticDialogInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun dragStretchesInItsDirectionAndReleaseReturnsToRest() {
        setElasticSurface()
        val rest = bounds()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("root").performTouchInput { down(rest.center) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("root").performTouchInput { moveBy(Offset(100f, 0f)) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()

        val dragged = bounds()
        assertTrue("The surface should follow the drag", dragged.center.x > rest.center.x)
        assertTrue("Press should enlarge the surface", dragged.height > rest.height)
        assertTrue("Horizontal drag should stretch width more", dragged.width / rest.width > dragged.height / rest.height)

        compose.onNodeWithTag("root").performTouchInput { up() }
        settle()
        assertRestored(rest)
    }

    @Test
    fun cancelAndMultiplePointersRestoreTheSurfaceAndAllowAnotherGesture() {
        setElasticSurface()
        val rest = bounds()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("root").performTouchInput {
            down(rest.center)
            moveBy(Offset(60f, 40f))
        }
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithTag("root").performTouchInput { cancel() }
        settle()
        assertRestored(rest)

        compose.onNodeWithTag("root").performTouchInput { down(0, rest.center) }
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithTag("root").performTouchInput {
            down(1, rest.center + Offset(40f, 0f))
            moveBy(0, Offset(80f, 0f))
        }
        settle()
        assertRestored(rest)
        compose.onNodeWithTag("root").performTouchInput {
            up(1)
            up(0)
            down(rest.center)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue(bounds().width > rest.width)
        compose.onNodeWithTag("root").performTouchInput { up() }
        settle()
    }

    @Test
    fun disablingDuringDragClearsTheTransform() {
        var enabled by mutableStateOf(true)
        setElasticSurface(enabled = { enabled })
        val rest = bounds()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("root").performTouchInput {
            down(rest.center)
            moveBy(Offset(100f, 0f))
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue(bounds().width > rest.width)
        compose.runOnIdle { enabled = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertRestored(rest)
        compose.onNodeWithTag("root").performTouchInput { up() }
    }

    @Test
    fun reducedMotionKeepsTheSurfaceStill() {
        setElasticSurface(reducedMotion = true)
        val rest = bounds()
        compose.onNodeWithTag("root").performTouchInput {
            down(rest.center)
            moveBy(Offset(100f, 30f))
        }
        compose.waitForIdle()
        assertRestored(rest)
        compose.onNodeWithTag("root").performTouchInput { up() }
    }

    @Test
    fun dialogKeepsButtonsSlidersTextInputAndListScrollingInteractive() {
        var clicks = 0
        var sliderValue by mutableFloatStateOf(0f)
        lateinit var listState: LazyListState
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    DialogContainer(modifier = Modifier.width(300.dp), enableGlass = false) {
                        Column {
                            Button(onClick = { clicks++ }, modifier = Modifier.testTag("button")) { Text("Click") }
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                modifier = Modifier.testTag("slider"),
                            )
                            var text by remember { mutableStateOf("") }
                            OutlinedTextField(text, { text = it }, modifier = Modifier.testTag("input"))
                            listState = rememberLazyListState()
                            LazyColumn(Modifier.height(160.dp).testTag("list"), state = listState) {
                                items(30) { Text("Row $it", Modifier.height(48.dp)) }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.onNodeWithTag("slider").performTouchInput {
            swipe(Offset(width * 0.2f, center.y), Offset(width * 0.8f, center.y))
        }
        compose.runOnIdle { assertTrue("Slider should receive the drag", sliderValue > 0.6f) }
        compose.onNodeWithTag("list").performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertTrue("List should receive the scroll", listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        }
        compose.onNodeWithTag("input").performTouchInput { click() }
        compose.onNodeWithTag("input").assertIsFocused().performTextInput("Hello")
        compose.onNodeWithTag("input").assertTextContains("Hello")
    }

    private fun setElasticSurface(
        enabled: () -> Boolean = { true },
        reducedMotion: Boolean = false,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
                Box(Modifier.fillMaxSize().testTag("root"), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(240.dp, 160.dp)
                            .elasticDrag(enabled = enabled())
                            .background(Color.Blue)
                            .testTag("elastic"),
                    )
                }
            }
        }
    }

    private fun bounds(): Rect = compose.onNodeWithTag("elastic").fetchSemanticsNode().boundsInRoot

    private fun settle() {
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
    }

    private fun assertRestored(rest: Rect) {
        val current = bounds()
        assertEquals(rest.left, current.left, 1f)
        assertEquals(rest.top, current.top, 1f)
        assertEquals(rest.right, current.right, 1f)
        assertEquals(rest.bottom, current.bottom, 1f)
    }
}
