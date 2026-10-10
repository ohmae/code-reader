/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import net.mm2d.codereader.extension.formatString
import net.mm2d.codereader.extension.typeString
import net.mm2d.codereader.permission.CameraPermission
import net.mm2d.codereader.permission.PermissionDialog
import net.mm2d.codereader.permission.registerForCameraPermissionRequest
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.setting.Settings
import net.mm2d.codereader.ui.main.CameraPreviewView
import net.mm2d.codereader.ui.main.MainScreen
import net.mm2d.codereader.ui.theme.AppTheme
import net.mm2d.codereader.util.ClipboardUtils
import net.mm2d.codereader.util.Launcher
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.Updater
import net.mm2d.codereader.util.observe

class MainActivity : AppCompatActivity() {
    private var cameraEnabled by mutableStateOf(false)
    private val launcher = registerForCameraPermissionRequest { granted, succeedToShowDialog ->
        if (granted) {
            startCamera()
        } else if (!succeedToShowDialog) {
            PermissionDialog.show(this, CAMERA_PERMISSION_REQUEST_KEY)
        } else {
            finishByError()
        }
    }
    private var vibrator: Vibrator? = null
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
        setContent {
            var cameraView by remember { mutableStateOf<CameraPreviewView?>(null) }
            val inactiveTorch = remember { MutableStateFlow(false) }
            val torchOn by (cameraView?.codeScanner?.getTorchStateStream() ?: inactiveTorch)
                .collectAsStateWithLifecycle(initialValue = false)
            val results by viewModel.getResultStream().collectAsStateWithLifecycle()
            AppTheme {
                MainScreen(
                    results = results,
                    torchOn = torchOn,
                    onToggleTorch = { cameraView?.codeScanner?.toggleTorch() },
                    onMenuAction = ::onMenuAction,
                    onOpen = {
                        if (!Launcher.openUri(this@MainActivity, it.value)) {
                            Launcher.search(this@MainActivity, it.value)
                        }
                        ReviewRequester.onAction()
                    },
                    onCopy = {
                        ClipboardUtils.copyToClipboard(this@MainActivity, it.type, it.value)
                        ReviewRequester.onAction()
                    },
                    onShare = {
                        Launcher.shareText(this@MainActivity, it.value)
                        ReviewRequester.onAction()
                    },
                    cameraPreview = { modifier ->
                        AndroidView(
                            factory = {
                                CameraPreviewView(this@MainActivity, ::onDetectCode).also { cameraView = it }
                            },
                            modifier = modifier,
                            update = { if (cameraEnabled) it.start() },
                            onRelease = {
                                it.release()
                                if (cameraView === it) cameraView = null
                            },
                        )
                    },
                )
            }
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService<Vibrator>()
        }
        viewModel.getResultStream().observe(this) { resultSet = it.toSet() }
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
        if (!cameraEnabled) {
            if (CameraPermission.hasPermission(this)) {
                startCamera()
            } else {
                finishByError()
                return
            }
        }
        ReviewRequester.requestIfNecessary(this)
    }

    override fun onResume() {
        super.onResume()
        Updater.onResume(this)
    }

    private fun finishByError() {
        toastPermissionError()
        super.finish()
    }

    private fun toastPermissionError() {
        Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show()
    }

    private fun onMenuAction(
        title: Int,
    ) {
        when (title) {
            R.string.options_menu_license -> LicenseActivity.start(this)
            R.string.options_menu_source_code -> Launcher.openSourceCode(this)
            R.string.options_menu_privacy_policy -> Launcher.openPrivacyPolicy(this)
            R.string.options_menu_share_this_app -> Launcher.shareThisApp(this)
            R.string.options_menu_play_store -> Launcher.openGooglePlay(this)
            R.string.options_menu_settings -> SettingsActivity.start(this)
        }
    }

    private fun startCamera() {
        cameraEnabled = true
    }

    private fun onDetectCode(
        codes: List<Barcode>,
    ): List<Barcode> {
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
        return detected
    }

    private fun vibrate() {
        if (!settings.vibrate) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_KEY = "CAMERA_PERMISSION_REQUEST_KEY"
    }
}
