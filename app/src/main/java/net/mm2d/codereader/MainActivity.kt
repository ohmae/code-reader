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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
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
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.result.ScanResultAdapter
import net.mm2d.codereader.result.ScanResultDialog
import net.mm2d.codereader.setting.Settings
import net.mm2d.codereader.util.Launcher
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.Updater
import net.mm2d.codereader.util.observe

class MainActivity : AppCompatActivity(), PermissionDialog.OnCancelListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var codeScanner: CodeScanner
    private var started: Boolean = false
    private val launcher = registerForActivityResult(
        CameraPermission.RequestContract(), ::onPermissionResult
    )
    private lateinit var adapter: ScanResultAdapter
    private lateinit var vibrator: Vibrator
    private val viewModel: MainActivityViewModel by viewModels()
    private val settings: Settings by lazy {
        Settings.get()
    }
    private var resultSet: Set<ScanResult> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        adapter = ScanResultAdapter(this) {
            ScanResultDialog.show(this, it)
        }
        binding.resultList.adapter = adapter
        binding.resultList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        vibrator = getSystemService()!!
        codeScanner = CodeScanner(this, binding.previewView, ::onDetectCode)
        binding.flash.setOnClickListener {
            codeScanner.toggleTorch()
        }
        codeScanner.torchStateFlow.observe(this) {
            onFlashOn(it)
        }
        val size = viewModel.resultFlow.value.size
        if (size >= 2) {
            binding.dummy.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = 0
            }
        }
        viewModel.resultFlow.observe(this) {
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
                finishByError()
            }
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        when {
            granted ->
                startCamera()

            CameraPermission.deniedWithoutShowDialog(this) ->
                PermissionDialog.show(this)

            else -> {
                toastPermissionError()
                finishByError()
            }
        }
    }

    override fun onPermissionCancel() {
        finishByError()
    }

    private fun finishByError() {
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

    private fun onFlashOn(on: Boolean) {
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

    private fun onDetectCode(codes: List<Barcode>) {
        codes.forEach {
            val value = it.rawValue ?: return@forEach
            val result = ScanResult(
                value = value,
                type = it.typeString(),
                format = it.formatString(),
                isUrl = it.valueType == Barcode.TYPE_URL
            )
            if (!resultSet.contains(result)) {
                viewModel.add(result)
                vibrate()
            }
        }
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
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
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
