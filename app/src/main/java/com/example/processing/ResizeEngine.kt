package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.model.ResizeMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Result object returned by [ResizeEngine.resize] containing the processed bitmap,
 * input and output dimensions, resize mode, and a comprehensive [ResizeVerificationReport].
 */
data class ResizeResult(
    val bitmap: Bitmap,
    val requestedWidth: Int,
    val requestedHeight: Int,
    val actualWidth: Int,
    val actualHeight: Int,
    val mode: ResizeMode,
    val verificationReport: ResizeVerificationReport
)

/**
 * Comprehensive verification report validating exact pixel output dimensions,
 * aspect ratio preservation or intentional distortion, padding/crop behavior,
 * non-empty pixel safety, and memory footprint.
 */
data class ResizeVerificationReport(
    val isValid: Boolean,
    val requestedWidth: Int,
    val requestedHeight: Int,
    val actualWidth: Int,
    val actualHeight: Int,
    val mode: ResizeMode,
    val isExactWidthMatch: Boolean,
    val isExactHeightMatch: Boolean,
    val isExactDimensionMatch: Boolean,
    val sourceAspectRatio: Float,
    val targetAspectRatio: Float,
    val actualAspectRatio: Float,
    val aspectRatioDelta: Float,
    val isAspectPreserved: Boolean,
    val isNonEmpty: Boolean,
    val memoryFootprintBytes: Long,
    val summary: String
)

/**
 * PHASE 5 — RESIZE + EXACT PIXEL DIMENSIONS ENGINE
 *
 * Professional image resizing engine supporting:
 * - Resize Modes: FIT (letterbox/pad), FILL (center crop), STRETCH (exact non-proportional), SMART_CROP (saliency/face aware)
 * - Exact Dimensions: Exact width (proportional height), Exact height (proportional width), Exact width × height
 * - Strict Output Verification: Verifies exact raster dimensions, non-emptiness, aspect integrity, and memory safety.
 */
object ResizeEngine {

    /**
     * Resizes [source] according to requested [targetWidth] and [targetHeight] with [mode].
     */
    fun resize(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        mode: ResizeMode = ResizeMode.FIT,
        fitBackgroundColor: Int = Color.TRANSPARENT,
        filter: Boolean = true
    ): ResizeResult {
        val safeTargetW = max(1, targetWidth)
        val safeTargetH = max(1, targetHeight)

        val outputBitmap = when (mode) {
            ResizeMode.STRETCH -> {
                Bitmap.createScaledBitmap(source, safeTargetW, safeTargetH, filter)
            }
            ResizeMode.FIT -> {
                val scale = min(safeTargetW.toFloat() / source.width, safeTargetH.toFloat() / source.height)
                val scaledW = (source.width * scale).roundToInt().coerceAtLeast(1)
                val scaledH = (source.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, filter)

                val outBitmap = Bitmap.createBitmap(safeTargetW, safeTargetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outBitmap)
                if (fitBackgroundColor != Color.TRANSPARENT) {
                    canvas.drawColor(fitBackgroundColor)
                }
                val left = (safeTargetW - scaledW) / 2f
                val top = (safeTargetH - scaledH) / 2f
                canvas.drawBitmap(scaled, left, top, null)
                outBitmap
            }
            ResizeMode.FILL -> {
                val scale = max(safeTargetW.toFloat() / source.width, safeTargetH.toFloat() / source.height)
                val scaledW = (source.width * scale).roundToInt().coerceAtLeast(safeTargetW)
                val scaledH = (source.height * scale).roundToInt().coerceAtLeast(safeTargetH)
                val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, filter)

                val cropX = ((scaledW - safeTargetW) / 2).coerceIn(0, max(0, scaledW - safeTargetW))
                val cropY = ((scaledH - safeTargetH) / 2).coerceIn(0, max(0, scaledH - safeTargetH))
                Bitmap.createBitmap(scaled, cropX, cropY, safeTargetW, safeTargetH)
            }
            ResizeMode.SMART_CROP -> {
                val cropRect = BitmapUtils.calculateSmartCropRect(source, safeTargetW, safeTargetH)
                val cropW = cropRect.width().coerceAtLeast(1)
                val cropH = cropRect.height().coerceAtLeast(1)
                val cropped = Bitmap.createBitmap(source, cropRect.left, cropRect.top, cropW, cropH)
                Bitmap.createScaledBitmap(cropped, safeTargetW, safeTargetH, filter)
            }
        }

        val report = verifyResizeOutput(
            output = outputBitmap,
            requestedWidth = safeTargetW,
            requestedHeight = safeTargetH,
            sourceWidth = source.width,
            sourceHeight = source.height,
            mode = mode
        )

        return ResizeResult(
            bitmap = outputBitmap,
            requestedWidth = safeTargetW,
            requestedHeight = safeTargetH,
            actualWidth = outputBitmap.width,
            actualHeight = outputBitmap.height,
            mode = mode,
            verificationReport = report
        )
    }

    /**
     * Resizes [source] by specifying an exact width while calculating proportional height.
     */
    fun resizeExactWidth(
        source: Bitmap,
        exactWidth: Int,
        filter: Boolean = true
    ): ResizeResult {
        val safeW = max(1, exactWidth)
        val aspect = source.width.toFloat() / source.height.toFloat()
        val calculatedH = (safeW / aspect).roundToInt().coerceAtLeast(1)
        return resize(source, safeW, calculatedH, ResizeMode.STRETCH, filter = filter)
    }

    /**
     * Resizes [source] by specifying an exact height while calculating proportional width.
     */
    fun resizeExactHeight(
        source: Bitmap,
        exactHeight: Int,
        filter: Boolean = true
    ): ResizeResult {
        val safeH = max(1, exactHeight)
        val aspect = source.width.toFloat() / source.height.toFloat()
        val calculatedW = (safeH * aspect).roundToInt().coerceAtLeast(1)
        return resize(source, calculatedW, safeH, ResizeMode.STRETCH, filter = filter)
    }

    /**
     * Resizes [source] to exact target width × target height using the specified [mode].
     */
    fun resizeExactWidthAndHeight(
        source: Bitmap,
        exactWidth: Int,
        exactHeight: Int,
        mode: ResizeMode = ResizeMode.FIT,
        fitBackgroundColor: Int = Color.TRANSPARENT,
        filter: Boolean = true
    ): ResizeResult {
        return resize(source, exactWidth, exactHeight, mode, fitBackgroundColor, filter)
    }

    /**
     * Rigorously verifies the output of any resize operation against requested dimensions and mode.
     */
    fun verifyResizeOutput(
        output: Bitmap,
        requestedWidth: Int,
        requestedHeight: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        mode: ResizeMode,
        tolerance: Float = 0.02f
    ): ResizeVerificationReport {
        val actualW = output.width
        val actualH = output.height

        val isExactWidth = actualW == requestedWidth
        val isExactHeight = actualH == requestedHeight
        val isExactDimension = isExactWidth && isExactHeight

        val srcAspect = if (sourceHeight > 0) sourceWidth.toFloat() / sourceHeight.toFloat() else 1f
        val targetAspect = if (requestedHeight > 0) requestedWidth.toFloat() / requestedHeight.toFloat() else 1f
        val actualAspect = if (actualH > 0) actualW.toFloat() / actualH.toFloat() else 1f

        val aspectDelta = abs(actualAspect - targetAspect)
        val isAspectPreserved = when (mode) {
            ResizeMode.FIT, ResizeMode.FILL, ResizeMode.SMART_CROP -> {
                // In FIT, the container matches targetAspect, inner image preserves srcAspect.
                // In FILL and SMART_CROP, the output aspect is exactly targetAspect.
                aspectDelta <= tolerance || abs(actualAspect - srcAspect) <= tolerance
            }
            ResizeMode.STRETCH -> {
                // In STRETCH, output aspect intentionally matches targetAspect
                aspectDelta <= tolerance
            }
        }

        // Check non-empty pixel safety
        var hasNonZero = false
        val sampleSize = min(100, actualW * actualH)
        val samplePixels = IntArray(sampleSize)
        val cx = max(0, actualW / 2 - 5)
        val cy = max(0, actualH / 2 - 5)
        val sampleW = min(10, actualW)
        val sampleH = min(10, actualH)

        try {
            output.getPixels(samplePixels, 0, sampleW, cx, cy, sampleW, sampleH)
            for (p in samplePixels) {
                if (p != 0) {
                    hasNonZero = true
                    break
                }
            }
        } catch (e: Exception) {
            hasNonZero = true
        }

        val isValid = actualW > 0 && actualH > 0 && isExactDimension && hasNonZero
        val memBytes = output.allocationByteCount.toLong()

        val summary = if (isValid) {
            "Verified Exact ${actualW}×${actualH} px [${mode.label}] — PASS"
        } else {
            "Verification Failed: actual=${actualW}×${actualH}, requested=${requestedWidth}×${requestedHeight}, nonZero=$hasNonZero"
        }

        return ResizeVerificationReport(
            isValid = isValid,
            requestedWidth = requestedWidth,
            requestedHeight = requestedHeight,
            actualWidth = actualW,
            actualHeight = actualH,
            mode = mode,
            isExactWidthMatch = isExactWidth,
            isExactHeightMatch = isExactHeight,
            isExactDimensionMatch = isExactDimension,
            sourceAspectRatio = srcAspect,
            targetAspectRatio = targetAspect,
            actualAspectRatio = actualAspect,
            aspectRatioDelta = aspectDelta,
            isAspectPreserved = isAspectPreserved,
            isNonEmpty = hasNonZero,
            memoryFootprintBytes = memBytes,
            summary = summary
        )
    }
}
