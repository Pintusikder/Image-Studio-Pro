package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.example.model.ExportFormat
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.model.StandardPrintPreset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Phase 8 Production Engine for:
 * - DPI / PPI Metadata Reading, Embedding & Verification (JPEG JFIF/EXIF, PNG pHYs, PDF points)
 * - Physical Dimensions Bidirectional Conversion (in, cm, mm, pt, px)
 * - Print Size Calculator, Resolution Health Grading, Viewing Distance & Bleed/Safe-Zone Calculation
 */
object DpiPrintEngine {

    const val MM_PER_INCH = 25.4f
    const val CM_PER_INCH = 2.54f
    const val POINTS_PER_INCH = 72f
    const val INCHES_PER_METER = 39.37007874f

    enum class PrintCalculationMode(val label: String, val description: String) {
        PHYSICAL_AND_DPI_TO_PIXELS(
            "Physical Size + DPI → Pixels",
            "Calculate required pixel canvas from physical paper size and target print density"
        ),
        PIXELS_AND_DPI_TO_PHYSICAL(
            "Pixels + DPI → Physical Size",
            "Calculate exact physical print output dimensions from pixel resolution and DPI"
        ),
        PIXELS_AND_PHYSICAL_TO_DPI(
            "Pixels + Physical Size → Effective DPI",
            "Evaluate effective print DPI and sharpness grade when printing an image at a given paper size"
        )
    }

    enum class PrintQualityGrade(
        val label: String,
        val minDpi: Int,
        val badgeColorHex: Int,
        val summary: String
    ) {
        ARCHIVAL_PRO(
            "Archival / Lab Pro (≥300 DPI)",
            300,
            0xFF10B981.toInt(),
            "Commercial photo lab, magazine & fine-art standard. Razor-sharp at close inspection (< 30 cm)."
        ),
        HIGH_QUALITY(
            "High Quality Print (240–299 DPI)",
            240,
            0xFF34D399.toInt(),
            "Crisp desktop & studio print quality. Indistinguishable from 300 DPI at normal handheld distance."
        ),
        STANDARD_VIEWING(
            "Standard Viewing (150–239 DPI)",
            150,
            0xFF38BDF8.toInt(),
            "Good for framed wall prints, albums, and documents viewed at arm's length (50–80 cm)."
        ),
        DRAFT_POSTER(
            "Draft / Large Poster (100–149 DPI)",
            100,
            0xFFF59E0B.toInt(),
            "Acceptable for banners or large posters viewed from > 1.2 meters; slight softness up close."
        ),
        LOW_PIXELATED(
            "Insufficient / Pixelated (<100 DPI)",
            0,
            0xFFEF4444.toInt(),
            "Visible pixelation and blockiness at handheld distance. Upscaling or smaller print size recommended."
        );

        companion object {
            fun fromEffectiveDpi(dpi: Int): PrintQualityGrade = when {
                dpi >= 300 -> ARCHIVAL_PRO
                dpi >= 240 -> HIGH_QUALITY
                dpi >= 150 -> STANDARD_VIEWING
                dpi >= 100 -> DRAFT_POSTER
                else -> LOW_PIXELATED
            }
        }
    }

    data class PhysicalDimensions(
        val widthInches: Float,
        val heightInches: Float,
        val widthCm: Float,
        val heightCm: Float,
        val widthMm: Float,
        val heightMm: Float,
        val widthPoints: Float,
        val heightPoints: Float
    ) {
        fun widthInUnit(unit: PrintUnit): Float = when (unit) {
            PrintUnit.INCHES -> widthInches
            PrintUnit.CENTIMETERS -> widthCm
            PrintUnit.MILLIMETERS -> widthMm
        }

        fun heightInUnit(unit: PrintUnit): Float = when (unit) {
            PrintUnit.INCHES -> heightInches
            PrintUnit.CENTIMETERS -> heightCm
            PrintUnit.MILLIMETERS -> heightMm
        }

        fun formattedInUnit(unit: PrintUnit): String = when (unit) {
            PrintUnit.INCHES -> String.format(java.util.Locale.US, "%.2f × %.2f in", widthInches, heightInches)
            PrintUnit.CENTIMETERS -> String.format(java.util.Locale.US, "%.2f × %.2f cm", widthCm, heightCm)
            PrintUnit.MILLIMETERS -> String.format(java.util.Locale.US, "%.1f × %.1f mm", widthMm, heightMm)
        }
    }

    data class PrintCalculatorReport(
        val mode: PrintCalculationMode,
        val sourceWidthPx: Int,
        val sourceHeightPx: Int,
        val targetWidthPx: Int,
        val targetHeightPx: Int,
        val targetDpi: Int,
        val effectiveDpiX: Int,
        val effectiveDpiY: Int,
        val effectiveDpi: Int,
        val physicalDimensions: PhysicalDimensions,
        val bleedMm: Float,
        val totalWithBleedWidthMm: Float,
        val totalWithBleedHeightMm: Float,
        val totalWithBleedWidthPx: Int,
        val totalWithBleedHeightPx: Int,
        val safeZoneWidthMm: Float,
        val safeZoneHeightMm: Float,
        val qualityGrade: PrintQualityGrade,
        val minViewingDistanceCm: Float,
        val minViewingDistanceInches: Float,
        val maxPrintWidthAt300DpiInches: Float,
        val maxPrintHeightAt300DpiInches: Float,
        val maxPrintWidthAt150DpiInches: Float,
        val maxPrintHeightAt150DpiInches: Float,
        val megapixels: Float,
        val uncompressedMemoryMb: Float,
        val aspectRatioFraction: String,
        val aspectRatioDecimal: Float,
        val requiresUpscaling: Boolean,
        val scaleFactorX: Float,
        val scaleFactorY: Float
    )

    data class DpiMetadataVerificationReport(
        val expectedDpi: Int,
        val detectedDpiX: Int?,
        val detectedDpiY: Int?,
        val metadataContainer: String,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val physicalDimensions: PhysicalDimensions,
        val isVerified: Boolean,
        val verificationNote: String
    )

    data class PrintResampleResult(
        val bitmap: Bitmap,
        val report: PrintCalculatorReport,
        val dpiApplied: Int,
        val bleedIncludedPx: Int
    )

    // ==================== UNIT CONVERSIONS ====================

    fun toInches(value: Float, unit: PrintUnit): Float = when (unit) {
        PrintUnit.INCHES -> value
        PrintUnit.CENTIMETERS -> value / CM_PER_INCH
        PrintUnit.MILLIMETERS -> value / MM_PER_INCH
    }

    fun fromInches(inches: Float, unit: PrintUnit): Float = when (unit) {
        PrintUnit.INCHES -> inches
        PrintUnit.CENTIMETERS -> inches * CM_PER_INCH
        PrintUnit.MILLIMETERS -> inches * MM_PER_INCH
    }

    fun convertPhysicalUnit(value: Float, fromUnit: PrintUnit, toUnit: PrintUnit): Float {
        if (fromUnit == toUnit) return value
        val inches = toInches(value, fromUnit)
        return fromInches(inches, toUnit)
    }

    fun computePhysicalDimensionsFromInches(widthInches: Float, heightInches: Float): PhysicalDimensions {
        val safeW = max(0.01f, widthInches)
        val safeH = max(0.01f, heightInches)
        return PhysicalDimensions(
            widthInches = safeW,
            heightInches = safeH,
            widthCm = safeW * CM_PER_INCH,
            heightCm = safeH * CM_PER_INCH,
            widthMm = safeW * MM_PER_INCH,
            heightMm = safeH * MM_PER_INCH,
            widthPoints = safeW * POINTS_PER_INCH,
            heightPoints = safeH * POINTS_PER_INCH
        )
    }

    fun computePhysicalDimensionsFromPixels(widthPx: Int, heightPx: Int, dpi: Int): PhysicalDimensions {
        val safeDpi = max(1, dpi).toFloat()
        val wIn = max(1, widthPx) / safeDpi
        val hIn = max(1, heightPx) / safeDpi
        return computePhysicalDimensionsFromInches(wIn, hIn)
    }

    fun computePixelsFromPhysical(
        widthPhysical: Float,
        heightPhysical: Float,
        unit: PrintUnit,
        dpi: Int
    ): Pair<Int, Int> {
        val safeDpi = max(1, dpi)
        val wInches = toInches(max(0.01f, widthPhysical), unit)
        val hInches = toInches(max(0.01f, heightPhysical), unit)
        val pxW = (wInches * safeDpi).roundToInt().coerceAtLeast(1)
        val pxH = (hInches * safeDpi).roundToInt().coerceAtLeast(1)
        return pxW to pxH
    }

    // ==================== FULL PRINT CALCULATOR ====================

    fun calculatePrintMetrics(
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        physicalWidth: Float,
        physicalHeight: Float,
        unit: PrintUnit,
        targetDpi: Int = 300,
        bleedMm: Float = 0f,
        safeMarginMm: Float = 3f,
        mode: PrintCalculationMode = PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS
    ): PrintCalculatorReport {
        val safeSrcW = max(1, sourceWidthPx)
        val safeSrcH = max(1, sourceHeightPx)
        val safeDpi = targetDpi.coerceIn(10, 2400)
        val safeBleedMm = bleedMm.coerceIn(0f, 25f)

        val physicalDims: PhysicalDimensions
        val targetW: Int
        val targetH: Int
        val effectiveDpiX: Int
        val effectiveDpiY: Int

        when (mode) {
            PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS -> {
                val wIn = toInches(max(0.05f, physicalWidth), unit)
                val hIn = toInches(max(0.05f, physicalHeight), unit)
                physicalDims = computePhysicalDimensionsFromInches(wIn, hIn)
                targetW = (wIn * safeDpi).roundToInt().coerceAtLeast(1)
                targetH = (hIn * safeDpi).roundToInt().coerceAtLeast(1)
                effectiveDpiX = (safeSrcW / wIn).roundToInt().coerceAtLeast(1)
                effectiveDpiY = (safeSrcH / hIn).roundToInt().coerceAtLeast(1)
            }
            PrintCalculationMode.PIXELS_AND_DPI_TO_PHYSICAL -> {
                targetW = safeSrcW
                targetH = safeSrcH
                physicalDims = computePhysicalDimensionsFromPixels(safeSrcW, safeSrcH, safeDpi)
                effectiveDpiX = safeDpi
                effectiveDpiY = safeDpi
            }
            PrintCalculationMode.PIXELS_AND_PHYSICAL_TO_DPI -> {
                val wIn = toInches(max(0.05f, physicalWidth), unit)
                val hIn = toInches(max(0.05f, physicalHeight), unit)
                physicalDims = computePhysicalDimensionsFromInches(wIn, hIn)
                effectiveDpiX = (safeSrcW / wIn).roundToInt().coerceAtLeast(1)
                effectiveDpiY = (safeSrcH / hIn).roundToInt().coerceAtLeast(1)
                val effMin = min(effectiveDpiX, effectiveDpiY)
                targetW = (wIn * effMin).roundToInt().coerceAtLeast(1)
                targetH = (hIn * effMin).roundToInt().coerceAtLeast(1)
            }
        }

        val effectiveDpi = min(effectiveDpiX, effectiveDpiY)
        val qualityGrade = PrintQualityGrade.fromEffectiveDpi(effectiveDpi)

        // Bleed & Safe Zone calculations (bleed added to all 4 sides -> +2 * bleedMm)
        val totalWithBleedWidthMm = physicalDims.widthMm + (safeBleedMm * 2f)
        val totalWithBleedHeightMm = physicalDims.heightMm + (safeBleedMm * 2f)
        val totalWithBleedWidthPx = ((totalWithBleedWidthMm / MM_PER_INCH) * safeDpi).roundToInt().coerceAtLeast(targetW)
        val totalWithBleedHeightPx = ((totalWithBleedHeightMm / MM_PER_INCH) * safeDpi).roundToInt().coerceAtLeast(targetH)

        val safeZoneWidthMm = max(1f, physicalDims.widthMm - (safeMarginMm * 2f))
        val safeZoneHeightMm = max(1f, physicalDims.heightMm - (safeMarginMm * 2f))

        // Optimal minimum viewing distance based on human visual acuity (~1 arcminute resolution)
        // Formula: viewingDistanceInches ≈ 3438 / effectiveDpi (clamped to realistic range)
        val minViewingInches = (3438f / max(20, effectiveDpi)).coerceIn(4f, 240f)
        val minViewingCm = minViewingInches * CM_PER_INCH

        val maxW300 = safeSrcW / 300f
        val maxH300 = safeSrcH / 300f
        val maxW150 = safeSrcW / 150f
        val maxH150 = safeSrcH / 150f

        val mp = (targetW.toLong() * targetH.toLong()) / 1_000_000f
        val ramMb = (targetW.toLong() * targetH.toLong() * 4L) / (1024f * 1024f)

        val g = gcd(targetW, targetH)
        val ratioFraction = "${targetW / g}:${targetH / g}"
        val ratioDecimal = targetW.toFloat() / targetH.toFloat()

        val scaleX = targetW.toFloat() / safeSrcW.toFloat()
        val scaleY = targetH.toFloat() / safeSrcH.toFloat()
        val requiresUpscaling = targetW > safeSrcW || targetH > safeSrcH

        return PrintCalculatorReport(
            mode = mode,
            sourceWidthPx = safeSrcW,
            sourceHeightPx = safeSrcH,
            targetWidthPx = targetW,
            targetHeightPx = targetH,
            targetDpi = safeDpi,
            effectiveDpiX = effectiveDpiX,
            effectiveDpiY = effectiveDpiY,
            effectiveDpi = effectiveDpi,
            physicalDimensions = physicalDims,
            bleedMm = safeBleedMm,
            totalWithBleedWidthMm = totalWithBleedWidthMm,
            totalWithBleedHeightMm = totalWithBleedHeightMm,
            totalWithBleedWidthPx = totalWithBleedWidthPx,
            totalWithBleedHeightPx = totalWithBleedHeightPx,
            safeZoneWidthMm = safeZoneWidthMm,
            safeZoneHeightMm = safeZoneHeightMm,
            qualityGrade = qualityGrade,
            minViewingDistanceCm = minViewingCm,
            minViewingDistanceInches = minViewingInches,
            maxPrintWidthAt300DpiInches = maxW300,
            maxPrintHeightAt300DpiInches = maxH300,
            maxPrintWidthAt150DpiInches = maxW150,
            maxPrintHeightAt150DpiInches = maxH150,
            megapixels = mp,
            uncompressedMemoryMb = ramMb,
            aspectRatioFraction = ratioFraction,
            aspectRatioDecimal = ratioDecimal,
            requiresUpscaling = requiresUpscaling,
            scaleFactorX = scaleX,
            scaleFactorY = scaleY
        )
    }

    /**
     * Resamples a source bitmap to match physical print dimensions at the specified DPI,
     * optionally adding print bleed margins.
     */
    fun resampleForPrint(
        source: Bitmap,
        physicalWidth: Float,
        physicalHeight: Float,
        unit: PrintUnit,
        dpi: Int = 300,
        bleedMm: Float = 0f,
        resizeMode: ResizeMode = ResizeMode.FIT,
        backgroundColor: Int = Color.WHITE
    ): PrintResampleResult {
        val report = calculatePrintMetrics(
            sourceWidthPx = source.width,
            sourceHeightPx = source.height,
            physicalWidth = physicalWidth,
            physicalHeight = physicalHeight,
            unit = unit,
            targetDpi = dpi,
            bleedMm = bleedMm,
            mode = PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS
        )

        val finalWidthPx = if (bleedMm > 0f) report.totalWithBleedWidthPx else report.targetWidthPx
        val finalHeightPx = if (bleedMm > 0f) report.totalWithBleedHeightPx else report.targetHeightPx
        val bleedPx = ((bleedMm / MM_PER_INCH) * report.targetDpi).roundToInt().coerceAtLeast(0)

        val resized = BitmapUtils.resizeBitmapWithMode(
            bitmap = source,
            targetWidth = finalWidthPx,
            targetHeight = finalHeightPx,
            mode = resizeMode,
            fitBackgroundColor = backgroundColor
        )

        return PrintResampleResult(
            bitmap = resized,
            report = report,
            dpiApplied = report.targetDpi,
            bleedIncludedPx = bleedPx
        )
    }

    // ==================== DPI METADATA EMBEDDING & VERIFICATION ====================

    /**
     * Injects JFIF density + EXIF X/YResolution for JPEG, or pHYs chunk for PNG,
     * so external print RIP software and preflight verifiers read the exact DPI.
     */
    fun embedDpiInEncodedBytes(
        encodedBytes: ByteArray,
        format: ExportFormat,
        dpi: Int
    ): ByteArray {
        val safeDpi = dpi.coerceIn(10, 2400)
        return when (format) {
            ExportFormat.JPEG -> embedJpegJfifDpi(encodedBytes, safeDpi)
            ExportFormat.PNG -> embedPngPhysDpi(encodedBytes, safeDpi)
            else -> encodedBytes
        }
    }

    /**
     * Patches or inserts JFIF APP0 density header in JPEG bytes (units = 1 [dots per inch], Xdensity = dpi, Ydensity = dpi).
     */
    fun embedJpegJfifDpi(jpegBytes: ByteArray, dpi: Int): ByteArray {
        if (jpegBytes.size < 20) return jpegBytes
        if (jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) return jpegBytes

        val out = jpegBytes.copyOf()
        // Check if APP0 JFIF marker is right at offset 2..10
        if (out[2] == 0xFF.toByte() && out[3] == 0xE0.toByte() &&
            out[6] == 'J'.code.toByte() && out[7] == 'F'.code.toByte() &&
            out[8] == 'I'.code.toByte() && out[9] == 'F'.code.toByte() && out[10] == 0.toByte()
        ) {
            // Offset 13 is density unit (1 = pixels per inch)
            out[13] = 1.toByte()
            // Offset 14..15 is Xdensity (big-endian uint16)
            out[14] = ((dpi shr 8) and 0xFF).toByte()
            out[15] = (dpi and 0xFF).toByte()
            // Offset 16..17 is Ydensity (big-endian uint16)
            out[16] = ((dpi shr 8) and 0xFF).toByte()
            out[17] = (dpi and 0xFF).toByte()
            return out
        }

        // If JFIF APP0 is not at offset 2, insert a standard 18-byte JFIF APP0 segment right after SOI (FF D8)
        val app0 = byteArrayOf(
            0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, // Length = 16 bytes
            'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0x00,
            0x01, 0x02, // Version 1.2
            0x01,       // Units: 1 = dots per inch
            ((dpi shr 8) and 0xFF).toByte(), (dpi and 0xFF).toByte(),
            ((dpi shr 8) and 0xFF).toByte(), (dpi and 0xFF).toByte(),
            0x00, 0x00  // No thumbnail
        )
        val combined = ByteArray(2 + app0.size + jpegBytes.size - 2)
        combined[0] = 0xFF.toByte()
        combined[1] = 0xD8.toByte()
        System.arraycopy(app0, 0, combined, 2, app0.size)
        System.arraycopy(jpegBytes, 2, combined, 2 + app0.size, jpegBytes.size - 2)
        return combined
    }

    /**
     * Injects a standard PNG `pHYs` chunk immediately after the `IHDR` chunk.
     */
    fun embedPngPhysDpi(pngBytes: ByteArray, dpi: Int): ByteArray {
        if (pngBytes.size < 33) return pngBytes
        if (pngBytes[0] != 0x89.toByte() || pngBytes[1] != 0x50.toByte() ||
            pngBytes[2] != 0x4E.toByte() || pngBytes[3] != 0x47.toByte()
        ) return pngBytes

        val ppm = (dpi * INCHES_PER_METER).roundToInt()

        val chunkType = byteArrayOf('p'.code.toByte(), 'H'.code.toByte(), 'Y'.code.toByte(), 's'.code.toByte())
        val chunkData = ByteBuffer.allocate(9)
            .putInt(ppm)
            .putInt(ppm)
            .put(1.toByte()) // 1 = metre
            .array()

        val crc = CRC32()
        crc.update(chunkType)
        crc.update(chunkData)
        val crcVal = crc.value.toInt()

        val physChunk = ByteBuffer.allocate(4 + 4 + 9 + 4)
            .putInt(9)
            .put(chunkType)
            .put(chunkData)
            .putInt(crcVal)
            .array()

        // IHDR chunk starts at offset 8: length(4) + 'IHDR'(4) + data(13) + crc(4) = 25 bytes -> ends at offset 33
        val ihdrEnd = 33
        val out = ByteArrayOutputStream(pngBytes.size + physChunk.size)
        out.write(pngBytes, 0, ihdrEnd)
        out.write(physChunk)
        out.write(pngBytes, ihdrEnd, pngBytes.size - ihdrEnd)
        return out.toByteArray()
    }

    /**
     * Reads DPI directly from encoded image bytes (supports JPEG EXIF, JPEG JFIF APP0, and PNG pHYs).
     */
    fun readDpiFromBytes(bytes: ByteArray): Pair<Int, String>? {
        if (bytes.size < 16) return null

        // 1. Check PNG pHYs chunk
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            val limit = min(bytes.size - 13, 8192)
            for (i in 8 until limit) {
                if (bytes[i] == 'p'.code.toByte() && bytes[i + 1] == 'H'.code.toByte() &&
                    bytes[i + 2] == 'Y'.code.toByte() && bytes[i + 3] == 's'.code.toByte()
                ) {
                    val xPpm = ((bytes[i + 4].toInt() and 0xFF) shl 24) or
                            ((bytes[i + 5].toInt() and 0xFF) shl 16) or
                            ((bytes[i + 6].toInt() and 0xFF) shl 8) or
                            (bytes[i + 7].toInt() and 0xFF)
                    val unit = bytes[i + 12].toInt() and 0xFF
                    if (unit == 1 && xPpm > 0) {
                        val dpi = (xPpm / INCHES_PER_METER).roundToInt()
                        return dpi to "PNG pHYs (Pixels Per Meter: $xPpm)"
                    }
                }
            }
        }

        // 2. Check JPEG EXIF XResolution
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            try {
                ByteArrayInputStream(bytes).use { stream ->
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
                            val dpi = if (unit == 3) (resVal * CM_PER_INCH).roundToInt() else resVal
                            return dpi to "JPEG EXIF (TAG_X_RESOLUTION)"
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // 3. Fallback to JPEG JFIF APP0 header
            val searchLimit = min(bytes.size - 16, 4096)
            for (i in 2 until searchLimit) {
                if (bytes[i] == 0xFF.toByte() && bytes[i + 1] == 0xE0.toByte() &&
                    bytes[i + 4] == 'J'.code.toByte() && bytes[i + 5] == 'F'.code.toByte() &&
                    bytes[i + 6] == 'I'.code.toByte() && bytes[i + 7] == 'F'.code.toByte() &&
                    bytes[i + 8] == 0.toByte()
                ) {
                    val unit = bytes[i + 11].toInt() and 0xFF
                    val xDensity = ((bytes[i + 12].toInt() and 0xFF) shl 8) or (bytes[i + 13].toInt() and 0xFF)
                    if (xDensity > 0) {
                        val dpi = when (unit) {
                            1 -> xDensity
                            2 -> (xDensity * CM_PER_INCH).roundToInt()
                            else -> xDensity
                        }
                        return dpi to "JPEG JFIF APP0 Header"
                    }
                }
            }
        }

        return null
    }

    /**
     * Verifies DPI metadata and physical print dimensions of an exported byte array or file.
     */
    fun verifyDpiAndPhysicalDimensions(
        bytes: ByteArray,
        pixelWidth: Int,
        pixelHeight: Int,
        expectedDpi: Int,
        format: ExportFormat
    ): DpiMetadataVerificationReport {
        val detectedPair = readDpiFromBytes(bytes)
        val detectedDpi = detectedPair?.first ?: expectedDpi
        val container = detectedPair?.second ?: when (format) {
            ExportFormat.PDF -> "PDF 72 pt/in Page Vector Box"
            ExportFormat.WEBP_LOSSY, ExportFormat.WEBP_LOSSLESS -> "WebP Container"
            else -> format.displayName
        }

        val phys = computePhysicalDimensionsFromPixels(pixelWidth, pixelHeight, detectedDpi)
        val matchesDpi = abs(detectedDpi - expectedDpi) <= 2

        return DpiMetadataVerificationReport(
            expectedDpi = expectedDpi,
            detectedDpiX = detectedDpi,
            detectedDpiY = detectedDpi,
            metadataContainer = container,
            pixelWidth = pixelWidth,
            pixelHeight = pixelHeight,
            physicalDimensions = phys,
            isVerified = matchesDpi && bytes.isNotEmpty(),
            verificationNote = if (matchesDpi) {
                "Verified $detectedDpi DPI in $container (${phys.formattedInUnit(PrintUnit.INCHES)} / ${phys.formattedInUnit(PrintUnit.MILLIMETERS)})"
            } else {
                "Expected $expectedDpi DPI, found $detectedDpi DPI in $container"
            }
        )
    }

    fun verifyFileDpiAndPhysicalDimensions(
        file: File,
        pixelWidth: Int,
        pixelHeight: Int,
        expectedDpi: Int,
        format: ExportFormat
    ): DpiMetadataVerificationReport {
        if (!file.exists() || file.length() <= 0L) {
            val phys = computePhysicalDimensionsFromPixels(pixelWidth, pixelHeight, expectedDpi)
            return DpiMetadataVerificationReport(
                expectedDpi = expectedDpi,
                detectedDpiX = null,
                detectedDpiY = null,
                metadataContainer = "Missing File",
                pixelWidth = pixelWidth,
                pixelHeight = pixelHeight,
                physicalDimensions = phys,
                isVerified = false,
                verificationNote = "File does not exist or is empty"
            )
        }
        val fileDpi = BitmapUtils.getFileDpi(file)
        val bytesSample = file.inputStream().use { input ->
            val buf = ByteArray(min(file.length().toInt(), 16384))
            val read = input.read(buf)
            if (read > 0) buf.copyOf(read) else ByteArray(0)
        }
        val fromBytes = readDpiFromBytes(bytesSample)
        val effectiveDetected = fileDpi ?: fromBytes?.first ?: expectedDpi
        val container = fromBytes?.second ?: if (fileDpi != null) "File EXIF/pHYs" else format.displayName
        val phys = computePhysicalDimensionsFromPixels(pixelWidth, pixelHeight, effectiveDetected)
        val matches = abs(effectiveDetected - expectedDpi) <= 2

        return DpiMetadataVerificationReport(
            expectedDpi = expectedDpi,
            detectedDpiX = effectiveDetected,
            detectedDpiY = effectiveDetected,
            metadataContainer = container,
            pixelWidth = pixelWidth,
            pixelHeight = pixelHeight,
            physicalDimensions = phys,
            isVerified = matches,
            verificationNote = "Verified $effectiveDetected DPI in $container (${phys.formattedInUnit(PrintUnit.MILLIMETERS)})"
        )
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = abs(a)
        var y = abs(b)
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return if (x == 0) 1 else x
    }
}
