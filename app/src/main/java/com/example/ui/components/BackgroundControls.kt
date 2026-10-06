package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.BackgroundProcessor
import com.example.processing.BitmapUtils
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

enum class BgTabSection(val label: String, val icon: ImageVector) {
    TARGET_BG("Background", Icons.Default.Palette),
    EDGE_TOLERANCE("Tolerance & Edges", Icons.Default.Tune),
    CLEANUP("Clean & Matting", Icons.Default.CleaningServices),
    REMOVAL_MODE("Method", Icons.Default.AutoAwesome)
}

/**
 * Universal Background Processing Controls Panel (Request 27).
 * 
 * Includes:
 * - Transparent / White / Preset / Custom Solid / Gradient / Blur / Image Replacement
 * - Edge Tolerance, Feathering, Smoothing Sliders
 * - Speckle & Noise Cleanup, Halo Decontamination
 * - Smart Edge / Flood Fill / Luminance Keying methods
 * - 100% On-Device execution & Local Photo Picker for Custom Background
 */
@Composable
fun BackgroundControls(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(BgTabSection.TARGET_BG) }

    val config = state.bgConfig

    // Custom Image BG Picker
    val customImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1200, 1200)
            if (bmp != null) {
                viewModel.setCustomBgImage(bmp)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("background_controls_panel")
    ) {
        // 1. Action Toolbar: Split-Screen Toggle, Reset, Privacy Badge
        BgActionToolbar(viewModel = viewModel, state = state)

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Tab Navigation
        TabRow(
            selectedTabIndex = selectedSection.ordinal,
            containerColor = Color(0xFF0F172A),
            contentColor = Color(0xFF38BDF8),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection.ordinal]),
                    color = Color(0xFF38BDF8),
                    height = 2.dp
                )
            },
            divider = {}
        ) {
            BgTabSection.values().forEach { section ->
                Tab(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    text = {
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.label,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Tab Body
        when (selectedSection) {
            BgTabSection.TARGET_BG -> TargetBackgroundSection(
                viewModel = viewModel,
                state = state,
                onPickCustomImage = {
                    customImagePicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )
            BgTabSection.EDGE_TOLERANCE -> EdgeToleranceSection(viewModel, state)
            BgTabSection.CLEANUP -> CleanupMattingSection(viewModel, state)
            BgTabSection.REMOVAL_MODE -> RemovalMethodSection(viewModel, state)
        }

        state.lastBackgroundReport?.let { rep ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFF064E3B).copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                modifier = Modifier.fillMaxWidth().testTag("background_verification_badge")
            ) {
                Text(
                    text = rep.summary,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6EE7B7), fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Bottom Commit & Hold-to-Compare Bar
        BgCommitBottomBar(viewModel = viewModel, state = state)
    }
}

/**
 * Top Toolbar with Quick Toggles & Reset.
 */
@Composable
private fun BgActionToolbar(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Privacy Guarantee
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "100% On-Device",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Split Comparison Toggle
            IconButton(
                onClick = { viewModel.toggleBeforeAfterSplit() },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (state.showBeforeAfterSplit) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                    .testTag("btn_toggle_bg_split")
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Split View",
                    tint = if (state.showBeforeAfterSplit) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Reset
            IconButton(
                onClick = { viewModel.resetBackgroundConfig() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_reset_bg_config")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Settings",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Tab 1: Target Background Modes (Transparent, Solid White/Presets, Custom Hex, Gradient, Blur, Image).
 */
@Composable
private fun TargetBackgroundSection(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    onPickCustomImage: () -> Unit
) {
    val config = state.bgConfig

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector Chips
        Text(
            text = "BACKGROUND TYPE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundProcessor.BgMode.values().forEach { mode ->
                val selected = config.mode == mode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setBackgroundMode(mode) },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.testTag("chip_bg_mode_${mode.name.lowercase()}")
                )
            }
        }

        // Mode-Specific Controls
        when (config.mode) {
            BackgroundProcessor.BgMode.TRANSPARENT -> {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InvertColors,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Transparent Background (Alpha PNG)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text(
                                text = "Isolates foreground subject. Save as PNG or WebP to preserve alpha.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            BackgroundProcessor.BgMode.SOLID_COLOR -> {
                // Preset Palette Pills (White, Off-White, Light Blue, Navy, Red, etc.)
                Text(
                    text = "PRESET COLORS",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundProcessor.PassportBgColor.values().filter { it != BackgroundProcessor.PassportBgColor.TRANSPARENT }.forEach { bg ->
                        val selected = config.solidColor == bg.colorInt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .clickable { viewModel.setBackgroundSolidColor(bg.colorInt, String.format("#%06X", 0xFFFFFF and bg.colorInt)) }
                                .testTag("btn_bg_color_${bg.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(bg.colorInt))
                                        .border(0.5.dp, Color.Gray, CircleShape)
                                )
                                Text(
                                    text = bg.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (selected) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                // Custom Hex Code Input
                var hexInput by remember(config.customColorHex) { mutableStateOf(config.customColorHex) }
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val clean = input.trim()
                        if (clean.startsWith("#") && (clean.length == 7 || clean.length == 9)) {
                            try {
                                val parsed = android.graphics.Color.parseColor(clean)
                                viewModel.setBackgroundSolidColor(parsed, clean)
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("Custom Hex Color (#FFFFFF, #1E293B)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_bg_hex")
                )
            }

            BackgroundProcessor.BgMode.GRADIENT -> {
                Text(
                    text = "STUDIO GRADIENT PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundProcessor.GradientPreset.values().forEach { preset ->
                        val selected = config.gradientPreset == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .clickable { viewModel.setBackgroundGradient(preset) }
                                .testTag("chip_gradient_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(preset.startColor), Color(preset.endColor))
                                            )
                                        )
                                )
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (selected) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            BackgroundProcessor.BgMode.BLUR_ORIGINAL -> {
                Text(
                    text = "BOKEH / BLUR INTENSITY",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Blur Radius", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${config.blurRadius} px", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.blurRadius.toFloat(),
                    onValueChange = { viewModel.setBackgroundBlurRadius(it.roundToInt()) },
                    valueRange = 5f..40f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("slider_bg_blur")
                )
            }

            BackgroundProcessor.BgMode.CUSTOM_IMAGE -> {
                Button(
                    onClick = onPickCustomImage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pick_custom_bg_image")
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Backdrop Photo from Device", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tab 2: Edge Tolerance, Feathering & Edge Smoothing.
 */
@Composable
private fun EdgeToleranceSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val config = state.bgConfig

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tolerance Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Color Distance Tolerance",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text("Higher values key out broader color variations", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            Text("${config.tolerance.roundToInt()}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.tolerance,
            onValueChange = { viewModel.setBackgroundTolerance(it) },
            valueRange = 5f..100f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
            modifier = Modifier.testTag("slider_bg_tolerance")
        )

        // Feathering Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Feather Radius",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text("Softens subject contour to prevent harsh borders", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            Text(String.format("%.1f px", config.featherRadius), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.featherRadius,
            onValueChange = { viewModel.setBackgroundFeatherRadius(it) },
            valueRange = 0f..8f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
            modifier = Modifier.testTag("slider_bg_feather")
        )

        // Edge Smoothness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Edge Smoothness",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text("Blends perimeter pixels for hair & fabric edges", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            Text(String.format("%.1f", config.edgeSmoothness), color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = config.edgeSmoothness,
            onValueChange = { viewModel.setBackgroundEdgeSmoothness(it) },
            valueRange = 0f..5f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
            modifier = Modifier.testTag("slider_bg_smoothness")
        )
    }
}

/**
 * Tab 3: Background Cleanup & Noise/Speckle suppression.
 */
@Composable
private fun CleanupMattingSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val config = state.bgConfig

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Noise Cleanup Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (config.cleanupNoise) Color(0xFF38BDF8) else Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleBackgroundNoiseCleanup() }
                .testTag("card_bg_noise_cleanup")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Speckle & Noise Cleanup",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = "Eliminates stray floating dots and background artifacts",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = config.cleanupNoise,
                    onCheckedChange = { viewModel.toggleBackgroundNoiseCleanup() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8), checkedTrackColor = Color(0xFF0284C7))
                )
            }
        }

        // Halo Decontaminate Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (config.decontaminateHalo) Color(0xFF38BDF8) else Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.toggleBackgroundHaloDecontaminate() }
                .testTag("card_bg_halo_decontaminate")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Decontaminate Color Fringe",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = "Removes color bleed from original backdrop on edge pixels",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = config.decontaminateHalo,
                    onCheckedChange = { viewModel.toggleBackgroundHaloDecontaminate() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8), checkedTrackColor = Color(0xFF0284C7))
                )
            }
        }
    }
}

/**
 * Tab 4: Removal Method (Smart Edge / Flood Fill / Luminance Key).
 */
@Composable
private fun RemovalMethodSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val config = state.bgConfig

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "REMOVAL ALGORITHM",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )

        BackgroundProcessor.RemovalMethod.values().forEach { method ->
            val selected = config.removalMethod == method
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) Color(0xFF1E293B) else Color(0xFF0F172A),
                border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setBackgroundRemovalMethod(method) }
                    .testTag("btn_removal_method_${method.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = method.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color(0xFF38BDF8) else Color.White
                            )
                        )
                        val desc = when (method) {
                            BackgroundProcessor.RemovalMethod.SMART_EDGE -> "Multi-corner perimeter sampling with gradient distance"
                            BackgroundProcessor.RemovalMethod.FLOOD_FILL -> "Contiguous border seed fill (ideal for complex interiors)"
                            BackgroundProcessor.RemovalMethod.LUMINANCE_KEY -> "High-key studio backdrop thresholding"
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                        )
                    }
                    if (selected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }
}

/**
 * Bottom Action Bar with Hold-to-Compare & Bake/Save.
 */
@Composable
private fun BgCommitBottomBar(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> viewModel.setHoldingCompareOriginal(true)
                is PressInteraction.Release -> viewModel.setHoldingCompareOriginal(false)
                is PressInteraction.Cancel -> viewModel.setHoldingCompareOriginal(false)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hold to Compare Button
        Button(
            onClick = {},
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isHoldingCompareOriginal) Color(0xFF0284C7) else Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8)
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_bg_hold_compare")
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Hold to View Original",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (state.isHoldingCompareOriginal) "Showing Original" else "Hold to Compare",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            )
        }

        // Bake & Apply Background Permanently
        Button(
            onClick = { viewModel.applyBackgroundPermanently() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_apply_background")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Bake Background",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Bake & Save",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
