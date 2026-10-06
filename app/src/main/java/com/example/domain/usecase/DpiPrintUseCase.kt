package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ExportFormat
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.processing.DpiPrintEngine
import java.io.File

/**
 * Domain UseCase for Phase 8:
 * - DPI / PPI Metadata reading, embedding & verification
 * - Physical dimensions conversion (in, cm, mm, pt, px)
 * - Print calculator & resolution health grading
 */
class DpiPrintUseCase {

    fun calculatePrintMetrics(
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        physicalWidth: Float,
        physicalHeight: Float,
        unit: PrintUnit,
        targetDpi: Int = 300,
        bleedMm: Float = 0f,
        safeMarginMm: Float = 3f,
        mode: DpiPrintEngine.PrintCalculationMode = DpiPrintEngine.PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS
    ): DpiPrintEngine.PrintCalculatorReport {
        return DpiPrintEngine.calculatePrintMetrics(
            sourceWidthPx = sourceWidthPx,
            sourceHeightPx = sourceHeightPx,
            physicalWidth = physicalWidth,
            physicalHeight = physicalHeight,
            unit = unit,
            targetDpi = targetDpi,
            bleedMm = bleedMm,
            safeMarginMm = safeMarginMm,
            mode = mode
        )
    }

    fun resampleForPrint(
        source: Bitmap,
        physicalWidth: Float,
        physicalHeight: Float,
        unit: PrintUnit,
        dpi: Int = 300,
        bleedMm: Float = 0f,
        resizeMode: ResizeMode = ResizeMode.FIT,
        backgroundColor: Int = Color.WHITE
    ): DpiPrintEngine.PrintResampleResult {
        return DpiPrintEngine.resampleForPrint(
            source = source,
            physicalWidth = physicalWidth,
            physicalHeight = physicalHeight,
            unit = unit,
            dpi = dpi,
            bleedMm = bleedMm,
            resizeMode = resizeMode,
            backgroundColor = backgroundColor
        )
    }

    fun embedAndVerifyDpi(
        encodedBytes: ByteArray,
        pixelWidth: Int,
        pixelHeight: Int,
        dpi: Int,
        format: ExportFormat
    ): Pair<ByteArray, DpiPrintEngine.DpiMetadataVerificationReport> {
        val withDpi = DpiPrintEngine.embedDpiInEncodedBytes(encodedBytes, format, dpi)
        val report = DpiPrintEngine.verifyDpiAndPhysicalDimensions(
            bytes = withDpi,
            pixelWidth = pixelWidth,
            pixelHeight = pixelHeight,
            expectedDpi = dpi,
            format = format
        )
        return withDpi to report
    }

    fun verifyFileDpi(
        file: File,
        pixelWidth: Int,
        pixelHeight: Int,
        expectedDpi: Int,
        format: ExportFormat
    ): DpiPrintEngine.DpiMetadataVerificationReport {
        return DpiPrintEngine.verifyFileDpiAndPhysicalDimensions(
            file = file,
            pixelWidth = pixelWidth,
            pixelHeight = pixelHeight,
            expectedDpi = expectedDpi,
            format = format
        )
    }
}
