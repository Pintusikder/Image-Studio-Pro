package com.example.processing

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.model.ExportFormat
import com.example.model.OutputDestinationType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

import com.example.model.MetadataPolicy
import com.example.model.MetadataPrivacyConfig
import com.example.model.OutputVerificationReport
import com.example.model.VerificationStatus

object ExportEngine {

    data class ExportResult(
        val success: Boolean,
        val outputUri: Uri?,
        val outputFile: File?,
        val fileSizeBytes: Long,
        val width: Int,
        val height: Int,
        val format: ExportFormat,
        val isExactMatch: Boolean = false,
        val isCompliantWithCeiling: Boolean = true,
        val sizeOptimizationResult: FileSizeOptimizationResult? = null,
        val verifiedFileWidth: Int = 0,
        val verifiedFileHeight: Int = 0,
        val dimensionsMatchRequested: Boolean = true,
        val metadataAudit: ExifManager.MetadataAuditResult? = null,
        val verificationReport: OutputVerificationReport? = null,
        val errorMessage: String? = null,
        val isOverwritten: Boolean = false
    )

    fun exportImage(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int = 90,
        targetSizeKb: Int? = null,
        fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
        allowDimensionAdjustment: Boolean = true,
        fileNamePrefix: String = "ImageStudio",
        customExactFileName: String? = null,
        dpi: Int = 300,
        privacyConfig: MetadataPrivacyConfig = MetadataPrivacyConfig(),
        sourceUri: Uri? = null,
        stripExif: Boolean = false,
        artist: String? = null,
        copyright: String? = null,
        jpegBackgroundColor: Int = Color.WHITE,
        overwriteOriginal: Boolean = false,
        destinationType: OutputDestinationType = OutputDestinationType.PUBLIC_MEDIASTORE,
        safTreeUri: Uri? = null
    ): ExportResult {
        return try {
            val timestamp = System.currentTimeMillis()

            if (format == ExportFormat.PDF) {
                return exportToPdf(context, bitmap, fileNamePrefix, customExactFileName, timestamp, destinationType, safTreeUri)
            }

            var currentBitmap = bitmap
            var sizeOptimizationResult: FileSizeOptimizationResult? = null
            val bytes: ByteArray

            if (targetSizeKb != null && targetSizeKb > 0) {
                val opt = FileSizeEngine.optimizeFileSize(
                    source = currentBitmap,
                    format = format,
                    targetKb = targetSizeKb,
                    mode = fileSizeMode,
                    allowDimensionAdjustment = allowDimensionAdjustment,
                    qualityHint = quality,
                    jpegBackgroundColor = jpegBackgroundColor
                )
                sizeOptimizationResult = opt
                bytes = opt.encodedBytes
                if (opt.outputWidth != currentBitmap.width || opt.outputHeight != currentBitmap.height) {
                    currentBitmap = Bitmap.createScaledBitmap(currentBitmap, opt.outputWidth, opt.outputHeight, true)
                }
            } else {
                val stream = ByteArrayOutputStream()
                compressBitmapToStream(currentBitmap, format, quality.coerceIn(5, 100), stream, jpegBackgroundColor)
                bytes = stream.toByteArray()
            }

            // Generate safe sanitized filename
            val rawFileName = if (!customExactFileName.isNullOrBlank()) {
                val sanitized = OutputFileManager.sanitizeFilename(customExactFileName.substringBeforeLast('.')).sanitizedName
                "$sanitized.${format.extension}"
            } else {
                val sanitizedPrefix = OutputFileManager.sanitizeFilename(fileNamePrefix).sanitizedName
                "${sanitizedPrefix}_$timestamp.${format.extension}"
            }

            val internalFile = File(context.cacheDir, rawFileName)
            FileOutputStream(internalFile).use { fos ->
                fos.write(bytes)
                fos.flush()
            }

            // Apply EXIF / DPI metadata & Privacy Scrubber
            val effectivePrivacyConfig = if (stripExif && privacyConfig.policy == MetadataPolicy.KEEP_ALL) {
                privacyConfig.copy(policy = MetadataPolicy.STRIP_ALL)
            } else {
                privacyConfig.copy(
                    customArtist = artist ?: privacyConfig.customArtist,
                    customCopyright = copyright ?: privacyConfig.customCopyright
                )
            }

            if (format == ExportFormat.JPEG || format == ExportFormat.WEBP_LOSSY || format == ExportFormat.WEBP_LOSSLESS) {
                ExifManager.applyExifToOutputFile(
                    outputFile = internalFile,
                    dpi = dpi,
                    privacyConfig = effectivePrivacyConfig,
                    sourceUri = sourceUri,
                    context = context,
                    format = format
                )
            } else if (format == ExportFormat.PNG) {
                embedPngDpi(internalFile, dpi)
            }

            // Perform live metadata audit check
            val audit = ExifManager.auditFileMetadata(internalFile, format)

            // Save to Target Destination (MediaStore, SAF folder, or Overwrite Original if explicitly chosen)
            var overwritten = false
            var finalUri: Uri? = null

            if (overwriteOriginal && sourceUri != null) {
                try {
                    if (sourceUri.scheme == "file") {
                        val originalFile = File(sourceUri.path ?: "")
                        if (originalFile.exists() && originalFile.canWrite()) {
                            internalFile.inputStream().use { input ->
                                FileOutputStream(originalFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            finalUri = sourceUri
                            overwritten = true
                        }
                    } else {
                        context.contentResolver.openOutputStream(sourceUri, "wt")?.use { output ->
                            internalFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                            finalUri = sourceUri
                            overwritten = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    overwritten = false
                }
            }

            if (!overwritten) {
                when (destinationType) {
                    OutputDestinationType.CUSTOM_SAF_DIRECTORY -> {
                        if (safTreeUri != null) {
                            val dataBytes = internalFile.readBytes()
                            finalUri = OutputFileManager.saveToSafTreeUri(
                                context = context,
                                treeUri = safTreeUri,
                                fileName = rawFileName,
                                mimeType = format.mimeType,
                                dataBytes = dataBytes,
                                overwrite = overwriteOriginal
                            )
                        }
                        if (finalUri == null) {
                            finalUri = saveToMediaStore(context, internalFile, rawFileName, format.mimeType) ?: Uri.fromFile(internalFile)
                        }
                    }
                    OutputDestinationType.PUBLIC_MEDIASTORE,
                    OutputDestinationType.SYSTEM_SAVE_AS -> {
                        finalUri = saveToMediaStore(context, internalFile, rawFileName, format.mimeType) ?: Uri.fromFile(internalFile)
                    }
                }
            }

            val actualFileLength = internalFile.length()
            val targetBytes = targetSizeKb?.let { it * 1024L }
            val verifiedExact = (targetBytes != null && actualFileLength == targetBytes)
            val verifiedCompliant = (targetBytes == null || actualFileLength <= targetBytes)

            // Programmatically verify actual image dimensions directly from written file bytes
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(internalFile.absolutePath, decodeOptions)
            val verifiedWidth = decodeOptions.outWidth
            val verifiedHeight = decodeOptions.outHeight
            val dimensionsMatch = (verifiedWidth == currentBitmap.width && verifiedHeight == currentBitmap.height)

            // Perform comprehensive output verification report across all measurable metrics
            val verificationReport = OutputVerificationEngine.verifyExportOutput(
                outputFile = internalFile,
                requestedWidth = currentBitmap.width,
                requestedHeight = currentBitmap.height,
                requestedFormat = format,
                requestedDpi = dpi,
                targetSizeKb = targetSizeKb,
                fileSizeMode = fileSizeMode,
                privacyConfig = effectivePrivacyConfig
            )

            ExportResult(
                success = true,
                outputUri = finalUri,
                outputFile = internalFile,
                fileSizeBytes = actualFileLength,
                width = currentBitmap.width,
                height = currentBitmap.height,
                format = format,
                isExactMatch = verifiedExact,
                isCompliantWithCeiling = verifiedCompliant,
                sizeOptimizationResult = sizeOptimizationResult,
                verifiedFileWidth = verifiedWidth,
                verifiedFileHeight = verifiedHeight,
                dimensionsMatchRequested = dimensionsMatch,
                metadataAudit = audit,
                verificationReport = verificationReport,
                isOverwritten = overwritten
            )
        } catch (e: OutOfMemoryError) {
            System.gc()
            ExportResult(
                success = false,
                outputUri = null,
                outputFile = null,
                fileSizeBytes = 0,
                width = 0,
                height = 0,
                format = format,
                errorMessage = "Export failed: Device ran out of memory while processing the high-resolution image. Try reducing dimensions or quality."
            )
        } catch (e: SecurityException) {
            ExportResult(
                success = false,
                outputUri = null,
                outputFile = null,
                fileSizeBytes = 0,
                width = 0,
                height = 0,
                format = format,
                errorMessage = "Export failed: Storage permission denied. Please verify write permissions or choose a different save location."
            )
        } catch (e: java.io.IOException) {
            val isSpaceError = e.message?.contains("ENOSPC", ignoreCase = true) == true ||
                    e.message?.contains("space", ignoreCase = true) == true
            ExportResult(
                success = false,
                outputUri = null,
                outputFile = null,
                fileSizeBytes = 0,
                width = 0,
                height = 0,
                format = format,
                errorMessage = if (isSpaceError) {
                    "Export failed: Insufficient storage space on device. Please free up storage and try again."
                } else {
                    "Export failed: Could not write file to disk (${e.localizedMessage ?: "I/O error"})."
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                outputUri = null,
                outputFile = null,
                fileSizeBytes = 0,
                width = 0,
                height = 0,
                format = format,
                errorMessage = "Export failed: ${e.localizedMessage ?: "An unexpected error occurred during image export."}"
            )
        }
    }

    private fun compressBitmapToStream(
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        outputStream: OutputStream,
        jpegBackgroundColor: Int = Color.WHITE
    ) {
        when (format) {
            ExportFormat.JPEG -> {
                // Ensure no alpha channel for JPEG by compositing onto background color
                val rgbBitmap = if (bitmap.hasAlpha()) {
                    BitmapUtils.compositeOnBackground(bitmap, jpegBackgroundColor)
                } else bitmap
                rgbBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            ExportFormat.WEBP_LOSSY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
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

    private fun exportToPdf(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String,
        customExactFileName: String?,
        timestamp: Long,
        destinationType: OutputDestinationType = OutputDestinationType.PUBLIC_MEDIASTORE,
        safTreeUri: Uri? = null
    ): ExportResult {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val fileName = if (!customExactFileName.isNullOrBlank()) {
            val sanitized = OutputFileManager.sanitizeFilename(customExactFileName.substringBeforeLast('.')).sanitizedName
            "$sanitized.pdf"
        } else {
            val sanitizedPrefix = OutputFileManager.sanitizeFilename(fileNamePrefix).sanitizedName
            "${sanitizedPrefix}_$timestamp.pdf"
        }

        val internalFile = File(context.cacheDir, fileName)
        FileOutputStream(internalFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        var finalUri: Uri? = null
        if (destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY && safTreeUri != null) {
            val dataBytes = internalFile.readBytes()
            finalUri = OutputFileManager.saveToSafTreeUri(
                context = context,
                treeUri = safTreeUri,
                fileName = fileName,
                mimeType = "application/pdf",
                dataBytes = dataBytes
            )
        }
        if (finalUri == null) {
            finalUri = saveToMediaStore(context, internalFile, fileName, "application/pdf")
        }

        val verificationReport = OutputVerificationEngine.verifyExportOutput(
            outputFile = internalFile,
            requestedWidth = bitmap.width,
            requestedHeight = bitmap.height,
            requestedFormat = ExportFormat.PDF,
            requestedDpi = 300
        )

        return ExportResult(
            success = true,
            outputUri = finalUri ?: Uri.fromFile(internalFile),
            outputFile = internalFile,
            fileSizeBytes = internalFile.length(),
            width = bitmap.width,
            height = bitmap.height,
            format = ExportFormat.PDF,
            verificationReport = verificationReport
        )
    }

    private fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (mimeType == "application/pdf") {
                        Environment.DIRECTORY_DOCUMENTS + "/ImageStudio"
                    } else {
                        Environment.DIRECTORY_PICTURES + "/ImageStudio"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (mimeType == "application/pdf") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val uri = context.contentResolver.insert(collection, contentValues) ?: return null

            context.contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportDirectlyToDocumentUri(
        context: Context,
        bitmap: Bitmap,
        documentUri: Uri,
        format: ExportFormat,
        quality: Int = 90,
        targetSizeKb: Int? = null,
        fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
        allowDimensionAdjustment: Boolean = true,
        dpi: Int = 300,
        privacyConfig: MetadataPrivacyConfig = MetadataPrivacyConfig(),
        sourceUri: Uri? = null,
        stripExif: Boolean = false,
        artist: String? = null,
        copyright: String? = null,
        jpegBackgroundColor: Int = Color.WHITE
    ): ExportResult {
        return try {
            val intermediateResult = exportImage(
                context = context,
                bitmap = bitmap,
                format = format,
                quality = quality,
                targetSizeKb = targetSizeKb,
                fileSizeMode = fileSizeMode,
                allowDimensionAdjustment = allowDimensionAdjustment,
                dpi = dpi,
                privacyConfig = privacyConfig,
                sourceUri = sourceUri,
                stripExif = stripExif,
                artist = artist,
                copyright = copyright,
                jpegBackgroundColor = jpegBackgroundColor
            )

            if (intermediateResult.success && intermediateResult.outputFile != null) {
                val dataBytes = intermediateResult.outputFile.readBytes()
                val written = OutputFileManager.saveToSafDocumentUri(context, documentUri, dataBytes)
                if (written) {
                    intermediateResult.copy(outputUri = documentUri)
                } else {
                    intermediateResult.copy(success = false, errorMessage = "Failed to write data to selected storage document")
                }
            } else {
                intermediateResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                outputUri = null,
                outputFile = null,
                fileSizeBytes = 0,
                width = 0,
                height = 0,
                format = format,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun shareImage(context: Context, file: File, mimeType: String, title: String = "Share Image") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun embedPngDpi(pngFile: File, dpi: Int) {
        try {
            val bytes = pngFile.readBytes()
            if (bytes.size < 33 || bytes[0] != 0x89.toByte() || bytes[1] != 0x50.toByte()) return

            val ppm = (dpi * 39.3700787).toInt() // pixels per meter
            val chunkData = ByteArray(9)
            chunkData[0] = (ppm ushr 24).toByte()
            chunkData[1] = (ppm ushr 16).toByte()
            chunkData[2] = (ppm ushr 8).toByte()
            chunkData[3] = ppm.toByte()
            chunkData[4] = (ppm ushr 24).toByte()
            chunkData[5] = (ppm ushr 16).toByte()
            chunkData[6] = (ppm ushr 8).toByte()
            chunkData[7] = ppm.toByte()
            chunkData[8] = 1 // Unit: meter

            val typeAndData = ByteArray(4 + 9)
            typeAndData[0] = 'p'.code.toByte()
            typeAndData[1] = 'H'.code.toByte()
            typeAndData[2] = 'y'.code.toByte()
            typeAndData[3] = 's'.code.toByte()
            System.arraycopy(chunkData, 0, typeAndData, 4, 9)

            val crc = java.util.zip.CRC32()
            crc.update(typeAndData)
            val crcVal = crc.value.toInt()

            val bos = ByteArrayOutputStream()
            bos.write(bytes, 0, 33)
            bos.write(0); bos.write(0); bos.write(0); bos.write(9)
            bos.write(typeAndData)
            bos.write((crcVal ushr 24) and 0xFF)
            bos.write((crcVal ushr 16) and 0xFF)
            bos.write((crcVal ushr 8) and 0xFF)
            bos.write(crcVal and 0xFF)
            bos.write(bytes, 33, bytes.size - 33)

            FileOutputStream(pngFile).use { fos ->
                fos.write(bos.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun measureActualCompressedBytes(
        source: Bitmap,
        targetW: Int,
        targetH: Int,
        format: ExportFormat,
        quality: Int = 90,
        targetSizeKb: Int? = null,
        fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
        allowDimensionAdjustment: Boolean = true,
        jpegBackgroundColor: Int = Color.WHITE
    ): FileSizeOptimizationResult {
        val safeW = max(1, targetW)
        val safeH = max(1, targetH)

        val bitmapToMeasure = if (source.width == safeW && source.height == safeH) {
            source
        } else {
            Bitmap.createScaledBitmap(source, safeW, safeH, true)
        }

        return if (targetSizeKb != null && targetSizeKb > 0) {
            FileSizeEngine.optimizeFileSize(
                source = bitmapToMeasure,
                format = format,
                targetKb = targetSizeKb,
                mode = fileSizeMode,
                allowDimensionAdjustment = allowDimensionAdjustment,
                qualityHint = quality,
                jpegBackgroundColor = jpegBackgroundColor
            )
        } else {
            // Quality-based measurement
            val stream = ByteArrayOutputStream()
            compressBitmapToStream(bitmapToMeasure, format, quality.coerceIn(5, 100), stream, jpegBackgroundColor)
            val bytes = stream.toByteArray()
            val actual = bytes.size.toLong()
            FileSizeOptimizationResult(
                mode = fileSizeMode,
                targetKb = (actual / 1024).toInt(),
                targetBytes = actual,
                actualBytes = actual,
                differenceBytes = 0,
                percentDifference = 0f,
                quality = quality,
                outputWidth = bitmapToMeasure.width,
                outputHeight = bitmapToMeasure.height,
                dimensionsAdjusted = false,
                isCompliantWithCeiling = true,
                isExactByteMatch = true,
                format = format,
                encodedBytes = bytes,
                iterationsCount = 1,
                statusBadge = "Manual Quality: $quality%",
                explanation = "Unconstrained file size with manual compression quality ($quality%)."
            )
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "image/jpeg", chooserTitle: String = "Share Signature") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun printBitmap(context: Context, bitmap: Bitmap, jobName: String = "Signature_Print") {
        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        printHelper.printBitmap(jobName, bitmap)
    }
}
