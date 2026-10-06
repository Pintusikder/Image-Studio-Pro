package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ExportFormat
import com.example.processing.CompressionEngine
import com.example.processing.CompressionResult
import com.example.processing.CompressionVerificationReport

/**
 * Clean Architecture Use Case: Handles image compression across quality reduction,
 * target file size optimization, maximum file size ceiling enforcement,
 * iterative downsampling, and output verification.
 */
class CompressImageUseCase {

    /**
     * Compresses [source] with explicit [quality] percentage.
     */
    fun compressByQuality(
        source: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        quality: Int = 85,
        backgroundColor: Int = Color.WHITE
    ): CompressionResult {
        return CompressionEngine.compressByQuality(source, format, quality, backgroundColor)
    }

    /**
     * Compresses [source] targeting a specific file size in KB.
     */
    fun compressToTargetSize(
        source: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        targetKb: Int,
        allowDimensionAdjustment: Boolean = true,
        exactByteAlignment: Boolean = false,
        backgroundColor: Int = Color.WHITE
    ): CompressionResult {
        return CompressionEngine.compressToTargetSize(
            source = source,
            format = format,
            targetKb = targetKb,
            allowDimensionAdjustment = allowDimensionAdjustment,
            exactByteAlignment = exactByteAlignment,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Compresses [source] ensuring the resulting file strictly adheres to a maximum ceiling in KB.
     */
    fun compressToMaximumCeiling(
        source: Bitmap,
        format: ExportFormat = ExportFormat.JPEG,
        maxKb: Int,
        allowDimensionAdjustment: Boolean = true,
        backgroundColor: Int = Color.WHITE
    ): CompressionResult {
        return CompressionEngine.compressToMaximumCeiling(
            source = source,
            format = format,
            maxKb = maxKb,
            allowDimensionAdjustment = allowDimensionAdjustment,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Verifies an encoded byte stream against compression requirements.
     */
    fun verifyCompressedBytes(
        bytes: ByteArray,
        format: ExportFormat,
        quality: Int,
        requestedTargetBytes: Long?,
        requestedMaxBytes: Long?,
        sourceWidth: Int,
        sourceHeight: Int,
        iterationsCount: Int = 1,
        dimensionsAdjusted: Boolean = false
    ): CompressionVerificationReport {
        return CompressionEngine.verifyCompressedBytes(
            bytes = bytes,
            format = format,
            quality = quality,
            requestedTargetBytes = requestedTargetBytes,
            requestedMaxBytes = requestedMaxBytes,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            iterationsCount = iterationsCount,
            dimensionsAdjusted = dimensionsAdjusted
        )
    }
}
