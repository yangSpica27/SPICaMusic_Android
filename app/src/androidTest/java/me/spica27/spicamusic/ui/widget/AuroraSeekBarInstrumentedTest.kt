package me.spica27.spicamusic.ui.widget

import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import me.spica27.spicamusic.ui.widget.audio_seekbar.AURORA_SHADER_SOURCE
import me.spica27.spicamusic.ui.widget.audio_seekbar.AuroraSeekBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuroraSeekBarInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun shaderCompilesAndRendersPlayedRibbonAboveTheRail() {
        // A Kotlin build does not compile AGSL. Do not let the production fallback hide an error.
        RuntimeShader(AURORA_SHADER_SOURCE)
        setSlider(progress = { 0.65f }, onChange = {})
        val pixels = compose.onNodeWithTag("aurora").captureToImage().toPixelMap()
        val background = pixels[pixels.width / 2, 0]
        var changedPixels = 0
        // Sample away from the centre line and head, where only the AGSL ribbons/glow exist.
        for (x in pixels.width / 5 until pixels.width / 2) {
            for (y in pixels.height * 3 / 8 until pixels.height * 7 / 16) {
                val pixel = pixels[x, y]
                if (kotlin.math.abs(pixel.red - background.red) +
                    kotlin.math.abs(pixel.green - background.green) +
                    kotlin.math.abs(pixel.blue - background.blue) > 0.04f
                ) {
                    changedPixels++
                }
            }
        }
        assertTrue("The shader should draw a visible ribbon above the rail", changedPixels > 10)
    }

    @Test
    fun tappingAndDraggingMatchTheVisibleTrackInLtr() = verifySeekDirection(LayoutDirection.Ltr)

    @Test
    fun tappingAndDraggingMatchTheVisibleTrackInRtl() = verifySeekDirection(LayoutDirection.Rtl)

    @Test
    fun accessibilitySeekFinishesAndUpdatesProgress() {
        var value by mutableFloatStateOf(0.25f)
        var finished = 0
        setSlider({ value }, { value = it }, { finished++ })
        compose
            .onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
            ).performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(0.8f)
            }
        compose.runOnIdle {
            assertEquals(0.8f, value, 0.001f)
            assertEquals(1, finished)
        }
    }

    @Test
    fun disabledSeekBarIgnoresTouch() {
        var changes = 0
        setSlider({ 0f }, { changes++ }, enabled = false)
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertIsNotEnabled()
        compose.onNodeWithTag("aurora").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(0, changes) }
    }

    private fun verifySeekDirection(direction: LayoutDirection) {
        var value by mutableFloatStateOf(0.2f)
        var finished = 0
        setSlider({ value }, { value = it }, { finished++ }, direction = direction)
        val node = compose.onNodeWithTag("aurora")
        node.performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(0.5f, value, 0.025f)
            assertEquals(1, finished)
        }
        node.performTouchInput { swipe(center, Offset(width.toFloat() - 1f, center.y)) }
        compose.runOnIdle { assertEquals(if (direction == LayoutDirection.Ltr) 1f else 0f, value, 0.001f) }
        node.performTouchInput { swipe(center, Offset(1f, center.y)) }
        compose.runOnIdle { assertEquals(if (direction == LayoutDirection.Ltr) 0f else 1f, value, 0.001f) }
    }

    private fun setSlider(
        progress: () -> Float,
        onChange: (Float) -> Unit,
        onFinished: () -> Unit = {},
        direction: LayoutDirection = LayoutDirection.Ltr,
        enabled: Boolean = true,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                MaterialTheme {
                    Surface {
                        AuroraSeekBar(
                            progress = progress(),
                            onProgressChange = onChange,
                            onProgressChangeFinished = onFinished,
                            enabled = enabled,
                            modifier = Modifier.width(320.dp).height(80.dp).testTag("aurora"),
                        )
                    }
                }
            }
        }
    }
}
