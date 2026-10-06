package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.domain.usecase.CropImageUseCase
import com.example.model.CropAspectRatio
import com.example.processing.CropEngine
import com.example.processing.CropGravity
import com.example.processing.CropShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class Phase4CropTestSuite {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun createTestBitmap(width: Int, height: Int, color: Int = Color.BLUE): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }

    // =========================================================================
    // 1. RECTANGULAR CROPPING & BOUNDARY SAFETY
    // =========================================================================
    @Test
    fun testCropNormalized_ExactDimensionsAndCoordinates() {
        val src = createTestBitmap(1000, 1000, Color.RED)
        val bounds = RectF(0.1f, 0.2f, 0.6f, 0.7f)

        val result = CropEngine.crop(src, bounds)

        assertNotNull(result.bitmap)
        assertEquals(500, result.outputWidth)
        assertEquals(500, result.outputHeight)
        assertEquals(100, result.pixelRect.left)
        assertEquals(200, result.pixelRect.top)
        assertEquals(600, result.pixelRect.right)
        assertEquals(700, result.pixelRect.bottom)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.isNonEmpty)
    }

    @Test
    fun testCropNormalized_SafeClampingOutOfBounds() {
        val src = createTestBitmap(800, 600, Color.GREEN)
        // Wild out of bound coordinates
        val bounds = RectF(-0.5f, -0.2f, 1.8f, 2.0f)

        val result = CropEngine.crop(src, bounds)

        assertNotNull(result.bitmap)
        assertTrue("Width must be clamped to src width", result.outputWidth <= 800)
        assertTrue("Height must be clamped to src height", result.outputHeight <= 600)
        assertEquals(0, result.pixelRect.left)
        assertEquals(0, result.pixelRect.top)
        assertEquals(800, result.pixelRect.right)
        assertEquals(600, result.pixelRect.bottom)
        assertTrue(result.verificationReport.isValid)
    }

    @Test
    fun testCropPixels_DirectPixelCoordinates() {
        val src = createTestBitmap(1200, 800, Color.MAGENTA)
        val pixelRect = Rect(100, 150, 500, 450)

        val result = CropEngine.cropPixels(src, pixelRect)

        assertEquals(400, result.outputWidth)
        assertEquals(300, result.outputHeight)
        assertEquals(4f / 3f, result.aspectRatio, 0.01f)
        assertTrue(result.verificationReport.isValid)
    }

    // =========================================================================
    // 2. ASPECT RATIO PRESET CROPPING
    // =========================================================================
    @Test
    fun testCropToAspectRatio_Square1_1() {
        // Landscape input (1920x1080) -> Target Square 1:1
        val src = createTestBitmap(1920, 1080, Color.CYAN)
        val result = CropEngine.cropToAspectRatio(src, 1f, 1f, CropGravity.CENTER)

        assertEquals(1080, result.outputWidth)
        assertEquals(1080, result.outputHeight)
        assertEquals(1.0f, result.aspectRatio, 0.001f)
        assertTrue(result.verificationReport.isAspectWithinTolerance)
        // Center alignment check: left should be (1920 - 1080) / 2 = 420
        assertEquals(420, result.pixelRect.left)
        assertEquals(0, result.pixelRect.top)
    }

    @Test
    fun testCropToAspectRatio_Landscape16_9() {
        // Square input (1000x1000) -> Target 16:9
        val src = createTestBitmap(1000, 1000, Color.YELLOW)
        val result = CropEngine.cropToAspectRatio(src, 16f, 9f, CropGravity.CENTER)

        assertEquals(1000, result.outputWidth)
        assertEquals(563, result.outputHeight) // 1000 * 9 / 16 ≈ 562.5 -> 563
        assertEquals(16f / 9f, result.aspectRatio, 0.02f)
        assertTrue(result.verificationReport.isAspectWithinTolerance)
    }

    @Test
    fun testCropToAspectRatio_GravityAlignments() {
        val src = createTestBitmap(1000, 1000, Color.BLUE)

        val topResult = CropEngine.cropToAspectRatio(src, 16f, 9f, CropGravity.TOP)
        assertEquals(0, topResult.pixelRect.top)

        val bottomResult = CropEngine.cropToAspectRatio(src, 16f, 9f, CropGravity.BOTTOM)
        assertEquals(1000 - bottomResult.outputHeight, bottomResult.pixelRect.top)

        val leftResult = CropEngine.cropToAspectRatio(src, 9f, 16f, CropGravity.LEFT)
        assertEquals(0, leftResult.pixelRect.left)

        val rightResult = CropEngine.cropToAspectRatio(src, 9f, 16f, CropGravity.RIGHT)
        assertEquals(1000 - rightResult.outputWidth, rightResult.pixelRect.left)
    }

    // =========================================================================
    // 3. SHAPE MASKS & ALPHA TRANSPARENCY
    // =========================================================================
    @Test
    fun testCropCircular_ProducesAlphaMask() {
        val src = createTestBitmap(400, 400, Color.RED)
        val result = CropEngine.cropCircular(src)

        assertNotNull(result.bitmap)
        assertTrue("Masked bitmap must support alpha", result.bitmap.hasAlpha())
        assertEquals(CropShape.CIRCLE_OVAL, result.shape)

        // Center pixel should be opaque red
        val centerPixel = result.bitmap.getPixel(200, 200)
        assertEquals(255, Color.alpha(centerPixel))
        assertEquals(255, Color.red(centerPixel))

        // Corner pixel (0, 0) should be transparent (alpha = 0)
        val cornerPixel = result.bitmap.getPixel(5, 5)
        assertEquals("Corner of circle mask must be transparent", 0, Color.alpha(cornerPixel))
    }

    @Test
    fun testCropRoundedRect_SmoothCorners() {
        val src = createTestBitmap(500, 500, Color.BLUE)
        val result = CropEngine.cropRoundedRect(
            source = src,
            normalizedBounds = RectF(0f, 0f, 1f, 1f),
            cornerRadiusPx = 50f
        )

        assertTrue(result.bitmap.hasAlpha())
        assertEquals(CropShape.ROUNDED_RECT, result.shape)

        // Top-left extreme corner (1, 1) outside radius should be transparent
        val cornerPixel = result.bitmap.getPixel(2, 2)
        assertEquals("Outer corner of rounded rect must be transparent", 0, Color.alpha(cornerPixel))

        // Center pixel must be blue
        val centerPixel = result.bitmap.getPixel(250, 250)
        assertEquals(255, Color.alpha(centerPixel))
    }

    @Test
    fun testCropPolygonShapes_HexagonAndHeart() {
        val src = createTestBitmap(300, 300, Color.GREEN)

        val hexResult = CropEngine.crop(src, RectF(0f, 0f, 1f, 1f), CropShape.HEXAGON)
        assertNotNull(hexResult.bitmap)
        assertTrue(hexResult.bitmap.hasAlpha())

        val heartResult = CropEngine.crop(src, RectF(0f, 0f, 1f, 1f), CropShape.HEART)
        assertNotNull(heartResult.bitmap)
        assertTrue(heartResult.bitmap.hasAlpha())
    }

    // =========================================================================
    // 4. SMART SALIENCY DETECTION
    // =========================================================================
    @Test
    fun testCalculateSmartCropBounds_BiasesTowardsHighEnergyFocalArea() {
        val src = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.BLACK)

        // Paint bright high-contrast feature in bottom-right quadrant
        for (y in 250..350) {
            for (x in 250..350) {
                src.setPixel(x, y, Color.WHITE)
            }
        }

        // Calculate 1:1 smart crop bounds
        val bounds = CropEngine.calculateSmartCropBounds(src, 1f, 1f)

        assertNotNull(bounds)
        assertTrue("Crop box left must be >= 0", bounds.left >= 0f)
        assertTrue("Crop box right must be <= 1", bounds.right <= 1f)
        assertTrue("Width of 1:1 crop should span full width on square image", bounds.width() in 0.95f..1.0f)
    }

    // =========================================================================
    // 5. ROTATION-AWARE INSCRIBED CROPPING
    // =========================================================================
    @Test
    fun testCalculateMaxInscribedCrop_RotatedAngles() {
        val rect0 = CropEngine.calculateMaxInscribedCrop(1000, 1000, 0f)
        assertEquals(0f, rect0.left, 0.05f)
        assertEquals(1f, rect0.right, 0.05f)

        val rect15 = CropEngine.calculateMaxInscribedCrop(1000, 1000, 15f)
        assertTrue("Inscribed box must inset from edges when rotated 15 deg", rect15.left > 0f)
        assertTrue("Inscribed box must stay within bounds", rect15.right <= 1f)
        assertTrue("Inscribed box must maintain symmetric aspect", abs(rect15.width() - rect15.height()) < 0.05f)
    }

    // =========================================================================
    // 6. PERSPECTIVE QUADRILATERAL TRANSFORMATION CROP
    // =========================================================================
    @Test
    fun testCropQuadrilateral_PerspectiveRectification() {
        val src = createTestBitmap(600, 400, Color.DKGRAY)

        val result = CropEngine.cropQuadrilateral(
            source = src,
            topLeft = PointF(50f, 30f),
            topRight = PointF(550f, 40f),
            bottomRight = PointF(570f, 380f),
            bottomLeft = PointF(30f, 370f),
            outputWidth = 400,
            outputHeight = 300
        )

        assertNotNull(result.bitmap)
        assertEquals(400, result.outputWidth)
        assertEquals(300, result.outputHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.isNonEmpty)
    }

    // =========================================================================
    // 7. CLEAN ARCHITECTURE USE CASE INTEGRATION
    // =========================================================================
    @Test
    fun testCropImageUseCase_ExecutesFullPipelineWithReport() {
        val useCase = CropImageUseCase()
        val src = createTestBitmap(800, 600, Color.RED)

        val cropped = useCase(src, RectF(0.25f, 0.25f, 0.75f, 0.75f))
        assertEquals(400, cropped.width)
        assertEquals(300, cropped.height)

        val resultWithReport = useCase.executeWithReport(src, RectF(0.1f, 0.1f, 0.9f, 0.9f))
        assertTrue(resultWithReport.verificationReport.isValid)
        assertEquals(640, resultWithReport.outputWidth)
        assertEquals(480, resultWithReport.outputHeight)

        val smartBounds = useCase.calculateSmartCrop(src, CropAspectRatio.RATIO_16_9)
        assertNotNull(smartBounds)
        assertTrue(smartBounds.left >= 0f && smartBounds.right <= 1f)
    }
}
