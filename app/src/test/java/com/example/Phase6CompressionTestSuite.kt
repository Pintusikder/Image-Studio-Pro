package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.usecase.CompressImageUseCase
import com.example.model.ExportFormat
import com.example.processing.CompressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE, sdk = [34])
class Phase6CompressionTestSuite {

    private lateinit var testBitmap: Bitmap
    private lateinit var compressUseCase: CompressImageUseCase

    @Before
    fun setup() {
        // Create 1000x800 test bitmap with rich color gradients
        testBitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        for (x in 0 until 1000) {
            for (y in 0 until 800) {
                testBitmap.setPixel(x, y, Color.rgb((x * 3) % 256, (y * 5) % 256, (x + y) % 256))
            }
        }
        compressUseCase = CompressImageUseCase()
    }

    @Test
    fun testQualityCompression_VariousLevels() {
        val q95 = CompressionEngine.compressByQuality(testBitmap, ExportFormat.JPEG, quality = 95)
        val q70 = CompressionEngine.compressByQuality(testBitmap, ExportFormat.JPEG, quality = 70)
        val q30 = CompressionEngine.compressByQuality(testBitmap, ExportFormat.JPEG, quality = 30)

        assertTrue("Q95 (${q95.actualBytes}) should be larger than Q70 (${q70.actualBytes})", q95.actualBytes > q70.actualBytes)
        assertTrue("Q70 (${q70.actualBytes}) should be larger than Q30 (${q30.actualBytes})", q70.actualBytes > q30.actualBytes)

        assertTrue(q95.verificationReport.isValid)
        assertTrue(q95.verificationReport.magicBytesValid)
        assertTrue(q95.verificationReport.isDecodeSafe)
        assertTrue(q95.compressionRatio > 1.0f)
    }

    @Test
    fun testQualityCompression_DifferentFormats() {
        val jpegResult = CompressionEngine.compressByQuality(testBitmap, ExportFormat.JPEG, quality = 85)
        val pngResult = CompressionEngine.compressByQuality(testBitmap, ExportFormat.PNG, quality = 100)
        val webpResult = CompressionEngine.compressByQuality(testBitmap, ExportFormat.WEBP_LOSSY, quality = 85)

        assertTrue(jpegResult.verificationReport.magicBytesValid)
        assertTrue(pngResult.verificationReport.magicBytesValid)
        assertTrue(webpResult.verificationReport.magicBytesValid)

        assertTrue(jpegResult.verificationReport.isDecodeSafe)
        assertTrue(pngResult.verificationReport.isDecodeSafe)
        assertTrue(webpResult.verificationReport.isDecodeSafe)
    }

    @Test
    fun testTargetFileSize_ClosestOptimization() {
        val targetKb = 100
        val result = CompressionEngine.compressToTargetSize(
            source = testBitmap,
            format = ExportFormat.JPEG,
            targetKb = targetKb,
            allowDimensionAdjustment = true
        )

        assertTrue(result.verificationReport.isValid)
        assertTrue("Iterations should be > 1 for binary search", result.iterationsCount > 1)
        assertTrue(result.actualBytes > 0)
        assertTrue(result.verificationReport.isDecodeSafe)
    }

    @Test
    fun testMaximumCeiling_StrictCompliance() {
        val maxKb = 150
        val maxBytes = maxKb * 1024L
        val result = CompressionEngine.compressToMaximumCeiling(
            source = testBitmap,
            format = ExportFormat.JPEG,
            maxKb = maxKb,
            allowDimensionAdjustment = true
        )

        assertTrue("Actual bytes (${result.actualBytes}) must be <= maxBytes ($maxBytes)", result.actualBytes <= maxBytes)
        assertTrue(result.isCompliantWithCeiling)
        assertTrue(result.verificationReport.isCompliantWithCeiling)
        assertTrue(result.verificationReport.isValid)
    }

    @Test
    fun testIterativeOptimization_AdaptiveDownscaling() {
        // Very small budget for 1000x800 gradient image (10 KB ceiling)
        val smallCeilingKb = 10
        val maxBytes = smallCeilingKb * 1024L
        val result = CompressionEngine.compressToMaximumCeiling(
            source = testBitmap,
            format = ExportFormat.JPEG,
            maxKb = smallCeilingKb,
            allowDimensionAdjustment = true
        )

        assertTrue("Must satisfy 10 KB ceiling", result.actualBytes <= maxBytes)
        assertTrue(result.isCompliantWithCeiling)
        assertTrue("Dimensions should be adaptively downscaled for very tight ceiling", result.dimensionsAdjusted)
        assertTrue(result.outputWidth < testBitmap.width || result.outputHeight < testBitmap.height)
    }

    @Test
    fun testExactByteAlignment_LosslessMetadataPadding() {
        val targetKb = 200
        val targetBytes = targetKb * 1024L
        val result = CompressionEngine.compressToTargetSize(
            source = testBitmap,
            format = ExportFormat.JPEG,
            targetKb = targetKb,
            allowDimensionAdjustment = true,
            exactByteAlignment = true
        )

        assertEquals("Should match exact target byte count with padding", targetBytes, result.actualBytes)
        assertTrue(result.isExactByteMatch)
        assertTrue(result.verificationReport.isExactByteMatch)
        assertTrue(result.verificationReport.isDecodeSafe)
    }

    @Test
    fun testActualVerification_ReportDetailsAndDecodeSafety() {
        val result = CompressionEngine.compressByQuality(testBitmap, ExportFormat.JPEG, quality = 80)
        val report = result.verificationReport

        assertTrue(report.isValid)
        assertTrue(report.magicBytesValid)
        assertTrue(report.isDecodeSafe)
        assertEquals(testBitmap.width, report.decodedWidth)
        assertEquals(testBitmap.height, report.decodedHeight)
        assertTrue(report.compressionRatio > 1.0f)
        assertTrue(report.summary.contains("Compressed"))

        // Decode directly from byte array to confirm non-corrupt raster bitmap
        val decoded = BitmapFactory.decodeByteArray(result.encodedBytes, 0, result.encodedBytes.size)
        assertNotNull("Decoded bitmap must not be null", decoded)
        assertEquals(testBitmap.width, decoded.width)
        assertEquals(testBitmap.height, decoded.height)
    }

    @Test
    fun testCompressUseCase_CleanArchitectureMethods() {
        val qualityRes = compressUseCase.compressByQuality(testBitmap, ExportFormat.JPEG, 75)
        assertTrue(qualityRes.verificationReport.isValid)

        val targetRes = compressUseCase.compressToTargetSize(testBitmap, ExportFormat.JPEG, 120)
        assertTrue(targetRes.verificationReport.isValid)

        val maxRes = compressUseCase.compressToMaximumCeiling(testBitmap, ExportFormat.JPEG, 80)
        assertTrue(maxRes.isCompliantWithCeiling)

        val verifyReport = compressUseCase.verifyCompressedBytes(
            bytes = qualityRes.encodedBytes,
            format = ExportFormat.JPEG,
            quality = 75,
            requestedTargetBytes = null,
            requestedMaxBytes = null,
            sourceWidth = testBitmap.width,
            sourceHeight = testBitmap.height
        )
        assertTrue(verifyReport.isValid)
        assertTrue(verifyReport.isDecodeSafe)
    }
}
