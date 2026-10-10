package net.mm2d.codereader.ui.result

import android.content.pm.ProviderInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import net.mm2d.codereader.result.ScanResultDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun selectingResultOpensExistingDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                ViewModelProvider(it)[MainActivityViewModel::class.java]
                    .add(ScanResult("selected result", "Text", "QR code", false))
            }
            composeRule.onNodeWithText("selected result").performClick()
            composeRule.waitForIdle()
            scenario.onActivity {
                it.supportFragmentManager.executePendingTransactions()
                val dialog = it.supportFragmentManager.fragments.filterIsInstance<ScanResultDialog>().single()
                assertTrue(dialog.requireDialog().isShowing)
                assertEquals(
                    "selected result",
                    dialog.requireDialog().findViewById<android.widget.TextView>(R.id.result_value).text.toString(),
                )
            }
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
}
