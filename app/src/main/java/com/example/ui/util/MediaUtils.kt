package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object MediaUtils {

    val FILTERS = listOf("Normal", "Warm", "Cool", "Black & White", "HDR", "Blur Background")

    fun uriToCompressedBase64(context: Context, uri: Uri, maxDim: Int = 520, quality: Int = 65): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null
            bitmapToBase64(original, maxDim, quality)
        } catch (_: Exception) {
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, maxDim: Int = 520, quality: Int = 65): String {
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val bytes = baos.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeBase64ToImageBitmap(dataUri: String): ImageBitmap? {
        return try {
            if (!dataUri.startsWith("data:image")) return null
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun colorFilterFor(filterName: String): ColorFilter? {
        return when (filterName) {
            "Warm" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 8f,
                        0f, 0f, 0.85f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Cool" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0f, 0f, 0f,
                        0f, 1.02f, 0f, 0f, 6f,
                        0f, 0f, 1.20f, 0f, 18f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Black & White" -> ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )
            "HDR" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, -15f,
                        0f, 1.25f, 0f, 0f, -15f,
                        0f, 0f, 1.25f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }
}
