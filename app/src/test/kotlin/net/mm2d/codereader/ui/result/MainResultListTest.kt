package net.mm2d.codereader.ui.result

import android.content.ClipboardManager
import android.content.pm.ProviderInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.common.internal.MlKitInitProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import net.mm2d.codereader.App
import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.MainActivity
import net.mm2d.codereader.MainActivityViewModel
import net.mm2d.codereader.R
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.setting.Settings
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class MainResultListTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun initializeMlKit() {
        if (runCatching { MlKitContext.getInstance() }.isSuccess) return
        // テスト環境では ML Kit の初期化 Provider が自動起動しないため、明示的に起動する。
        MlKitInitProvider().attachInfo(
            ApplicationProvider.getApplicationContext<App>(),
            ProviderInfo().apply { authority = BuildConfig.APPLICATION_ID + ".mlkitinitprovider" },
        )
    }

    @Test
    fun restoresSelectedResultWithoutRepeatingActionAndCopiesOnce() {
        val settings = Settings.get()
        val initialCount = settings.detectValueActionCount
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                ViewModelProvider(it)[MainActivityViewModel::class.java]
                    .add(ScanResult("selected result", "Text", "QR code", false))
            }
            composeRule.onNodeWithText("selected result").performClick()
            composeRule.onNodeWithText("copy").assertIsDisplayed()
            scenario.recreate()
            composeRule.onNodeWithText("copy").assertIsDisplayed()
            assertEquals(initialCount, settings.detectValueActionCount)
            composeRule.onNodeWithText("copy").performClick()
            composeRule.onNodeWithText("copy").assertDoesNotExist()
            scenario.onActivity {
                val clip = it.getSystemService<ClipboardManager>()!!.primaryClip!!
                assertEquals("Text", clip.description.label.toString())
                assertEquals("selected result", clip.getItemAt(0).text.toString())
            }
            assertEquals(initialCount + 1, settings.detectValueActionCount)
            scenario.recreate()
            composeRule.onNodeWithText("copy").assertDoesNotExist()
            assertEquals(initialCount + 1, settings.detectValueActionCount)
            scenario.onActivity {
                ViewModelProvider(it)[MainActivityViewModel::class.java]
                    .add(ScanResult("next result", "Text", "QR code", false))
            }
            composeRule.onNodeWithText("next result").performClick()
            composeRule.onNodeWithText("copy").assertIsDisplayed()
        }
    }

    @Test
    fun restoresUserScrollPositionAcrossActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val model = ViewModelProvider(it)[MainActivityViewModel::class.java]
                (0..9).forEach { model.add(ScanResult("value $it", "Text", "QR code", false)) }
            }
            composeRule.onNodeWithText("value 9").assertIsDisplayed()
            composeRule.onNode(hasScrollAction()).performScrollToIndex(0)
            composeRule.onNodeWithText("value 0").assertIsDisplayed()
            scenario.recreate()
            composeRule.onNodeWithText("value 0").assertIsDisplayed()
            scenario.onActivity {
                assertEquals(10, ViewModelProvider(it)[MainActivityViewModel::class.java].getResultStream().value.size)
            }
        }
    }

    @Test
    fun settingsNavigationAndRecreationRetainScanResultsInSingleActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val context = ApplicationProvider.getApplicationContext<App>()
            scenario.onActivity {
                ViewModelProvider(it)[MainActivityViewModel::class.java]
                    .add(ScanResult("retained result", "Text", "QR code", false))
            }
            composeRule.onNodeWithContentDescription(context.getString(R.string.action_options_menu)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.options_menu_settings)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.preference_title_vibration)).assertExists()
            scenario.recreate()
            composeRule.onNodeWithText(context.getString(R.string.preference_title_vibration)).assertExists()
            composeRule.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
            composeRule.onNodeWithText("retained result").assertIsDisplayed()
        }
    }
}
