/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.code

import androidx.activity.ComponentActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScanner(
    private val activity: ComponentActivity,
    previewView: PreviewView,
    callback: (ImageProxy, List<Barcode>) -> Unit,
) {
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private val analyzer: CodeAnalyzer = CodeAnalyzer(scanner, callback)
    private var camera: Camera? = null
    private val preview: Preview
    private val analysis: ImageAnalysis
    private var processCameraProvider: ProcessCameraProvider? = null
    private var isInitialized: Boolean = false

    init {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.surfaceProvider = previewView.surfaceProvider
        analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> bind()

                    Lifecycle.Event.ON_PAUSE -> unbind()

                    Lifecycle.Event.ON_DESTROY -> {
                        workerExecutor.shutdown()
                        scanner.close()
                    }

                    else -> Unit
                }
            },
        )
    }

    fun start() {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            processCameraProvider = future.get()
            bind()
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun bind() {
        if (activity.lifecycle.currentState != Lifecycle.State.RESUMED) return
        val provider = processCameraProvider ?: return
        analysis.setAnalyzer(workerExecutor, analyzer)
        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            camera.attachTorchObserver()
            this.camera = camera
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun unbind() {
        val provider = processCameraProvider ?: return
        provider.unbindAll()
        analysis.clearAnalyzer()
        camera?.detachTorchObserver()
        camera = null
    }

    fun toggleTorch() {
        val camera = camera ?: return
        camera.cameraControl.enableTorch(!torchStateFlow.value)
    }

    private val torchStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val torchStateObserver: Observer<Int> = Observer { state ->
        torchStateFlow.tryEmit(state == TorchState.ON)
    }

    fun getTouchStateStream(): Flow<Boolean> = torchStateFlow

    private fun Camera.attachTorchObserver() {
        cameraInfo.torchState.observe(activity, torchStateObserver)
    }

    private fun Camera.detachTorchObserver() {
        cameraInfo.torchState.removeObserver(torchStateObserver)
    }

    fun resume() {
        analyzer.resume()
    }

    fun pause() {
        analyzer.pause()
    }
}
