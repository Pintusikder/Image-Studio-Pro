package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BiometricGuideOverlay(
    modifier: Modifier = Modifier,
    guideColor: Color = Color(0xFF38BDF8), // Cyan
    showFaceOval: Boolean = true,
    showCrownChin: Boolean = true,
    showEyeLine: Boolean = true,
    showCenterAxis: Boolean = true,
    showGrid: Boolean = false
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f

        val strokeWidth = 2.dp.toPx()
        val thinStroke = 1.dp.toPx()
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

        // 3x3 Rule of thirds alignment grid
        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.25f)
            // Vertical grid lines
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), thinStroke)
            drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), thinStroke)
            // Horizontal grid lines
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), thinStroke)
            drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), thinStroke)
        }

        // Center vertical alignment line (Symmetry axis)
        if (showCenterAxis) {
            drawLine(
                color = guideColor.copy(alpha = 0.55f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, h),
                strokeWidth = thinStroke,
                pathEffect = dashedEffect
            )
        }

        // Crown guideline (approx 12-15% from top)
        val crownY = h * 0.12f
        if (showCrownChin) {
            drawLine(
                color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                start = Offset(w * 0.18f, crownY),
                end = Offset(w * 0.82f, crownY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashedEffect
            )
        }

        // Eye line guideline (approx 44% from top)
        val eyeY = h * 0.44f
        if (showEyeLine) {
            drawLine(
                color = guideColor.copy(alpha = 0.85f),
                start = Offset(w * 0.10f, eyeY),
                end = Offset(w * 0.90f, eyeY),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Chin guideline (approx 74% from top)
        val chinY = h * 0.74f
        if (showCrownChin) {
            drawLine(
                color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                start = Offset(w * 0.18f, chinY),
                end = Offset(w * 0.82f, chinY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashedEffect
            )
        }

        // Biometric Face Oval Outline (Head-positioning guide)
        if (showFaceOval) {
            val ovalW = w * 0.54f
            val ovalH = h * 0.62f
            val ovalLeft = centerX - (ovalW / 2f)
            val ovalTop = crownY

            drawOval(
                color = guideColor,
                topLeft = Offset(ovalLeft, ovalTop),
                size = Size(ovalW, ovalH),
                style = Stroke(width = strokeWidth)
            )

            // Inner Face Feature guideline (nose/mouth reference)
            val innerW = w * 0.32f
            val innerH = h * 0.36f
            drawOval(
                color = guideColor.copy(alpha = 0.35f),
                topLeft = Offset(centerX - (innerW / 2f), eyeY - (innerH * 0.2f)),
                size = Size(innerW, innerH),
                style = Stroke(width = thinStroke, pathEffect = dashedEffect)
            )
        }
    }
}

