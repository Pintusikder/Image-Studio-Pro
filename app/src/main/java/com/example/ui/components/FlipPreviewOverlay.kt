package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

/**
 * Preview overlay for Flip tool.
 * Shows the flipped working bitmap with optional dashed reflection axis guidelines.
 */
@Composable
fun FlipPreviewOverlay(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.previewBitmap ?: state.workingBitmap ?: state.originalBitmap

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Flipped Image Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Optional Mirror Symmetry Axis Guides
            if (state.showFlipAxisGuide) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)

                    // Vertical Mirror Axis (Cyan)
                    drawLine(
                        color = Color(0x80000000),
                        start = Offset(w / 2f + 1f, 0f),
                        end = Offset(w / 2f + 1f, h),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF38BDF8).copy(alpha = 0.75f),
                        start = Offset(w / 2f, 0f),
                        end = Offset(w / 2f, h),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )

                    // Horizontal Mirror Axis (Amber)
                    drawLine(
                        color = Color(0x80000000),
                        start = Offset(0f, h / 2f + 1f),
                        end = Offset(w, h / 2f + 1f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                }
            }

            // Top Status Badges
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                if (state.isFlippedHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC0C4A6E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text(
                                text = "H-FLIPPED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                if (state.isFlippedVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC451A03),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                            Text(
                                text = "V-FLIPPED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
