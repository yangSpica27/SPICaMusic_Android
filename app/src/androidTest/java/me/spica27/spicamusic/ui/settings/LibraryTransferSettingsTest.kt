package me.spica27.spicamusic.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.transfer.domain.ImportPreview
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryTransferSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun settingsOffersBothCompletePackageActions() {
        var exported = false
        var imported = false
        compose.setContent {
            MaterialTheme {
                LibraryTransferContent(TransferState.Idle, { exported = true }, { imported = true }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.library_transfer_export)).assertHasClickAction().performClick()
        compose.onNodeWithText(context.getString(R.string.library_transfer_import)).assertHasClickAction().performClick()
        compose.runOnIdle {
            assertTrue(exported)
            assertTrue(imported)
        }
    }

    @Test fun importPreviewKeepsExistingLyricsByDefaultAndAllowsExplicitReplacement() {
        var replace: Boolean? = null
        compose.setContent {
            MaterialTheme {
                LibraryTransferContent(
                    TransferState.Preview(ImportPreview(2, 1, 1, 1, 1, 1, 1024)),
                    {},
                    {},
                    {},
                    {},
                    {},
                    { replace = it },
                )
            }
        }
        compose.onAllNodes(isToggleable())[0].assertIsOff()
        compose.onNodeWithText(context.getString(R.string.library_transfer_start_import)).performClick()
        compose.runOnIdle { assertEquals(false, replace) }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText(context.getString(R.string.library_transfer_start_import)).performClick()
        compose.runOnIdle { assertEquals(true, replace) }
    }

    @Test fun runningTransferDisablesNewTransfersAndOffersPause() {
        var paused = false
        compose.setContent {
            MaterialTheme {
                LibraryTransferContent(TransferState.Running("测试传输", 1, 2), {}, {}, { paused = true }, {}, {}, {})
            }
        }
        compose.onNodeWithText(context.getString(R.string.library_transfer_export)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.library_transfer_import)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.library_transfer_pause)).performClick()
        compose.runOnIdle { assertTrue(paused) }
    }
}
