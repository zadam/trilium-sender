package io.github.zadam.triliumsender.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min

class ImageConverter {
    fun scaleImage(inputStream: InputStream, mimeType: String?): InputStream {
        // we won't do anything with GIFs, PNGs etc. This is minority use case anyway
        if (mimeType != "image/jpeg") {
            return inputStream
        }

        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

        val maxWidth = 2000
        val maxHeight = 2000
        val width = bitmap!!.width
        val height = bitmap.height
        val scale = min(maxHeight.toFloat() / width, maxWidth.toFloat() / height)

        val newWidth: Int = if (scale < 1) (width * scale).toInt() else width
        val newHeight: Int = if (scale < 1) (height * scale).toInt() else height

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bitmapData = outputStream.toByteArray()

        return ByteArrayInputStream(bitmapData)

    }
}
