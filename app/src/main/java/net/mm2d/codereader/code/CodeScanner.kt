/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.code

import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScanner(
    private val activity: ComponentActivity,
    private val previewView: PreviewView,
    callback: (List<Barcode>) -> Unit
) {
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private val analyzer: CodeAnalyzer = CodeAnalyzer(scanner, callback)
    private var camera: Camera? = null
    val torchState: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Event.ON_DESTROY) {
                    workerExecutor.shutdown()
                    scanner.close()
                }
            }
        )
    }

    fun start() {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            setUp(future.get())
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun setUp(provider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val analysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(workerExecutor, analyzer)

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            ).let {
                it.cameraInfo.torchState.observe(activity) {
                    torchState.postValue(it == TorchState.ON)
                }
                camera = it
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun toggleTorch() {
        camera?.let {
            val next = !(torchState.value ?: false)
            it.cameraControl.enableTorch(next)
        }
    }
}