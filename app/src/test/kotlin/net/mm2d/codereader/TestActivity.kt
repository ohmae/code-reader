package net.mm2d.codereader

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import net.mm2d.codereader.ui.license.LicenseScreen
import net.mm2d.codereader.ui.settings.SettingsScreen
import net.mm2d.codereader.ui.theme.AppTheme
import org.robolectric.Shadows.shadowOf

class SettingsTestActivity : AppCompatActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { SettingsScreen(onBack = ::finish) } }
    }
}

class LicenseTestActivity : AppCompatActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { LicenseScreen(onBack = ::finish) } }
    }
}

fun launchSettingsTestActivity(): ActivityScenario<SettingsTestActivity> =
    launchTestActivity(SettingsTestActivity::class.java)

fun launchLicenseTestActivity(): ActivityScenario<LicenseTestActivity> =
    launchTestActivity(LicenseTestActivity::class.java)

fun <T : AppCompatActivity> launchTestActivity(
    activityClass: Class<T>,
): ActivityScenario<T> {
    val app = ApplicationProvider.getApplicationContext<App>()
    // AndroidX Test にテスト専用 Activity を登録する API がないため、この部分だけ Shadow を使う。
    shadowOf(app.packageManager).addOrUpdateActivity(
        ActivityInfo().apply {
            packageName = app.packageName
            name = activityClass.name
            applicationInfo = app.applicationInfo
            theme = R.style.Theme_CodeReader_NoActionBar
            exported = true
        },
    )
    return ActivityScenario.launch(activityClass)
}
