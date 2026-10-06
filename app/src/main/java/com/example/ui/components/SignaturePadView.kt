package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun SignaturePadView(
    strokes: List<SignatureStroke>,
    onAddStroke: (SignatureStroke) -> Unit,
    currentColor: Color,
    currentStrokeWidth: Float,
    backgroundColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val currentPoints = remember { mutableStateListOf<Offset>() }

    Box(modifier = modifier.background(backgroundColor)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentColor, currentStrokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                onAddStroke(
                                    SignatureStroke(
                                        points = currentPoints.toList(),
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth
                                    )
                                )
                                currentPoints.clear()
                            }
                        },
                        onDragCancel = {
                            currentPoints.clear()
                        }
                    )
                }
        ) {
            // Draw baseline guideline
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(0f, size.height * 0.75f),
                end = Offset(size.width, size.height * 0.75f),
                strokeWidth = 2.dp.toPx()
            )

            // Draw historical strokes
            strokes.forEach { stroke ->
                if (stroke.points.size > 1) {
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (stroke.points.size == 1) {
                    drawCircle(
                        color = stroke.color,
                        radius = stroke.strokeWidth / 2f,
                        center = stroke.points[0]
                    )
                }
            }

            // Draw currently active stroke
            if (currentPoints.size > 1) {
                val activePath = androidx.compose.ui.graphics.Path()
                activePath.moveTo(currentPoints[0].x, currentPoints[0].y)
                for (i in 1 until currentPoints.size) {
                    activePath.lineTo(currentPoints[i].x, currentPoints[i].y)
                }
                drawPath(
                    path = activePath,
                    color = currentColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (currentPoints.size == 1) {
                drawCircle(
                    color = currentColor,
                    radius = currentStrokeWidth / 2f,
                    center = currentPoints[0]
                )
            }
        }
    }
}

object SignatureBitmapGenerator {
    fun renderStrokesToBitmap(
        strokes: List<SignatureStroke>,
        width: Int,
        height: Int,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Bitmap {
        val safeW = if (width > 0) width else 800
        val safeH = if (height > 0) height else 400
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            paint.strokeWidth = stroke.strokeWidth
            paint.color = android.graphics.Color.argb(
                (stroke.color.alpha * 255).toInt(),
                (stroke.color.red * 255).toInt(),
                (stroke.color.green * 255).toInt(),
                (stroke.color.blue * 255).toInt()
            )

            if (stroke.points.size > 1) {
                val path = Path()
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                }
                canvas.drawPath(path, paint)
            } else if (stroke.points.size == 1) {
                canvas.drawCircle(stroke.points[0].x, stroke.points[0].y, stroke.strokeWidth / 2f, paint)
            }
        }

        return bitmap
    }
}
