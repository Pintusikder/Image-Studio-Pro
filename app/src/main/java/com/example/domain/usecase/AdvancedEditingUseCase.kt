package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.example.processing.AdvancedCreativeEngine
import com.example.processing.BackgroundProcessor
import com.example.processing.CropEngine
import com.example.processing.CropShape
import com.example.processing.CropVerificationReport
import com.example.processing.ImageEnhancer
import com.example.processing.PerspectiveEngine
import com.example.processing.RotateStraightenEngine

/**
 * Phase 10 — Advanced Editing UseCase.
 *
 * Orchestrates and verifies all Phase 10 Advanced Editing operations:
 * 1. Enhancement (Brightness, Contrast, Saturation, Exposure, Highlights, Shadows, Sharpness,
 *    Warmth, Vignette, Vibrance, Gamma, CLAHE, Grayscale, B&W, Presets, Auto-Enhance, Histogram)
 * 2. Perspective (4-corner homography warp, Keystone vertical/horizontal tilt, Smart Document
 *    Quad Detection, A4/Letter/Receipt/ID presets, Document Magic Clean / Crisp B&W)
 * 3. Straighten & Rotate (-45°..+45° fine straighten, inscribed auto-crop, expand canvas,
 *    retain dimensions, Smart Horizon Detection, Auto-Orient, Symmetry Mirroring)
 * 4. Background (Smart Chroma/Corner flood-fill segmentation, Transparent PNG cutout,
 *    Solid/Gradient/Blur Bokeh/Custom Image replacement, Edge Tolerance & Feathering)
 * 5. Advanced Crop (Free/Locked Aspect Ratio, Shape Masks: Circle/Rounded/Oval/Heart/Star/
 *    Hexagon/Diamond/Badge, Smart Subject Saliency Auto-Crop, Decorative Border Frame)
 * 6. Other Approved Advanced Features (Watermark, Frame & Shadow Styling, Privacy Blur &
 *    Pixelate, Region Redaction, Color Palette Extraction, Image Quality & Sharpness Analysis,
 *    Perceptual dHash Duplicate Comparison)
 */
class AdvancedEditingUseCase {

    data class EnhancementResult(
        val bitmap: Bitmap,
        val parameters: ImageEnhancer.EnhancementParameters,
        val verification: ImageEnhancer.EnhancementVerificationReport,
        val histogram: ImageEnhancer.HistogramData
    )

    data class PerspectiveResult(
        val bitmap: Bitmap,
        val quad: PerspectiveEngine.PerspectiveQuad,
        val preset: PerspectiveEngine.PerspectiveOutputPreset,
        val enhanceMode: PerspectiveEngine.DocumentEnhanceMode,
        val verification: PerspectiveEngine.PerspectiveVerificationReport
    )

    data class StraightenResult(
        val bitmap: Bitmap,
        val angleDegrees: Float,
        val cropMode: RotateStraightenEngine.StraightenCropMode,
        val verification: RotateStraightenEngine.StraightenVerificationReport
    )

    data class BackgroundResult(
        val bitmap: Bitmap,
        val config: BackgroundProcessor.BackgroundConfig,
        val verification: BackgroundProcessor.BackgroundVerificationReport
    )

    data class AdvancedCropResult(
        val bitmap: Bitmap,
        val cropRectNormalized: RectF,
        val shape: CropShape,
        val verification: CropVerificationReport
    )

    // ==================== 1. ENHANCEMENT ====================

    fun executeEnhancement(
        source: Bitmap,
        params: ImageEnhancer.EnhancementParameters
    ): EnhancementResult {
        val enhanced = ImageEnhancer.applyEnhancements(source, params)
        val verification = ImageEnhancer.verifyEnhancement(source, enhanced, params)
        val histogram = ImageEnhancer.computeHistogram(enhanced)
        return EnhancementResult(
            bitmap = enhanced,
            parameters = params,
            verification = verification,
            histogram = histogram
        )
    }

    fun executeAutoEnhancement(source: Bitmap): EnhancementResult {
        val autoParams = ImageEnhancer.computeAutoEnhancements(source)
        return executeEnhancement(source, autoParams)
    }

    // ==================== 2. PERSPECTIVE ====================

    fun executePerspectiveWarp(
        source: Bitmap,
        quad: PerspectiveEngine.PerspectiveQuad,
        preset: PerspectiveEngine.PerspectiveOutputPreset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
        enhanceMode: PerspectiveEngine.DocumentEnhanceMode = PerspectiveEngine.DocumentEnhanceMode.ORIGINAL
    ): PerspectiveResult {
        val warped = PerspectiveEngine.warpPerspective(source, quad, preset, enhanceMode)
        val verification = PerspectiveEngine.verifyPerspective(source, warped, quad, preset)
        return PerspectiveResult(
            bitmap = warped,
            quad = quad,
            preset = preset,
            enhanceMode = enhanceMode,
            verification = verification
        )
    }

    fun executeKeystoneTilt(
        source: Bitmap,
        verticalTilt: Float,
        horizontalTilt: Float,
        preset: PerspectiveEngine.PerspectiveOutputPreset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
        enhanceMode: PerspectiveEngine.DocumentEnhanceMode = PerspectiveEngine.DocumentEnhanceMode.ORIGINAL
    ): PerspectiveResult {
        val quad = PerspectiveEngine.quadFromKeystoneTilt(verticalTilt, horizontalTilt)
        return executePerspectiveWarp(source, quad, preset, enhanceMode)
    }

    fun detectDocumentCorners(source: Bitmap): PerspectiveEngine.PerspectiveQuad {
        return PerspectiveEngine.detectDocumentQuad(source)
    }

    // ==================== 3. STRAIGHTEN & ROTATE ====================

    fun executeStraighten(
        source: Bitmap,
        angleDegrees: Float,
        cropMode: RotateStraightenEngine.StraightenCropMode = RotateStraightenEngine.StraightenCropMode.AUTO_CROP,
        backgroundColor: Int = Color.TRANSPARENT
    ): StraightenResult {
        val rotated = RotateStraightenEngine.rotateCustomAngle(
            bitmap = source,
            angleDegrees = angleDegrees,
            cropMode = cropMode,
            backgroundColor = backgroundColor
        )
        val verification = RotateStraightenEngine.verifyStraighten(
            before = source,
            after = rotated,
            angleDegrees = angleDegrees,
            cropMode = cropMode
        )
        return StraightenResult(
            bitmap = rotated,
            angleDegrees = angleDegrees,
            cropMode = cropMode,
            verification = verification
        )
    }

    fun detectHorizon(source: Bitmap): RotateStraightenEngine.HorizonDetectionResult {
        return RotateStraightenEngine.detectHorizonAngle(source)
    }

    fun executeAutoOrient(source: Bitmap, exifOrientation: Int? = null): RotateStraightenEngine.AutoOrientResult {
        return RotateStraightenEngine.autoOrient(source, exifOrientation)
    }

    fun executeSymmetryMirror(
        source: Bitmap,
        mode: RotateStraightenEngine.FlipSymmetryMode
    ): Bitmap {
        return RotateStraightenEngine.applySymmetryMirror(source, mode)
    }

    // ==================== 4. BACKGROUND ====================

    fun executeBackgroundProcessing(
        source: Bitmap,
        config: BackgroundProcessor.BackgroundConfig,
        customBg: Bitmap? = null
    ): BackgroundResult {
        val processed = BackgroundProcessor.processBackground(
            source = source,
            config = config,
            customImageBg = customBg
        )
        val verification = BackgroundProcessor.verifyBackgroundOutput(
            before = source,
            after = processed,
            mode = config.mode,
            tolerance = config.tolerance
        )
        return BackgroundResult(
            bitmap = processed,
            config = config,
            verification = verification
        )
    }

    fun detectDominantBackgroundColor(source: Bitmap): Int {
        return BackgroundProcessor.sampleCornerBackgroundColor(source)
    }

    // ==================== 5. ADVANCED CROP ====================

    fun executeAdvancedCrop(
        source: Bitmap,
        cropRectNormalized: RectF,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusFraction: Float = 0.18f,
        borderWidthPx: Float = 0f,
        borderColor: Int = Color.WHITE,
        expectedRatio: Float? = null
    ): AdvancedCropResult {
        val cropped = CropEngine.applyAdvancedCropWithBorder(
            bitmap = source,
            cropRectNormalized = cropRectNormalized,
            shape = shape,
            cornerRadiusFraction = cornerRadiusFraction,
            borderWidthPx = borderWidthPx,
            borderColor = borderColor
        )
        val verification = CropEngine.verifyCropOutput(
            bitmap = cropped,
            expectedRatio = expectedRatio,
            shape = shape
        )
        return AdvancedCropResult(
            bitmap = cropped,
            cropRectNormalized = cropRectNormalized,
            shape = shape,
            verification = verification
        )
    }

    fun detectSmartSubjectCrop(
        source: Bitmap,
        targetRatio: Float? = null
    ): RectF {
        return CropEngine.detectSmartSubjectCropRect(source, targetRatio)
    }

    // ==================== 6. OTHER APPROVED ADVANCED FEATURES ====================

    fun executeWatermark(
        source: Bitmap,
        config: AdvancedCreativeEngine.WatermarkConfig
    ): Bitmap {
        return AdvancedCreativeEngine.applyWatermark(source, config)
    }

    fun executeFrameStyling(
        source: Bitmap,
        config: AdvancedCreativeEngine.FrameConfig
    ): Bitmap {
        return AdvancedCreativeEngine.applyFrameStyling(source, config)
    }

    fun executeBlur(source: Bitmap, radius: Int): Bitmap {
        return AdvancedCreativeEngine.applyFastBlur(source, radius)
    }

    fun executePixelate(source: Bitmap, blockSize: Int): Bitmap {
        return AdvancedCreativeEngine.applyPixelate(source, blockSize)
    }

    fun executeRegionRedaction(
        source: Bitmap,
        regions: List<AdvancedCreativeEngine.PrivacyRegion>,
        blockSize: Int = 24,
        blurRadius: Int = 25
    ): Bitmap {
        return AdvancedCreativeEngine.applyRegionRedaction(source, regions, blockSize, blurRadius)
    }

    fun extractColorPalette(
        source: Bitmap,
        maxColors: Int = 6
    ): List<AdvancedCreativeEngine.PaletteSwatch> {
        return AdvancedCreativeEngine.extractColorPalette(source, maxColors)
    }

    fun analyzeQuality(source: Bitmap): AdvancedCreativeEngine.ImageQualityReport {
        return AdvancedCreativeEngine.analyzeImageQuality(source)
    }

    fun computePerceptualHash(source: Bitmap): Long {
        return AdvancedCreativeEngine.computeDHash(source)
    }

    fun compareSimilarity(bitmapA: Bitmap, bitmapB: Bitmap): Int {
        return AdvancedCreativeEngine.calculateSimilarityPercent(bitmapA, bitmapB)
    }
}
