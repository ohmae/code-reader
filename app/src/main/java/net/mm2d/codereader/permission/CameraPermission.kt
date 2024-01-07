/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

object CameraPermission {
    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
}

fun ComponentActivity.registerForCameraPermissionRequest(
    callback: (granted: Boolean, succeedToShowDialog: Boolean) -> Unit,
): PermissionRequestLauncher = registerForPermissionRequest(Manifest.permission.CAMERA, callback)
