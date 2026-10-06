package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PresetEntity
import com.example.model.IdDocumentCategory
import com.example.model.PassportPreset
import com.example.model.ResizeMode
import com.example.model.SocialMediaCategory
import com.example.model.SocialMediaPreset
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.UtilityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(
    viewModel: UtilityViewModel,
    onNavigateToEditor: (StudioTab) -> Unit,
    onBack: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()

    var selectedCategory by remember { mutableIntStateOf(2) } // Default to Custom presets
    var selectedIdCategory by remember { mutableStateOf(IdDocumentCategory.ALL) }
    
    // Social Media Presets State (Request 35)
    var selectedSocialCategory by remember { mutableStateOf(SocialMediaCategory.ALL) }
    var selectedSocialPlatform by remember { mutableStateOf<String?>(null) }
    var socialSearchQuery by remember { mutableStateOf("") }
    var showCreateSocialDialog by remember { mutableStateOf(false) }
    var socialPresetToEdit by remember { mutableStateOf<SocialMediaPreset?>(null) }
    var socialPresetToDuplicate by remember { mutableStateOf<SocialMediaPreset?>(null) }
    var socialPresetToDelete by remember { mutableStateOf<SocialMediaPreset?>(null) }
    var showResetAllSocialDialog by remember { mutableStateOf(false) }

    val customPresets by viewModel.allPresets.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<PresetEntity?>(null) }
    var presetToDuplicate by remember { mutableStateOf<PresetEntity?>(null) }
    var presetToDelete by remember { mutableStateOf<PresetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Presets & Dimensions",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        )
                        Text(
                            text = "Create, edit, duplicate, and apply custom configurations",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("presets_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.topBarBackground)
            )
        },
        floatingActionButton = {
            if (selectedCategory == 2) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = palette.accentPrimary,
                    contentColor = palette.accentOnPrimary,
                    modifier = Modifier.testTag("create_preset_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Custom Preset")
                }
            }
        },
        containerColor = palette.appBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedCategory,
                containerColor = palette.cardElevated,
                contentColor = palette.accentPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategory]),
                        color = palette.accentPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedCategory == 0,
                    onClick = { selectedCategory = 0 },
                    text = { Text("Social Media", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedCategory == 1,
                    onClick = { selectedCategory = 1 },
                    text = { Text("Passport & ID", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedCategory == 2,
                    onClick = { selectedCategory = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Custom Presets", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${customPresets.size}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }

            when (selectedCategory) {
                0 -> {
                    val allSocialPresets = viewModel.getEffectiveSocialPresets()
                    val availablePlatforms = remember(allSocialPresets) {
                        listOf("All") + allSocialPresets.map { it.platform }.distinct().sorted()
                    }

                    val filteredSocialPresets = remember(allSocialPresets, selectedSocialCategory, selectedSocialPlatform, socialSearchQuery) {
                        allSocialPresets.filter { preset ->
                            val matchesCat = when (selectedSocialCategory) {
                                SocialMediaCategory.ALL -> true
                                SocialMediaCategory.CUSTOM -> preset.isCustom
                                else -> preset.category == selectedSocialCategory
                            }

                            val matchesPlatform = selectedSocialPlatform == null ||
                                selectedSocialPlatform == "All" ||
                                preset.platform.equals(selectedSocialPlatform, ignoreCase = true)

                            val matchesSearch = socialSearchQuery.isBlank() ||
                                preset.name.contains(socialSearchQuery, ignoreCase = true) ||
                                preset.platform.contains(socialSearchQuery, ignoreCase = true) ||
                                "${preset.width}x${preset.height}".contains(socialSearchQuery, ignoreCase = true) ||
                                preset.aspectRatioLabel.contains(socialSearchQuery, ignoreCase = true) ||
                                preset.note.contains(socialSearchQuery, ignoreCase = true) ||
                                preset.category.label.contains(socialSearchQuery, ignoreCase = true)

                            matchesCat && matchesPlatform && matchesSearch
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search and Quick Add Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = socialSearchQuery,
                                onValueChange = { socialSearchQuery = it },
                                placeholder = { Text("Search platform, specs, or name...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (socialSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { socialSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("social_preset_search_input")
                            )

                            Button(
                                onClick = { showCreateSocialDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("create_custom_social_preset_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SocialMediaCategory.values().forEach { cat ->
                                val selected = selectedSocialCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedSocialCategory = cat },
                                    label = { Text(cat.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier.testTag("social_cat_chip_${cat.name}")
                                )
                            }
                        }

                        // Platform Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availablePlatforms.forEach { platform ->
                                val isSelected = (selectedSocialPlatform == null && platform == "All") || selectedSocialPlatform == platform
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            selectedSocialPlatform = if (platform == "All") null else platform
                                        }
                                        .testTag("platform_chip_$platform")
                                ) {
                                    Text(
                                        text = platform,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Subtitle & Reset bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredSocialPresets.size} specifications",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (uiState.customSocialPresets.isNotEmpty() || uiState.modifiedSocialPresets.isNotEmpty()) {
                                TextButton(
                                    onClick = { showResetAllSocialDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("reset_all_social_presets_button")
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset All to Defaults", color = Color(0xFFF59E0B), fontSize = 10.sp)
                                }
                            }
                        }

                        if (filteredSocialPresets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(44.dp))
                                    Text("No matching social presets found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Try clearing your filter or create a new custom social dimension spec.", color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            selectedSocialCategory = SocialMediaCategory.ALL
                                            selectedSocialPlatform = null
                                            socialSearchQuery = ""
                                        }) {
                                            Text("Clear Filters", color = Color(0xFF38BDF8))
                                        }
                                        Button(
                                            onClick = { showCreateSocialDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("Create Custom Spec", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredSocialPresets, key = { it.id }) { preset ->
                                    SocialPresetCard(
                                        preset = preset,
                                        onApply = {
                                            viewModel.applySocialPreset(preset)
                                            onNavigateToEditor(StudioTab.RESIZE)
                                        },
                                        onEdit = { socialPresetToEdit = preset },
                                        onDuplicate = { socialPresetToDuplicate = preset },
                                        onDelete = { socialPresetToDelete = preset },
                                        onReset = { viewModel.resetSocialPresetToDefault(preset.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val allDocPresets = PassportPreset.PRESETS + uiState.customPassportPresets
                    val filteredDocPresets = if (selectedIdCategory == IdDocumentCategory.ALL) {
                        allDocPresets
                    } else {
                        allDocPresets.filter { it.category == selectedIdCategory }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IdDocumentCategory.values().forEach { cat ->
                                val selected = selectedIdCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedIdCategory = cat },
                                    label = { Text(cat.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredDocPresets) { preset ->
                                PassportPresetCard(preset = preset) {
                                    viewModel.selectPassportPreset(preset)
                                    onNavigateToEditor(StudioTab.PASSPORT)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Quick Header Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Custom Export & Resize Profiles",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("create_preset_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (customPresets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color(0xFF1E293B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text(
                                        text = "No custom presets created yet",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Save exact dimensions, DPI, file format, max file size ceilings, and crop modes (FIT, FILL, STRETCH, SMART CROP).",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F172A))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Preset", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(customPresets, key = { it.id }) { preset ->
                                    CustomPresetCard(
                                        preset = preset,
                                        onApply = {
                                            viewModel.applyCustomPreset(preset)
                                            onNavigateToEditor(StudioTab.RESIZE)
                                        },
                                        onEdit = { presetToEdit = preset },
                                        onDuplicate = { presetToDuplicate = preset },
                                        onDelete = { presetToDelete = preset }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE PRESET DIALOG
    if (showCreateDialog) {
        PresetEditorDialog(
            title = "Create Custom Preset",
            initialName = "",
            initialWidth = uiState.targetWidthPx.takeIf { it > 0 } ?: 300,
            initialHeight = uiState.targetHeightPx.takeIf { it > 0 } ?: 100,
            initialDpi = uiState.dpi,
            initialFormat = uiState.exportFormat.extension.uppercase(),
            initialMaxSizeKb = uiState.targetSizeKb,
            initialQuality = uiState.quality,
            initialCropMode = "FIT",
            currentEditorWidth = uiState.targetWidthPx,
            currentEditorHeight = uiState.targetHeightPx,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, width, height, dpi, format, maxKb, quality, cropMode ->
                viewModel.createCustomPreset(
                    name = name,
                    targetWidth = width,
                    targetHeight = height,
                    dpi = dpi,
                    format = format,
                    maxFileSizeKb = maxKb,
                    quality = quality,
                    cropMode = cropMode
                )
                showCreateDialog = false
            }
        )
    }

    // EDIT PRESET DIALOG
    presetToEdit?.let { preset ->
        PresetEditorDialog(
            title = "Edit Preset",
            initialName = preset.name,
            initialWidth = preset.targetWidth,
            initialHeight = preset.targetHeight,
            initialDpi = preset.dpi,
            initialFormat = preset.format,
            initialMaxSizeKb = preset.maxFileSizeKb,
            initialQuality = preset.quality,
            initialCropMode = preset.cropMode,
            currentEditorWidth = uiState.targetWidthPx,
            currentEditorHeight = uiState.targetHeightPx,
            onDismiss = { presetToEdit = null },
            onConfirm = { name, width, height, dpi, format, maxKb, quality, cropMode ->
                viewModel.updateCustomPreset(
                    preset.copy(
                        name = name,
                        targetWidth = width,
                        targetHeight = height,
                        dpi = dpi,
                        format = format,
                        maxFileSizeKb = maxKb,
                        quality = quality,
                        cropMode = cropMode
                    )
                )
                presetToEdit = null
            }
        )
    }

    // DUPLICATE PRESET DIALOG
    presetToDuplicate?.let { preset ->
        var duplicateName by remember { mutableStateOf("${preset.name} (Copy)") }
        AlertDialog(
            onDismissRequest = { presetToDuplicate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text("Duplicate Preset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create an exact clone of '${preset.name}' with all dimensions, DPI, and limits.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = duplicateName,
                        onValueChange = { duplicateName = it },
                        label = { Text("New Preset Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (duplicateName.isNotBlank()) {
                            viewModel.duplicateCustomPreset(preset, duplicateName)
                            presetToDuplicate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Duplicate", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDuplicate = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DELETE PRESET DIALOG
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                    Text("Delete Preset?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${preset.name}' (${preset.targetWidth}×${preset.targetHeight} px)? This action cannot be undone.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomPreset(preset)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // CREATE SOCIAL PRESET DIALOG (Request 35)
    if (showCreateSocialDialog) {
        SocialPresetEditorDialog(
            title = "Create Custom Social Spec",
            initialPlatform = selectedSocialPlatform?.takeIf { it != "All" } ?: "Instagram",
            initialName = "",
            initialCategory = if (selectedSocialCategory != SocialMediaCategory.ALL) selectedSocialCategory else SocialMediaCategory.POSTS,
            initialWidth = 1080,
            initialHeight = 1080,
            initialFormat = "JPG",
            initialMaxSizeKb = null,
            initialDpi = 72,
            initialCropMode = "FIT",
            initialNote = "Custom user dimension specification",
            onDismiss = { showCreateSocialDialog = false },
            onConfirm = { platform, name, cat, w, h, ratio, format, maxKb, dpi, cropMode, note ->
                viewModel.createCustomSocialPreset(
                    platform = platform,
                    name = name,
                    category = cat,
                    width = w,
                    height = h,
                    aspectRatioLabel = ratio,
                    recommendedFormat = format,
                    maxFileSizeKb = maxKb,
                    dpi = dpi,
                    cropMode = cropMode,
                    note = note
                )
                showCreateSocialDialog = false
            }
        )
    }

    // EDIT SOCIAL PRESET DIALOG (Request 35)
    socialPresetToEdit?.let { preset ->
        SocialPresetEditorDialog(
            title = if (preset.isCustom) "Edit Custom Social Spec" else "Modify Platform Spec",
            initialPlatform = preset.platform,
            initialName = preset.name,
            initialCategory = preset.category,
            initialWidth = preset.width,
            initialHeight = preset.height,
            initialFormat = preset.recommendedFormat,
            initialMaxSizeKb = preset.maxFileSizeKb,
            initialDpi = preset.dpi,
            initialCropMode = preset.cropMode,
            initialNote = preset.note,
            onDismiss = { socialPresetToEdit = null },
            onConfirm = { platform, name, cat, w, h, ratio, format, maxKb, dpi, cropMode, note ->
                viewModel.updateSocialPreset(
                    preset.copy(
                        platform = platform,
                        name = name,
                        category = cat,
                        width = w,
                        height = h,
                        aspectRatioLabel = ratio,
                        recommendedFormat = format,
                        maxFileSizeKb = maxKb,
                        dpi = dpi,
                        cropMode = cropMode,
                        note = note
                    )
                )
                socialPresetToEdit = null
            }
        )
    }

    // DUPLICATE SOCIAL PRESET DIALOG (Request 35)
    socialPresetToDuplicate?.let { preset ->
        var duplicateName by remember { mutableStateOf("${preset.name} (Custom)") }
        AlertDialog(
            onDismissRequest = { socialPresetToDuplicate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text("Duplicate Social Spec", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a customizable duplicate copy of this social specification:", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    OutlinedTextField(
                        value = duplicateName,
                        onValueChange = { duplicateName = it },
                        label = { Text("Specification Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("duplicate_social_preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.duplicateSocialPreset(preset, duplicateName)
                        socialPresetToDuplicate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("confirm_duplicate_social_preset_button")
                ) {
                    Text("Duplicate", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { socialPresetToDuplicate = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DELETE / RESET SOCIAL PRESET DIALOG (Request 35)
    socialPresetToDelete?.let { preset ->
        val isCustom = preset.isCustom
        AlertDialog(
            onDismissRequest = { socialPresetToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isCustom) Icons.Default.Delete else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (isCustom) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    )
                    Text(if (isCustom) "Delete Preset?" else "Reset to Factory Spec?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = if (isCustom) {
                        "Are you sure you want to delete custom spec '${preset.platform} - ${preset.name}' (${preset.width}×${preset.height} px)? This action cannot be undone."
                    } else {
                        "Reset '${preset.platform} - ${preset.name}' back to official default dimensions and limits?"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCustom) {
                            viewModel.deleteSocialPreset(preset)
                        } else {
                            viewModel.resetSocialPresetToDefault(preset.id)
                        }
                        socialPresetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCustom) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.testTag("confirm_delete_social_preset_button")
                ) {
                    Text(if (isCustom) "Delete" else "Reset", color = if (isCustom) Color.White else Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { socialPresetToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // RESET ALL SOCIAL PRESETS DIALOG (Request 35)
    if (showResetAllSocialDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllSocialDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("Reset All Social Specs?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "This will restore all modified social media specifications back to standard platform defaults and delete all custom-defined dimensions. Proceed?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllSocialPresetsToDefault()
                        showResetAllSocialDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.testTag("confirm_reset_all_social_presets_button")
                ) {
                    Text("Reset All", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllSocialDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetEditorDialog(
    title: String,
    initialName: String,
    initialWidth: Int,
    initialHeight: Int,
    initialDpi: Int,
    initialFormat: String,
    initialMaxSizeKb: Int?,
    initialQuality: Int,
    initialCropMode: String,
    currentEditorWidth: Int,
    currentEditorHeight: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        width: Int,
        height: Int,
        dpi: Int,
        format: String,
        maxKb: Int?,
        quality: Int,
        cropMode: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var widthInput by remember { mutableStateOf(initialWidth.toString()) }
    var heightInput by remember { mutableStateOf(initialHeight.toString()) }
    var dpiInput by remember { mutableStateOf(initialDpi.toString()) }
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var hasMaxLimit by remember { mutableStateOf(initialMaxSizeKb != null) }
    var maxKbInput by remember { mutableStateOf(initialMaxSizeKb?.toString() ?: "20") }
    var quality by remember { mutableStateOf(initialQuality.toFloat()) }
    var selectedCropMode by remember { mutableStateOf(initialCropMode.uppercase()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (title.contains("Create")) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name (e.g. Signature 300x100)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("preset_name_input")
                )

                // Populate from current editor button
                if (currentEditorWidth > 0 && currentEditorHeight > 0) {
                    OutlinedButton(
                        onClick = {
                            widthInput = currentEditorWidth.toString()
                            heightInput = currentEditorHeight.toString()
                            if (name.isBlank()) {
                                name = "Preset ${currentEditorWidth}x${currentEditorHeight}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Editor Size (${currentEditorWidth}×${currentEditorHeight} px)", fontSize = 11.sp)
                    }
                }

                // Dimensions (Width x Height)
                Text("Dimensions (Pixels)", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Width (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f).testTag("preset_width_input")
                    )
                    IconButton(
                        onClick = {
                            val temp = widthInput
                            widthInput = heightInput
                            heightInput = temp
                        },
                        modifier = Modifier
                            .background(Color(0xFF334155), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = Color(0xFF38BDF8))
                    }
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Height (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f).testTag("preset_height_input")
                    )
                }

                // Quick Dimensions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Signature 300x100" to (300 to 100),
                        "Passport 600x600" to (600 to 600),
                        "Banner 1200x400" to (1200 to 400),
                        "Story 1080x1920" to (1080 to 1920)
                    ).forEach { (label, dims) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.clickable {
                                widthInput = dims.first.toString()
                                heightInput = dims.second.toString()
                                if (name.isBlank()) name = label
                            }
                        ) {
                            Text(
                                text = label,
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // DPI & Format
                Text("DPI & Export Format", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dpiInput,
                        onValueChange = { dpiInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("DPI") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(0.9f).testTag("preset_dpi_input")
                    )

                    Row(
                        modifier = Modifier.weight(1.3f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(72, 150, 300, 600).forEach { dpiVal ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (dpiInput == dpiVal.toString()) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier.clickable { dpiInput = dpiVal.toString() }
                            ) {
                                Text(
                                    text = "$dpiVal",
                                    color = if (dpiInput == dpiVal.toString()) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Format Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("JPG", "PNG", "WEBP", "PDF").forEach { fmt ->
                        val isSelected = selectedFormat.equals(fmt, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = fmt }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = fmt,
                                    color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Crop Mode (FIT, FILL, STRETCH, SMART CROP)
                Text("Crop Mode / Processing Strategy", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ResizeMode.FIT to "Preserves full image with padding (No cropping)",
                        ResizeMode.FILL to "Fills target frame with center cropping",
                        ResizeMode.STRETCH to "Distorts image directly to exact dimensions",
                        ResizeMode.SMART_CROP to "AI Salient detection centered on subjects"
                    ).forEach { (mode, desc) ->
                        val isSelected = selectedCropMode.equals(mode.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCropMode = mode.name }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Maximum File Size Limit (KB)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Maximum File Size Ceiling", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Strictly enforce max target size (e.g. 20 KB)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Switch(
                                checked = hasMaxLimit,
                                onCheckedChange = { hasMaxLimit = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF38BDF8),
                                    checkedTrackColor = Color(0xFF0369A1)
                                )
                            )
                        }

                        if (hasMaxLimit) {
                            OutlinedTextField(
                                value = maxKbInput,
                                onValueChange = { maxKbInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Max Size (KB)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                suffix = { Text("KB", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("preset_max_kb_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(10, 20, 50, 100, 200, 500).forEach { kb ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (maxKbInput == kb.toString()) Color(0xFF38BDF8) else Color(0xFF334155),
                                        modifier = Modifier.clickable { maxKbInput = kb.toString() }
                                    ) {
                                        Text(
                                            text = "$kb KB",
                                            color = if (maxKbInput == kb.toString()) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quality Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Default Quality", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("${quality.toInt()}%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedWidth = widthInput.toIntOrNull() ?: 300
                    val parsedHeight = heightInput.toIntOrNull() ?: 100
                    val parsedDpi = dpiInput.toIntOrNull() ?: 300
                    val parsedMaxKb = if (hasMaxLimit) maxKbInput.toIntOrNull() else null
                    val validName = name.ifBlank { "Preset ${parsedWidth}x${parsedHeight}" }

                    onConfirm(
                        validName,
                        parsedWidth,
                        parsedHeight,
                        parsedDpi,
                        selectedFormat,
                        parsedMaxKb,
                        quality.toInt(),
                        selectedCropMode
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                modifier = Modifier.testTag("save_preset_confirm_button")
            ) {
                Text("Save Preset", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun CustomPresetCard(
    preset: PresetEntity,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth().testTag("custom_preset_card_${preset.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Name & Crop Mode Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                                ),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = preset.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${preset.targetWidth} × ${preset.targetHeight} px",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Crop Mode Badge
                val cropBg = when (preset.cropMode.uppercase()) {
                    "FILL" -> Color(0xFF0284C7)
                    "STRETCH" -> Color(0xFFD97706)
                    "SMART_CROP", "SMART CROP" -> Color(0xFF9333EA)
                    else -> Color(0xFF059669) // FIT
                }
                Box(
                    modifier = Modifier
                        .background(cropBg.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, cropBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Crop: ${preset.cropMode.uppercase()}",
                        color = cropBg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Specs Row (Badges)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // DPI
                SpecBadge(label = "${preset.dpi} DPI", color = Color(0xFF818CF8))
                // Format
                SpecBadge(label = preset.format.uppercase(), color = Color(0xFF38BDF8))
                // Quality
                SpecBadge(label = "Q: ${preset.quality}%", color = Color(0xFFA78BFA))
                // Max Size
                if (preset.maxFileSizeKb != null) {
                    SpecBadge(label = "Max: ≤ ${preset.maxFileSizeKb} KB", color = Color(0xFFFBBF24))
                } else {
                    SpecBadge(label = "Unrestricted", color = Color(0xFF94A3B8))
                }
            }

            // Actions Row (Apply, Edit, Duplicate, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // APPLY Button
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("apply_preset_${preset.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Secondary Actions: Edit, Duplicate, Delete
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .testTag("edit_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Preset",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .testTag("duplicate_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Preset",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .testTag("delete_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Preset",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SocialPresetCard(
    preset: SocialMediaPreset,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("social_preset_card_${preset.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Platform Icon & Name, Category Tag & Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Platform Icon / Color badge
                    val platformGrad = when (preset.platform.lowercase()) {
                        "instagram" -> listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                        "youtube" -> listOf(Color(0xFFFF0000), Color(0xFFB91C1C))
                        "tiktok" -> listOf(Color(0xFF00F2FE), Color(0xFFFE2C55))
                        "twitter", "twitter / x", "x" -> listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                        "facebook" -> listOf(Color(0xFF1877F2), Color(0xFF0D47A1))
                        "linkedin" -> listOf(Color(0xFF0A66C2), Color(0xFF004182))
                        "discord" -> listOf(Color(0xFF5865F2), Color(0xFF404EED))
                        "pinterest" -> listOf(Color(0xFFE60023), Color(0xFFAD081B))
                        "twitch" -> listOf(Color(0xFF9146FF), Color(0xFF772CE8))
                        "snapchat" -> listOf(Color(0xFFEAB308), Color(0xFFCA8A04))
                        "whatsapp" -> listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                        "podcast", "podcasts", "apple podcast" -> listOf(Color(0xFFA855F7), Color(0xFF7E22CE))
                        else -> listOf(Color(0xFF0284C7), Color(0xFF6366F1))
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Brush.linearGradient(platformGrad), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = preset.platform,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (preset.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("CUSTOM", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (preset.isModified) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("MODIFIED", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = preset.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Category Tag
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = preset.category.label,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Specs Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecBadge(label = "${preset.width} × ${preset.height} px", color = Color(0xFF38BDF8))
                SpecBadge(label = preset.aspectRatioLabel, color = Color(0xFF818CF8))
                SpecBadge(label = preset.recommendedFormat.uppercase(), color = Color(0xFF34D399))
                SpecBadge(label = preset.cropMode.uppercase(), color = Color(0xFFA78BFA))
                SpecBadge(label = "${preset.dpi} DPI", color = Color(0xFFCBD5E1))
                if (preset.maxFileSizeKb != null) {
                    SpecBadge(label = "Max: ≤ ${preset.maxFileSizeKb} KB", color = Color(0xFFFBBF24))
                }
            }

            // Platform Safe-Zone / Guidance Note
            if (preset.note.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = preset.note,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            // Actions Row (Apply, Edit, Duplicate, Delete / Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // APPLY Button
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("apply_social_preset_${preset.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Action Icons: Edit, Duplicate, Delete / Reset
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .testTag("edit_social_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Dimensions & Spec",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .testTag("duplicate_social_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Preset",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (preset.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                                .size(34.dp)
                                .testTag("delete_social_preset_${preset.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Custom Preset",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (preset.isModified) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier
                                .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                                .size(34.dp)
                                .testTag("reset_social_preset_${preset.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset to Official Default Spec",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialPresetEditorDialog(
    title: String,
    initialPlatform: String,
    initialName: String,
    initialCategory: SocialMediaCategory,
    initialWidth: Int,
    initialHeight: Int,
    initialFormat: String,
    initialMaxSizeKb: Int?,
    initialDpi: Int,
    initialCropMode: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (
        platform: String,
        name: String,
        category: SocialMediaCategory,
        width: Int,
        height: Int,
        aspectRatioLabel: String,
        format: String,
        maxKb: Int?,
        dpi: Int,
        cropMode: String,
        note: String
    ) -> Unit
) {
    var platform by remember { mutableStateOf(initialPlatform) }
    var name by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var widthInput by remember { mutableStateOf(initialWidth.toString()) }
    var heightInput by remember { mutableStateOf(initialHeight.toString()) }
    var selectedFormat by remember { mutableStateOf(initialFormat.uppercase()) }
    var hasMaxLimit by remember { mutableStateOf(initialMaxSizeKb != null) }
    var maxKbInput by remember { mutableStateOf(initialMaxSizeKb?.toString() ?: "2048") }
    var dpiInput by remember { mutableStateOf(initialDpi.toString()) }
    var selectedCropMode by remember { mutableStateOf(initialCropMode.uppercase()) }
    var note by remember { mutableStateOf(initialNote) }

    val parsedW = widthInput.toIntOrNull() ?: 1
    val parsedH = heightInput.toIntOrNull() ?: 1
    val computedRatio = remember(parsedW, parsedH) {
        if (parsedW <= 0 || parsedH <= 0) "1:1"
        else {
            fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
            val div = gcd(parsedW, parsedH)
            val rw = parsedW / div
            val rh = parsedH / div
            if (rw < 50 && rh < 50) "$rw:$rh" else {
                val r = parsedW.toFloat() / parsedH.toFloat()
                String.format(java.util.Locale.US, "%.2f:1", r)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (title.contains("Create")) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Platform selection & text input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Platform", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        placeholder = { Text("e.g. Instagram, YouTube, TikTok, Custom") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("social_platform_input")
                    )

                    // Quick Platform Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Instagram", "YouTube", "TikTok", "Twitter / X", "Facebook", "LinkedIn", "Discord", "Pinterest", "Twitch", "Threads", "Custom").forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (platform.equals(p, ignoreCase = true)) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier.clickable { platform = p }
                            ) {
                                Text(
                                    text = p,
                                    color = if (platform.equals(p, ignoreCase = true)) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Specification / Preset Name
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Specification Name", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Portrait Post, Channel Banner, 4K Thumbnail") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("social_spec_name_input")
                    )
                }

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SocialMediaCategory.values().filter { it != SocialMediaCategory.ALL }.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat.label,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Width & Height with Ratio & Swap
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dimensions (Pixels)", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Aspect Ratio: $computedRatio", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { widthInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Width (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.weight(1f).testTag("social_width_input")
                        )

                        IconButton(
                            onClick = {
                                val temp = widthInput
                                widthInput = heightInput
                                heightInput = temp
                            },
                            modifier = Modifier
                                .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Dimensions", tint = Color(0xFF38BDF8))
                        }

                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Height (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.weight(1f).testTag("social_height_input")
                        )
                    }

                    // Common Dimension Quick Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            1080 to 1080,
                            1080 to 1350,
                            1080 to 1920,
                            1200 to 630,
                            1280 to 720,
                            1500 to 500,
                            2560 to 1440,
                            3000 to 3000,
                            400 to 400
                        ).forEach { (w, h) ->
                            val isMatch = widthInput == w.toString() && heightInput == h.toString()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMatch) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier.clickable {
                                    widthInput = w.toString()
                                    heightInput = h.toString()
                                }
                            ) {
                                Text(
                                    text = "${w}×${h}",
                                    color = if (isMatch) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Format Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Recommended Export Format", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("JPG", "PNG", "WEBP", "PDF").forEach { fmt ->
                            val isSelected = selectedFormat.equals(fmt, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = fmt }
                            ) {
                                Text(
                                    text = fmt,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Crop Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Default Crop / Resize Behavior", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            ResizeMode.FIT to "Fit (Letterbox)",
                            ResizeMode.FILL to "Fill (Crop)",
                            ResizeMode.STRETCH to "Stretch",
                            ResizeMode.SMART_CROP to "Smart Crop"
                        ).forEach { (mode, label) ->
                            val isSelected = selectedCropMode.equals(mode.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCropMode = mode.name }
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Max File Size Ceiling (KB)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Maximum File Size Ceiling", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Enforce max size (e.g. 2048 KB for YouTube)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                            Switch(
                                checked = hasMaxLimit,
                                onCheckedChange = { hasMaxLimit = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF38BDF8),
                                    checkedTrackColor = Color(0xFF0369A1)
                                )
                            )
                        }

                        if (hasMaxLimit) {
                            OutlinedTextField(
                                value = maxKbInput,
                                onValueChange = { maxKbInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Max Size (KB)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                suffix = { Text("KB", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("social_max_kb_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(500, 1024, 2048, 5120, 10240).forEach { kb ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (maxKbInput == kb.toString()) Color(0xFF38BDF8) else Color(0xFF334155),
                                        modifier = Modifier.clickable { maxKbInput = kb.toString() }
                                    ) {
                                        Text(
                                            text = "$kb KB",
                                            color = if (maxKbInput == kb.toString()) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Platform Guidance / Notes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Guidance Notes / Safe Zones", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("e.g. Keep text in center safe area 1546x423 px") },
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("social_note_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validPlatform = platform.ifBlank { "Custom" }
                    val validName = name.ifBlank { "Preset ${parsedW}x${parsedH}" }
                    val parsedDpi = dpiInput.toIntOrNull() ?: 72
                    val parsedMaxKb = if (hasMaxLimit) maxKbInput.toIntOrNull() else null

                    onConfirm(
                        validPlatform,
                        validName,
                        selectedCategory,
                        parsedW,
                        parsedH,
                        computedRatio,
                        selectedFormat,
                        parsedMaxKb,
                        parsedDpi,
                        selectedCropMode,
                        note
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                modifier = Modifier.testTag("save_social_preset_confirm_button")
            ) {
                Text("Save Spec", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun PassportPresetCard(preset: PassportPreset, onApply: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${preset.country} - ${preset.documentType}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = preset.category.label,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (preset.maxFileSizeKb != null) {
                        Text(
                            text = "• Max: ≤${preset.maxFileSizeKb} KB",
                            color = Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "${preset.widthMm}×${preset.heightMm} mm (${preset.widthPxAt300Dpi}×${preset.heightPxAt300Dpi} px @${preset.defaultDpi}DPI)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Text(
                    text = "Background: ${preset.backgroundColor}",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    contentColor = Color(0xFF38BDF8)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
