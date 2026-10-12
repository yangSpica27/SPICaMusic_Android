package me.spica27.spicamusic.ui.home.player_bar

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.common.entity.BottomBarLayout
import me.spica27.spicamusic.ui.glass.LiquidGlassConfig
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.home.HomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomBarAppearanceInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()
    private var glassEnabled by mutableStateOf(false)
    private var layout by mutableStateOf(BottomBarLayout.Traditional)
    private var selectedPage by mutableStateOf(HomePage.Finder)
    private val container = Color(0xFFE4CFFF)

    @Test
    fun solidMiniPlayerUsesTheThemeContainerColor() {
        setContent()
        val pixels = compose.onNodeWithTag("mini").captureToImage().toPixelMap()
        val actual = pixels[2, pixels.height / 2]
        assertEquals(container.red, actual.red, 0.01f)
        assertEquals(container.green, actual.green, 0.01f)
        assertEquals(container.blue, actual.blue, 0.01f)
        savePreview("bottom-bar-solid.png")
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun changingTheSharedGlassPreferenceUpdatesBothSurfacesInPlace() {
        setContent(BottomBarLayout.Floating)
        val solidMini = compose.onNodeWithTag("mini").captureToImage().toPixelMap()
        val solidNavigation = compose.onNodeWithTag("navigation").captureToImage().toPixelMap()
        compose.runOnIdle { glassEnabled = true }
        compose.mainClock.advanceTimeBy(300)
        val glassMini = compose.onNodeWithTag("mini").captureToImage().toPixelMap()
        val glassNavigation = compose.onNodeWithTag("navigation").captureToImage().toPixelMap()

        fun changes(
            first: androidx.compose.ui.graphics.PixelMap,
            second: androidx.compose.ui.graphics.PixelMap,
        ): Int {
            var count = 0
            for (y in 0 until first.height step 3) {
                for (x in 0 until first.width step 3) {
                    val a = first[x, y]
                    val b = second[x, y]
                    if (kotlin.math.abs(a.red - b.red) + kotlin.math.abs(a.green - b.green) + kotlin.math.abs(a.blue - b.blue) >
                        0.03f
                    ) {
                        count++
                    }
                }
            }
            return count
        }
        assertTrue("The preference must enable the mini player's glass surface", changes(solidMini, glassMini) > 30)
        assertTrue("The preference must enable the whole navigation glass surface", changes(solidNavigation, glassNavigation) > 30)
        compose.onAllNodes(isSelectable()).assertCountEquals(3)
        compose.onNodeWithText("Unsayable").assertIsDisplayed()
        savePreview("bottom-bar-glass.png")
        compose.runOnIdle { glassEnabled = false }
        val restored = compose.onNodeWithTag("mini").captureToImage().toPixelMap()
        assertEquals(container.red, restored[2, restored.height / 2].red, 0.01f)
    }

    @Test
    fun changingLayoutPreservesNavigationSelectionAndKeepsGlassIndependent() {
        glassEnabled = true
        setContent()
        val libraryTitle = InstrumentationRegistry.getInstrumentation().targetContext.getString(HomePage.Library.titleRes)
        compose.onNodeWithText(libraryTitle).performClick()
        val traditionalMini = compose.onNodeWithTag("mini").fetchSemanticsNode().boundsInRoot
        val traditionalNavigation = compose.onNodeWithTag("navigation").fetchSemanticsNode().boundsInRoot
        assertEquals(traditionalMini.bottom, traditionalNavigation.top, 1f)

        compose.runOnIdle { layout = BottomBarLayout.Floating }
        val floatingMini = compose.onNodeWithTag("mini").fetchSemanticsNode().boundsInRoot
        val floatingNavigation = compose.onNodeWithTag("navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(floatingMini.left > traditionalMini.left)
        assertTrue(floatingNavigation.top > floatingMini.bottom)
        compose.onNodeWithText(libraryTitle).assertIsSelected()
        compose.runOnIdle {
            assertTrue(glassEnabled)
            glassEnabled = false
        }
        assertEquals(floatingNavigation, compose.onNodeWithTag("navigation").fetchSemanticsNode().boundsInRoot)

        compose.runOnIdle { layout = BottomBarLayout.Traditional }
        assertEquals(traditionalMini, compose.onNodeWithTag("mini").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText(libraryTitle).assertIsSelected()
        compose.runOnIdle { assertTrue(!glassEnabled) }
    }

    private fun setContent(initialLayout: BottomBarLayout = BottomBarLayout.Traditional) {
        layout = initialLayout
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primaryContainer = container, surfaceContainer = container)) {
                CompositionLocalProvider(LocalLiquidGlassConfig provides LiquidGlassConfig(glassEnabled)) {
                    val source = rememberHazeState()
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        HomePlayerScaffold(
                            playerAvailable = true,
                            sheetState = rememberPlayerSheetState(),
                            navigationBar = {
                                HomeNavigationBar(
                                    selectedPage,
                                    { selectedPage = it },
                                    Modifier.testTag("navigation"),
                                    hazeState = source,
                                    layout = layout,
                                )
                            },
                            miniPlayer = { dragModifier ->
                                MiniPlayerBar(
                                    "Unsayable",
                                    "Brambles",
                                    null,
                                    true,
                                    {},
                                    {},
                                    {},
                                    dragModifier.testTag("mini"),
                                    hazeState = source,
                                    layout = layout,
                                )
                            },
                            fullScreenPlayer = { _, _ -> },
                            modifier = Modifier.width(360.dp).testTag("bottomBar"),
                            layout = layout,
                        ) {
                            Canvas(Modifier.fillMaxSize().hazeSource(source)) {
                                drawRect(Brush.linearGradient(listOf(Color(0xFFFFAFA0), Color(0xFF65C8BC))))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun savePreview(name: String) {
        val bitmap = compose.onNodeWithTag("bottomBar").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = context.getExternalFilesDir(null) ?: context.cacheDir
        directory.resolve(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
