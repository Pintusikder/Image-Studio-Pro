package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.AspectRatioApplyMode
import com.example.processing.AspectRatioEngine
import com.example.processing.AspectRatioPreset
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspectRatioControls(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val workingBitmap = state.workingBitmap

    // Current Image Analysis
    val currentAnalysis = remember(workingBitmap?.width, workingBitmap?.height, state.dpi) {
        if (workingBitmap != null) {
            AspectRatioEngine.analyzeAspectRatio(workingBitmap.width, workingBitmap.height, state.dpi)
        } else {
            AspectRatioEngine.analyzeAspectRatio(1920, 1080, 300)
        }
    }

    var showCalculatorSection by remember { mutableStateOf(true) }
    var showCustomRatioSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ==========================================
        // 1. CURRENT DIMENSIONS & ASPECT RATIO CARD
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_current_aspect_ratio"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CURRENT IMAGE RATIO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = currentAnalysis.orientation.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // 3 Core Metric Tiles: Width, Height, Aspect Ratio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Width Tile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_current_width"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "WIDTH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentAnalysis.width}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = "px",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                            )
                        }
                    }

                    // Height Tile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tile_current_height"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "HEIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentAnalysis.height}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = "px",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                            )
                        }
                    }

                    // Aspect Ratio Tile
                    Card(
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("tile_current_aspect_ratio"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0369A1).copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ASPECT RATIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentAnalysis.simplifiedRatioString,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = String.format("%.3f:1", currentAnalysis.decimalRatio),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                // Sub-stats: Megapixels, standard match, print size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${String.format("%.2f MP", currentAnalysis.megapixels)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )

                    val matchText = if (currentAnalysis.nearestPreset != null && currentAnalysis.variancePercent < 1.0f) {
                        "~${currentAnalysis.nearestPreset.name}"
                    } else {
                        "Custom Proportion"
                    }
                    Text(
                        text = "• $matchText",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontSize = 11.sp)
                    )

                    Text(
                        text = "• ${String.format("%.1f×%.1f in @ 300DPI", currentAnalysis.widthInchesAt300Dpi, currentAnalysis.heightInchesAt300Dpi)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                }
            }
        }

        // ==========================================
        // 2. RATIO LOCK & ORIENTATION CONTROLS
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (state.aspectRatioLocked) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.aspectRatioLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (state.aspectRatioLocked) "Ratio Locked" else "Ratio Unlocked",
                                tint = if (state.aspectRatioLocked) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (state.aspectRatioLocked) "Aspect Ratio Locked" else "Aspect Ratio Unlocked",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.aspectRatioLocked) Color(0xFF34D399) else Color(0xFFFBBF24)
                                )
                            )
                            Text(
                                text = if (state.aspectRatioLocked) "Width & Height scale proportionally" else "Dimensions adjust independently",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                            )
                        }
                    }

                    Switch(
                        checked = state.aspectRatioLocked,
                        onCheckedChange = { viewModel.setAspectRatioLock(it) },
                        modifier = Modifier.testTag("aspect_ratio_lock_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF34D399),
                            checkedTrackColor = Color(0xFF065F46),
                            uncheckedThumbColor = Color(0xFFFBBF24),
                            uncheckedTrackColor = Color(0xFF451A03)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // Quick Invert / Flip Orientation button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeRatioStr = if (state.isCustomAspectSelected) {
                        "${state.customAspectXInput}:${state.customAspectYInput}"
                    } else {
                        state.activeAspectPreset.displayRatio
                    }

                    Text(
                        text = "Active Target: $activeRatioStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium
                        )
                    )

                    OutlinedButton(
                        onClick = { viewModel.swapAspectOrientation() },
                        modifier = Modifier.testTag("btn_swap_orientation"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF38BDF8)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Invert Orientation",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flip W ⇄ H", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // ==========================================
        // 3. PRESETS & CATEGORIES
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASPECT RATIO PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                )

                Text(
                    text = if (showCustomRatioSection) "Hide Custom" else "+ Custom Ratio",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clickable { showCustomRatioSection = !showCustomRatioSection }
                        .padding(4.dp)
                )
            }

            // Category Filter Scroll Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioEngine.CATEGORIES.forEach { category ->
                    val isSelected = state.aspectCategoryFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setAspectCategoryFilter(category) },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFF334155),
                            selectedBorderColor = Color(0xFF38BDF8)
                        )
                    )
                }
            }

            // Presets Chips Flow
            val filteredPresets = remember(state.aspectCategoryFilter) {
                if (state.aspectCategoryFilter == "All") {
                    AspectRatioEngine.ALL_PRESETS
                } else {
                    AspectRatioEngine.ALL_PRESETS.filter { it.category == state.aspectCategoryFilter }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredPresets.forEach { preset ->
                    val isSelected = !state.isCustomAspectSelected && state.activeAspectPreset.id == preset.id
                    Card(
                        modifier = Modifier
                            .testTag("preset_${preset.id}")
                            .clickable {
                                viewModel.setAspectPreset(preset)
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.displayRatio,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = preset.name.substringBefore("(").trim(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. CUSTOM RATIO SECTION (COLLAPSIBLE)
        // ==========================================
        AnimatedVisibility(visible = showCustomRatioSection || state.isCustomAspectSelected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_custom_aspect_ratio"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (state.isCustomAspectSelected) Color(0xFF38BDF8) else Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM ASPECT RATIO",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )

                        if (state.isCustomAspectSelected) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0284C7), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ratio X Input
                        OutlinedTextField(
                            value = state.customAspectXInput,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() || it == '.' }
                                val x = cleaned.toFloatOrNull() ?: 1f
                                viewModel.setCustomAspect(x, state.customAspectY, cleaned, state.customAspectYInput)
                            },
                            label = { Text("Ratio X", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_custom_ratio_x"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        )

                        // Ratio Y Input
                        OutlinedTextField(
                            value = state.customAspectYInput,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() || it == '.' }
                                val y = cleaned.toFloatOrNull() ?: 1f
                                viewModel.setCustomAspect(state.customAspectX, y, state.customAspectXInput, cleaned)
                            },
                            label = { Text("Ratio Y", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_custom_ratio_y"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }

                    // Common Quick Ratios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("18:9", Pair(18f, 9f)),
                            Pair("2.39:1", Pair(2.39f, 1f)),
                            Pair("1.85:1", Pair(1.85f, 1f)),
                            Pair("5:4", Pair(5f, 4f))
                        ).forEach { (label, pair) ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.setCustomAspect(pair.first, pair.second, pair.first.toString(), pair.second.toString())
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                            ) {
                                Text(label, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. RATIO CALCULATOR & DIMENSION SOLVER
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_ratio_calculator"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RATIO CALCULATOR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Text(
                        text = if (showCalculatorSection) "Collapse" else "Expand",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B)),
                        modifier = Modifier
                            .clickable { showCalculatorSection = !showCalculatorSection }
                            .padding(4.dp)
                    )
                }

                AnimatedVisibility(visible = showCalculatorSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Solve Dimensions (Auto-updates linked dimension):",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                        )

                        // Dimension Solver Input Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.calcDimensionWidthInput,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isDigit() }
                                    val w = cleaned.toIntOrNull() ?: 1
                                    viewModel.setCalcWidth(w, cleaned)
                                },
                                label = { Text("Width (px)", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_input_width"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )

                            Icon(
                                imageVector = if (state.aspectRatioLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (state.aspectRatioLocked) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )

                            OutlinedTextField(
                                value = state.calcDimensionHeightInput,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isDigit() }
                                    val h = cleaned.toIntOrNull() ?: 1
                                    viewModel.setCalcHeight(h, cleaned)
                                },
                                label = { Text("Height (px)", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calc_input_height"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                        }

                        // Quick Scale Multipliers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.25f, 1.5f, 2.0f).forEach { scale ->
                                OutlinedButton(
                                    onClick = { viewModel.applyCalcScale(scale) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0))
                                ) {
                                    Text("${scale}×", fontSize = 10.sp)
                                }
                            }
                        }

                        // Standard Resolutions for Active Ratio
                        if (!state.isCustomAspectSelected && state.activeAspectPreset.standardResolutions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Standard Resolutions for ${state.activeAspectPreset.name}:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.activeAspectPreset.standardResolutions.forEach { res ->
                                        Card(
                                            modifier = Modifier
                                                .clickable { viewModel.setCalcStandardResolution(res) },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                            border = BorderStroke(1.dp, Color(0xFF334155))
                                        ) {
                                            Text(
                                                text = res.label,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFFE2E8F0),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                        // Arbitrary Aspect Ratio Finder / Calculator
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Analyze Any Custom Resolution:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = state.ratioCalcWInput,
                                    onValueChange = { viewModel.setRatioCalcWInput(it.filter { c -> c.isDigit() }) },
                                    label = { Text("Custom W", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Text("×", color = Color(0xFF94A3B8))
                                OutlinedTextField(
                                    value = state.ratioCalcHInput,
                                    onValueChange = { viewModel.setRatioCalcHInput(it.filter { c -> c.isDigit() }) },
                                    label = { Text("Custom H", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            // Dynamic analysis of the user's arbitrary dimensions
                            val calcW = state.ratioCalcWInput.toIntOrNull() ?: 1920
                            val calcH = state.ratioCalcHInput.toIntOrNull() ?: 1080
                            val arbitraryAnalysis = remember(calcW, calcH) {
                                AspectRatioEngine.analyzeAspectRatio(calcW, calcH)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Ratio: ${arbitraryAnalysis.simplifiedRatioString}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                        Text(
                                            text = "Decimal: ${String.format("%.3f:1", arbitraryAnalysis.decimalRatio)} • GCD: ${arbitraryAnalysis.gcd}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setCustomAspect(
                                                arbitraryAnalysis.simplifiedX.toFloat(),
                                                arbitraryAnalysis.simplifiedY.toFloat(),
                                                arbitraryAnalysis.simplifiedX.toString(),
                                                arbitraryAnalysis.simplifiedY.toString()
                                            )
                                            Toast.makeText(context, "Applied ${arbitraryAnalysis.simplifiedRatioString} to tool", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                                    ) {
                                        Text("Use This", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. TRANSFORMATION MODE SELECTION
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "APPLICATION METHOD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AspectRatioApplyMode.values().forEach { mode ->
                    val isSelected = state.aspectApplyMode == mode
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_${mode.name}")
                            .clickable { viewModel.setAspectApplyMode(mode) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    AspectRatioApplyMode.CROP -> Icons.Default.Crop
                                    AspectRatioApplyMode.INTERACTIVE_CROP -> Icons.Default.Tune
                                    AspectRatioApplyMode.PAD_LETTERBOX -> Icons.Default.FitScreen
                                    AspectRatioApplyMode.RESIZE_SCALE -> Icons.Default.PhotoSizeSelectLarge
                                },
                                contentDescription = mode.label,
                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Description of active method
            Text(
                text = state.aspectApplyMode.description,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
            )

            // Padding Color Options if PAD_LETTERBOX selected
            if (state.aspectApplyMode == AspectRatioApplyMode.PAD_LETTERBOX) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pad Color:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    )
                    listOf(
                        Pair("White", android.graphics.Color.WHITE),
                        Pair("Black", android.graphics.Color.BLACK),
                        Pair("Slate", 0xFF0F172A.toInt()),
                        Pair("Transparent", android.graphics.Color.TRANSPARENT)
                    ).forEach { (colorName, colorVal) ->
                        val isSelected = state.aspectPadColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    2.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569),
                                    CircleShape
                                )
                                .clickable { viewModel.setAspectPadColor(colorVal) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorVal == android.graphics.Color.WHITE) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. PRIMARY ACTION BUTTON
        // ==========================================
        Button(
            onClick = {
                viewModel.applyAspectRatioTransformation { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_apply_aspect_ratio"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply ${state.aspectApplyMode.label}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}
