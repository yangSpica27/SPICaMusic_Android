package me.spica27.spicamusic.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = "me.spica27.spicamusic") {
        // Cold startup
        startActivityAndWait()

        // Wait for the songs list to appear
        device.wait(Until.hasObject(By.res("me.spica27.spicamusic", "song_list")), 5_000)

        // Cover song-list scroll (AllSongPage LazyColumn)
        val songList = device.findObject(By.res("me.spica27.spicamusic", "song_list"))
            ?: device.findObject(By.scrollable(true))
        songList?.let {
            it.setGestureMargin(device.displayWidth / 5)
            it.fling(Direction.DOWN)
            it.fling(Direction.DOWN)
            it.fling(Direction.UP)
        }
    }
}
