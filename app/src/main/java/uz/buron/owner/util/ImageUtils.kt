package uz.buron.owner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {
    private const val MAX_SIZE_BYTES = 5 * 1024 * 1024
    private const val INITIAL_MAX_DIMENSION = 1920

    private val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/pjpeg",
        "image/x-png"
    )

    fun isSupportedImage(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)?.lowercase()
        if (mime.isNullOrBlank()) return true
        return mime in SUPPORTED_MIME_TYPES || mime.startsWith("image/")
    }

    fun uriToCompressedFile(context: Context, uri: Uri): File {
        var bitmap = decodeBitmap(context, uri)
            ?: throw IllegalArgumentException("Rasmni o'qib bo'lmadi. JPEG, PNG yoki WebP tanlang.")

        bitmap = scaleDown(bitmap, INITIAL_MAX_DIMENSION)
        return compressToMaxSize(bitmap, context)
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (_: Exception) {
                // Fall through to BitmapFactory
            }
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, bounds)

            context.contentResolver.openInputStream(uri)?.use { decodeInput ->
                val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, INITIAL_MAX_DIMENSION)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(decodeInput, null, options)
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / sampleSize >= maxDimension || halfHeight / sampleSize >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val newWidth = (width * scale).roundToInt()
        val newHeight = (height * scale).roundToInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun compressToMaxSize(bitmap: Bitmap, context: Context): File {
        var quality = 90
        val file = File.createTempFile("venue_upload_", ".jpg", context.cacheDir)

        try {
            do {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                if (file.length() <= MAX_SIZE_BYTES || quality <= 20) break
                quality -= 10
            } while (true)

            if (file.length() > MAX_SIZE_BYTES) {
                throw IllegalArgumentException("Rasm 5 MB dan katta. Boshqa rasm tanlang.")
            }
            return file
        } finally {
            bitmap.recycle()
        }
    }
}
