/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.code

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis.Analyzer
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

class CodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val callback: (ImageProxy, List<Barcode>) -> Unit,
) : Analyzer {
    private var paused: Boolean = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(
        imageProxy: ImageProxy,
    ) {
        if (paused) {
            imageProxy.close()
            return
        }
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { callback(imageProxy, it) }
            .addOnFailureListener { Timber.e(it) }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun resume() {
        paused = false
    }

    fun pause() {
        paused = true
    }
}
