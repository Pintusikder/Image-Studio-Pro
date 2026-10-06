package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.RotateStraightenEngine
import com.example.ui.viewmodel.StudioUiState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Visual preview overlay for the Rotate & Straighten Tool (Request 23).
 * Provides:
 * - Live real-time rotated image view
 * - Composition Grid Overlay (Rule of Thirds, Fine Mesh, Golden Ratio)
 * - Draggable Horizon Guide with visual Spirit Level bubble and angle readout
 * - Inscribed crop bounds indicator
 */
@Composable
fun RotateStraightenPreviewOverlay(
    bitmap: Bitmap?,
    state: StudioUiState,
    onHorizonOffsetChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return

    val totalAngle = state.rotationAngle + state.straightenAngle
    val isLevel = abs(totalAngle % 90f) <= 0.25f || abs(state.straightenAngle) <= 0.25f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("rotate_straighten_preview_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Calculate aspect-fit dimensions of image within container
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerAspect = if (containerHeight > 0f) containerWidth / containerHeight else 1f

        val fittedW: Float
        val fittedH: Float
        if (imageAspect > containerAspect) {
            fittedW = containerWidth
            fittedH = containerWidth / imageAspect
        } else {
            fittedH = containerHeight
            fittedW = containerHeight * imageAspect
        }

        // 1. Live Rotated Image Layer
        Box(
            modifier = Modifier
                .size(
                    width = (fittedW / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                    height = (fittedH / androidx.compose.ui.platform.LocalDensity.current.density).dp
                )
                .graphicsLayer {
                    rotationZ = totalAngle
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Rotated Image Preview",
                modifier = Modifier.fillMaxSize()
            )

            // If auto-crop is enabled and image is rotated, draw the inscribed crop boundary
            if (state.straightenCropMode == RotateStraightenEngine.StraightenCropMode.AUTO_CROP && abs(totalAngle % 90f) > 0.1f) {
                val inscribedScale = RotateStraightenEngine.calculateInscribedCropFraction(
                    bitmap.width,
                    bitmap.height,
                    totalAngle
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cropW = size.width * inscribedScale
                    val cropH = size.height * inscribedScale
                    val left = (size.width - cropW) / 2f
                    val top = (size.height - cropH) / 2f

                    // Dashed framing line of inscribed crop
                    drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(left, top),
                        size = Size(cropW, cropH),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }
            }
        }

        // 2. Grid Overlay (Drawn across the image bounds)
        if (state.showGrid) {
            Canvas(
                modifier = Modifier
                    .size(
                        width = (fittedW / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                        height = (fittedH / androidx.compose.ui.platform.LocalDensity.current.density).dp
                    )
                    .testTag("composition_grid_overlay")
            ) {
                val gridAlpha = state.gridOpacity.coerceIn(0.1f, 1.0f)
                val lineColor = Color.White.copy(alpha = gridAlpha * 0.85f)
                val shadowColor = Color.Black.copy(alpha = gridAlpha * 0.6f)
                val strokeW = 1.2.dp.toPx()

                when (state.gridType) {
                    RotateStraightenEngine.GridType.RULE_OF_THIRDS -> {
                        // 3x3 grid
                        val w1 = size.width / 3f
                        val w2 = size.width * 2f / 3f
                        val h1 = size.height / 3f
                        val h2 = size.height * 2f / 3f

                        // Vertical lines with slight shadow
                        drawLine(shadowColor, Offset(w1 + 1f, 0f), Offset(w1 + 1f, size.height), strokeW)
                        drawLine(lineColor, Offset(w1, 0f), Offset(w1, size.height), strokeW)
                        drawLine(shadowColor, Offset(w2 + 1f, 0f), Offset(w2 + 1f, size.height), strokeW)
                        drawLine(lineColor, Offset(w2, 0f), Offset(w2, size.height), strokeW)

                        // Horizontal lines with slight shadow
                        drawLine(shadowColor, Offset(0f, h1 + 1f), Offset(size.width, h1 + 1f), strokeW)
                        drawLine(lineColor, Offset(0f, h1), Offset(size.width, h1), strokeW)
                        drawLine(shadowColor, Offset(0f, h2 + 1f), Offset(size.width, h2 + 1f), strokeW)
                        drawLine(lineColor, Offset(0f, h2), Offset(size.width, h2), strokeW)
                    }

                    RotateStraightenEngine.GridType.GOLDEN_RATIO -> {
                        // Phi = 0.618 / 0.382
                        val phi = 0.382f
                        val w1 = size.width * phi
                        val w2 = size.width * (1f - phi)
                        val h1 = size.height * phi
                        val h2 = size.height * (1f - phi)

                        drawLine(shadowColor, Offset(w1 + 1f, 0f), Offset(w1 + 1f, size.height), strokeW)
                        drawLine(lineColor, Offset(w1, 0f), Offset(w1, size.height), strokeW)
                        drawLine(shadowColor, Offset(w2 + 1f, 0f), Offset(w2 + 1f, size.height), strokeW)
                        drawLine(lineColor, Offset(w2, 0f), Offset(w2, size.height), strokeW)

                        drawLine(shadowColor, Offset(0f, h1 + 1f), Offset(size.width, h1 + 1f), strokeW)
                        drawLine(lineColor, Offset(0f, h1), Offset(size.width, h1), strokeW)
                        drawLine(shadowColor, Offset(0f, h2 + 1f), Offset(size.width, h2 + 1f), strokeW)
                        drawLine(lineColor, Offset(0f, h2), Offset(size.width, h2), strokeW)
                    }

                    RotateStraightenEngine.GridType.FINE_GRID, RotateStraightenEngine.GridType.DENSE_GRID -> {
                        val cols = state.gridType.cols
                        val rows = state.gridType.rows
                        for (c in 1 until cols) {
                            val x = size.width * (c.toFloat() / cols)
                            drawLine(shadowColor, Offset(x + 1f, 0f), Offset(x + 1f, size.height), strokeW * 0.8f)
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeW * 0.8f)
                        }
                        for (r in 1 until rows) {
                            val y = size.height * (r.toFloat() / rows)
                            drawLine(shadowColor, Offset(0f, y + 1f), Offset(size.width, y + 1f), strokeW * 0.8f)
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeW * 0.8f)
                        }
                    }
                }
            }
        }

        // 3. Horizon Guide Overlay
        if (state.showHorizonGuide) {
            val yOffset = state.horizonGuideYOffset.coerceIn(0.05f, 0.95f)
            val currentYPx = fittedH * yOffset

            Box(
                modifier = Modifier
                    .size(
                        width = (fittedW / androidx.compose.ui.platform.LocalDensity.current.density).dp,
                        height = (fittedH / androidx.compose.ui.platform.LocalDensity.current.density).dp
                    )
            ) {
                // Interactive horizontal line with draggable handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, (currentYPx - 20.dp.toPx()).roundToInt()) }
                        .height(40.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newY = (currentYPx + dragAmount.y).coerceIn(0f, fittedH)
                                val newOffset = (newY / fittedH).coerceIn(0.05f, 0.95f)
                                onHorizonOffsetChanged(newOffset)
                            }
                        }
                        .testTag("horizon_guide_interactive_line"),
                    contentAlignment = Alignment.Center
                ) {
                    // Full-width horizontal reference line
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        val centerY = size.height / 2f
                        val levelColor = if (isLevel) Color(0xFF10B981) else Color(0xFF38BDF8)

                        // Outer glow/shadow line
                        drawLine(
                            color = Color.Black.copy(alpha = 0.5f),
                            start = Offset(0f, centerY + 1f),
                            end = Offset(size.width, centerY + 1f),
                            strokeWidth = 3.dp.toPx()
                        )
                        // Core horizon guide line
                        drawLine(
                            color = levelColor,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Pitch tick marks at center and thirds
                        val tickH = 6.dp.toPx()
                        drawLine(levelColor, Offset(size.width * 0.25f, centerY - tickH), Offset(size.width * 0.25f, centerY + tickH), 1.5.dp.toPx())
                        drawLine(levelColor, Offset(size.width * 0.75f, centerY - tickH), Offset(size.width * 0.75f, centerY + tickH), 1.5.dp.toPx())
                    }

                    // Center Spirit Level / Degree Indicator Bubble
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLevel) Color(0xFF065F46) else Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isLevel) Color(0xFF34D399) else Color(0xFF38BDF8)
                        ),
                        shadowElevation = 6.dp,
                        modifier = Modifier.testTag("horizon_spirit_level_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (isLevel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Level",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LEVEL 0.0°",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            } else {
                                // Bubble offset simulation
                                val bubbleOffset = (state.straightenAngle / 15f).coerceIn(-1f, 1f) * 8f
                                Box(
                                    modifier = Modifier
                                        .size(14.dp, 8.dp)
                                        .background(Color(0xFF334155), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(bubbleOffset.roundToInt(), 0) }
                                            .size(5.dp, 6.dp)
                                            .background(Color(0xFF38BDF8), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                val sign = if (state.straightenAngle > 0) "+" else ""
                                Text(
                                    text = String.format("%s%.1f°", sign, state.straightenAngle),
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Floating Top Readout Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xDD0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .testTag("rotate_status_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = String.format("Rotation: %.1f°", totalAngle),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (abs(state.straightenAngle) > 0.05f) {
                    Text(
                        text = String.format(" (Tilt: %s%.1f°)", if (state.straightenAngle > 0) "+" else "", state.straightenAngle),
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLevel) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLevel) Color(0xFF10B981) else Color(0xFFF59E0B))
                ) {
                    Text(
                        text = if (isLevel) "Level" else "Tilted",
                        color = if (isLevel) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
