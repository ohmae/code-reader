package net.mm2d.codereader.ui.main

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import net.mm2d.codereader.SettingsActivity
import net.mm2d.codereader.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
class DetectionEffectTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun frame() =
        DetectedFrame(
            Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888),
            listOf(
                listOf(
                    DetectionPoint(2f, 4f),
                    DetectionPoint(18f, 4f),
                    DetectionPoint(18f, 36f),
                    DetectionPoint(2f, 36f),
                ),
            ),
            20,
            40,
            0,
        )

    @Test
    fun clearingAndStaleCompletionDoNotRecycleOrResumeNewFrame() {
        var paused = 0
        var resumed = 0
        val state = DetectionEffectState({ paused++ }, { resumed++ })
        val first = frame()
        val second = frame()
        state.show(first)
        state.show(second)
        state.finish(first)
        assertSame(second, state.frame)
        assertEquals(0, resumed)
        state.clear()
        state.clear()
        state.finish(second)
        assertNull(state.frame)
        assertEquals(2, paused)
        assertEquals(1, resumed)
        assertFalse(first.bitmap.isRecycled)
        assertFalse(second.bitmap.isRecycled)
    }

    @Test
    fun overlayFinishesAfterAnimationAndWait() {
        var resumed = 0
        val state = DetectionEffectState({}, { resumed++ })
        composeRule.mainClock.autoAdvance = false
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity {
                state.show(frame())
                it.setContent {
                    AppTheme {
                        state.frame?.let { DetectionOverlay(it, state::finish, Modifier.size(100.dp, 200.dp)) }
                    }
                }
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.mainClock.advanceTimeBy(1200)
            composeRule.onNodeWithTag("detection").assertExists()
            assertEquals(0, resumed)
            composeRule.mainClock.advanceTimeBy(400)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("detection").assertDoesNotExist()
            assertEquals(1, resumed)
        }
    }

    @Test
    fun removingOverlayCancelsWaitAndResumesOnce() {
        var visible by mutableStateOf(true)
        var resumed = 0
        val state = DetectionEffectState({}, { resumed++ })
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity {
                state.show(frame())
                it.setContent {
                    AppTheme {
                        if (visible) {
                            state.frame?.let { DetectionOverlay(it, state::finish, Modifier.size(100.dp, 200.dp)) }
                        } else {
                            Box(Modifier.size(100.dp))
                        }
                    }
                }
            }
            composeRule.mainClock.autoAdvance = false
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.runOnIdle { visible = false }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            assertNull(state.frame)
            assertEquals(1, resumed)
        }
    }
}
