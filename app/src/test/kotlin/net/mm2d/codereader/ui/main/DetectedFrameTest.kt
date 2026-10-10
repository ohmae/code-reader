package net.mm2d.codereader.ui.main

import android.graphics.Bitmap
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.mm2d.codereader.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetectedFrameTest {
    @Test
    fun captureRotatesBitmapButDoesNotCloseAnalyzerFrame() {
        listOf(0, 90, 180, 270).forEach { rotation ->
            var closed = false
            val original = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888).apply {
                setPixel(0, 0, 0xffff0000.toInt())
            }
            val info = proxy<ImageInfo> { if (it == "getRotationDegrees") rotation else 0 }
            val image = proxy<ImageProxy> { name ->
                when (name) {
                    "getWidth" -> 20

                    "getHeight" -> 40

                    "toBitmap" -> original

                    "getImageInfo" -> info

                    "close" -> {
                        closed = true
                        null
                    }

                    else -> null
                }
            }
            val frame = DetectedFrame.capture(image, emptyList())
            assertEquals(if (rotation == 90 || rotation == 270) 40 else 20, frame.bitmap.width)
            assertEquals(if (rotation == 90 || rotation == 270) 20 else 40, frame.bitmap.height)
            val corner = when (rotation) {
                90 -> 39 to 0
                180 -> 19 to 39
                270 -> 0 to 19
                else -> 0 to 0
            }
            assertEquals(0xffff0000.toInt(), frame.bitmap.getPixel(corner.first, corner.second))
            assertEquals(rotation, frame.rotationDegrees)
            assertFalse(frame.bitmap.isRecycled)
            assertFalse(closed)
        }
    }

    private inline fun <reified T> proxy(
        crossinline call: (String) -> Any?,
    ): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            call(method.name)
        } as T
}
