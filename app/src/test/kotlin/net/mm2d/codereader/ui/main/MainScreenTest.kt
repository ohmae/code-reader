package net.mm2d.codereader.ui.main

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.R
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
class MainScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun expandingResultsKeepsCameraBoundsAndMovesTorch() {
        var results by mutableStateOf(emptyList<ScanResult>())
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme {
                        MainScreen(
                            results = results,
                            torchOn = false,
                            onToggleTorch = {},
                            onMenuAction = {},
                            onOpen = {},
                            onCopy = {},
                            onShare = {},
                            systemBars = WindowInsets(0, 24, 0, 16),
                            cameraPreview = { Box(it.testTag("camera")) },
                        )
                    }
                }
            }
            val camera = composeRule.onNodeWithTag("camera").fetchSemanticsNode().boundsInRoot
            val flash = composeRule.onNodeWithTag("torch").fetchSemanticsNode().boundsInRoot
            val compactList = composeRule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
            composeRule.onNodeWithText("Scanning…").assertIsDisplayed()
            composeRule.runOnIdle { results = listOf(ScanResult("first", "Text", "QR code", false)) }
            composeRule.onNodeWithText("Scanning…").assertDoesNotExist()
            assertEquals(compactList, composeRule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot)
            composeRule.runOnIdle { results = results + ScanResult("second", "Text", "QR code", false) }
            composeRule.waitForIdle()
            val expandedList = composeRule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
            val expandedFlash = composeRule.onNodeWithTag("torch").fetchSemanticsNode().boundsInRoot
            assertEquals(camera, composeRule.onNodeWithTag("camera").fetchSemanticsNode().boundsInRoot)
            assertEquals(80f, expandedList.height - compactList.height, 1f)
            assertEquals(80f, flash.top - expandedFlash.top, 1f)
        }
    }

    @Test
    fun menuAndTorchDispatchSelectedActions() {
        var menuAction = 0
        var torchActions = 0
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme {
                        MainScreen(
                            results = emptyList(),
                            torchOn = true,
                            onToggleTorch = { torchActions++ },
                            onMenuAction = { menuAction = it },
                            onOpen = {},
                            onCopy = {},
                            onShare = {},
                            cameraPreview = { Box(it) },
                        )
                    }
                }
            }
            composeRule.onNodeWithContentDescription("Turn flashlight off").performClick()
            assertEquals(1, torchActions)
            composeRule.onNodeWithContentDescription("Options menu").performClick()
            composeRule.onNodeWithText("Settings").performClick()
            assertEquals(R.string.options_menu_settings, menuAction)
            composeRule.onNodeWithText("Settings").assertDoesNotExist()
        }
    }
}
