/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.common.Barcode

class DetectedFrame(
    val bitmap: Bitmap,
    val corners: List<List<DetectionPoint>>,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
) {
    companion object {
        fun capture(
            image: ImageProxy,
            codes: List<Barcode>,
        ): DetectedFrame {
            val rotation = image.imageInfo.rotationDegrees
            val corners = codes.mapNotNull { code ->
                code.cornerPoints?.takeIf { it.isNotEmpty() }?.map { DetectionPoint(it.x.toFloat(), it.y.toFloat()) }
            }
            val original = image.toBitmap()
            val bitmap = if (rotation == 0) {
                original
            } else {
                try {
                    Bitmap.createBitmap(
                        original,
                        0,
                        0,
                        original.width,
                        original.height,
                        Matrix().apply { postRotate(rotation.toFloat()) },
                        true,
                    )
                } finally {
                    original.recycle()
                }
            }
            return DetectedFrame(bitmap, corners, image.width, image.height, rotation)
        }
    }
}
