package com.example.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.processing.*

/**
 * Clean Architecture Facade: Exposes all modular processing engines through a coherent
 * interface for the presentation layer (ViewModel), keeping logic testable, modular,
 * and decoupled from Compose UI components.
 *
 * Encapsulates:
 * - CropEngine (BitmapUtils / CropImageUseCase)
 * - ResizeEngine (BitmapUtils / ResizeImageUseCase)
 * - CompressionEngine (FileSizeEngine)
 * - ExactSizeEngine (ExactSizeControls / BitmapUtils)
 * - FormatConversionEngine (ExportEngine)
 * - DPIEngine (BitmapUtils / ExportEngine)
 * - MetadataEngine (ExifManager)
 * - EnhancementEngine (ImageEnhancer)
 * - BackgroundEngine (BackgroundProcessor)
 * - PrintLayoutEngine (PhotoSheetGenerator)
 * - CreativeAdvancedEngine (AdvancedCreativeEngine)
 * - ExportManager (ExportEngine)
 * - FileManager (OutputFileManager)
 */
class ImageProcessingRepository(
    private val context: Context
) {
    // Engine Delegations
    val cropUseCase = CropImageUseCase()
    val resizeUseCase = ResizeImageUseCase()
    val exportUseCase = ExportImageUseCase()
    val loadUseCase = LoadImageUseCase()
    val analyzeUseCase = AnalyzeImageUseCase()
    val advancedEditingUseCase = AdvancedEditingUseCase()

    fun optimizeFileSize(
        source: Bitmap,
        format: com.example.model.ExportFormat,
        targetKb: Int,
        mode: FileSizeMode,
        allowDownscale: Boolean,
        bgColor: Int
    ) = FileSizeEngine.optimizeFileSize(
        source = source,
        format = format,
        targetKb = targetKb,
        mode = mode,
        allowDimensionAdjustment = allowDownscale,
        jpegBackgroundColor = bgColor
    )

    fun createPhotoSheet(
        photo: Bitmap,
        config: PhotoSheetGenerator.SheetConfig
    ) = PhotoSheetGenerator.createPhotoSheet(photo, config)

    fun analyzeQuality(bitmap: Bitmap) =
        AdvancedCreativeEngine.analyzeImageQuality(bitmap)

    fun applyWatermark(bitmap: Bitmap, config: AdvancedCreativeEngine.WatermarkConfig) =
        AdvancedCreativeEngine.applyWatermark(bitmap, config)

    fun applyFrame(bitmap: Bitmap, config: AdvancedCreativeEngine.FrameConfig) =
        AdvancedCreativeEngine.applyFrameStyling(bitmap, config)

    fun applyBlur(bitmap: Bitmap, radius: Int) =
        AdvancedCreativeEngine.applyFastBlur(bitmap, radius)

    fun applyPixelate(bitmap: Bitmap, size: Int) =
        AdvancedCreativeEngine.applyPixelate(bitmap, size)

    fun computeDHash(bitmap: Bitmap) =
        AdvancedCreativeEngine.computeDHash(bitmap)

    fun checkDuplicate(hashA: Long, hashB: Long) =
        AdvancedCreativeEngine.hammingDistance(hashA, hashB) <= 10
}
