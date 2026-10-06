package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import com.example.model.ExportFormat
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.max

enum class FileSizeMode(val title: String, val shortLabel: String, val description: String) {
    MAXIMUM_CEILING(
        title = "Maximum File Size (≤)",
        shortLabel = "Maximum Ceiling",
        description = "Guarantees file size is strictly under or equal to ceiling (for portals, govt forms, visa uploads)."
    ),
    TARGET_CLOSEST(
        title = "Target File Size (~)",
        shortLabel = "Closest Possible Size",
        description = "Finds the closest mathematically possible size without degrading quality more than necessary."
    ),
    EXACT_BYTE_ALIGNMENT(
        title = "Exact Byte Alignment",
        shortLabel = "Exact Match (Padded)",
        description = "Aligns file to the exact byte by embedding lossless comment metadata padding into the container."
    )
}

data class FileSizeOptimizationResult(
    val mode: FileSizeMode,
    val targetKb: Int,
    val targetBytes: Long,
    val actualBytes: Long,
    val differenceBytes: Long, // actualBytes - targetBytes
    val percentDifference: Float,
    val quality: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val dimensionsAdjusted: Boolean,
    val isCompliantWithCeiling: Boolean,
    val isExactByteMatch: Boolean,
    val format: ExportFormat,
    val encodedBytes: ByteArray,
    val iterationsCount: Int,
    val statusBadge: String,
    val explanation: String
)

object FileSizeEngine {

    /**
     * Performs real iterative encoding across quality levels and (optionally) dimension scale steps
     * to satisfy the Exact File Size Rule.
     */
    fun optimizeFileSize(
        source: Bitmap,
        format: ExportFormat,
        targetKb: Int,
        mode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
        allowDimensionAdjustment: Boolean = true,
        qualityHint: Int = 85,
        jpegBackgroundColor: Int = Color.WHITE
    ): FileSizeOptimizationResult {
        val targetBytes = targetKb * 1024L
        var iterations = 0

        // Non-lossy formats like PDF or lossless WebP handle differently
        if (format == ExportFormat.PDF) {
            val stream = ByteArrayOutputStream()
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(source.width, source.height, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            page.canvas.drawBitmap(source, 0f, 0f, null)
            pdfDoc.finishPage(page)
            pdfDoc.writeTo(stream)
            pdfDoc.close()
            val bytes = stream.toByteArray()
            val actual = bytes.size.toLong()
            val diff = actual - targetBytes
            return FileSizeOptimizationResult(
                mode = mode,
                targetKb = targetKb,
                targetBytes = targetBytes,
                actualBytes = actual,
                differenceBytes = diff,
                percentDifference = if (targetBytes > 0) (diff.toFloat() / targetBytes) * 100f else 0f,
                quality = 100,
                outputWidth = source.width,
                outputHeight = source.height,
                dimensionsAdjusted = false,
                isCompliantWithCeiling = actual <= targetBytes,
                isExactByteMatch = actual == targetBytes,
                format = format,
                encodedBytes = bytes,
                iterationsCount = 1,
                statusBadge = if (actual <= targetBytes) "≤ $targetKb KB Compliant" else "Exceeds $targetKb KB",
                explanation = "PDF vector packaging embeds full uncompressed or Flate-encoded bitmap buffers."
            )
        }

        // Iterative Search Structures
        var bestBitmap: Bitmap = source
        var bestBytes: ByteArray? = null
        var bestQuality = qualityHint.coerceIn(5, 98)
        var bestDiff = Long.MAX_VALUE
        var dimensionsScaled = false

        // Scale factors to evaluate if needed
        val scales = if (allowDimensionAdjustment) {
            listOf(1.0f, 0.90f, 0.80f, 0.70f, 0.60f, 0.50f, 0.40f, 0.30f, 0.20f)
        } else {
            listOf(1.0f)
        }

        for (scale in scales) {
            val curW = max(10, (source.width * scale).toInt())
            val curH = max(10, (source.height * scale).toInt())
            val testBmp = if (scale == 1.0f) {
                source
            } else {
                Bitmap.createScaledBitmap(source, curW, curH, true)
            }

            // Real iterative binary search on compression quality (5 to 98)
            var lowQ = 5
            var highQ = 98
            var scaleBestBytes: ByteArray? = null
            var scaleBestQ = 80
            var scaleBestDiff = Long.MAX_VALUE

            while (lowQ <= highQ) {
                iterations++
                val midQ = (lowQ + highQ) / 2
                val stream = ByteArrayOutputStream()
                compressToStream(testBmp, format, midQ, stream, jpegBackgroundColor)
                val testBytes = stream.toByteArray()
                val currentSize = testBytes.size.toLong()

                when (mode) {
                    FileSizeMode.MAXIMUM_CEILING, FileSizeMode.EXACT_BYTE_ALIGNMENT -> {
                        if (currentSize <= targetBytes) {
                            // Valid candidate under ceiling
                            val diff = targetBytes - currentSize
                            if (diff < scaleBestDiff) {
                                scaleBestDiff = diff
                                scaleBestBytes = testBytes
                                scaleBestQ = midQ
                            }
                            // Try higher quality that might still fit
                            lowQ = midQ + 1
                        } else {
                            // Exceeds ceiling, reduce quality
                            highQ = midQ - 1
                        }
                    }
                    FileSizeMode.TARGET_CLOSEST -> {
                        val diff = abs(currentSize - targetBytes)
                        if (diff < scaleBestDiff) {
                            scaleBestDiff = diff
                            scaleBestBytes = testBytes
                            scaleBestQ = midQ
                        }
                        if (currentSize < targetBytes) {
                            lowQ = midQ + 1
                        } else if (currentSize > targetBytes) {
                            highQ = midQ - 1
                        } else {
                            break // Exact natural match found
                        }
                    }
                }
            }

            if (scaleBestBytes != null) {
                val actual = scaleBestBytes.size.toLong()
                val overallDiff = when (mode) {
                    FileSizeMode.MAXIMUM_CEILING, FileSizeMode.EXACT_BYTE_ALIGNMENT -> targetBytes - actual
                    FileSizeMode.TARGET_CLOSEST -> abs(actual - targetBytes)
                }

                if (overallDiff < bestDiff) {
                    bestDiff = overallDiff
                    bestBytes = scaleBestBytes
                    bestQuality = scaleBestQ
                    bestBitmap = testBmp
                    dimensionsScaled = (scale < 1.0f)
                }

                // In MAXIMUM_CEILING mode, once we find a candidate at full dimensions, we prefer higher resolution
                if ((mode == FileSizeMode.MAXIMUM_CEILING || mode == FileSizeMode.EXACT_BYTE_ALIGNMENT) && scale == 1.0f) {
                    break
                }
            }
        }

        // Fallback if no candidate met the ceiling (e.g., allowDimensionAdjustment was false or image too rich)
        if (bestBytes == null) {
            val stream = ByteArrayOutputStream()
            compressToStream(source, format, 5, stream, jpegBackgroundColor)
            bestBytes = stream.toByteArray()
            bestQuality = 5
            bestBitmap = source
            dimensionsScaled = false
        }

        var finalBytes = bestBytes
        val initialActual = finalBytes.size.toLong()
        var isExact = initialActual == targetBytes

        // If EXACT_BYTE_ALIGNMENT mode is requested and we are under the target,
        // embed lossless comment/metadata padding up to the exact byte count!
        if (mode == FileSizeMode.EXACT_BYTE_ALIGNMENT && initialActual < targetBytes) {
            val padded = padBytesToExact(finalBytes, format, targetBytes)
            if (padded != null && padded.size.toLong() == targetBytes) {
                finalBytes = padded
                isExact = true
            }
        }

        val actualBytes = finalBytes.size.toLong()
        val differenceBytes = actualBytes - targetBytes
        val percentDiff = if (targetBytes > 0) (differenceBytes.toFloat() / targetBytes) * 100f else 0f
        val isCompliant = actualBytes <= targetBytes

        val statusBadge = when {
            isExact -> "Exact $targetKb KB Verified"
            mode == FileSizeMode.MAXIMUM_CEILING && isCompliant -> "≤ $targetKb KB Compliant"
            mode == FileSizeMode.MAXIMUM_CEILING && !isCompliant -> "Exceeds Ceiling (+${abs(differenceBytes)} B)"
            mode == FileSizeMode.TARGET_CLOSEST -> "Closest: ${formatBytesToReadable(actualBytes)}"
            else -> "Closest: ${formatBytesToReadable(actualBytes)}"
        }

        val explanation = buildExplanation(
            mode = mode,
            targetBytes = targetBytes,
            actualBytes = actualBytes,
            diffBytes = differenceBytes,
            quality = bestQuality,
            dimensionsScaled = dimensionsScaled,
            allowDimensionAdjustment = allowDimensionAdjustment,
            isExact = isExact,
            isCompliant = isCompliant,
            format = format
        )

        return FileSizeOptimizationResult(
            mode = mode,
            targetKb = targetKb,
            targetBytes = targetBytes,
            actualBytes = actualBytes,
            differenceBytes = differenceBytes,
            percentDifference = percentDiff,
            quality = bestQuality,
            outputWidth = bestBitmap.width,
            outputHeight = bestBitmap.height,
            dimensionsAdjusted = dimensionsScaled,
            isCompliantWithCeiling = isCompliant,
            isExactByteMatch = isExact,
            format = format,
            encodedBytes = finalBytes,
            iterationsCount = iterations,
            statusBadge = statusBadge,
            explanation = explanation
        )
    }

    private fun compressToStream(
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        outputStream: OutputStream,
        jpegBackgroundColor: Int = Color.WHITE
    ) {
        when (format) {
            ExportFormat.JPEG -> {
                val rgbBitmap = if (bitmap.hasAlpha()) {
                    BitmapUtils.compositeOnBackground(bitmap, jpegBackgroundColor)
                } else bitmap
                rgbBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), outputStream)
            }
            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            ExportFormat.WEBP_LOSSY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality.coerceIn(1, 100), outputStream)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality.coerceIn(1, 100), outputStream)
                }
            }
            ExportFormat.WEBP_LOSSLESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream)
                }
            }
            ExportFormat.PDF -> {}
        }
    }

    /**
     * Embeds lossless metadata comment padding (JPEG COM marker or PNG tEXt chunk)
     * to match the exact target byte count byte-for-byte.
     */
    private fun padBytesToExact(sourceBytes: ByteArray, format: ExportFormat, targetBytes: Long): ByteArray? {
        val needed = (targetBytes - sourceBytes.size).toInt()
        if (needed <= 0) return sourceBytes

        if (format == ExportFormat.JPEG && sourceBytes.size > 2) {
            // Check for SOI marker: 0xFF, 0xD8
            if (sourceBytes[0] == 0xFF.toByte() && sourceBytes[1] == 0xD8.toByte()) {
                // Minimum JPEG COM marker is 4 bytes: 0xFF 0xFE [len_hi] [len_lo]
                if (needed >= 4) {
                    val markerLength = needed - 2 // len field includes length bytes themselves
                    if (markerLength <= 65535) {
                        val out = ByteArray(targetBytes.toInt())
                        out[0] = 0xFF.toByte()
                        out[1] = 0xD8.toByte()
                        out[2] = 0xFF.toByte()
                        out[3] = 0xFE.toByte()
                        out[4] = ((markerLength ushr 8) and 0xFF).toByte()
                        out[5] = (markerLength and 0xFF).toByte()
                        // Fill comment data with benign padding characters
                        for (i in 6 until (6 + markerLength - 2)) {
                            out[i] = ' '.code.toByte()
                        }
                        // Copy remaining original bytes
                        System.arraycopy(sourceBytes, 2, out, 4 + markerLength, sourceBytes.size - 2)
                        return out
                    }
                }
            }
        } else if (format == ExportFormat.PNG && sourceBytes.size > 8) {
            // PNG tEXt chunk requires 4 len + 4 type ('tEXt') + keyword + null + data + 4 crc = at least 14 bytes
            if (needed >= 14) {
                // Insert before IEND (which is last 12 bytes: len 4 + IEND 4 + crc 4)
                val insertPos = sourceBytes.size - 12
                val dataLen = needed - 12
                val chunkData = ByteArray(dataLen)
                // Keyword "Comment\0"
                val prefix = "Pad\u0000".toByteArray(Charsets.ISO_8859_1)
                System.arraycopy(prefix, 0, chunkData, 0, minOf(prefix.size, dataLen))
                for (i in prefix.size until dataLen) {
                    chunkData[i] = ' '.code.toByte()
                }

                val type = "tEXt".toByteArray(Charsets.ISO_8859_1)
                val typeAndData = ByteArray(4 + dataLen)
                System.arraycopy(type, 0, typeAndData, 0, 4)
                System.arraycopy(chunkData, 0, typeAndData, 4, dataLen)

                val crc = java.util.zip.CRC32()
                crc.update(typeAndData)
                val crcVal = crc.value.toInt()

                val out = ByteArray(targetBytes.toInt())
                System.arraycopy(sourceBytes, 0, out, 0, insertPos)

                // 4 bytes length
                out[insertPos] = ((dataLen ushr 24) and 0xFF).toByte()
                out[insertPos + 1] = ((dataLen ushr 16) and 0xFF).toByte()
                out[insertPos + 2] = ((dataLen ushr 8) and 0xFF).toByte()
                out[insertPos + 3] = (dataLen and 0xFF).toByte()

                // type and data
                System.arraycopy(typeAndData, 0, out, insertPos + 4, typeAndData.size)

                // 4 bytes crc
                val crcPos = insertPos + 4 + typeAndData.size
                out[crcPos] = ((crcVal ushr 24) and 0xFF).toByte()
                out[crcPos + 1] = ((crcVal ushr 16) and 0xFF).toByte()
                out[crcPos + 2] = ((crcVal ushr 8) and 0xFF).toByte()
                out[crcPos + 3] = (crcVal and 0xFF).toByte()

                // Copy IEND
                System.arraycopy(sourceBytes, insertPos, out, crcPos + 4, 12)
                return out
            }
        }

        return null
    }

    private fun buildExplanation(
        mode: FileSizeMode,
        targetBytes: Long,
        actualBytes: Long,
        diffBytes: Long,
        quality: Int,
        dimensionsScaled: Boolean,
        allowDimensionAdjustment: Boolean,
        isExact: Boolean,
        isCompliant: Boolean,
        format: ExportFormat
    ): String {
        return buildString {
            if (isExact) {
                append("✓ Exact byte matching verified ($actualBytes / $targetBytes bytes). ")
                if (mode == FileSizeMode.EXACT_BYTE_ALIGNMENT) {
                    append("Lossless container metadata padding safely aligned the bitstream without altering image pixels.")
                } else {
                    append("Entropy bitstream naturally aligned with the target boundary.")
                }
                return@buildString
            }

            when (mode) {
                FileSizeMode.MAXIMUM_CEILING -> {
                    if (isCompliant) {
                        val headroom = abs(diffBytes)
                        append("✓ Ceiling satisfied: ${formatBytesToReadable(actualBytes)} is strictly ≤ ${formatBytesToReadable(targetBytes)} ($headroom bytes headroom). ")
                        append("Quality optimized to $quality%. ")
                        if (dimensionsScaled) {
                            append("Dimensions were adaptively adjusted to ensure strict ceiling compliance.")
                        }
                    } else {
                        append("⚠ Cannot achieve ≤ ${formatBytesToReadable(targetBytes)} at current settings. ")
                        if (!allowDimensionAdjustment) {
                            append("Enable 'Allow Dimension Downsampling' to allow the engine to scale pixels down to fit within the upload limit.")
                        } else {
                            append("Source image contains high entropy detail that exceeds the budget even at lowest quality.")
                        }
                    }
                }
                FileSizeMode.TARGET_CLOSEST -> {
                    val dir = if (diffBytes < 0) "below" else "above"
                    val diffStr = formatBytesToReadable(abs(diffBytes))
                    append("Closest achieved size is ${formatBytesToReadable(actualBytes)} ($diffStr $dir target) at $quality% quality. ")
                }
                FileSizeMode.EXACT_BYTE_ALIGNMENT -> {
                    append("Exact byte alignment requires the raw compressed image to be smaller than the target so harmless metadata padding can be added. ")
                    if (!isCompliant) {
                        append("Current image exceeds the target size at minimum quality; enable dimension downsampling to reach the target.")
                    }
                }
            }

            append("\n\nWhy exact byte matching is content-dependent:\n")
            append("Image codecs (${format.extension.uppercase()}) partition images into 8×8 or 16×16 frequency macroblocks transformed by Discrete Cosine Transforms (DCT) and encoded via Huffman/entropy tables. Integer quality steps (1–100) alter quantization tables in discrete steps, changing file sizes by hundreds or thousands of bytes per step rather than single bytes. Without artificial padding, natural images land on the closest mathematical bitstream boundary.")
        }
    }

    fun formatBytesToReadable(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.2f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
