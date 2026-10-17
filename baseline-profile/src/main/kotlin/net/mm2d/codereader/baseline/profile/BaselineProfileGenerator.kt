/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.baseline.profile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val targetAppId = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: InstrumentationRegistry.getArguments().getString("targetPackage")
            ?: "net.mm2d.codereader"

        rule.collect(
            packageName = targetAppId,
            includeInStartupProfile = true,
        ) {
            device.executeShellCommand("pm grant $targetAppId android.permission.CAMERA")

            // 1. Cold Startup
            pressHome()
            startActivityAndWait()

            // メイン画面の表示を待機
            device.wait(Until.hasObject(By.desc(MENU_DESC_PATTERN)), TIMEOUT_MS)

            // 2. メニューを開いて設定画面へ遷移
            val menuButtonForSettings = device.findObject(By.desc(MENU_DESC_PATTERN))
            if (menuButtonForSettings != null) {
                menuButtonForSettings.click()
                device.waitForIdle()

                val settingsItem = device.wait(Until.findObject(By.text(SETTINGS_TEXT_PATTERN)), TIMEOUT_MS)
                if (settingsItem != null) {
                    settingsItem.click()
                    device.waitForIdle()

                    // 設定画面の表示を待機
                    device.wait(Until.hasObject(By.text(VIBRATION_TEXT_PATTERN)), TIMEOUT_MS)

                    // 戻る操作でメイン画面へ復帰
                    device.pressBack()
                    device.waitForIdle()
                }
            }

            // 3. メニューを開いてライセンス画面へ遷移
            val menuButtonForLicense = device.findObject(By.desc(MENU_DESC_PATTERN))
            if (menuButtonForLicense != null) {
                menuButtonForLicense.click()
                device.waitForIdle()

                val licenseItem = device.wait(Until.findObject(By.text(LICENSE_TEXT_PATTERN)), TIMEOUT_MS)
                if (licenseItem != null) {
                    licenseItem.click()
                    device.waitForIdle()

                    // ライセンス画面（WebView）の読み込み待機
                    device.wait(Until.hasObject(By.text(LICENSE_TEXT_PATTERN)), TIMEOUT_MS)

                    // 戻る操作でメイン画面へ復帰
                    device.pressBack()
                    device.waitForIdle()
                }
            }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
        private val MENU_DESC_PATTERN = Pattern.compile("Options menu|メニュー")
        private val SETTINGS_TEXT_PATTERN = Pattern.compile("Settings|設定")
        private val VIBRATION_TEXT_PATTERN = Pattern.compile("Vibration|バイブ")
        private val LICENSE_TEXT_PATTERN = Pattern.compile("Open source licenses|オープンソースライセンス")
    }
}
