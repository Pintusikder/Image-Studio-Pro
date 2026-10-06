package com.example.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.ExportFormat
import com.example.model.MetadataPrivacyConfig
import com.example.model.OutputFileConfiguration
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode

/**
 * Clean Architecture Use Case: Coordinates image encoding, compression optimization,
 * metadata privacy scrubbing, filename formatting, and physical storage export.
 */
class ExportImageUseCase {

    operator fun invoke(
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
        metadataPrivacyConfig: MetadataPrivacyConfig = MetadataPrivacyConfig(),
        sourceUri: Uri? = null,
        outputConfig: OutputFileConfiguration? = null,
        isOverwriteOriginal: Boolean = false,
        jpegBackgroundColor: Int = android.graphics.Color.WHITE
    ): ExportEngine.ExportResult {
        return ExportEngine.exportImage(
            context = context,
            bitmap = bitmap,
            format = format,
            quality = quality,
            targetSizeKb = targetSizeKb,
            fileSizeMode = fileSizeMode,
            allowDimensionAdjustment = allowDimensionAdjustment,
            fileNamePrefix = fileNamePrefix,
            customExactFileName = customExactFileName,
            dpi = dpi,
            privacyConfig = metadataPrivacyConfig,
            sourceUri = sourceUri,
            jpegBackgroundColor = jpegBackgroundColor,
            overwriteOriginal = isOverwriteOriginal,
            destinationType = outputConfig?.destinationType ?: com.example.model.OutputDestinationType.PUBLIC_MEDIASTORE,
            safTreeUri = outputConfig?.safTreeUri
        )
    }
}
