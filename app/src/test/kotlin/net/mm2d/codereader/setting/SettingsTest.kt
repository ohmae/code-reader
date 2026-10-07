package net.mm2d.codereader.setting

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class SettingsTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".Main", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun readsExistingVibrationSettingWithoutChangingReviewSettings() {
        preferences.edit()
            .putInt("PREFERENCES_VERSION_INT", 1)
            .putBoolean("VIBRATE_BOOLEAN", false)
            .putInt("COUNT_DETECT_VALUE_ACTION_INT", 17)
            .putBoolean("REVIEW_REVIEWED_BOOLEAN", true)
            .commit()

        Settings.initialize(context)

        assertFalse(Settings.get().vibrate)
        assertEquals(17, Settings.get().detectValueActionCount)
        assertTrue(Settings.get().reviewed)
    }

    @Test
    fun persistsVibrationChangesUsingExistingKey() {
        Settings.initialize(context)
        assertTrue(Settings.get().vibrate)
        Settings.get().detectValueActionCount = 17

        Settings.get().vibrate = false
        Settings.initialize(context)
        assertFalse(Settings.get().vibrate)
        assertFalse(preferences.getBoolean("VIBRATE_BOOLEAN", true))
        assertEquals(17, Settings.get().detectValueActionCount)

        Settings.get().vibrate = true
        Settings.initialize(context)
        assertTrue(Settings.get().vibrate)
        assertTrue(preferences.getBoolean("VIBRATE_BOOLEAN", false))
    }
}
