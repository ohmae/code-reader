/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.settings

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.dropUnlessResumed
import net.mm2d.codereader.BuildConfig
import net.mm2d.codereader.R
import net.mm2d.codereader.setting.Settings
import net.mm2d.codereader.ui.theme.AppTheme
import net.mm2d.codereader.util.ClipboardUtils

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val settings = Settings.get()
    var vibrate by remember(settings) { mutableStateOf(settings.vibrate) }
    LifecycleStartEffect(settings) {
        vibrate = settings.vibrate
        onStopOrDispose {}
    }
    val context = LocalContext.current
    SettingsScreenContent(
        vibrate = vibrate,
        onVibrateChange = {
            settings.vibrate = it
            vibrate = it
        },
        onCopySummary = { label, summary -> ClipboardUtils.copyToClipboard(context, label, summary) },
        onBack = dropUnlessResumed { onBack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    onCopySummary: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.options_menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { CategoryTitle(stringResource(R.string.preference_category_settings)) }
            item {
                val title = stringResource(R.string.preference_title_vibration)
                val summary = stringResource(
                    if (vibrate) {
                        R.string.preference_summary_vibration_on
                    } else {
                        R.string.preference_summary_vibration_off
                    },
                )
                SettingsItem(
                    title = title,
                    summary = summary,
                    onClick = { onVibrateChange(!vibrate) },
                    onCopy = { onCopySummary(title, summary) },
                    role = Role.Switch,
                    modifier = Modifier.semantics { toggleableState = ToggleableState(vibrate) },
                    trailingContent = { Switch(checked = vibrate, onCheckedChange = null) },
                )
            }
            item { CategoryTitle(stringResource(R.string.preference_category_information)) }
            item {
                val title = stringResource(R.string.preference_title_version)
                SettingsItem(
                    title = title,
                    summary = BuildConfig.VERSION_NAME,
                    onClick = {},
                    onCopy = { onCopySummary(title, BuildConfig.VERSION_NAME) },
                )
            }
        }
    }
}

@Composable
private fun CategoryTitle(
    title: String,
) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .semantics { heading() }
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsItem(
    title: String,
    summary: String,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role? = null,
    trailingContent: @Composable () -> Unit = {},
) {
    var showCopyMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    role = role,
                    onClick = onClick,
                    onLongClickLabel = stringResource(R.string.action_copy),
                    onLongClick = { showCopyMenu = true },
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            trailingContent()
        }
        DropdownMenu(
            expanded = showCopyMenu,
            onDismissRequest = { showCopyMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy)) },
                onClick = {
                    showCopyMenu = false
                    onCopy()
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewSettingsScreen() {
    AppTheme {
        SettingsScreenContent(vibrate = true, onVibrateChange = {}, onCopySummary = { _, _ -> }, onBack = {})
    }
}
