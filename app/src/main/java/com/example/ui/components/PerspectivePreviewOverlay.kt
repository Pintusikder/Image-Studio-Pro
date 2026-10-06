package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.PerspectiveEngine
import com.example.processing.PerspectiveEngine.PerspectiveCorner
import com.example.processing.PerspectiveEngine.PointFNormalized
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

/**
 * Interactive preview overlay for Perspective Correction Tool (Request 25).
 * Supports:
 * - 4-corner tactile drag handles (TL, TR, BR, BL)
 * - Perspective bilinear grid mesh
 * - Real-time corner loupe (magnification lens with crosshairs)
 * - Rectified Live Preview rendering
 */
@Composable
fun PerspectivePreviewOverlay(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.workingBitmap ?: state.originalBitmap ?: return
    var activeDraggingCorner by remember { mutableStateOf<PerspectiveCorner?>(null) }
    var loupeNormalizedPos by remember { mutableStateOf(PointFNormalized(0.5f, 0.5f)) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B13)),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val boxHeightPx = with(density) { maxHeight.toPx() }

        // Calculate aspect-fit image draw rect inside Box
        val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerRatio = boxWidthPx / boxHeightPx

        val (drawW, drawH, drawLeft, drawTop) = if (imgRatio > containerRatio) {
            val w = boxWidthPx
            val h = w / imgRatio
            val l = 0f
            val t = (boxHeightPx - h) / 2f
            floatArrayOf(w, h, l, t)
        } else {
            val h = boxHeightPx
            val w = h * imgRatio
            val t = 0f
            val l = (boxWidthPx - w) / 2f
            floatArrayOf(w, h, l, t)
        }

        // Conversion helpers between normalized [0..1] and container pixel coordinates
        fun normToPx(point: PointFNormalized): Offset {
            return Offset(drawLeft + point.x * drawW, drawTop + point.y * drawH)
        }

        fun pxToNorm(px: Float, py: Float): PointFNormalized {
            val nx = ((px - drawLeft) / drawW).coerceIn(0f, 1f)
            val ny = ((py - drawTop) / drawH).coerceIn(0f, 1f)
            return PointFNormalized(nx, ny)
        }

        // Render base image
        Image(
            bitmap = (state.previewBitmap ?: bitmap).asImageBitmap(),
            contentDescription = "Perspective Target Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // IF NOT in live rectified preview mode, draw Quad Overlay and 4 Handles
        if (!state.isPerspectivePreviewActive) {
            val quad = state.perspectiveQuad

            val tlPx = normToPx(quad.topLeft)
            val trPx = normToPx(quad.topRight)
            val brPx = normToPx(quad.bottomRight)
            val blPx = normToPx(quad.bottomLeft)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw quadrilateral semi-transparent fill and perimeter
                val polyPath = Path().apply {
                    moveTo(tlPx.x, tlPx.y)
                    lineTo(trPx.x, trPx.y)
                    lineTo(brPx.x, brPx.y)
                    lineTo(blPx.x, blPx.y)
                    close()
                }

                // Translucent blue document tint
                drawPath(polyPath, color = Color(0x330284C7))

                // Perimeter stroke
                drawPath(
                    polyPath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(width = 3.dp.toPx())
                )

                // 2. Perspective Bilinear Subdivision Grid
                if (state.showPerspectiveGrid) {
                    val divisions = state.perspectiveGridDivisions.coerceIn(2, 10)
                    val gridDash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

                    // Horizontal grid curves
                    for (i in 1 until divisions) {
                        val t = i.toFloat() / divisions
                        // Left edge point
                        val leftPt = bilinear(tlPx, blPx, t)
                        // Right edge point
                        val rightPt = bilinear(trPx, brPx, t)
                        drawLine(
                            color = Color(0x80E2E8F0),
                            start = leftPt,
                            end = rightPt,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = gridDash
                        )
                    }

                    // Vertical grid curves
                    for (i in 1 until divisions) {
                        val s = i.toFloat() / divisions
                        // Top edge point
                        val topPt = bilinear(tlPx, trPx, s)
                        // Bottom edge point
                        val bottomPt = bilinear(blPx, brPx, s)
                        drawLine(
                            color = Color(0x80E2E8F0),
                            start = topPt,
                            end = bottomPt,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = gridDash
                        )
                    }
                }
            }

            // 3. Interactive Corner Handles
            PerspectiveCornerHandle(
                corner = PerspectiveCorner.TOP_LEFT,
                positionPx = tlPx,
                color = Color(0xFF38BDF8), // Cyan
                isSelected = state.activePerspectiveCorner == PerspectiveCorner.TOP_LEFT,
                onDragStart = {
                    activeDraggingCorner = PerspectiveCorner.TOP_LEFT
                    loupeNormalizedPos = quad.topLeft
                    viewModel.setActivePerspectiveCorner(PerspectiveCorner.TOP_LEFT)
                },
                onDrag = { dx, dy ->
                    val cur = normToPx(quad.topLeft)
                    val newNorm = pxToNorm(cur.x + dx, cur.y + dy)
                    loupeNormalizedPos = newNorm
                    viewModel.updatePerspectiveCorner(PerspectiveCorner.TOP_LEFT, newNorm.x, newNorm.y)
                },
                onDragEnd = { activeDraggingCorner = null }
            )

            PerspectiveCornerHandle(
                corner = PerspectiveCorner.TOP_RIGHT,
                positionPx = trPx,
                color = Color(0xFF34D399), // Emerald
                isSelected = state.activePerspectiveCorner == PerspectiveCorner.TOP_RIGHT,
                onDragStart = {
                    activeDraggingCorner = PerspectiveCorner.TOP_RIGHT
                    loupeNormalizedPos = quad.topRight
                    viewModel.setActivePerspectiveCorner(PerspectiveCorner.TOP_RIGHT)
                },
                onDrag = { dx, dy ->
                    val cur = normToPx(quad.topRight)
                    val newNorm = pxToNorm(cur.x + dx, cur.y + dy)
                    loupeNormalizedPos = newNorm
                    viewModel.updatePerspectiveCorner(PerspectiveCorner.TOP_RIGHT, newNorm.x, newNorm.y)
                },
                onDragEnd = { activeDraggingCorner = null }
            )

            PerspectiveCornerHandle(
                corner = PerspectiveCorner.BOTTOM_RIGHT,
                positionPx = brPx,
                color = Color(0xFFFBBF24), // Amber
                isSelected = state.activePerspectiveCorner == PerspectiveCorner.BOTTOM_RIGHT,
                onDragStart = {
                    activeDraggingCorner = PerspectiveCorner.BOTTOM_RIGHT
                    loupeNormalizedPos = quad.bottomRight
                    viewModel.setActivePerspectiveCorner(PerspectiveCorner.BOTTOM_RIGHT)
                },
                onDrag = { dx, dy ->
                    val cur = normToPx(quad.bottomRight)
                    val newNorm = pxToNorm(cur.x + dx, cur.y + dy)
                    loupeNormalizedPos = newNorm
                    viewModel.updatePerspectiveCorner(PerspectiveCorner.BOTTOM_RIGHT, newNorm.x, newNorm.y)
                },
                onDragEnd = { activeDraggingCorner = null }
            )

            PerspectiveCornerHandle(
                corner = PerspectiveCorner.BOTTOM_LEFT,
                positionPx = blPx,
                color = Color(0xFFF43F5E), // Rose
                isSelected = state.activePerspectiveCorner == PerspectiveCorner.BOTTOM_LEFT,
                onDragStart = {
                    activeDraggingCorner = PerspectiveCorner.BOTTOM_LEFT
                    loupeNormalizedPos = quad.bottomLeft
                    viewModel.setActivePerspectiveCorner(PerspectiveCorner.BOTTOM_LEFT)
                },
                onDrag = { dx, dy ->
                    val cur = normToPx(quad.bottomLeft)
                    val newNorm = pxToNorm(cur.x + dx, cur.y + dy)
                    loupeNormalizedPos = newNorm
                    viewModel.updatePerspectiveCorner(PerspectiveCorner.BOTTOM_LEFT, newNorm.x, newNorm.y)
                },
                onDragEnd = { activeDraggingCorner = null }
            )

            // 4. Magnifying Loupe Lens when dragging
            if (activeDraggingCorner != null) {
                val isTopCorner = activeDraggingCorner == PerspectiveCorner.TOP_LEFT || activeDraggingCorner == PerspectiveCorner.TOP_RIGHT
                // Position loupe at opposite top or bottom to avoid occlusion by user's thumb
                val loupeAlignment = if (isTopCorner) Alignment.BottomCenter else Alignment.TopCenter

                Box(
                    modifier = Modifier
                        .align(loupeAlignment)
                        .padding(16.dp)
                ) {
                    PerspectiveLoupeView(
                        bitmap = bitmap,
                        normalizedCenter = loupeNormalizedPos,
                        cornerName = activeDraggingCorner?.label ?: ""
                    )
                }
            }
        } else {
            // Live Preview Mode indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC047857),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                    Text(
                        text = "LIVE RECTIFIED PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Bilinear interpolation helper for grid points.
 */
private fun bilinear(p1: Offset, p2: Offset, t: Float): Offset {
    return Offset(
        p1.x + (p2.x - p1.x) * t,
        p1.y + (p2.y - p1.y) * t
    )
}

/**
 * Tactile touch handle for quadrilateral corners with 48dp touch target.
 */
@Composable
private fun PerspectiveCornerHandle(
    corner: PerspectiveCorner,
    positionPx: Offset,
    color: Color,
    isSelected: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val handleSize = 48.dp
    val handleSizePx = with(LocalDensity.current) { handleSize.toPx() }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (positionPx.x - handleSizePx / 2f).roundToInt(),
                    (positionPx.y - handleSizePx / 2f).roundToInt()
                )
            }
            .size(handleSize)
            .pointerInput(corner) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .testTag("handle_${corner.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing halo
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isSelected) 0.4f else 0.25f))
                .border(2.dp, color, CircleShape)
        )

        // Center Pin
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White)
        )

        // Small corner badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, color),
            modifier = Modifier
                .align(
                    when (corner) {
                        PerspectiveCorner.TOP_LEFT -> Alignment.TopStart
                        PerspectiveCorner.TOP_RIGHT -> Alignment.TopEnd
                        PerspectiveCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
                        PerspectiveCorner.BOTTOM_LEFT -> Alignment.BottomStart
                    }
                )
        ) {
            Text(
                text = corner.shortName,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

/**
 * 2x Magnification Loupe with precision crosshairs for micro-millimeter corner alignment.
 */
@Composable
private fun PerspectiveLoupeView(
    bitmap: Bitmap,
    normalizedCenter: PointFNormalized,
    cornerName: String
) {
    val loupeSize = 100.dp
    val sampleRadius = 40

    val pixelX = (normalizedCenter.x * bitmap.width).roundToInt()
    val pixelY = (normalizedCenter.y * bitmap.height).roundToInt()

    val cropLeft = (pixelX - sampleRadius).coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
    val cropTop = (pixelY - sampleRadius).coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
    val cropW = (sampleRadius * 2).coerceAtMost(bitmap.width - cropLeft).coerceAtLeast(1)
    val cropH = (sampleRadius * 2).coerceAtMost(bitmap.height - cropTop).coerceAtLeast(1)

    val sampledBitmap = remember(normalizedCenter, bitmap) {
        try {
            Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropW, cropH)
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        shape = CircleShape,
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF38BDF8)),
        shadowElevation = 12.dp,
        modifier = Modifier.size(loupeSize)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (sampledBitmap != null) {
                Image(
                    bitmap = sampledBitmap.asImageBitmap(),
                    contentDescription = "Magnified Corner Loupe",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Crosshair overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Precision Red Crosshair
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(cx - 16f, cy),
                    end = Offset(cx + 16f, cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(cx, cy - 16f),
                    end = Offset(cx, cy + 16f),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Corner tag label
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xCC000000),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = cornerName,
                    color = Color(0xFF38BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
