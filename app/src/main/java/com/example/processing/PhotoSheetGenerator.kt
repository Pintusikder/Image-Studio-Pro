package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.model.CutLineStyle
import com.example.model.PhotoSheetRotation
import com.example.model.SheetAlignment
import com.example.model.SheetOrientation
import com.example.model.SheetPaperPreset
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object PhotoSheetGenerator {

    data class SheetConfig(
        val paperPreset: SheetPaperPreset = SheetPaperPreset.A4,
        val customWidthInches: Float = 8.27f,
        val customHeightInches: Float = 11.69f,
        val orientation: SheetOrientation = SheetOrientation.PORTRAIT,
        val copies: Int = 6,
        val customCopiesInput: String = "6",
        val columns: Int? = null,
        val rows: Int? = null,
        val marginMm: Float = 10f,
        val spacingMm: Float = 4f,
        val alignment: SheetAlignment = SheetAlignment.CENTER,
        val rotation: PhotoSheetRotation = PhotoSheetRotation.ROT_0,
        val cutLineStyle: CutLineStyle = CutLineStyle.CORNER_TICKS,
        val dpi: Int = 300,
        val backgroundColor: Int = Color.WHITE,
        val useExactPhotoPhysicalSize: Boolean = false,
        val targetPhotoWidthMm: Float = 35f,
        val targetPhotoHeightMm: Float = 45f,
        val autoFillMaxCopies: Boolean = false,
        val showCalibrationFooter: Boolean = false
    )

    data class SheetResult(
        val bitmap: Bitmap,
        val totalCopies: Int,
        val rows: Int,
        val cols: Int,
        val paperPreset: SheetPaperPreset,
        val paperWidthMm: Float,
        val paperHeightMm: Float,
        val photoWidthMm: Float,
        val photoHeightMm: Float,
        val marginMm: Float,
        val spacingMm: Float,
        val dpi: Int,
        val orientation: SheetOrientation,
        val maxPossibleCopiesOnPage: Int = totalCopies,
        val paperUtilizationPercent: Float = 0f,
        val placedPhotoRectsPx: List<Rect> = emptyList()
    )

    /**
     * Computes how many copies of a photo with exact physical size (photoWidthMm × photoHeightMm)
     * can fit onto the specified paper without clipping.
     * Returns Triple(maxCopies, cols, rows).
     */
    fun calculateMaxCopiesForPhysicalPhoto(
        paperPreset: SheetPaperPreset,
        orientation: SheetOrientation = SheetOrientation.PORTRAIT,
        customWidthInches: Float = 8.27f,
        customHeightInches: Float = 11.69f,
        marginMm: Float = 10f,
        spacingMm: Float = 4f,
        photoWidthMm: Float = 35f,
        photoHeightMm: Float = 45f,
        rotation: PhotoSheetRotation = PhotoSheetRotation.ROT_0
    ): Triple<Int, Int, Int> {
        val baseWIn = if (paperPreset.isCustom) customWidthInches else paperPreset.widthInches
        val baseHIn = if (paperPreset.isCustom) customHeightInches else paperPreset.heightInches
        val (paperWIn, paperHIn) = when (orientation) {
            SheetOrientation.PORTRAIT -> min(baseWIn, baseHIn) to max(baseWIn, baseHIn)
            SheetOrientation.LANDSCAPE -> max(baseWIn, baseHIn) to min(baseWIn, baseHIn)
        }
        val paperWMm = paperWIn * 25.4f
        val paperHMm = paperHIn * 25.4f
        val usableWMm = max(10f, paperWMm - marginMm * 2f)
        val usableHMm = max(10f, paperHMm - marginMm * 2f)

        val (effPhotoWMm, effPhotoHMm) = if (rotation == PhotoSheetRotation.ROT_90 || rotation == PhotoSheetRotation.ROT_270) {
            max(5f, photoHeightMm) to max(5f, photoWidthMm)
        } else {
            max(5f, photoWidthMm) to max(5f, photoHeightMm)
        }

        val cols = max(1, ((usableWMm + spacingMm) / (effPhotoWMm + spacingMm)).toInt())
        val rows = max(1, ((usableHMm + spacingMm) / (effPhotoHMm + spacingMm)).toInt())
        return Triple(cols * rows, cols, rows)
    }

    fun createPhotoSheet(
        photo: Bitmap,
        paperPreset: SheetPaperPreset,
        columns: Int = paperPreset.defaultCols,
        rows: Int = paperPreset.defaultRows,
        dpi: Int = 300,
        cutLineStyle: CutLineStyle = CutLineStyle.SOLID,
        maxCopies: Int? = null,
        backgroundColor: Int = Color.WHITE
    ): SheetResult {
        return createPhotoSheet(
            photo = photo,
            config = SheetConfig(
                paperPreset = paperPreset,
                columns = columns,
                rows = rows,
                dpi = dpi,
                cutLineStyle = cutLineStyle,
                copies = maxCopies ?: (columns * rows),
                backgroundColor = backgroundColor
            )
        )
    }

    fun createPhotoSheet(
        photo: Bitmap,
        config: SheetConfig
    ): SheetResult {
        // 1. Effective paper dimension in inches
        val baseWidthInches = if (config.paperPreset.isCustom) config.customWidthInches else config.paperPreset.widthInches
        val baseHeightInches = if (config.paperPreset.isCustom) config.customHeightInches else config.paperPreset.heightInches

        val (paperWInches, paperHInches) = when (config.orientation) {
            SheetOrientation.PORTRAIT -> min(baseWidthInches, baseHeightInches) to max(baseWidthInches, baseHeightInches)
            SheetOrientation.LANDSCAPE -> max(baseWidthInches, baseHeightInches) to min(baseWidthInches, baseHeightInches)
        }

        val dpi = config.dpi.coerceIn(72, 600)
        val totalWidthPx = (paperWInches * dpi).roundToInt().coerceAtLeast(100)
        val totalHeightPx = (paperHInches * dpi).roundToInt().coerceAtLeast(100)

        val sheetBitmap = Bitmap.createBitmap(totalWidthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        canvas.drawColor(config.backgroundColor)

        // 2. Rotate photo if requested
        val rotatedPhoto = if (config.rotation.degrees != 0f) {
            BitmapUtils.rotateBitmapFloat(photo, config.rotation.degrees)
        } else {
            photo
        }

        // 3. Margins and Usable Area
        val marginPx = ((config.marginMm / 25.4f) * dpi).roundToInt().coerceAtLeast(0)
        val spacingPx = ((config.spacingMm / 25.4f) * dpi).roundToInt().coerceAtLeast(0)

        val usableWidth = (totalWidthPx - (marginPx * 2)).coerceAtLeast(60)
        val usableHeight = (totalHeightPx - (marginPx * 2)).coerceAtLeast(60)

        val drawW: Int
        val drawH: Int
        val cols: Int
        val rows: Int
        val requestedCopies: Int
        val maxFitOnPage: Int

        if (config.useExactPhotoPhysicalSize) {
            val (effWMm, effHMm) = if (config.rotation == PhotoSheetRotation.ROT_90 || config.rotation == PhotoSheetRotation.ROT_270) {
                max(5f, config.targetPhotoHeightMm) to max(5f, config.targetPhotoWidthMm)
            } else {
                max(5f, config.targetPhotoWidthMm) to max(5f, config.targetPhotoHeightMm)
            }

            val exactW = ((effWMm / 25.4f) * dpi).roundToInt().coerceIn(10, usableWidth)
            val exactH = ((effHMm / 25.4f) * dpi).roundToInt().coerceIn(10, usableHeight)
            drawW = exactW
            drawH = exactH

            val fitCols = max(1, (usableWidth + spacingPx) / max(1, exactW + spacingPx))
            val fitRows = max(1, (usableHeight + spacingPx) / max(1, exactH + spacingPx))
            maxFitOnPage = fitCols * fitRows

            if (config.autoFillMaxCopies) {
                requestedCopies = maxFitOnPage
                cols = fitCols
                rows = fitRows
            } else {
                requestedCopies = min(config.copies.coerceAtLeast(1), maxFitOnPage)
                cols = min(fitCols, max(1, requestedCopies))
                rows = min(fitRows, ceil(requestedCopies.toDouble() / cols).toInt().coerceAtLeast(1))
            }
        } else {
            requestedCopies = config.copies.coerceAtLeast(1)
            val grid = calculateOptimalGrid(
                copies = requestedCopies,
                usableWidth = usableWidth,
                usableHeight = usableHeight,
                photoAspect = rotatedPhoto.width.toFloat() / rotatedPhoto.height.toFloat(),
                manualCols = config.columns,
                manualRows = config.rows
            )
            cols = grid.first
            rows = grid.second
            maxFitOnPage = cols * rows

            val totalHorizSpacing = (cols - 1).coerceAtLeast(0) * spacingPx
            val totalVertSpacing = (rows - 1).coerceAtLeast(0) * spacingPx

            val cellMaxW = max(1, (usableWidth - totalHorizSpacing) / cols)
            val cellMaxH = max(1, (usableHeight - totalVertSpacing) / rows)

            val photoAspect = rotatedPhoto.width.toFloat() / rotatedPhoto.height.toFloat()
            val cellAspect = cellMaxW.toFloat() / cellMaxH.toFloat()

            if (photoAspect > cellAspect) {
                drawW = cellMaxW
                drawH = max(1, (cellMaxW / photoAspect).roundToInt())
            } else {
                drawH = cellMaxH
                drawW = max(1, (cellMaxH * photoAspect).roundToInt())
            }
        }

        val scaledPhoto = Bitmap.createScaledBitmap(rotatedPhoto, drawW, drawH, true)

        val totalHorizSpacing = (cols - 1).coerceAtLeast(0) * spacingPx
        val totalVertSpacing = (rows - 1).coerceAtLeast(0) * spacingPx

        // 6. Overall grid dimensions
        val gridWidth = cols * drawW + totalHorizSpacing
        val gridHeight = rows * drawH + totalVertSpacing

        // Starting origin based on Alignment
        val startX: Float
        val startY: Float
        val effectiveColGap: Float
        val effectiveRowGap: Float

        when (config.alignment) {
            SheetAlignment.CENTER -> {
                startX = marginPx + (usableWidth - gridWidth) / 2f
                startY = marginPx + (usableHeight - gridHeight) / 2f
                effectiveColGap = spacingPx.toFloat()
                effectiveRowGap = spacingPx.toFloat()
            }
            SheetAlignment.TOP_LEFT -> {
                startX = marginPx.toFloat()
                startY = marginPx.toFloat()
                effectiveColGap = spacingPx.toFloat()
                effectiveRowGap = spacingPx.toFloat()
            }
            SheetAlignment.JUSTIFIED -> {
                startX = marginPx.toFloat()
                startY = marginPx.toFloat()
                val extraW = max(0, usableWidth - (cols * drawW))
                val extraH = max(0, usableHeight - (rows * drawH))
                effectiveColGap = if (cols > 1) extraW.toFloat() / (cols - 1) else extraW / 2f
                effectiveRowGap = if (rows > 1) extraH.toFloat() / (rows - 1) else extraH / 2f
            }
        }

        // 7. Cut Line / Crop Marks Paint
        val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(150, 160, 175)
            style = Paint.Style.STROKE
            strokeWidth = max(1f, dpi / 200f)
            if (config.cutLineStyle == CutLineStyle.DASHED) {
                val dash = max(8f, dpi / 25f)
                pathEffect = DashPathEffect(floatArrayOf(dash, dash), 0f)
            }
        }

        val tickLengthPx = (2.5f / 25.4f * dpi) // 2.5mm tick length

        // 8. Render copies
        var copiesDrawn = 0
        val placedRects = mutableListOf<Rect>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (copiesDrawn >= requestedCopies) break

                val photoLeft = startX + c * (drawW + effectiveColGap)
                val photoTop = startY + r * (drawH + effectiveRowGap)
                val photoRight = photoLeft + drawW
                val photoBottom = photoTop + drawH

                // Draw Photo
                canvas.drawBitmap(scaledPhoto, photoLeft, photoTop, null)
                placedRects.add(
                    Rect(
                        photoLeft.roundToInt(),
                        photoTop.roundToInt(),
                        photoRight.roundToInt(),
                        photoBottom.roundToInt()
                    )
                )

                // Draw Cut Marks
                when (config.cutLineStyle) {
                    CutLineStyle.SOLID, CutLineStyle.DASHED -> {
                        canvas.drawRect(photoLeft - 1, photoTop - 1, photoRight + 1, photoBottom + 1, cutLinePaint)
                    }
                    CutLineStyle.CORNER_TICKS -> {
                        // Top-left
                        canvas.drawLine(photoLeft - tickLengthPx, photoTop, photoLeft, photoTop, cutLinePaint)
                        canvas.drawLine(photoLeft, photoTop - tickLengthPx, photoLeft, photoTop, cutLinePaint)
                        // Top-right
                        canvas.drawLine(photoRight, photoTop, photoRight + tickLengthPx, photoTop, cutLinePaint)
                        canvas.drawLine(photoRight, photoTop - tickLengthPx, photoRight, photoTop, cutLinePaint)
                        // Bottom-left
                        canvas.drawLine(photoLeft - tickLengthPx, photoBottom, photoLeft, photoBottom, cutLinePaint)
                        canvas.drawLine(photoLeft, photoBottom, photoLeft, photoBottom + tickLengthPx, cutLinePaint)
                        // Bottom-right
                        canvas.drawLine(photoRight, photoBottom, photoRight + tickLengthPx, photoBottom, cutLinePaint)
                        canvas.drawLine(photoRight, photoBottom, photoRight, photoBottom + tickLengthPx, cutLinePaint)
                    }
                    CutLineStyle.CROSSHAIRS -> {
                        val ext = tickLengthPx * 1.2f
                        canvas.drawLine(photoLeft - ext, photoTop, photoLeft + ext, photoTop, cutLinePaint)
                        canvas.drawLine(photoLeft, photoTop - ext, photoLeft, photoTop + ext, cutLinePaint)
                        canvas.drawLine(photoRight - ext, photoTop, photoRight + ext, photoTop, cutLinePaint)
                        canvas.drawLine(photoRight, photoTop - ext, photoRight, photoTop + ext, cutLinePaint)
                        canvas.drawLine(photoLeft - ext, photoBottom, photoLeft + ext, photoBottom, cutLinePaint)
                        canvas.drawLine(photoLeft, photoBottom - ext, photoLeft, photoBottom + ext, cutLinePaint)
                        canvas.drawLine(photoRight - ext, photoBottom, photoRight + ext, photoBottom, cutLinePaint)
                        canvas.drawLine(photoRight, photoBottom - ext, photoRight, photoBottom + ext, cutLinePaint)
                    }
                    CutLineStyle.NONE -> {}
                }

                copiesDrawn++
            }
        }

        val photoWidthMm = (drawW.toFloat() / dpi) * 25.4f
        val photoHeightMm = (drawH.toFloat() / dpi) * 25.4f
        val paperWidthMm = paperWInches * 25.4f
        val paperHeightMm = paperHInches * 25.4f

        if (config.showCalibrationFooter) {
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textSize = max(10f, dpi / 22f)
                textAlign = Paint.Align.CENTER
            }
            val specText = String.format(
                java.util.Locale.US,
                "%s | %d Copies (%dx%d) | Photo: %.1fx%.1f mm | %d DPI — Print at 100%% Actual Scale",
                config.paperPreset.name,
                copiesDrawn,
                cols,
                rows,
                photoWidthMm,
                photoHeightMm,
                dpi
            )
            val footerY = totalHeightPx - max(8f, marginPx * 0.35f)
            canvas.drawText(specText, totalWidthPx / 2f, footerY, footerPaint)
        }

        val totalPhotoAreaPx = copiesDrawn.toLong() * drawW.toLong() * drawH.toLong()
        val totalSheetAreaPx = max(1L, totalWidthPx.toLong() * totalHeightPx.toLong())
        val utilization = ((totalPhotoAreaPx.toFloat() / totalSheetAreaPx.toFloat()) * 100f).coerceIn(0f, 100f)

        return SheetResult(
            bitmap = sheetBitmap,
            totalCopies = copiesDrawn,
            rows = rows,
            cols = cols,
            paperPreset = config.paperPreset,
            paperWidthMm = paperWidthMm,
            paperHeightMm = paperHeightMm,
            photoWidthMm = photoWidthMm,
            photoHeightMm = photoHeightMm,
            marginMm = config.marginMm,
            spacingMm = config.spacingMm,
            dpi = dpi,
            orientation = config.orientation,
            maxPossibleCopiesOnPage = maxFitOnPage,
            paperUtilizationPercent = utilization,
            placedPhotoRectsPx = placedRects
        )
    }

    private fun calculateOptimalGrid(
        copies: Int,
        usableWidth: Int,
        usableHeight: Int,
        photoAspect: Float,
        manualCols: Int?,
        manualRows: Int?
    ): Pair<Int, Int> {
        if (manualCols != null && manualRows != null && manualCols > 0 && manualRows > 0) {
            return manualCols to manualRows
        }

        when (copies) {
            1 -> return 1 to 1
            2 -> return if (usableWidth > usableHeight) 2 to 1 else 1 to 2
            3 -> return if (usableWidth > usableHeight) 3 to 1 else 1 to 3
            4 -> return 2 to 2
            5 -> return if (usableWidth > usableHeight) 3 to 2 else 2 to 3
            6 -> return if (usableWidth > usableHeight) 3 to 2 else 2 to 3
            8 -> return if (usableWidth > usableHeight) 4 to 2 else 2 to 4
            10 -> return if (usableWidth > usableHeight) 5 to 2 else 2 to 5
            12 -> return if (usableWidth > usableHeight) 4 to 3 else 3 to 4
            16 -> return 4 to 4
            20 -> return if (usableWidth > usableHeight) 5 to 4 else 4 to 5
        }

        val paperAspect = usableWidth.toFloat() / usableHeight.toFloat()
        var bestCols = 2
        var bestRows = max(1, ceil(copies / 2.0).toInt())
        var bestWaste = Float.MAX_VALUE

        val maxTestCols = min(copies, 10)
        for (c in 1..maxTestCols) {
            val r = ceil(copies.toDouble() / c).toInt()
            if (r > 12) continue

            val gridAspect = (c * photoAspect) / r
            val waste = kotlin.math.abs(gridAspect - paperAspect) + (c * r - copies) * 0.1f
            if (waste < bestWaste) {
                bestWaste = waste
                bestCols = c
                bestRows = r
            }
        }

        return bestCols to bestRows
    }
}
