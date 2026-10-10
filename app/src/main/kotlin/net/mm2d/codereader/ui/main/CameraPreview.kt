/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CameraPreview(
    state: CameraPreviewState,
    modifier: Modifier = Modifier,
) {
    val surfaceRequest by state.codeScanner.surfaceRequest.collectAsStateWithLifecycle()
    Box(modifier) {
        surfaceRequest?.let {
            CameraXViewfinder(
                surfaceRequest = it,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        state.detection.frame?.let { frame ->
            DetectionOverlay(frame, state.detection::finish, Modifier.fillMaxSize())
        }
    }
}
