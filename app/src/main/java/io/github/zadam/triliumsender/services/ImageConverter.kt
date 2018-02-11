package io.github.zadam.triliumsender.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImageConverter {
    fun scaleImage(inputStream: InputStream, mimeType: String): InputStream {
        // we won't do anything with GIFs, PNGs etc. This is minority use case anyway
        if (mimeType != "image/jpeg") {
            return inputStream;
        }

        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

        val maxWidth = 2000
        val maxHeight = 2000

        val scale = Math.min(maxHeight.toFloat() / bitmap.width, maxWidth.toFloat() / bitmap.height)

        val newWidth: Int = if (scale < 1) (bitmap.width * scale).toInt() else bitmap.width;
        val newHeight: Int = if (scale < 1) (bitmap.height * scale).toInt() else bitmap.height;

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        val bitmapdata = baos.toByteArray()

        return ByteArrayInputStream(bitmapdata)
    }
}