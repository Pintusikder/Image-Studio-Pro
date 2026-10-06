package com.example.model

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import com.example.processing.BackgroundProcessor
import com.example.processing.ImageEnhancer
import com.example.processing.PerspectiveEngine

/**
 * Non-Destructive Editing Instructions (Request 31).
 * Captures all editing transformations as a lightweight set of parameters.
 * Preserves the pristine original image without keeping duplicate full-resolution
 * bitmaps in memory.
 */
data class EditingInstructions(
    // 1. Geometry & Alignment
    val rotationAngle: Float = 0f,
    val straightenAngle: Float = 0f,
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,

    // 2. Perspective Rectification
    val perspectiveQuad: PerspectiveEngine.PerspectiveQuad? = null,
    val perspectivePreset: PerspectiveEngine.PerspectiveOutputPreset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
    val perspectiveEnhanceMode: PerspectiveEngine.DocumentEnhanceMode = PerspectiveEngine.DocumentEnhanceMode.ORIGINAL,

    // 3. Crop Rectangle (Normalized 0f..1f)
    val cropRect: RectF? = null,

    // 4. Background Replacement & Cutout
    val bgConfig: BackgroundProcessor.BackgroundConfig? = null,

    // 5. Tonal & Color Enhancements
    val enhancementParams: ImageEnhancer.EnhancementParameters = ImageEnhancer.EnhancementParameters.DEFAULT,

    // 6. Resizing & Dimensions
    val targetWidthPx: Int? = null,
    val targetHeightPx: Int? = null,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val fitBackgroundColor: Int = Color.TRANSPARENT,

    // 7. Output DPI
    val dpi: Int = 300,

    // 8. Step Label
    val stepDescription: String = "Original"
) {
    val isDefault: Boolean
        get() = rotationAngle == 0f &&
                straightenAngle == 0f &&
                !isFlippedHorizontally &&
                !isFlippedVertically &&
                perspectiveQuad == null &&
                (cropRect == null || (cropRect.left <= 0.001f && cropRect.top <= 0.001f && cropRect.right >= 0.999f && cropRect.bottom >= 0.999f)) &&
                bgConfig == null &&
                enhancementParams.isNeutral &&
                targetWidthPx == null &&
                targetHeightPx == null
}
