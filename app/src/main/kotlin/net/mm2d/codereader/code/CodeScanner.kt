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
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
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
    private val previewView: PreviewView,
    callback: (ImageProxy, List<Barcode>) -> Unit,
) {
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private val analyzer: CodeAnalyzer = CodeAnalyzer(scanner, callback)
    private var camera: Camera? = null
    private val torchStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var cameraProviderInternal: ProcessCameraProvider? = null
    private var previewUseCaseInternal: Preview? = null
    private var analysisUseCaseInternal: ImageAnalysis? = null

    fun getTouchStateStream(): Flow<Boolean> = torchStateFlow

    init {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Event.ON_DESTROY) {
                    if (!workerExecutor.isShutdown) {
                        workerExecutor.shutdown()
                    }
                    scanner.close()
                }
            },
        )
    }

    fun start() {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                val provider = future.get()
                this.cameraProviderInternal = provider
                setUp(provider)
            } catch (e: Exception) {
                Timber.e(e, "CodeScanner: Failed to get ProcessCameraProvider in start() listener.")
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun setUp(
        provider: ProcessCameraProvider,
    ) {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.surfaceProvider = previewView.surfaceProvider
        this.previewUseCaseInternal = preview

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(workerExecutor, analyzer)
        this.analysisUseCaseInternal = analysis

        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                activity,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCaseInternal!!,
                analysisUseCaseInternal!!,
            )
            this.camera = camera
            camera.cameraInfo.torchState.observe(activity) { state ->
                torchStateFlow.tryEmit(state == TorchState.ON)
            }
        } catch (e: Exception) {
            Timber.e(e, "CodeScanner: Use case binding failed in setUp.")
            this.camera = null
        }
    }

    fun shutdownCamera() {
        cameraProviderInternal?.let { provider ->
            val useCasesToUnbind = mutableListOf<UseCase>()
            previewUseCaseInternal?.let { useCasesToUnbind.add(it) }
            analysisUseCaseInternal?.let { useCasesToUnbind.add(it) }

            if (useCasesToUnbind.isNotEmpty()) {
                try {
                    provider.unbind(*useCasesToUnbind.toTypedArray())
                } catch (e: Exception) {
                    Timber.e(e, "CodeScanner: [shutdownCamera] Error unbinding specific use cases: ${e.message}")
                }
            }
        }
        previewUseCaseInternal = null
        analysisUseCaseInternal = null
        camera = null
    }

    fun toggleTorch() {
        val camera = this.camera ?: return
        camera.cameraControl.enableTorch(!torchStateFlow.value)
    }

    fun resume() {
        analyzer.resume()
    }

    fun pause() {
        analyzer.pause()
    }
}
