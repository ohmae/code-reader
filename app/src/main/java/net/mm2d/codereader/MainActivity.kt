/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.animation.ValueAnimator
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.code.CodeScanner
import net.mm2d.codereader.databinding.ActivityMainBinding
import net.mm2d.codereader.extension.formatString
import net.mm2d.codereader.extension.typeString
import net.mm2d.codereader.permission.CameraPermission
import net.mm2d.codereader.permission.PermissionDialog
import net.mm2d.codereader.permission.registerForCameraPermissionRequest
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.result.ScanResultAdapter
import net.mm2d.codereader.result.ScanResultDialog
import net.mm2d.codereader.setting.Settings
import net.mm2d.codereader.util.Launcher
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.Updater
import net.mm2d.codereader.util.observe

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var codeScanner: CodeScanner
    private var started: Boolean = false
    private val launcher = registerForCameraPermissionRequest { granted, succeedToShowDialog ->
        if (granted) {
            startCamera()
        } else if (!succeedToShowDialog) {
            PermissionDialog.show(this, CAMERA_PERMISSION_REQUEST_KEY)
        } else {
            finishByError()
        }
    }
    private lateinit var adapter: ScanResultAdapter
    private lateinit var vibrator: Vibrator
    private lateinit var detectedPresenter: DetectedPresenter
    private val viewModel: MainActivityViewModel by viewModels()
    private val settings: Settings by lazy {
        Settings.get()
    }
    private var resultSet: Set<ScanResult> = emptySet()

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        adapter = ScanResultAdapter(this) {
            ScanResultDialog.show(this, it)
        }
        binding.resultList.adapter = adapter
        binding.resultList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL),
        )
        vibrator = getSystemService()!!
        codeScanner = CodeScanner(this, binding.previewView, ::onDetectCode)
        binding.flash.setOnClickListener {
            codeScanner.toggleTorch()
        }
        codeScanner.getTouchStateStream().observe(this) {
            onFlashOn(it)
        }
        detectedPresenter = DetectedPresenter(
            codeScanner = codeScanner,
            detectedMarker = binding.detectedMarker,
            stillImage = binding.stillImage,
        )
        val size = viewModel.getResultStream().value.size
        if (size >= 2) {
            binding.dummy.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = 0
            }
        }
        viewModel.getResultStream().observe(this) {
            resultSet = it.toSet()
            adapter.onChanged(it)
            binding.resultList.scrollToPosition(adapter.itemCount - 1)
            if (it.isNotEmpty()) {
                binding.scanning.isGone = true
            }
            if (it.size == 2) {
                expandList()
            }
        }
        if (CameraPermission.hasPermission(this)) {
            startCamera()
            Updater.startIfAvailable(this)
        } else {
            launcher.launch()
        }
        PermissionDialog.registerListener(this, CAMERA_PERMISSION_REQUEST_KEY) {
            finishByError()
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (!started) {
            if (CameraPermission.hasPermission(this)) {
                startCamera()
            } else {
                finishByError()
            }
        }
    }

    private fun finishByError() {
        toastPermissionError()
        super.finish()
    }

    override fun finish() {
        if (ReviewRequester.requestIfNecessary(this)) {
            return
        }
        super.finish()
    }

    private fun toastPermissionError() {
        Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show()
    }

    private fun onFlashOn(
        on: Boolean,
    ) {
        val icon = if (on) {
            R.drawable.ic_flash_on
        } else {
            R.drawable.ic_flash_off
        }
        binding.flash.setImageResource(icon)
    }

    private fun startCamera() {
        if (started) return
        started = true
        codeScanner.start()
    }

    private fun onDetectCode(
        imageProxy: ImageProxy,
        codes: List<Barcode>,
    ) {
        val detected = mutableListOf<Barcode>()
        codes.forEach {
            val value = it.rawValue ?: return@forEach
            val result = ScanResult(
                value = value,
                type = it.typeString(),
                format = it.formatString(),
                isUrl = it.valueType == Barcode.TYPE_URL,
            )
            if (!resultSet.contains(result)) {
                viewModel.add(result)
                vibrate()
                detected.add(it)
            }
        }
        if (detected.isEmpty()) return
        detectedPresenter.onDetected(imageProxy, detected)
    }

    private fun expandList() {
        ValueAnimator.ofInt(binding.dummy.height, 0)
            .also {
                it.addUpdateListener {
                    binding.dummy.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        height = it.animatedValue as Int
                    }
                }
            }.start()
    }

    private fun vibrate() {
        if (!settings.vibrate) return
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
    ): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem,
    ): Boolean {
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

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_KEY = "CAMERA_PERMISSION_REQUEST_KEY"
    }
}
