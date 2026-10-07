/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.license

import android.content.Context
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.dropUnlessResumed
import net.mm2d.codereader.R
import net.mm2d.codereader.ui.theme.AppTheme
import net.mm2d.codereader.util.Launcher

@Composable
fun LicenseScreen(
    onBack: () -> Unit,
) {
    LicenseScreenContent(
        onBack = dropUnlessResumed { onBack() },
    ) { modifier ->
        var webViewKey by remember { mutableIntStateOf(0) }
        key(webViewKey) {
            AndroidView(
                modifier = modifier,
                factory = { context ->
                    createWebView(context) { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                        webViewKey++
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicenseScreenContent(
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.options_menu_license)) },
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
        content(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

private fun createWebView(
    context: Context,
    onRenderProcessGone: (WebView) -> Unit,
): WebView {
    val view = WebView(context)
    view.settings.setSupportZoom(false)
    view.settings.displayZoomControls = false
    view.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean = request.isForMainFrame && Launcher.openCustomTabs(context, request.url)

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail,
        ): Boolean {
            onRenderProcessGone(view)
            return true
        }
    }
    view.loadUrl("file:///android_asset/license.html")
    view.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    return view
}

@PreviewLightDark
@Composable
private fun PreviewLicenseScreen() {
    AppTheme {
        LicenseScreenContent(onBack = {}) { Box(it) }
    }
}
