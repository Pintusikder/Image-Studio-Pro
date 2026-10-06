package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.FormatConversionPair
import com.example.processing.FormatConversionEngine
import com.example.processing.FormatConversionResult
import com.example.processing.FormatConversionVerificationReport
import java.io.File

/**
 * Clean Architecture Use Case: Handles image format conversions across all supported image/document formats,
 * verifies MIME types via magic binary signatures, and validates output file integrity.
 */
class ConvertFormatUseCase {

    /**
     * Converts [source] from [fromFormat] to [toFormat].
     */
    fun convert(
        source: Bitmap,
        fromFormat: DetectedImageFormat = DetectedImageFormat.JPEG,
        toFormat: ExportFormat = ExportFormat.PNG,
        quality: Int = 90,
        backgroundColor: Int = Color.WHITE
    ): FormatConversionResult {
        return FormatConversionEngine.convert(
            source = source,
            fromFormat = fromFormat,
            toFormat = toFormat,
            quality = quality,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Converts [source] using predefined [pair].
     */
    fun convertPair(
        source: Bitmap,
        pair: FormatConversionPair,
        quality: Int = 90,
        backgroundColor: Int = Color.WHITE
    ): FormatConversionResult {
        return FormatConversionEngine.convertPair(
            source = source,
            pair = pair,
            quality = quality,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Verifies byte stream format and MIME compliance.
     */
    fun verifyBytes(
        bytes: ByteArray,
        fromFormat: DetectedImageFormat,
        toFormat: ExportFormat,
        sourceWidth: Int,
        sourceHeight: Int
    ): FormatConversionVerificationReport {
        return FormatConversionEngine.verifyConvertedBytes(
            bytes = bytes,
            fromFormat = fromFormat,
            toFormat = toFormat,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )
    }

    /**
     * Verifies on-disk file format and MIME compliance.
     */
    fun verifyFile(file: File, expectedFormat: ExportFormat): FormatConversionVerificationReport {
        return FormatConversionEngine.verifyConvertedFile(file, expectedFormat)
    }
}
