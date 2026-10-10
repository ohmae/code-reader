/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.mm2d.codereader.R
import net.mm2d.codereader.ui.theme.AppTheme

@Composable
fun CameraPermissionDialog(
    onOpenAppInfo: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.dialog_title_permission)) },
        text = { Text(stringResource(R.string.dialog_message_camera_permission)) },
        confirmButton = {
            TextButton(onClick = onOpenAppInfo) { Text(stringResource(R.string.app_info)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@PreviewLightDark
@Composable
private fun CameraPermissionDialogPreview() {
    AppTheme { CameraPermissionDialog({}, {}) }
}
