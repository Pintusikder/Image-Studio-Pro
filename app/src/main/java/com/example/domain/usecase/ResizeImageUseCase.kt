package com.example.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ResizeMode
import com.example.processing.ResizeEngine
import com.example.processing.ResizeResult

/**
 * Clean Architecture Use Case: Handles image resizing across nearest neighbor, bilinear,
 * bicubic, downscale, fit/fill modes, exact dimensions, and output verification.
 */
class ResizeImageUseCase {

    operator fun invoke(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        resizeMode: ResizeMode = ResizeMode.FIT,
        fitBackgroundColor: Int = Color.TRANSPARENT
    ): Bitmap {
        return ResizeEngine.resize(source, targetWidth, targetHeight, resizeMode, fitBackgroundColor).bitmap
    }

    fun executeWithReport(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        resizeMode: ResizeMode = ResizeMode.FIT,
        fitBackgroundColor: Int = Color.TRANSPARENT
    ): ResizeResult {
        return ResizeEngine.resize(source, targetWidth, targetHeight, resizeMode, fitBackgroundColor)
    }

    fun resizeExactWidth(
        source: Bitmap,
        exactWidth: Int
    ): ResizeResult {
        return ResizeEngine.resizeExactWidth(source, exactWidth)
    }

    fun resizeExactHeight(
        source: Bitmap,
        exactHeight: Int
    ): ResizeResult {
        return ResizeEngine.resizeExactHeight(source, exactHeight)
    }

    fun resizeExactWidthAndHeight(
        source: Bitmap,
        exactWidth: Int,
        exactHeight: Int,
        mode: ResizeMode = ResizeMode.FIT,
        fitBackgroundColor: Int = Color.TRANSPARENT
    ): ResizeResult {
        return ResizeEngine.resizeExactWidthAndHeight(source, exactWidth, exactHeight, mode, fitBackgroundColor)
    }

    fun scaleByPercentage(
        source: Bitmap,
        percentage: Int
    ): Bitmap {
        val factor = (percentage.coerceIn(1, 500)) / 100f
        val newW = (source.width * factor).toInt().coerceAtLeast(1)
        val newH = (source.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
