package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageEncodingUtils {

    fun bitmapToCompactBase64(
        bitmap: Bitmap,
        maxDimension: Int = 180,
        quality: Int = 68
    ): String {
        val longestSide = max(bitmap.width, bitmap.height)
        val scaledBitmap = if (longestSide > maxDimension && longestSide > 0) {
            val scale = maxDimension.toFloat() / longestSide.toFloat()
            val targetW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun uriToScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512
    ): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            var sampleSize = 1
            val longest = max(boundsOptions.outWidth, boundsOptions.outHeight)
            while (longest / sampleSize > maxDimension * 2 && sampleSize < 32) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val longestDecoded = max(decoded.width, decoded.height)
            if (longestDecoded > maxDimension && longestDecoded > 0) {
                val scale = maxDimension.toFloat() / longestDecoded.toFloat()
                val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }
        } catch (_: Exception) {
            null
        }
    }

    fun uriToCompactBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 180,
        quality: Int = 68
    ): String? {
        val decoded = uriToScaledBitmap(context, uri, maxDimension) ?: return null
        val encoded = bitmapToCompactBase64(decoded, maxDimension, quality)
        decoded.recycle()
        return encoded
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
