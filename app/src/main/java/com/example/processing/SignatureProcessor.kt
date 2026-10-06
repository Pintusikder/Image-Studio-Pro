package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object SignatureProcessor {

    enum class OutputInkColor(val label: String, val hexColor: Int) {
        PURE_BLACK("Pure Black", Color.rgb(17, 24, 39)),
        ROYAL_BLUE("Ballpoint / Royal Blue", Color.rgb(30, 64, 175)),
        NAVY_INK("Fountain Navy Ink", Color.rgb(15, 23, 42)),
        CRIMSON_STAMP("Official Stamp Red", Color.rgb(185, 28, 28)),
        KEEP_ORIGINAL("Original Ink", 0)
    }

    enum class SignatureFitMode(
        val label: String,
        val description: String,
        val warningText: String? = null
    ) {
        FIT(
            label = "FIT",
            description = "Keep the signature proportional inside the target dimensions."
        ),
        FILL(
            label = "FILL",
            description = "Fill the target area while preserving aspect ratio and cropping excess."
        ),
        STRETCH(
            label = "STRETCH",
            description = "Force exact width and height.",
            warningText = "Stretching may distort the signature."
        ),
        SMART_CROP(
            label = "SMART CROP",
            description = "Crop unnecessary whitespace while preserving the signature."
        )
    }

    enum class SignatureBackgroundType(
        val label: String,
        val description: String
    ) {
        WHITE(
            label = "White background",
            description = "Clean solid white background, required by official portals and banks."
        ),
        TRANSPARENT(
            label = "Transparent background",
            description = "Transparent background where technically supported (PNG & WebP formats)."
        ),
        CUSTOM(
            label = "Custom background",
            description = "Custom document background tint or specific hex color code."
        )
    }

    enum class SignatureInkMode(val label: String) {
        ORIGINAL("Original Color"),
        GRAYSCALE("Grayscale"),
        BLACK_AND_WHITE("Clean Black & White (Threshold)"),
        ROYAL_BLUE("Ballpoint Blue"),
        NAVY_INK("Fountain Navy"),
        CRIMSON_STAMP("Official Stamp Red")
    }

    data class SignaturePreset(
        val id: String,
        val title: String,
        val authority: String,
        val widthPx: Int,
        val heightPx: Int,
        val widthCm: Float,
        val heightCm: Float,
        val dpi: Int = 200,
        val maxFileSizeKb: Int = 20,
        val minFileSizeKb: Int? = 10,
        val recommendedInk: SignatureInkMode = SignatureInkMode.BLACK_AND_WHITE,
        val recommendedBg: SignatureBackgroundType = SignatureBackgroundType.WHITE
    ) {
        companion object {
            val PRESETS = listOf(
                SignaturePreset(
                    id = "standard_300x100",
                    title = "Standard Portal (300×100 px)",
                    authority = "General Portals",
                    widthPx = 300,
                    heightPx = 100,
                    widthCm = 3.81f,
                    heightCm = 1.27f,
                    dpi = 200,
                    maxFileSizeKb = 30
                ),
                SignaturePreset(
                    id = "upsc_ssc_140x60",
                    title = "UPSC / SSC / IBPS (140×60 px)",
                    authority = "Civil Services & Banking",
                    widthPx = 140,
                    heightPx = 60,
                    widthCm = 3.5f,
                    heightCm = 1.5f,
                    dpi = 200,
                    maxFileSizeKb = 20,
                    minFileSizeKb = 10
                ),
                SignaturePreset(
                    id = "neet_jee_gate_280x80",
                    title = "NEET / JEE / GATE (280×80 px)",
                    authority = "NTA / Exam Portals",
                    widthPx = 280,
                    heightPx = 80,
                    widthCm = 3.5f,
                    heightCm = 1.0f,
                    dpi = 200,
                    maxFileSizeKb = 30,
                    minFileSizeKb = 4
                ),
                SignaturePreset(
                    id = "pan_nsdl_354x157",
                    title = "PAN Card NSDL (4.5×2.0 cm)",
                    authority = "Income Tax / NSDL",
                    widthPx = 354,
                    heightPx = 157,
                    widthCm = 4.5f,
                    heightCm = 2.0f,
                    dpi = 200,
                    maxFileSizeKb = 50,
                    minFileSizeKb = 10
                ),
                SignaturePreset(
                    id = "high_res_600x200",
                    title = "High-Res Document (600×200 px)",
                    authority = "PDF & Contract Signing",
                    widthPx = 600,
                    heightPx = 200,
                    widthCm = 5.08f,
                    heightCm = 1.69f,
                    dpi = 300,
                    maxFileSizeKb = 100,
                    minFileSizeKb = null,
                    recommendedBg = SignatureBackgroundType.TRANSPARENT
                )
            )
        }
    }

    data class InkBoundsResult(
        val bounds: Rect,
        val hasInk: Boolean,
        val inkPixelCount: Int,
        val totalPixelCount: Int
    ) {
        val inkCoveragePercent: Float
            get() = if (totalPixelCount > 0) (inkPixelCount.toFloat() / totalPixelCount.toFloat()) * 100f else 0f
    }

    data class SignatureVerificationReport(
        val widthPx: Int,
        val heightPx: Int,
        val dpi: Int,
        val physicalWidthMm: Float,
        val physicalHeightMm: Float,
        val inkCoveragePercent: Float,
        val backgroundType: SignatureBackgroundType,
        val hasTransparency: Boolean,
        val exportFormat: ExportFormat,
        val detectedMimeType: String,
        val fileSizeBytes: Int,
        val maxLimitKb: Int?,
        val meetsSizeRequirement: Boolean,
        val isVerified: Boolean
    )

    /**
     * Scans bitmap to detect precise bounding rectangle of ink strokes.
     */
    fun findInkBounds(
        source: Bitmap,
        threshold: Int = 200,
        isTransparent: Boolean = false
    ): InkBoundsResult {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        var inkCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val alpha = Color.alpha(pixel)

                val isInkPixel = if (isTransparent) {
                    alpha > 35
                } else {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    alpha > 35 && lum < threshold
                }

                if (isInkPixel) {
                    inkCount++
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val hasInk = inkCount > 0 && maxX >= minX && maxY >= minY
        val bounds = if (hasInk) {
            Rect(minX, minY, maxX + 1, maxY + 1)
        } else {
            Rect(0, 0, width, height)
        }

        return InkBoundsResult(
            bounds = bounds,
            hasInk = hasInk,
            inkPixelCount = inkCount,
            totalPixelCount = width * height
        )
    }

    /**
     * Removes unnecessary empty space/margins around signature ink strokes.
     */
    fun autoTrimEmptyArea(
        source: Bitmap,
        threshold: Int = 200,
        paddingPx: Int = 16,
        isTransparent: Boolean = false
    ): Bitmap {
        val detection = findInkBounds(source, threshold, isTransparent)
        if (!detection.hasInk) return source

        val bounds = detection.bounds
        val trimLeft = max(0, bounds.left - paddingPx)
        val trimTop = max(0, bounds.top - paddingPx)
        val trimRight = min(source.width, bounds.right + paddingPx)
        val trimBottom = min(source.height, bounds.bottom + paddingPx)

        val trimW = max(1, trimRight - trimLeft)
        val trimH = max(1, trimBottom - trimTop)

        return Bitmap.createBitmap(source, trimLeft, trimTop, trimW, trimH)
    }

    /**
     * Applies brightness, contrast, grayscale, or clean B&W/Colored ink thresholding,
     * plus optional stroke weight adjustment (morphological ink thickening/thinning).
     */
    fun processInkAndColor(
        source: Bitmap,
        brightness: Float = 0f, // -100 to +100
        contrast: Float = 0f,   // -100 to +100
        inkMode: SignatureInkMode = SignatureInkMode.BLACK_AND_WHITE,
        paperThreshold: Int = 190, // 80 to 240
        outputInkColor: OutputInkColor = OutputInkColor.PURE_BLACK,
        makeTransparent: Boolean = false,
        backgroundColor: Int = Color.WHITE,
        strokeWeightDelta: Int = 0 // -2 (thinner) to +3 (bolder)
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val contrastFactor = if (contrast != 0f) {
            val c = contrast / 100f
            (1f + c).coerceIn(0.1f, 3.0f)
        } else 1.0f

        val brightnessOffset = (brightness * 2.55f).toInt()

        val targetInkColor = when (inkMode) {
            SignatureInkMode.ROYAL_BLUE -> OutputInkColor.ROYAL_BLUE.hexColor
            SignatureInkMode.NAVY_INK -> OutputInkColor.NAVY_INK.hexColor
            SignatureInkMode.CRIMSON_STAMP -> OutputInkColor.CRIMSON_STAMP.hexColor
            SignatureInkMode.BLACK_AND_WHITE -> outputInkColor.hexColor
            else -> 0
        }

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val alpha = Color.alpha(pixel)

            // If already completely transparent
            if (alpha < 20) {
                pixels[i] = if (makeTransparent) Color.TRANSPARENT else backgroundColor
                continue
            }

            var r = Color.red(pixel)
            var g = Color.green(pixel)
            var b = Color.blue(pixel)

            // Apply Brightness & Contrast adjustment
            if (brightnessOffset != 0 || contrastFactor != 1.0f) {
                r = (((r - 128) * contrastFactor) + 128 + brightnessOffset).roundToInt().coerceIn(0, 255)
                g = (((g - 128) * contrastFactor) + 128 + brightnessOffset).roundToInt().coerceIn(0, 255)
                b = (((b - 128) * contrastFactor) + 128 + brightnessOffset).roundToInt().coerceIn(0, 255)
            }

            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

            when (inkMode) {
                SignatureInkMode.BLACK_AND_WHITE,
                SignatureInkMode.ROYAL_BLUE,
                SignatureInkMode.NAVY_INK,
                SignatureInkMode.CRIMSON_STAMP -> {
                    if (lum > paperThreshold) {
                        // Paper background
                        pixels[i] = if (makeTransparent) Color.TRANSPARENT else backgroundColor
                    } else {
                        // Ink pixel with antialiasing
                        val inkDarkness = (1f - (lum.toFloat() / max(1, paperThreshold))).coerceIn(0.35f, 1f)
                        val inkAlpha = (255 * inkDarkness).toInt().coerceIn(100, 255)

                        val inkHex = if (targetInkColor != 0) targetInkColor else OutputInkColor.PURE_BLACK.hexColor
                        val ir = Color.red(inkHex)
                        val ig = Color.green(inkHex)
                        val ib = Color.blue(inkHex)

                        if (makeTransparent) {
                            pixels[i] = Color.argb(inkAlpha, ir, ig, ib)
                        } else {
                            // Blend ink over background color
                            val bgR = Color.red(backgroundColor)
                            val bgG = Color.green(backgroundColor)
                            val bgB = Color.blue(backgroundColor)
                            val blendFactor = inkAlpha / 255f
                            val finalR = (ir * blendFactor + bgR * (1f - blendFactor)).roundToInt().coerceIn(0, 255)
                            val finalG = (ig * blendFactor + bgG * (1f - blendFactor)).roundToInt().coerceIn(0, 255)
                            val finalB = (ib * blendFactor + bgB * (1f - blendFactor)).roundToInt().coerceIn(0, 255)
                            pixels[i] = Color.rgb(finalR, finalG, finalB)
                        }
                    }
                }
                SignatureInkMode.GRAYSCALE -> {
                    if (makeTransparent && lum > paperThreshold) {
                        pixels[i] = Color.TRANSPARENT
                    } else {
                        pixels[i] = Color.argb(alpha, lum, lum, lum)
                    }
                }
                SignatureInkMode.ORIGINAL -> {
                    if (makeTransparent && lum > paperThreshold) {
                        pixels[i] = Color.TRANSPARENT
                    } else {
                        pixels[i] = Color.argb(alpha, r, g, b)
                    }
                }
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)

        return if (strokeWeightDelta != 0) {
            adjustStrokeWeight(
                source = output,
                strokeWeightDelta = strokeWeightDelta,
                makeTransparent = makeTransparent,
                backgroundColor = backgroundColor
            )
        } else {
            output
        }
    }

    /**
     * Thickens (positive delta) or thins (negative delta) signature ink strokes
     * so small portal signatures (e.g. 140x60 px) remain bold and legible.
     */
    fun adjustStrokeWeight(
        source: Bitmap,
        strokeWeightDelta: Int,
        makeTransparent: Boolean = false,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val clamped = strokeWeightDelta.coerceIn(-2, 3)
        if (clamped == 0) return source
        val w = source.width
        val h = source.height
        if (w < 4 || h < 4) return source

        val srcPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val outPixels = srcPixels.copyOf()

        val radius = kotlin.math.abs(clamped)
        val bgHex = if (makeTransparent) Color.TRANSPARENT else backgroundColor

        fun isInk(px: Int): Boolean {
            val a = Color.alpha(px)
            if (makeTransparent) return a > 50
            val dr = kotlin.math.abs(Color.red(px) - Color.red(backgroundColor))
            val dg = kotlin.math.abs(Color.green(px) - Color.green(backgroundColor))
            val db = kotlin.math.abs(Color.blue(px) - Color.blue(backgroundColor))
            return (dr + dg + db) > 45
        }

        if (clamped > 0) {
            // Dilation: expand ink pixels into neighboring background pixels
            for (y in radius until h - radius) {
                for (x in radius until w - radius) {
                    val idx = y * w + x
                    if (!isInk(srcPixels[idx])) {
                        var neighborInkColor: Int? = null
                        for (dy in -radius..radius) {
                            for (dx in -radius..radius) {
                                if (dx * dx + dy * dy <= radius * radius) {
                                    val np = srcPixels[(y + dy) * w + (x + dx)]
                                    if (isInk(np)) {
                                        neighborInkColor = np
                                        break
                                    }
                                }
                            }
                            if (neighborInkColor != null) break
                        }
                        if (neighborInkColor != null) {
                            outPixels[idx] = neighborInkColor
                        }
                    }
                }
            }
        } else {
            // Erosion: thin ink pixels that touch background
            for (y in radius until h - radius) {
                for (x in radius until w - radius) {
                    val idx = y * w + x
                    if (isInk(srcPixels[idx])) {
                        var touchesBg = false
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                if (!isInk(srcPixels[(y + dy) * w + (x + dx)])) {
                                    touchesBg = true
                                    break
                                }
                            }
                            if (touchesBg) break
                        }
                        if (touchesBg) {
                            outPixels[idx] = bgHex
                        }
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Renders signature into the specified target dimensions (W × H) using the chosen fit strategy,
     * background color, and alignment.
     */
    fun fitSignatureToTargetBox(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        fitMode: SignatureFitMode,
        backgroundType: SignatureBackgroundType,
        customBackgroundColor: Int = Color.WHITE,
        center: Boolean = true,
        marginPaddingPx: Int = 16
    ): Bitmap {
        val safeW = max(10, targetWidth)
        val safeH = max(10, targetHeight)

        val resolvedBgColor = when (backgroundType) {
            SignatureBackgroundType.WHITE -> Color.WHITE
            SignatureBackgroundType.CUSTOM -> customBackgroundColor
            SignatureBackgroundType.TRANSPARENT -> Color.TRANSPARENT
        }

        // STRETCH: Force exact width and height independently
        if (fitMode == SignatureFitMode.STRETCH) {
            val stretched = Bitmap.createScaledBitmap(source, safeW, safeH, true)
            if (backgroundType == SignatureBackgroundType.TRANSPARENT) {
                return stretched
            }
            val canvasBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawColor(resolvedBgColor)
            canvas.drawBitmap(stretched, 0f, 0f, null)
            return canvasBitmap
        }

        // SMART CROP: Crop unnecessary whitespace while preserving the signature
        val effectiveSource = if (fitMode == SignatureFitMode.SMART_CROP) {
            val isAlpha = backgroundType == SignatureBackgroundType.TRANSPARENT
            val inkDetection = findInkBounds(source, threshold = 200, isTransparent = isAlpha)
            if (inkDetection.hasInk) {
                val b = inkDetection.bounds
                val pad = max(0, marginPaddingPx)
                val left = max(0, b.left - pad)
                val top = max(0, b.top - pad)
                val right = min(source.width, b.right + pad)
                val bottom = min(source.height, b.bottom + pad)
                val w = max(1, right - left)
                val h = max(1, bottom - top)
                Bitmap.createBitmap(source, left, top, w, h)
            } else {
                source
            }
        } else {
            source
        }

        // Target Canvas Setup with deterministic pixel compositing
        val outPixels = IntArray(safeW * safeH) { resolvedBgColor }

        val srcW = effectiveSource.width.toFloat().coerceAtLeast(1f)
        val srcH = effectiveSource.height.toFloat().coerceAtLeast(1f)

        val scale = when (fitMode) {
            SignatureFitMode.FIT, SignatureFitMode.SMART_CROP -> min(safeW / srcW, safeH / srcH)
            SignatureFitMode.FILL -> max(safeW / srcW, safeH / srcH)
            SignatureFitMode.STRETCH -> 1f
        }

        val destW = (srcW * scale).roundToInt().coerceAtLeast(1)
        val destH = (srcH * scale).roundToInt().coerceAtLeast(1)
        val scaledSig = BitmapUtils.resizeBitmap(effectiveSource, destW, destH)
        val scaledPixels = IntArray(destW * destH)
        scaledSig.getPixels(scaledPixels, 0, destW, 0, 0, destW, destH)

        val offsetX = if (center) ((safeW - destW) / 2f).roundToInt() else 0
        val offsetY = if (center) ((safeH - destH) / 2f).roundToInt() else 0

        val bgR = Color.red(resolvedBgColor)
        val bgG = Color.green(resolvedBgColor)
        val bgB = Color.blue(resolvedBgColor)

        for (sy in 0 until destH) {
            val ty = offsetY + sy
            if (ty !in 0 until safeH) continue
            for (sx in 0 until destW) {
                val tx = offsetX + sx
                if (tx !in 0 until safeW) continue
                val fg = scaledPixels[sy * destW + sx]
                val fgA = Color.alpha(fg)
                if (backgroundType == SignatureBackgroundType.TRANSPARENT) {
                    outPixels[ty * safeW + tx] = fg
                } else if (fgA >= 250) {
                    outPixels[ty * safeW + tx] = Color.rgb(Color.red(fg), Color.green(fg), Color.blue(fg))
                } else if (fgA > 0) {
                    val alpha = fgA / 255f
                    val r = (Color.red(fg) * alpha + bgR * (1f - alpha)).roundToInt().coerceIn(0, 255)
                    val g = (Color.green(fg) * alpha + bgG * (1f - alpha)).roundToInt().coerceIn(0, 255)
                    val b = (Color.blue(fg) * alpha + bgB * (1f - alpha)).roundToInt().coerceIn(0, 255)
                    outPixels[ty * safeW + tx] = Color.rgb(r, g, b)
                }
            }
        }

        val canvasBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        canvasBitmap.setPixels(outPixels, 0, safeW, 0, 0, safeW, safeH)
        return canvasBitmap
    }

    /**
     * Verifies a processed signature bitmap and its encoded output against portal/document requirements.
     */
    fun verifySignatureOutput(
        bitmap: Bitmap,
        encodedBytes: ByteArray,
        dpi: Int,
        backgroundType: SignatureBackgroundType,
        exportFormat: ExportFormat,
        maxLimitKb: Int? = null
    ): SignatureVerificationReport {
        val isTransparent = backgroundType == SignatureBackgroundType.TRANSPARENT &&
                (exportFormat == ExportFormat.PNG || exportFormat == ExportFormat.WEBP_LOSSY || exportFormat == ExportFormat.WEBP_LOSSLESS)
        val inkStats = findInkBounds(bitmap, threshold = 210, isTransparent = isTransparent)
        val detectedMime = FormatConversionEngine.detectMimeTypeFromBytes(encodedBytes)
        val phys = DpiPrintEngine.computePhysicalDimensionsFromPixels(bitmap.width, bitmap.height, dpi)
        val meetsSize = maxLimitKb == null || encodedBytes.size <= (maxLimitKb * 1024)
        val mimeValid = detectedMime == exportFormat.mimeType

        return SignatureVerificationReport(
            widthPx = bitmap.width,
            heightPx = bitmap.height,
            dpi = dpi,
            physicalWidthMm = phys.widthMm,
            physicalHeightMm = phys.heightMm,
            inkCoveragePercent = inkStats.inkCoveragePercent,
            backgroundType = backgroundType,
            hasTransparency = isTransparent && BitmapUtils.hasTransparency(bitmap),
            exportFormat = exportFormat,
            detectedMimeType = detectedMime,
            fileSizeBytes = encodedBytes.size,
            maxLimitKb = maxLimitKb,
            meetsSizeRequirement = meetsSize,
            isVerified = encodedBytes.isNotEmpty() && meetsSize && mimeValid && inkStats.hasInk
        )
    }

    /**
     * Legacy helper kept for backward compatibility with other callers.
     */
    fun prepareSignature(
        source: Bitmap,
        threshold: Int = 185,
        makeTransparent: Boolean = false,
        inkColor: OutputInkColor = OutputInkColor.PURE_BLACK,
        autoTrim: Boolean = true,
        paddingPx: Int = 16,
        strokeWeightDelta: Int = 0
    ): Bitmap {
        val cleaned = processInkAndColor(
            source = source,
            inkMode = when (inkColor) {
                OutputInkColor.ROYAL_BLUE -> SignatureInkMode.ROYAL_BLUE
                OutputInkColor.NAVY_INK -> SignatureInkMode.NAVY_INK
                OutputInkColor.CRIMSON_STAMP -> SignatureInkMode.CRIMSON_STAMP
                else -> SignatureInkMode.BLACK_AND_WHITE
            },
            paperThreshold = threshold,
            outputInkColor = inkColor,
            makeTransparent = makeTransparent,
            strokeWeightDelta = strokeWeightDelta
        )
        return if (autoTrim) {
            autoTrimEmptyArea(cleaned, threshold = threshold, paddingPx = paddingPx, isTransparent = makeTransparent)
        } else {
            cleaned
        }
    }
}
