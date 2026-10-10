package net.mm2d.codereader.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.R
import net.mm2d.codereader.launchSettingsTestActivity
import net.mm2d.codereader.setting.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun togglesAndRestoresVibrationAcrossActivityRecreation() {
        Settings.get().vibrate = true
        launchSettingsTestActivity().use { scenario ->
            val title = ApplicationProvider.getApplicationContext<App>().getString(R.string.preference_title_vibration)
            composeRule.onNodeWithText(title).assertIsOn().performClick()
            composeRule.onNodeWithText(title).assertIsOff()
            composeRule.runOnIdle { assertFalse(Settings.get().vibrate) }

            scenario.recreate()
            composeRule.onNodeWithText(title).assertIsOff()
            composeRule.onNodeWithText(title).performClick().assertIsOn()
            composeRule.runOnIdle { assertTrue(Settings.get().vibrate) }
        }
    }

    @Test
    fun copiesVersionSummaryAndReturnsFromToolbar() {
        launchSettingsTestActivity().use { scenario ->
            val context = ApplicationProvider.getApplicationContext<App>()
            composeRule.onNodeWithText(context.getString(R.string.preference_title_version))
                .performTouchInput { longClick() }
            composeRule.onNodeWithText(context.getString(R.string.action_copy)).performClick()
            composeRule.runOnIdle {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals(BuildConfig.VERSION_NAME, clipboard.primaryClip?.getItemAt(0)?.text.toString())
            }

            composeRule.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }

    @Test
    @Config(qualifiers = "ja")
    fun copiesCurrentVibrationSummaryInJapaneseWithoutToggling() {
        Settings.get().vibrate = false
        launchSettingsTestActivity().use {
            val context = ApplicationProvider.getApplicationContext<App>()
            composeRule.onNodeWithText(context.getString(R.string.preference_title_vibration))
                .performTouchInput { longClick() }
            composeRule.onNodeWithText(context.getString(R.string.action_copy)).performClick()
            composeRule.runOnIdle {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals(
                    context.getString(R.string.preference_summary_vibration_off),
                    clipboard.primaryClip?.getItemAt(0)?.text.toString(),
                )
                assertFalse(Settings.get().vibrate)
            }
        }
    }

    @Test
    fun refreshesVibrationSettingWhenActivityStartsAgain() {
        Settings.get().vibrate = true
        launchSettingsTestActivity().use { scenario ->
            val title = ApplicationProvider.getApplicationContext<App>().getString(R.string.preference_title_vibration)
            composeRule.onNodeWithText(title).assertIsOn()
            scenario.moveToState(Lifecycle.State.CREATED)
            Settings.get().vibrate = false
            scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.onNodeWithText(title).assertIsOff()
        }
    }
}
