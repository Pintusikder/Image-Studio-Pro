package com.example.domain.usecase

import android.graphics.Bitmap
import com.example.processing.PassportIdDocumentEngine
import com.example.processing.PhotoSheetGenerator
import com.example.processing.SignatureProcessor

/**
 * Domain UseCase for Phase 8:
 * - Passport Photo, ID Photo, Visa Photo, Exam Photo, Document Photo
 * - Biometric Compliance Analysis & Auto-Framing
 * - Signature Processing & Verification
 * - Multi-Copy Photo Sheet Generation
 */
class PassportIdPhotoUseCase {

    fun analyzeBiometricCompliance(
        bitmap: Bitmap,
        targetDpi: Int = 300
    ): PassportIdDocumentEngine.BiometricComplianceReport {
        return PassportIdDocumentEngine.analyzeBiometricCompliance(bitmap, targetDpi)
    }

    fun processPassportOrIdPhoto(
        source: Bitmap,
        config: PassportIdDocumentEngine.PassportIdProcessConfig
    ): PassportIdDocumentEngine.PassportIdProcessResult {
        return PassportIdDocumentEngine.processPassportOrIdPhoto(source, config)
    }

    fun processDocumentPhoto(
        source: Bitmap,
        mode: PassportIdDocumentEngine.DocumentProcessMode,
        autoTrimDarkBorders: Boolean = false
    ): Bitmap {
        return PassportIdDocumentEngine.processDocumentPhoto(source, mode, autoTrimDarkBorders)
    }

    fun generatePhotoSheet(
        photo: Bitmap,
        config: PhotoSheetGenerator.SheetConfig
    ): PhotoSheetGenerator.SheetResult {
        return PhotoSheetGenerator.createPhotoSheet(photo, config)
    }

    fun processAndFitSignature(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        inkMode: SignatureProcessor.SignatureInkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE,
        paperThreshold: Int = 190,
        strokeWeightDelta: Int = 0,
        fitMode: SignatureProcessor.SignatureFitMode = SignatureProcessor.SignatureFitMode.SMART_CROP,
        backgroundType: SignatureProcessor.SignatureBackgroundType = SignatureProcessor.SignatureBackgroundType.WHITE,
        customBackgroundColor: Int = android.graphics.Color.WHITE,
        marginPaddingPx: Int = 16
    ): Bitmap {
        val makeTransparent = backgroundType == SignatureProcessor.SignatureBackgroundType.TRANSPARENT
        val cleaned = SignatureProcessor.processInkAndColor(
            source = source,
            inkMode = inkMode,
            paperThreshold = paperThreshold,
            makeTransparent = makeTransparent,
            backgroundColor = if (backgroundType == SignatureProcessor.SignatureBackgroundType.CUSTOM) customBackgroundColor else android.graphics.Color.WHITE,
            strokeWeightDelta = strokeWeightDelta
        )
        return SignatureProcessor.fitSignatureToTargetBox(
            source = cleaned,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            fitMode = fitMode,
            backgroundType = backgroundType,
            customBackgroundColor = customBackgroundColor,
            center = true,
            marginPaddingPx = marginPaddingPx
        )
    }
}
