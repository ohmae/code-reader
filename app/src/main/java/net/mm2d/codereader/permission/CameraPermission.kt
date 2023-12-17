/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object CameraPermission {
    private const val PERMISSION = Manifest.permission.CAMERA
    private const val ACTION = RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS
    private const val EXTRA_REQUEST = RequestMultiplePermissions.EXTRA_PERMISSIONS
    private const val EXTRA_RESULT = RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS

    class RequestContract : ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(ACTION).putExtra(EXTRA_REQUEST, arrayOf(PERMISSION))

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            if (resultCode != AppCompatActivity.RESULT_OK) return false
            return intent
                ?.getIntArrayExtra(EXTRA_RESULT)
                ?.getOrNull(0) == PackageManager.PERMISSION_GRANTED
        }

        override fun getSynchronousResult(
            context: Context,
            input: Unit,
        ): SynchronousResult<Boolean>? =
            if (hasPermission(context)) {
                SynchronousResult(true)
            } else {
                null
            }
    }

    fun deniedWithoutShowDialog(activity: Activity): Boolean =
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
}
