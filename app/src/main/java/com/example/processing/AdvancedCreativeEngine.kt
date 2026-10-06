package com.example.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Creative & Diagnostic Advanced Processing Engine (Requirement 49 & 50):
 *
 * Provides real, reliable, on-device algorithms:
 * - Text & Watermark Overlay (opacity, angle, positioning)
 * - Border, Rounded Corners & Drop Shadow frames
 * - Box Blur & Mosaic Pixelation
 * - Laplacian Variance Blur Detection (Focus Sharpness Metric)
 * - Dynamic Range & Exposure Quality Analysis
 * - 64-bit Perceptual Difference Hash (dHash) for duplicate detection
 * - Multi-Image Photo Collage compositor (2 to 4 photo split arrangements)
 */
object AdvancedCreativeEngine {

    enum class WatermarkPosition(val displayName: String) {
        CENTER("Center"),
        BOTTOM_RIGHT("Bottom-Right"),
        BOTTOM_LEFT("Bottom-Left"),
        TOP_RIGHT("Top-Right"),
        TOP_LEFT("Top-Left"),
        TILED("Tiled Diagonal")
    }

    data class WatermarkConfig(
        val text: String = "",
        val textSizeSp: Float = 24f,
        val textColor: Int = Color.WHITE,
        val opacity: Float = 0.7f,
        val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        val rotationDegrees: Float = 0f,
        val isBold: Boolean = true
    )

    data class FrameConfig(
        val borderWidthPx: Int = 0,
        val borderColor: Int = Color.WHITE,
        val cornerRadiusPx: Int = 0,
        val addDropShadow: Boolean = false,
        val shadowRadiusPx: Float = 16f,
        val shadowColor: Int = 0x88000000.toInt()
    )

    data class ImageQualityReport(
        val sharpnessScore: Double, // Laplacian variance
        val isBlurry: Boolean,
        val sharpnessLabel: String,
        val dynamicRangeScore: Int, // 0 - 100
        val clippedHighlightsPercent: Float,
        val clippedShadowsPercent: Float,
        val recommendation: String
    )

    /**
     * Applies text or watermark overlay with configurable opacity, position, rotation, and font style.
     */
    fun applyWatermark(
        source: Bitmap,
        config: WatermarkConfig
    ): Bitmap {
        if (config.text.isBlank()) return source

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
            alpha = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
            textSize = config.textSizeSp * (source.width / 400f).coerceIn(1f, 4f)
            typeface = if (config.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setShadowLayer(4f, 2f, 2f, 0x99000000.toInt())
        }

        val textBounds = Rect()
        paint.getTextBounds(config.text, 0, config.text.length, textBounds)
        val textWidth = paint.measureText(config.text)
        val textHeight = textBounds.height().toFloat()

        val padding = source.width * 0.04f

        if (config.position == WatermarkPosition.TILED) {
            val stepX = textWidth + padding * 2
            val stepY = textHeight + padding * 3
            var y = 0f
            while (y < source.height + stepY) {
                var x = -stepX
                while (x < source.width + stepX) {
                    canvas.save()
                    canvas.rotate(-30f, x, y)
                    canvas.drawText(config.text, x, y, paint)
                    canvas.restore()
                    x += stepX
                }
                y += stepY
            }
        } else {
            val (x, y) = when (config.position) {
                WatermarkPosition.CENTER -> {
                    (source.width - textWidth) / 2f to (source.height + textHeight) / 2f
                }
                WatermarkPosition.BOTTOM_RIGHT -> {
                    source.width - textWidth - padding to source.height - padding
                }
                WatermarkPosition.BOTTOM_LEFT -> {
                    padding to source.height - padding
                }
                WatermarkPosition.TOP_RIGHT -> {
                    source.width - textWidth - padding to textHeight + padding
                }
                WatermarkPosition.TOP_LEFT -> {
                    padding to textHeight + padding
                }
                else -> padding to source.height - padding
            }

            canvas.save()
            if (config.rotationDegrees != 0f) {
                canvas.rotate(config.rotationDegrees, x + textWidth / 2, y - textHeight / 2)
            }
            canvas.drawText(config.text, x, y, paint)
            canvas.restore()
        }

        return result
    }

    /**
     * Applies styling borders, rounded corner masks, and drop shadows to an image.
     */
    fun applyFrameStyling(
        source: Bitmap,
        config: FrameConfig
    ): Bitmap {
        if (config.borderWidthPx <= 0 && config.cornerRadiusPx <= 0 && !config.addDropShadow) {
            return source
        }

        val shadowMargin = if (config.addDropShadow) (config.shadowRadiusPx * 2).toInt() else 0
        val outWidth = source.width + (config.borderWidthPx * 2) + shadowMargin * 2
        val outHeight = source.height + (config.borderWidthPx * 2) + shadowMargin * 2

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val imageLeft = shadowMargin + config.borderWidthPx.toFloat()
        val imageTop = shadowMargin + config.borderWidthPx.toFloat()
        val imageRight = imageLeft + source.width
        val imageBottom = imageTop + source.height

        val rect = RectF(imageLeft, imageTop, imageRight, imageBottom)

        // Drop shadow
        if (config.addDropShadow) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.shadowColor
                maskFilter = android.graphics.BlurMaskFilter(
                    config.shadowRadiusPx,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            val shadowRect = RectF(rect)
            shadowRect.offset(4f, 8f)
            canvas.drawRoundRect(shadowRect, config.cornerRadiusPx.toFloat(), config.cornerRadiusPx.toFloat(), shadowPaint)
        }

        // Clip rounded corners if requested
        if (config.cornerRadiusPx > 0) {
            val path = android.graphics.Path().apply {
                addRoundRect(
                    rect,
                    config.cornerRadiusPx.toFloat(),
                    config.cornerRadiusPx.toFloat(),
                    android.graphics.Path.Direction.CW
                )
            }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(source, imageLeft, imageTop, null)
            canvas.restore()
        } else {
            canvas.drawBitmap(source, imageLeft, imageTop, null)
        }

        // Draw solid border
        if (config.borderWidthPx > 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = config.borderWidthPx.toFloat()
                color = config.borderColor
            }
            val borderRect = RectF(
                imageLeft - config.borderWidthPx / 2f,
                imageTop - config.borderWidthPx / 2f,
                imageRight + config.borderWidthPx / 2f,
                imageBottom + config.borderWidthPx / 2f
            )
            canvas.drawRoundRect(
                borderRect,
                config.cornerRadiusPx.toFloat(),
                config.cornerRadiusPx.toFloat(),
                borderPaint
            )
        }

        return output
    }

    /**
     * Fast Box Blur with iterative passes for smooth bokeh effects.
     */
    fun applyFastBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source
        val clampedRadius = radius.coerceIn(1, 50)
        val w = source.width
        val h = source.height
        val pix = IntArray(w * h)
        source.getPixels(pix, 0, w, 0, 0, w, h)

        // Downscale-blur-upscale trick for fast, high-quality blurring
        val scale = if (clampedRadius > 10) 0.5f else 1f
        val scaledW = (w * scale).toInt().coerceAtLeast(1)
        val scaledH = (h * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)

        val smallPix = IntArray(scaledW * scaledH)
        small.getPixels(smallPix, 0, scaledW, 0, 0, scaledW, scaledH)

        val r = (clampedRadius * scale).toInt().coerceAtLeast(1)
        fastBlurPixels(smallPix, scaledW, scaledH, r)

        val blurredSmall = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        blurredSmall.setPixels(smallPix, 0, scaledW, 0, 0, scaledW, scaledH)

        return Bitmap.createScaledBitmap(blurredSmall, w, h, true)
    }

    private fun fastBlurPixels(pix: IntArray, w: Int, h: Int, r: Int) {
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val rArr = IntArray(wh)
        val gArr = IntArray(wh)
        val bArr = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int
        var x: Int; var y: Int; var i: Int; var p: Int; var yp: Int; var yi: Int; var yw: Int

        val vmin = IntArray(max(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0; yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = r + 1
        var routsum: Int; var goutsum: Int; var boutsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            for (idx in -r..r) {
                p = pix[yi + min(wm, max(idx, 0))]
                sir = stack[idx + r]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - abs(idx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = r

            for (curX in 0 until w) {
                rArr[yi] = dv[rsum]
                gArr[yi] = dv[gsum]
                bArr[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curY == 0) {
                    vmin[curX] = min(curX + r + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            yp = -r * w
            for (idx in -r..r) {
                yi = max(0, yp) + curX
                sir = stack[idx + r]
                sir[0] = rArr[yi]
                sir[1] = gArr[yi]
                sir[2] = bArr[yi]
                rbs = r1 - abs(idx)
                rsum += rArr[yi] * rbs
                gsum += gArr[yi] * rbs
                bsum += bArr[yi] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (idx < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = r
            for (curY in 0 until h) {
                pix[yi] = (0xff000000.toInt() or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum])
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (curX == 0) {
                    vmin[curY] = min(curY + r1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = rArr[p]
                sir[1] = gArr[p]
                sir[2] = bArr[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }
    }

    /**
     * Mosaic Pixelation effect with configurable block size.
     */
    fun applyPixelate(source: Bitmap, blockSize: Int): Bitmap {
        if (blockSize <= 1) return source
        val clampedSize = blockSize.coerceIn(2, 100)
        val w = source.width
        val h = source.height

        val smallW = (w / clampedSize).coerceAtLeast(1)
        val smallH = (h / clampedSize).coerceAtLeast(1)

        val small = Bitmap.createScaledBitmap(source, smallW, smallH, false)
        return Bitmap.createScaledBitmap(small, w, h, false)
    }

    /**
     * Focus Sharpness & Blur Detection using Laplacian Operator Variance.
     * Computes the variance of the 3x3 Laplacian kernel convolution. Low variance = blurry.
     */
    fun analyzeImageQuality(bitmap: Bitmap): ImageQualityReport {
        val sampleW = 400
        val sampleH = (bitmap.height * (400f / bitmap.width)).toInt().coerceAtLeast(100)
        val sampled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)

        val w = sampled.width
        val h = sampled.height
        val pixels = IntArray(w * h)
        sampled.getPixels(pixels, 0, w, 0, 0, w, h)

        // Convert to grayscale
        val gray = DoubleArray(w * h)
        var shadowClipped = 0
        var highlightClipped = 0

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            gray[i] = lum

            if (lum < 5) shadowClipped++
            if (lum > 250) highlightClipped++
        }

        val totalPixels = (w * h).toDouble()
        val shadowPercent = ((shadowClipped / totalPixels) * 100).toFloat()
        val highlightPercent = ((highlightClipped / totalPixels) * 100).toFloat()

        // 3x3 Laplacian Kernel:
        // [ 0,  1,  0 ]
        // [ 1, -4,  1 ]
        // [ 0,  1,  0 ]
        var lapSum = 0.0
        var lapSqSum = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            val yOffset = y * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x
                val lap = gray[idx - w] + gray[idx + w] + gray[idx - 1] + gray[idx + 1] - 4.0 * gray[idx]
                lapSum += lap
                lapSqSum += lap * lap
                count++
            }
        }

        val mean = if (count > 0) lapSum / count else 0.0
        val variance = if (count > 0) (lapSqSum / count) - (mean * mean) else 0.0

        val isBlurry = variance < 100.0
        val label = when {
            variance > 500.0 -> "Crisp & Sharp"
            variance > 180.0 -> "Good Focus"
            variance > 90.0 -> "Moderate Softness"
            else -> "Noticeable Motion/Focus Blur"
        }

        // Dynamic Range Score (0-100)
        val drScore = (100 - (shadowPercent * 0.8f + highlightPercent * 1.2f).coerceAtMost(100f)).toInt()

        val recommendation = when {
            isBlurry -> "Image appears blurry. Consider using sharpening filter or retaking photo."
            highlightPercent > 10f -> "Highlights are blown out. Try lowering brightness/exposure."
            shadowPercent > 15f -> "Deep shadows clipped. Try shadow boost or HDR curves."
            else -> "Image optical sharpness and dynamic exposure look great."
        }

        return ImageQualityReport(
            sharpnessScore = (variance * 10).roundToInt() / 10.0,
            isBlurry = isBlurry,
            sharpnessLabel = label,
            dynamicRangeScore = drScore,
            clippedHighlightsPercent = (highlightPercent * 10).roundToInt() / 10f,
            clippedShadowsPercent = (shadowPercent * 10).roundToInt() / 10f,
            recommendation = recommendation
        )
    }

    /**
     * Computes 64-bit Difference Hash (dHash) for fast, robust duplicate image detection.
     * Scale to 9x8 grayscale, compare adjacent pixel intensities.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        val pixels = IntArray(72)
        scaled.getPixels(pixels, 0, 9, 0, 0, 9, 8)

        var hash = 0L
        for (row in 0 until 8) {
            val offset = row * 9
            for (col in 0 until 8) {
                val left = pixels[offset + col] and 0xFF
                val right = pixels[offset + col + 1] and 0xFF
                if (left > right) {
                    hash = hash or (1L shl (row * 8 + col))
                }
            }
        }
        return hash
    }

    /**
     * Computes Hamming distance between two 64-bit hashes (0 = exact match, <= 10 = near duplicate).
     */
    fun hammingDistance(hashA: Long, hashB: Long): Int {
        return java.lang.Long.bitCount(hashA xor hashB)
    }

    /**
     * Calculates perceptual similarity percentage (0..100%) between two bitmaps using 64-bit dHash.
     */
    fun calculateSimilarityPercent(bitmapA: Bitmap, bitmapB: Bitmap): Int {
        val hashA = computeDHash(bitmapA)
        val hashB = computeDHash(bitmapB)
        val dist = hammingDistance(hashA, hashB)
        return (((64 - dist) * 100f) / 64f).toInt().coerceIn(0, 100)
    }

    enum class RedactionMode(val label: String) {
        PIXELATE("Pixelate"),
        BLUR("Gaussian Blur"),
        BLACKOUT("Solid Blackout")
    }

    data class PrivacyRegion(
        val normalizedRect: RectF,
        val mode: RedactionMode = RedactionMode.PIXELATE
    )

    /**
     * Applies selective privacy redaction (Pixelate, Blur, or Solid Blackout) to normalized regions.
     */
    fun applyRegionRedaction(
        source: Bitmap,
        regions: List<PrivacyRegion>,
        blockSize: Int = 24,
        blurRadius: Int = 25
    ): Bitmap {
        if (regions.isEmpty()) return source.copy(Bitmap.Config.ARGB_8888, true)
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val w = out.width
        val h = out.height

        for (region in regions) {
            val left = (region.normalizedRect.left.coerceIn(0f, 1f) * w).toInt().coerceIn(0, w - 1)
            val top = (region.normalizedRect.top.coerceIn(0f, 1f) * h).toInt().coerceIn(0, h - 1)
            val right = (region.normalizedRect.right.coerceIn(0f, 1f) * w).toInt().coerceIn(left + 1, w)
            val bottom = (region.normalizedRect.bottom.coerceIn(0f, 1f) * h).toInt().coerceIn(top + 1, h)
            val rw = right - left
            val rh = bottom - top
            if (rw <= 1 || rh <= 1) continue

            when (region.mode) {
                RedactionMode.BLACKOUT -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                }
                RedactionMode.PIXELATE -> {
                    val patch = Bitmap.createBitmap(out, left, top, rw, rh)
                    val pixelated = applyPixelate(patch, blockSize.coerceAtLeast(4))
                    canvas.drawBitmap(pixelated, left.toFloat(), top.toFloat(), null)
                }
                RedactionMode.BLUR -> {
                    val patch = Bitmap.createBitmap(out, left, top, rw, rh)
                    val blurred = applyFastBlur(patch, blurRadius.coerceAtLeast(4))
                    canvas.drawBitmap(blurred, left.toFloat(), top.toFloat(), null)
                }
            }
        }
        return out
    }

    data class PaletteSwatch(
        val colorInt: Int,
        val hexCode: String,
        val populationFraction: Float
    )

    /**
     * Extracts dominant color palette swatches from a bitmap using quantized color histogram buckets.
     */
    fun extractColorPalette(source: Bitmap, maxColors: Int = 6): List<PaletteSwatch> {
        val sampleW = min(64, source.width.coerceAtLeast(1))
        val sampleH = min(64, source.height.coerceAtLeast(1))
        val scaled = Bitmap.createScaledBitmap(source, sampleW, sampleH, true)
        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (scaled != source) scaled.recycle()

        val counts = HashMap<Int, Int>()
        var rSumMap = HashMap<Int, Long>()
        var gSumMap = HashMap<Int, Long>()
        var bSumMap = HashMap<Int, Long>()
        var validPixels = 0

        for (px in pixels) {
            if (Color.alpha(px) < 64) continue
            validPixels++
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            val key = ((r shr 5) shl 6) or ((g shr 5) shl 3) or (b shr 5)
            counts[key] = (counts[key] ?: 0) + 1
            rSumMap[key] = (rSumMap[key] ?: 0L) + r
            gSumMap[key] = (gSumMap[key] ?: 0L) + g
            bSumMap[key] = (bSumMap[key] ?: 0L) + b
        }

        if (validPixels == 0) return emptyList()

        return counts.entries
            .sortedByDescending { it.value }
            .take(maxColors.coerceAtLeast(1))
            .map { (key, count) ->
                val avgR = ((rSumMap[key] ?: 0L) / count).toInt().coerceIn(0, 255)
                val avgG = ((gSumMap[key] ?: 0L) / count).toInt().coerceIn(0, 255)
                val avgB = ((bSumMap[key] ?: 0L) / count).toInt().coerceIn(0, 255)
                val cInt = Color.rgb(avgR, avgG, avgB)
                val hex = String.format("#%02X%02X%02X", avgR, avgG, avgB)
                PaletteSwatch(
                    colorInt = cInt,
                    hexCode = hex,
                    populationFraction = count.toFloat() / validPixels.toFloat()
                )
            }
    }

    /**
     * Composites 2, 3, or 4 images into a photo collage with configurable layout and border spacing.
     */
    fun createCollage(
        images: List<Bitmap>,
        outputWidth: Int = 1200,
        outputHeight: Int = 1200,
        spacingPx: Int = 16,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        if (images.isEmpty()) {
            return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        }
        if (images.size == 1) {
            return Bitmap.createScaledBitmap(images[0], outputWidth, outputHeight, true)
        }

        val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)

        val halfW = (outputWidth - spacingPx * 3) / 2
        val halfH = (outputHeight - spacingPx * 3) / 2

        when (images.size) {
            2 -> {
                val colW = (outputWidth - spacingPx * 3) / 2
                val h = outputHeight - spacingPx * 2
                val b1 = BitmapUtils.resizeBitmapWithMode(images[0], colW, h, com.example.model.ResizeMode.FILL)
                val b2 = BitmapUtils.resizeBitmapWithMode(images[1], colW, h, com.example.model.ResizeMode.FILL)
                canvas.drawBitmap(b1, spacingPx.toFloat(), spacingPx.toFloat(), null)
                canvas.drawBitmap(b2, (spacingPx * 2 + colW).toFloat(), spacingPx.toFloat(), null)
            }
            3 -> {
                val topH = (outputHeight - spacingPx * 3) / 2
                val botH = topH
                val botW = (outputWidth - spacingPx * 3) / 2

                val b1 = BitmapUtils.resizeBitmapWithMode(images[0], outputWidth - spacingPx * 2, topH, com.example.model.ResizeMode.FILL)
                val b2 = BitmapUtils.resizeBitmapWithMode(images[1], botW, botH, com.example.model.ResizeMode.FILL)
                val b3 = BitmapUtils.resizeBitmapWithMode(images[2], botW, botH, com.example.model.ResizeMode.FILL)

                canvas.drawBitmap(b1, spacingPx.toFloat(), spacingPx.toFloat(), null)
                val botY = (spacingPx * 2 + topH).toFloat()
                canvas.drawBitmap(b2, spacingPx.toFloat(), botY, null)
                canvas.drawBitmap(b3, (spacingPx * 2 + botW).toFloat(), botY, null)
            }
            else -> {
                val b1 = BitmapUtils.resizeBitmapWithMode(images[0], halfW, halfH, com.example.model.ResizeMode.FILL)
                val b2 = BitmapUtils.resizeBitmapWithMode(images[1], halfW, halfH, com.example.model.ResizeMode.FILL)
                val b3 = BitmapUtils.resizeBitmapWithMode(images[2], halfW, halfH, com.example.model.ResizeMode.FILL)
                val b4 = BitmapUtils.resizeBitmapWithMode(images[3], halfW, halfH, com.example.model.ResizeMode.FILL)

                val x1 = spacingPx.toFloat()
                val x2 = (spacingPx * 2 + halfW).toFloat()
                val y1 = spacingPx.toFloat()
                val y2 = (spacingPx * 2 + halfH).toFloat()

                canvas.drawBitmap(b1, x1, y1, null)
                canvas.drawBitmap(b2, x2, y1, null)
                canvas.drawBitmap(b3, x1, y2, null)
                canvas.drawBitmap(b4, x2, y2, null)
            }
        }

        return result
    }
}
