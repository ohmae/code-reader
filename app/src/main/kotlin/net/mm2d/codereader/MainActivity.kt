/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.content.Intent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.extension.formatString
import net.mm2d.codereader.extension.typeString
import net.mm2d.codereader.permission.CameraPermission
import net.mm2d.codereader.permission.registerForCameraPermissionRequest
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.setting.Settings
import net.mm2d.codereader.ui.main.CameraPermissionDialog
import net.mm2d.codereader.ui.main.CameraPreview
import net.mm2d.codereader.ui.main.CameraPreviewState
import net.mm2d.codereader.ui.main.MainScreen
import net.mm2d.codereader.ui.theme.AppTheme
import net.mm2d.codereader.util.ClipboardUtils
import net.mm2d.codereader.util.Launcher
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.Updater
import net.mm2d.codereader.util.observe
import android.provider.Settings as AndroidSettings

class MainActivity : AppCompatActivity() {
    private var cameraEnabled by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionRequestPending = false
    private val launcher = registerForCameraPermissionRequest { granted, succeedToShowDialog ->
        permissionRequestPending = false
        if (granted) {
            startCamera()
        } else if (!succeedToShowDialog) {
            showPermissionDialog = true
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
        showPermissionDialog = savedInstanceState?.getBoolean(SHOW_PERMISSION_DIALOG) ?: false
        permissionRequestPending = savedInstanceState?.getBoolean(PERMISSION_REQUEST_PENDING) ?: false
        enableEdgeToEdge()
        setContent {
            val camera = remember { CameraPreviewState(this@MainActivity, ::onDetectCode) }
            DisposableEffect(camera) {
                camera.initialize()
                onDispose { camera.release() }
            }
            LaunchedEffect(camera, cameraEnabled) {
                if (cameraEnabled) camera.start()
            }
            val torchOn by camera.codeScanner.getTorchStateStream()
                .collectAsStateWithLifecycle(initialValue = false)
            val results by viewModel.getResultStream().collectAsStateWithLifecycle()
            AppTheme {
                MainScreen(
                    results = results,
                    torchOn = torchOn,
                    onToggleTorch = { camera.codeScanner.toggleTorch() },
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
                        CameraPreview(camera, modifier)
                    },
                )
                if (showPermissionDialog) {
                    CameraPermissionDialog(
                        onOpenAppInfo = {
                            showPermissionDialog = false
                            startActivity(
                                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:$packageName".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        onCancel = {
                            showPermissionDialog = false
                            finishByError()
                        },
                    )
                }
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
        } else if (!showPermissionDialog && !permissionRequestPending) {
            permissionRequestPending = true
            launcher.launch()
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (CameraPermission.hasPermission(this)) {
            startCamera()
        } else {
            cameraEnabled = false
            finishByError()
            return
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
        showPermissionDialog = false
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

    override fun onSaveInstanceState(
        outState: Bundle,
    ) {
        outState.putBoolean(SHOW_PERMISSION_DIALOG, showPermissionDialog)
        outState.putBoolean(PERMISSION_REQUEST_PENDING, permissionRequestPending)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val SHOW_PERMISSION_DIALOG = "SHOW_PERMISSION_DIALOG"
        private const val PERMISSION_REQUEST_PENDING = "PERMISSION_REQUEST_PENDING"
    }
}
