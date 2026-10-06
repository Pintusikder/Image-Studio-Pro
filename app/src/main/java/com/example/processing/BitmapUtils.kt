package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.model.DetectedImageFormat
import com.example.model.ResizeMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtils {

    data class ImageHeader(
        val width: Int,
        val height: Int,
        val fileSizeBytes: Long,
        val dpi: Int? = null,
        val detectedFormat: DetectedImageFormat = DetectedImageFormat.JPEG,
        val fileName: String = ""
    )

    fun detectFormatFromBytes(bytes: ByteArray): DetectedImageFormat {
        if (bytes.size < 4) return DetectedImageFormat.UNKNOWN

        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return DetectedImageFormat.JPEG
        }

        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return DetectedImageFormat.PNG
        }

        // GIF: GIF87a / GIF89a
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
            return DetectedImageFormat.GIF
        }

        // BMP: 42 4D ("BM")
        if (bytes[0] == 'B'.code.toByte() && bytes[1] == 'M'.code.toByte()) {
            return DetectedImageFormat.BMP
        }

        // WEBP: RIFF....WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return DetectedImageFormat.WEBP
        }

        // HEIC / HEIF / AVIF: ftypheic, ftypmif1, ftypmsf1, ftypheix, ftyphevc, ftypavis, ftypavif at offset 4..11
        if (bytes.size >= 12 && bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() && bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()) {
            val brand = String(bytes, 8, min(4, bytes.size - 8))
            if (brand.startsWith("heic") || brand.startsWith("mif1") || brand.startsWith("msf1") ||
                brand.startsWith("heix") || brand.startsWith("hevc") || brand.startsWith("heim") ||
                brand.startsWith("heis") || brand.startsWith("avif") || brand.startsWith("avis")
            ) {
                return DetectedImageFormat.HEIC
            }
        }

        return DetectedImageFormat.UNKNOWN
    }

    fun detectFormat(context: Context, uri: Uri): DetectedImageFormat {
        try {
            val headerBytes = ByteArray(32)
            var bytesRead = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bytesRead = stream.read(headerBytes, 0, headerBytes.size)
            }
            if (bytesRead >= 4) {
                val detected = detectFormatFromBytes(headerBytes.copyOf(bytesRead))
                if (detected != DetectedImageFormat.UNKNOWN) {
                    return detected
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to MIME type
        try {
            val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
            return when {
                mime.contains("jpeg") || mime.contains("jpg") -> DetectedImageFormat.JPEG
                mime.contains("png") -> DetectedImageFormat.PNG
                mime.contains("webp") -> DetectedImageFormat.WEBP
                mime.contains("heic") || mime.contains("heif") -> DetectedImageFormat.HEIC
                mime.contains("bmp") -> DetectedImageFormat.BMP
                mime.contains("gif") -> DetectedImageFormat.GIF
                else -> {
                    val path = uri.path?.lowercase() ?: ""
                    when {
                        path.endsWith(".jpg") || path.endsWith(".jpeg") -> DetectedImageFormat.JPEG
                        path.endsWith(".png") -> DetectedImageFormat.PNG
                        path.endsWith(".webp") -> DetectedImageFormat.WEBP
                        path.endsWith(".heic") || path.endsWith(".heif") -> DetectedImageFormat.HEIC
                        path.endsWith(".bmp") -> DetectedImageFormat.BMP
                        path.endsWith(".gif") -> DetectedImageFormat.GIF
                        else -> DetectedImageFormat.JPEG
                    }
                }
            }
        } catch (e: Exception) {
            return DetectedImageFormat.JPEG
        }
    }

    fun hasTransparency(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return false
        if (bitmap.config != Bitmap.Config.ARGB_8888) return false

        val w = bitmap.width
        val h = bitmap.height
        val stepX = max(1, w / 40)
        val stepY = max(1, h / 40)

        for (y in 0 until h step stepY) {
            for (x in 0 until w step stepX) {
                val pixel = bitmap.getPixel(x, y)
                if (android.graphics.Color.alpha(pixel) < 250) {
                    return true
                }
            }
        }

        // Check border corners
        val corners = listOf(
            0 to 0,
            w - 1 to 0,
            0 to h - 1,
            w - 1 to h - 1,
            w / 2 to 0,
            w / 2 to h - 1,
            0 to h / 2,
            w - 1 to h / 2
        )
        for ((cx, cy) in corners) {
            val pixel = bitmap.getPixel(cx.coerceIn(0, w - 1), cy.coerceIn(0, h - 1))
            if (android.graphics.Color.alpha(pixel) < 250) {
                return true
            }
        }
        return false
    }

    fun compositeOnBackground(bitmap: Bitmap, backgroundColor: Int): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return out
    }

    fun getExifDpi(context: Context, uri: Uri): Int? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val xRes = exif.getAttribute(ExifInterface.TAG_X_RESOLUTION)
                val unit = exif.getAttributeInt(ExifInterface.TAG_RESOLUTION_UNIT, 2) // 2 = inches, 3 = cm
                if (xRes != null) {
                    val resVal = if (xRes.contains("/")) {
                        val parts = xRes.split("/")
                        val num = parts[0].toDoubleOrNull() ?: 0.0
                        val den = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                        if (den > 0) (num / den).roundToInt() else num.roundToInt()
                    } else {
                        xRes.toDoubleOrNull()?.roundToInt()
                    }
                    if (resVal != null && resVal > 0) {
                        if (unit == 3) (resVal * 2.54).roundToInt() else resVal
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPngDpi(context: Context, uri: Uri): Int? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(8)
                if (stream.read(header) != 8) return null
                if (header[0] != 0x89.toByte() || header[1] != 0x50.toByte() || header[2] != 0x4E.toByte() || header[3] != 0x47.toByte()) {
                    return null
                }
                val buffer = ByteArray(4096)
                var read = stream.read(buffer)
                while (read > 8) {
                    for (i in 0 until read - 8) {
                        if (buffer[i] == 'p'.code.toByte() && buffer[i + 1] == 'H'.code.toByte() &&
                            buffer[i + 2] == 'y'.code.toByte() && buffer[i + 3] == 's'.code.toByte()
                        ) {
                            if (i + 12 < read) {
                                val xPpm = ((buffer[i + 4].toInt() and 0xFF) shl 24) or
                                        ((buffer[i + 5].toInt() and 0xFF) shl 16) or
                                        ((buffer[i + 6].toInt() and 0xFF) shl 8) or
                                        (buffer[i + 7].toInt() and 0xFF)
                                val unit = buffer[i + 12].toInt() and 0xFF
                                if (unit == 1 && xPpm > 0) { // 1 = metre
                                    return (xPpm / 39.37007874).roundToInt()
                                }
                            }
                        }
                    }
                    read = stream.read(buffer)
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readImageDpi(context: Context, uri: Uri): Int? {
        val exifDpi = getExifDpi(context, uri)
        if (exifDpi != null && exifDpi > 0) return exifDpi
        val pngDpi = getPngDpi(context, uri)
        if (pngDpi != null && pngDpi > 0) return pngDpi
        return null
    }

    fun getFileExifDpi(file: File): Int? {
        return try {
            file.inputStream().use { stream ->
                val exif = ExifInterface(stream)
                val xRes = exif.getAttribute(ExifInterface.TAG_X_RESOLUTION)
                val unit = exif.getAttributeInt(ExifInterface.TAG_RESOLUTION_UNIT, 2)
                if (xRes != null) {
                    val resVal = if (xRes.contains("/")) {
                        val parts = xRes.split("/")
                        val num = parts[0].toDoubleOrNull() ?: 0.0
                        val den = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                        if (den > 0) (num / den).roundToInt() else num.roundToInt()
                    } else {
                        xRes.toDoubleOrNull()?.roundToInt()
                    }
                    if (resVal != null && resVal > 0) {
                        if (unit == 3) (resVal * 2.54).roundToInt() else resVal
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFilePngDpi(file: File): Int? {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(8)
                if (stream.read(header) != 8) return null
                if (header[0] != 0x89.toByte() || header[1] != 0x50.toByte() || header[2] != 0x4E.toByte() || header[3] != 0x47.toByte()) {
                    return null
                }
                val buffer = ByteArray(4096)
                var read = stream.read(buffer)
                while (read > 8) {
                    for (i in 0 until read - 8) {
                        if (buffer[i] == 'p'.code.toByte() && buffer[i + 1] == 'H'.code.toByte() &&
                            buffer[i + 2] == 'y'.code.toByte() && buffer[i + 3] == 's'.code.toByte()
                        ) {
                            if (i + 12 < read) {
                                val xPpm = ((buffer[i + 4].toInt() and 0xFF) shl 24) or
                                        ((buffer[i + 5].toInt() and 0xFF) shl 16) or
                                        ((buffer[i + 6].toInt() and 0xFF) shl 8) or
                                        (buffer[i + 7].toInt() and 0xFF)
                                val unit = buffer[i + 12].toInt() and 0xFF
                                if (unit == 1 && xPpm > 0) { // 1 = metre
                                    return (xPpm / 39.37007874).roundToInt()
                                }
                            }
                        }
                    }
                    read = stream.read(buffer)
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileDpi(file: File): Int? {
        val exifDpi = getFileExifDpi(file)
        if (exifDpi != null && exifDpi > 0) return exifDpi
        val pngDpi = getFilePngDpi(file)
        if (pngDpi != null && pngDpi > 0) return pngDpi
        return null
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (name.isNullOrBlank()) {
            val path = uri.path
            if (path != null) {
                val cut = path.lastIndexOf('/')
                name = if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return if (!name.isNullOrBlank()) name else "image_${System.currentTimeMillis()}.jpg"
    }

    fun getImageHeader(context: Context, uri: Uri): ImageHeader {
        var w = 0
        var h = 0
        val dpi = readImageDpi(context, uri)
        val fileName = getFileName(context, uri)
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            // Check EXIF orientation swap
            val orientation = getExifOrientation(context, uri)
            if (orientation == 90 || orientation == 270) {
                w = options.outHeight
                h = options.outWidth
            } else {
                w = options.outWidth
                h = options.outHeight
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var sizeBytes = 0L
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                sizeBytes = pfd.statSize
            }
        } catch (e: Exception) {
            // fallback
        }
        if (sizeBytes <= 0) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    sizeBytes = stream.available().toLong()
                }
            } catch (e: Exception) {
                // fallback
            }
        }

        val detectedFormat = detectFormat(context, uri)
        return ImageHeader(w, h, sizeBytes, dpi, detectedFormat, fileName)
    }

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 4096,
        reqHeight: Int = 4096
    ): Bitmap? {
        return try {
            val uriValidation = SecurityPrivacyEngine.validateAndInspectUri(
                context = context,
                uri = uri,
                verifyImageStream = false
            )
            if (!uriValidation.isSafe) {
                return null
            }

            val headerBytes = ByteArray(32)
            val bytesRead = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.read(headerBytes, 0, headerBytes.size)
            } ?: -1

            if (bytesRead < 4) {
                return null
            }

            val format = detectFormatFromBytes(headerBytes.copyOf(bytesRead))
            if (format == DetectedImageFormat.UNKNOWN) {
                return null
            }

            // First decode bounds only
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            // Decompression bomb guard
            val totalPixels = options.outWidth.toLong() * options.outHeight.toLong()
            if (options.outWidth > SecurityPrivacyEngine.MAX_SAFE_DIMENSION ||
                options.outHeight > SecurityPrivacyEngine.MAX_SAFE_DIMENSION ||
                totalPixels > SecurityPrivacyEngine.MAX_SAFE_PIXEL_COUNT
            ) {
                return null
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            // Apply EXIF rotation if needed
            val orientation = getExifOrientation(context, uri)
            rotateBitmap(bitmap, orientation)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getExifOrientationRaw(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun getExifOrientationDescription(orientation: Int): String {
        return when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> "Normal (0°)"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped Horizontal"
            ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped Vertical"
            ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed (Flip & 90° CW)"
            ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90° CW"
            ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse (Flip & 270° CW)"
            ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270° CW"
            else -> "Normal (0°)"
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun rotateBitmapFloat(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            postScale(
                if (horizontal) -1f else 1f,
                if (vertical) -1f else 1f,
                bitmap.width / 2f,
                bitmap.height / 2f
            )
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cropBitmap(bitmap: Bitmap, normalizedRect: RectF): Bitmap {
        val left = (normalizedRect.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (normalizedRect.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (normalizedRect.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (normalizedRect.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)

        val cropWidth = max(1, right - left)
        val cropHeight = max(1, bottom - top)

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int, filter: Boolean = true): Bitmap {
        val safeW = max(1, targetWidth)
        val safeH = max(1, targetHeight)
        return Bitmap.createScaledBitmap(bitmap, safeW, safeH, filter)
    }

    /**
     * Resizes a bitmap according to professional modes:
     * - FIT: Entire image fits inside target dimensions without cropping (centered with canvas padding).
     * - FILL: Target dimensions are completely filled with cropping.
     * - STRETCH: Image is forced into target dimensions (may distort).
     * - SMART_CROP: Automatically determines the most informative subject region using saliency/face detection.
     */
    fun resizeBitmapWithMode(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        mode: ResizeMode,
        fitBackgroundColor: Int = android.graphics.Color.TRANSPARENT,
        filter: Boolean = true
    ): Bitmap {
        val safeW = max(1, targetWidth)
        val safeH = max(1, targetHeight)

        return when (mode) {
            ResizeMode.STRETCH -> {
                Bitmap.createScaledBitmap(bitmap, safeW, safeH, filter)
            }
            ResizeMode.FIT -> {
                val scale = min(safeW.toFloat() / bitmap.width, safeH.toFloat() / bitmap.height)
                val scaledW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val scaledH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, filter)

                // Render centered onto exact target dimensions canvas
                val outBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outBitmap)
                if (fitBackgroundColor != android.graphics.Color.TRANSPARENT) {
                    canvas.drawColor(fitBackgroundColor)
                }
                val left = (safeW - scaledW) / 2f
                val top = (safeH - scaledH) / 2f
                canvas.drawBitmap(scaled, left, top, null)
                outBitmap
            }
            ResizeMode.FILL -> {
                val scale = max(safeW.toFloat() / bitmap.width, safeH.toFloat() / bitmap.height)
                val scaledW = (bitmap.width * scale).roundToInt().coerceAtLeast(safeW)
                val scaledH = (bitmap.height * scale).roundToInt().coerceAtLeast(safeH)
                val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, filter)
                val cropX = ((scaledW - safeW) / 2).coerceIn(0, max(0, scaledW - safeW))
                val cropY = ((scaledH - safeH) / 2).coerceIn(0, max(0, scaledH - safeH))
                Bitmap.createBitmap(scaled, cropX, cropY, safeW, safeH)
            }
            ResizeMode.SMART_CROP -> {
                val cropRect = calculateSmartCropRect(bitmap, safeW, safeH)
                val cropW = cropRect.width().coerceAtLeast(1)
                val cropH = cropRect.height().coerceAtLeast(1)
                val cropped = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropW, cropH)
                Bitmap.createScaledBitmap(cropped, safeW, safeH, filter)
            }
        }
    }

    private fun findFaceCenter(bitmap: Bitmap): PointF? {
        return try {
            val analysisWidth = min(bitmap.width, 320).let { if (it % 2 != 0) it - 1 else it }
            val analysisHeight = (bitmap.height.toFloat() / bitmap.width.toFloat() * analysisWidth).roundToInt()
            if (analysisWidth <= 0 || analysisHeight <= 0) return null
            val rgb565 = Bitmap.createScaledBitmap(bitmap, analysisWidth, analysisHeight, true)
                .copy(Bitmap.Config.RGB_565, false) ?: return null
            val detector = android.media.FaceDetector(analysisWidth, analysisHeight, 3)
            val faces = Array<android.media.FaceDetector.Face?>(3) { null }
            val count = detector.findFaces(rgb565, faces)
            if (count > 0 && faces[0] != null) {
                val pt = PointF()
                faces[0]!!.getMidPoint(pt)
                PointF(pt.x / analysisWidth, pt.y / analysisHeight)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the optimal cropping rectangle for target aspect ratio using face detection
     * and visual saliency / edge energy analysis.
     */
    fun calculateSmartCropRect(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Rect {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val srcAspect = srcW.toFloat() / srcH.toFloat()

        if (kotlin.math.abs(srcAspect - targetAspect) < 0.005f) {
            return Rect(0, 0, srcW, srcH)
        }

        // Try Face Detection
        val faceCenter = findFaceCenter(bitmap)
        if (faceCenter != null) {
            if (srcAspect > targetAspect) {
                // Source is wider: crop horizontally around face
                val cropW = (srcH * targetAspect).roundToInt().coerceIn(1, srcW)
                val desiredCenterX = (faceCenter.x * srcW).roundToInt()
                val left = (desiredCenterX - cropW / 2).coerceIn(0, srcW - cropW)
                return Rect(left, 0, left + cropW, srcH)
            } else {
                // Source is taller: crop vertically around face
                val cropH = (srcW / targetAspect).roundToInt().coerceIn(1, srcH)
                val desiredCenterY = (faceCenter.y * srcH).roundToInt()
                val top = (desiredCenterY - cropH / 2).coerceIn(0, srcH - cropH)
                return Rect(0, top, srcW, top + cropH)
            }
        }

        // Fast Visual Saliency / Gradient Energy Map Analysis
        return try {
            val sampleW = 100
            val sampleH = (100f / srcAspect).roundToInt().coerceIn(10, 200)
            val thumb = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
            val pixels = IntArray(sampleW * sampleH)
            thumb.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

            if (srcAspect > targetAspect) {
                // Crop horizontally
                val colEnergy = FloatArray(sampleW)
                for (x in 1 until sampleW - 1) {
                    var sum = 0f
                    for (y in 1 until sampleH - 1) {
                        val idx = y * sampleW + x
                        val pL = pixels[idx - 1]
                        val pR = pixels[idx + 1]
                        val pT = pixels[idx - sampleW]
                        val pB = pixels[idx + sampleW]
                        val lumL = (android.graphics.Color.red(pL) * 299 + android.graphics.Color.green(pL) * 587 + android.graphics.Color.blue(pL) * 114) / 1000
                        val lumR = (android.graphics.Color.red(pR) * 299 + android.graphics.Color.green(pR) * 587 + android.graphics.Color.blue(pR) * 114) / 1000
                        val lumT = (android.graphics.Color.red(pT) * 299 + android.graphics.Color.green(pT) * 587 + android.graphics.Color.blue(pT) * 114) / 1000
                        val lumB = (android.graphics.Color.red(pB) * 299 + android.graphics.Color.green(pB) * 587 + android.graphics.Color.blue(pB) * 114) / 1000
                        sum += kotlin.math.abs(lumR - lumL) + kotlin.math.abs(lumB - lumT)
                    }
                    colEnergy[x] = sum
                }

                val windowSize = (sampleH * targetAspect).roundToInt().coerceIn(1, sampleW)
                var maxEnergy = -1f
                var bestStart = (sampleW - windowSize) / 2
                val maxStart = sampleW - windowSize
                for (start in 0..maxStart) {
                    var wEnergy = 0f
                    for (x in start until (start + windowSize)) {
                        wEnergy += colEnergy[x]
                    }
                    val centerDist = kotlin.math.abs((start + windowSize / 2f) - sampleW / 2f) / (sampleW / 2f)
                    val finalScore = wEnergy * (1f - 0.15f * centerDist)
                    if (finalScore > maxEnergy) {
                        maxEnergy = finalScore
                        bestStart = start
                    }
                }

                val normStart = if (maxStart > 0) bestStart.toFloat() / maxStart else 0f
                val actualCropW = (srcH * targetAspect).roundToInt().coerceIn(1, srcW)
                val maxLeft = srcW - actualCropW
                val actualLeft = (normStart * maxLeft).roundToInt().coerceIn(0, maxLeft)
                Rect(actualLeft, 0, actualLeft + actualCropW, srcH)
            } else {
                // Crop vertically
                val rowEnergy = FloatArray(sampleH)
                for (y in 1 until sampleH - 1) {
                    var sum = 0f
                    for (x in 1 until sampleW - 1) {
                        val idx = y * sampleW + x
                        val pL = pixels[idx - 1]
                        val pR = pixels[idx + 1]
                        val pT = pixels[idx - sampleW]
                        val pB = pixels[idx + sampleW]
                        val lumL = (android.graphics.Color.red(pL) * 299 + android.graphics.Color.green(pL) * 587 + android.graphics.Color.blue(pL) * 114) / 1000
                        val lumR = (android.graphics.Color.red(pR) * 299 + android.graphics.Color.green(pR) * 587 + android.graphics.Color.blue(pR) * 114) / 1000
                        val lumT = (android.graphics.Color.red(pT) * 299 + android.graphics.Color.green(pT) * 587 + android.graphics.Color.blue(pT) * 114) / 1000
                        val lumB = (android.graphics.Color.red(pB) * 299 + android.graphics.Color.green(pB) * 587 + android.graphics.Color.blue(pB) * 114) / 1000
                        sum += kotlin.math.abs(lumR - lumL) + kotlin.math.abs(lumB - lumT)
                    }
                    rowEnergy[y] = sum
                }

                val windowSize = (sampleW / targetAspect).roundToInt().coerceIn(1, sampleH)
                var maxEnergy = -1f
                var bestStart = (sampleH - windowSize) / 2
                val maxStart = sampleH - windowSize
                for (start in 0..maxStart) {
                    var wEnergy = 0f
                    for (y in start until (start + windowSize)) {
                        wEnergy += rowEnergy[y]
                    }
                    val centerDist = kotlin.math.abs((start + windowSize / 2f) - sampleH / 2f) / (sampleH / 2f)
                    val finalScore = wEnergy * (1f - 0.15f * centerDist)
                    if (finalScore > maxEnergy) {
                        maxEnergy = finalScore
                        bestStart = start
                    }
                }

                val normStart = if (maxStart > 0) bestStart.toFloat() / maxStart else 0f
                val actualCropH = (srcW / targetAspect).roundToInt().coerceIn(1, srcH)
                val maxTop = srcH - actualCropH
                val actualTop = (normStart * maxTop).roundToInt().coerceIn(0, maxTop)
                Rect(0, actualTop, srcW, actualTop + actualCropH)
            }
        } catch (e: Exception) {
            // Fallback to center crop
            if (srcAspect > targetAspect) {
                val cropW = (srcH * targetAspect).roundToInt().coerceIn(1, srcW)
                val left = (srcW - cropW) / 2
                Rect(left, 0, left + cropW, srcH)
            } else {
                val cropH = (srcW / targetAspect).roundToInt().coerceIn(1, srcH)
                val top = (srcH - cropH) / 2
                Rect(0, top, srcW, top + cropH)
            }
        }
    }

    fun straightenBitmap(bitmap: Bitmap, angleDegrees: Float): Bitmap {
        if (angleDegrees == 0f) return bitmap
        val matrix = Matrix().apply {
            postRotate(angleDegrees, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun generateThumbnailBase64(bitmap: Bitmap, maxDim: Int = 120): String {
        val scale = min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height).coerceAtMost(1f)
        val w = max(1, (bitmap.width * scale).toInt())
        val h = max(1, (bitmap.height * scale).toInt())
        val thumb = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val stream = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeThumbnailFromBase64(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
