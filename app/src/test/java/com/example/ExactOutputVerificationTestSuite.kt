package com.example

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.MetadataPrivacyConfig
import com.example.model.VerificationStatus
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode
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
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExactOutputVerificationTestSuite {

    @Test
    fun testExactDimensionVerification_PassAndFail() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.PNG,
            quality = 100,
            dpi = 300
        )

        assertTrue(result.success)
        assertNotNull(result.verificationReport)

        val report = result.verificationReport!!
        assertEquals("300 × 100 px", report.dimensionsMetric.requested)
        assertEquals("300 × 100 px", report.dimensionsMetric.actual)
        assertEquals(VerificationStatus.PASS, report.dimensionsMetric.status)

        // Test mismatching check
        val mismatchedReport = OutputVerificationEngine.verifyExportOutput(
            outputFile = result.outputFile,
            requestedWidth = 400,
            requestedHeight = 200,
            requestedFormat = ExportFormat.PNG,
            requestedDpi = 300
        )
        assertEquals(VerificationStatus.FAIL, mismatchedReport.dimensionsMetric.status)
    }

    @Test
    fun testFileSizeVerification_WithinLimitAndExceeded() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        // Target: 200 KB
        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 85,
            targetSizeKb = 200,
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING
        )

        assertTrue(result.success)
        val report = result.verificationReport!!
        assertEquals("≤ 200 KB", report.fileSizeMetric.requested)
        assertEquals(VerificationStatus.WITHIN_LIMIT, report.fileSizeMetric.status)

        // Test when actual file exceeds target ceiling
        val smallCeilingReport = OutputVerificationEngine.verifyExportOutput(
            outputFile = result.outputFile,
            requestedWidth = 200,
            requestedHeight = 200,
            requestedFormat = ExportFormat.JPEG,
            requestedDpi = 300,
            targetSizeKb = 1, // 1 KB will be exceeded
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING
        )
        assertEquals(VerificationStatus.EXCEEDED, smallCeilingReport.fileSizeMetric.status)
    }

    @Test
    fun testDpiVerification_NoFalsePassWhenUnverified() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val testFile = File(context.cacheDir, "unsupported_dpi.bin")
        testFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))

        val report = OutputVerificationEngine.verifyExportOutput(
            outputFile = testFile,
            requestedWidth = 100,
            requestedHeight = 100,
            requestedFormat = ExportFormat.WEBP_LOSSY,
            requestedDpi = 300
        )

        // Crucial policy check: Never falsely report PASS if DPI metadata is unverified or unsupported
        assertFalse(report.dpiMetric.status == VerificationStatus.PASS)
        assertEquals(VerificationStatus.UNVERIFIED, report.dpiMetric.status)
    }

    @Test
    fun testVerificationReportFormattedOutput() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.PNG,
            quality = 100,
            targetSizeKb = 200,
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING,
            dpi = 300,
            privacyConfig = MetadataPrivacyConfig(policy = MetadataPolicy.STRIP_ALL)
        )

        val report = result.verificationReport!!
        val summary = report.toFormattedSummary()

        assertTrue(summary.contains("DIMENSIONS"))
        assertTrue(summary.contains("300 × 100 px"))
        assertTrue(summary.contains("PASS"))
        assertTrue(summary.contains("FILE SIZE"))
        assertTrue(summary.contains("WITHIN LIMIT") || summary.contains("PASS"))
    }
}
