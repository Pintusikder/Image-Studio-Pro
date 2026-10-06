package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import com.example.domain.usecase.AdvancedEditingUseCase
import com.example.processing.AdvancedCreativeEngine
import com.example.processing.BackgroundProcessor
import com.example.processing.CropShape
import com.example.processing.ImageEnhancer
import com.example.processing.PerspectiveEngine
import com.example.processing.RotateStraightenEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase10AdvancedEditingTestSuite {

    private lateinit var advancedUseCase: AdvancedEditingUseCase

    @Before
    fun setUp() {
        advancedUseCase = AdvancedEditingUseCase()
    }

    private fun createSyntheticDocumentBitmap(width: Int = 240, height: Int = 180): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val isBorder = x < 24 || x >= width - 24 || y < 20 || y >= height - 20
                if (isBorder) {
                    // Light warm background around perimeter
                    pixels[y * width + x] = Color.rgb(245, 244, 240)
                } else {
                    // Rich colorful inner subject with dark text/detail lines
                    val line = (y % 18 < 3) && (x in 40..(width - 40))
                    if (line) {
                        pixels[y * width + x] = Color.rgb(25, 35, 55)
                    } else {
                        val r = (80 + (x * 140) / width).coerceIn(0, 255)
                        val g = (60 + (y * 150) / height).coerceIn(0, 255)
                        val b = 180
                        pixels[y * width + x] = Color.rgb(r, g, b)
                    }
                }
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    @Test
    fun `1 - Enhancement Pipeline applies brightness contrast vibrance gamma CLAHE and verifies output`() {
        val source = createSyntheticDocumentBitmap(200, 150)
        val params = ImageEnhancer.EnhancementParameters(
            brightness = 18f,
            contrast = 22f,
            saturation = 12f,
            vibrance = 25f,
            warmth = 10f,
            sharpness = 20f,
            gamma = 1.15f,
            claheStrength = 35f,
            vignette = 15f
        )

        val result = advancedUseCase.executeEnhancement(source, params)
        assertNotNull(result.bitmap)
        assertEquals(200, result.bitmap.width)
        assertEquals(150, result.bitmap.height)
        assertTrue(result.verification.isValid)
        assertTrue(result.verification.widthPreserved)
        assertTrue(result.verification.heightPreserved)
        assertTrue(result.verification.isModifiedWhenNonNeutral)
        assertEquals(256, result.histogram.luminance.size)
        assertTrue(result.verification.summary.isNotBlank())
    }

    @Test
    fun `2 - Enhancement Auto Enhance computes adaptive parameters and preserves dimensions`() {
        val source = createSyntheticDocumentBitmap(180, 140)
        val result = advancedUseCase.executeAutoEnhancement(source)
        assertFalse(result.parameters.isNeutral)
        assertTrue(result.verification.isValid)
        assertEquals(180, result.bitmap.width)
        assertEquals(140, result.bitmap.height)
    }

    @Test
    fun `3 - Perspective 4-corner homography warp Deskew and Keystone tilt produce valid rectified output`() {
        val source = createSyntheticDocumentBitmap(240, 180)
        val quad = PerspectiveEngine.PerspectiveQuad(
            topLeft = PerspectiveEngine.PointFNormalized(0.08f, 0.10f),
            topRight = PerspectiveEngine.PointFNormalized(0.92f, 0.06f),
            bottomRight = PerspectiveEngine.PointFNormalized(0.95f, 0.92f),
            bottomLeft = PerspectiveEngine.PointFNormalized(0.05f, 0.90f)
        )

        val warpResult = advancedUseCase.executePerspectiveWarp(
            source = source,
            quad = quad,
            preset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
            enhanceMode = PerspectiveEngine.DocumentEnhanceMode.MAGIC_CLEAN
        )
        assertTrue(warpResult.verification.isValid)
        assertTrue(warpResult.verification.dimensionsMatchPreset)
        assertTrue(warpResult.bitmap.width > 0 && warpResult.bitmap.height > 0)

        // Test Vertical + Horizontal Keystone Tilt
        val tiltResult = advancedUseCase.executeKeystoneTilt(
            source = source,
            verticalTilt = 20f,
            horizontalTilt = -12f
        )
        assertTrue(tiltResult.verification.isValid)
        assertTrue(tiltResult.verification.dimensionsMatchPreset)
    }

    @Test
    fun `4 - Straighten with auto-crop removes empty wedges and verifies pixel integrity`() {
        val source = createSyntheticDocumentBitmap(220, 160)
        val straightenWithCrop = advancedUseCase.executeStraighten(
            source = source,
            angleDegrees = 7.5f,
            cropMode = RotateStraightenEngine.StraightenCropMode.AUTO_CROP
        )
        assertTrue(straightenWithCrop.verification.isValid)
        assertTrue(straightenWithCrop.bitmap.width in 50..220)
        assertTrue(straightenWithCrop.bitmap.height in 50..160)

        // Also test auto-horizon detection & symmetry mirror
        val horizon = advancedUseCase.detectHorizon(source)
        assertTrue(horizon.angleDegrees in -45f..45f)

        val mirrored = advancedUseCase.executeSymmetryMirror(
            source = source,
            mode = RotateStraightenEngine.FlipSymmetryMode.LEFT_TO_RIGHT
        )
        assertEquals(220, mirrored.width)
        assertEquals(160, mirrored.height)
    }

    @Test
    fun `5 - Background Processor supports Transparent Solid Blur and Gradient modes with verification`() {
        val source = createSyntheticDocumentBitmap(200, 160)

        // Transparent mode
        val transparentRes = advancedUseCase.executeBackgroundProcessing(
            source = source,
            config = BackgroundProcessor.BackgroundConfig(
                mode = BackgroundProcessor.BgMode.TRANSPARENT,
                tolerance = 38f,
                featherRadius = 2f
            )
        )
        assertTrue(transparentRes.verification.isValid)
        assertEquals(200, transparentRes.bitmap.width)
        assertEquals(160, transparentRes.bitmap.height)
        assertEquals(0, Color.alpha(transparentRes.bitmap.getPixel(2, 2)))

        // Blur Bokeh mode
        val blurRes = advancedUseCase.executeBackgroundProcessing(
            source = source,
            config = BackgroundProcessor.BackgroundConfig(
                mode = BackgroundProcessor.BgMode.BLUR_ORIGINAL,
                blurRadius = 12,
                tolerance = 38f
            )
        )
        assertTrue(blurRes.verification.isValid)
        assertEquals(200, blurRes.bitmap.width)
        assertEquals(160, blurRes.bitmap.height)
    }

    @Test
    fun `6 - Advanced Crop supports geometric shapes and decorative border`() {
        val source = createSyntheticDocumentBitmap(240, 240)
        val cropRect = RectF(0.1f, 0.1f, 0.9f, 0.9f)

        for (shape in listOf(
            CropShape.RECTANGLE,
            CropShape.ROUNDED_RECT,
            CropShape.CIRCLE_OVAL,
            CropShape.HEXAGON,
            CropShape.HEART,
            CropShape.STAR,
            CropShape.BADGE
        )) {
            val res = advancedUseCase.executeAdvancedCrop(
                source = source,
                cropRectNormalized = cropRect,
                shape = shape,
                cornerRadiusFraction = 0.18f,
                borderWidthPx = 4f,
                borderColor = Color.WHITE
            )
            assertTrue("Failed for shape $shape", res.verification.isValid)
            assertTrue(res.bitmap.width > 0 && res.bitmap.height > 0)
        }
    }

    @Test
    fun `7 - Approved Creative and Diagnostic Features Watermark Frame Redaction Sharpness PerceptualHash Palette`() {
        val source = createSyntheticDocumentBitmap(220, 160)

        // Watermark
        val watermarked = advancedUseCase.executeWatermark(
            source = source,
            config = AdvancedCreativeEngine.WatermarkConfig(
                text = "CONFIDENTIAL",
                opacity = 0.45f,
                position = AdvancedCreativeEngine.WatermarkPosition.TILED
            )
        )
        assertEquals(220, watermarked.width)
        assertEquals(160, watermarked.height)

        // Decorative Frame & Drop Shadow
        val framed = advancedUseCase.executeFrameStyling(
            source = source,
            config = AdvancedCreativeEngine.FrameConfig(
                borderWidthPx = 12,
                borderColor = Color.WHITE,
                cornerRadiusPx = 16,
                addDropShadow = true
            )
        )
        assertTrue(framed.width > source.width)
        assertTrue(framed.height > source.height)

        // Privacy Region Redaction
        val redacted = advancedUseCase.executeRegionRedaction(
            source = source,
            regions = listOf(
                AdvancedCreativeEngine.PrivacyRegion(
                    normalizedRect = RectF(0.25f, 0.25f, 0.75f, 0.75f),
                    mode = AdvancedCreativeEngine.RedactionMode.PIXELATE
                )
            )
        )
        assertEquals(220, redacted.width)
        assertEquals(160, redacted.height)

        // Diagnostics: Sharpness, Perceptual Hash, and Dominant Color Palette
        val quality = advancedUseCase.analyzeQuality(source)
        assertTrue(quality.sharpnessScore >= 0f)
        assertEquals(100, advancedUseCase.compareSimilarity(source, source))

        val palette = advancedUseCase.extractColorPalette(source, 6)
        assertTrue(palette.isNotEmpty())
        assertTrue(palette.first().hexCode.startsWith("#"))
    }
}
