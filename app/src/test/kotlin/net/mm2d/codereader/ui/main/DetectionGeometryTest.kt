package net.mm2d.codereader.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionGeometryTest {
    @Test
    fun quarterTurnsUseRotatedResolutionWithoutRotatingMlKitPointsAgain() {
        listOf(90, 270).forEach { rotation ->
            val transform = DetectionTransform(640, 480, rotation, 480f, 640f)
            assertEquals(DetectionPoint(120f, 160f), transform.map(DetectionPoint(120f, 160f)))
        }
    }

    @Test
    fun portraitCenterCropRemovesHorizontalOverflow() {
        val transform = DetectionTransform(640, 480, 0, 300f, 600f)
        assertEquals(DetectionPoint(150f, 300f), transform.map(DetectionPoint(320f, 240f)))
        assertEquals(DetectionPoint(-250f, 0f), transform.map(DetectionPoint(0f, 0f)))
    }

    @Test
    fun landscapeCenterCropRemovesVerticalOverflow() {
        val transform = DetectionTransform(640, 480, 90, 600f, 300f)
        assertEquals(DetectionPoint(300f, 150f), transform.map(DetectionPoint(240f, 320f)))
        assertEquals(DetectionPoint(0f, -250f), transform.map(DetectionPoint(0f, 0f)))
    }

    @Test
    fun animatedMarkerScalesAroundItsCenter() {
        val points = listOf(
            DetectionPoint(10f, 10f),
            DetectionPoint(30f, 10f),
            DetectionPoint(30f, 30f),
            DetectionPoint(10f, 30f),
        )
        assertEquals(DetectionPoint(-20f, -20f), scaleMarker(points, 4f).first())
        assertEquals(DetectionPoint(8f, 8f), scaleMarker(points, 1.2f).first())
        assertEquals(emptyList<DetectionPoint>(), scaleMarker(emptyList(), 4f))
    }
}
