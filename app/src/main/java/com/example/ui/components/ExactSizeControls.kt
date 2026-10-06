package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExactSizePreset
import com.example.model.ExactSizeUnit
import com.example.model.ResizeMode
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

@Composable
fun ExactSizeControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 1. Header & Current Dimensions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EXACT SIZE SPECIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                )
                val currentW = state.workingBitmap?.width ?: 0
                val currentH = state.workingBitmap?.height ?: 0
                Text(
                    text = "Current: $currentW × $currentH px (@ ${state.dpi} DPI)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            // Quick presets button / indicator
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Text(
                    text = "${state.exactSizeUnit.label} (${state.exactSizeUnit.symbol})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Unit Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExactSizeUnit.values().forEach { unit ->
                val selected = state.exactSizeUnit == unit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color(0xFF38BDF8) else Color.Transparent)
                        .clickable { viewModel.setExactSizeUnit(unit) }
                        .padding(vertical = 6.dp)
                        .testTag("exact_unit_${unit.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit.symbol.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Width, Height, and Aspect Lock Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Width Input
            OutlinedTextField(
                value = state.exactWidthInput,
                onValueChange = { viewModel.setExactWidthInput(it) },
                label = { Text("Width (${state.exactSizeUnit.symbol})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("exact_width_input")
            )

            // Aspect Ratio Lock Toggle
            IconButton(
                onClick = { viewModel.setExactAspectLocked(!state.exactAspectLocked) },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (state.exactAspectLocked) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF0F172A),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (state.exactAspectLocked) Color(0xFF38BDF8) else Color(0xFF334155),
                        RoundedCornerShape(8.dp)
                    )
                    .testTag("exact_aspect_lock_button")
            ) {
                Icon(
                    imageVector = if (state.exactAspectLocked) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = if (state.exactAspectLocked) "Aspect Ratio Locked" else "Aspect Ratio Unlocked",
                    tint = if (state.exactAspectLocked) Color(0xFF38BDF8) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Height Input
            OutlinedTextField(
                value = state.exactHeightInput,
                onValueChange = { viewModel.setExactHeightInput(it) },
                label = { Text("Height (${state.exactSizeUnit.symbol})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("exact_height_input")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Exact Resize / Fit Mode Chips
        Text(
            text = "FIT & RESIZE BEHAVIOR",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf(
                Pair(ResizeMode.FIT, "Fit (Letterbox/Pad)"),
                Pair(ResizeMode.FILL, "Fill (Center Crop)"),
                Pair(ResizeMode.STRETCH, "Stretch (Exact)"),
                Pair(ResizeMode.SMART_CROP, "Smart Focus Crop")
            )
            modes.forEach { (mode, label) ->
                val selected = state.exactSizeMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setExactSizeMode(mode) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("exact_mode_${mode.name.lowercase()}")
                )
            }
        }

        // Background matte color if FIT
        if (state.exactSizeMode == ResizeMode.FIT) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pad Color:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
                val colors = listOf(
                    Pair(android.graphics.Color.TRANSPARENT, "Clear"),
                    Pair(android.graphics.Color.WHITE, "White"),
                    Pair(android.graphics.Color.BLACK, "Black"),
                    Pair(android.graphics.Color.DKGRAY, "Dark Gray")
                )
                colors.forEach { (c, label) ->
                    val isCurrent = state.exactBgColor == c
                    Surface(
                        color = if (c == android.graphics.Color.TRANSPARENT) Color(0xFF1E293B) else Color(c),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isCurrent) Color(0xFF38BDF8) else Color(0xFF475569)),
                        modifier = Modifier
                            .clickable { viewModel.setExactBgColor(c) }
                            .padding(2.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (c == android.graphics.Color.WHITE) Color.Black else Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Quick Standard Presets
        Text(
            text = "STANDARD EXACT PRESETS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExactSizePreset.PRESETS.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyExactPreset(preset) },
                    label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.testTag("preset_${preset.name.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Exact Dimension Action Buttons (Exact Width, Exact Height, Exact Width x Height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val w = state.exactWidthInput.toIntOrNull() ?: state.workingBitmap?.width ?: 1080
                    viewModel.applyExactWidth(w)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_exact_width_button")
            ) {
                Text("Exact Width", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Button(
                onClick = {
                    val h = state.exactHeightInput.toIntOrNull() ?: state.workingBitmap?.height ?: 1080
                    viewModel.applyExactHeight(h)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("apply_exact_height_button")
            ) {
                Text("Exact Height", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Apply Exact Size (Width × Height) Button
        Button(
            onClick = { viewModel.applyExactSize() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_exact_size_button")
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Apply Exact Size (Width × Height)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            )
        }

        // 7. Output Verification Status
        state.lastResizeReport?.let { report ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (report.isValid) Color(0x2210B981) else Color(0x22EF4444)
                ),
                border = BorderStroke(
                    1.dp,
                    if (report.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (report.isValid) "✓ RESIZE VERIFIED" else "⚠ RESIZE WARNING",
                            color = if (report.isValid) Color(0xFF34D399) else Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${report.actualWidth} × ${report.actualHeight} px",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode: ${report.mode.label} | Non-empty: ${report.isNonEmpty} | Memory: ${report.memoryFootprintBytes / 1024} KB",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
