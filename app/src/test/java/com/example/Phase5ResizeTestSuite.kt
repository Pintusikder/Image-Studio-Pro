package com.example

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.usecase.ResizeImageUseCase
import com.example.model.ResizeMode
import com.example.processing.ResizeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [34])
class Phase5ResizeTestSuite {

    private lateinit var testBitmap: Bitmap
    private lateinit var resizeUseCase: ResizeImageUseCase

    @Before
    fun setup() {
        // Create 800x400 (2:1 aspect ratio) sample bitmap with test content
        testBitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        for (x in 0 until 800) {
            for (y in 0 until 400) {
                testBitmap.setPixel(x, y, Color.rgb((x % 255), (y % 255), 180))
            }
        }
        resizeUseCase = ResizeImageUseCase()
    }

    @Test
    fun testResize_Stretch_ExactDimensions() {
        val result = ResizeEngine.resize(testBitmap, 300, 100, ResizeMode.STRETCH)
        assertEquals(300, result.actualWidth)
        assertEquals(100, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactDimensionMatch)
        assertTrue(result.verificationReport.isNonEmpty)
    }

    @Test
    fun testResize_Fit_LetterboxAndPadding() {
        // Source 800x400 (2:1) resized into 500x500 square frame
        val result = ResizeEngine.resize(
            source = testBitmap,
            targetWidth = 500,
            targetHeight = 500,
            mode = ResizeMode.FIT,
            fitBackgroundColor = Color.BLACK
        )
        assertEquals(500, result.actualWidth)
        assertEquals(500, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactDimensionMatch)
        assertTrue(result.verificationReport.isAspectPreserved)
    }

    @Test
    fun testResize_Fill_CenterCrop() {
        // Source 800x400 (2:1) filled into 400x400 square frame
        val result = ResizeEngine.resize(
            source = testBitmap,
            targetWidth = 400,
            targetHeight = 400,
            mode = ResizeMode.FILL
        )
        assertEquals(400, result.actualWidth)
        assertEquals(400, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactDimensionMatch)
    }

    @Test
    fun testResize_SmartCrop_SaliencyCentered() {
        val result = ResizeEngine.resize(
            source = testBitmap,
            targetWidth = 300,
            targetHeight = 300,
            mode = ResizeMode.SMART_CROP
        )
        assertEquals(300, result.actualWidth)
        assertEquals(300, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactDimensionMatch)
        assertTrue(result.verificationReport.isNonEmpty)
    }

    @Test
    fun testResize_ExactWidth_CalculatesProportionalHeight() {
        // Source is 800x400 (2:1). Given exact width 400, height must be 200.
        val result = ResizeEngine.resizeExactWidth(testBitmap, 400)
        assertEquals(400, result.actualWidth)
        assertEquals(200, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactWidthMatch)
        assertTrue(result.verificationReport.isExactHeightMatch)
    }

    @Test
    fun testResize_ExactHeight_CalculatesProportionalWidth() {
        // Source is 800x400 (2:1). Given exact height 100, width must be 200.
        val result = ResizeEngine.resizeExactHeight(testBitmap, 100)
        assertEquals(200, result.actualWidth)
        assertEquals(100, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
        assertTrue(result.verificationReport.isExactWidthMatch)
        assertTrue(result.verificationReport.isExactHeightMatch)
    }

    @Test
    fun testResize_ExactWidthAndHeight_AllModes() {
        val modes = listOf(ResizeMode.FIT, ResizeMode.FILL, ResizeMode.STRETCH, ResizeMode.SMART_CROP)
        for (mode in modes) {
            val result = ResizeEngine.resizeExactWidthAndHeight(testBitmap, 600, 300, mode)
            assertEquals(600, result.actualWidth)
            assertEquals(300, result.actualHeight)
            assertTrue("Mode $mode failed verification", result.verificationReport.isValid)
            assertTrue("Mode $mode dimension mismatch", result.verificationReport.isExactDimensionMatch)
        }
    }

    @Test
    fun testResizeVerification_ReportDetails() {
        val result = ResizeEngine.resize(testBitmap, 1080, 1080, ResizeMode.STRETCH)
        val report = result.verificationReport

        assertTrue(report.isValid)
        assertEquals(1080, report.requestedWidth)
        assertEquals(1080, report.requestedHeight)
        assertEquals(1080, report.actualWidth)
        assertEquals(1080, report.actualHeight)
        assertTrue(report.isExactWidthMatch)
        assertTrue(report.isExactHeightMatch)
        assertTrue(report.isExactDimensionMatch)
        assertTrue(report.isNonEmpty)
        assertTrue(report.memoryFootprintBytes > 0)
        assertTrue(report.summary.contains("PASS"))
    }

    @Test
    fun testResizeUseCase_ExecuteWithReport() {
        val result = resizeUseCase.executeWithReport(testBitmap, 413, 531, ResizeMode.FILL)
        assertNotNull(result.bitmap)
        assertEquals(413, result.actualWidth)
        assertEquals(531, result.actualHeight)
        assertTrue(result.verificationReport.isValid)
    }

    @Test
    fun testResizeUseCase_ExactWidthAndHeightMethods() {
        val widthRes = resizeUseCase.resizeExactWidth(testBitmap, 500)
        assertEquals(500, widthRes.actualWidth)
        assertEquals(250, widthRes.actualHeight)

        val heightRes = resizeUseCase.resizeExactHeight(testBitmap, 300)
        assertEquals(600, heightRes.actualWidth)
        assertEquals(300, heightRes.actualHeight)

        val exactRes = resizeUseCase.resizeExactWidthAndHeight(testBitmap, 1200, 630, ResizeMode.STRETCH)
        assertEquals(1200, exactRes.actualWidth)
        assertEquals(630, exactRes.actualHeight)
        assertTrue(exactRes.verificationReport.isExactDimensionMatch)
    }
}
