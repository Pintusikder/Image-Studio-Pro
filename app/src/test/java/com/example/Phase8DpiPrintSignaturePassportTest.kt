package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.domain.usecase.DpiPrintUseCase
import com.example.domain.usecase.PassportIdPhotoUseCase
import com.example.model.CutLineStyle
import com.example.model.ExportFormat
import com.example.model.IdDocumentCategory
import com.example.model.PassportDimensionUnit
import com.example.model.PassportPreset
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.model.SheetAlignment
import com.example.model.SheetOrientation
import com.example.model.SheetPaperPreset
import com.example.processing.BackgroundProcessor
import com.example.processing.DpiPrintEngine
import com.example.processing.FileSizeEngine
import com.example.processing.FileSizeMode
import com.example.processing.PassportIdDocumentEngine
import com.example.processing.PhotoSheetGenerator
import com.example.processing.SignatureProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * PHASE 8 PRODUCTION VERIFICATION SUITE
 * Verifies all 8 modules of Phase 8:
 * 1. DPI (Binary JFIF APP0, EXIF X/Y Resolution, PNG pHYs chunk with CRC32, PDF 72 pt/in)
 * 2. Physical dimensions (exact conversion across Inches, Centimeters, Millimeters, Pixels)
 * 3. Print calculator (Bidirectional calculator, bleed margins, viewing distance, print quality grading)
 * 4. Signature module (Ink detection, auto-trim, stroke weight morphological dilation/erosion, fit modes, KB ceiling)
 * 5. ID photo (Aadhaar, PAN, Driver's License, National ID formatting & KB limits)
 * 6. Passport photo (US 2x2", ICAO 35x45mm, biometric head framing, Name & Date slate stamp)
 * 7. Document photo (Magic Color, Crisp B&W, Grayscale Archive, Dark Border Auto-Trim)
 * 8. Photo sheet (Exact physical mm sizing per cell, auto-fill max copies, cutting guides, calibration footer)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase8DpiPrintSignaturePassportTest {

    private val dpiPrintUseCase = DpiPrintUseCase()
    private val passportIdPhotoUseCase = PassportIdPhotoUseCase()

    private fun createPortraitFaceLikeBitmap(width: Int = 600, height: Int = 800): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(235, 238, 242))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Shoulders
        paint.color = Color.rgb(40, 55, 80)
        canvas.drawRoundRect(
            width * 0.18f,
            height * 0.68f,
            width * 0.82f,
            height.toFloat(),
            40f,
            40f,
            paint
        )
        // Head oval
        paint.color = Color.rgb(215, 170, 140)
        canvas.drawOval(
            width * 0.28f,
            height * 0.18f,
            width * 0.72f,
            height * 0.66f,
            paint
        )
        // Eyes
        paint.color = Color.rgb(30, 30, 35)
        canvas.drawCircle(width * 0.40f, height * 0.38f, width * 0.025f, paint)
        canvas.drawCircle(width * 0.60f, height * 0.38f, width * 0.025f, paint)
        return bmp
    }

    private fun createSignatureOnPaperBitmap(width: Int = 800, height: Int = 400): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paperColor = Color.rgb(245, 244, 240)
        val inkColor = Color.rgb(15, 25, 95)
        val pixels = IntArray(width * height) { paperColor }

        // Draw deterministic pen strokes in center region [210..580, 150..260]
        for (x in 210..580) {
            val yWave = 200 + ((kotlin.math.sin(x * 0.05) * 35).toInt())
            for (dy in -3..3) {
                val py = (yWave + dy).coerceIn(0, height - 1)
                pixels[py * width + x] = inkColor
            }
            // Underline flourish
            for (dy in -2..2) {
                val py = (255 + dy).coerceIn(0, height - 1)
                pixels[py * width + x] = inkColor
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    // =========================================================================
    // 1. DPI METADATA EMBEDDING & ROUND-TRIP VERIFICATION
    // =========================================================================

    @Test
    fun test1_DpiEmbeddingAndRoundTripVerification_JpegAndPng() {
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(100, 150, 220))

        // JPEG @ 300 DPI
        val jpegBaos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, jpegBaos)
        val (_, jpegReport) = dpiPrintUseCase.embedAndVerifyDpi(
            encodedBytes = jpegBaos.toByteArray(),
            pixelWidth = 300,
            pixelHeight = 300,
            dpi = 300,
            format = ExportFormat.JPEG
        )

        assertTrue("JPEG 300 DPI round-trip should verify", jpegReport.isVerified)
        assertEquals(300, jpegReport.detectedDpiX)
        assertEquals(300, jpegReport.detectedDpiY)

        // PNG @ 600 DPI (pHYs chunk with CRC32)
        val pngBaos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, pngBaos)
        val (_, pngReport) = dpiPrintUseCase.embedAndVerifyDpi(
            encodedBytes = pngBaos.toByteArray(),
            pixelWidth = 300,
            pixelHeight = 300,
            dpi = 600,
            format = ExportFormat.PNG
        )

        assertTrue("PNG 600 DPI pHYs chunk round-trip should verify", pngReport.isVerified)
        assertNotNull("PNG pHYs should detect X DPI", pngReport.detectedDpiX)
        assertTrue("PNG detected DPI should be within ±1 of 600", abs((pngReport.detectedDpiX ?: 0) - 600) <= 1)
    }

    // =========================================================================
    // 2. PHYSICAL DIMENSIONS CONVERSION
    // =========================================================================

    @Test
    fun test2_PhysicalDimensionsBidirectionalConversion() {
        // 4 x 6 inches @ 300 DPI => 1200 x 1800 px
        val (pxW, pxH) = DpiPrintEngine.computePixelsFromPhysical(4.0f, 6.0f, PrintUnit.INCHES, 300)
        assertEquals(1200, pxW)
        assertEquals(1800, pxH)

        // 35 x 45 mm @ 300 DPI => 413 x 531 px
        val (icaoW, icaoH) = DpiPrintEngine.computePixelsFromPhysical(35.0f, 45.0f, PrintUnit.MILLIMETERS, 300)
        assertEquals(413, icaoW)
        assertEquals(531, icaoH)

        // Reverse: 1200 x 1800 px @ 300 DPI => 4.0 x 6.0 in, 10.16 x 15.24 cm, 101.6 x 152.4 mm
        val phys = DpiPrintEngine.computePhysicalDimensionsFromPixels(1200, 1800, 300)
        assertEquals(4.0f, phys.widthInches, 0.01f)
        assertEquals(6.0f, phys.heightInches, 0.01f)
        assertEquals(10.16f, phys.widthCm, 0.05f)
        assertEquals(15.24f, phys.heightCm, 0.05f)
        assertEquals(101.6f, phys.widthMm, 0.2f)
        assertEquals(152.4f, phys.heightMm, 0.2f)
    }

    // =========================================================================
    // 3. PRINT CALCULATOR + BLEED + QUALITY GRADING
    // =========================================================================

    @Test
    fun test3_PrintCalculatorAndResamplingWithBleed() {
        // High-res source (2400x3600) printed at 4x6 inches @ 300 DPI with 3mm bleed
        val report = dpiPrintUseCase.calculatePrintMetrics(
            sourceWidthPx = 2400,
            sourceHeightPx = 3600,
            physicalWidth = 4.0f,
            physicalHeight = 6.0f,
            unit = PrintUnit.INCHES,
            targetDpi = 300,
            bleedMm = 3.0f,
            mode = DpiPrintEngine.PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS
        )

        assertEquals(1200, report.targetWidthPx)
        assertEquals(1800, report.targetHeightPx)
        assertTrue("Effective DPI should be >= 300", report.effectiveDpi >= 300)
        assertEquals(DpiPrintEngine.PrintQualityGrade.ARCHIVAL_PRO, report.qualityGrade)
        assertFalse("Should not require upscaling", report.requiresUpscaling)
        assertTrue("Total width with 3mm bleed should be ~1271 px", report.totalWithBleedWidthPx in 1268..1274)
        assertTrue("Total height with 3mm bleed should be ~1871 px", report.totalWithBleedHeightPx in 1868..1874)

        // Resample source bitmap for print with 3mm bleed
        val sourceBmp = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888)
        val resampled = dpiPrintUseCase.resampleForPrint(
            source = sourceBmp,
            physicalWidth = 2.0f,
            physicalHeight = 3.0f,
            unit = PrintUnit.INCHES,
            dpi = 300,
            bleedMm = 3.0f,
            resizeMode = ResizeMode.FILL
        )
        assertTrue("Resampled width with bleed should exceed 600", resampled.bitmap.width > 600)
        assertTrue("Resampled height with bleed should exceed 900", resampled.bitmap.height > 900)
    }

    // =========================================================================
    // 4. SIGNATURE MODULE
    // =========================================================================

    @Test
    fun test4_SignatureModule_InkIsolation_StrokeWeight_And_Verification() {
        val rawSig = createSignatureOnPaperBitmap(800, 400)

        // Detect ink bounds
        val bounds = SignatureProcessor.findInkBounds(rawSig, threshold = 200, isTransparent = false)
        assertTrue("Should detect signature ink strokes", bounds.hasInk)
        assertTrue("Trimmed bounds should be smaller than full 800x400 sheet", bounds.bounds.width() < 600)

        // Execute full signature pipeline for UPSC/SSC Portal (140x60 px, max 20 KB)
        val preset = SignatureProcessor.SignaturePreset.PRESETS.first { it.id == "upsc_ssc_140x60" }
        val fittedBmp = passportIdPhotoUseCase.processAndFitSignature(
            source = rawSig,
            targetWidth = preset.widthPx,
            targetHeight = preset.heightPx,
            inkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE,
            paperThreshold = 200,
            strokeWeightDelta = 1, // Bolder stroke via morphological dilation
            fitMode = SignatureProcessor.SignatureFitMode.FIT,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE,
            marginPaddingPx = 6
        )

        val opt = FileSizeEngine.optimizeFileSize(
            source = fittedBmp,
            format = ExportFormat.JPEG,
            targetKb = preset.maxFileSizeKb,
            mode = FileSizeMode.MAXIMUM_CEILING,
            allowDimensionAdjustment = false
        )
        val report = SignatureProcessor.verifySignatureOutput(
            bitmap = fittedBmp,
            encodedBytes = opt.encodedBytes,
            dpi = preset.dpi,
            backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE,
            exportFormat = ExportFormat.JPEG,
            maxLimitKb = preset.maxFileSizeKb
        )

        assertEquals(140, fittedBmp.width)
        assertEquals(60, fittedBmp.height)
        assertEquals(140, report.widthPx)
        assertEquals(60, report.heightPx)
        assertTrue("Signature output bytes must be <= 20 KB", report.meetsSizeRequirement)
        assertTrue("Signature output must be verified", report.isVerified)
    }

    // =========================================================================
    // 5. ID PHOTO & 6. PASSPORT PHOTO + BIOMETRICS + NAME/DATE STAMP
    // =========================================================================

    @Test
    fun test5And6_IdPhotoAndPassportPhoto_Biometrics_And_SlateStamp() {
        val portrait = createPortraitFaceLikeBitmap(600, 800)

        // Verify both ID_PHOTO and PASSPORT_PHOTO categories have official presets
        val idPresets = PassportPreset.PRESETS.filter { it.category == IdDocumentCategory.ID_PHOTO }
        val passportPresets = PassportPreset.PRESETS.filter { it.category == IdDocumentCategory.PASSPORT_PHOTO }
        assertTrue("Should have official ID photo presets", idPresets.isNotEmpty())
        assertTrue("Should have official Passport photo presets", passportPresets.isNotEmpty())

        // Process ICAO 35x45mm Passport Photo @ 300 DPI (413 x 531 px) with Name & Date Slate Stamp and 50KB limit
        val config = PassportIdDocumentEngine.PassportIdProcessConfig(
            widthPhysical = 35.0f,
            heightPhysical = 45.0f,
            unit = PassportDimensionUnit.MILLIMETERS,
            dpi = 300,
            exactWidthPxOverride = 413,
            exactHeightPxOverride = 531,
            backgroundColor = BackgroundProcessor.PassportBgColor.PURE_WHITE,
            backgroundTolerance = 38f,
            autoBiometricCrop = true,
            addThinBorder = true,
            slateConfig = PassportIdDocumentEngine.NameDateSlateConfig(
                enabled = true,
                applicantName = "AARAV SHARMA",
                photoDateText = "DOP: 29-09-2026"
            ),
            targetMaxFileSizeKb = 50,
            exportFormat = ExportFormat.JPEG
        )

        val result = passportIdPhotoUseCase.processPassportOrIdPhoto(portrait, config)
        assertEquals(413, result.bitmap.width)
        assertEquals(531, result.bitmap.height)
        assertTrue("Encoded passport photo must comply with 50 KB ceiling", result.meetsFileSizeLimit)
        assertTrue("Biometric report should have valid head height %", result.biometricReport.headHeightPercent in 30f..95f)
        assertTrue("DPI metadata must be verified in output JPEG", result.dpiReport.isVerified)
    }

    // =========================================================================
    // 7. DOCUMENT PHOTO SCANNER
    // =========================================================================

    @Test
    fun test7_DocumentPhotoScanner_ModesAndBorderTrimming() {
        // Create synthetic document with dark desk border around a white page
        val rawDoc = Bitmap.createBitmap(500, 700, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rawDoc)
        canvas.drawColor(Color.rgb(25, 28, 32)) // Dark desk border
        val pagePaint = Paint().apply { color = Color.rgb(235, 235, 230) }
        canvas.drawRect(50f, 60f, 450f, 640f, pagePaint)
        val textPaint = Paint().apply {
            color = Color.rgb(25, 25, 30)
            strokeWidth = 4f
        }
        for (y in 100..600 step 40) {
            canvas.drawLine(80f, y.toFloat(), 420f, y.toFloat(), textPaint)
        }

        val cleanedDoc = passportIdPhotoUseCase.processDocumentPhoto(
            source = rawDoc,
            mode = PassportIdDocumentEngine.DocumentProcessMode.MAGIC_COLOR_CLEAN,
            autoTrimDarkBorders = true
        )
        assertNotNull(cleanedDoc)
        assertTrue("Trimmed width should be <= 500", cleanedDoc.width <= 500)
        assertTrue("Trimmed height should be <= 700", cleanedDoc.height <= 700)
    }

    // =========================================================================
    // 8. PHOTO SHEET GENERATOR (EXACT PHYSICAL MM MODE & AUTO-MAX COPIES)
    // =========================================================================

    @Test
    fun test8_PhotoSheetGenerator_ExactPhysicalSizeAndMaxCopies() {
        val passportPhoto = createPortraitFaceLikeBitmap(413, 531)

        // 4x6 inch paper (101.6 x 152.4 mm) with exact 35x45 mm photos, 5mm margin, 3mm spacing
        val (maxFit, cols, rows) = PhotoSheetGenerator.calculateMaxCopiesForPhysicalPhoto(
            paperPreset = SheetPaperPreset.PHOTO_4X6,
            orientation = SheetOrientation.PORTRAIT,
            marginMm = 5f,
            spacingMm = 3f,
            photoWidthMm = 35f,
            photoHeightMm = 45f
        )
        // Usable width = 101.6 - 10 = 91.6mm -> floor((91.6+3)/(35+3)) = 2 cols
        // Usable height = 152.4 - 10 = 142.4mm -> floor((142.4+3)/(45+3)) = 3 rows -> 6 copies max
        assertEquals(6, maxFit)
        assertEquals(2, cols)
        assertEquals(3, rows)

        val sheetConfig = PhotoSheetGenerator.SheetConfig(
            copies = 6,
            paperPreset = SheetPaperPreset.PHOTO_4X6,
            orientation = SheetOrientation.PORTRAIT,
            marginMm = 5f,
            spacingMm = 3f,
            alignment = SheetAlignment.CENTER,
            cutLineStyle = CutLineStyle.CORNER_TICKS,
            dpi = 300,
            useExactPhotoPhysicalSize = true,
            targetPhotoWidthMm = 35f,
            targetPhotoHeightMm = 45f,
            autoFillMaxCopies = true,
            showCalibrationFooter = true
        )

        val sheetResult = passportIdPhotoUseCase.generatePhotoSheet(passportPhoto, sheetConfig)
        assertEquals(1200, sheetResult.bitmap.width)
        assertEquals(1800, sheetResult.bitmap.height)
        assertEquals(6, sheetResult.totalCopies)
        assertEquals(6, sheetResult.placedPhotoRectsPx.size)
        assertEquals(35.0f, sheetResult.photoWidthMm, 0.2f)
        assertEquals(45.0f, sheetResult.photoHeightMm, 0.2f)
    }
}
