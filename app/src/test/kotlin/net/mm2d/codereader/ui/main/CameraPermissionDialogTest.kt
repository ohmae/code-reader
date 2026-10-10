package net.mm2d.codereader.ui.main

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.R
import net.mm2d.codereader.SettingsActivity
import net.mm2d.codereader.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class CameraPermissionDialogTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun appInfoAndCancelDispatchCallbacks() {
        var appInfo = 0
        var canceled = 0
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            var appInfoLabel = ""
            var cancelLabel = ""
            scenario.onActivity {
                appInfoLabel = it.getString(R.string.app_info)
                cancelLabel = it.getString(R.string.cancel)
                it.setContent { AppTheme { CameraPermissionDialog({ appInfo++ }, { canceled++ }) } }
            }
            composeRule.onNodeWithText(appInfoLabel).performClick()
            assertEquals(1, appInfo)
            assertEquals(0, canceled)
            composeRule.onNodeWithText(cancelLabel).performClick()
            assertEquals(1, appInfo)
            assertEquals(1, canceled)
        }
    }
}
