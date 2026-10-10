package net.mm2d.codereader.ui.result

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.launchSettingsTestActivity
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class ScanResultContentTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun eachActionRunsOnceAndClosesTheDialog() {
        val result = ScanResult("selected", "Text", "QR code", false)
        val actions = mutableListOf<Pair<String, ScanResult>>()
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme {
                        ScanResultContent(
                            results = listOf(result),
                            onOpen = { actions += "open" to it },
                            onCopy = { actions += "copy" to it },
                            onShare = { actions += "share" to it },
                        )
                    }
                }
            }
            listOf("open", "share", "copy").forEach { action ->
                composeRule.onNodeWithText(result.value).performClick()
                composeRule.onNodeWithText(action).performClick()
                composeRule.onNodeWithText(action).assertDoesNotExist()
            }
            assertEquals(listOf("open" to result, "share" to result, "copy" to result), actions)
        }
    }

    @Test
    fun longValueCanScrollWhileActionsRemainVisible() {
        val result = ScanResult("long value\n".repeat(200), "Text", "QR code", false)
        var actions = 0
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme {
                        ScanResultContent(
                            listOf(result),
                            onOpen = { actions++ },
                            onCopy = { actions++ },
                            onShare = { actions++ },
                        )
                    }
                }
            }
            composeRule.onNodeWithText(result.value).performClick()
            composeRule.onNode(hasScrollAction() and hasAnyAncestor(isDialog())).performTouchInput { swipeUp() }
            composeRule.onNodeWithText("copy").assertIsDisplayed()
            composeRule.onNodeWithText("open").assertIsDisplayed()
            assertEquals(0, actions)
        }
    }
}
