/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.permission

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import net.mm2d.codereader.R

class PermissionDialog : DialogFragment() {
    override fun onCreateDialog(
        savedInstanceState: Bundle?,
    ): Dialog {
        val context = requireContext()
        return AlertDialog.Builder(context)
            .setTitle(R.string.dialog_title_permission)
            .setMessage(R.string.dialog_message_camera_permission)
            .setPositiveButton(R.string.app_info) { _, _ ->
                startAppInfo(context)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()
    }

    override fun onCancel(
        dialog: DialogInterface,
    ) {
        val requestKey = requireArguments().getString(REQUEST_KEY, "")
        parentFragmentManager.setFragmentResult(requestKey, Bundle())
    }

    private fun startAppInfo(
        context: Context,
    ) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = ("package:" + context.packageName).toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "PermissionDialog"
        private const val REQUEST_KEY = "REQUEST_KEY"

        fun registerListener(
            activity: FragmentActivity,
            requestKey: String,
            onCancel: () -> Unit,
        ) {
            val manager = activity.supportFragmentManager
            manager.setFragmentResultListener(requestKey, activity) { _, _ ->
                onCancel()
            }
        }

        fun show(
            activity: FragmentActivity,
            requestKey: String,
        ) {
            val manager = activity.supportFragmentManager
            if (manager.isStateSaved) return
            if (manager.findFragmentByTag(TAG) != null) return
            PermissionDialog().also {
                it.arguments = bundleOf(REQUEST_KEY to requestKey)
            }.show(manager, TAG)
        }
    }
}
