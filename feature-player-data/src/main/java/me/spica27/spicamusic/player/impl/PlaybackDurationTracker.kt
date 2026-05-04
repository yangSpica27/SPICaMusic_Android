package me.spica27.spicamusic.player.impl

internal class PlaybackDurationTracker {
    private var startPositionMs: Long = 0L
    private var startTimeMs: Long = 0L
    private var accumulatedPlayedDurationMs: Long = 0L

    var isTracking: Boolean = false
        private set

    fun beginSession(
        positionMs: Long,
        nowMs: Long,
    ) {
        startPositionMs = positionMs.coerceAtLeast(0L)
        startTimeMs = nowMs
        accumulatedPlayedDurationMs = 0L
        isTracking = true
    }

    fun splitOnSeek(
        oldPositionMs: Long,
        newPositionMs: Long,
        nowMs: Long,
    ) {
        if (!isTracking) return
        accumulatedPlayedDurationMs += currentSegmentDuration(oldPositionMs)
        startPositionMs = newPositionMs.coerceAtLeast(0L)
        startTimeMs = nowMs
    }

    fun playedDurationFromPosition(currentPositionMs: Long): Long {
        if (!isTracking) return 0L
        return accumulatedPlayedDurationMs + currentSegmentDuration(currentPositionMs)
    }

    fun playedDurationFromElapsed(nowMs: Long): Long {
        if (!isTracking) return 0L
        return accumulatedPlayedDurationMs + (nowMs - startTimeMs).coerceAtLeast(0L)
    }

    fun clear() {
        startPositionMs = 0L
        startTimeMs = 0L
        accumulatedPlayedDurationMs = 0L
        isTracking = false
    }

    private fun currentSegmentDuration(positionMs: Long): Long =
        (positionMs - startPositionMs).coerceAtLeast(0L)
}
