package me.spica27.spicamusic.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "me.spica27.spicamusic"

@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Baseline: cold startup with no compiled code — use this as the comparison denominator. */
    @Test
    fun startupNoCompilation() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
    ) {
        pressHome()
        startActivityAndWait()
    }

    /** Target: startup with the Baseline Profile applied.
     *  Fails loudly if assets/dexopt/baseline.prof is absent from the release APK. */
    @Test
    fun startupBaselineProfile() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
    ) {
        pressHome()
        startActivityAndWait()
    }

    /** Frame timing while scrolling the song list — the hottest Compose path in the app. */
    @Test
    fun scrollSongList() = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.res(PACKAGE, "song_list")), 5_000)
        val songList = device.findObject(By.res(PACKAGE, "song_list"))
            ?: device.findObject(By.scrollable(true))
        songList?.let {
            it.setGestureMargin(device.displayWidth / 5)
            it.fling(Direction.DOWN)
            it.fling(Direction.DOWN)
        }
    }
}
