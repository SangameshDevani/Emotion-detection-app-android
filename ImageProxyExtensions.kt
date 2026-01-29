package com.example.facedetection

import android.graphics.*
import android.graphics.ImageFormat
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/** Safer conversion from ImageProxy to upright Bitmap. Always attempts to close the proxy. */
fun ImageProxy.toBitmapSafe(): Bitmap {
    val image: Image? = this.image
    if (image == null) {
        try { this.close() } catch (_: Exception) {}
        throw IllegalStateException("ImageProxy.image is null")
    }

    try {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalStateException("Unsupported image format: ${image.format}")
        }

        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) {
            throw IllegalStateException("Invalid image dimensions: w=$width h=$height")
        }

        val nv21 = yuv420ToNv21Safe(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        val ok = yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        if (!ok) throw IllegalStateException("YuvImage.compressToJpeg returned false")

        val imageBytes = out.toByteArray()
        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("BitmapFactory returned null (bytes: ${imageBytes.size})")

        val rotated = rotateBitmap(bmp, this.imageInfo.rotationDegrees.toFloat())
        return rotated
    } finally {
        try { this.close() } catch (_: Exception) {}
    }
}

private fun yuv420ToNv21Safe(image: Image): ByteArray {
    val yPlane = image.planes.getOrNull(0) ?: throw IllegalStateException("Missing Y plane")
    val uPlane = image.planes.getOrNull(1) ?: throw IllegalStateException("Missing U plane")
    val vPlane = image.planes.getOrNull(2) ?: throw IllegalStateException("Missing V plane")

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    if (ySize == 0 || (uSize == 0 && vSize == 0)) {
        throw IllegalStateException("Unexpected plane sizes: y=$ySize u=$uSize v=$vSize")
    }

    val nv21 = ByteArray(ySize + uSize + vSize)
    try {
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
    } catch (e: Exception) {
        throw IllegalStateException("Buffer copy failed: ${e.message}", e)
    }
    return nv21
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    if (rotationDegrees == 0f) return bitmap
    val matrix = Matrix()
    matrix.postRotate(rotationDegrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
