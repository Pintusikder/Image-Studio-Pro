package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CropAspectRatio
import com.example.processing.CropGuideGrid
import com.example.processing.CropShape
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

@Composable
fun CropControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val workingBitmap = state.workingBitmap
    val bmpW = workingBitmap?.width ?: 1
    val bmpH = workingBitmap?.height ?: 1

    val currentCropW = ((state.activeCropBounds.right - state.activeCropBounds.left) * bmpW).roundToInt().coerceAtLeast(1)
    val currentCropH = ((state.activeCropBounds.bottom - state.activeCropBounds.top) * bmpH).roundToInt().coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Crop Resolution Header & Aspect Inversion
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OUTPUT RESOLUTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$currentCropW × $currentCropH px",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = String.format("%.2f MP • %s", (currentCropW * currentCropH) / 1_000_000f, state.selectedCropRatio.title),
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Invert Ratio (Landscape <-> Portrait)
                    if (state.selectedCropRatio.ratioX != null && state.selectedCropRatio.ratioY != null) {
                        IconButton(
                            onClick = { viewModel.invertCropRatio() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Invert Aspect Ratio",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Smart Saliency Auto-framing
                    IconButton(
                        onClick = { viewModel.applySmartCrop() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Smart Saliency Focus",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reset crop
                    IconButton(
                        onClick = { viewModel.resetCropBounds() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Crop Window",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Aspect Ratio Presets
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASPECT RATIO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                )
                Text(
                    text = state.selectedCropRatio.title,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CropAspectRatio.DEFAULT_LIST.forEach { ratio ->
                    val selected = state.selectedCropRatio == ratio
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCropRatio(ratio) },
                        label = { Text(ratio.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // 3. Shape Mask Selection
        Column {
            Text(
                text = "CROP SHAPE MASK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CropShape.values().forEach { shape ->
                    val selected = state.cropShape == shape
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCropShape(shape) },
                        label = { Text(shape.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // If Rounded Rectangle is selected, show corner radius slider
            AnimatedVisibility(visible = state.cropShape == CropShape.ROUNDED_RECT) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Corner Radius", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text("${state.cropCornerRadiusPx.roundToInt()} px", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.cropCornerRadiusPx,
                        onValueChange = { viewModel.setCropCornerRadius(it) },
                        valueRange = 4f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("slider_crop_corner_radius")
                    )
                }
            }

            // Decorative Border Outline Slider (Phase 10)
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Shape Frame Border", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text("${state.cropBorderWidthPx.roundToInt()} px", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = state.cropBorderWidthPx,
                    onValueChange = { viewModel.setCropBorderWidth(it) },
                    valueRange = 0f..32f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("slider_crop_border_width")
                )
            }
        }

        // 4. Composition Guide Overlay Selection
        Column {
            Text(
                text = "COMPOSITION GUIDE GRID",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CropGuideGrid.values().forEach { grid ->
                    val selected = state.cropGuideGrid == grid
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCropGuideGrid(grid) },
                        label = { Text(grid.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // 5. Verification Badge (shown if crop was previously performed)
        state.lastCropReport?.let { report ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (report.isValid) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (report.isValid) Color(0xFF10B981) else Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (report.isValid) Icons.Default.Verified else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (report.isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = if (report.isValid) "Crop Output Verified" else "Crop Verification Alert",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Text(
                            text = report.summary,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // 6. Action Button: Apply Crop
        Button(
            onClick = { viewModel.applyCrop() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_crop_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply Professional Crop",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            )
        }
    }
}
