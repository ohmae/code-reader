package net.mm2d.codereader

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import com.google.mlkit.vision.barcode.common.Barcode
import net.mm2d.codereader.code.CodeScanner
import net.mm2d.codereader.view.DetectedMarkerView

class DetectedPresenter(
    private val codeScanner: CodeScanner,
    private val detectedMarker: DetectedMarkerView,
    private val stillImage: ImageView,
) {
    fun onDetected(
        imageProxy: ImageProxy,
        detectedCodes: List<Barcode>,
    ) {
        codeScanner.pause()
        val pointsList = detectedCodes.mapNotNull { it.toCornerPoints() }
        detectedMarker.setMarkers(imageProxy, pointsList)
        stillImage.setImageBitmap(toBitmap(imageProxy))
        stillImage.isVisible = true
        ValueAnimator.ofFloat(4f, 1.2f)
            .also {
                it.setDuration(ANIMATION_DURATION)
                it.setInterpolator(DecelerateInterpolator(3f))
                it.addUpdateListener {
                    detectedMarker.drawMarker(it.animatedValue as Float)
                }
                it.addListener(onEnd = { onEnd() })
            }.start()
    }

    private fun Barcode.toCornerPoints(): Array<Point>? {
        val cornerPoints = cornerPoints ?: return null
        if (cornerPoints.isEmpty()) return null
        return cornerPoints
    }

    private fun onEnd() {
        detectedMarker.postDelayed({
            detectedMarker.clearMarker()
            stillImage.setImageBitmap(null)
            stillImage.isVisible = false
            codeScanner.resume()
        }, RESUME_INTERVAL)
    }

    private fun toBitmap(
        imageProxy: ImageProxy,
    ): Bitmap =
        if (imageProxy.imageInfo.rotationDegrees == 0) {
            imageProxy.toBitmap()
        } else {
            val temp = imageProxy.toBitmap()
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }
            Bitmap.createBitmap(temp, 0, 0, temp.width, temp.height, matrix, true)
        }

    companion object {
        private const val ANIMATION_DURATION = 1000L
        private const val RESUME_INTERVAL = 500L
    }
}
