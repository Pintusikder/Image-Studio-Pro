package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.RotateStraightenEngine
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Control panel for Rotate & Straighten Tool (Request 23).
 * Supports:
 * - 90° clockwise
 * - 90° counter-clockwise
 * - 180°
 * - Custom angle (-180° to +180° with direct numeric entry)
 * - Auto orientation (EXIF + Smart Horizon analysis)
 * - Manual straighten (-45° to +45° with 0.1° sensitivity)
 * - Grid (Rule of Thirds, Fine Alignment, Dense Mesh, Golden Ratio)
 * - Horizon guide (Draggable, spirit level meter)
 */
@Composable
fun RotateStraightenControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val totalAngle = state.rotationAngle + state.straightenAngle
    val isLevel = abs(totalAngle % 90f) <= 0.25f || abs(state.straightenAngle) <= 0.25f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rotate_straighten_controls")
    ) {
        // 1. Status Banner & Quick Reset
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
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
                        text = "TOTAL ROTATION ANGLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("%.1f°", totalAngle),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLevel) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (isLevel) Color(0xFF10B981) else Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = if (isLevel) "✓ Level" else "Tilted",
                                color = if (isLevel) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (abs(totalAngle) > 0.05f) {
                    OutlinedButton(
                        onClick = { viewModel.resetRotationAndStraighten() },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("btn_reset_rotation")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset 0°", fontSize = 12.sp)
                    }
                }
            }
        }

        // Informational feedback banner if orientation action occurred
        if (state.lastOrientationMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2537)),
                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.lastOrientationMessage,
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. QUICK ROTATE ACTIONS (90° CW, 90° CCW, 180°, Flip H, Flip V)
        Text(
            text = "QUICK ROTATE & FLIP",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.rotate90Cw() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rotate_90_cw")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("90° CW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = { viewModel.rotate90Ccw() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rotate_90_ccw")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("90° CCW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = { viewModel.rotate180() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rotate_180")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("180°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Flip Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.flip(horizontal = true, vertical = false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_flip_horizontal")
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip Horizontal", color = Color(0xFFE2E8F0), fontSize = 12.sp)
            }
            Button(
                onClick = { viewModel.flip(horizontal = false, vertical = true) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_flip_vertical")
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip Vertical", color = Color(0xFFE2E8F0), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. AUTO ORIENTATION & SMART HORIZON
        Text(
            text = "AUTO ORIENTATION",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Orientation & Straighten",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Normalizes EXIF orientation tag + detects horizon tilt automatically",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.autoOrientImage() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        enabled = !state.isDetectingHorizon,
                        modifier = Modifier.testTag("btn_auto_orient")
                    ) {
                        if (state.isDetectingHorizon) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Horizon detection probe button
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.detectHorizon() },
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        enabled = !state.isDetectingHorizon,
                        modifier = Modifier.testTag("btn_detect_horizon")
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detect Horizon Tilt", fontSize = 11.sp)
                    }

                    if (state.detectedHorizon != null) {
                        val det = state.detectedHorizon
                        Button(
                            onClick = { viewModel.levelHorizonToZero() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.testTag("btn_level_horizon_snap")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(String.format("Level (%.1f°)", det.angleDegrees), fontSize = 11.sp)
                        }
                    }
                }

                if (state.detectedHorizon != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.detectedHorizon.description,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. MANUAL STRAIGHTEN (-45° to +45°)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUAL STRAIGHTEN",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            )
            Text(
                text = String.format("%s%.1f°", if (state.straightenAngle > 0) "+" else "", state.straightenAngle),
                color = if (abs(state.straightenAngle) <= 0.25f) Color(0xFF34D399) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Precision slider
                Slider(
                    value = state.straightenAngle,
                    onValueChange = { viewModel.setStraightenAngle(it) },
                    valueRange = -45f..45f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (abs(state.straightenAngle) <= 0.25f) Color(0xFF34D399) else Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF0284C7),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("slider_manual_straighten")
                )

                // Micro-step nudge chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5f, -1f, -0.1f, 0f, 0.1f, 1f, 5f).forEach { step ->
                        val isZero = step == 0f
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isZero && abs(state.straightenAngle) <= 0.05f) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFF0F172A),
                            border = BorderStroke(
                                1.dp,
                                if (isZero && abs(state.straightenAngle) <= 0.05f) Color(0xFF34D399) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .clickable {
                                    if (isZero) viewModel.setStraightenAngle(0f) else viewModel.nudgeStraightenAngle(step)
                                }
                                .testTag("step_straighten_${step}")
                        ) {
                            val label = when {
                                step == 0f -> "0.0° Level"
                                step > 0f -> "+$step°"
                                else -> "$step°"
                            }
                            Text(
                                text = label,
                                color = if (isZero) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. CUSTOM ROTATION ANGLE (-180° to +180°)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CUSTOM ROTATION ANGLE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            )
            Text(
                text = String.format("%.1f°", state.rotationAngle),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Direct numeric angle input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = state.customAngleInput,
                        onValueChange = { viewModel.setCustomAngleInput(it) },
                        label = { Text("Exact Angle (°)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_custom_angle")
                    )

                    Button(
                        onClick = { viewModel.setCustomRotationAngle(0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("0°", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom angle slider (-180° to +180°)
                Slider(
                    value = state.rotationAngle,
                    onValueChange = { viewModel.setCustomRotationAngle(it) },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF0284C7),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("slider_custom_angle")
                )

                // Quick Angle Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f, 45f, 90f, 135f, 180f, -45f, -90f, -135f).forEach { presetAngle ->
                        val isSelected = abs(state.rotationAngle - presetAngle) < 0.1f
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCustomRotationAngle(presetAngle) },
                            label = { Text("${presetAngle.toInt()}°", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. STRAIGHTEN CROP MODE
        Text(
            text = "STRAIGHTEN CROP BEHAVIOR",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RotateStraightenEngine.StraightenCropMode.values().forEach { mode ->
                val isSelected = state.straightenCropMode == mode
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setStraightenCropMode(mode) }
                        .testTag("mode_${mode.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (mode) {
                                RotateStraightenEngine.StraightenCropMode.AUTO_CROP -> "Auto-Crop"
                                RotateStraightenEngine.StraightenCropMode.EXPAND_CANVAS -> "Expand"
                                RotateStraightenEngine.StraightenCropMode.ORIGINAL_SIZE -> "Retain"
                            },
                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (mode) {
                                RotateStraightenEngine.StraightenCropMode.AUTO_CROP -> "No borders"
                                RotateStraightenEngine.StraightenCropMode.EXPAND_CANVAS -> "Fit all"
                                RotateStraightenEngine.StraightenCropMode.ORIGINAL_SIZE -> "Keep W×H"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. COMPOSITION GRID CONTROLS
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Composition Grid", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Switch(
                        checked = state.showGrid,
                        onCheckedChange = { viewModel.setShowGrid(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.testTag("toggle_show_grid")
                    )
                }

                if (state.showGrid) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "GRID PATTERN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RotateStraightenEngine.GridType.values().forEach { type ->
                            val isSelected = state.gridType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGridType(type) },
                                label = { Text(type.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grid Opacity: ${(state.gridOpacity * 100).roundToInt()}%", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Slider(
                            value = state.gridOpacity,
                            onValueChange = { viewModel.setGridOpacity(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier
                                .width(180.dp)
                                .testTag("slider_grid_opacity")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 8. HORIZON GUIDE & LEVEL
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Horizon Guide & Spirit Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Switch(
                        checked = state.showHorizonGuide,
                        onCheckedChange = { viewModel.setShowHorizonGuide(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.testTag("toggle_show_horizon")
                    )
                }

                if (state.showHorizonGuide) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Drag the horizontal guide line directly on the photo to align with water or skyline.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vertical Position", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Slider(
                            value = state.horizonGuideYOffset,
                            onValueChange = { viewModel.setHorizonGuideYOffset(it) },
                            valueRange = 0.1f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF10B981),
                                activeTrackColor = Color(0xFF059669)
                            ),
                            modifier = Modifier
                                .width(180.dp)
                                .testTag("slider_horizon_y")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 9. PRIMARY APPLY TRANSFORMATION BUTTON
        Button(
            onClick = { viewModel.applyRotationAndStraighten() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(12.dp),
            enabled = abs(totalAngle) > 0.05f && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_apply_rotation_straighten")
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("Apply Rotation & Straighten (%.1f°)", totalAngle),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
