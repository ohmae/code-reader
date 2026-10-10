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
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
class CodeAnalyzer(
    private val scanner: BarcodeScanner,
    callback: (ImageProxy, List<Barcode>) -> Unit,
    private val processFrame: (ImageProxy) -> Task<List<Barcode>>? = { proxy ->
        proxy.image?.let { image ->
            scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
        }
    },
) : Analyzer {
    private val lock = Any()
    private var callback: ((ImageProxy, List<Barcode>) -> Unit)? = callback
    private var paused: Boolean = false
    private var closed: Boolean = false
    private var inFlight: Int = 0

    override fun analyze(
        imageProxy: ImageProxy,
    ) {
        synchronized(lock) {
            if (paused || closed) {
                imageProxy.close()
                return
            }
            inFlight++
        }
        val task = try {
            processFrame(imageProxy)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
        if (task == null) {
            completeFrame(imageProxy)
            return
        }
        task
            .addOnSuccessListener { codes ->
                synchronized(lock) { if (!closed) callback?.invoke(imageProxy, codes) }
            }
            .addOnFailureListener { Timber.e(it) }
            .addOnCompleteListener { completeFrame(imageProxy) }
    }

    private fun completeFrame(
        imageProxy: ImageProxy,
    ) {
        try {
            imageProxy.close()
        } finally {
            synchronized(lock) {
                inFlight--
                if (closed && inFlight == 0) scanner.close()
            }
        }
    }

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            callback = null
            // ML Kit がフレームを使い終わるまで scanner を閉じない。
            if (inFlight == 0) scanner.close()
        }
    }

    fun resume() {
        synchronized(lock) { paused = false }
    }

    fun pause() {
        synchronized(lock) { paused = true }
    }
}
