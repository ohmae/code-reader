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
    private var animator: ValueAnimator? = null
    private val resumeRunnable = Runnable {
        detectedMarker.clearMarker()
        stillImage.setImageBitmap(null)
        stillImage.isVisible = false
        codeScanner.resume()
    }

    fun onDetected(
        imageProxy: ImageProxy,
        detectedCodes: List<Barcode>,
    ) {
        codeScanner.pause()
        animator?.cancel()
        detectedMarker.removeCallbacks(resumeRunnable)

        val pointsList = detectedCodes.mapNotNull { it.toCornerPoints() }
        detectedMarker.setMarkers(imageProxy, pointsList)
        stillImage.setImageBitmap(toBitmap(imageProxy))
        stillImage.isVisible = true
        val animator = ValueAnimator.ofFloat(4f, 1.2f)
        animator.duration = ANIMATION_DURATION
        animator.interpolator = DecelerateInterpolator(3f)
        animator.addUpdateListener {
            detectedMarker.drawMarker(it.animatedValue as Float)
        }
        animator.addListener(onEnd = { onEnd() })
        animator.start()
        this.animator = animator
    }

    private fun Barcode.toCornerPoints(): Array<Point>? {
        val cornerPoints = cornerPoints ?: return null
        if (cornerPoints.isEmpty()) return null
        return cornerPoints
    }

    private fun onEnd() {
        detectedMarker.removeCallbacks(resumeRunnable)
        detectedMarker.postDelayed(resumeRunnable, RESUME_INTERVAL)
    }

    fun destroy() {
        animator?.cancel()
        animator = null
        detectedMarker.removeCallbacks(resumeRunnable)
        detectedMarker.clearMarker()
        stillImage.setImageBitmap(null)
        stillImage.isVisible = false
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
            try {
                Bitmap.createBitmap(temp, 0, 0, temp.width, temp.height, matrix, true)
            } finally {
                temp.recycle()
            }
        }

    companion object {
        private const val ANIMATION_DURATION = 1000L
        private const val RESUME_INTERVAL = 500L
    }
}
