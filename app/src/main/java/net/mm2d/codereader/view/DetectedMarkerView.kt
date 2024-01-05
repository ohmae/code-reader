package net.mm2d.codereader.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Size
import android.view.View
import androidx.camera.core.ResolutionInfo
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
    private val drawPaths: MutableList<Path> = mutableListOf<Path>()

    fun setMarkers(resolutionInfo: ResolutionInfo, pointsList: List<Array<Point>>) {
        val aspect = if (width > height) PointF(16f, 9f) else PointF(9f, 16f)
        val offset: PointF
        val ratio: Float
        val resolution = normalizeResolution(resolutionInfo)
        if (width * aspect.y > height * aspect.x) {
            ratio = width / resolution.width.toFloat()
            offset = PointF(0f, (width * aspect.y / aspect.x - height) / 2)
        } else {
            ratio = height / resolution.height.toFloat()
            offset = PointF((height * aspect.x / aspect.y - width) / 2, 0f)
        }

        pointsList
            .map { it.map { PointF(it.x * ratio - offset.x, it.y * ratio - offset.y) } }
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

    private fun normalizeResolution(info: ResolutionInfo): Size =
        when (info.rotationDegrees) {
            90, 270 -> Size(info.resolution.height, info.resolution.width)
            else -> info.resolution
        }

    fun clearMarker() {
        markers.clear()
        drawPaths.clear()
        invalidate()
    }

    fun drawMarker(scale: Float) {
        drawPaths.clear()
        markers.forEach {
            drawPaths.add(transformPath(scale, it))
        }
        invalidate()
    }

    private fun transformPath(scale: Float, marker: Marker): Path =
        Path(marker.path).also {
            matrix.setScale(scale, scale, marker.center.x, marker.center.y)
            it.transform(matrix)
        }

    override fun onDraw(canvas: Canvas) {
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
