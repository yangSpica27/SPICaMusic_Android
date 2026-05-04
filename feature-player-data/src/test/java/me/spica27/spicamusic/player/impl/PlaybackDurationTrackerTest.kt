package me.spica27.spicamusic.player.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDurationTrackerTest {
    @Test
    fun `played duration uses position delta without seek`() {
        val tracker = PlaybackDurationTracker()

        tracker.beginSession(positionMs = 5_000L, nowMs = 1_000L)

        assertEquals(12_000L, tracker.playedDurationFromPosition(17_000L))
    }

    @Test
    fun `seek forward excludes skipped portion`() {
        val tracker = PlaybackDurationTracker()

        tracker.beginSession(positionMs = 0L, nowMs = 0L)
        tracker.splitOnSeek(oldPositionMs = 10_000L, newPositionMs = 240_000L, nowMs = 10_000L)

        assertEquals(20_000L, tracker.playedDurationFromPosition(250_000L))
    }

    @Test
    fun `seek backward keeps previously listened chunk and counts replayed chunk`() {
        val tracker = PlaybackDurationTracker()

        tracker.beginSession(positionMs = 0L, nowMs = 0L)
        tracker.splitOnSeek(oldPositionMs = 60_000L, newPositionMs = 30_000L, nowMs = 60_000L)

        assertEquals(80_000L, tracker.playedDurationFromPosition(50_000L))
    }

    @Test
    fun `transition duration keeps accumulated listened time after seek`() {
        val tracker = PlaybackDurationTracker()

        tracker.beginSession(positionMs = 0L, nowMs = 0L)
        tracker.splitOnSeek(oldPositionMs = 10_000L, newPositionMs = 240_000L, nowMs = 10_000L)

        assertEquals(20_000L, tracker.playedDurationFromElapsed(20_000L))
    }
}
