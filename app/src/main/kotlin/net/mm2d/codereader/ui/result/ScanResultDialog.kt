/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import net.mm2d.codereader.R
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.ui.theme.AppTheme

@Composable
fun ScanResultDialog(
    result: ScanResult,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_select_action)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "${stringResource(R.string.label_type)} ${result.type}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "${stringResource(R.string.label_format)} ${result.format}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(text = result.value, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_open))
                }
                TextButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_share))
                }
                TextButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_copy))
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun PreviewScanResultDialog() {
    AppTheme {
        ScanResultDialog(
            result = ScanResult("https://example.com/".repeat(30), "URL", "QR code", true),
            onDismiss = {},
            onOpen = {},
            onCopy = {},
            onShare = {},
        )
    }
}
