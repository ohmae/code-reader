package net.mm2d.codereader.ui.result

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.launchSettingsTestActivity
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class, qualifiers = "en")
class ScanResultListTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun selectsSameValueWithDifferentMetadataAndDisplaysLongValue() {
        val first = ScanResult("same", "Text", "QR code", false)
        val second = first.copy(format = "Code 128")
        val long = first.copy(value = "long value ".repeat(100))
        var selected: ScanResult? = null
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme { ScanResultList(listOf(first, second, long), onSelect = { selected = it }) }
                }
            }
            composeRule.onNodeWithText("Code 128").performClick()
            composeRule.runOnIdle { assertEquals(second, selected) }
            composeRule.onNodeWithText(long.value).assertExists()
        }
    }

    @Test
    fun scrollsOnlyWhenResultsAreAdded() {
        var results by mutableStateOf(emptyList<ScanResult>())
        lateinit var listState: LazyListState
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity {
                it.setContent {
                    AppTheme {
                        listState = rememberLazyListState()
                        ScanResultList(
                            results = results,
                            onSelect = {},
                            listState = listState,
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                    }
                }
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
            composeRule.runOnIdle {
                results = (0..9).map { ScanResult("value $it", "Text", "QR code", false) }
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertTrue(listState.firstVisibleItemIndex >= 8) }
            composeRule.runOnIdle { listState.requestScrollToItem(0) }
            composeRule.waitForIdle()
            composeRule.runOnIdle { results = results.toList() }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
            composeRule.runOnIdle { results = results + ScanResult("new value", "Text", "QR code", false) }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertTrue(listState.firstVisibleItemIndex >= 9) }
        }
    }
}
