package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSheetStateTest {
    @Test
    fun shortSlowDragReturnsToTheMiniPlayer() =
        runTest {
            val state = PlayerSheetState(this)
            state.beginDrag()
            state.dragBy(-25f, 1000f)
            state.settle(0f, 1000f, 56f, 700f, true)
            assertFalse(state.isVisible)
        }

    @Test
    fun deliberateSwipeOpensWithoutRequiringHalfTheScreen() =
        runTest {
            val state = PlayerSheetState(this)
            state.beginDrag()
            state.dragBy(-80f, 1000f)
            state.settle(0f, 1000f, 56f, 700f, true)
            assertEquals(1f, state.progress, 0f)
            assertTrue(state.targetExpanded)
        }

    @Test
    fun reversingTheFlingKeepsThePlayerOpen() =
        runTest {
            val state = PlayerSheetState(this, initiallyExpanded = true)
            state.beginDrag()
            state.dragBy(200f, 1000f)
            state.settle(-1200f, 1000f, 56f, 700f, true)
            assertEquals(1f, state.progress, 0f)
        }

    @Test
    fun rapidOpenThenCloseCannotBeOverwrittenByTheOldAnimation() =
        runTest {
            val state = PlayerSheetState(CoroutineScope(coroutineContext + TestFrameClock()))
            state.animateTo(true)
            advanceTimeBy(80)
            assertTrue(state.progress > 0f)
            state.animateTo(false)
            advanceUntilIdle()
            assertEquals(0f, state.progress, 0f)
            assertFalse(state.isVisible)
        }

    @Test
    fun draggingTakesOverAnInFlightAnimation() =
        runTest {
            val state = PlayerSheetState(CoroutineScope(coroutineContext + TestFrameClock()))
            state.animateTo(true)
            advanceTimeBy(80)
            state.beginDrag()
            state.dragBy(50f, 1000f)
            val draggedProgress = state.progress
            advanceUntilIdle()
            assertEquals(draggedProgress, state.progress, 0f)
            assertTrue(state.isDragging)
        }

    @Test
    fun removingTheSongCancelsAnOpeningPlayer() =
        runTest {
            val state = PlayerSheetState(CoroutineScope(coroutineContext + TestFrameClock()))
            state.animateTo(true)
            advanceTimeBy(80)
            state.reset()
            advanceUntilIdle()
            assertFalse(state.isVisible)
            assertFalse(state.targetExpanded)
            assertEquals(0f, state.progress, 0f)
        }

    private class TestFrameClock : MonotonicFrameClock {
        private var time = 0L

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16)
            time += 16_000_000L
            return onFrame(time)
        }
    }
}
