package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.RotateStraightenEngine
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

@Composable
fun StraightenControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 1. Header with Angle indicator and Auto-Level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STRAIGHTEN & HORIZON LEVEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                )
                Text(
                    text = String.format("Current tilt: %.1f°", state.straightenAngle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (state.straightenAngle == 0f) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                )
            }

            // Smart Horizon Auto-Level Button
            Button(
                onClick = { viewModel.detectHorizon() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("auto_straighten_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Auto-Level",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Continuous Precision Slider (-45° to +45°)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-45°",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
            )
            Slider(
                value = state.straightenAngle,
                onValueChange = { viewModel.setStraightenAngle(it) },
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("straighten_angle_slider")
            )
            Text(
                text = "+45°",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
            )
        }

        // 3. Fine-Tuning Step Nudge Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nudges = listOf(-1.0f, -0.1f, 0.0f, 0.1f, 1.0f)
            nudges.forEach { delta ->
                val isZero = delta == 0.0f
                Surface(
                    color = if (isZero) Color(0xFF1E293B) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isZero) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isZero) viewModel.setStraightenAngle(0f)
                            else viewModel.nudgeStraightenAngle(delta)
                        }
                        .padding(vertical = 4.dp)
                        .testTag(if (isZero) "straighten_reset_zero" else "straighten_nudge_${delta.toString().replace(".", "_")}")
                ) {
                    Text(
                        text = if (isZero) "0.0°" else if (delta > 0) "+$delta°" else "$delta°",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isZero) Color(0xFF38BDF8) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Alignment Grid Overlays
        Text(
            text = "ALIGNMENT GRID OVERLAY",
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
            RotateStraightenEngine.GridType.values().forEach { type ->
                val selected = state.gridType == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setGridType(type) },
                    label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("grid_type_${type.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Crop Mode Selection (Auto Crop vs Expand)
        Text(
            text = "CANVAS EDGE BEHAVIOR",
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
            RotateStraightenEngine.StraightenCropMode.values().forEach { mode ->
                val selected = state.straightenCropMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setStraightenCropMode(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF0F172A),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("straighten_crop_${mode.name.lowercase()}")
                )
            }
        }

        state.lastStraightenReport?.let { rep ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFF064E3B).copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth().testTag("straighten_verification_badge")
            ) {
                Text(
                    text = rep.summary,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6EE7B7), fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Action Button: Apply Straighten
        Button(
            onClick = { viewModel.applyRotationAndStraighten() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_straighten_button")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Apply Straighten & Level",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            )
        }
    }
}
