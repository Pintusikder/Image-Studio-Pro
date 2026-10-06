package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.model.DetectedImageFormat
import com.example.model.ImageExifData
import com.example.processing.BitmapUtils
import com.example.processing.ExifManager
import com.example.processing.ImageInfoAnalyzer
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
class Phase3ImportAndInfoTestSuite {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // =========================================================================
    // 1. IMAGE DECODER & SUBSAMPLING (Bounds checking & Memory Safety)
    // =========================================================================
    @Test
    fun testImageDecoder_DecodesLargeImageWithSubsampling() {
        val testFile = File(context.cacheDir, "decoder_test_large.jpg")
        val bmp = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        val uri = Uri.fromFile(testFile)

        // Decode with 500x500 max bounds (subsampled)
        val decoded = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 500, 500)
        assertNotNull("Sampled bitmap must decode successfully", decoded)
        assertTrue("Subsampled width must be <= 2000", decoded!!.width <= 2000)
        assertTrue("Subsampled height must be <= 1500", decoded.height <= 1500)
        assertTrue("Decoded dimension should be efficiently scaled", decoded.width in 250..1000)

        testFile.delete()
    }

    @Test
    fun testImageDecoder_HandlesInvalidStreamGracefully() {
        val dummyFile = File(context.cacheDir, "invalid_stream.bin")
        dummyFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        val uri = Uri.fromFile(dummyFile)

        val decoded = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1024, 1024)
        assertNull("Invalid image data must return null instead of throwing unhandled exception", decoded)

        dummyFile.delete()
    }

    // =========================================================================
    // 2. DIMENSION & HEADER DETECTION (Fast bounds without allocating full pixels)
    // =========================================================================
    @Test
    fun testDimensionDetection_DetectsDimensionsWithoutFullAllocation() {
        val testFile = File(context.cacheDir, "dim_detect.png")
        val bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val uri = Uri.fromFile(testFile)

        val header = BitmapUtils.getImageHeader(context, uri)
        assertEquals("Width must match generated image", 1280, header.width)
        assertEquals("Height must match generated image", 720, header.height)
        assertTrue("Detected format must be PNG", header.detectedFormat == DetectedImageFormat.PNG)

        testFile.delete()
    }

    // =========================================================================
    // 3. FILE-SIZE DETECTION & FORMAT RECOGNITION
    // =========================================================================
    @Test
    fun testFileSizeDetection_ComputesAccurateFileSizeBytes() {
        val testFile = File(context.cacheDir, "size_detect.jpg")
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        }
        val uri = Uri.fromFile(testFile)

        val header = BitmapUtils.getImageHeader(context, uri)
        val expectedSize = testFile.length()
        assertEquals("Detected file size must match file length", expectedSize, header.fileSizeBytes)
        assertTrue("Detected file size must be greater than 0", header.fileSizeBytes > 0)

        val format = BitmapUtils.detectFormat(context, uri)
        assertEquals(DetectedImageFormat.JPEG, format)

        testFile.delete()
    }

    // =========================================================================
    // 4. EXIF DETECTION & METADATA PARSING
    // =========================================================================
    @Test
    fun testExifDetection_ReadsExifMetadata() {
        val testFile = File(context.cacheDir, "exif_test.jpg")
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        testFile.outputStream().use { stream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }

        // Write sample EXIF tags to the test file
        val exifInterface = ExifInterface(testFile.absolutePath)
        exifInterface.setAttribute(ExifInterface.TAG_MAKE, "Google")
        exifInterface.setAttribute(ExifInterface.TAG_MODEL, "Pixel 8 Pro")
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        exifInterface.saveAttributes()

        val uri = Uri.fromFile(testFile)
        val exifData = ExifManager.readExif(context, uri)

        assertNotNull(exifData)
        assertEquals("Google", exifData.make)
        assertEquals("Pixel 8 Pro", exifData.model)
        assertTrue("Has camera info must be true", exifData.hasCameraInfo)

        testFile.delete()
    }

    // =========================================================================
    // 5. IMAGE INFORMATION ANALYZER (Comprehensive metrics audit)
    // =========================================================================
    @Test
    fun testImageInfoAnalyzer_AnalyzesCompleteImageProfile() {
        val bmp = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val dummyExif = ImageExifData(
            make = "Sony",
            model = "Alpha 7",
            shutterSpeed = "1/250",
            aperture = "2.8",
            iso = "200",
            focalLength = "50"
        )

        val details = ImageInfoAnalyzer.analyze(
            context = context,
            uri = null,
            bitmap = bmp,
            originalWidth = 1920,
            originalHeight = 1080,
            originalFileSize = 512_000L,
            originalMime = "image/jpeg",
            originalDpi = 300,
            exif = dummyExif,
            currentDpi = 300,
            detectedFormat = DetectedImageFormat.JPEG
        )

        assertEquals(1920, details.width)
        assertEquals(1080, details.height)
        assertEquals("Landscape", details.orientationCategory)
        assertEquals("16:9", details.aspectRatioRatioString)
        assertTrue("Standard name should contain 16:9", details.aspectRatioStandardName?.contains("16:9") == true)
        assertEquals(300, details.dpi)
        assertTrue(details.megapixels > 2.0f)
        assertEquals(512_000L, details.fileSizeBytes)
        assertEquals("Sony", details.cameraMake)
        assertEquals("Alpha 7", details.cameraModel)
        assertNotNull(details.exposureSummary)
        assertTrue(details.exposureSummary!!.contains("ISO 200"))
    }
}
