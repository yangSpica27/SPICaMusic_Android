package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassNavigationIndicatorStateTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun releaseEndsInteractionWhilePositionIsStillSettling() =
        runTest {
            val clock =
                object : MonotonicFrameClock {
                    private var frameTime = 0L

                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
                        delay(16)
                        frameTime += 16_000_000L
                        return onFrame(frameTime)
                    }
                }
            val state = GlassNavigationIndicatorState(0, 3, CoroutineScope(coroutineContext + clock))
            state.press()
            state.beginDrag()
            state.dragBy(0.75f, 2f)
            state.animateTo(1, reducedMotion = false)
            state.release()
            assertTrue("Position should still be settling", state.isSettling)
            assertFalse("Release must end the pressed appearance immediately", state.isInteracting)
            advanceUntilIdle()
            assertFalse(state.isSettling)
            assertEquals(1f, state.position, 0.001f)
        }

    @Test
    fun firstTabAllowsResistedOverscrollAndReturnsToItsValidSelection() =
        runTest {
            val state = GlassNavigationIndicatorState(0, 3, this)
            state.beginDrag()
            state.dragBy(-2f, -4f)
            assertTrue("The indicator should float past the first tab", state.position < 0f)
            assertTrue("Resistance should prevent a full tab of overscroll", state.position > -1f)
            assertEquals(0, state.nearestIndex())
            state.animateTo(state.nearestIndex(), reducedMotion = true)
            assertEquals(0f, state.position, 0.001f)
        }

    @Test
    fun lastTabAllowsResistedOverscrollWithoutAnInvalidPageIndex() =
        runTest {
            val state = GlassNavigationIndicatorState(2, 3, this)
            state.beginDrag()
            state.dragBy(2f, 4f)
            assertTrue("The indicator should float past the last tab", state.position > 2f)
            assertTrue("Resistance should prevent a full tab of overscroll", state.position < 3f)
            assertEquals(2, state.nearestIndex())
            state.animateTo(state.nearestIndex(), reducedMotion = true)
            assertEquals(2f, state.position, 0.001f)
        }

    @Test
    fun reversingAnEdgeDragStillReachesTheOtherTabs() =
        runTest {
            val state = GlassNavigationIndicatorState(0, 3, this)
            state.beginDrag()
            state.dragBy(-0.5f, -2f)
            state.dragBy(2f, 3f)
            assertEquals(1.5f, state.position, 0.001f)
            assertEquals(2, state.nearestIndex())
        }
}
