package me.spica27.spicamusic.utils

import android.text.format.DateUtils

fun Long.formatDurationDs(isElapsed: Boolean = true) = dsToSecs().formatDurationSecs(isElapsed)

fun Long.formatDurationSecs(isElapsed: Boolean = true): String {
    if (!isElapsed && this == 0L) {
        return "--:--"
    }
    var durationString = DateUtils.formatElapsedTime(this)
    if (durationString[0] == '0') {
        durationString = durationString.slice(1 until durationString.length)
    }
    return durationString
}

fun Long.msToDs() = floorDiv(100)

fun Long.msToSecs() = floorDiv(1000)

fun Long.dsToMs() = times(100)

fun Long.dsToSecs() = floorDiv(10)

fun Long.secsToMs() = times(1000)
