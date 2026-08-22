package me.spica27.spicamusic.player.api

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerStateTest {
    @Test
    fun updatedAtReportsElapsedTimeAndClampsAtZero() {
        val state =
            SleepTimerState(
                durationMs = 60_000L,
                remainingMs = 60_000L,
                deadlineElapsedRealtimeMs = 160_000L,
            )

        assertEquals(45_000L, state.updatedAt(115_000L).remainingMs)
        assertEquals(0L, state.updatedAt(160_001L).remainingMs)
    }

    @Test
    fun updatedAtDoesNotExceedInitialDurationWhenClockMovesBack() {
        val state =
            SleepTimerState(
                durationMs = 30_000L,
                remainingMs = 10_000L,
                deadlineElapsedRealtimeMs = 130_000L,
            )

        assertEquals(30_000L, state.updatedAt(90_000L).remainingMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationIsRejected() {
        SleepTimerState(
            durationMs = 0L,
            remainingMs = 0L,
            deadlineElapsedRealtimeMs = 100L,
        )
    }
}
