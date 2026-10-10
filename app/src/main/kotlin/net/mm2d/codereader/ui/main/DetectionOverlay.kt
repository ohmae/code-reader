/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.delay
import net.mm2d.codereader.R
import net.mm2d.codereader.ui.theme.AppTheme
import kotlin.math.pow

@Composable
fun DetectionOverlay(
    frame: DetectedFrame,
    onFinished: (DetectedFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember(frame) { Animatable(4f) }
    val finish = rememberUpdatedState(onFinished)
    LaunchedEffect(frame) {
        try {
            scale.animateTo(1.2f, tween(1000, easing = Easing { 1f - (1f - it).pow(6) }))
            delay(500)
        } finally {
            finish.value(frame)
        }
    }
    val baseColor = MaterialTheme.colorScheme.onSecondary
    val accentColor = MaterialTheme.colorScheme.secondary
    val baseStroke = dimensionResource(R.dimen.frame_base_stroke_width)
    val accentStroke = dimensionResource(R.dimen.frame_stroke_width)
    val image = remember(frame) { frame.bitmap.asImageBitmap() }
    Box(modifier.clipToBounds().testTag("detection")) {
        Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Canvas(Modifier.fillMaxSize()) {
            val transform =
                DetectionTransform(frame.width, frame.height, frame.rotationDegrees, size.width, size.height)
            frame.corners.forEach { corners ->
                val points = scaleMarker(corners.map(transform::map), scale.value)
                if (points.isEmpty()) return@forEach
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, baseColor, style = Stroke(baseStroke.toPx()))
                drawPath(path, accentColor, style = Stroke(accentStroke.toPx()))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DetectionOverlayPreview() {
    val frame = remember {
        DetectedFrame(
            createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff808080.toInt()) },
            listOf(
                listOf(
                    DetectionPoint(100f, 150f),
                    DetectionPoint(200f, 150f),
                    DetectionPoint(200f, 250f),
                    DetectionPoint(100f, 250f),
                ),
            ),
            300,
            400,
            0,
        )
    }
    AppTheme { DetectionOverlay(frame, {}, Modifier.size(300.dp, 400.dp)) }
}
