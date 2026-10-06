package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.usecase.ConvertFormatUseCase
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.FormatConversionPair
import com.example.processing.FormatConversionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [34])
class Phase7FormatConversionTestSuite {

    private lateinit var testOpaqueBitmap: Bitmap
    private lateinit var testAlphaBitmap: Bitmap
    private lateinit var convertUseCase: ConvertFormatUseCase

    @Before
    fun setup() {
        // Create 400x300 opaque bitmap
        testOpaqueBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) {
            for (y in 0 until 300) {
                testOpaqueBitmap.setPixel(x, y, Color.rgb((x * 2) % 256, (y * 3) % 256, (x + y) % 256))
            }
        }

        // Create 400x300 bitmap with transparent alpha areas
        testAlphaBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        for (x in 0 until 400) {
            for (y in 0 until 300) {
                if (x < 200) {
                    testAlphaBitmap.setPixel(x, y, Color.argb(128, 255, 0, 0))
                } else {
                    testAlphaBitmap.setPixel(x, y, Color.argb(0, 0, 0, 0)) // fully transparent
                }
            }
        }

        convertUseCase = ConvertFormatUseCase()
    }

    @Test
    fun testConversion_ToJpeg_MimeAndMagicHeader() {
        val res = FormatConversionEngine.convert(
            source = testOpaqueBitmap,
            fromFormat = DetectedImageFormat.PNG,
            toFormat = ExportFormat.JPEG,
            quality = 90
        )

        assertTrue("Report must be valid", res.verificationReport.isValid)
        assertEquals("image/jpeg", res.detectedMimeType)
        assertEquals("image/jpeg", res.expectedMimeType)
        assertTrue(res.isMimeTypeVerified)
        assertTrue(res.verificationReport.isMagicHeaderValid)
        assertTrue(res.isDecodeSafe)
        assertTrue(res.actualBytes > 0)
    }

    @Test
    fun testConversion_ToPng_MimeAndMagicHeader() {
        val res = FormatConversionEngine.convert(
            source = testAlphaBitmap,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.PNG,
            quality = 100
        )

        assertTrue(res.verificationReport.isValid)
        assertEquals("image/png", res.detectedMimeType)
        assertEquals("image/png", res.expectedMimeType)
        assertTrue(res.isMimeTypeVerified)
        assertTrue(res.verificationReport.isMagicHeaderValid)
        assertTrue(res.isDecodeSafe)
    }

    @Test
    fun testConversion_ToWebp_LossyAndLossless() {
        val lossy = FormatConversionEngine.convert(
            source = testOpaqueBitmap,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.WEBP_LOSSY,
            quality = 85
        )
        assertTrue(lossy.verificationReport.isValid)
        assertEquals("image/webp", lossy.expectedMimeType)
        assertTrue(lossy.verificationReport.isMagicHeaderValid)
        assertTrue(lossy.isDecodeSafe)

        val lossless = FormatConversionEngine.convert(
            source = testAlphaBitmap,
            fromFormat = DetectedImageFormat.PNG,
            toFormat = ExportFormat.WEBP_LOSSLESS,
            quality = 100
        )
        assertTrue(lossless.verificationReport.isValid)
        assertEquals("image/webp", lossless.expectedMimeType)
        assertTrue(lossless.verificationReport.isMagicHeaderValid)
        assertTrue(lossless.isDecodeSafe)
    }

    @Test
    fun testConversion_ToPdf_DocumentIntegrity() {
        val res = FormatConversionEngine.convert(
            source = testOpaqueBitmap,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.PDF
        )

        assertTrue(res.verificationReport.isValid)
        assertEquals("application/pdf", res.detectedMimeType)
        assertEquals("application/pdf", res.expectedMimeType)
        assertTrue(res.verificationReport.isMagicHeaderValid)
        assertTrue(res.isDecodeSafe)
        assertTrue(res.actualBytes > 100)
    }

    @Test
    fun testConversion_AlphaMatteHandlingForJpeg() {
        val res = FormatConversionEngine.convert(
            source = testAlphaBitmap,
            fromFormat = DetectedImageFormat.PNG,
            toFormat = ExportFormat.JPEG,
            quality = 90,
            backgroundColor = Color.WHITE
        )

        assertTrue(res.alphaMatteApplied)
        assertTrue(res.verificationReport.isValid)
        assertEquals("image/jpeg", res.detectedMimeType)

        // Decode result and verify no transparency remains in JPEG
        val decoded = BitmapFactory.decodeByteArray(res.encodedBytes, 0, res.encodedBytes.size)
        assertNotNull(decoded)
        assertEquals(testAlphaBitmap.width, decoded.width)
        assertEquals(testAlphaBitmap.height, decoded.height)
    }

    @Test
    fun testConversion_FormatConversionPairs() {
        val pairs = listOf(
            FormatConversionPair.JPG_TO_PNG,
            FormatConversionPair.JPG_TO_WEBP,
            FormatConversionPair.JPG_TO_PDF,
            FormatConversionPair.PNG_TO_JPG,
            FormatConversionPair.PNG_TO_WEBP,
            FormatConversionPair.PNG_TO_PDF,
            FormatConversionPair.WEBP_TO_JPG,
            FormatConversionPair.WEBP_TO_PNG,
            FormatConversionPair.WEBP_TO_PDF,
            FormatConversionPair.BMP_TO_JPG,
            FormatConversionPair.BMP_TO_PNG,
            FormatConversionPair.GIF_TO_PNG,
            FormatConversionPair.HEIC_TO_JPG
        )

        for (pair in pairs) {
            val source = if (pair.fromFormat.supportsAlpha) testAlphaBitmap else testOpaqueBitmap
            val result = FormatConversionEngine.convertPair(source, pair)
            assertTrue("Pair ${pair.label} must produce valid verification", result.verificationReport.isValid)
            assertTrue("Pair ${pair.label} must be decode safe", result.isDecodeSafe)
            assertEquals("Pair ${pair.label} must match target format", pair.toFormat, result.toFormat)
        }
    }

    @Test
    fun testConversion_DiskFileVerification() {
        val res = FormatConversionEngine.convert(
            source = testOpaqueBitmap,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.PNG
        )

        val tempFile = File.createTempFile("test_verify_phase7", ".png")
        tempFile.deleteOnExit()
        tempFile.writeBytes(res.encodedBytes)

        val fileReport = FormatConversionEngine.verifyConvertedFile(tempFile, ExportFormat.PNG)
        assertTrue("Disk file report must be valid", fileReport.isValid)
        assertEquals("image/png", fileReport.detectedMimeType)
        assertEquals("png", fileReport.expectedExtension)
        assertEquals(tempFile.length(), fileReport.actualBytes)
    }

    @Test
    fun testConvertUseCase_CleanArchitecture() {
        val result = convertUseCase.convert(
            source = testOpaqueBitmap,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.PNG,
            quality = 95
        )
        assertTrue(result.verificationReport.isValid)
        assertEquals("image/png", result.detectedMimeType)

        val pairResult = convertUseCase.convertPair(testOpaqueBitmap, FormatConversionPair.JPG_TO_WEBP)
        assertTrue(pairResult.verificationReport.isValid)

        val verifyBytesReport = convertUseCase.verifyBytes(
            bytes = result.encodedBytes,
            fromFormat = DetectedImageFormat.JPEG,
            toFormat = ExportFormat.PNG,
            sourceWidth = testOpaqueBitmap.width,
            sourceHeight = testOpaqueBitmap.height
        )
        assertTrue(verifyBytesReport.isValid)
    }
}
