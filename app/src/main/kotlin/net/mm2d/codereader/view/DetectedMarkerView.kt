package net.mm2d.codereader.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import net.mm2d.codereader.R
import net.mm2d.codereader.extension.resolveColor
import com.google.android.material.R as MR

class DetectedMarkerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val basePaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = context.resolveColor(MR.attr.colorOnSecondary)
        strokeWidth = context.resources.getDimension(R.dimen.frame_base_stroke_width)
    }
    private val accentPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = context.resolveColor(MR.attr.colorSecondary)
        strokeWidth = context.resources.getDimension(R.dimen.frame_stroke_width)
    }
    private val matrix = Matrix()
    private val markers: MutableList<Marker> = mutableListOf()
    private val drawPaths: MutableList<Path> = mutableListOf()

    fun setMarkers(
        imageProxy: ImageProxy,
        pointsList: List<Array<Point>>,
    ) {
        val (rw, rh) = normalizeResolution(imageProxy)
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = maxOf(w / rw, h / rh)
        val offset = PointF((rw * scale - w) / 2f, (rh * scale - h) / 2f)

        pointsList
            .map { it.map { PointF(it.x * scale - offset.x, it.y * scale - offset.y) } }
            .forEach { points ->
                val center = PointF(
                    points.fold(0f) { acc, point -> acc + point.x } / points.size,
                    points.fold(0f) { acc, point -> acc + point.y } / points.size,
                )
                val path = Path()
                points.first().let {
                    path.moveTo(it.x, it.y)
                }
                points.drop(1).forEach {
                    path.lineTo(it.x, it.y)
                }
                path.close()
                markers.add(Marker(center, path))
            }
    }

    private fun normalizeResolution(
        imageProxy: ImageProxy,
    ): Pair<Float, Float> {
        val w = imageProxy.width.toFloat()
        val h = imageProxy.height.toFloat()
        return when (imageProxy.imageInfo.rotationDegrees) {
            90, 270 -> h to w
            else -> w to h
        }
    }

    fun clearMarker() {
        markers.clear()
        drawPaths.clear()
        invalidate()
    }

    fun drawMarker(
        scale: Float,
    ) {
        drawPaths.clear()
        markers.forEach {
            drawPaths.add(transformPath(scale, it))
        }
        invalidate()
    }

    private fun transformPath(
        scale: Float,
        marker: Marker,
    ): Path =
        Path(marker.path).also {
            matrix.setScale(scale, scale, marker.center.x, marker.center.y)
            it.transform(matrix)
        }

    override fun onDraw(
        canvas: Canvas,
    ) {
        drawPaths.forEach {
            canvas.drawPath(it, basePaint)
            canvas.drawPath(it, accentPaint)
        }
    }

    private data class Marker(
        val center: PointF,
        val path: Path,
    )
}
