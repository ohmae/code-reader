/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.mm2d.codereader.result.ScanResult

@Composable
fun ScanResultContent(
    results: List<ScanResult>,
    onOpen: (ScanResult) -> Unit,
    onCopy: (ScanResult) -> Unit,
    onShare: (ScanResult) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<ScanResult?>(null) }
    fun completeAction(
        action: (ScanResult) -> Unit,
    ) {
        val result = selected ?: return
        // 外部起動前に閉じ、連打や画面復元による操作の再実行を防ぐ。
        selected = null
        action(result)
    }
    ScanResultList(results = results, onSelect = { if (selected == null) selected = it })
    selected?.let { result ->
        ScanResultDialog(
            result = result,
            onDismiss = { selected = null },
            onOpen = { completeAction(onOpen) },
            onCopy = { completeAction(onCopy) },
            onShare = { completeAction(onShare) },
        )
    }
}
