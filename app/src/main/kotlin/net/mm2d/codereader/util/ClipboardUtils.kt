/*
 * Copyright (c) 2021 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService
import net.mm2d.codereader.R

object ClipboardUtils {
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
    ) {
        context.getSystemService<ClipboardManager>()?.let {
            it.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, R.string.toast_copy_to_clipboard, Toast.LENGTH_SHORT)
                .show()
        }
    }
}
