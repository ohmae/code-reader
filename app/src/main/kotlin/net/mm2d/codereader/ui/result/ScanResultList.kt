/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.mm2d.codereader.R
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.ui.theme.AppTheme

@Composable
fun ScanResultList(
    results: List<ScanResult>,
    onSelect: (ScanResult) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    var previousSize by rememberSaveable { mutableIntStateOf(results.size) }
    LaunchedEffect(results.size) {
        val added = results.size > previousSize
        previousSize = results.size
        if (added) listState.scrollToItem(results.lastIndex)
    }
    Surface(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            // Parcelable の ScanResult 全体をキーにし、同じ値でも種別・形式の異なる結果を区別する。
            itemsIndexed(results, key = { _, result -> result }) { index, result ->
                ScanResultRow(result = result, onClick = { onSelect(result) })
                if (index < results.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ScanResultRow(
    result: ScanResult,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.result_height))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.label_type),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = result.type,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp).widthIn(min = 72.dp),
            )
            Text(
                text = stringResource(R.string.label_format),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp),
            )
            Text(
                text = result.format,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
        }
        Text(
            text = result.value,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewScanResultList() {
    AppTheme {
        ScanResultList(
            results = listOf(
                ScanResult("https://example.com", "URL", "QR code", true),
                ScanResult("Long value ".repeat(20), "Text", "QR code", false),
            ),
            onSelect = {},
        )
    }
}

@Preview
@Composable
private fun PreviewEmptyScanResultList() {
    AppTheme { ScanResultList(results = emptyList(), onSelect = {}) }
}

@Preview
@Composable
private fun PreviewSingleScanResultList() {
    AppTheme {
        ScanResultList(results = listOf(ScanResult("123456789", "Text", "QR code", false)), onSelect = {})
    }
}
