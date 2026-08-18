package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.InputStream
import java.util.EnumMap

/**
 * Helper utility to decode QR codes directly from image Uris or Bitmaps.
 * Used as a fallback when the physical camera is unavailable or user chooses to upload an image.
 */
object QrCodeImageDecoder {

    /**
     * Decodes a QR code from a content Uri.
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                decodeQrFromBitmap(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a QR code from a Bitmap.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val intArray = IntArray(width * height)
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
            hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
