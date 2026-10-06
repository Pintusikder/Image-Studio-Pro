package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.CropAspectRatio
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class AspectRatioPreset(
    val id: String,
    val name: String,
    val ratioX: Float,
    val ratioY: Float,
    val category: String,
    val description: String,
    val standardResolutions: List<StandardResolution> = emptyList()
) {
    val decimalRatio: Float get() = if (ratioY != 0f) ratioX / ratioY else 1f

    val displayRatio: String
        get() = if (ratioX % 1f == 0f && ratioY % 1f == 0f) {
            "${ratioX.toInt()}:${ratioY.toInt()}"
        } else {
            String.format("%.2f:1", decimalRatio)
        }

    fun toCropAspectRatio(): CropAspectRatio {
        return CropAspectRatio(name, ratioX, ratioY)
    }
}

data class StandardResolution(
    val label: String,
    val width: Int,
    val height: Int
)

enum class AspectOrientation(val label: String) {
    LANDSCAPE("Landscape (Horizontal)"),
    PORTRAIT("Portrait (Vertical)"),
    SQUARE("Square (1:1)")
}

data class RatioAnalysis(
    val width: Int,
    val height: Int,
    val gcd: Long,
    val simplifiedX: Long,
    val simplifiedY: Long,
    val simplifiedRatioString: String,
    val decimalRatio: Float,
    val orientation: AspectOrientation,
    val megapixels: Float,
    val nearestPreset: AspectRatioPreset?,
    val variancePercent: Float,
    val widthInchesAt300Dpi: Float,
    val heightInchesAt300Dpi: Float,
    val widthMmAt300Dpi: Float,
    val heightMmAt300Dpi: Float
)

enum class AspectRatioApplyMode(val label: String, val description: String) {
    CROP("Center Crop", "Crops outer edges to match target ratio exactly"),
    INTERACTIVE_CROP("Interactive Framing", "Move and scale crop window manually"),
    PAD_LETTERBOX("Fit & Pad (No Cropping)", "Expands canvas with background fill"),
    RESIZE_SCALE("Scale / Resize", "Resizes image directly to target dimensions")
}

object AspectRatioEngine {

    val PRESET_FREE = AspectRatioPreset(
        id = "free",
        name = "Free",
        ratioX = 1f,
        ratioY = 1f,
        category = "Popular",
        description = "Unconstrained aspect ratio"
    )

    val PRESET_1_1 = AspectRatioPreset(
        id = "1_1",
        name = "1:1 Square",
        ratioX = 1f,
        ratioY = 1f,
        category = "Popular",
        description = "Instagram Post, Profile Photo, Avatar, App Icon",
        standardResolutions = listOf(
            StandardResolution("500×500 (Avatar)", 500, 500),
            StandardResolution("1080×1080 (IG Square Post)", 1080, 1080),
            StandardResolution("1200×1200 (HD Square)", 1200, 1200),
            StandardResolution("2048×2048 (Hi-Res Square)", 2048, 2048)
        )
    )

    val PRESET_16_9 = AspectRatioPreset(
        id = "16_9",
        name = "16:9 Widescreen",
        ratioX = 16f,
        ratioY = 9f,
        category = "Popular",
        description = "YouTube Video/Thumbnail, TV, Monitors, Presentation Slides",
        standardResolutions = listOf(
            StandardResolution("1280×720 (720p HD)", 1280, 720),
            StandardResolution("1920×1080 (1080p Full HD)", 1920, 1080),
            StandardResolution("2560×1440 (1440p 2K QHD)", 2560, 1440),
            StandardResolution("3840×2160 (2160p 4K UHD)", 3840, 2160)
        )
    )

    val PRESET_9_16 = AspectRatioPreset(
        id = "9_16",
        name = "9:16 Story / Reels",
        ratioX = 9f,
        ratioY = 16f,
        category = "Social",
        description = "Instagram Story/Reel, TikTok, YouTube Shorts, Snapchat",
        standardResolutions = listOf(
            StandardResolution("720×1280 (720p Vertical)", 720, 1280),
            StandardResolution("1080×1920 (1080p Story/Reels)", 1080, 1920),
            StandardResolution("1440×2560 (2K Vertical)", 1440, 2560)
        )
    )

    val PRESET_4_5 = AspectRatioPreset(
        id = "4_5",
        name = "4:5 IG Portrait",
        ratioX = 4f,
        ratioY = 5f,
        category = "Social",
        description = "Instagram Portrait Feed Post (maximum screen estate)",
        standardResolutions = listOf(
            StandardResolution("1080×1350 (IG Recommended)", 1080, 1350),
            StandardResolution("1440×1800 (HD Portrait)", 1440, 1800)
        )
    )

    val PRESET_3_2 = AspectRatioPreset(
        id = "3_2",
        name = "3:2 Classic 35mm",
        ratioX = 3f,
        ratioY = 2f,
        category = "Photo & Print",
        description = "Classic 35mm Film, DSLR & Mirrorless Standard, 4×6 in Print",
        standardResolutions = listOf(
            StandardResolution("1080×720 (Web 3:2)", 1080, 720),
            StandardResolution("1800×1200 (4×6 in @ 300 DPI)", 1800, 1200),
            StandardResolution("3000×2000 (HD 3:2)", 3000, 2000)
        )
    )

    val PRESET_2_3 = AspectRatioPreset(
        id = "2_3",
        name = "2:3 Vertical 35mm",
        ratioX = 2f,
        ratioY = 3f,
        category = "Photo & Print",
        description = "Vertical DSLR Portrait, 4×6 in Vertical Print",
        standardResolutions = listOf(
            StandardResolution("720×1080 (Web 2:3)", 720, 1080),
            StandardResolution("1200×1800 (4×6 in @ 300 DPI)", 1200, 1800),
            StandardResolution("2000×3000 (HD 2:3)", 2000, 3000)
        )
    )

    val PRESET_4_3 = AspectRatioPreset(
        id = "4_3",
        name = "4:3 Standard Display",
        ratioX = 4f,
        ratioY = 3f,
        category = "Photo & Print",
        description = "Micro 4/3 Cameras, iPad Screens, Classic CRT Television",
        standardResolutions = listOf(
            StandardResolution("1024×768 (XGA)", 1024, 768),
            StandardResolution("1600×1200 (UXGA)", 1600, 1200),
            StandardResolution("2048×1536 (QXGA)", 2048, 1536)
        )
    )

    val PRESET_3_4 = AspectRatioPreset(
        id = "3_4",
        name = "3:4 Vertical Standard",
        ratioX = 3f,
        ratioY = 4f,
        category = "Photo & Print",
        description = "Vertical iPad Display, Tablet Photo, Document Portrait",
        standardResolutions = listOf(
            StandardResolution("768×1024 (Vertical XGA)", 768, 1024),
            StandardResolution("1200×1600 (UXGA)", 1200, 1600),
            StandardResolution("1536×2048 (QXGA)", 1536, 2048)
        )
    )

    val PRESET_21_9 = AspectRatioPreset(
        id = "21_9",
        name = "21:9 Ultrawide Cinema",
        ratioX = 21f,
        ratioY = 9f,
        category = "Display & Cinema",
        description = "Cinemascope Anamorphic (2.33:1), Ultrawide Gaming Monitors",
        standardResolutions = listOf(
            StandardResolution("2560×1080 (FHD Ultrawide)", 2560, 1080),
            StandardResolution("3440×1440 (QHD Ultrawide)", 3440, 1440),
            StandardResolution("5120×2160 (5K2K Ultrawide)", 5120, 2160)
        )
    )

    val PRESET_16_10 = AspectRatioPreset(
        id = "16_10",
        name = "16:10 Laptop & Mac",
        ratioX = 16f,
        ratioY = 10f,
        category = "Display & Cinema",
        description = "Apple MacBook, Modern 16:10 Laptops & Productivity Monitors",
        standardResolutions = listOf(
            StandardResolution("1920×1200 (WUXGA)", 1920, 1200),
            StandardResolution("2560×1600 (WQXGA)", 2560, 1600)
        )
    )

    val PRESET_2_1 = AspectRatioPreset(
        id = "2_1",
        name = "2:1 (18:9) Modern Phone",
        ratioX = 2f,
        ratioY = 1f,
        category = "Display & Cinema",
        description = "Univisium, Twitter/X Header, Modern Smartphone Displays",
        standardResolutions = listOf(
            StandardResolution("1200×600 (Twitter Post)", 1200, 600),
            StandardResolution("2160×1080 (FHD+ 18:9)", 2160, 1080)
        )
    )

    val PRESET_1_91_1 = AspectRatioPreset(
        id = "1_91_1",
        name = "1.91:1 Social Link Preview",
        ratioX = 1.91f,
        ratioY = 1f,
        category = "Social",
        description = "Facebook Open Graph, LinkedIn Link Share, Twitter Card",
        standardResolutions = listOf(
            StandardResolution("1200×628 (Standard Social Link)", 1200, 628)
        )
    )

    val PRESET_ISO_A = AspectRatioPreset(
        id = "iso_a",
        name = "1:1.414 (ISO A4 / A5)",
        ratioX = 1f,
        ratioY = 1.4142f,
        category = "Photo & Print",
        description = "Lichtenberg Ratio (1:√2), International Paper Standard (A3, A4, A5)",
        standardResolutions = listOf(
            StandardResolution("2480×3508 (A4 @ 300 DPI)", 2480, 3508),
            StandardResolution("1748×2480 (A5 @ 300 DPI)", 1748, 2480)
        )
    )

    val PRESET_5_7 = AspectRatioPreset(
        id = "5_7",
        name = "5:7 Print",
        ratioX = 5f,
        ratioY = 7f,
        category = "Photo & Print",
        description = "Standard 5×7 in Photographic Print, Greeting Cards",
        standardResolutions = listOf(
            StandardResolution("1500×2100 (5×7 in @ 300 DPI)", 1500, 2100)
        )
    )

    val PRESET_GOLDEN = AspectRatioPreset(
        id = "golden",
        name = "1.618:1 Golden Ratio (φ)",
        ratioX = 1.618f,
        ratioY = 1f,
        category = "Aesthetic",
        description = "Divine Proportion (Golden Rectangle), naturally pleasing composition",
        standardResolutions = listOf(
            StandardResolution("1618×1000 (Golden 1K)", 1618, 1000),
            StandardResolution("1942×1200 (Golden HD)", 1942, 1200)
        )
    )

    val ALL_PRESETS = listOf(
        PRESET_1_1,
        PRESET_16_9,
        PRESET_9_16,
        PRESET_4_5,
        PRESET_3_2,
        PRESET_2_3,
        PRESET_4_3,
        PRESET_3_4,
        PRESET_21_9,
        PRESET_16_10,
        PRESET_2_1,
        PRESET_1_91_1,
        PRESET_ISO_A,
        PRESET_5_7,
        PRESET_GOLDEN
    )

    val CATEGORIES = listOf("All", "Popular", "Social", "Photo & Print", "Display & Cinema")

    fun gcd(a: Long, b: Long): Long {
        var num1 = abs(a)
        var num2 = abs(b)
        while (num2 != 0L) {
            val temp = num2
            num2 = num1 % num2
            num1 = temp
        }
        return if (num1 == 0L) 1L else num1
    }

    fun analyzeAspectRatio(width: Int, height: Int, dpi: Int = 300): RatioAnalysis {
        val safeW = max(1, width)
        val safeH = max(1, height)
        val g = gcd(safeW.toLong(), safeH.toLong())
        val simpX = safeW / g
        val simpY = safeH / g

        val decimal = safeW.toFloat() / safeH.toFloat()
        val orientation = when {
            safeW > safeH -> AspectOrientation.LANDSCAPE
            safeH > safeW -> AspectOrientation.PORTRAIT
            else -> AspectOrientation.SQUARE
        }
        val mp = (safeW.toLong() * safeH.toLong()) / 1_000_000f

        // Search closest preset
        var closest: AspectRatioPreset? = null
        var minDiff = Float.MAX_VALUE
        for (preset in ALL_PRESETS) {
            val diff = abs(decimal - preset.decimalRatio)
            if (diff < minDiff) {
                minDiff = diff
                closest = preset
            }
        }
        val variancePercent = if (closest != null && closest.decimalRatio > 0f) {
            (abs(decimal - closest.decimalRatio) / closest.decimalRatio) * 100f
        } else 0f

        // Simplified ratio string: if numbers are reasonable (< 100), show exact fraction, else show decimal
        val simplifiedString = if (simpX < 100 && simpY < 100) {
            "$simpX:$simpY"
        } else if (variancePercent < 0.8f && closest != null) {
            "${closest.displayRatio} (exact $simpX:$simpY)"
        } else {
            String.format("%.3f:1", decimal)
        }

        val safeDpi = max(1, dpi).toFloat()
        val wInches = safeW / safeDpi
        val hInches = safeH / safeDpi

        return RatioAnalysis(
            width = safeW,
            height = safeH,
            gcd = g,
            simplifiedX = simpX,
            simplifiedY = simpY,
            simplifiedRatioString = simplifiedString,
            decimalRatio = decimal,
            orientation = orientation,
            megapixels = mp,
            nearestPreset = closest,
            variancePercent = variancePercent,
            widthInchesAt300Dpi = wInches,
            heightInchesAt300Dpi = hInches,
            widthMmAt300Dpi = wInches * 25.4f,
            heightMmAt300Dpi = hInches * 25.4f
        )
    }

    fun calculateHeightFromWidth(width: Int, ratioX: Float, ratioY: Float): Int {
        if (ratioX <= 0f) return width
        return ((width.toFloat() * ratioY) / ratioX).roundToInt().coerceAtLeast(1)
    }

    fun calculateWidthFromHeight(height: Int, ratioX: Float, ratioY: Float): Int {
        if (ratioY <= 0f) return height
        return ((height.toFloat() * ratioX) / ratioY).roundToInt().coerceAtLeast(1)
    }

    fun cropToAspectRatio(bitmap: Bitmap, ratioX: Float, ratioY: Float): Bitmap {
        if (ratioX <= 0f || ratioY <= 0f) return bitmap
        val targetRatio = ratioX / ratioY
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        return if (abs(currentRatio - targetRatio) < 0.001f) {
            bitmap
        } else if (currentRatio > targetRatio) {
            // Image is wider than target ratio: crop left & right
            val cropWidth = (bitmap.height * targetRatio).roundToInt().coerceIn(1, bitmap.width)
            val cropX = (bitmap.width - cropWidth) / 2
            Bitmap.createBitmap(bitmap, cropX, 0, cropWidth, bitmap.height)
        } else {
            // Image is taller than target ratio: crop top & bottom
            val cropHeight = (bitmap.width / targetRatio).roundToInt().coerceIn(1, bitmap.height)
            val cropY = (bitmap.height - cropHeight) / 2
            Bitmap.createBitmap(bitmap, 0, cropY, bitmap.width, cropHeight)
        }
    }

    fun padToAspectRatio(bitmap: Bitmap, ratioX: Float, ratioY: Float, padColor: Int = Color.WHITE): Bitmap {
        if (ratioX <= 0f || ratioY <= 0f) return bitmap
        val targetRatio = ratioX / ratioY
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        if (abs(currentRatio - targetRatio) < 0.001f) return bitmap

        val newWidth: Int
        val newHeight: Int

        if (currentRatio > targetRatio) {
            // Need vertical padding
            newWidth = bitmap.width
            newHeight = (bitmap.width / targetRatio).roundToInt().coerceAtLeast(bitmap.height)
        } else {
            // Need horizontal padding
            newHeight = bitmap.height
            newWidth = (bitmap.height * targetRatio).roundToInt().coerceAtLeast(bitmap.width)
        }

        val outBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        if (padColor != Color.TRANSPARENT) {
            canvas.drawColor(padColor)
        }

        val left = (newWidth - bitmap.width) / 2f
        val top = (newHeight - bitmap.height) / 2f
        canvas.drawBitmap(bitmap, left, top, null)
        return outBitmap
    }

    fun scaleToDimensions(bitmap: Bitmap, targetWidth: Int, targetHeight: Int, filter: Boolean = true): Bitmap {
        val safeW = max(1, targetWidth)
        val safeH = max(1, targetHeight)
        return Bitmap.createScaledBitmap(bitmap, safeW, safeH, filter)
    }
}
