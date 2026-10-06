package com.example.processing

import android.graphics.Bitmap
import com.example.model.EditingInstructions

/**
 * Non-Destructive Editing Pipeline (Request 31).
 * 
 * Pipeline Flow:
 * Original Image → Editing Instructions → Rendered / Exported Image
 * 
 * The pristine original image is NEVER modified. All operations are applied
 * in a deterministic, optimal sequence on-demand.
 * Duplicate full-resolution bitmaps are never stored in memory for history states.
 */
object NonDestructivePipeline {

    /**
     * Executes the sequence: Original → Editing Instructions → Rendered Bitmap
     */
    fun applyPipeline(
        source: Bitmap,
        instructions: EditingInstructions
    ): Bitmap {
        if (instructions.isDefault) {
            return source
        }

        var current = source

        // 1. Perspective Warp (if quad is configured)
        val quad = instructions.perspectiveQuad
        if (quad != null) {
            current = PerspectiveEngine.warpPerspective(
                current,
                quad,
                instructions.perspectivePreset,
                instructions.perspectiveEnhanceMode
            )
        }

        // 2. Straighten, Rotation, and Flipping
        val totalRotation = instructions.rotationAngle + instructions.straightenAngle
        if (totalRotation != 0f) {
            current = RotateStraightenEngine.rotateCustomAngle(
                current,
                totalRotation,
                RotateStraightenEngine.StraightenCropMode.AUTO_CROP
            )
        }

        if (instructions.isFlippedHorizontally || instructions.isFlippedVertically) {
            current = BitmapUtils.flipBitmap(
                current,
                instructions.isFlippedHorizontally,
                instructions.isFlippedVertically
            )
        }

        // 3. Crop Rectangle
        val crop = instructions.cropRect
        if (crop != null && (crop.left > 0.001f || crop.top > 0.001f || crop.right < 0.999f || crop.bottom < 0.999f)) {
            current = BitmapUtils.cropBitmap(current, crop)
        }

        // 4. Background Replacement
        val bg = instructions.bgConfig
        if (bg != null) {
            current = BackgroundProcessor.processBackground(
                source = current,
                config = bg
            )
        }

        // 5. Tonal, Light & Color Enhancements
        if (!instructions.enhancementParams.isNeutral) {
            current = ImageEnhancer.applyEnhancements(
                source = current,
                params = instructions.enhancementParams
            )
        }

        // 6. Resizing / Dimensions
        val targetW = instructions.targetWidthPx
        val targetH = instructions.targetHeightPx
        if (targetW != null && targetH != null && targetW > 0 && targetH > 0 &&
            (targetW != current.width || targetH != current.height)
        ) {
            current = BitmapUtils.resizeBitmapWithMode(
                current,
                targetW,
                targetH,
                instructions.resizeMode,
                instructions.fitBackgroundColor
            )
        }

        return current
    }

    fun serializeInstructions(instructions: EditingInstructions): String {
        val json = org.json.JSONObject().apply {
            put("rotationAngle", instructions.rotationAngle.toDouble())
            put("straightenAngle", instructions.straightenAngle.toDouble())
            put("isFlippedHorizontally", instructions.isFlippedHorizontally)
            put("isFlippedVertically", instructions.isFlippedVertically)
            put("targetWidthPx", instructions.targetWidthPx ?: -1)
            put("targetHeightPx", instructions.targetHeightPx ?: -1)
            put("resizeMode", instructions.resizeMode.name)
            put("dpi", instructions.dpi)
            put("stepDescription", instructions.stepDescription)
            instructions.cropRect?.let { rect ->
                put("crop_left", rect.left.toDouble())
                put("crop_top", rect.top.toDouble())
                put("crop_right", rect.right.toDouble())
                put("crop_bottom", rect.bottom.toDouble())
            }
        }
        return json.toString()
    }

    fun deserializeInstructions(jsonStr: String): EditingInstructions? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val crop = if (json.has("crop_left")) {
                android.graphics.RectF(
                    json.getDouble("crop_left").toFloat(),
                    json.getDouble("crop_top").toFloat(),
                    json.getDouble("crop_right").toFloat(),
                    json.getDouble("crop_bottom").toFloat()
                )
            } else null
            val targetW = json.optInt("targetWidthPx", -1).takeIf { it > 0 }
            val targetH = json.optInt("targetHeightPx", -1).takeIf { it > 0 }
            val resizeMode = try {
                com.example.model.ResizeMode.valueOf(json.optString("resizeMode", "FIT"))
            } catch (e: Exception) {
                com.example.model.ResizeMode.FIT
            }

            EditingInstructions(
                rotationAngle = json.optDouble("rotationAngle", 0.0).toFloat(),
                straightenAngle = json.optDouble("straightenAngle", 0.0).toFloat(),
                isFlippedHorizontally = json.optBoolean("isFlippedHorizontally", false),
                isFlippedVertically = json.optBoolean("isFlippedVertically", false),
                cropRect = crop,
                targetWidthPx = targetW,
                targetHeightPx = targetH,
                resizeMode = resizeMode,
                dpi = json.optInt("dpi", 300),
                stepDescription = json.optString("stepDescription", "History Preset")
            )
        } catch (e: Exception) {
            null
        }
    }
}
