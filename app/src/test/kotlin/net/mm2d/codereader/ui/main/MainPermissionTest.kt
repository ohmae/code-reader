package net.mm2d.codereader.ui.main

import android.Manifest
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.common.internal.MlKitInitProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import net.mm2d.codereader.App
import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.MainActivity
import net.mm2d.codereader.launchSettingsTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
class MainPermissionTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun initialize() {
        val app = ApplicationProvider.getApplicationContext<App>()
        // AndroidX Test に権限取消し・要求履歴を取得する API がないため、この部分だけ Shadow を使う。
        shadowOf(app).denyPermissions(Manifest.permission.CAMERA)
        if (runCatching { MlKitContext.getInstance() }.isSuccess) return
        MlKitInitProvider().attachInfo(
            app,
            ProviderInfo().apply { authority = BuildConfig.APPLICATION_ID + ".mlkitinitprovider" },
        )
    }

    @Test
    fun recreatingDuringPermissionRequestDoesNotRequestAgain() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertNotNull(shadowOf(it).lastRequestedPermission) }
            scenario.recreate()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }
        }
    }

    @Test
    fun previewWithoutSurfaceStillDisplaysDetectionOverlay() {
        composeRule.mainClock.autoAdvance = false
        launchSettingsTestActivity().use { scenario ->
            lateinit var camera: CameraPreviewState
            scenario.onActivity {
                camera = CameraPreviewState(it) { emptyList() }
                camera.initialize()
                assertNull(camera.codeScanner.surfaceRequest.value)
                camera.detection.show(
                    DetectedFrame(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888), emptyList(), 20, 40, 0),
                )
                it.setContent { CameraPreview(camera, Modifier.size(100.dp, 200.dp)) }
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("detection").assertExists()
            scenario.onActivity { camera.release() }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag("detection").assertDoesNotExist()
        }
    }

    @Test
    fun pausingCameraOwnerClearsDetectionAndAllowsNextEffect() {
        launchSettingsTestActivity().use { scenario ->
            lateinit var camera: CameraPreviewState
            scenario.onActivity {
                camera = CameraPreviewState(it) { emptyList() }
                camera.initialize()
                camera.detection.show(
                    DetectedFrame(
                        Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888),
                        emptyList(),
                        20,
                        40,
                        0,
                    ),
                )
                assertNotNull(camera.detection.frame)
            }
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.onActivity { assertNull(camera.detection.frame) }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                val next = DetectedFrame(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888), emptyList(), 20, 40, 0)
                camera.detection.show(next)
                assertNotNull(camera.detection.frame)
                camera.release()
                camera.release()
                assertNull(camera.detection.frame)
            }
        }
    }

    @Test
    fun pausingNavigationEntryClearsDetectionWhileActivityRemainsResumed() {
        launchSettingsTestActivity().use { scenario ->
            scenario.onActivity { activity ->
                val owner = object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry(this)
                }
                owner.lifecycle.currentState = Lifecycle.State.RESUMED
                val camera = CameraPreviewState(activity, owner) { emptyList() }
                camera.initialize()
                assertEquals(2, owner.lifecycle.observerCount)
                camera.detection.show(
                    DetectedFrame(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888), emptyList(), 20, 40, 0),
                )
                assertNotNull(camera.detection.frame)
                owner.lifecycle.currentState = Lifecycle.State.STARTED
                assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
                assertNull(camera.detection.frame)
                owner.lifecycle.currentState = Lifecycle.State.RESUMED
                camera.detection.show(
                    DetectedFrame(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888), emptyList(), 20, 40, 0),
                )
                assertNotNull(camera.detection.frame)
                camera.release()
                assertEquals(0, owner.lifecycle.observerCount)
            }
        }
    }
}
