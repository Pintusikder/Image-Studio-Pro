package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.BackgroundProcessor
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

/**
 * Interactive Preview Overlay for Background Tools (Request 27).
 * 
 * Features:
 * - Checkerboard canvas for Transparent background inspection
 * - Draggable Before/After Split wipe slider
 * - Hold-to-Compare original toggle
 * - Sample point indicator markers
 * - Privacy guarantee badge ("100% On-Device • No Cloud Uploads")
 */
@Composable
fun BackgroundPreviewOverlay(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val originalBitmap = state.workingBitmap
    val processedBitmap = state.previewBitmap ?: state.workingBitmap

    if (originalBitmap == null || processedBitmap == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No image available", color = Color(0xFF94A3B8))
        }
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("bg_preview_overlay"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val isHoldingCompare = state.isHoldingCompareOriginal
        val isSplitMode = state.showBeforeAfterSplit
        val isTransparentMode = state.bgConfig.mode == BackgroundProcessor.BgMode.TRANSPARENT

        // Background Layer: If in transparent mode, draw high-contrast checkerboard
        if (isTransparentMode && !isHoldingCompare) {
            CheckerboardCanvas(modifier = Modifier.fillMaxSize())
        }

        // Main Image Layer
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isHoldingCompare) {
                // Show raw original bitmap
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "Original Background",
                    modifier = Modifier.fillMaxSize()
                )

                // Holding Badge
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
                // Split Screen Before / After
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
                    // Base: Processed Bitmap
                    Image(
                        bitmap = processedBitmap.asImageBitmap(),
                        contentDescription = "Processed Background Preview",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay: Clipped Original Image on the Left
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val splitX = size.width * splitPos
                        val originalImage = originalBitmap.asImageBitmap()

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

                        clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                            drawImage(
                                image = originalImage,
                                dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
                                dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt())
                            )
                        }

                        // Divider Line
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Divider Knob
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
                            contentDescription = "Divider",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Badges
                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    ) {
                        Text(
                            text = "ORIGINAL",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color(0xCC0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    ) {
                        Text(
                            text = "NEW BG",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                // Standard single view
                Image(
                    bitmap = processedBitmap.asImageBitmap(),
                    contentDescription = "Processed Background Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Privacy / On-Device Shield Badge (Bottom Left)
        Surface(
            color = Color(0xDD0F172A),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "On-Device",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "100% On-Device Processing",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * High-contrast transparent checkerboard pattern.
 */
@Composable
fun CheckerboardCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = 16.dp.toPx()
        val numCols = (size.width / squareSize).toInt() + 1
        val numRows = (size.height / squareSize).toInt() + 1

        val darkColor = Color(0xFF1E293B)
        val lightColor = Color(0xFF0F172A)

        for (r in 0 until numRows) {
            for (c in 0 until numCols) {
                val color = if ((r + c) % 2 == 0) darkColor else lightColor
                drawRect(
                    color = color,
                    topLeft = Offset(c * squareSize, r * squareSize),
                    size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                )
            }
        }
    }
}
