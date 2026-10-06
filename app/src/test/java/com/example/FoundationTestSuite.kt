package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.ExportFormat
import com.example.model.NamingMode
import com.example.model.OutputFileConfiguration
import com.example.model.ResizeMode
import com.example.processing.BitmapUtils
import com.example.processing.ExportEngine
import com.example.processing.ImageEnhancer
import com.example.processing.NonDestructivePipeline
import com.example.processing.OutputFileManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FoundationTestSuite {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // =========================================================================
    // 1. FILE PICKER & DECODING FOUNDATION
    // =========================================================================
    @Test
    fun testFilePickerDecoding_ValidImageLoadsCleanly() {
        val testFile = File(context.cacheDir, "foundation_test_image.jpg")
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        val uri = Uri.fromFile(testFile)

        val decoded = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1024, 1024)
        assertNotNull("Valid JPEG URI must decode successfully", decoded)
        assertEquals(400, decoded!!.width)
        assertEquals(300, decoded.height)

        testFile.delete()
    }

    @Test
    fun testFilePickerDecoding_CorruptedDataHandledGracefully() {
        val corruptedFile = File(context.cacheDir, "corrupted_foundation_file.bin")
        corruptedFile.writeText("THIS IS NOT A VALID IMAGE STREAM 12345")
        val uri = Uri.fromFile(corruptedFile)

        val decoded = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1024, 1024)
        assertNull("Corrupted non-image file must safely return null without throwing", decoded)

        corruptedFile.delete()
    }

    // =========================================================================
    // 2. PROCESSING FRAMEWORK & NON-DESTRUCTIVE PIPELINE
    // =========================================================================
    @Test
    fun testProcessingFramework_AppliesColorEnhancementCleanly() = runTest {
        val src = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val params = ImageEnhancer.EnhancementParameters(
            brightness = 10f,
            contrast = 15f,
            saturation = 5f
        )
        val result = ImageEnhancer.applyEnhancements(src, params)
        assertNotNull(result)
        assertEquals(200, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun testProcessingFramework_BicubicResizeAndCropModes() {
        val src = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)

        // Fit mode
        val fitBmp = BitmapUtils.resizeBitmapWithMode(src, 200, 200, ResizeMode.FIT)
        assertEquals(200, fitBmp.width)
        assertEquals(200, fitBmp.height)

        // Stretch mode
        val stretchBmp = BitmapUtils.resizeBitmapWithMode(src, 300, 300, ResizeMode.STRETCH)
        assertEquals(300, stretchBmp.width)
        assertEquals(300, stretchBmp.height)
    }

    // =========================================================================
    // 3. EXPORT FRAMEWORK & EXACT COMPRESSION
    // =========================================================================
    @Test
    fun testExportFramework_GeneratesValidJpegAndVerificationReport() {
        val src = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val exportResult = ExportEngine.exportImage(
            context = context,
            bitmap = src,
            format = ExportFormat.JPEG,
            quality = 85,
            dpi = 300
        )

        assertTrue(exportResult.success)
        assertNotNull(exportResult.outputFile)
        assertTrue(exportResult.outputFile!!.exists())
        assertTrue(exportResult.fileSizeBytes > 0)
        assertNotNull(exportResult.verificationReport)
        assertTrue(exportResult.verificationReport!!.allMeasurablePassed)

        exportResult.outputFile?.delete()
    }

    @Test
    fun testExportFramework_TargetsExactFileSizeCeiling() {
        val src = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
        val targetMaxKb = 50

        val exportResult = ExportEngine.exportImage(
            context = context,
            bitmap = src,
            format = ExportFormat.JPEG,
            targetSizeKb = targetMaxKb
        )

        assertTrue(exportResult.success)
        val sizeKb = exportResult.fileSizeBytes / 1024.0
        assertTrue("Exported file ($sizeKb KB) must be within ceiling of $targetMaxKb KB", sizeKb <= targetMaxKb + 2.0)

        exportResult.outputFile?.delete()
    }

    // =========================================================================
    // 4. FILENAME SANITIZATION & ERROR ARCHITECTURE
    // =========================================================================
    @Test
    fun testFilenameSanitizer_BlocksPathTraversalAndReservedNames() {
        // Path traversal attack vector
        val traversalResult = OutputFileManager.sanitizeFilename("../../etc/passwd.png")
        assertFalse(traversalResult.sanitizedName.contains(".."))
        assertFalse(traversalResult.sanitizedName.contains("/"))
        assertTrue(traversalResult.hasPathTraversal)

        // Windows / Android reserved device names
        val reservedResult = OutputFileManager.sanitizeFilename("CON.jpg")
        assertTrue(reservedResult.isReservedName)
        assertTrue(reservedResult.sanitizedName.startsWith("safe_"))

        // Formatted token templating
        val config = OutputFileConfiguration(
            namingMode = NamingMode.CUSTOM_PATTERN,
            customTemplate = "{name}_{w}x{h}_{dpi}dpi"
        )
        val formatted = OutputFileManager.generateFormattedFilename(
            config = config,
            originalName = "vacation",
            width = 1920,
            height = 1080,
            dpi = 300,
            format = ExportFormat.JPEG
        )
        assertEquals("vacation_1920x1080_300dpi.jpg", formatted)
    }
}
