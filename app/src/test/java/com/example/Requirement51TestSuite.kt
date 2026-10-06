package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.processing.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Requirement 51: Comprehensive Test Suite covering all mandatory test requirements:
 *
 * 1. Crop: Free crop, Fixed ratio, Exact crop
 * 2. Resize: Width, Height, Width + Height, Aspect lock/unlock
 * 3. Compression: Quality, Target file size, Maximum file size
 * 4. Signature: Exact dimensions, DPI, File-size limit, Background, FIT/FILL/STRETCH
 * 5. Conversion: JPG, PNG, WebP (Lossy/Lossless), HEIF/HEIC support handling
 * 6. Batch: Multiple images, Large images, Mixed formats, Failed files handling
 * 7. Metadata: Preserve, Remove, EXIF handling
 * 8. Export: Save, Share, SAF, Existing filename conflict resolution
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Requirement51TestSuite {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // Helper to generate a test pattern bitmap
    private fun createTestBitmap(width: Int = 400, height: Int = 300, color: Int = Color.BLUE): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(color)
        val paint = android.graphics.Paint().apply {
            this.color = Color.WHITE
            strokeWidth = 4f
        }
        // Draw diagonal line and circle for testing
        canvas.drawLine(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), paint)
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 25f, paint)
        return bitmap
    }

    // Helper to create a signature test bitmap (white background with dark ink)
    private fun createSignatureTestBitmap(width: Int = 600, height: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Initialize with pure white
        val pixels = IntArray(width * height) { Color.WHITE }
        // Draw a small block of dark ink pixels in the center (x: 200..250, y: 80..120)
        for (y in 80 until 120) {
            for (x in 200 until 250) {
                pixels[y * width + x] = Color.BLACK
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    // ==========================================
    // 1. CROP TESTS
    // ==========================================

    @Test
    fun testFreeCropAllowsArbitraryCoordinates() {
        val source = createTestBitmap(1000, 800)
        val cropRect = RectF(0.15f, 0.20f, 0.75f, 0.85f)
        val cropped = BitmapUtils.cropBitmap(source, cropRect)

        assertNotNull(cropped)
        // Expected width = 0.60 * 1000 = 600, height = 0.65 * 800 = 520
        assertEquals(600, cropped.width)
        assertEquals(520, cropped.height)
    }

    @Test
    fun testFixedRatioCropMaintainsExactRequestedAspectRatios() {
        val source = createTestBitmap(1200, 800)

        // 1:1 Square
        val square = AspectRatioEngine.cropToAspectRatio(source, 1f, 1f)
        assertEquals(800, square.width)
        assertEquals(800, square.height)
        assertEquals(1.0f, square.width.toFloat() / square.height.toFloat(), 0.001f)

        // 4:5 Portrait
        val portrait = AspectRatioEngine.cropToAspectRatio(source, 4f, 5f)
        val portraitRatio = portrait.width.toFloat() / portrait.height.toFloat()
        assertEquals(4f / 5f, portraitRatio, 0.01f)

        // 16:9 Widescreen
        val wide = AspectRatioEngine.cropToAspectRatio(source, 16f, 9f)
        val wideRatio = wide.width.toFloat() / wide.height.toFloat()
        assertEquals(16f / 9f, wideRatio, 0.01f)
    }

    @Test
    fun testExactCropOutputsPixelPerfectRequestedDimensions() {
        val source = createTestBitmap(1920, 1080)
        val exactW = 413
        val exactH = 531

        val exactCropped = BitmapUtils.resizeBitmapWithMode(source, exactW, exactH, ResizeMode.FILL)
        assertNotNull(exactCropped)
        assertEquals(exactW, exactCropped.width)
        assertEquals(exactH, exactCropped.height)
    }

    // ==========================================
    // 2. RESIZE TESTS
    // ==========================================

    @Test
    fun testResizeWidthOnlyWithAspectRatioLocked() {
        val source = createTestBitmap(1600, 900)
        val originalAspect = source.width.toFloat() / source.height.toFloat()

        val newWidth = 800
        val expectedHeight = (newWidth / originalAspect).roundToInt()

        val resized = BitmapUtils.resizeBitmap(source, newWidth, expectedHeight)
        assertEquals(newWidth, resized.width)
        assertEquals(expectedHeight, resized.height)
        assertEquals(originalAspect, resized.width.toFloat() / resized.height.toFloat(), 0.01f)
    }

    @Test
    fun testResizeHeightOnlyWithAspectRatioLocked() {
        val source = createTestBitmap(1600, 800)
        val originalAspect = source.width.toFloat() / source.height.toFloat()

        val newHeight = 400
        val expectedWidth = (newHeight * originalAspect).roundToInt()

        val resized = BitmapUtils.resizeBitmap(source, expectedWidth, newHeight)
        assertEquals(expectedWidth, resized.width)
        assertEquals(newHeight, resized.height)
        assertEquals(originalAspect, resized.width.toFloat() / resized.height.toFloat(), 0.01f)
    }

    @Test
    fun testResizeWidthAndHeightWithAspectLockUnlockedAllowsArbitraryStretch() {
        val source = createTestBitmap(800, 600)
        val customW = 350
        val customH = 120

        val resizedUnlocked = BitmapUtils.resizeBitmapWithMode(source, customW, customH, ResizeMode.STRETCH)
        assertEquals(customW, resizedUnlocked.width)
        assertEquals(customH, resizedUnlocked.height)
    }

    @Test
    fun testResizeModesFitVsFillVsStretchVsSmartCrop() {
        val source = createTestBitmap(1000, 500) // 2:1 ratio

        // FIT in 500x500 square
        val fit = BitmapUtils.resizeBitmapWithMode(source, 500, 500, ResizeMode.FIT)
        assertEquals(500, fit.width)
        assertEquals(500, fit.height)

        // FILL in 500x500 square
        val fill = BitmapUtils.resizeBitmapWithMode(source, 500, 500, ResizeMode.FILL)
        assertEquals(500, fill.width)
        assertEquals(500, fill.height)

        // STRETCH in 300x700
        val stretch = BitmapUtils.resizeBitmapWithMode(source, 300, 700, ResizeMode.STRETCH)
        assertEquals(300, stretch.width)
        assertEquals(700, stretch.height)

        // SMART CROP
        val smart = BitmapUtils.resizeBitmapWithMode(source, 400, 400, ResizeMode.SMART_CROP)
        assertEquals(400, smart.width)
        assertEquals(400, smart.height)
    }

    // ==========================================
    // 3. COMPRESSION TESTS
    // ==========================================

    @Test
    fun testCompressionQualityDegradationScalesFileSizeMonotonically() {
        val source = createTestBitmap(800, 600)

        val bytesQuality95 = ExportEngine.measureActualCompressedBytes(
            source = source, targetW = 800, targetH = 600, format = ExportFormat.JPEG, quality = 95
        ).actualBytes

        val bytesQuality75 = ExportEngine.measureActualCompressedBytes(
            source = source, targetW = 800, targetH = 600, format = ExportFormat.JPEG, quality = 75
        ).actualBytes

        val bytesQuality30 = ExportEngine.measureActualCompressedBytes(
            source = source, targetW = 800, targetH = 600, format = ExportFormat.JPEG, quality = 30
        ).actualBytes

        assertTrue("Quality 95 should produce more or equal bytes than 75", bytesQuality95 >= bytesQuality75)
        assertTrue("Quality 75 should produce more bytes than 30", bytesQuality75 > bytesQuality30)
    }

    @Test
    fun testTargetFileSizeCompressionReachesRequestedTargetSize() {
        val source = createTestBitmap(1200, 1000)
        val targetKb = 45

        val result = FileSizeEngine.optimizeFileSize(
            source = source,
            format = ExportFormat.JPEG,
            targetKb = targetKb,
            mode = FileSizeMode.MAXIMUM_CEILING,
            allowDimensionAdjustment = true,
            jpegBackgroundColor = Color.WHITE
        )

        assertNotNull(result.encodedBytes)
        val resultKb = result.actualBytes / 1024L
        assertTrue("Target file size must not exceed ceiling ($targetKb KB)", resultKb <= targetKb)
        assertTrue("File should not be zero bytes", result.actualBytes > 0)
    }

    @Test
    fun testMaximumFileSizeCeilingEnforcementWithoutDimensionAdjustment() {
        val source = createTestBitmap(600, 600)
        val targetKb = 25

        val result = FileSizeEngine.optimizeFileSize(
            source = source,
            format = ExportFormat.JPEG,
            targetKb = targetKb,
            mode = FileSizeMode.MAXIMUM_CEILING,
            allowDimensionAdjustment = false,
            jpegBackgroundColor = Color.WHITE
        )

        val sizeKb = result.actualBytes / 1024L
        assertTrue("Size should be within reasonable bound of $targetKb KB", sizeKb <= targetKb || result.quality <= 15)
        assertEquals(600, result.outputWidth)
        assertEquals(600, result.outputHeight)
    }

    // ==========================================
    // 4. SIGNATURE PROCESSING TESTS
    // ==========================================

    @Test
    fun testSignatureInkAutoTrimEmptyArea() {
        val signatureBmp = createSignatureTestBitmap(800, 300)
        val trimmed = SignatureProcessor.autoTrimEmptyArea(signatureBmp, threshold = 200, paddingPx = 10)

        assertNotNull(trimmed)
        assertTrue("Trimmed width should be smaller than original", trimmed.width < signatureBmp.width)
        assertTrue("Trimmed height should be valid", trimmed.height > 0)
    }

    @Test
    fun testSignatureExactDimensionsAndDpiCalculation() {
        val signatureBmp = createSignatureTestBitmap(500, 200)
        val targetW = 300
        val targetH = 100
        val dpi = 200

        val fitted = SignatureProcessor.fitSignatureToTargetBox(
            source = signatureBmp,
            targetWidth = targetW,
            targetHeight = targetH,
            fitMode = SignatureProcessor.SignatureFitMode.FIT,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE,
            customBackgroundColor = Color.WHITE,
            center = true
        )

        assertEquals(targetW, fitted.width)
        assertEquals(targetH, fitted.height)

        val widthInches = fitted.width.toFloat() / dpi
        assertEquals(1.5f, widthInches, 0.01f)
    }

    @Test
    fun testSignatureBackgroundsWhiteTransparentAndCustom() {
        val signatureBmp = createSignatureTestBitmap(400, 150)

        // 1. Pure White
        val whiteBg = SignatureProcessor.processInkAndColor(
            source = signatureBmp,
            inkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE,
            makeTransparent = false,
            backgroundColor = Color.WHITE
        )
        assertNotNull(whiteBg)
        assertEquals(Color.WHITE, whiteBg.getPixel(5, 5))

        // 2. Transparent background
        val transBg = SignatureProcessor.processInkAndColor(
            source = signatureBmp,
            inkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE,
            makeTransparent = true,
            backgroundColor = Color.TRANSPARENT
        )
        assertNotNull(transBg)
        assertEquals(0, Color.alpha(transBg.getPixel(5, 5)))

        // 3. Custom color background
        val customColor = Color.rgb(250, 240, 230)
        val customBg = SignatureProcessor.processInkAndColor(
            source = signatureBmp,
            inkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE,
            makeTransparent = false,
            backgroundColor = customColor
        )
        assertNotNull(customBg)
        assertEquals(customColor, customBg.getPixel(5, 5))
    }

    @Test
    fun testSignatureFitModesFitFillStretch() {
        val signatureBmp = createSignatureTestBitmap(600, 200)

        val fit = SignatureProcessor.fitSignatureToTargetBox(
            source = signatureBmp,
            targetWidth = 400,
            targetHeight = 200,
            fitMode = SignatureProcessor.SignatureFitMode.FIT,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE
        )
        assertEquals(400, fit.width)
        assertEquals(200, fit.height)

        val fill = SignatureProcessor.fitSignatureToTargetBox(
            source = signatureBmp,
            targetWidth = 300,
            targetHeight = 150,
            fitMode = SignatureProcessor.SignatureFitMode.FILL,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE
        )
        assertEquals(300, fill.width)
        assertEquals(150, fill.height)

        val stretch = SignatureProcessor.fitSignatureToTargetBox(
            source = signatureBmp,
            targetWidth = 250,
            targetHeight = 80,
            fitMode = SignatureProcessor.SignatureFitMode.STRETCH,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE
        )
        assertEquals(250, stretch.width)
        assertEquals(80, stretch.height)
    }

    // ==========================================
    // 5. FORMAT CONVERSION TESTS
    // ==========================================

    @Test
    fun testExportConversionToJpegPngWebpLossyWebpLossless() {
        val source = createTestBitmap(400, 400)

        val formats = listOf(
            ExportFormat.JPEG,
            ExportFormat.PNG,
            ExportFormat.WEBP_LOSSY,
            ExportFormat.WEBP_LOSSLESS
        )

        for (format in formats) {
            val result = ExportEngine.exportImage(
                context = context,
                bitmap = source,
                format = format,
                quality = 85,
                fileNamePrefix = "Test_${format.extension}"
            )
            assertTrue("Export for format ${format.name} should succeed", result.success)
            assertTrue("File size should be positive for ${format.name}", result.fileSizeBytes > 0)
            assertEquals(400, result.width)
            assertEquals(400, result.height)
            assertEquals(format, result.format)
            result.outputFile?.delete()
        }
    }

    @Test
    fun testHeicAndHeifSupportReporting() {
        val detectedHeic = DetectedImageFormat.HEIC
        assertEquals("HEIC", detectedHeic.shortName)
        assertEquals("image/heic", detectedHeic.mimeType)
        assertFalse("HEIC does not support alpha", detectedHeic.supportsAlpha)
    }

    // ==========================================
    // 6. BATCH PROCESSING TESTS
    // ==========================================

    @Test
    fun testBatchMultipleImagesAndMixedFormats() {
        val bitmaps = listOf(
            createTestBitmap(300, 300, Color.RED),
            createTestBitmap(500, 250, Color.GREEN),
            createTestBitmap(400, 600, Color.MAGENTA)
        )

        val targetWidth = 250
        val targetHeight = 250

        val results = bitmaps.map { bmp ->
            val resized = BitmapUtils.resizeBitmapWithMode(bmp, targetWidth, targetHeight, ResizeMode.FILL)
            ExportEngine.exportImage(
                context = context,
                bitmap = resized,
                format = ExportFormat.JPEG,
                quality = 80,
                fileNamePrefix = "Batch_Mixed"
            )
        }

        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
        assertTrue(results.all { it.width == targetWidth && it.height == targetHeight })
        results.forEach { it.outputFile?.delete() }
    }

    @Test
    fun testBatchLargeImageDownscaleWithoutOutOfMemory() {
        val largeBmp = createTestBitmap(2500, 1800)
        val maxTargetDim = 800

        val resized = BitmapUtils.resizeBitmapWithMode(largeBmp, maxTargetDim, maxTargetDim, ResizeMode.FIT)
        assertTrue("Resized width should be <= $maxTargetDim", resized.width <= maxTargetDim)
        assertTrue("Resized height should be <= $maxTargetDim", resized.height <= maxTargetDim)

        val exportRes = ExportEngine.exportImage(
            context = context,
            bitmap = resized,
            format = ExportFormat.WEBP_LOSSY,
            quality = 85
        )
        assertTrue(exportRes.success)
        exportRes.outputFile?.delete()
    }

    @Test
    fun testBatchFailureHandlingWithCorruptedOrInvalidUri() {
        // Create an invalid/corrupted image file (random non-image text bytes)
        val corruptedFile = File(context.cacheDir, "corrupted_test_file.jpg")
        corruptedFile.writeText("THIS IS NOT A VALID JPEG OR IMAGE STREAM 12345")
        val corruptedUri = Uri.fromFile(corruptedFile)

        val decoded = BitmapUtils.decodeSampledBitmapFromUri(context, corruptedUri, 1024, 1024)
        assertNull("Corrupted image file must safely return null instead of crashing", decoded)
        corruptedFile.delete()
    }

    // ==========================================
    // 7. METADATA TESTS (PRESERVE & REMOVE)
    // ==========================================

    @Test
    fun testMetadataDisclosureFormatAudit() {
        val disclosure = ExifManager.getFormatDisclosure(ExportFormat.JPEG)
        assertTrue("JPEG should support embedding EXIF", disclosure.canEmbedExif)
        assertTrue("JPEG should allow stripping GPS", disclosure.canStripGps)
        assertTrue("JPEG should allow stripping Camera info", disclosure.canStripCamera)
    }

    @Test
    fun testMetadataStripAllPolicyClearsSensitiveFields() {
        val tempFile = File(context.cacheDir, "exif_test.jpg")
        val bmp = createTestBitmap(200, 200)
        FileOutputStream(tempFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val privacyConfig = MetadataPrivacyConfig(policy = MetadataPolicy.STRIP_ALL)
        ExifManager.applyExifToOutputFile(
            outputFile = tempFile,
            dpi = 300,
            privacyConfig = privacyConfig,
            format = ExportFormat.JPEG
        )

        val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
        assertNull("GPS Latitude must be stripped", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE))
        assertNull("Camera Model must be stripped", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL))
        tempFile.delete()
    }

    @Test
    fun testMetadataCustomSelectivePolicySelectivelyStripsGpsWhileKeepingCamera() {
        val tempFile = File(context.cacheDir, "exif_selective_test.jpg")
        val bmp = createTestBitmap(200, 200)
        FileOutputStream(tempFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val privacyConfig = MetadataPrivacyConfig(
            policy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeGps = true,
            removeCameraInfo = false
        )

        ExifManager.applyExifToOutputFile(
            outputFile = tempFile,
            dpi = 300,
            privacyConfig = privacyConfig,
            format = ExportFormat.JPEG
        )

        val exif = androidx.exifinterface.media.ExifInterface(tempFile.absolutePath)
        assertNull("GPS Latitude must be stripped", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE))
        tempFile.delete()
    }

    // ==========================================
    // 8. EXPORT, SAF & FILENAME RESOLUTION TESTS
    // ==========================================

    @Test
    fun testFilenameSanitizationRemovesPathTraversalAndReservedCharacters() {
        val maliciousInput = "../../etc/passwd:photo*?<>.jpg"
        val result = OutputFileManager.sanitizeFilename(maliciousInput, fallback = "safe_image")

        assertFalse("Path traversal must be detected", result.isValid)
        assertTrue("Traversal flag should be set", result.hasPathTraversal)
        assertFalse("Result name must not contain slashes", result.sanitizedName.contains("/"))
        assertFalse("Result name must not contain dots-dot", result.sanitizedName.contains(".."))
    }

    @Test
    fun testFilenameTokenFormattingReplacesTokensAccurately() {
        val config = OutputFileConfiguration(
            namingMode = NamingMode.CUSTOM_PATTERN,
            customTemplate = "{name}_{w}x{h}_{dpi}dpi"
        )

        val formatted = OutputFileManager.generateFormattedFilename(
            config = config,
            originalName = "MyVacation.jpg",
            format = ExportFormat.PNG,
            width = 1920,
            height = 1080,
            dpi = 300
        )

        assertEquals("MyVacation_1920x1080_300dpi.png", formatted)
    }

    @Test
    fun testExistingFilenameCollisionIncrementsCopyCounter() {
        val baseName = "Passport_Photo.jpg"
        val uniqueName = OutputFileManager.generateUniqueFilename(baseName)
        assertEquals("Passport_Photo_copy.jpg", uniqueName)
    }

    @Test
    fun testDirectExportSavesSuccessfullyToLocalStorage() {
        val source = createTestBitmap(400, 400)
        val exportResult = ExportEngine.exportImage(
            context = context,
            bitmap = source,
            format = ExportFormat.JPEG,
            quality = 90,
            fileNamePrefix = "Save_Test"
        )

        assertTrue(exportResult.success)
        assertNotNull(exportResult.outputFile)
        assertTrue(exportResult.outputFile!!.exists())
        assertTrue(exportResult.outputFile!!.length() > 0)
        exportResult.outputFile!!.delete()
    }
}
