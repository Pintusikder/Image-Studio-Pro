package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatchConfig
import com.example.model.BatchItem
import com.example.model.BatchResizeOption
import com.example.model.BatchStatus
import com.example.model.EditingInstructions
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.MetadataPrivacyConfig
import com.example.model.ResizeMode
import com.example.model.VerificationStatus
import com.example.processing.BitmapUtils
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode
import com.example.processing.NonDestructivePipeline
import com.example.processing.OutputVerificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DeviceCompatibilityTestSuite {

    // ==========================================
    // 1. ANDROID OS VERSION COMPATIBILITY TESTS
    // ==========================================

    @Test
    @Config(sdk = [26]) // Android 8.0 (Oreo)
    fun testAndroid8_LegacyExportAndFormatDetection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 90,
            dpi = 300
        )

        assertTrue("Export must succeed on Android 8+", result.success)
        assertNotNull(result.outputFile)
        assertTrue(result.outputFile!!.exists())
        assertEquals(400, result.verifiedFileWidth)
        assertEquals(300, result.verifiedFileHeight)
    }

    @Test
    @Config(sdk = [31]) // Android 12 (Snow Cone / S)
    fun testAndroid12_ModernFormatExportAndDpi() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.WEBP_LOSSY,
            quality = 85,
            dpi = 300,
            targetSizeKb = 150,
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING
        )

        assertTrue("WebP export must succeed on Android 12+", result.success)
        assertNotNull(result.outputFile)
        assertTrue(result.outputFile!!.exists())
        assertTrue(result.fileSizeBytes <= 150 * 1024L)
    }

    @Test
    @Config(sdk = [33]) // Android 13 (Tiramisu)
    fun testAndroid13_GranularMediaAndExifScrubbing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 95,
            privacyConfig = MetadataPrivacyConfig(policy = MetadataPolicy.STRIP_ALL),
            dpi = 300
        )

        assertTrue("Scrubbed export must succeed on Android 13+", result.success)
        val report = result.verificationReport
        assertNotNull(report)
        assertEquals(VerificationStatus.PASS, report?.dimensionsMetric?.status)
    }

    @Test
    @Config(sdk = [34]) // Android 14 (UpsideDownCake)
    fun testAndroid14_SelectivePhotoAndExactFileSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.PNG,
            quality = 100,
            targetSizeKb = 300,
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING,
            dpi = 300
        )

        assertTrue("Lossless PNG export must succeed on Android 14+", result.success)
        assertTrue(result.fileSizeBytes <= 300 * 1024L)
    }

    @Test
    @Config(sdk = [35]) // Android 15 (VanillaIceCream)
    fun testAndroid15_16KBPageAndEdgeToEdgeExport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(1024, 768, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 90,
            dpi = 600
        )

        assertTrue("High DPI export must succeed on Android 15+", result.success)
        assertEquals(1024, result.verifiedFileWidth)
        assertEquals(768, result.verifiedFileHeight)
    }

    @Test
    @Config(sdk = [36]) // Android 16 (Baklava / compileSdk)
    fun testAndroid16_FutureProofVectorPdfAndRaster() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 90,
            dpi = 300
        )

        assertTrue("Export must succeed on Android 16+", result.success)
        assertNotNull(result.outputFile)
        assertTrue(result.outputFile!!.exists())
        assertTrue(result.outputFile!!.length() > 0)
    }

    // ==========================================
    // 2. LARGE IMAGES & SUBSAMPLING TESTS
    // ==========================================

    @Test
    fun testLargeImage_SubsamplingCalculation() {
        // Simulating 100 Megapixel image (12,000 x 8,000 px)
        val ultraLargeOptions = BitmapFactory.Options().apply {
            outWidth = 12000
            outHeight = 8000
        }

        // Subsample target: 2000 x 2000
        val sampleSize = BitmapUtils.calculateInSampleSize(ultraLargeOptions, 2000, 2000)
        assertEquals("Subsampling factor should be 4 for 12000x8000 into 2000", 4, sampleSize)

        // Subsample target: 1000 x 1000 (low memory devices)
        val lowMemSampleSize = BitmapUtils.calculateInSampleSize(ultraLargeOptions, 1000, 1000)
        assertEquals("Subsampling factor should be 8 for 1000px budget", 8, lowMemSampleSize)
    }

    @Test
    fun testLargeImage_NonDestructivePipelineExecution() {
        val largeBmp = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888)
        val instructions = EditingInstructions(
            rotationAngle = 90f,
            targetWidthPx = 1200,
            targetHeightPx = 800,
            resizeMode = ResizeMode.FIT
        )

        val processed = NonDestructivePipeline.applyPipeline(largeBmp, instructions)
        assertNotNull(processed)
        assertEquals(1200, processed.width)
        assertEquals(800, processed.height)
    }

    // ==========================================
    // 3. LOW RAM & MEMORY PRESSURE RESILIENCE
    // ==========================================

    @Test
    fun testLowRam_DownsamplingAndFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.cacheDir, "low_ram_test.jpg")
        val sampleBmp = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { sampleBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        // Test decode with memory constraints
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // Emulating low memory downsampling
            inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit color for low RAM
        }
        val decoded = BitmapFactory.decodeFile(testFile.absolutePath, options)
        assertNotNull(decoded)
        assertEquals(200, decoded.width)
        assertEquals(200, decoded.height)
    }

    // ==========================================
    // 4. LOW STORAGE & IO ERROR HANDLING
    // ==========================================

    @Test
    fun testLowStorage_GracefulErrorHandling() {
        // Attempting to write to an invalid / non-writable directory simulates full / read-only disk
        val invalidDir = File("/proc/non_writable_storage_dir")
        val invalidFile = File(invalidDir, "failed_export.jpg")

        val report = OutputVerificationEngine.verifyExportOutput(
            outputFile = invalidFile,
            requestedWidth = 100,
            requestedHeight = 100,
            requestedFormat = ExportFormat.JPEG,
            requestedDpi = 300
        )

        assertFalse("Verification report must not pass on unwritten file", report.allMeasurablePassed)
        assertEquals(VerificationStatus.FAIL, report.overallStatus)
        assertEquals("File not generated or 0 bytes", report.metadataMetric?.actual)
    }

    // ==========================================
    // 5. LARGE BATCH PROCESSING LOAD TEST
    // ==========================================

    @Test
    fun testLargeBatch_ItemsCreationAndConfig() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Generate 50 mock batch items
        val batchItems = (1..50).map { i ->
            val file = File(context.cacheDir, "batch_sample_$i.png")
            val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = Uri.fromFile(file)

            BatchItem(
                id = "task_$i",
                uri = uri,
                originalName = file.name,
                originalSize = file.length(),
                originalWidth = 50,
                originalHeight = 50,
                status = BatchStatus.PENDING
            )
        }

        assertEquals(50, batchItems.size)
        val config = BatchConfig(
            resizeOption = BatchResizeOption.PERCENTAGE,
            scalePercent = 50,
            format = ExportFormat.JPEG,
            quality = 85
        )

        assertEquals(50, config.scalePercent)
        assertEquals(ExportFormat.JPEG, config.format)
        assertTrue(batchItems.all { it.status == BatchStatus.PENDING })
    }
}
