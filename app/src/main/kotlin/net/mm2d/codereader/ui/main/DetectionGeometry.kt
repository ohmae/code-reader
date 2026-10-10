/*
 * Copyright (c) 2026 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.codereader.ui.main

data class DetectionPoint(
    val x: Float,
    val y: Float,
)

class DetectionTransform(
    frameWidth: Int,
    frameHeight: Int,
    rotationDegrees: Int,
    viewWidth: Float,
    viewHeight: Float,
) {
    private val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) frameHeight else frameWidth
    private val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) frameWidth else frameHeight
    private val scale = maxOf(viewWidth / rotatedWidth, viewHeight / rotatedHeight)
    private val offsetX = (rotatedWidth * scale - viewWidth) / 2f
    private val offsetY = (rotatedHeight * scale - viewHeight) / 2f

    // ML Kit の角点は回転補正後の座標なので、ここでは center crop だけを適用する。
    fun map(
        point: DetectionPoint,
    ): DetectionPoint = DetectionPoint(point.x * scale - offsetX, point.y * scale - offsetY)
}

fun scaleMarker(
    points: List<DetectionPoint>,
    scale: Float,
): List<DetectionPoint> {
    if (points.isEmpty()) return emptyList()
    val centerX = points.sumOf { it.x.toDouble() }.toFloat() / points.size
    val centerY = points.sumOf { it.y.toDouble() }.toFloat() / points.size
    return points.map { DetectionPoint(centerX + (it.x - centerX) * scale, centerY + (it.y - centerY) * scale) }
}
