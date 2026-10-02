package me.spica27.spicamusic.ui.home.player_bar

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationBarInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private var selectedPage by mutableStateOf(HomePage.Finder)
    private val selections = mutableListOf<HomePage>()
    private var parentDragDistance = 0f

    @Test
    fun tappingSelectsOnceWithoutDuplicateAccessibleTabs() {
        setNavigation(glassEnabled = true)
        compose.onAllNodes(isSelectable()).assertCountEquals(3)
        tab(HomePage.Library).performClick()
        tab(HomePage.Library).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(HomePage.Library), selections) }
    }

    @Test
    fun horizontalDragCommitsOnlyWhenReleased() {
        setNavigation(glassEnabled = true)
        val start = tabCenter(HomePage.Finder)
        val end = tabCenter(HomePage.Library)
        compose.onNodeWithTag("navigation").performTouchInput {
            down(start)
            moveTo(end, delayMillis = 250)
        }
        compose.runOnIdle {
            assertTrue("Dragging must not rebuild intermediate pages", selections.isEmpty())
        }
        tab(HomePage.Finder).assertIsSelected()
        compose.onNodeWithTag("navigation").performTouchInput { up() }
        tab(HomePage.Library).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(HomePage.Library), selections) }
    }

    @Test
    fun cancellingDragRestoresSelectionAndAllowsAnotherTap() {
        setNavigation(glassEnabled = true)
        val start = tabCenter(HomePage.Finder)
        val end = tabCenter(HomePage.Library)
        compose.onNodeWithTag("navigation").performTouchInput {
            down(start)
            moveTo(end, delayMillis = 250)
            cancel()
        }
        tab(HomePage.Finder).assertIsSelected()
        compose.runOnIdle { assertTrue(selections.isEmpty()) }
        tab(HomePage.Music).performClick()
        tab(HomePage.Music).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(HomePage.Music), selections) }
    }

    @Test
    fun verticalDragIsLeftToTheParent() {
        setNavigation(glassEnabled = true)
        val start = tabCenter(HomePage.Finder)
        compose.onNodeWithTag("navigation").performTouchInput {
            down(start)
            moveBy(Offset(0f, -100f), delayMillis = 100)
            moveBy(Offset(0f, -100f), delayMillis = 100)
            up()
        }
        tab(HomePage.Finder).assertIsSelected()
        compose.runOnIdle {
            assertTrue("Navigation must not consume vertical dragging", parentDragDistance < -50f)
            assertTrue(selections.isEmpty())
        }
    }

    @Test
    fun rtlDraggingWorksWithGlassDisabledAndReducedMotion() {
        setNavigation(glassEnabled = false, reducedMotion = true, layoutDirection = LayoutDirection.Rtl)
        val start = tabCenter(HomePage.Finder)
        val end = tabCenter(HomePage.Library)
        assertTrue("The first RTL tab should be on the right", start.x > end.x)
        compose.onNodeWithTag("navigation").performTouchInput {
            down(start)
            moveTo(end, delayMillis = 250)
            up()
        }
        tab(HomePage.Library).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(HomePage.Library), selections) }
    }

    @Test
    fun pressingRendersTheLensAndReleasingKeepsTheSelectedTab() {
        setNavigation(glassEnabled = true)
        savePreview("navigation-glass-rest.png")
        val start = tabCenter(HomePage.Finder)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("navigation").performTouchInput { down(start) }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onAllNodes(isSelectable()).assertCountEquals(3)
        savePreview("navigation-glass-pressed.png")
        compose.onNodeWithTag("navigation").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
        tab(HomePage.Finder).assertIsSelected()
        compose.runOnIdle { assertTrue(selections.isEmpty()) }
    }

    @Test
    fun darkNavigationKeepsAllLabelsVisibleAtLargeFontScale() {
        setNavigation(glassEnabled = true, fontScale = 2f, darkTheme = true)
        HomePage.entries.forEach { tab(it).assertIsDisplayed() }
        tab(HomePage.Library).performClick()
        tab(HomePage.Library).assertIsSelected()
        savePreview("navigation-dark-large-font.png")
    }

    private fun setNavigation(
        glassEnabled: Boolean,
        reducedMotion: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
    ) {
        compose.setContent {
            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(primary = Color(0xFFAC365C))) {
                CompositionLocalProvider(
                    LocalReducedMotion provides reducedMotion,
                    LocalLayoutDirection provides layoutDirection,
                    LocalDensity provides Density(LocalDensity.current.density, fontScale),
                ) {
                    Box(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectVerticalDragGestures { change, distance ->
                                parentDragDistance += distance
                                change.consume()
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(
                                Brush.linearGradient(listOf(Color(0xFFFFDBC8), Color(0xFFEADCF6), Color(0xFFCCE3EF))),
                            )
                        }
                        HomeNavigationBar(
                            selectedPage = selectedPage,
                            onPageSelected = {
                                selections += it
                                selectedPage = it
                            },
                            modifier = Modifier.width(320.dp).testTag("navigation"),
                            glassEnabled = glassEnabled,
                        )
                    }
                }
            }
        }
    }

    private fun tab(page: HomePage) =
        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext.getString(page.titleRes))

    private fun tabCenter(page: HomePage): Offset {
        val navigation = compose.onNodeWithTag("navigation").fetchSemanticsNode().boundsInRoot
        return tab(page).fetchSemanticsNode().boundsInRoot.center - navigation.topLeft
    }

    private fun savePreview(name: String) {
        val bitmap = compose.onNodeWithTag("navigation").captureToImage().asAndroidBitmap()
        val destination =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.cacheDir
                .resolve(name)
        destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
