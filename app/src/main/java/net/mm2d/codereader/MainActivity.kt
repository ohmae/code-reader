/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import net.mm2d.codereader.databinding.ActivityMainBinding
import net.mm2d.codereader.extension.formatString
import net.mm2d.codereader.extension.typeString
import net.mm2d.codereader.permission.CameraPermission
import net.mm2d.codereader.permission.PermissionDialog
import net.mm2d.codereader.util.Launcher
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var workerExecutor: ExecutorService
    private lateinit var scanner: BarcodeScanner
    private var started: Boolean = false
    private val launcher = registerForActivityResult(/**/
        CameraPermission.RequestContract(), ::onPermissionResult
    )
    private var detectedType: String = ""
    private var detectedString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.openButton.setOnClickListener {
            Launcher.openUri(this, detectedString)
        }
        binding.copyButton.setOnClickListener {
            getSystemService<ClipboardManager>()?.let {
                it.setPrimaryClip(ClipData.newPlainText(detectedType, detectedString))
                Toast.makeText(this, R.string.toast_copy_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
        binding.shareButton.setOnClickListener {
            Launcher.shareText(this, detectedString)
        }

        workerExecutor = Executors.newSingleThreadExecutor()
        scanner = BarcodeScanning.getClient()
        if (CameraPermission.hasPermission(this)) {
            startCamera()
        } else {
            launcher.launch(Unit)
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (!started) {
            if (CameraPermission.hasPermission(this)) {
                startCamera()
            } else {
                toastPermissionError()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        workerExecutor.shutdown()
        scanner.close()
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            startCamera()
        } else {
            if (CameraPermission.deniedWithoutShowDialog(this)) {
                PermissionDialog.show(this)
            } else {
                toastPermissionError()
                finish()
            }
        }
    }

    private fun toastPermissionError() {
        Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show()
    }

    private fun startCamera() {
        if (started) return
        started = true
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            setUpCameraProvider(future.get())
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setUpCameraProvider(provider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val analysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(workerExecutor, CodeAnalyzer(scanner, ::onDetectCode))

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun onDetectCode(codes: List<Barcode>) {
        codes.getOrNull(0)?.let {
            detectedString = it.rawValue ?: ""
            detectedType = it.typeString()
            binding.resultText.text = it.rawValue
            binding.resultType.text = getString(R.string.type, detectedType)
            binding.resultFormat.text = getString(R.string.format, it.formatString())
            binding.openButton.isEnabled = it.valueType == Barcode.TYPE_URL
            binding.copyButton.isEnabled = true
            binding.shareButton.isEnabled = true
        }
    }

    private class CodeAnalyzer(
        private val scanner: BarcodeScanner,
        private val callback: (List<Barcode>) -> Unit
    ) : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val image = imageProxy.image
            if (image != null) {
                val inputImage = InputImage
                    .fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { callback(it) }
                    .addOnFailureListener { Timber.e(it) }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.license -> LicenseActivity.start(this)
            R.id.source_code -> Launcher.openSourceCode(this)
            R.id.privacy_policy -> Launcher.openPrivacyPolicy(this)
            R.id.share_this_app -> Launcher.shareThisApp(this)
            R.id.play_store -> Launcher.openGooglePlay(this)
            R.id.settings -> SettingsActivity.start(this)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
