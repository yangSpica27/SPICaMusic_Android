package me.spica27.spicamusic.player.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPositionSmootherTest {
    @Test
    fun `stale samples do not rewind position`() {
        val smoother = PlaybackPositionSmoother()

        assertEquals(10_000L, smoother.sample("song", 10_000L))
        assertEquals(10_000L, smoother.sample("song", 9_700L))
        assertEquals(10_200L, smoother.sample("song", 10_200L))
    }

    @Test
    fun `media transition starts a new position sequence`() {
        val smoother = PlaybackPositionSmoother()

        smoother.sample("old", 120_000L)

        assertEquals(0L, smoother.sample("new", 0L))
        assertEquals(150L, smoother.sample("new", 150L))
    }

    @Test
    fun `explicit seek permits backward position`() {
        val smoother = PlaybackPositionSmoother()

        smoother.sample("song", 120_000L)
        smoother.resetTo("song", 30_000L)

        assertEquals(30_000L, smoother.sample("song", 30_000L))
        assertEquals(30_500L, smoother.sample("song", 30_500L))
    }

    @Test
    fun `missing samples keep the last position for the same media`() {
        val smoother = PlaybackPositionSmoother()

        smoother.sample("song", 42_000L)

        assertEquals(42_000L, smoother.lastPosition("song"))
        assertEquals(0L, smoother.lastPosition("other"))
    }

    @Test
    fun `first sample after reconnect is trusted even when it moves backward`() {
        val smoother = PlaybackPositionSmoother()

        smoother.sample("song", 120_000L)
        smoother.markDisconnected()

        assertEquals(35_000L, smoother.sample("song", 35_000L))
        assertEquals(35_100L, smoother.sample("song", 35_100L))
    }
}
