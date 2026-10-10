/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.code.CodeScanner
import timber.log.Timber

class CameraPreviewState(
    private val activity: ComponentActivity,
    onDetect: (List<Barcode>) -> List<Barcode>,
) {
    val codeScanner: CodeScanner = CodeScanner(activity, callback = { image, codes ->
        if (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
            val detected = onDetect(codes)
            if (detected.isNotEmpty()) {
                try {
                    detection.show(DetectedFrame.capture(image, detected))
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    })
    val detection: DetectionEffectState = DetectionEffectState(codeScanner::pause, codeScanner::resume)
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE) detection.clear()
    }

    fun initialize() {
        codeScanner.initialize()
        activity.lifecycle.addObserver(lifecycleObserver)
    }

    fun start() {
        codeScanner.start()
    }

    fun release() {
        activity.lifecycle.removeObserver(lifecycleObserver)
        codeScanner.destroy()
        detection.clear()
    }
}
