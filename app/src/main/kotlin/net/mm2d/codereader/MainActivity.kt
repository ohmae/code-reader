/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader

import android.animation.ValueAnimator
import android.content.Intent // Mushroom mode 用に追加
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ViewGroup.MarginLayoutParams
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
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.Updater
import net.mm2d.codereader.util.observe

// Mushroom mode 用に追加
const val ACTION_INTERCEPT_MAIN = "com.adamrocker.android.simeji.ACTION_INTERCEPT" // 定数名変更
const val REPLACE_KEY_MAIN = "replace_key" // 定数名変更

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var codeScanner: CodeScanner
    private val launcher = registerForCameraPermissionRequest { granted, succeedToShowDialog ->
        if (granted) {
            // パーミッションが付与されたら onResume でカメラが開始される
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
    private var isMushroomMode: Boolean = false // Mushroom mode 用フラグ

    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Mushroom mode の判定 (intent.action を確認)
        isMushroomMode = intent.action?.contains(ACTION_INTERCEPT_MAIN) ?: false

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.guideTop.updateLayoutParams<MarginLayoutParams> {
                topMargin = systemBars.top
            }
            insets
        }
        adapter = ScanResultAdapter(this) {
            ScanResultDialog.show(this, it)
        }
        binding.resultList.adapter = adapter
        binding.resultList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL),
        )
        vibrator = getSystemService()!!
        codeScanner = CodeScanner(this, binding.previewView, ::onDetectCode) // CodeScanner はここで初期化
        binding.flash.setOnClickListener {
            codeScanner.toggleTorch()
        }
        codeScanner.getTouchStateStream().observe(this) { // メソッド名を getTouchStateStream に戻す
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
        PermissionDialog.registerListener(this, CAMERA_PERMISSION_REQUEST_KEY) {
            finishByError()
        }
        OptionsMenuPresenter(this, binding.menu).setUp()
        Updater.startIfAvailable(this) // Updaterはパーミッション状態に関わらず呼べるならここに
    }

    // onStart を追加
    override fun onStart() {
        super.onStart()
        if (!CameraPermission.hasPermission(this)) {
            launcher.launch()
        }
        // パーミッションがある場合は onResume でカメラが開始される
    }

    // onResume を追加/修正
    override fun onResume() {
        super.onResume()
        if (CameraPermission.hasPermission(this)) {
            if (::codeScanner.isInitialized) { // codeScannerが初期化済みか確認
                codeScanner.start()  // カメラの初期化/再初期化
                codeScanner.resume() // 解析の再開
            }
        }
    }

    // onPause を追加
    override fun onPause() {
        super.onPause()
        if (::codeScanner.isInitialized) { // codeScannerが初期化済みか確認
            codeScanner.pause()          // 解析の一時停止
            codeScanner.shutdownCamera() // カメラリソースの解放
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 以前の onRestart のロジックは onStart と onResume でカバーされるため、
        // ここでの特別なカメラ処理は不要になる。
    }

    // onDestroy を追加 (任意、デバッグや最終確認用)
    override fun onDestroy() {
        // onPause で shutdownCamera が呼ばれるため、通常は不要。
        super.onDestroy()
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

    private fun onDetectCode(
        imageProxy: ImageProxy,
        codes: List<Barcode>,
    ) {
        val detected = mutableListOf<Barcode>()
        codes.forEach { // it を使用
            val value = it.rawValue ?: return@forEach
            val result = ScanResult(
                value = value,
                type = it.typeString(),
                format = it.formatString(),
                isUrl = it.valueType == Barcode.TYPE_URL,
            )
            if (!resultSet.contains(result)) {
                if (isMushroomMode) { // Mushroom mode の処理を先に行う
                    val data = Intent()
                    data.putExtra(REPLACE_KEY_MAIN, result.value)
                    setResult(RESULT_OK, data)
                    finish()
                    return // Mushroomモードでは最初の検出でActivityを終了
                }
                viewModel.add(result)
                vibrate()
                detected.add(it)
            }
        }
        if (detected.isEmpty()) {
            // imageProxy.close() は CodeAnalyzer 内で自動的に行われるはずなので、
            // ここでの呼び出しは不要。
            return
        }
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

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_KEY = "CAMERA_PERMISSION_REQUEST_KEY"
    }
}
