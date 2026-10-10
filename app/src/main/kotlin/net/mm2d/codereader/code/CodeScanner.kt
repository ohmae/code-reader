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
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScanner(
    private val activity: ComponentActivity,
    callback: (ImageProxy, List<Barcode>) -> Unit,
    private val lifecycleOwner: LifecycleOwner = activity,
    private val providerFactory: () -> ListenableFuture<ProcessCameraProvider> = {
        ProcessCameraProvider.getInstance(activity)
    },
) {
    private val workerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private val analyzer: CodeAnalyzer = CodeAnalyzer(scanner, callback)
    private var camera: Camera? = null
    private val surfaceRequestFlow = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = surfaceRequestFlow.asStateFlow()
    private val preview: Preview
    private val analysis: ImageAnalysis
    private var processCameraProvider: ProcessCameraProvider? = null
    private var isInitialized: Boolean = false
    private var startRequested: Boolean = false
    private var destroyed: Boolean = false
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> bind()
            Lifecycle.Event.ON_PAUSE -> unbind()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> Unit
        }
    }

    init {
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.setSurfaceProvider { request ->
            if (destroyed) {
                request.willNotProvideSurface()
            } else {
                surfaceRequestFlow.value = request
            }
        }
        analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    fun initialize() {
        if (isInitialized || destroyed) return
        isInitialized = true
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    fun start() {
        if (startRequested || destroyed) return
        startRequested = true
        val future = providerFactory()
        future.addListener({
            // プレビューの解放後に provider が届いても再接続しない。
            if (destroyed) return@addListener
            try {
                processCameraProvider = future.get()
                bind()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        unbind()
        preview.surfaceProvider = null
        processCameraProvider = null
        analyzer.close()
        workerExecutor.shutdown()
    }

    private fun bind() {
        if (destroyed || camera != null || lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) return
        val provider = processCameraProvider ?: return
        analysis.setAnalyzer(workerExecutor, analyzer)
        try {
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            camera.attachTorchObserver()
            this.camera = camera
        } catch (e: Exception) {
            analysis.clearAnalyzer()
            Timber.e(e)
        }
    }

    private fun unbind() {
        analysis.clearAnalyzer()
        camera?.detachTorchObserver()
        camera = null
        processCameraProvider?.unbind(preview, analysis)
        surfaceRequestFlow.value?.willNotProvideSurface()
        surfaceRequestFlow.value = null
        torchStateFlow.value = false
    }

    fun toggleTorch() {
        val camera = camera ?: return
        camera.cameraControl.enableTorch(!torchStateFlow.value)
    }

    private val torchStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val torchStateObserver: Observer<Int> = Observer { state ->
        torchStateFlow.tryEmit(state == TorchState.ON)
    }

    fun getTorchStateStream(): Flow<Boolean> = torchStateFlow

    private fun Camera.attachTorchObserver() {
        cameraInfo.torchState.observe(lifecycleOwner, torchStateObserver)
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
