package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CropAspectRatio
import com.example.processing.CropEngine
import com.example.processing.CropGuideGrid
import com.example.processing.CropShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CropOverlayView(
    bitmap: Bitmap,
    aspectRatio: CropAspectRatio,
    cropShape: CropShape = CropShape.RECTANGLE,
    guideGrid: CropGuideGrid = CropGuideGrid.RULE_OF_THIRDS,
    onCropBoundsChanged: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.background(Color(0xFF0B1120)),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val viewAspect = maxWidthPx / maxHeightPx

        val displayedW: Float
        val displayedH: Float
        if (imgAspect > viewAspect) {
            displayedW = maxWidthPx
            displayedH = maxWidthPx / imgAspect
        } else {
            displayedH = maxHeightPx
            displayedW = maxHeightPx * imgAspect
        }

        val imgLeft = (maxWidthPx - displayedW) / 2f
        val imgTop = (maxHeightPx - displayedH) / 2f

        // Normalized crop rectangle (0..1 relative to the displayed image)
        var cropRect by remember { mutableStateOf(RectF(0.05f, 0.05f, 0.95f, 0.95f)) }

        // Update crop rectangle when aspect ratio or bitmap changes
        LaunchedEffect(aspectRatio, bitmap) {
            if (aspectRatio.ratioX != null && aspectRatio.ratioY != null) {
                val targetRatio = aspectRatio.ratioX / aspectRatio.ratioY
                val targetAspectNormalized = targetRatio / imgAspect

                if (targetAspectNormalized > 1f) {
                    val h = (0.85f / targetAspectNormalized).coerceIn(0.1f, 0.9f)
                    val top = ((1f - h) / 2f).coerceIn(0.05f, 0.9f)
                    cropRect = RectF(0.08f, top, 0.92f, (top + h).coerceAtMost(0.95f))
                } else {
                    val w = (0.85f * targetAspectNormalized).coerceIn(0.1f, 0.9f)
                    val left = ((1f - w) / 2f).coerceIn(0.05f, 0.9f)
                    cropRect = RectF(left, 0.08f, (left + w).coerceAtMost(0.95f), 0.92f)
                }
            } else {
                cropRect = RectF(0.05f, 0.05f, 0.95f, 0.95f)
            }
            onCropBoundsChanged(cropRect)
        }

        // Active drag handle:
        // 0 = none, 1 = TopLeft, 2 = TopRight, 3 = BottomRight, 4 = BottomLeft, 5 = Center Move
        // 6 = Top Edge, 7 = Right Edge, 8 = Bottom Edge, 9 = Left Edge
        var activeHandle by remember { mutableStateOf(0) }

        val hasFixedRatio = aspectRatio.ratioX != null && aspectRatio.ratioY != null
        val lockedRatioVal = if (hasFixedRatio) aspectRatio.ratioX!! / aspectRatio.ratioY!! else null

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(displayedW, displayedH, imgLeft, imgTop, hasFixedRatio) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cLeft = imgLeft + cropRect.left * displayedW
                            val cTop = imgTop + cropRect.top * displayedH
                            val cRight = imgLeft + cropRect.right * displayedW
                            val cBottom = imgTop + cropRect.bottom * displayedH

                            val handleRadius = 44.dp.toPx()
                            val edgeTolerance = 28.dp.toPx()

                            // Check corners first
                            activeHandle = when {
                                (offset - Offset(cLeft, cTop)).getDistance() < handleRadius -> 1
                                (offset - Offset(cRight, cTop)).getDistance() < handleRadius -> 2
                                (offset - Offset(cRight, cBottom)).getDistance() < handleRadius -> 3
                                (offset - Offset(cLeft, cBottom)).getDistance() < handleRadius -> 4
                                // Edge handles (active if freeform or edge drag)
                                !hasFixedRatio && abs(offset.y - cTop) < edgeTolerance && offset.x in cLeft..cRight -> 6
                                !hasFixedRatio && abs(offset.x - cRight) < edgeTolerance && offset.y in cTop..cBottom -> 7
                                !hasFixedRatio && abs(offset.y - cBottom) < edgeTolerance && offset.x in cLeft..cRight -> 8
                                !hasFixedRatio && abs(offset.x - cLeft) < edgeTolerance && offset.y in cTop..cBottom -> 9
                                // Inside center
                                offset.x in cLeft..cRight && offset.y in cTop..cBottom -> 5
                                else -> 0
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle == 0) return@detectDragGestures

                            val dxNorm = dragAmount.x / displayedW
                            val dyNorm = dragAmount.y / displayedH

                            val newRect = RectF(cropRect)

                            if (hasFixedRatio && lockedRatioVal != null) {
                                // Locked aspect ratio corner resizing
                                val targetImageAspect = lockedRatioVal / imgAspect

                                when (activeHandle) {
                                    1 -> { // TopLeft: drag diagonally keeping aspect
                                        val delta = if (abs(dxNorm) > abs(dyNorm)) dxNorm else dyNorm * targetImageAspect
                                        val maxL = newRect.right - 0.1f
                                        val testL = (newRect.left + delta).coerceIn(0f, maxL)
                                        val newW = newRect.right - testL
                                        val newH = newW / targetImageAspect
                                        val testT = newRect.bottom - newH
                                        if (testT >= 0f) {
                                            newRect.left = testL
                                            newRect.top = testT
                                        }
                                    }
                                    2 -> { // TopRight
                                        val delta = if (abs(dxNorm) > abs(dyNorm)) dxNorm else -dyNorm * targetImageAspect
                                        val minR = newRect.left + 0.1f
                                        val testR = (newRect.right + delta).coerceIn(minR, 1f)
                                        val newW = testR - newRect.left
                                        val newH = newW / targetImageAspect
                                        val testT = newRect.bottom - newH
                                        if (testT >= 0f) {
                                            newRect.right = testR
                                            newRect.top = testT
                                        }
                                    }
                                    3 -> { // BottomRight
                                        val delta = if (abs(dxNorm) > abs(dyNorm)) dxNorm else dyNorm * targetImageAspect
                                        val minR = newRect.left + 0.1f
                                        val testR = (newRect.right + delta).coerceIn(minR, 1f)
                                        val newW = testR - newRect.left
                                        val newH = newW / targetImageAspect
                                        val testB = newRect.top + newH
                                        if (testB <= 1f) {
                                            newRect.right = testR
                                            newRect.bottom = testB
                                        }
                                    }
                                    4 -> { // BottomLeft
                                        val delta = if (abs(dxNorm) > abs(dyNorm)) dxNorm else -dyNorm * targetImageAspect
                                        val maxL = newRect.right - 0.1f
                                        val testL = (newRect.left + delta).coerceIn(0f, maxL)
                                        val newW = newRect.right - testL
                                        val newH = newW / targetImageAspect
                                        val testB = newRect.top + newH
                                        if (testB <= 1f) {
                                            newRect.left = testL
                                            newRect.bottom = testB
                                        }
                                    }
                                    5 -> { // Center Move
                                        val w = newRect.width()
                                        val h = newRect.height()
                                        val newL = (newRect.left + dxNorm).coerceIn(0f, 1f - w)
                                        val newT = (newRect.top + dyNorm).coerceIn(0f, 1f - h)
                                        newRect.set(newL, newT, newL + w, newT + h)
                                    }
                                }
                            } else {
                                // Freeform resizing
                                when (activeHandle) {
                                    1 -> { // TopLeft
                                        newRect.left = (newRect.left + dxNorm).coerceIn(0f, newRect.right - 0.05f)
                                        newRect.top = (newRect.top + dyNorm).coerceIn(0f, newRect.bottom - 0.05f)
                                    }
                                    2 -> { // TopRight
                                        newRect.right = (newRect.right + dxNorm).coerceIn(newRect.left + 0.05f, 1f)
                                        newRect.top = (newRect.top + dyNorm).coerceIn(0f, newRect.bottom - 0.05f)
                                    }
                                    3 -> { // BottomRight
                                        newRect.right = (newRect.right + dxNorm).coerceIn(newRect.left + 0.05f, 1f)
                                        newRect.bottom = (newRect.bottom + dyNorm).coerceIn(newRect.top + 0.05f, 1f)
                                    }
                                    4 -> { // BottomLeft
                                        newRect.left = (newRect.left + dxNorm).coerceIn(0f, newRect.right - 0.05f)
                                        newRect.bottom = (newRect.bottom + dyNorm).coerceIn(newRect.top + 0.05f, 1f)
                                    }
                                    5 -> { // Center Move
                                        val w = newRect.width()
                                        val h = newRect.height()
                                        val newL = (newRect.left + dxNorm).coerceIn(0f, 1f - w)
                                        val newT = (newRect.top + dyNorm).coerceIn(0f, 1f - h)
                                        newRect.set(newL, newT, newL + w, newT + h)
                                    }
                                    6 -> { // Top Edge
                                        newRect.top = (newRect.top + dyNorm).coerceIn(0f, newRect.bottom - 0.05f)
                                    }
                                    7 -> { // Right Edge
                                        newRect.right = (newRect.right + dxNorm).coerceIn(newRect.left + 0.05f, 1f)
                                    }
                                    8 -> { // Bottom Edge
                                        newRect.bottom = (newRect.bottom + dyNorm).coerceIn(newRect.top + 0.05f, 1f)
                                    }
                                    9 -> { // Left Edge
                                        newRect.left = (newRect.left + dxNorm).coerceIn(0f, newRect.right - 0.05f)
                                    }
                                }
                            }

                            cropRect = newRect
                            onCropBoundsChanged(cropRect)
                        },
                        onDragEnd = { activeHandle = 0 },
                        onDragCancel = { activeHandle = 0 }
                    )
                }
        ) {
            // 1. Draw scaled original image
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                dstSize = IntSize(displayedW.toInt(), displayedH.toInt())
            )

            val cLeft = imgLeft + cropRect.left * displayedW
            val cTop = imgTop + cropRect.top * displayedH
            val cRight = imgLeft + cropRect.right * displayedW
            val cBottom = imgTop + cropRect.bottom * displayedH
            val cW = cRight - cLeft
            val cH = cBottom - cTop

            // 2. Dim outer mask
            val dimColor = Color(0xB3000000)
            drawRect(dimColor, Offset(0f, 0f), Size(size.width, cTop))
            drawRect(dimColor, Offset(0f, cBottom), Size(size.width, size.height - cBottom))
            drawRect(dimColor, Offset(0f, cTop), Size(cLeft, cH))
            drawRect(dimColor, Offset(cRight, cTop), Size(size.width - cRight, cH))

            // 3. Grid overlays based on active CropGuideGrid
            val gridPaint = Stroke(width = 1.dp.toPx())
            val gridColor = Color(0x66FFFFFF)

            when (guideGrid) {
                CropGuideGrid.RULE_OF_THIRDS -> {
                    // Vertical lines
                    drawLine(gridColor, Offset(cLeft + cW / 3f, cTop), Offset(cLeft + cW / 3f, cBottom), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft + (cW * 2f) / 3f, cTop), Offset(cLeft + (cW * 2f) / 3f, cBottom), gridPaint.width)
                    // Horizontal lines
                    drawLine(gridColor, Offset(cLeft, cTop + cH / 3f), Offset(cRight, cTop + cH / 3f), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft, cTop + (cH * 2f) / 3f), Offset(cRight, cTop + (cH * 2f) / 3f), gridPaint.width)

                    // 4 Golden intersection dots
                    val dotRadius = 3.dp.toPx()
                    val dotColor = Color(0xFF38BDF8)
                    drawCircle(dotColor, dotRadius, Offset(cLeft + cW / 3f, cTop + cH / 3f))
                    drawCircle(dotColor, dotRadius, Offset(cLeft + (cW * 2f) / 3f, cTop + cH / 3f))
                    drawCircle(dotColor, dotRadius, Offset(cLeft + cW / 3f, cTop + (cH * 2f) / 3f))
                    drawCircle(dotColor, dotRadius, Offset(cLeft + (cW * 2f) / 3f, cTop + (cH * 2f) / 3f))
                }
                CropGuideGrid.GOLDEN_RATIO -> {
                    // Phi lines at 0.382 and 0.618
                    val phi1 = 0.382f
                    val phi2 = 0.618f
                    drawLine(gridColor, Offset(cLeft + cW * phi1, cTop), Offset(cLeft + cW * phi1, cBottom), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft + cW * phi2, cTop), Offset(cLeft + cW * phi2, cBottom), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft, cTop + cH * phi1), Offset(cRight, cTop + cH * phi1), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft, cTop + cH * phi2), Offset(cRight, cTop + cH * phi2), gridPaint.width)
                }
                CropGuideGrid.DIAGONAL -> {
                    // Corner-to-corner harmonic diagonals
                    drawLine(gridColor, Offset(cLeft, cTop), Offset(cRight, cBottom), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft, cBottom), Offset(cRight, cTop), gridPaint.width)
                }
                CropGuideGrid.CENTER_CROSS -> {
                    // Precision crosshairs
                    val crossColor = Color(0x9938BDF8)
                    drawLine(crossColor, Offset(cLeft + cW / 2f, cTop), Offset(cLeft + cW / 2f, cBottom), gridPaint.width)
                    drawLine(crossColor, Offset(cLeft, cTop + cH / 2f), Offset(cRight, cTop + cH / 2f), gridPaint.width)
                    drawCircle(crossColor, 4.dp.toPx(), Offset(cLeft + cW / 2f, cTop + cH / 2f))
                }
                CropGuideGrid.GOLDEN_SPIRAL -> {
                    // Approximated Fibonacci spiral arcs
                    drawLine(gridColor, Offset(cLeft + cW * 0.618f, cTop), Offset(cLeft + cW * 0.618f, cBottom), gridPaint.width)
                    drawLine(gridColor, Offset(cLeft + cW * 0.618f, cTop + cH * 0.618f), Offset(cRight, cTop + cH * 0.618f), gridPaint.width)
                }
                CropGuideGrid.NONE -> {
                    // Clean
                }
            }

            // 4. White framing box
            drawRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(cLeft, cTop),
                size = Size(cW, cH),
                style = Stroke(width = 2.dp.toPx())
            )

            // 5. If a non-rectangular shape is selected, draw vector silhouette
            if (cropShape != CropShape.RECTANGLE) {
                val androidPath = CropEngine.createShapePath(cropShape, cW, cH)
                val composePath = androidPath.asComposePath()
                // Shift to crop coordinates
                val shiftedPath = Path()
                shiftedPath.addPath(composePath, Offset(cLeft, cTop))
                drawPath(
                    path = shiftedPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 6. Corner handles
            val handleLen = 22.dp.toPx()
            val handleThick = 4.dp.toPx()
            val handleColor = Color(0xFF38BDF8)

            // TopLeft
            drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft + handleLen, cTop), handleThick)
            drawLine(handleColor, Offset(cLeft, cTop), Offset(cLeft, cTop + handleLen), handleThick)
            // TopRight
            drawLine(handleColor, Offset(cRight, cTop), Offset(cRight - handleLen, cTop), handleThick)
            drawLine(handleColor, Offset(cRight, cTop), Offset(cRight, cTop + handleLen), handleThick)
            // BottomLeft
            drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft + handleLen, cBottom), handleThick)
            drawLine(handleColor, Offset(cLeft, cBottom), Offset(cLeft, cBottom - handleLen), handleThick)
            // BottomRight
            drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight - handleLen, cBottom), handleThick)
            drawLine(handleColor, Offset(cRight, cBottom), Offset(cRight, cBottom - handleLen), handleThick)

            // 7. Edge midpoint handles (if freeform)
            if (!hasFixedRatio) {
                val edgeMidLen = 14.dp.toPx()
                // Top midpoint
                drawLine(handleColor, Offset(cLeft + cW / 2f - edgeMidLen, cTop), Offset(cLeft + cW / 2f + edgeMidLen, cTop), handleThick)
                // Bottom midpoint
                drawLine(handleColor, Offset(cLeft + cW / 2f - edgeMidLen, cBottom), Offset(cLeft + cW / 2f + edgeMidLen, cBottom), handleThick)
                // Left midpoint
                drawLine(handleColor, Offset(cLeft, cTop + cH / 2f - edgeMidLen), Offset(cLeft, cTop + cH / 2f + edgeMidLen), handleThick)
                // Right midpoint
                drawLine(handleColor, Offset(cRight, cTop + cH / 2f - edgeMidLen), Offset(cRight, cTop + cH / 2f + edgeMidLen), handleThick)
            }
        }

        // Live Dimension Pill Badge
        val activePixelW = ((cropRect.right - cropRect.left) * bitmap.width).roundToInt().coerceAtLeast(1)
        val activePixelH = ((cropRect.bottom - cropRect.top) * bitmap.height).roundToInt().coerceAtLeast(1)
        val activeRatioStr = if (aspectRatio.ratioX != null && aspectRatio.ratioY != null) {
            "${aspectRatio.ratioX.toInt()}:${aspectRatio.ratioY.toInt()}"
        } else {
            String.format("%.2f:1", activePixelW.toFloat() / activePixelH.toFloat())
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC0F172A),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "$activePixelW × $activePixelH px ($activeRatioStr)",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
