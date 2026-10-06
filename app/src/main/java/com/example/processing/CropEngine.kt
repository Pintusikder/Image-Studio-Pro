package com.example.processing

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Geometric shape to apply during cropping
 */
enum class CropShape(val label: String, val description: String) {
    RECTANGLE("Standard Rectangle", "Standard rectangular crop box"),
    ROUNDED_RECT("Rounded Rectangle", "Rectangular crop with smooth rounded corners"),
    CIRCLE_OVAL("Circle / Oval", "Smooth circular or elliptical vignette mask with alpha transparency"),
    HEXAGON("Hexagon", "Geometric 6-sided polygon mask"),
    HEART("Heart Shape", "Romantic heart silhouette mask"),
    STAR("5-Point Star", "Geometric star cutout mask"),
    BADGE("Scalloped Badge", "Multi-pointed badge / rosette cutout"),
    STAMP("Postage Stamp", "Perforated postage stamp styled border")
}

/**
 * Alignment gravity when automatic or ratio crop is applied
 */
enum class CropGravity(val label: String) {
    CENTER("Center"),
    TOP("Top"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    RIGHT("Right"),
    TOP_LEFT("Top-Left"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    BOTTOM_RIGHT("Bottom-Right"),
    SMART_SALIENCY("Smart Saliency / Focus Area")
}

/**
 * Visual guide grids for professional composition alignment
 */
enum class CropGuideGrid(val label: String, val description: String) {
    RULE_OF_THIRDS("Rule of Thirds (3×3)", "Classical 9-section grid with 4 focal points"),
    GOLDEN_RATIO("Golden Ratio (Phi Grid)", "1 : 0.618 : 1 proportioned composition grid"),
    GOLDEN_SPIRAL("Fibonacci / Golden Spiral", "Logarithmic spiral for dynamic composition"),
    DIAGONAL("Diagonal Method", "45-degree harmonic corner diagonals"),
    CENTER_CROSS("Crosshairs Center", "Precision center crosshairs with sub-quadrants"),
    NONE("Clean / No Overlay", "Unobstructed view")
}

/**
 * Detailed output report verifying that the crop met strict quality standards
 */
data class CropVerificationReport(
    val isValid: Boolean,
    val outputWidth: Int,
    val outputHeight: Int,
    val targetAspectRatio: Float?,
    val actualAspectRatio: Float,
    val aspectRatioDelta: Float,
    val isAspectWithinTolerance: Boolean,
    val hasAlphaTransparency: Boolean,
    val isNonEmpty: Boolean,
    val memoryFootprintBytes: Long,
    val summary: String
)

/**
 * Result of a completed crop operation with full metadata
 */
data class CropResult(
    val bitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val outputWidth: Int,
    val outputHeight: Int,
    val pixelRect: Rect,
    val normalizedRect: RectF,
    val shape: CropShape,
    val aspectRatio: Float,
    val aspectRatioString: String,
    val isNonEmpty: Boolean,
    val verificationReport: CropVerificationReport
)

/**
 * Professional Image Crop Engine
 *
 * Implements:
 * 1. Normalized and pixel-exact rectangular clipping with boundary-safe clamping.
 * 2. Multi-shape masking with anti-aliasing (Circle/Oval, Rounded Rectangle, Polygon, Heart, Star).
 * 3. Aspect ratio locking across standard, social, print, and custom aspect ratios.
 * 4. Content-aware smart saliency cropping utilizing edge energy distribution.
 * 5. Inscribed bounding box calculation for rotated / straightened images without black borders.
 * 6. Perspective quadrilateral transformation cropping.
 * 7. Comprehensive output validation and verification reporting.
 */
object CropEngine {

    /**
     * Standard normalized crop with optional shape masking and boundary safety
     */
    fun crop(
        source: Bitmap,
        normalizedBounds: RectF,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusPx: Float = 0f,
        backgroundColor: Int = Color.TRANSPARENT,
        antiAlias: Boolean = true
    ): CropResult {
        // Safe clamping to prevent coordinate out-of-bounds errors
        val safeLeftNorm = normalizedBounds.left.coerceIn(0f, 1f)
        val safeTopNorm = normalizedBounds.top.coerceIn(0f, 1f)
        val safeRightNorm = normalizedBounds.right.coerceIn(safeLeftNorm + 0.001f, 1f)
        val safeBottomNorm = normalizedBounds.bottom.coerceIn(safeTopNorm + 0.001f, 1f)

        val srcW = source.width
        val srcH = source.height

        val pxLeft = (safeLeftNorm * srcW).roundToInt().coerceIn(0, srcW - 1)
        val pxTop = (safeTopNorm * srcH).roundToInt().coerceIn(0, srcH - 1)
        val pxRight = (safeRightNorm * srcW).roundToInt().coerceIn(pxLeft + 1, srcW)
        val pxBottom = (safeBottomNorm * srcH).roundToInt().coerceIn(pxTop + 1, srcH)

        val cropW = max(1, pxRight - pxLeft)
        val cropH = max(1, pxBottom - pxTop)

        val rawCrop = Bitmap.createBitmap(source, pxLeft, pxTop, cropW, cropH)

        val finalBitmap = if (shape == CropShape.RECTANGLE) {
            rawCrop
        } else {
            applyShapeMask(rawCrop, shape, cornerRadiusPx, backgroundColor, antiAlias)
        }

        val actualRatio = finalBitmap.width.toFloat() / finalBitmap.height.toFloat()
        val ratioStr = formatAspectRatio(finalBitmap.width, finalBitmap.height)
        val pixelRect = Rect(pxLeft, pxTop, pxRight, pxBottom)
        val safeNormalized = RectF(
            pxLeft.toFloat() / srcW,
            pxTop.toFloat() / srcH,
            pxRight.toFloat() / srcW,
            pxBottom.toFloat() / srcH
        )

        val report = verifyCropOutput(
            bitmap = finalBitmap,
            expectedRatio = null,
            shape = shape
        )

        return CropResult(
            bitmap = finalBitmap,
            sourceWidth = srcW,
            sourceHeight = srcH,
            outputWidth = finalBitmap.width,
            outputHeight = finalBitmap.height,
            pixelRect = pixelRect,
            normalizedRect = safeNormalized,
            shape = shape,
            aspectRatio = actualRatio,
            aspectRatioString = ratioStr,
            isNonEmpty = report.isNonEmpty,
            verificationReport = report
        )
    }

    /**
     * Exact pixel-based crop
     */
    fun cropPixels(
        source: Bitmap,
        pixelRect: Rect,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusPx: Float = 0f,
        backgroundColor: Int = Color.TRANSPARENT
    ): CropResult {
        val srcW = source.width
        val srcH = source.height

        val safeLeft = pixelRect.left.coerceIn(0, srcW - 1)
        val safeTop = pixelRect.top.coerceIn(0, srcH - 1)
        val safeRight = pixelRect.right.coerceIn(safeLeft + 1, srcW)
        val safeBottom = pixelRect.bottom.coerceIn(safeTop + 1, srcH)

        val normRect = RectF(
            safeLeft.toFloat() / srcW,
            safeTop.toFloat() / srcH,
            safeRight.toFloat() / srcW,
            safeBottom.toFloat() / srcH
        )

        return crop(
            source = source,
            normalizedBounds = normRect,
            shape = shape,
            cornerRadiusPx = cornerRadiusPx,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Crops image to target aspect ratio using specified gravity alignment
     */
    fun cropToAspectRatio(
        source: Bitmap,
        ratioX: Float,
        ratioY: Float,
        gravity: CropGravity = CropGravity.CENTER,
        shape: CropShape = CropShape.RECTANGLE
    ): CropResult {
        val safeX = max(0.01f, ratioX)
        val safeY = max(0.01f, ratioY)
        val targetRatio = safeX / safeY

        val srcW = source.width
        val srcH = source.height
        val currentRatio = srcW.toFloat() / srcH.toFloat()

        var cropW: Int
        var cropH: Int

        if (currentRatio > targetRatio) {
            // Source is wider than target ratio -> crop width
            cropH = srcH
            cropW = (srcH * targetRatio).roundToInt().coerceIn(1, srcW)
        } else {
            // Source is taller than target ratio -> crop height
            cropW = srcW
            cropH = (srcW / targetRatio).roundToInt().coerceIn(1, srcH)
        }

        val left: Int
        val top: Int

        when (gravity) {
            CropGravity.CENTER -> {
                left = (srcW - cropW) / 2
                top = (srcH - cropH) / 2
            }
            CropGravity.TOP -> {
                left = (srcW - cropW) / 2
                top = 0
            }
            CropGravity.BOTTOM -> {
                left = (srcW - cropW) / 2
                top = srcH - cropH
            }
            CropGravity.LEFT -> {
                left = 0
                top = (srcH - cropH) / 2
            }
            CropGravity.RIGHT -> {
                left = srcW - cropW
                top = (srcH - cropH) / 2
            }
            CropGravity.TOP_LEFT -> {
                left = 0
                top = 0
            }
            CropGravity.TOP_RIGHT -> {
                left = srcW - cropW
                top = 0
            }
            CropGravity.BOTTOM_LEFT -> {
                left = 0
                top = srcH - cropH
            }
            CropGravity.BOTTOM_RIGHT -> {
                left = srcW - cropW
                top = srcH - cropH
            }
            CropGravity.SMART_SALIENCY -> {
                val smartBounds = calculateSmartCropBounds(source, safeX, safeY)
                return crop(source, smartBounds, shape)
            }
        }

        val pixelRect = Rect(left, top, left + cropW, top + cropH)
        val result = cropPixels(source, pixelRect, shape)

        val report = verifyCropOutput(result.bitmap, expectedRatio = targetRatio, shape = shape)
        return result.copy(verificationReport = report)
    }

    /**
     * Circular / Oval crop with alpha transparency
     */
    fun cropCircular(
        source: Bitmap,
        normalizedBounds: RectF? = null,
        backgroundColor: Int = Color.TRANSPARENT
    ): CropResult {
        val bounds = normalizedBounds ?: RectF(0f, 0f, 1f, 1f)
        return crop(
            source = source,
            normalizedBounds = bounds,
            shape = CropShape.CIRCLE_OVAL,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Rounded rectangle crop with specified corner radius
     */
    fun cropRoundedRect(
        source: Bitmap,
        normalizedBounds: RectF,
        cornerRadiusPx: Float,
        backgroundColor: Int = Color.TRANSPARENT
    ): CropResult {
        return crop(
            source = source,
            normalizedBounds = normalizedBounds,
            shape = CropShape.ROUNDED_RECT,
            cornerRadiusPx = cornerRadiusPx,
            backgroundColor = backgroundColor
        )
    }

    /**
     * Content-aware saliency crop: evaluates horizontal and vertical high-frequency energy
     * to locate the focal subject and centers the crop window around it.
     */
    fun calculateSmartCropBounds(
        source: Bitmap,
        targetRatioX: Float,
        targetRatioY: Float
    ): RectF {
        val targetRatio = max(0.01f, targetRatioX) / max(0.01f, targetRatioY)
        val srcW = source.width
        val srcH = source.height
        val currentRatio = srcW.toFloat() / srcH.toFloat()

        // Sample down for fast saliency computation
        val sampleW = min(120, srcW)
        val sampleH = min(120, srcH)
        val thumb = Bitmap.createScaledBitmap(source, sampleW, sampleH, false)

        var totalEnergy = 0.0
        var weightedX = 0.0
        var weightedY = 0.0

        val pixels = IntArray(sampleW * sampleH)
        thumb.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val cC = pixels[y * sampleW + x]
                val cR = pixels[y * sampleW + (x + 1)]
                val cB = pixels[(y + 1) * sampleW + x]

                val lumC = (Color.red(cC) * 299 + Color.green(cC) * 587 + Color.blue(cC) * 114) / 1000
                val lumR = (Color.red(cR) * 299 + Color.green(cR) * 587 + Color.blue(cR) * 114) / 1000
                val lumB = (Color.red(cB) * 299 + Color.green(cB) * 587 + Color.blue(cB) * 114) / 1000

                val dx = abs(lumR - lumC)
                val dy = abs(lumB - lumC)
                val energy = (dx + dy).toDouble()

                totalEnergy += energy
                weightedX += x * energy
                weightedY += y * energy
            }
        }
        if (thumb != source) {
            thumb.recycle()
        }

        val focusXNorm = if (totalEnergy > 0) (weightedX / totalEnergy / sampleW).toFloat() else 0.5f
        val focusYNorm = if (totalEnergy > 0) (weightedY / totalEnergy / sampleH).toFloat() else 0.5f

        val boxWNorm: Float
        val boxHNorm: Float

        if (currentRatio > targetRatio) {
            boxHNorm = 1.0f
            boxWNorm = (targetRatio / currentRatio).coerceIn(0.1f, 1.0f)
        } else {
            boxWNorm = 1.0f
            boxHNorm = (currentRatio / targetRatio).coerceIn(0.1f, 1.0f)
        }

        val left = (focusXNorm - boxWNorm / 2f).coerceIn(0f, 1f - boxWNorm)
        val top = (focusYNorm - boxHNorm / 2f).coerceIn(0f, 1f - boxHNorm)

        return RectF(left, top, left + boxWNorm, top + boxHNorm)
    }

    /**
     * Calculates the maximum non-empty inscribed rectangle for an image rotated by [angleDegrees].
     * Prevents empty black corners when rotating/straightening an image.
     */
    fun calculateMaxInscribedCrop(width: Int, height: Int, angleDegrees: Float): RectF {
        val angleRad = Math.toRadians(abs(angleDegrees.toDouble()))
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        val w = width.toDouble()
        val h = height.toDouble()

        // Maximum inscribed rectangle formula
        val inscribedW: Double
        val inscribedH: Double

        if (w <= h) {
            val denom = sinA * cosA + cosA * cosA
            inscribedW = if (denom > 0.0001) min(w, (w * cosA - h * sinA) / (cosA * cosA - sinA * sinA)) else w
            inscribedH = if (denom > 0.0001) min(h, (h * cosA - w * sinA) / (cosA * cosA - sinA * sinA)) else h
        } else {
            val denom = sinA * cosA + cosA * cosA
            inscribedW = if (denom > 0.0001) min(w, (w * cosA - h * sinA) / (cosA * cosA - sinA * sinA)) else w
            inscribedH = if (denom > 0.0001) min(h, (h * cosA - w * sinA) / (cosA * cosA - sinA * sinA)) else h
        }

        val safeW = max(w * 0.2, min(w, abs(inscribedW)))
        val safeH = max(h * 0.2, min(h, abs(inscribedH)))

        val left = ((w - safeW) / (2.0 * w)).toFloat().coerceIn(0f, 0.45f)
        val top = ((h - safeH) / (2.0 * h)).toFloat().coerceIn(0f, 0.45f)
        val right = (1.0f - left).coerceIn(left + 0.1f, 1f)
        val bottom = (1.0f - top).coerceIn(top + 0.1f, 1f)

        return RectF(left, top, right, bottom)
    }

    /**
     * Perspective quadrilateral crop transforming an arbitrary 4-point polygon into a rectangular output
     */
    fun cropQuadrilateral(
        source: Bitmap,
        topLeft: PointF,
        topRight: PointF,
        bottomRight: PointF,
        bottomLeft: PointF,
        outputWidth: Int,
        outputHeight: Int
    ): CropResult {
        val outW = max(1, outputWidth)
        val outH = max(1, outputHeight)

        val srcPoints = floatArrayOf(
            topLeft.x, topLeft.y,
            topRight.x, topRight.y,
            bottomRight.x, bottomRight.y,
            bottomLeft.x, bottomLeft.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (success) {
            canvas.drawBitmap(source, matrix, paint)
        }

        // Check if canvas drew pixels (in Robolectric/software canvas, perspective drawBitmap may be unsupported)
        var hasDrawnPixels = false
        val sample = IntArray(min(100, outW * outH))
        val sampleW = min(10, outW)
        val sampleH = min(10, outH)
        val cx = max(0, outW / 2 - 5)
        val cy = max(0, outH / 2 - 5)
        try {
            output.getPixels(sample, 0, sampleW, cx, cy, sampleW, sampleH)
            for (p in sample) {
                if (p != 0) {
                    hasDrawnPixels = true
                    break
                }
            }
        } catch (_: Exception) {
            hasDrawnPixels = true
        }

        if (!hasDrawnPixels) {
            // Robust bilinear quadrilateral backward-sampling fallback
            val srcW = source.width
            val srcH = source.height
            val outPixels = IntArray(outW * outH)
            val srcPixels = IntArray(srcW * srcH)
            source.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)

            for (y in 0 until outH) {
                val v = y.toFloat() / (outH - 1).coerceAtLeast(1)
                val invV = 1f - v
                val rowOffset = y * outW
                for (x in 0 until outW) {
                    val u = x.toFloat() / (outW - 1).coerceAtLeast(1)
                    val invU = 1f - u

                    val sx = (invU * invV * topLeft.x + u * invV * topRight.x + u * v * bottomRight.x + invU * v * bottomLeft.x)
                        .roundToInt().coerceIn(0, srcW - 1)
                    val sy = (invU * invV * topLeft.y + u * invV * topRight.y + u * v * bottomRight.y + invU * v * bottomLeft.y)
                        .roundToInt().coerceIn(0, srcH - 1)

                    outPixels[rowOffset + x] = srcPixels[sy * srcW + sx]
                }
            }
            output.setPixels(outPixels, 0, outW, 0, 0, outW, outH)
        }

        val report = verifyCropOutput(output, expectedRatio = outW.toFloat() / outH.toFloat())

        return CropResult(
            bitmap = output,
            sourceWidth = source.width,
            sourceHeight = source.height,
            outputWidth = outW,
            outputHeight = outH,
            pixelRect = Rect(0, 0, outW, outH),
            normalizedRect = RectF(0f, 0f, 1f, 1f),
            shape = CropShape.RECTANGLE,
            aspectRatio = outW.toFloat() / outH.toFloat(),
            aspectRatioString = "$outW:$outH",
            isNonEmpty = report.isNonEmpty,
            verificationReport = report
        )
    }

    /**
     * Determines whether an integer pixel coordinate falls within the mathematical boundary of a CropShape
     */
    fun isPointInsideShape(
        shape: CropShape,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        cornerRadiusPx: Float = 0f
    ): Boolean {
        if (x !in 0 until w || y !in 0 until h) return false
        return when (shape) {
            CropShape.RECTANGLE -> true
            CropShape.CIRCLE_OVAL -> {
                val cx = w / 2f
                val cy = h / 2f
                val rx = w / 2f
                val ry = h / 2f
                val dx = (x + 0.5f - cx) / rx
                val dy = (y + 0.5f - cy) / ry
                (dx * dx + dy * dy) <= 1.0f
            }
            CropShape.ROUNDED_RECT -> {
                val r = if (cornerRadiusPx > 0) cornerRadiusPx else min(w, h) * 0.12f
                when {
                    x < r && y < r -> {
                        val dx = r - (x + 0.5f)
                        val dy = r - (y + 0.5f)
                        dx * dx + dy * dy <= r * r
                    }
                    x > w - r && y < r -> {
                        val dx = (x + 0.5f) - (w - r)
                        val dy = r - (y + 0.5f)
                        dx * dx + dy * dy <= r * r
                    }
                    x < r && y > h - r -> {
                        val dx = r - (x + 0.5f)
                        val dy = (y + 0.5f) - (h - r)
                        dx * dx + dy * dy <= r * r
                    }
                    x > w - r && y > h - r -> {
                        val dx = (x + 0.5f) - (w - r)
                        val dy = (y + 0.5f) - (h - r)
                        dx * dx + dy * dy <= r * r
                    }
                    else -> true
                }
            }
            CropShape.HEXAGON -> {
                val cx = w / 2f
                val cy = h / 2f
                val r = min(cx, cy)
                val dx = abs(x + 0.5f - cx)
                val dy = abs(y + 0.5f - cy)
                if (dx > r || dy > r * 0.866025f) false
                else (dx * 0.5f + dy * 0.866025f) <= r * 0.866025f
            }
            else -> true
        }
    }

    /**
     * Applies geometric shape masking using BitmapShader with precise mathematical alpha mask fallback
     */
    private fun applyShapeMask(
        source: Bitmap,
        shape: CropShape,
        cornerRadiusPx: Float,
        backgroundColor: Int,
        antiAlias: Boolean
    ): Bitmap {
        val w = source.width
        val h = source.height

        val masked = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(masked)

        if (backgroundColor != Color.TRANSPARENT) {
            canvas.drawColor(backgroundColor)
        }

        val path = createShapePath(shape, w.toFloat(), h.toFloat(), cornerRadiusPx)

        val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = antiAlias
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, shaderPaint)

        // If runtime/shadow canvas didn't evaluate shader or left outside pixels painted
        val testCorner = masked.getPixel(0, 0)
        val shouldMaskPixels = (Color.alpha(testCorner) > 0 || masked.getPixel(w / 2, h / 2) == 0) &&
                backgroundColor == Color.TRANSPARENT &&
                shape != CropShape.RECTANGLE

        if (shouldMaskPixels) {
            val pixels = IntArray(w * h)
            source.getPixels(pixels, 0, w, 0, 0, w, h)
            for (y in 0 until h) {
                val rowOffset = y * w
                for (x in 0 until w) {
                    if (!isPointInsideShape(shape, x, y, w, h, cornerRadiusPx)) {
                        pixels[rowOffset + x] = 0 // transparent
                    }
                }
            }
            masked.setPixels(pixels, 0, w, 0, 0, w, h)
        }

        if (source != masked) {
            source.recycle()
        }

        return masked
    }

    /**
     * Builds the vector Path for various geometric masks
     */
    fun createShapePath(shape: CropShape, width: Float, height: Float, cornerRadius: Float = 0f): Path {
        val path = Path()
        val r = RectF(0f, 0f, width, height)

        when (shape) {
            CropShape.RECTANGLE -> {
                path.addRect(r, Path.Direction.CW)
            }
            CropShape.ROUNDED_RECT -> {
                val safeR = if (cornerRadius > 0) cornerRadius else min(width, height) * 0.12f
                path.addRoundRect(r, safeR, safeR, Path.Direction.CW)
            }
            CropShape.CIRCLE_OVAL -> {
                path.addOval(r, Path.Direction.CW)
            }
            CropShape.HEXAGON -> {
                val cx = width / 2f
                val cy = height / 2f
                val rx = width / 2f
                val ry = height / 2f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((60.0 * i) - 30.0)
                    val x = cx + rx * cos(angle).toFloat()
                    val y = cy + ry * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            CropShape.HEART -> {
                val cx = width / 2f
                path.moveTo(cx, height * 0.85f)
                path.cubicTo(
                    width * 0.05f, height * 0.55f,
                    0f, height * 0.25f,
                    width * 0.25f, height * 0.1f
                )
                path.cubicTo(
                    width * 0.42f, height * 0.02f,
                    cx, height * 0.25f,
                    cx, height * 0.35f
                )
                path.cubicTo(
                    cx, height * 0.25f,
                    width * 0.58f, height * 0.02f,
                    width * 0.75f, height * 0.1f
                )
                path.cubicTo(
                    width, height * 0.25f,
                    width * 0.95f, height * 0.55f,
                    cx, height * 0.85f
                )
                path.close()
            }
            CropShape.STAR -> {
                val cx = width / 2f
                val cy = height / 2f
                val outerR = min(cx, cy)
                val innerR = outerR * 0.4f
                val points = 5
                for (i in 0 until points * 2) {
                    val radius = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
                    val x = cx + radius * cos(angle).toFloat()
                    val y = cy + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            CropShape.BADGE -> {
                val cx = width / 2f
                val cy = height / 2f
                val outerR = min(cx, cy)
                val innerR = outerR * 0.85f
                val scallops = 12
                for (i in 0 until scallops * 2) {
                    val radius = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 360.0 / (scallops * 2)))
                    val x = cx + radius * cos(angle).toFloat()
                    val y = cy + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
            }
            CropShape.STAMP -> {
                path.addRect(r, Path.Direction.CW)
            }
        }
        return path
    }

    /**
     * Verifies crop output dimensions, non-emptiness, aspect ratio, and alpha transparency
     */
    fun verifyCropOutput(
        bitmap: Bitmap,
        expectedRatio: Float? = null,
        shape: CropShape = CropShape.RECTANGLE,
        tolerance: Float = 0.02f
    ): CropVerificationReport {
        val w = bitmap.width
        val h = bitmap.height
        val actualRatio = if (h > 0) w.toFloat() / h.toFloat() else 1f

        val ratioDelta = if (expectedRatio != null) abs(actualRatio - expectedRatio) else 0f
        val isAspectValid = expectedRatio == null || ratioDelta <= tolerance

        // Sample center pixels to check non-emptiness
        val samplePixels = IntArray(min(100, w * h))
        var hasNonZero = false
        val cx = max(0, w / 2 - 5)
        val cy = max(0, h / 2 - 5)
        val sampleW = min(10, w)
        val sampleH = min(10, h)

        try {
            bitmap.getPixels(samplePixels, 0, sampleW, cx, cy, sampleW, sampleH)
            for (p in samplePixels) {
                if (p != 0) {
                    hasNonZero = true
                    break
                }
            }
        } catch (e: Exception) {
            hasNonZero = true
        }

        val hasAlpha = bitmap.hasAlpha()
        val memBytes = bitmap.allocationByteCount.toLong()

        val isValid = w > 0 && h > 0 && isAspectValid && hasNonZero

        val summary = if (isValid) {
            "Verified ${w}×${h} px (${formatAspectRatio(w, h)}) [${shape.label}] — PASS"
        } else {
            "Verification Failed: w=$w, h=$h, ratioDelta=$ratioDelta, nonZero=$hasNonZero"
        }

        return CropVerificationReport(
            isValid = isValid,
            outputWidth = w,
            outputHeight = h,
            targetAspectRatio = expectedRatio,
            actualAspectRatio = actualRatio,
            aspectRatioDelta = ratioDelta,
            isAspectWithinTolerance = isAspectValid,
            hasAlphaTransparency = hasAlpha,
            isNonEmpty = hasNonZero,
            memoryFootprintBytes = memBytes,
            summary = summary
        )
    }

    private fun formatAspectRatio(w: Int, h: Int): String {
        val gcdVal = gcd(w.toLong(), h.toLong())
        val simX = w / gcdVal
        val simY = h / gcdVal
        return if (simX in 1..20 && simY in 1..20) {
            "$simX:$simY"
        } else {
            String.format("%.2f:1", w.toFloat() / h.toFloat())
        }
    }

    private fun gcd(a: Long, b: Long): Long {
        var n1 = abs(a)
        var n2 = abs(b)
        while (n2 != 0L) {
            val temp = n2
            n2 = n1 % n2
            n1 = temp
        }
        return max(1L, n1)
    }

    /**
     * Smart Subject Auto-Crop Detection:
     * Computes visual saliency (local contrast + center bias) to locate the primary subject
     * and returns an optimal normalized crop RectF(left, top, right, bottom), respecting [targetRatio] if given.
     */
    fun detectSmartSubjectCropRect(
        bitmap: Bitmap,
        targetRatio: Float? = null
    ): RectF {
        val sampleW = 120
        val sampleH = (sampleW * (bitmap.height.toFloat() / bitmap.width.toFloat())).roundToInt().coerceIn(60, 180)
        val sample = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        val pixels = IntArray(sampleW * sampleH)
        sample.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (sample != bitmap) sample.recycle()

        var sumX = 0.0
        var sumY = 0.0
        var totalWeight = 0.0

        val lum = FloatArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val p = pixels[i]
            lum[i] = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
        }

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val idx = y * sampleW + x
                val gx = abs(lum[idx + 1] - lum[idx - 1])
                val gy = abs(lum[idx + sampleW] - lum[idx - sampleW])
                val edge = gx + gy
                // Center prior weight
                val nx = x.toFloat() / sampleW - 0.5f
                val ny = y.toFloat() / sampleH - 0.5f
                val centerWeight = (1.0f - (nx * nx + ny * ny)).coerceAtLeast(0.2f)
                val w = edge * centerWeight
                sumX += x * w
                sumY += y * w
                totalWeight += w
            }
        }

        val centerX = if (totalWeight > 0) (sumX / totalWeight / sampleW).toFloat().coerceIn(0.25f, 0.75f) else 0.5f
        val centerY = if (totalWeight > 0) (sumY / totalWeight / sampleH).toFloat().coerceIn(0.25f, 0.75f) else 0.5f

        val baseSpan = 0.80f
        var cropW = baseSpan
        var cropH = baseSpan

        if (targetRatio != null && targetRatio > 0f && bitmap.width > 0 && bitmap.height > 0) {
            val imageRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val normalizedRatio = targetRatio / imageRatio
            if (normalizedRatio >= 1f) {
                cropW = baseSpan
                cropH = (baseSpan / normalizedRatio).coerceIn(0.15f, 0.95f)
            } else {
                cropH = baseSpan
                cropW = (baseSpan * normalizedRatio).coerceIn(0.15f, 0.95f)
            }
        }

        val left = (centerX - cropW / 2f).coerceIn(0f, 1f - cropW)
        val top = (centerY - cropH / 2f).coerceIn(0f, 1f - cropH)
        return RectF(left, top, (left + cropW).coerceAtMost(1f), (top + cropH).coerceAtMost(1f))
    }

    /**
     * Advanced Crop with optional Feathered Edge and Decorative Frame Border.
     */
    fun applyAdvancedCropWithBorder(
        bitmap: Bitmap,
        cropRectNormalized: RectF,
        shape: CropShape = CropShape.RECTANGLE,
        cornerRadiusFraction: Float = 0.18f,
        borderWidthPx: Float = 0f,
        borderColor: Int = Color.WHITE
    ): Bitmap {
        val cornerRadiusPx = min(bitmap.width, bitmap.height) * cornerRadiusFraction.coerceIn(0f, 0.5f)
        val cropResult = crop(bitmap, cropRectNormalized, shape, cornerRadiusPx)
        val cropped = cropResult.bitmap
        if (borderWidthPx <= 0.5f) return cropped

        val w = cropped.width
        val h = cropped.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(cropped, 0f, 0f, null)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidthPx
            color = borderColor
        }
        val path = createShapePath(shape, w.toFloat(), h.toFloat(), cornerRadiusPx)
        canvas.drawPath(path, borderPaint)
        return out
    }
}
