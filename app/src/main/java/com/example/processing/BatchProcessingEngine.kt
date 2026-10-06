package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.BatchCompressionMode
import com.example.model.BatchConfig
import com.example.model.BatchFilterOption
import com.example.model.BatchItem
import com.example.model.BatchResizeOption
import com.example.model.BatchSortOption
import com.example.model.BatchStatus
import com.example.model.BatchSummaryReport
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.OutputFileConfiguration
import com.example.model.ResizeMode
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PHASE 9 — PRODUCTION BATCH PROCESSING ENGINE
 *
 * Implements:
 * 1. Multi-select queue inspection, deduplication, sorting, and filtering
 * 2. Batch Resize (Original, Scale %, Same Width, Same Height, Longest Edge, Exact W×H, Physical Print mm/cm/in, Do Not Upscale)
 * 3. Batch Compression (Quality %, Preset Tiers, Exact Target KB, Max Ceiling KB)
 * 4. Batch Format Conversion (Keep Original Format or Convert to JPEG, PNG, WEBP Lossy, WEBP Lossless, PDF with Alpha Matte)
 * 5. Batch Metadata & Binary DPI (Keep All, Strip All, Selective Scrub GPS/Camera/Device/Date, Custom Artist/Copyright, Binary DPI Injection)
 * 6. Granular Stage Progress & Telemetry
 * 7. Cooperative Cancellation (AtomicBoolean + Coroutine Cancellation safe)
 * 8. Single & Multi-Item Retry and Reprocess Mechanics
 */
object BatchProcessingEngine {

    data class BatchItemStageProgress(
        val itemIndex: Int,
        val itemId: String,
        val fileName: String,
        val stageLabel: String,
        val itemProgress: Float,
        val overallProgress: Float
    )

    /**
     * Inspects a list of URIs and constructs rich [BatchItem] entries with detected format,
     * dimensions, file size, and EXIF/GPS presence. Optionally skips duplicate URIs already in [existingItems].
     */
    fun inspectAndCreateBatchItems(
        context: Context,
        uris: List<Uri>,
        existingItems: List<BatchItem> = emptyList(),
        skipDuplicates: Boolean = true
    ): List<BatchItem> {
        val existingUriStrings = if (skipDuplicates) {
            existingItems.map { it.uri.toString() }.toHashSet()
        } else {
            emptySet()
        }

        val added = mutableListOf<BatchItem>()
        val seenInBatch = mutableSetOf<String>()

        for (uri in uris) {
            val uriStr = uri.toString()
            if (skipDuplicates && (existingUriStrings.contains(uriStr) || !seenInBatch.add(uriStr))) {
                continue
            }

            val header = BitmapUtils.getImageHeader(context, uri)
            val exif = ExifManager.readExif(context, uri)
            val hasExif = exif.totalIdentifiedTagsCount > 0

            added.add(
                BatchItem(
                    uri = uri,
                    originalName = header.fileName.ifBlank {
                        uri.lastPathSegment?.substringAfterLast('/') ?: "image_${System.currentTimeMillis()}.jpg"
                    },
                    originalSize = header.fileSizeBytes,
                    originalWidth = header.width,
                    originalHeight = header.height,
                    status = BatchStatus.PENDING,
                    selected = true,
                    detectedFormat = header.detectedFormat,
                    hasExif = hasExif,
                    hasGps = exif.hasGps,
                    stageLabel = "Queued"
                )
            )
        }
        return added
    }

    /**
     * Resolves the output [ExportFormat] for a given [BatchItem] and [BatchConfig].
     * When [BatchConfig.keepOriginalFormat] is true, maps the item's detected input format to the matching [ExportFormat].
     */
    fun resolveOutputFormat(item: BatchItem, config: BatchConfig): ExportFormat {
        if (!config.keepOriginalFormat) return config.format
        return when (item.detectedFormat) {
            DetectedImageFormat.PNG -> ExportFormat.PNG
            DetectedImageFormat.WEBP -> ExportFormat.WEBP_LOSSY
            DetectedImageFormat.JPEG,
            DetectedImageFormat.HEIC,
            DetectedImageFormat.BMP,
            DetectedImageFormat.GIF,
            DetectedImageFormat.UNKNOWN -> ExportFormat.JPEG
        }
    }

    /**
     * Resolves the effective quality percentage (1–100) for [config].
     */
    fun resolveEffectiveQuality(config: BatchConfig): Int {
        return when (config.compressionMode) {
            BatchCompressionMode.PRESET -> config.compressionPreset.defaultQuality.coerceIn(1, 100)
            else -> config.quality.coerceIn(1, 100)
        }
    }

    /**
     * Resolves the effective target/ceiling KB and [FileSizeMode] for [config].
     */
    fun resolveFileSizeConstraint(config: BatchConfig): Pair<Int?, FileSizeMode> {
        return when (config.compressionMode) {
            BatchCompressionMode.EXACT_TARGET_KB -> {
                val kb = config.targetMaxKb?.takeIf { it > 0 }
                kb to FileSizeMode.TARGET_CLOSEST
            }
            BatchCompressionMode.MAX_CEILING_KB -> {
                val kb = config.targetMaxKb?.takeIf { it > 0 }
                kb to FileSizeMode.MAXIMUM_CEILING
            }
            BatchCompressionMode.QUALITY,
            BatchCompressionMode.PRESET -> {
                val kb = config.targetMaxKb?.takeIf { it > 0 }
                kb to FileSizeMode.MAXIMUM_CEILING
            }
        }
    }

    /**
     * Computes the exact target `(width, height)` in pixels for an image of `(srcWidth, srcHeight)`
     * according to [BatchConfig]. Respects [BatchConfig.doNotUpscale].
     */
    fun computeBatchTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        config: BatchConfig
    ): Pair<Int, Int> {
        val safeSrcW = srcWidth.coerceAtLeast(1)
        val safeSrcH = srcHeight.coerceAtLeast(1)

        val (rawTargetW, rawTargetH) = when (config.resizeOption) {
            BatchResizeOption.ORIGINAL -> safeSrcW to safeSrcH

            BatchResizeOption.PERCENTAGE -> {
                val pct = if (config.doNotUpscale) min(config.scalePercent, 100) else config.scalePercent
                val factor = (pct / 100f).coerceIn(0.01f, 5.0f)
                val w = (safeSrcW * factor).roundToInt().coerceAtLeast(1)
                val h = (safeSrcH * factor).roundToInt().coerceAtLeast(1)
                w to h
            }

            BatchResizeOption.SAME_WIDTH -> {
                val targetW = if (config.doNotUpscale) {
                    min(config.sameWidth.coerceAtLeast(1), safeSrcW)
                } else {
                    config.sameWidth.coerceAtLeast(1)
                }
                val targetH = (safeSrcH.toFloat() / safeSrcW.toFloat() * targetW).roundToInt().coerceAtLeast(1)
                targetW to targetH
            }

            BatchResizeOption.SAME_HEIGHT -> {
                val targetH = if (config.doNotUpscale) {
                    min(config.sameHeight.coerceAtLeast(1), safeSrcH)
                } else {
                    config.sameHeight.coerceAtLeast(1)
                }
                val targetW = (safeSrcW.toFloat() / safeSrcH.toFloat() * targetH).roundToInt().coerceAtLeast(1)
                targetW to targetH
            }

            BatchResizeOption.LONGEST_EDGE -> {
                val maxEdge = config.longestEdge.coerceAtLeast(1)
                val currentLongest = max(safeSrcW, safeSrcH)
                if (config.doNotUpscale && currentLongest <= maxEdge) {
                    safeSrcW to safeSrcH
                } else {
                    val scale = maxEdge.toFloat() / currentLongest.toFloat()
                    val w = (safeSrcW * scale).roundToInt().coerceAtLeast(1)
                    val h = (safeSrcH * scale).roundToInt().coerceAtLeast(1)
                    w to h
                }
            }

            BatchResizeOption.EXACT_DIMENSIONS -> {
                val reqW = config.exactWidth.coerceAtLeast(1)
                val reqH = config.exactHeight.coerceAtLeast(1)
                if (config.doNotUpscale && safeSrcW <= reqW && safeSrcH <= reqH && config.cropMode == ResizeMode.FIT) {
                    safeSrcW to safeSrcH
                } else {
                    reqW to reqH
                }
            }

            BatchResizeOption.PHYSICAL_PRINT -> {
                val pxDims = DpiPrintEngine.computePixelsFromPhysical(
                    widthPhysical = config.physicalWidth.coerceAtLeast(0.1f),
                    heightPhysical = config.physicalHeight.coerceAtLeast(0.1f),
                    unit = config.physicalUnit,
                    dpi = config.dpi.coerceIn(10, 2400)
                )
                pxDims.first.coerceAtLeast(1) to pxDims.second.coerceAtLeast(1)
            }
        }

        return rawTargetW.coerceIn(1, 12000) to rawTargetH.coerceIn(1, 12000)
    }

    /**
     * Resizes [source] bitmap according to [config].
     */
    fun applyBatchResize(
        source: Bitmap,
        config: BatchConfig
    ): Bitmap {
        val (targetW, targetH) = computeBatchTargetDimensions(source.width, source.height, config)
        return when (config.resizeOption) {
            BatchResizeOption.ORIGINAL -> source
            BatchResizeOption.PERCENTAGE,
            BatchResizeOption.SAME_WIDTH,
            BatchResizeOption.SAME_HEIGHT,
            BatchResizeOption.LONGEST_EDGE -> {
                if (targetW == source.width && targetH == source.height) {
                    source
                } else {
                    BitmapUtils.resizeBitmap(source, targetW, targetH)
                }
            }
            BatchResizeOption.EXACT_DIMENSIONS,
            BatchResizeOption.PHYSICAL_PRINT -> {
                if (targetW == source.width && targetH == source.height && config.cropMode != ResizeMode.FIT) {
                    source
                } else {
                    val res = ResizeEngine.resize(
                        source = source,
                        targetWidth = targetW,
                        targetHeight = targetH,
                        mode = config.cropMode,
                        fitBackgroundColor = config.fitBackgroundColor
                    )
                    res.bitmap
                }
            }
        }
    }

    /**
     * Builds a concise human-readable metadata summary badge for a processed item.
     */
    fun buildMetadataSummary(config: BatchConfig, format: ExportFormat): String {
        val policyPart = when (config.metadataPolicy) {
            MetadataPolicy.STRIP_ALL -> "EXIF Stripped"
            MetadataPolicy.KEEP_ALL -> "EXIF Preserved"
            MetadataPolicy.CUSTOM_SELECTIVE -> {
                val scrubbed = mutableListOf<String>()
                if (config.removeGps) scrubbed.add("GPS")
                if (config.removeCameraInfo) scrubbed.add("Cam")
                if (config.removeDeviceInfo) scrubbed.add("Dev")
                if (config.removeDateMetadata) scrubbed.add("Date")
                if (scrubbed.isEmpty()) "EXIF Kept" else "No ${scrubbed.joinToString("/")}"
            }
        }
        val authorPart = if (config.customArtist.isNotBlank() || config.customCopyright.isNotBlank()) " • ©Tagged" else ""
        val dpiPart = if (format != ExportFormat.PDF) " • ${config.dpi} DPI" else ""
        return "$policyPart$authorPart$dpiPart"
    }

    /**
     * Processes a single [BatchItem] through the full Phase 9 pipeline:
     * 1. Memory-safe decoding
     * 2. Rotation & Flipping
     * 3. Batch Resize (with crop/fit/print/longest-edge/doNotUpscale rules)
     * 4. Format Conversion & Compression (Quality / Preset / Target KB / Max Ceiling KB)
     * 5. Metadata Privacy Policy + Custom Author/Copyright + Binary DPI injection
     * 6. Output File Naming & Destination saving
     */
    fun processSingleBatchItem(
        context: Context,
        item: BatchItem,
        indexInBatch: Int,
        config: BatchConfig,
        outputFileConfig: OutputFileConfiguration = OutputFileConfiguration(),
        cancelFlag: AtomicBoolean? = null,
        onStageUpdate: (stage: String, itemProgress: Float) -> Unit = { _, _ -> }
    ): BatchItem {
        val startTime = System.currentTimeMillis()
        var workingBmp: Bitmap? = null

        try {
            if (cancelFlag?.get() == true) {
                return item.copy(
                    status = BatchStatus.CANCELLED,
                    stageLabel = "Cancelled",
                    errorMessage = "Processing cancelled"
                )
            }

            // Stage 1: Decode
            onStageUpdate("Decoding image...", 0.15f)
            val maxDecodeBound = when (config.resizeOption) {
                BatchResizeOption.EXACT_DIMENSIONS -> maxOf(config.exactWidth, config.exactHeight, 2048)
                BatchResizeOption.SAME_WIDTH -> maxOf(config.sameWidth, 2048)
                BatchResizeOption.SAME_HEIGHT -> maxOf(config.sameHeight, 2048)
                BatchResizeOption.LONGEST_EDGE -> maxOf(config.longestEdge, 2048)
                BatchResizeOption.PHYSICAL_PRINT -> {
                    val (pw, ph) = DpiPrintEngine.computePixelsFromPhysical(
                        config.physicalWidth,
                        config.physicalHeight,
                        config.physicalUnit,
                        config.dpi
                    )
                    maxOf(pw, ph, 2048)
                }
                else -> 4096
            }

            workingBmp = BitmapUtils.decodeSampledBitmapFromUri(
                context = context,
                uri = item.uri,
                reqWidth = maxDecodeBound,
                reqHeight = maxDecodeBound
            ) ?: return item.copy(
                status = BatchStatus.FAILED,
                stageLabel = "Decode Failed",
                errorMessage = "Could not decode image file (unsupported or corrupted stream)"
            )

            if (cancelFlag?.get() == true) {
                workingBmp.recycle()
                return item.copy(
                    status = BatchStatus.CANCELLED,
                    stageLabel = "Cancelled",
                    errorMessage = "Processing cancelled"
                )
            }

            // Stage 2: Orientation (Rotate & Flip)
            if (config.rotationDegrees != 0 || config.flipHorizontal || config.flipVertical) {
                onStageUpdate("Transforming orientation...", 0.30f)
                if (config.rotationDegrees != 0) {
                    val rotated = BitmapUtils.rotateBitmap(workingBmp, config.rotationDegrees)
                    if (rotated !== workingBmp) {
                        workingBmp.recycle()
                        workingBmp = rotated
                    }
                }
                if (config.flipHorizontal || config.flipVertical) {
                    val flipped = BitmapUtils.flipBitmap(workingBmp!!, config.flipHorizontal, config.flipVertical)
                    if (flipped !== workingBmp) {
                        workingBmp?.recycle()
                        workingBmp = flipped
                    }
                }
            }

            if (cancelFlag?.get() == true) {
                workingBmp?.recycle()
                return item.copy(
                    status = BatchStatus.CANCELLED,
                    stageLabel = "Cancelled",
                    errorMessage = "Processing cancelled"
                )
            }

            // Stage 3: Batch Resize
            onStageUpdate("Resizing dimensions...", 0.50f)
            val resized = applyBatchResize(workingBmp!!, config)
            if (resized !== workingBmp) {
                workingBmp?.recycle()
                workingBmp = resized
            }

            if (cancelFlag?.get() == true) {
                workingBmp.recycle()
                return item.copy(
                    status = BatchStatus.CANCELLED,
                    stageLabel = "Cancelled",
                    errorMessage = "Processing cancelled"
                )
            }

            // Stage 4: Format Conversion, Compression & Output Naming
            val targetFormat = resolveOutputFormat(item, config)
            val effectiveQuality = resolveEffectiveQuality(config)
            val (targetKb, fileSizeMode) = resolveFileSizeConstraint(config)
            val privacyConfig = config.toMetadataPrivacyConfig()

            onStageUpdate("Compressing & converting to ${targetFormat.extension.uppercase()}...", 0.70f)

            val formattedNameWithExt = OutputFileManager.generateFormattedFilename(
                config = outputFileConfig,
                originalName = item.originalName,
                width = workingBmp.width,
                height = workingBmp.height,
                dpi = config.dpi,
                format = targetFormat,
                index = indexInBatch + 1
            )
            val exactBaseName = formattedNameWithExt.substringBeforeLast('.')

            // Stage 5: Export + Metadata + DPI
            onStageUpdate("Applying metadata & saving...", 0.88f)
            val exportRes = ExportEngine.exportImage(
                context = context,
                bitmap = workingBmp,
                format = targetFormat,
                quality = effectiveQuality,
                targetSizeKb = targetKb,
                fileSizeMode = fileSizeMode,
                allowDimensionAdjustment = config.allowDimensionDownscalingForKb,
                fileNamePrefix = "Batch_${indexInBatch + 1}",
                customExactFileName = exactBaseName,
                dpi = config.dpi,
                privacyConfig = privacyConfig,
                sourceUri = item.uri,
                stripExif = privacyConfig.policy == MetadataPolicy.STRIP_ALL,
                artist = privacyConfig.customArtist.ifBlank { null },
                copyright = privacyConfig.customCopyright.ifBlank { null },
                jpegBackgroundColor = config.jpegBackgroundColor,
                destinationType = outputFileConfig.destinationType,
                safTreeUri = outputFileConfig.safTreeUri
            )

            // Ensure binary JFIF/PNG/WebP DPI headers are injected if requested
            if (config.injectBinaryDpi && exportRes.success && exportRes.outputFile != null && exportRes.outputFile.exists() && targetFormat != ExportFormat.PDF) {
                try {
                    val rawBytes = exportRes.outputFile.readBytes()
                    val dpiInjected = DpiPrintEngine.embedDpiInEncodedBytes(
                        encodedBytes = rawBytes,
                        format = targetFormat,
                        dpi = config.dpi
                    )
                    FileOutputStream(exportRes.outputFile).use { fos ->
                        fos.write(dpiInjected)
                        fos.flush()
                    }
                    // Re-apply EXIF privacy/author tags on JPEG/WebP after JFIF header update
                    if (targetFormat == ExportFormat.JPEG || targetFormat == ExportFormat.WEBP_LOSSY || targetFormat == ExportFormat.WEBP_LOSSLESS) {
                        ExifManager.applyExifToOutputFile(
                            outputFile = exportRes.outputFile,
                            dpi = config.dpi,
                            privacyConfig = privacyConfig,
                            sourceUri = item.uri,
                            context = context,
                            format = targetFormat
                        )
                    }
                } catch (_: Exception) {
                    // Keep exported file intact if post-injection fails
                }
            }

            workingBmp.recycle()
            workingBmp = null

            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)

            return if (exportRes.success) {
                val finalSize = exportRes.outputFile?.takeIf { it.exists() }?.length() ?: exportRes.fileSizeBytes
                onStageUpdate("Completed", 1.0f)
                item.copy(
                    status = BatchStatus.COMPLETED,
                    progress = 1.0f,
                    outputUri = exportRes.outputUri,
                    outputSize = finalSize,
                    outputWidth = exportRes.width,
                    outputHeight = exportRes.height,
                    outputFormat = targetFormat,
                    outputFileName = exportRes.outputFile?.name ?: formattedNameWithExt,
                    stageLabel = "Completed",
                    processingTimeMs = elapsed,
                    metadataSummary = buildMetadataSummary(config, targetFormat),
                    errorMessage = null
                )
            } else {
                item.copy(
                    status = BatchStatus.FAILED,
                    progress = 0f,
                    stageLabel = "Failed",
                    processingTimeMs = elapsed,
                    errorMessage = exportRes.errorMessage ?: "Export failed"
                )
            }
        } catch (oom: OutOfMemoryError) {
            workingBmp?.recycle()
            System.gc()
            return item.copy(
                status = BatchStatus.FAILED,
                progress = 0f,
                stageLabel = "Out of Memory",
                errorMessage = "Out of memory processing image"
            )
        } catch (e: Exception) {
            workingBmp?.recycle()
            return item.copy(
                status = BatchStatus.FAILED,
                progress = 0f,
                stageLabel = "Error",
                errorMessage = e.localizedMessage ?: "Failed to process image"
            )
        }
    }

    /**
     * Sorts [items] according to [sortOption].
     */
    fun sortBatchItems(items: List<BatchItem>, sortOption: BatchSortOption): List<BatchItem> {
        return when (sortOption) {
            BatchSortOption.QUEUE_ORDER -> items
            BatchSortOption.NAME_ASC -> items.sortedBy { it.originalName.lowercase() }
            BatchSortOption.SIZE_DESC -> items.sortedByDescending { it.originalSize }
            BatchSortOption.SIZE_ASC -> items.sortedBy { it.originalSize }
            BatchSortOption.RESOLUTION_DESC -> items.sortedByDescending { it.originalWidth.toLong() * it.originalHeight.toLong() }
            BatchSortOption.STATUS -> items.sortedBy { it.status.ordinal }
        }
    }

    /**
     * Filters [items] according to [filterOption].
     */
    fun filterBatchItems(items: List<BatchItem>, filterOption: BatchFilterOption): List<BatchItem> {
        return when (filterOption) {
            BatchFilterOption.ALL -> items
            BatchFilterOption.SELECTED -> items.filter { it.selected }
            BatchFilterOption.PENDING -> items.filter { it.status == BatchStatus.PENDING || it.status == BatchStatus.PROCESSING }
            BatchFilterOption.COMPLETED -> items.filter { it.status == BatchStatus.COMPLETED }
            BatchFilterOption.FAILED -> items.filter { it.status == BatchStatus.FAILED || it.status == BatchStatus.CANCELLED }
        }
    }

    /**
     * Computes an aggregate [BatchSummaryReport] for the given [items].
     */
    fun computeSummaryReport(items: List<BatchItem>): BatchSummaryReport {
        val completed = items.filter { it.status == BatchStatus.COMPLETED }
        val failedCount = items.count { it.status == BatchStatus.FAILED }
        val cancelledCount = items.count { it.status == BatchStatus.CANCELLED }
        val pendingCount = items.count { it.status == BatchStatus.PENDING || it.status == BatchStatus.PROCESSING }

        val origBytes = completed.sumOf { it.originalSize }
        val outBytes = completed.sumOf { it.outputSize }
        val savedBytes = (origBytes - outBytes).coerceAtLeast(0L)
        val avgSavingsPct = if (origBytes > 0L) {
            ((origBytes - outBytes).toFloat() / origBytes.toFloat()) * 100f
        } else 0f

        val totalTime = completed.sumOf { it.processingTimeMs }
        val avgTime = if (completed.isNotEmpty()) totalTime / completed.size else 0L

        return BatchSummaryReport(
            totalItems = items.size,
            completedItems = completed.size,
            failedItems = failedCount,
            cancelledItems = cancelledCount,
            pendingItems = pendingCount,
            totalOriginalBytes = origBytes,
            totalOutputBytes = outBytes,
            totalSavedBytes = savedBytes,
            averageCompressionPercent = avgSavingsPct,
            totalProcessingTimeMs = totalTime,
            averageTimePerItemMs = avgTime
        )
    }
}
