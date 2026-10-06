package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.PerspectiveEngine
import com.example.processing.PerspectiveEngine.DocumentEnhanceMode
import com.example.processing.PerspectiveEngine.PerspectiveCorner
import com.example.processing.PerspectiveEngine.PerspectiveOutputPreset
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

/**
 * Material 3 UI Controls for Perspective Correction (Request 25).
 * Supports:
 * - Four-corner adjustment & nudge D-pad
 * - Grid toggle & density
 * - Live Preview
 * - Reset to full/default
 * - Apply Homography Warp
 * - Presets for Documents, Certificates, Receipts, Photos of paper
 */
@Composable
fun PerspectiveControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    var nudgeStepSize by remember { mutableStateOf(0.01f) } // 1% normalized nudge step

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Tool Header with Auto-Detect Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PERSPECTIVE CORRECTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF38BDF8)
                    )
                )
                Text(
                    text = "Documents, Certificates, Receipts & Paper",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                )
            }

            // Auto-Detect Document Edges Button
            Button(
                onClick = { viewModel.autoDetectDocumentCorners() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_auto_detect_corners")
            ) {
                if (state.isAutoDetectingCorners) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF67E8F9), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text("Auto-Detect", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. DOCUMENT & PAPER TARGET PRESETS
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET DOCUMENT PRESET",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = state.perspectiveOutputPreset.label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PerspectiveOutputPreset.values()) { preset ->
                    val isSelected = state.perspectiveOutputPreset == preset
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setPerspectiveOutputPreset(preset) }
                            .testTag("preset_${preset.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. FOUR-CORNER SELECTOR & MICRO-NUDGE CONTROLS
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORNER FINE-TUNING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Step Size selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Step:", color = Color(0xFF64748B), fontSize = 10.sp)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (nudgeStepSize == 0.005f) Color(0xFF0284C7) else Color(0xFF1E293B),
                            modifier = Modifier.clickable { nudgeStepSize = 0.005f }
                        ) {
                            Text("0.5%", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (nudgeStepSize == 0.01f) Color(0xFF0284C7) else Color(0xFF1E293B),
                            modifier = Modifier.clickable { nudgeStepSize = 0.01f }
                        ) {
                            Text("1%", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (nudgeStepSize == 0.03f) Color(0xFF0284C7) else Color(0xFF1E293B),
                            modifier = Modifier.clickable { nudgeStepSize = 0.03f }
                        ) {
                            Text("3%", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }

                // 4 Corner Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PerspectiveCorner.values().forEach { corner ->
                        val isSelected = state.activePerspectiveCorner == corner
                        val cornerColor = when (corner) {
                            PerspectiveCorner.TOP_LEFT -> Color(0xFF38BDF8)
                            PerspectiveCorner.TOP_RIGHT -> Color(0xFF34D399)
                            PerspectiveCorner.BOTTOM_RIGHT -> Color(0xFFFBBF24)
                            PerspectiveCorner.BOTTOM_LEFT -> Color(0xFFF43F5E)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) cornerColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) cornerColor else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setActivePerspectiveCorner(corner) }
                                .testTag("corner_${corner.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = corner.shortName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) cornerColor else Color(0xFFCBD5E1)
                                    )
                                )
                                val pt = state.perspectiveQuad.getCorner(corner)
                                Text(
                                    text = String.format("%.0f,%.0f", pt.x * 100f, pt.y * 100f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                }

                // D-Pad Nudge Buttons for Selected Corner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeCorner = state.activePerspectiveCorner
                    val curPt = state.perspectiveQuad.getCorner(activeCorner)

                    // Nudge Left
                    IconButton(
                        onClick = { viewModel.updatePerspectiveCorner(activeCorner, curPt.x - nudgeStepSize, curPt.y) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("nudge_left")
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Nudge Left", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Nudge Up
                        IconButton(
                            onClick = { viewModel.updatePerspectiveCorner(activeCorner, curPt.x, curPt.y - nudgeStepSize) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("nudge_up")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Nudge Up", tint = Color.White)
                        }

                        // Nudge Down
                        IconButton(
                            onClick = { viewModel.updatePerspectiveCorner(activeCorner, curPt.x, curPt.y + nudgeStepSize) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("nudge_down")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nudge Down", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Nudge Right
                    IconButton(
                        onClick = { viewModel.updatePerspectiveCorner(activeCorner, curPt.x + nudgeStepSize, curPt.y) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("nudge_right")
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Nudge Right", tint = Color.White)
                    }
                }
            }
        }

        // 4. GRID & DOCUMENT ENHANCEMENT ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid Toggle Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Text("Grid Mesh", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp))
                    }
                    Switch(
                        checked = state.showPerspectiveGrid,
                        onCheckedChange = { viewModel.togglePerspectiveGrid(it) },
                        modifier = Modifier.testTag("switch_perspective_grid"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0C4A6E)
                        )
                    )
                }
            }

            // Live Preview Toggle Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (state.isPerspectivePreviewActive) Color(0xFF064E3B) else Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isPerspectivePreviewActive) Color(0xFF10B981) else Color(0xFF1E293B)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (state.isPerspectivePreviewActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (state.isPerspectivePreviewActive) Color(0xFF34D399) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rectified",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (state.isPerspectivePreviewActive) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = if (state.isPerspectivePreviewActive) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                    Switch(
                        checked = state.isPerspectivePreviewActive,
                        onCheckedChange = { viewModel.togglePerspectivePreview(it) },
                        modifier = Modifier.testTag("switch_perspective_preview"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF34D399),
                            checkedTrackColor = Color(0xFF065F46)
                        )
                    )
                }
            }
        }

        // 5. DOCUMENT ENHANCEMENT FILTERS (Magic Clean, Crisp B&W, Grayscale)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "DOCUMENT FILTER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DocumentEnhanceMode.values().forEach { mode ->
                    val isSelected = state.perspectiveEnhanceMode == mode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0369A1) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setPerspectiveEnhanceMode(mode) }
                            .testTag("filter_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // 5B. KEYSTONE VERTICAL & HORIZONTAL TILT SLIDERS (Phase 10)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "KEYSTONE TILT CORRECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    )
                    Text(
                        text = String.format("V: %.0f  H: %.0f", state.perspectiveVerticalTilt, state.perspectiveHorizontalTilt),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Vertical Tilt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vertical", color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.width(64.dp))
                    androidx.compose.material3.Slider(
                        value = state.perspectiveVerticalTilt,
                        onValueChange = { viewModel.setPerspectiveKeystoneTilt(verticalTilt = it) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(1f).testTag("slider_perspective_vertical_tilt")
                    )
                }

                // Horizontal Tilt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Horizontal", color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.width(64.dp))
                    androidx.compose.material3.Slider(
                        value = state.perspectiveHorizontalTilt,
                        onValueChange = { viewModel.setPerspectiveKeystoneTilt(horizontalTilt = it) },
                        valueRange = -100f..100f,
                        modifier = Modifier.weight(1f).testTag("slider_perspective_horizontal_tilt")
                    )
                }
            }
        }

        state.lastPerspectiveReport?.let { rep ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF064E3B).copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth().testTag("perspective_verification_badge")
            ) {
                Text(
                    text = rep.summary,
                    color = Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // 6. ACTION BAR (Reset & Apply)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetPerspectiveQuad() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_reset_perspective"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Corners", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { viewModel.applyPerspectiveCorrection() },
                modifier = Modifier
                    .weight(1.4f)
                    .height(44.dp)
                    .testTag("btn_apply_perspective"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Rectification", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
