package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.AspectRatioApplyMode
import com.example.processing.AspectRatioEngine
import com.example.ui.viewmodel.StudioUiState
import kotlin.math.abs
import kotlin.math.min

@Composable
fun AspectRatioPreviewOverlay(
    bitmap: Bitmap,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val analysis = remember(bitmap.width, bitmap.height, state.dpi) {
        AspectRatioEngine.analyzeAspectRatio(bitmap.width, bitmap.height, state.dpi)
    }

    val (ratioX, ratioY) = if (state.isCustomAspectSelected) {
        Pair(state.customAspectX, state.customAspectY)
    } else {
        Pair(state.activeAspectPreset.ratioX, state.activeAspectPreset.ratioY)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Render base bitmap
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Aspect Ratio Preview",
            modifier = Modifier.fillMaxSize()
        )

        // Draw guideline overlay for target aspect ratio
        if (ratioX > 0f && ratioY > 0f && state.aspectApplyMode != AspectRatioApplyMode.INTERACTIVE_CROP) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val containerRatio = size.width / size.height

                // Calculate displayed image bounds inside Canvas
                val displayedW: Float
                val displayedH: Float
                val imgLeft: Float
                val imgTop: Float

                if (containerRatio > imgRatio) {
                    displayedH = size.height
                    displayedW = size.height * imgRatio
                    imgLeft = (size.width - displayedW) / 2f
                    imgTop = 0f
                } else {
                    displayedW = size.width
                    displayedH = size.width / imgRatio
                    imgLeft = 0f
                    imgTop = (size.height - displayedH) / 2f
                }

                val targetRatio = ratioX / ratioY

                if (state.aspectApplyMode == AspectRatioApplyMode.CROP) {
                    // Show crop framing lines
                    if (abs(imgRatio - targetRatio) > 0.005f) {
                        val cropW: Float
                        val cropH: Float
                        val cropLeft: Float
                        val cropTop: Float

                        if (imgRatio > targetRatio) {
                            cropH = displayedH
                            cropW = displayedH * targetRatio
                            cropLeft = imgLeft + (displayedW - cropW) / 2f
                            cropTop = imgTop
                        } else {
                            cropW = displayedW
                            cropH = displayedW / targetRatio
                            cropLeft = imgLeft
                            cropTop = imgTop + (displayedH - cropH) / 2f
                        }

                        // Dim out-of-crop areas
                        // Top
                        drawRect(Color(0x99000000), Offset(0f, 0f), Size(size.width, cropTop))
                        // Bottom
                        drawRect(Color(0x99000000), Offset(0f, cropTop + cropH), Size(size.width, size.height - (cropTop + cropH)))
                        // Left
                        drawRect(Color(0x99000000), Offset(0f, cropTop), Size(cropLeft, cropH))
                        // Right
                        drawRect(Color(0x99000000), Offset(cropLeft + cropW, cropTop), Size(size.width - (cropLeft + cropW), cropH))

                        // Draw cyan crop frame border
                        drawRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(cropLeft, cropTop),
                            size = Size(cropW, cropH),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // Draw corner brackets
                        val bracketLen = min(cropW, cropH) * 0.12f
                        val bracketStroke = 4.dp.toPx()
                        val bracketColor = Color(0xFF38BDF8)

                        // Top-left
                        drawLine(bracketColor, Offset(cropLeft, cropTop), Offset(cropLeft + bracketLen, cropTop), bracketStroke)
                        drawLine(bracketColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + bracketLen), bracketStroke)

                        // Top-right
                        drawLine(bracketColor, Offset(cropLeft + cropW, cropTop), Offset(cropLeft + cropW - bracketLen, cropTop), bracketStroke)
                        drawLine(bracketColor, Offset(cropLeft + cropW, cropTop), Offset(cropLeft + cropW, cropTop + bracketLen), bracketStroke)

                        // Bottom-left
                        drawLine(bracketColor, Offset(cropLeft, cropTop + cropH), Offset(cropLeft + bracketLen, cropTop + cropH), bracketStroke)
                        drawLine(bracketColor, Offset(cropLeft, cropTop + cropH), Offset(cropLeft, cropTop + cropH - bracketLen), bracketStroke)

                        // Bottom-right
                        drawLine(bracketColor, Offset(cropLeft + cropW, cropTop + cropH), Offset(cropLeft + cropW - bracketLen, cropTop + cropH), bracketStroke)
                        drawLine(bracketColor, Offset(cropLeft + cropW, cropTop + cropH), Offset(cropLeft + cropW, cropTop + cropH - bracketLen), bracketStroke)
                    }
                }
            }
        }

        // Floating Info Pill at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color(0xDD0F172A), RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${bitmap.width} × ${bitmap.height} px • ${analysis.simplifiedRatioString} (${String.format("%.2f:1", analysis.decimalRatio)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
