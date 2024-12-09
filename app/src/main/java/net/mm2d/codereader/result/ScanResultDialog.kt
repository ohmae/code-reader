/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.result

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import net.mm2d.codereader.R
import net.mm2d.codereader.databinding.DialogResultBinding
import net.mm2d.codereader.util.ClipboardUtils
import net.mm2d.codereader.util.Launcher
import net.mm2d.codereader.util.ReviewRequester
import net.mm2d.codereader.util.getParcelableSafely

class ScanResultDialog : DialogFragment() {
    override fun onCreateDialog(
        savedInstanceState: Bundle?,
    ): Dialog {
        val activity = requireActivity()
        val binding = DialogResultBinding.inflate(activity.layoutInflater)
        val result: ScanResult? = requireArguments().getParcelableSafely(KEY_SCAN_RESULT)
        result?.let {
            binding.resultValue.text = result.value
            binding.resultType.text = result.type
            binding.resultFormat.text = result.format
            binding.openButton.setText(R.string.action_open)
            binding.openButton.setOnClickListener {
                if (!Launcher.openUri(activity, result.value)) {
                    Launcher.search(activity, result.value)
                }
                ReviewRequester.onAction()
                dismiss()
            }
            binding.copyButton.setOnClickListener {
                ClipboardUtils.copyToClipboard(activity, result.type, result.value)
                ReviewRequester.onAction()
                dismiss()
            }
            binding.shareButton.setOnClickListener {
                Launcher.shareText(activity, result.value)
                ReviewRequester.onAction()
                dismiss()
            }
        }
        return AlertDialog.Builder(activity)
            .setTitle(R.string.dialog_title_select_action)
            .setView(binding.root)
            .create()
    }

    companion object {
        private const val TAG = "ScanResultDialog"
        private const val KEY_SCAN_RESULT = "KEY_SCAN_RESULT"

        fun show(
            activity: FragmentActivity,
            result: ScanResult,
        ) {
            val manager = activity.supportFragmentManager
            if (manager.isStateSaved) return
            if (manager.findFragmentByTag(TAG) != null) return
            ScanResultDialog().also {
                it.arguments = bundleOf(KEY_SCAN_RESULT to result)
            }.show(manager, TAG)
        }
    }
}
