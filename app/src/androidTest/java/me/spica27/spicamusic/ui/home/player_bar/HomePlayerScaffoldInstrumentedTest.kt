package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomePlayerScaffoldInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()
    private var available by mutableStateOf(true)
    private lateinit var state: PlayerSheetState
    private var playPauseClicks = 0
    private var queueClicks = 0
    private var bottomPadding = 0f
    private var miniBounds = Rect.Zero
    private var navigationBounds = Rect.Zero

    @Test
    fun emptyPlaybackShowsOnlyNavigationAndReclaimsTheMiniPlayerSpace() {
        available = false
        setContent()
        compose.onNodeWithTag("mini").assertDoesNotExist()
        compose.onNodeWithTag("navigation").assertIsDisplayed()
        var emptyPadding = 0f
        compose.runOnIdle { emptyPadding = bottomPadding }
        compose.runOnIdle { available = true }
        val miniHeight =
            compose
                .onNodeWithTag("mini")
                .fetchSemanticsNode()
                .boundsInRoot.height
        compose.runOnIdle { assertEquals(miniHeight, bottomPadding - emptyPadding, 1f) }
    }

    @Test
    fun scrollingToTheLastRowLeavesItAboveThePersistentControls() {
        setContent()
        compose.onNodeWithTag("contentList").performScrollToIndex(79)
        compose.onNodeWithText("Row 79").assertIsDisplayed()
        val lastRow = compose.onNodeWithText("Row 79").fetchSemanticsNode().boundsInRoot
        val mini = compose.onNodeWithTag("mini").fetchSemanticsNode().boundsInRoot
        assertTrue(lastRow.bottom <= mini.top + 1f)
        compose.onNodeWithTag("navigation").assertIsDisplayed()
    }

    @Test
    fun transportButtonsDoNotAlsoExpandThePlayer() {
        setContent()
        compose.onNodeWithContentDescription(label(R.string.pause)).performClick()
        compose.runOnIdle {
            assertEquals(1, playPauseClicks)
            assertFalse(state.isVisible)
        }
        compose.onNodeWithContentDescription(label(R.string.queue)).performClick()
        compose.onNodeWithTag("fullPlayer").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, queueClicks) }
    }

    @Test
    fun tapAndSystemBackReturnToTheFixedBottomBar() {
        setContent()
        compose.onNodeWithTag("mini").performClick()
        compose.onNodeWithTag("fullPlayer").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("fullPlayer").assertDoesNotExist()
        compose.onNodeWithTag("navigation").assertIsDisplayed()
        compose.onNodeWithTag("mini").assertIsDisplayed()
    }

    @Test
    fun swipeUpOpensAndDraggingTheHandleDownCloses() {
        setContent()
        compose.onNodeWithTag("mini").performTouchInput {
            swipe(center, center - Offset(0f, miniBounds.top * 0.4f), 300)
        }
        compose.onNodeWithTag("fullPlayer").assertIsDisplayed()
        compose.onNodeWithTag("handle").performTouchInput {
            swipe(center, center + Offset(0f, miniBounds.top * 0.4f + 600f), 300)
        }
        compose.onNodeWithTag("fullPlayer").assertDoesNotExist()
        compose.onNodeWithTag("mini").assertIsDisplayed()
    }

    @Test
    fun removingTheSongDismissesThePlayerAndRemovesItsControls() {
        setContent()
        compose.onNodeWithTag("mini").performClick()
        compose.runOnIdle { available = false }
        compose.onNodeWithTag("fullPlayer").assertDoesNotExist()
        compose.onNodeWithTag("mini").assertDoesNotExist()
        compose.onNodeWithTag("navigation").assertIsDisplayed()
        compose.runOnIdle { assertFalse(state.isVisible) }
    }

    @Test
    fun expandedPlayerSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        setContent(restoration)
        compose.onNodeWithTag("mini").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("fullPlayer").assertIsDisplayed()
    }

    @Test
    fun playbackVisibilityAnimatesTheOccupiedSpaceInBothDirections() {
        setContent(reducedMotion = false)
        val fullPadding = bottomPadding
        val miniHeight = miniBounds.height
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { available = false }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(80)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(bottomPadding < fullPadding)
            assertTrue(bottomPadding > fullPadding - miniHeight)
        }
        compose.onNodeWithText("Unsayable").assertExists()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithTag("mini").assertDoesNotExist()
        val emptyPadding = bottomPadding
        compose.runOnIdle { available = true }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(80)
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(bottomPadding > emptyPadding)
            assertTrue(bottomPadding < fullPadding)
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithTag("mini").assertIsDisplayed()
        compose.runOnIdle { assertEquals(fullPadding, bottomPadding, 1f) }
    }

    @Test
    fun draggingMovesTheMiniPlayerAndNavigationContinuouslyWithoutChangingReservedSpace() {
        setContent(reducedMotion = false)
        val restMini = miniBounds
        val restNavigation = navigationBounds
        val padding = bottomPadding
        compose.runOnIdle {
            state.beginDrag()
            state.dragBy(-restMini.top * 0.1f, restMini.top)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(restMini.top * 0.9f, miniBounds.top, 2f)
            assertTrue(navigationBounds.top > restNavigation.top)
            assertEquals(padding, bottomPadding, 1f)
        }
        compose.runOnIdle {
            state.dragBy(restMini.top * 0.1f, restMini.top)
            state.settle(0f, restMini.top, 56f, 700f, true)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("mini").assertIsDisplayed()
        compose.onNodeWithTag("navigation").assertIsDisplayed()
        compose.runOnIdle { assertEquals(restMini.top, miniBounds.top, 1f) }
    }

    private fun setContent(
        restoration: StateRestorationTester? = null,
        reducedMotion: Boolean = true,
    ) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            MaterialTheme {
                CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
                    state = rememberPlayerSheetState()
                    HomePlayerScaffold(
                        playerAvailable = available,
                        sheetState = state,
                        navigationBar = {
                            HomeNavigationBar(
                                HomePage.Finder,
                                {},
                                Modifier.testTag("navigation").onGloballyPositioned {
                                    navigationBounds =
                                        it.boundsInRoot()
                                },
                            )
                        },
                        miniPlayer = { drag ->
                            MiniPlayerBar(
                                title = "Unsayable",
                                artist = "Brambles",
                                artworkUri = null,
                                isPlaying = true,
                                onExpand = { state.animateTo(true, reducedMotion) },
                                onPlayPause = { playPauseClicks++ },
                                onOpenQueue = {
                                    queueClicks++
                                    state.animateTo(true, reducedMotion)
                                },
                                modifier = Modifier.testTag("mini").onGloballyPositioned { miniBounds = it.boundsInRoot() }.then(drag),
                            )
                        },
                        fullScreenPlayer = { progress, drag ->
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = progress() }
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                    ).testTag("fullPlayer"),
                            ) {
                                PlayerSheetHandle({ state.animateTo(false, reducedMotion) }, Modifier.testTag("handle").then(drag))
                                Text("Player")
                            }
                        },
                    ) { padding ->
                        bottomPadding = with(androidx.compose.ui.platform.LocalDensity.current) { padding.calculateBottomPadding().toPx() }
                        LazyColumn(Modifier.fillMaxSize().testTag("contentList"), contentPadding = padding) {
                            items(80) { Text("Row $it") }
                        }
                    }
                }
            }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
    }

    private fun label(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
