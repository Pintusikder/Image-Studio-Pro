package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterHdr
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonochromePhotos
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterPreset
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel
import kotlin.math.roundToInt

enum class EnhanceSection(val label: String, val icon: ImageVector) {
    TONE("Tone", Icons.Default.Tune),
    DETAIL("Detail", Icons.Default.FilterHdr),
    MONOCHROME("B&W", Icons.Default.MonochromePhotos),
    PRESETS("Presets", Icons.Default.InvertColors),
    CREATIVE("Creative", Icons.Default.AutoAwesome)
}

/**
 * Universal Image Enhancement Controls Panel (Request 26).
 */
@Composable
fun EnhanceControls(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    var selectedSection by remember { mutableStateOf(EnhanceSection.TONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("enhance_controls_panel")
    ) {
        // 1. Primary Action Toolbar: Auto Enhance, Undo, Redo, Reset, Before/After Split, Histogram
        EnhanceActionToolbar(viewModel = viewModel, state = state)

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Section Navigation Tabs
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
            EnhanceSection.values().forEach { section ->
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

        // 3. Section Content
        when (selectedSection) {
            EnhanceSection.TONE -> ToneExposureSection(viewModel, state)
            EnhanceSection.DETAIL -> DetailClaritySection(viewModel, state)
            EnhanceSection.MONOCHROME -> MonochromeSection(viewModel, state)
            EnhanceSection.PRESETS -> PresetsSection(viewModel, state)
            EnhanceSection.CREATIVE -> CreativeAdvancedSection(viewModel, state)
        }

        state.lastEnhancementReport?.let { rep ->
            if (rep.isValid && rep.isModifiedWhenNonNeutral) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF064E3B).copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().testTag("enhance_verification_badge")
                ) {
                    Text(
                        text = rep.summary,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6EE7B7), fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Bottom Commit & Hold-to-Compare Bar
        EnhanceCommitBottomBar(viewModel = viewModel, state = state)
    }
}

/**
 * Top Toolbar with Auto Enhance, Undo/Redo, Reset, and Split Comparison.
 */
@Composable
private fun EnhanceActionToolbar(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1-Tap Auto Enhance Button
        Button(
            onClick = { viewModel.autoEnhance() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_auto_enhance")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Auto Enhance",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Auto",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            val canUndo = state.enhanceUndoStack.isNotEmpty()
            IconButton(
                onClick = { viewModel.undoEnhancement() },
                enabled = canUndo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_enhance_undo")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color(0xFF38BDF8) else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo
            val canRedo = state.enhanceRedoStack.isNotEmpty()
            IconButton(
                onClick = { viewModel.redoEnhancement() },
                enabled = canRedo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_enhance_redo")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color(0xFF38BDF8) else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Reset
            IconButton(
                onClick = { viewModel.resetEnhancements() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_enhance_reset")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset All Sliders",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Split Comparison Toggle
            IconButton(
                onClick = { viewModel.toggleBeforeAfterSplit() },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (state.showBeforeAfterSplit) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                    .testTag("btn_toggle_split_compare")
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Split View Comparison",
                    tint = if (state.showBeforeAfterSplit) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Histogram Toggle
            IconButton(
                onClick = { viewModel.toggleEnhanceHistogram() },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (state.showEnhanceHistogram) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
                    .testTag("btn_toggle_histogram")
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Histogram Overlay",
                    tint = if (state.showEnhanceHistogram) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Tonal & Exposure Adjustment Sliders.
 */
@Composable
private fun ToneExposureSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Brightness
        EnhanceSliderRow(
            label = "Brightness",
            value = state.brightness,
            range = -100f..100f,
            unit = "",
            icon = Icons.Default.LightMode,
            tag = "slider_brightness",
            onValueChange = { viewModel.setEnhancements(brightness = it) },
            onReset = { viewModel.setEnhancements(brightness = 0f) }
        )

        // Contrast
        EnhanceSliderRow(
            label = "Contrast",
            value = state.contrast,
            range = -100f..100f,
            unit = "",
            icon = Icons.Default.Contrast,
            tag = "slider_contrast",
            onValueChange = { viewModel.setEnhancements(contrast = it) },
            onReset = { viewModel.setEnhancements(contrast = 0f) }
        )

        // Exposure (EV)
        EnhanceSliderRow(
            label = "Exposure",
            value = state.exposure,
            range = -100f..100f,
            unit = " EV",
            valueFormatter = { String.format("%.2f", it / 50f) },
            icon = Icons.Default.WbSunny,
            tag = "slider_exposure",
            onValueChange = { viewModel.setEnhancements(exposure = it) },
            onReset = { viewModel.setEnhancements(exposure = 0f) }
        )

        // Highlights
        EnhanceSliderRow(
            label = "Highlights",
            value = state.highlights,
            range = -100f..100f,
            unit = "",
            tag = "slider_highlights",
            onValueChange = { viewModel.setEnhancements(highlights = it) },
            onReset = { viewModel.setEnhancements(highlights = 0f) }
        )

        // Shadows
        EnhanceSliderRow(
            label = "Shadows",
            value = state.shadows,
            range = -100f..100f,
            unit = "",
            tag = "slider_shadows",
            onValueChange = { viewModel.setEnhancements(shadows = it) },
            onReset = { viewModel.setEnhancements(shadows = 0f) }
        )

        // Saturation
        EnhanceSliderRow(
            label = "Saturation",
            value = state.saturation,
            range = -100f..100f,
            unit = "",
            tag = "slider_saturation",
            onValueChange = { viewModel.setEnhancements(saturation = it) },
            onReset = { viewModel.setEnhancements(saturation = 0f) }
        )

        // Vibrance (Skin-tone aware smart saturation)
        EnhanceSliderRow(
            label = "Vibrance",
            value = state.vibrance,
            range = -100f..100f,
            unit = "",
            tag = "slider_vibrance",
            onValueChange = { viewModel.setEnhancements(vibrance = it) },
            onReset = { viewModel.setEnhancements(vibrance = 0f) }
        )

        // Gamma Correction
        EnhanceSliderRow(
            label = "Gamma",
            value = state.gamma,
            range = 0.2f..3.0f,
            unit = "",
            valueFormatter = { String.format("%.2f", it) },
            tag = "slider_gamma",
            onValueChange = { viewModel.setEnhancements(gamma = it) },
            onReset = { viewModel.setEnhancements(gamma = 1.0f) }
        )
    }
}

/**
 * Detail, Sharpness, CLAHE Local Contrast, Warmth, Vignette Section.
 */
@Composable
private fun DetailClaritySection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sharpness
        EnhanceSliderRow(
            label = "Sharpness",
            value = state.sharpness,
            range = 0f..100f,
            unit = "%",
            tag = "slider_sharpness",
            onValueChange = { viewModel.setEnhancements(sharpness = it) },
            onReset = { viewModel.setEnhancements(sharpness = 0f) }
        )

        // CLAHE Adaptive Local Contrast
        EnhanceSliderRow(
            label = "CLAHE Clarity",
            value = state.claheStrength,
            range = 0f..100f,
            unit = "%",
            tag = "slider_clahe",
            onValueChange = { viewModel.setEnhancements(claheStrength = it) },
            onReset = { viewModel.setEnhancements(claheStrength = 0f) }
        )

        // Warmth / Color Temp
        EnhanceSliderRow(
            label = "Warmth",
            value = state.warmth,
            range = -100f..100f,
            unit = "",
            tag = "slider_warmth",
            onValueChange = { viewModel.setEnhancements(warmth = it) },
            onReset = { viewModel.setEnhancements(warmth = 0f) }
        )

        // Vignette
        EnhanceSliderRow(
            label = "Vignette",
            value = state.vignette,
            range = 0f..100f,
            unit = "%",
            tag = "slider_vignette",
            onValueChange = { viewModel.setEnhancements(vignette = it) },
            onReset = { viewModel.setEnhancements(vignette = 0f) }
        )
    }
}

/**
 * Grayscale & High-Contrast Black & White Section.
 */
@Composable
private fun MonochromeSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Switches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Grayscale Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isGrayscale) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (state.isGrayscale) Color(0xFF38BDF8) else Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleGrayscale() }
                    .testTag("toggle_grayscale_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Grayscale",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Tonal Gray",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                        )
                    }
                    Switch(
                        checked = state.isGrayscale,
                        onCheckedChange = { viewModel.toggleGrayscale() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
            }

            // Black & White Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isBlackAndWhite) Color(0xFF1E293B) else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (state.isBlackAndWhite) Color(0xFF38BDF8) else Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleBlackAndWhite() }
                    .testTag("toggle_bw_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Black & White",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "High Contrast",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                        )
                    }
                    Switch(
                        checked = state.isBlackAndWhite,
                        onCheckedChange = { viewModel.toggleBlackAndWhite() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
            }
        }

        // B&W Binarization Threshold Slider (Active only when B&W is enabled)
        AnimatedVisibility(
            visible = state.isBlackAndWhite,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            EnhanceSliderRow(
                label = "B&W Threshold",
                value = state.bwThreshold,
                range = 10f..245f,
                unit = "",
                tag = "slider_bw_threshold",
                onValueChange = { viewModel.setBwThreshold(it) },
                onReset = { viewModel.setBwThreshold(128f) }
            )
        }
    }
}

/**
 * Filter Presets Section.
 */
@Composable
private fun PresetsSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPreset.values().forEach { filter ->
                val selected = state.activeFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setEnhancements(filter = filter) },
                    label = { Text(filter.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.testTag("chip_preset_${filter.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Phase 10 Approved Advanced Creative & Diagnostic Tools:
 * Watermark, Frame Styling, Privacy Redaction, Color Palette Extraction & Optical Quality Analysis.
 */
@Composable
private fun CreativeAdvancedSection(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("creative_advanced_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Quick Watermark Stamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = state.watermarkText,
                onValueChange = { viewModel.setWatermarkText(it) },
                label = { Text("Watermark Text") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_watermark_text")
            )
            Button(
                onClick = { viewModel.applyWatermarkOverlay() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_apply_watermark")
            ) {
                Text("Stamp", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. Quick Frame & Privacy Redaction Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.applyDecorativeFrame() },
                modifier = Modifier.testTag("btn_creative_frame")
            ) {
                Text("Studio Frame", fontSize = 11.sp, color = Color(0xFF38BDF8))
            }

            OutlinedButton(
                onClick = { viewModel.applyPrivacyPixelateCenter() },
                modifier = Modifier.testTag("btn_privacy_pixelate")
            ) {
                Text("Privacy Mosaic", fontSize = 11.sp, color = Color(0xFFFBBF24))
            }

            OutlinedButton(
                onClick = { viewModel.runImageDiagnosticsAndPalette() },
                modifier = Modifier.testTag("btn_extract_palette")
            ) {
                Text("Palette & Focus Audit", fontSize = 11.sp, color = Color(0xFF34D399))
            }
        }

        // Display Extracted Color Palette Swatches if available
        if (state.colorPaletteSwatches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.colorPaletteSwatches.forEach { swatch ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch.colorInt))
                            )
                            Text(swatch.hexCode, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Display Quality Report if available
        state.qualityReport?.let { q ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("quality_audit_badge")
            ) {
                Text(
                    text = "Focus: ${q.sharpnessLabel} (Score ${q.sharpnessScore}) • Dynamic Range: ${q.dynamicRangeScore}/100 • dHash: ${state.perceptualHashHex ?: ""}",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * Reusable Parameter Slider Row with Double-Tap to Reset & Value Badge.
 */
@Composable
fun EnhanceSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    valueFormatter: ((Float) -> String)? = null,
    icon: ImageVector? = null,
    tag: String,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val displayValue = valueFormatter?.invoke(value) ?: "${value.roundToInt()}$unit"
    val isNonZero = value != 0f && value != 128f // 128 for threshold

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isNonZero) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNonZero) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }

            // Numeric Badge with Double-Tap to Reset
            Surface(
                color = if (isNonZero) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0F172A),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isNonZero) Color(0xFF38BDF8) else Color(0xFF334155)),
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { onReset() },
                            onTap = {}
                        )
                    }
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isNonZero) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF38BDF8),
                inactiveTrackColor = Color(0xFF334155)
            )
        )
    }
}

/**
 * Bottom Commit & Hold-to-Compare Bar.
 */
@Composable
private fun EnhanceCommitBottomBar(
    viewModel: UtilityViewModel,
    state: StudioUiState
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Detect press and release for Hold to Compare
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
                .testTag("btn_hold_compare")
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

        // Apply / Bake Enhancements Permanently
        Button(
            onClick = { viewModel.applyEnhancementsPermanently() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("btn_apply_enhancements")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Bake Enhancements",
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
