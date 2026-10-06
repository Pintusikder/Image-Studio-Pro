package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.model.CropAspectRatio
import com.example.processing.CropEngine
import com.example.processing.CropGravity
import com.example.processing.CropResult
import com.example.processing.CropShape

/**
 * Clean Architecture Use Case: Handles image cropping operations including freeform,
 * fixed aspect ratio cropping, shape masking, and smart saliency cropping.
 */
class CropImageUseCase {

    operator fun invoke(
        source: Bitmap,
        cropRect: RectF,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusPx: Float = 0f
    ): Bitmap {
        return CropEngine.crop(source, cropRect, shape, cornerRadiusPx).bitmap
    }

    fun executeWithReport(
        source: Bitmap,
        cropRect: RectF,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusPx: Float = 0f
    ): CropResult {
        return CropEngine.crop(source, cropRect, shape, cornerRadiusPx)
    }

    fun cropToAspectRatio(
        source: Bitmap,
        ratioX: Float,
        ratioY: Float,
        gravity: CropGravity = CropGravity.CENTER,
        shape: CropShape = CropShape.RECTANGLE
    ): CropResult {
        return CropEngine.cropToAspectRatio(source, ratioX, ratioY, gravity, shape)
    }

    fun calculateSmartCrop(source: Bitmap, ratio: CropAspectRatio): RectF {
        val targetRatioX = (ratio.ratioX ?: 1f).coerceAtLeast(0.01f)
        val targetRatioY = (ratio.ratioY ?: 1f).coerceAtLeast(0.01f)
        return CropEngine.calculateSmartCropBounds(source, targetRatioX, targetRatioY)
    }
}
