package net.mm2d.codereader.ui.navigation

import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.R
import net.mm2d.codereader.launchTestActivity
import net.mm2d.codereader.ui.theme.AppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class NavigationRootTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun restoresSettingsAndMainStateAcrossRecreationAndReturnsFromToolbar() {
        launchTestActivity(NavigationTestActivity::class.java).use { scenario ->
            composeRule.onNodeWithText("Count 0").performClick()
            composeRule.onNodeWithText("Settings").performClick()
            composeRule.onNodeWithText(string(R.string.preference_title_vibration)).assertExists()
            scenario.recreate()
            composeRule.onNodeWithText(string(R.string.preference_title_vibration)).assertExists()
            composeRule.onNodeWithContentDescription(string(R.string.navigate_back)).performClick()
            composeRule.onNodeWithText("Count 1").assertExists()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    @Test
    fun restoresLicenseAndSystemBackReturnsToMainThenExits() {
        launchTestActivity(NavigationTestActivity::class.java).use { scenario ->
            composeRule.onNodeWithText("License").performClick()
            composeRule.onNodeWithText(string(R.string.options_menu_license)).assertExists()
            scenario.recreate()
            composeRule.onNodeWithText(string(R.string.options_menu_license)).assertExists()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText("Count 0").assertExists()
            scenario.onActivity {
                assertFalse(it.isFinishing)
                it.onBackPressedDispatcher.onBackPressed()
                assertTrue(it.isFinishing)
            }
        }
    }

    @Test
    fun cancellingPredictiveBackKeepsSettingsAndCompletingReturnsToMain() {
        launchTestActivity(NavigationTestActivity::class.java).use { scenario ->
            composeRule.onNodeWithText("Settings").performClick()
            composeRule.waitForIdle()
            scenario.onActivity {
                it.onBackPressedDispatcher.dispatchOnBackStarted(backEvent(0f))
                it.onBackPressedDispatcher.dispatchOnBackProgressed(backEvent(0.5f))
            }
            composeRule.waitForIdle()
            scenario.onActivity { it.onBackPressedDispatcher.dispatchOnBackCancelled() }
            composeRule.onNodeWithText(string(R.string.preference_title_vibration)).assertExists()
            scenario.onActivity {
                it.onBackPressedDispatcher.dispatchOnBackStarted(backEvent(0f))
                it.onBackPressedDispatcher.dispatchOnBackProgressed(backEvent(0.5f))
            }
            composeRule.waitForIdle()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText("Count 0").assertExists()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    private fun string(
        id: Int,
    ): String = ApplicationProvider.getApplicationContext<App>().getString(id)

    private fun backEvent(
        progress: Float,
    ): BackEventCompat = BackEventCompat(0f, 0f, progress, BackEventCompat.EDGE_LEFT)
}

class NavigationTestActivity : AppCompatActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                NavigationRoot { settings, license ->
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        Button(onClick = { count++ }) { Text("Count $count") }
                        Button(onClick = settings) { Text("Settings") }
                        Button(onClick = license) { Text("License") }
                    }
                }
            }
        }
    }
}
