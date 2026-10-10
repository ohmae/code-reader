package net.mm2d.codereader.ui.license

import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.LicenseActivity
import net.mm2d.codereader.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class LicenseScreenTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun showsLicenseAndReturnsFromToolbar() {
        ActivityScenario.launch(LicenseActivity::class.java).use { scenario ->
            val context = ApplicationProvider.getApplicationContext<App>()
            composeRule.onNodeWithText(context.getString(R.string.options_menu_license)).assertExists()
            scenario.onActivity {
                val webView = findWebView(it.window.decorView)!!
                assertEquals("file:///android_asset/license.html", webView.url)
                assertFalse(webView.settings.supportZoom())
                assertFalse(webView.settings.javaScriptEnabled)
            }
            composeRule.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
            scenario.onActivity { assertTrue(it.isFinishing) }
        }
    }

    @Test
    fun recreatesWebViewWithLicenseLoaded() {
        ActivityScenario.launch(LicenseActivity::class.java).use { scenario ->
            composeRule.waitForIdle()
            lateinit var previous: WebView
            scenario.onActivity { previous = findWebView(it.window.decorView)!! }
            scenario.recreate()
            composeRule.waitForIdle()
            scenario.onActivity {
                val current = findWebView(it.window.decorView)!!
                assertNotSame(previous, current)
                assertEquals("file:///android_asset/license.html", current.url)
            }
        }
    }

    @Test
    fun replacesWebViewWhenRendererExitsRepeatedly() {
        ActivityScenario.launch(LicenseActivity::class.java).use { scenario ->
            repeat(2) { attempt ->
                composeRule.waitForIdle()
                lateinit var previous: WebView
                scenario.onActivity {
                    previous = findWebView(it.window.decorView)!!
                    val detail = object : RenderProcessGoneDetail() {
                        override fun didCrash(): Boolean = attempt == 0
                        override fun rendererPriorityAtExit(): Int = WebView.RENDERER_PRIORITY_IMPORTANT
                    }
                    assertTrue(previous.webViewClient.onRenderProcessGone(previous, detail))
                    assertEquals(null, previous.parent)
                }
                composeRule.waitForIdle()
                scenario.onActivity {
                    val current = findWebView(it.window.decorView)!!
                    assertNotSame(previous, current)
                    assertEquals("file:///android_asset/license.html", current.url)
                    assertFalse(current.settings.supportZoom())
                    assertFalse(current.settings.javaScriptEnabled)
                }
            }
        }
    }

    private fun findWebView(
        view: View,
    ): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findWebView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
