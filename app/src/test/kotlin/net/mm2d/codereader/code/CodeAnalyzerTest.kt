package net.mm2d.codereader.code

import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.App
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = App::class)
class CodeAnalyzerTest {
    private val events = mutableListOf<String>()
    private val task = TaskCompletionSource<List<Barcode>>()
    private val scanner = Proxy.newProxyInstance(
        BarcodeScanner::class.java.classLoader,
        arrayOf(BarcodeScanner::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "close" -> {
                events += "scanner.close"
                null
            }

            else -> null
        }
    } as BarcodeScanner

    @Test
    fun releaseWaitsForInFlightFrameAndSuppressesLateCallback() {
        val analyzer = CodeAnalyzer(
            scanner,
            callback = { _, _ -> events += "callback" },
            processFrame = {
                events += "process"
                task.task
            },
        )
        analyzer.analyze(frame())
        assertEquals(listOf("process"), events)
        analyzer.close()
        analyzer.close()
        assertEquals(listOf("process"), events)
        task.setResult(emptyList())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(listOf("process", "frame.close", "scanner.close"), events)
    }

    @Test
    fun callbackUsesFrameBeforeItClosesAndReleaseRejectsNewFrames() {
        val analyzer = CodeAnalyzer(
            scanner,
            callback = { _, _ -> events += "callback" },
            processFrame = {
                events += "process"
                task.task
            },
        )
        analyzer.analyze(frame())
        task.setResult(emptyList())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(listOf("process", "callback", "frame.close"), events)
        analyzer.close()
        analyzer.analyze(frame())
        assertEquals(listOf("process", "callback", "frame.close", "scanner.close", "frame.close"), events)
    }

    @Test
    fun failedTaskClosesFrameBeforeScanner() {
        val analyzer = CodeAnalyzer(scanner, callback = { _, _ -> events += "callback" }, processFrame = { task.task })
        analyzer.analyze(frame())
        analyzer.close()
        task.setException(IllegalStateException("processing failed"))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(listOf("frame.close", "scanner.close"), events)
    }

    @Test
    fun processExceptionClosesFrameAndAllowsImmediateRelease() {
        val analyzer = CodeAnalyzer(scanner, callback = { _, _ -> }, processFrame = { error("invalid frame") })
        analyzer.analyze(frame())
        analyzer.close()
        assertEquals(listOf("frame.close", "scanner.close"), events)
    }

    private fun frame(): ImageProxy =
        Proxy.newProxyInstance(
            ImageProxy::class.java.classLoader,
            arrayOf(ImageProxy::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "close" -> {
                    events += "frame.close"
                    null
                }

                else -> null
            }
        } as ImageProxy
}
