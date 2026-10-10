/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.mm2d.codereader.R
import net.mm2d.codereader.result.ScanResult
import net.mm2d.codereader.ui.result.ScanResultContent
import net.mm2d.codereader.ui.theme.AppTheme

@Composable
fun MainScreen(
    results: List<ScanResult>,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onMenuAction: (Int) -> Unit,
    onOpen: (ScanResult) -> Unit,
    onCopy: (ScanResult) -> Unit,
    onShare: (ScanResult) -> Unit,
    cameraPreview: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    systemBars: WindowInsets = WindowInsets.systemBars,
) {
    val resultHeight = dimensionResource(R.dimen.result_height)
    val listHeight = dimensionResource(R.dimen.list_height)
    val visibleListHeight by animateDpAsState(
        targetValue = if (results.size >= 2) listHeight else resultHeight,
        animationSpec = tween(300),
        label = "resultListHeight",
    )
    Surface(modifier = modifier.fillMaxSize()) {
        // カメラは上部の system bars の背後まで表示し、左右と下部だけを一度適用する。
        Box(
            modifier = Modifier
                .windowInsetsPadding(systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .clipToBounds(),
        ) {
            // 一覧を拡張してもプレビュー・静止画・検出枠の領域は変えない。
            cameraPreview(Modifier.fillMaxSize().padding(bottom = resultHeight))
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, (listHeight - visibleListHeight).roundToPx()) }
                    .fillMaxWidth()
                    .height(listHeight)
                    .testTag("results"),
            ) {
                ScanResultContent(results, onOpen = onOpen, onCopy = onCopy, onShare = onShare)
            }
            if (results.isEmpty()) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).height(resultHeight).padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(stringResource(R.string.scanning), style = MaterialTheme.typography.bodyLarge)
                }
            }
            FilledIconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = visibleListHeight + 24.dp)
                    .size(56.dp)
                    .testTag("torch"),
            ) {
                Icon(
                    painterResource(if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off),
                    contentDescription = stringResource(
                        if (torchOn) R.string.action_turn_flash_off else R.string.action_turn_flash_on,
                    ),
                )
            }
            MainOptionsMenu(
                onMenuAction = onMenuAction,
                modifier = Modifier.align(Alignment.TopEnd)
                    .windowInsetsPadding(systemBars.only(WindowInsetsSides.Top)).padding(16.dp),
            )
        }
    }
}

@Composable
private fun MainOptionsMenu(
    onMenuAction: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FilledIconButton(onClick = { expanded = true }, modifier = Modifier.size(56.dp)) {
            Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.action_options_menu))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(dimensionResource(R.dimen.menu_width)),
        ) {
            listOf(
                R.string.options_menu_license,
                R.string.options_menu_source_code,
                R.string.options_menu_privacy_policy,
                R.string.options_menu_share_this_app,
                R.string.options_menu_play_store,
                R.string.options_menu_settings,
            ).forEach { title ->
                DropdownMenuItem(
                    text = { Text(stringResource(title)) },
                    onClick = {
                        expanded = false
                        onMenuAction(title)
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewMainScreen(
    @PreviewParameter(MainPreviewResultsProvider::class) results: List<ScanResult>,
) {
    AppTheme {
        MainScreen(
            results = results,
            torchOn = false,
            onToggleTorch = {},
            onMenuAction = {},
            onOpen = {},
            onCopy = {},
            onShare = {},
            cameraPreview = { Surface(modifier = it, color = MaterialTheme.colorScheme.surfaceVariant) {} },
        )
    }
}

class MainPreviewResultsProvider : PreviewParameterProvider<List<ScanResult>> {
    override val values: Sequence<List<ScanResult>> = sequenceOf(
        emptyList(),
        listOf(ScanResult("https://example.com", "URL", "QR code", true)),
        listOf(
            ScanResult("https://example.com", "URL", "QR code", true),
            ScanResult("Long value ".repeat(20), "Text", "QR code", false),
        ),
    )
}
