package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import com.example.model.DetectedImageFormat
import com.example.model.ImageExifData
import kotlin.math.abs
import kotlin.math.roundToInt

data class ColorSwatchInfo(
    val hex: String,
    val colorInt: Int,
    val red: Int,
    val green: Int,
    val blue: Int,
    val percentage: Float,
    val label: String
)

data class ImageInfoDetails(
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedFileSize: String,
    val memoryFootprintBytes: Long,
    val formattedMemoryFootprint: String,
    val width: Int,
    val height: Int,
    val originalWidth: Int,
    val originalHeight: Int,
    val megapixels: Float,
    val aspectRatioRatioString: String,
    val aspectRatioDecimal: Float,
    val aspectRatioStandardName: String?,
    val orientationCategory: String, // "Landscape", "Portrait", "Square"
    val formatDisplayName: String,
    val formatExtension: String,
    val mimeType: String,
    val dpi: Int,
    val isDefaultDpi: Boolean,
    val printWidthInches: Float,
    val printHeightInches: Float,
    val printWidthCm: Float,
    val printHeightCm: Float,
    val exifOrientationDegree: Int,
    val exifOrientationDescription: String,
    val canvasOrientationDescription: String,
    val colorModel: String,
    val bitDepth: String,
    val colorSpace: String,
    val hasAlphaChannel: Boolean,
    val hasTransparentPixels: Boolean,
    val isGrayscale: Boolean,
    val dominantColors: List<ColorSwatchInfo>,
    val exifAvailable: Boolean,
    val exifTagCount: Int,
    val hasGps: Boolean,
    val hasCameraInfo: Boolean,
    val hasDateInfo: Boolean,
    val hasAuthorCopyright: Boolean,
    val creationDateTimeOriginal: String?,
    val creationDateTimeDigitized: String?,
    val modificationDateTime: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val lensModel: String?,
    val software: String?,
    val artist: String?,
    val copyright: String?,
    val exposureSummary: String?,
    val gpsCoordinatesFormatted: String?
)

object ImageInfoAnalyzer {

    fun analyze(
        context: Context,
        uri: Uri?,
        bitmap: Bitmap?,
        originalWidth: Int = 0,
        originalHeight: Int = 0,
        originalFileSize: Long = 0,
        originalMime: String = "image/jpeg",
        originalDpi: Int? = null,
        exif: ImageExifData? = null,
        currentDpi: Int = 300,
        detectedFormat: DetectedImageFormat = DetectedImageFormat.JPEG
    ): ImageInfoDetails {
        val fileName = if (uri != null) {
            BitmapUtils.getFileName(context, uri)
        } else {
            "canvas_image_${System.currentTimeMillis()}.jpg"
        }

        val curW = bitmap?.width ?: originalWidth.coerceAtLeast(1)
        val curH = bitmap?.height ?: originalHeight.coerceAtLeast(1)
        val origW = if (originalWidth > 0) originalWidth else curW
        val origH = if (originalHeight > 0) originalHeight else curH

        // File Size
        val fileSize = if (originalFileSize > 0) originalFileSize else (curW.toLong() * curH.toLong() * 3L / 2L)
        val formattedSize = formatBytes(fileSize)

        // Memory Footprint in RAM
        val memoryBytes = bitmap?.allocationByteCount?.toLong() ?: (curW.toLong() * curH.toLong() * 4L)
        val formattedMemory = formatBytes(memoryBytes)

        // Megapixels
        val megapixels = (curW.toFloat() * curH.toFloat()) / 1_000_000f

        // Aspect Ratio
        val decimalRatio = if (curH > 0) curW.toFloat() / curH.toFloat() else 1.0f
        val ratioString = calculateAspectRatioString(curW, curH)
        val standardName = matchStandardAspectRatio(curW, curH, decimalRatio)
        val orientationCategory = when {
            curW > curH -> "Landscape"
            curH > curW -> "Portrait"
            else -> "Square"
        }

        // DPI & Physical Print
        val effectiveDpi = (if (originalDpi != null && originalDpi > 0) originalDpi else currentDpi).coerceAtLeast(1)
        val isDefaultDpi = originalDpi == null
        val printWInches = curW.toFloat() / effectiveDpi
        val printHInches = curH.toFloat() / effectiveDpi
        val printWCm = printWInches * 2.54f
        val printHCm = printHInches * 2.54f

        // Orientation
        val rawExifOrientation = if (uri != null) BitmapUtils.getExifOrientationRaw(context, uri) else 1
        val exifDegree = if (uri != null) BitmapUtils.getExifOrientation(context, uri) else 0
        val exifOrientationDesc = BitmapUtils.getExifOrientationDescription(rawExifOrientation)
        val canvasOrientationDesc = "$orientationCategory (${curW} × ${curH} px)"

        // Color Information
        val config = bitmap?.config ?: Bitmap.Config.ARGB_8888
        val (colorModel, bitDepth) = when (config) {
            Bitmap.Config.ARGB_8888 -> Pair("RGBA (Red, Green, Blue, Alpha)", "32-bit (8-bit per channel)")
            Bitmap.Config.RGB_565 -> Pair("RGB (Red, Green, Blue)", "16-bit (5-6-5)")
            Bitmap.Config.ALPHA_8 -> Pair("Alpha Channel Only", "8-bit")
            else -> Pair("RGBA (High Dynamic Range)", "64-bit Half-Float")
        }

        val colorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap != null) {
            bitmap.colorSpace?.name ?: "sRGB (IEC 61966-2.1)"
        } else {
            "sRGB (Standard Color Space)"
        }

        val hasAlphaChannel = bitmap?.hasAlpha() ?: false
        val hasTransparentPixels = if (bitmap != null) BitmapUtils.hasTransparency(bitmap) else false

        // Extract Dominant Colors
        val dominantColors = extractDominantColors(bitmap)
        val isGrayscale = checkIsGrayscale(dominantColors)

        // EXIF Availability
        val exifAvailable = exif != null && exif.totalIdentifiedTagsCount > 0
        val exifTagCount = exif?.totalIdentifiedTagsCount ?: 0
        val hasGps = exif?.hasGps == true
        val hasCamera = exif?.hasCameraInfo == true
        val hasDates = exif?.hasDateInfo == true
        val hasAuthor = !exif?.artist.isNullOrBlank() || !exif?.copyright.isNullOrBlank()

        // Exposure summary
        val exposureParts = mutableListOf<String>()
        if (!exif?.shutterSpeed.isNullOrBlank()) exposureParts.add("${exif?.shutterSpeed}s")
        if (!exif?.aperture.isNullOrBlank()) exposureParts.add("f/${exif?.aperture}")
        if (!exif?.iso.isNullOrBlank()) exposureParts.add("ISO ${exif?.iso}")
        if (!exif?.focalLength.isNullOrBlank()) exposureParts.add("${exif?.focalLength}mm")
        val exposureSummary = if (exposureParts.isNotEmpty()) exposureParts.joinToString(", ") else null

        // GPS formatted
        val gpsCoordinatesFormatted = if (exif != null && exif.hasGps && exif.latitude != null && exif.longitude != null) {
            val latRef = if (exif.latitude >= 0) "N" else "S"
            val lonRef = if (exif.longitude >= 0) "E" else "W"
            String.format("%.4f° %s, %.4f° %s", abs(exif.latitude), latRef, abs(exif.longitude), lonRef)
        } else null

        return ImageInfoDetails(
            fileName = fileName,
            fileSizeBytes = fileSize,
            formattedFileSize = formattedSize,
            memoryFootprintBytes = memoryBytes,
            formattedMemoryFootprint = formattedMemory,
            width = curW,
            height = curH,
            originalWidth = origW,
            originalHeight = origH,
            megapixels = megapixels,
            aspectRatioRatioString = ratioString,
            aspectRatioDecimal = decimalRatio,
            aspectRatioStandardName = standardName,
            orientationCategory = orientationCategory,
            formatDisplayName = detectedFormat.displayName,
            formatExtension = ".${detectedFormat.extension}",
            mimeType = if (!originalMime.isNullOrBlank()) originalMime else detectedFormat.mimeType,
            dpi = effectiveDpi,
            isDefaultDpi = isDefaultDpi,
            printWidthInches = printWInches,
            printHeightInches = printHInches,
            printWidthCm = printWCm,
            printHeightCm = printHCm,
            exifOrientationDegree = exifDegree,
            exifOrientationDescription = exifOrientationDesc,
            canvasOrientationDescription = canvasOrientationDesc,
            colorModel = colorModel,
            bitDepth = bitDepth,
            colorSpace = colorSpace,
            hasAlphaChannel = hasAlphaChannel,
            hasTransparentPixels = hasTransparentPixels,
            isGrayscale = isGrayscale,
            dominantColors = dominantColors,
            exifAvailable = exifAvailable,
            exifTagCount = exifTagCount,
            hasGps = hasGps,
            hasCameraInfo = hasCamera,
            hasDateInfo = hasDates,
            hasAuthorCopyright = hasAuthor,
            creationDateTimeOriginal = exif?.dateTimeOriginal,
            creationDateTimeDigitized = exif?.dateTimeDigitized,
            modificationDateTime = exif?.dateTime,
            cameraMake = exif?.make,
            cameraModel = exif?.model,
            lensModel = exif?.lensModel,
            software = exif?.software,
            artist = exif?.artist,
            copyright = exif?.copyright,
            exposureSummary = exposureSummary,
            gpsCoordinatesFormatted = gpsCoordinatesFormatted
        )
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    private fun calculateAspectRatioString(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "1:1"
        val d = gcd(width, height)
        val simW = width / d
        val simH = height / d
        // If reduced ratio is clean, use it
        if (simW <= 32 && simH <= 32) {
            return "$simW:$simH"
        }
        // Approximate standard ratios
        val ratio = width.toFloat() / height.toFloat()
        val standardList = listOf(
            Pair(16f / 9f, "16:9"),
            Pair(4f / 3f, "4:3"),
            Pair(3f / 2f, "3:2"),
            Pair(1f / 1f, "1:1"),
            Pair(5f / 4f, "5:4"),
            Pair(21f / 9f, "21:9"),
            Pair(9f / 16f, "9:16"),
            Pair(3f / 4f, "3:4"),
            Pair(2f / 3f, "2:3"),
            Pair(4f / 5f, "4:5")
        )
        val closest = standardList.minByOrNull { abs(it.first - ratio) }
        return if (closest != null && abs(closest.first - ratio) < 0.035f) {
            closest.second
        } else {
            String.format("%.2f:1", ratio)
        }
    }

    private fun matchStandardAspectRatio(width: Int, height: Int, ratio: Float): String? {
        val standardPresets = listOf(
            Triple(16f / 9f, "16:9", "Widescreen / Full HD (16:9)"),
            Triple(4f / 3f, "4:3", "Standard Photo / Tablet (4:3)"),
            Triple(3f / 2f, "3:2", "Classic 35mm DSLR (3:2)"),
            Triple(1f / 1f, "1:1", "Square / Avatar / Instagram (1:1)"),
            Triple(5f / 4f, "5:4", "Large Format / Portrait (5:4)"),
            Triple(21f / 9f, "21:9", "Ultrawide Cinematic (21:9)"),
            Triple(9f / 16f, "9:16", "Vertical Story / Reel (9:16)"),
            Triple(3f / 4f, "3:4", "Portrait 3:4"),
            Triple(2f / 3f, "2:3", "Vertical 2:3 Photo"),
            Triple(4f / 5f, "4:5", "Social Portrait 4:5")
        )
        val match = standardPresets.firstOrNull { abs(it.first - ratio) < 0.035f }
        return match?.third
    }

    private fun extractDominantColors(bitmap: Bitmap?): List<ColorSwatchInfo> {
        if (bitmap == null || bitmap.isRecycled) {
            return listOf(
                ColorSwatchInfo("#38BDF8", Color.parseColor("#38BDF8"), 56, 189, 248, 100f, "Accent Blue")
            )
        }

        try {
            // Sample on a downscaled 48x48 thumbnail to be lightweight and fast
            val sampleSize = 48
            val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, false)
            val pixels = IntArray(sampleSize * sampleSize)
            scaled.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)
            if (scaled != bitmap) scaled.recycle()

            val colorCounts = mutableMapOf<Int, Int>()
            var totalValidPixels = 0

            for (pixel in pixels) {
                val alpha = Color.alpha(pixel)
                if (alpha < 64) continue // Skip highly transparent pixels

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Quantize to 5-bit color buckets to group similar shades
                val qR = (r shr 3) shl 3
                val qG = (g shr 3) shl 3
                val qB = (b shr 3) shl 3
                val quantizedColor = Color.rgb(qR, qG, qB)

                colorCounts[quantizedColor] = (colorCounts[quantizedColor] ?: 0) + 1
                totalValidPixels++
            }

            if (totalValidPixels == 0) {
                return listOf(
                    ColorSwatchInfo("#00000000", Color.TRANSPARENT, 0, 0, 0, 100f, "Transparent")
                )
            }

            // Pick top 5 dominant colors
            val topColors = colorCounts.entries
                .sortedByDescending { it.value }
                .take(5)

            val labels = listOf("Primary Dominant", "Secondary Tone", "Accent Shade", "Highlight", "Base Neutral")

            return topColors.mapIndexed { index, entry ->
                val c = entry.key
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                val hex = String.format("#%02X%02X%02X", r, g, b)
                val pct = (entry.value.toFloat() / totalValidPixels.toFloat()) * 100f
                val label = labels.getOrElse(index) { "Swatch #${index + 1}" }

                ColorSwatchInfo(
                    hex = hex,
                    colorInt = c,
                    red = r,
                    green = g,
                    blue = b,
                    percentage = pct,
                    label = label
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf(
                ColorSwatchInfo("#0F172A", Color.parseColor("#0F172A"), 15, 23, 42, 100f, "Default Dark")
            )
        }
    }

    private fun checkIsGrayscale(swatches: List<ColorSwatchInfo>): Boolean {
        if (swatches.isEmpty()) return false
        // A swatch is grayscale if |R - G| <= 12 and |G - B| <= 12
        return swatches.all { swatch ->
            val diffRG = abs(swatch.red - swatch.green)
            val diffGB = abs(swatch.green - swatch.blue)
            val diffRB = abs(swatch.red - swatch.blue)
            diffRG <= 12 && diffGB <= 12 && diffRB <= 12
        }
    }
}
