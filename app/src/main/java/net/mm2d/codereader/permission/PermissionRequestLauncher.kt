package net.mm2d.codereader.permission

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat

interface PermissionRequestLauncher {
    fun launch(activity: Activity)
}

fun ComponentActivity.registerForPermissionRequest(
    permission: String,
    callback: (granted: Boolean, succeedToShowDialog: Boolean) -> Unit,
): PermissionRequestLauncher = PermissionRequestLauncherImpl(permission).also { launcher ->
    launcher.launcher = registerForActivityResult(RequestPermission()) { granted ->
        callback(granted, launcher.succeedToShowDialog(this))
    }
}

class PermissionRequestLauncherImpl(
    private val permission: String,
) : PermissionRequestLauncher {
    lateinit var launcher: ActivityResultLauncher<String>
    private var shouldShowRationalBefore: Boolean = false
    private var start: Long = 0L

    fun succeedToShowDialog(activity: Activity): Boolean {
        if (shouldShowRationalBefore) return true
        if (System.currentTimeMillis() - start > ENOUGH_DURATION) return true
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    override fun launch(activity: Activity) {
        start = System.currentTimeMillis()
        shouldShowRationalBefore = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        launcher.launch(permission)
    }

    companion object {
        private const val ENOUGH_DURATION = 1000L
    }
}
