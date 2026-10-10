/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import android.annotation.SuppressLint
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.DetectedPresenter
import net.mm2d.codereader.code.CodeScanner
import net.mm2d.codereader.view.DetectedMarkerView

// AndroidView の factory 専用で、Activity と検出コールバックが必要なため XML からは生成しない。
@SuppressLint("ViewConstructor")
class CameraPreviewView(
    activity: ComponentActivity,
    onDetect: (List<Barcode>) -> List<Barcode>,
) : FrameLayout(activity) {
    private val preview = PreviewView(activity).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    private val stillImage = ImageView(activity).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        visibility = View.GONE
    }
    private val marker = DetectedMarkerView(activity)
    val codeScanner: CodeScanner = CodeScanner(activity, preview, callback = { image, codes ->
        val detected = onDetect(codes)
        if (detected.isNotEmpty()) presenter.onDetected(image, detected)
    })
    private val presenter: DetectedPresenter = DetectedPresenter(codeScanner, marker, stillImage)

    init {
        listOf(preview, stillImage, marker).forEach {
            addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        codeScanner.initialize()
    }

    fun start() {
        codeScanner.start()
    }

    fun release() {
        codeScanner.destroy()
        presenter.destroy()
    }
}
