package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.ImageEnhancer
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

/**
 * High-performance interactive preview overlay for Image Enhancement (Request 26).
 * 
 * Includes:
 * - Interactive Draggable Split-Screen Slider (Before / After wipe divider)
 * - Hold-to-Compare instant switch overlay
 * - Real-time RGB & Luminance Histogram HUD
 * - Visual exposure clipping alerts
 */
@Composable
fun EnhancePreviewOverlay(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val originalBitmap = state.workingBitmap
    val enhancedBitmap = state.previewBitmap ?: state.workingBitmap

    if (originalBitmap == null || enhancedBitmap == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No image available", color = Color(0xFF94A3B8))
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("enhance_preview_overlay"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val isHoldingCompare = state.isHoldingCompareOriginal
        val isSplitMode = state.showBeforeAfterSplit

        // Main Image Display Area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isHoldingCompare) {
                // Showing Pure Original while holding
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "Original Unenhanced Image",
                    modifier = Modifier.fillMaxSize()
                )

                // Holding Badge Overlay
                Surface(
                    color = Color(0xDD0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ORIGINAL IMAGE (HOLDING)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            } else if (isSplitMode) {
                // Interactive Before/After Split Screen Slider
                val splitPos = state.beforeAfterSplitPosition

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newPos = (state.beforeAfterSplitPosition + dragAmount.x / size.width.toFloat())
                                viewModel.setBeforeAfterSplitPosition(newPos)
                            }
                        }
                ) {
                    // Layer 1: Full Enhanced Bitmap
                    Image(
                        bitmap = enhancedBitmap.asImageBitmap(),
                        contentDescription = "Enhanced Preview",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Layer 2: Clipped Original Bitmap on the Left
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val splitX = size.width * splitPos
                        val originalImage = originalBitmap.asImageBitmap()

                        // Calculate destination rect preserving aspect ratio (Fit Inside)
                        val imgRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val canvasRatio = size.width / size.height

                        val (dstW, dstH) = if (canvasRatio > imgRatio) {
                            val h = size.height
                            val w = h * imgRatio
                            Pair(w, h)
                        } else {
                            val w = size.width
                            val h = w / imgRatio
                            Pair(w, h)
                        }

                        val dstLeft = (size.width - dstW) / 2f
                        val dstTop = (size.height - dstH) / 2f
                        val dstRect = Rect(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH)

                        // Clip left portion up to splitX
                        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                            drawImage(
                                image = originalImage,
                                dstOffset = androidx.compose.ui.unit.IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(dstW.roundToInt(), dstH.roundToInt())
                            )
                        }

                        // Draw Divider Line
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, size.height),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Draw subtle shadow for divider
                        drawLine(
                            color = Color(0x66000000),
                            start = Offset(splitX + 2.dp.toPx(), 0f),
                            end = Offset(splitX + 2.dp.toPx(), size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Floating Center Handle Knob
                    val knobOffsetX = (containerWidth.value * splitPos).dp - 18.dp
                    Box(
                        modifier = Modifier
                            .offset(x = knobOffsetX)
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, Color(0xFF38BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Divider Handle",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Left "BEFORE" Badge
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "BEFORE (ORIGINAL)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Right "AFTER" Badge
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "AFTER (ENHANCED)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Standard Single Preview
                Image(
                    bitmap = enhancedBitmap.asImageBitmap(),
                    contentDescription = "Enhanced Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top-Right Live Histogram HUD
        AnimatedVisibility(
            visible = state.showEnhanceHistogram && state.histogramData != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            state.histogramData?.let { hist ->
                HistogramHud(
                    histogram = hist,
                    onDismiss = { viewModel.toggleEnhanceHistogram() }
                )
            }
        }
    }
}

/**
 * Real-time RGB & Luminance Histogram Mini HUD.
 */
@Composable
fun HistogramHud(
    histogram: ImageEnhancer.HistogramData,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xE60F172A),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .width(170.dp)
            .testTag("enhance_histogram_hud")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Histogram",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "HISTOGRAM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                // Legend Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Graph Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF020617))
            ) {
                val maxC = histogram.maxCount.toFloat().coerceAtLeast(1f)
                val w = size.width
                val h = size.height
                val stepX = w / 255f

                // Draw Red curve
                val redPath = Path()
                redPath.moveTo(0f, h)
                for (i in 0..255) {
                    val y = h - (histogram.red[i] / maxC) * h
                    redPath.lineTo(i * stepX, y)
                }
                redPath.lineTo(w, h)
                redPath.close()
                drawPath(redPath, color = Color(0x44EF4444))

                // Draw Green curve
                val greenPath = Path()
                greenPath.moveTo(0f, h)
                for (i in 0..255) {
                    val y = h - (histogram.green[i] / maxC) * h
                    greenPath.lineTo(i * stepX, y)
                }
                greenPath.lineTo(w, h)
                greenPath.close()
                drawPath(greenPath, color = Color(0x4410B981))

                // Draw Blue curve
                val bluePath = Path()
                bluePath.moveTo(0f, h)
                for (i in 0..255) {
                    val y = h - (histogram.blue[i] / maxC) * h
                    bluePath.lineTo(i * stepX, y)
                }
                bluePath.lineTo(w, h)
                bluePath.close()
                drawPath(bluePath, color = Color(0x443B82F6))

                // Draw Luminance curve in White/Silver
                val lumPath = Path()
                for (i in 0..255) {
                    val x = i * stepX
                    val y = h - (histogram.luminance[i] / maxC) * h
                    if (i == 0) lumPath.moveTo(x, y) else lumPath.lineTo(x, y)
                }
                drawPath(
                    path = lumPath,
                    color = Color(0xCCF8FAFC),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Shadows", color = Color(0xFF64748B), fontSize = 8.sp)
                Text("Midtones", color = Color(0xFF64748B), fontSize = 8.sp)
                Text("Highlights", color = Color(0xFF64748B), fontSize = 8.sp)
            }
        }
    }
}
