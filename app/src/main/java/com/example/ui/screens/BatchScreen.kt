package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.BatchCompressionMode
import com.example.model.BatchFilterOption
import com.example.model.BatchItem
import com.example.model.BatchResizeOption
import com.example.model.BatchSortOption
import com.example.model.BatchStatus
import com.example.model.CompressionPreset
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.NamingMode
import com.example.model.OutputDestinationType
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.processing.DpiPrintEngine
import com.example.ui.viewmodel.UtilityViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    viewModel: UtilityViewModel,
    onBack: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val config = state.batchConfig
    val outputConfig = state.outputFileConfig
    val summary = state.batchSummaryReport

    val openBatchFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.setSafTreeUri(uri)
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addBatchUris(uris)
        }
    }

    val multiDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addBatchUris(uris)
        }
    }

    var showOptionsPanel by remember { mutableStateOf(true) }
    var exactAspectLocked by remember { mutableStateOf(true) }
    var exactWidthInput by remember(config.exactWidth) { mutableStateOf(config.exactWidth.toString()) }
    var exactHeightInput by remember(config.exactHeight) { mutableStateOf(config.exactHeight.toString()) }
    var sameWidthInput by remember(config.sameWidth) { mutableStateOf(config.sameWidth.toString()) }
    var sameHeightInput by remember(config.sameHeight) { mutableStateOf(config.sameHeight.toString()) }
    var longestEdgeInput by remember(config.longestEdge) { mutableStateOf(config.longestEdge.toString()) }
    var physicalWidthInput by remember(config.physicalWidth) { mutableStateOf(config.physicalWidth.toString()) }
    var physicalHeightInput by remember(config.physicalHeight) { mutableStateOf(config.physicalHeight.toString()) }
    var customKbInput by remember(config.targetMaxKb) { mutableStateOf(config.targetMaxKb?.toString() ?: "") }

    val totalCount = state.batchItems.size
    val selectedCount = state.batchItems.count { it.selected }
    val completedCount = state.batchItems.count { it.status == BatchStatus.COMPLETED }
    val failedCount = state.batchItems.count { it.status == BatchStatus.FAILED }
    val cancelledCount = state.batchItems.count { it.status == BatchStatus.CANCELLED }
    val eligiblePendingCount = state.batchItems.count { item ->
        (item.status == BatchStatus.PENDING || item.status == BatchStatus.PROCESSING) &&
            (!config.processSelectedOnly || item.selected)
    }

    val filteredItems = remember(state.batchItems, state.batchFilterOption) {
        viewModel.batchProcessingUseCase.filterItems(state.batchItems, state.batchFilterOption)
    }

    val overallProgress = if (state.isBatchProcessing) {
        state.batchOverallProgress.coerceIn(0f, 1f)
    } else if (totalCount > 0) {
        (completedCount + failedCount + cancelledCount).toFloat() / totalCount
    } else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Batch Processing Studio",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        )
                        Text(
                            text = "$totalCount queued • $selectedCount selected • $completedCount done",
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("batch_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOptionsPanel = !showOptionsPanel },
                        modifier = Modifier.testTag("batch_toggle_settings_button")
                    ) {
                        Icon(
                            imageVector = if (showOptionsPanel) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Toggle Settings",
                            tint = palette.accentPrimary
                        )
                    }
                    if (state.batchItems.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearBatch() },
                            modifier = Modifier.testTag("batch_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Queue",
                                tint = palette.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.topBarBackground)
            )
        },
        containerColor = palette.appBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Live Progress Banner OR Summary Analytics Banner
            if (state.isBatchProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("batch_progress_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF38BDF8)))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Processing ${(state.batchCurrentProcessingIndex + 1).coerceAtLeast(1)} of $totalCount (${(overallProgress * 100).roundToInt()}%)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.cancelBatchProcessing() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("batch_banner_cancel_button")
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (state.batchCurrentFileName.isNotBlank() || state.batchCurrentStage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.batchCurrentFileName} • ${state.batchCurrentStage}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.batchElapsedTimeMs > 0) {
                                    Text(
                                        text = String.format(Locale.US, "%.1fs", state.batchElapsedTimeMs / 1000f),
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            } else if (totalCount > 0) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(label = "Total", count = totalCount, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                        StatusBadge(label = "Selected", count = selectedCount, color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        StatusBadge(label = "Done", count = completedCount, color = Color(0xFF34D399), modifier = Modifier.weight(1f))
                        if (failedCount > 0) {
                            StatusBadge(label = "Failed", count = failedCount, color = Color(0xFFF87171), modifier = Modifier.weight(1f))
                        }
                        if (cancelledCount > 0) {
                            StatusBadge(label = "Cancelled", count = cancelledCount, color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                        }
                    }

                    // Batch Savings & Reprocess Summary Banner when items are completed
                    if (completedCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_summary_card"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val origStr = formatByteCount(summary.totalOriginalBytes)
                                    val outStr = formatByteCount(summary.totalOutputBytes)
                                    val savedStr = formatByteCount(summary.totalSavedBytes)
                                    val pctStr = String.format(Locale.US, "%.1f%%", summary.averageCompressionPercent)
                                    Text(
                                        text = "Batch Output: $origStr → $outStr (Saved $savedStr / $pctStr)",
                                        color = Color(0xFFA7F3D0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Avg ${summary.averageTimePerItemMs} ms/photo • ${config.dpi} DPI • ${if (config.keepOriginalFormat) "Original Format" else config.format.extension.uppercase()}",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 10.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetCompletedBatchItems() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("batch_reprocess_all_button"),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                                    border = BorderStroke(1.dp, Color(0xFF34D399))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reprocess", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Collapsible Batch Configuration Panel
            AnimatedVisibility(visible = showOptionsPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        // SECTION 1: BATCH RESIZE
                        Text(
                            text = "1. BATCH RESIZE & PHYSICAL DIMENSIONS",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BatchResizeOption.values().forEach { opt ->
                                val sel = config.resizeOption == opt
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (sel) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.updateBatchConfig { it.copy(resizeOption = opt) } }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("batch_resize_opt_${opt.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt.label,
                                        color = if (sel) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.ORIGINAL) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Preserves each photo's original pixel width & height (100% scale).",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }

                        if (config.resizeOption == BatchResizeOption.PERCENTAGE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Scale: ${config.scalePercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(25, 50, 75, 100, 150, 200).forEach { p ->
                                        FilterChip(
                                            selected = config.scalePercent == p,
                                            onClick = { viewModel.updateBatchConfig { it.copy(scalePercent = p) } },
                                            label = { Text("$p%", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A),
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFE2E8F0)
                                            )
                                        )
                                    }
                                }
                            }
                            Slider(
                                value = config.scalePercent.toFloat(),
                                onValueChange = { viewModel.updateBatchConfig { c -> c.copy(scalePercent = it.roundToInt()) } },
                                valueRange = 10f..200f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF334155)
                                )
                            )
                        }

                        if (config.resizeOption == BatchResizeOption.SAME_WIDTH) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = sameWidthInput,
                                onValueChange = {
                                    sameWidthInput = it
                                    val w = it.toIntOrNull()
                                    if (w != null && w > 0) {
                                        viewModel.updateBatchConfig { c -> c.copy(sameWidth = w) }
                                    }
                                },
                                label = { Text("Target Width (px) — Height Auto-Scales", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(720 to "720px", 1080 to "1080px", 1440 to "1440px", 1920 to "1920px", 3840 to "4K 3840px").forEach { (w, label) ->
                                    FilterChip(
                                        selected = config.sameWidth == w,
                                        onClick = {
                                            sameWidthInput = w.toString()
                                            viewModel.updateBatchConfig { it.copy(sameWidth = w) }
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.SAME_HEIGHT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = sameHeightInput,
                                onValueChange = {
                                    sameHeightInput = it
                                    val h = it.toIntOrNull()
                                    if (h != null && h > 0) {
                                        viewModel.updateBatchConfig { c -> c.copy(sameHeight = h) }
                                    }
                                },
                                label = { Text("Target Height (px) — Width Auto-Scales", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(720 to "720px", 1080 to "1080px", 1350 to "1350px", 1920 to "1920px", 2160 to "2160px").forEach { (h, label) ->
                                    FilterChip(
                                        selected = config.sameHeight == h,
                                        onClick = {
                                            sameHeightInput = h.toString()
                                            viewModel.updateBatchConfig { it.copy(sameHeight = h) }
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.LONGEST_EDGE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = longestEdgeInput,
                                onValueChange = {
                                    longestEdgeInput = it
                                    val edge = it.toIntOrNull()
                                    if (edge != null && edge > 0) {
                                        viewModel.updateBatchConfig { c -> c.copy(longestEdge = edge) }
                                    }
                                },
                                label = { Text("Max Longest Side (px) — Preserves Aspect Ratio", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(800, 1080, 1440, 1920, 2048, 3840).forEach { edge ->
                                    FilterChip(
                                        selected = config.longestEdge == edge,
                                        onClick = {
                                            longestEdgeInput = edge.toString()
                                            viewModel.updateBatchConfig { it.copy(longestEdge = edge) }
                                        },
                                        label = { Text("${edge}px", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.EXACT_DIMENSIONS) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = exactWidthInput,
                                    onValueChange = {
                                        exactWidthInput = it
                                        val w = it.toIntOrNull()
                                        if (w != null && w > 0) {
                                            viewModel.updateBatchConfig { c ->
                                                if (exactAspectLocked && c.exactWidth > 0) {
                                                    val ratio = c.exactHeight.toFloat() / c.exactWidth
                                                    val newH = (w * ratio).roundToInt().coerceAtLeast(1)
                                                    exactHeightInput = newH.toString()
                                                    c.copy(exactWidth = w, exactHeight = newH)
                                                } else {
                                                    c.copy(exactWidth = w)
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("Width px", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                IconButton(onClick = { exactAspectLocked = !exactAspectLocked }) {
                                    Icon(
                                        imageVector = if (exactAspectLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock Aspect",
                                        tint = if (exactAspectLocked) Color(0xFF38BDF8) else Color(0xFF64748B)
                                    )
                                }

                                OutlinedTextField(
                                    value = exactHeightInput,
                                    onValueChange = {
                                        exactHeightInput = it
                                        val h = it.toIntOrNull()
                                        if (h != null && h > 0) {
                                            viewModel.updateBatchConfig { c ->
                                                if (exactAspectLocked && c.exactHeight > 0) {
                                                    val ratio = c.exactWidth.toFloat() / c.exactHeight
                                                    val newW = (h * ratio).roundToInt().coerceAtLeast(1)
                                                    exactWidthInput = newW.toString()
                                                    c.copy(exactWidth = newW, exactHeight = h)
                                                } else {
                                                    c.copy(exactHeight = h)
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("Height px", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Triple("1:1 Square", 1080, 1080),
                                    Triple("16:9 FHD", 1920, 1080),
                                    Triple("9:16 Story", 1080, 1920),
                                    Triple("4:5 IG", 1080, 1350),
                                    Triple("2×2 Passport", 600, 600),
                                    Triple("SSC Sign", 140, 60)
                                ).forEach { (presetName, w, h) ->
                                    FilterChip(
                                        selected = config.exactWidth == w && config.exactHeight == h,
                                        onClick = {
                                            exactWidthInput = w.toString()
                                            exactHeightInput = h.toString()
                                            viewModel.updateBatchConfig { it.copy(exactWidth = w, exactHeight = h) }
                                        },
                                        label = { Text(presetName, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.PHYSICAL_PRINT) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = physicalWidthInput,
                                    onValueChange = {
                                        physicalWidthInput = it
                                        val w = it.toFloatOrNull()
                                        if (w != null && w > 0f) {
                                            viewModel.updateBatchConfig { c -> c.copy(physicalWidth = w) }
                                        }
                                    },
                                    label = { Text("Width (${config.physicalUnit.symbol})", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                OutlinedTextField(
                                    value = physicalHeightInput,
                                    onValueChange = {
                                        physicalHeightInput = it
                                        val h = it.toFloatOrNull()
                                        if (h != null && h > 0f) {
                                            viewModel.updateBatchConfig { c -> c.copy(physicalHeight = h) }
                                        }
                                    },
                                    label = { Text("Height (${config.physicalUnit.symbol})", fontSize = 10.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PrintUnit.values().forEach { unit ->
                                        FilterChip(
                                            selected = config.physicalUnit == unit,
                                            onClick = { viewModel.updateBatchConfig { it.copy(physicalUnit = unit) } },
                                            label = { Text(unit.symbol, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A),
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFE2E8F0)
                                            )
                                        )
                                    }
                                }
                                val (pxW, pxH) = DpiPrintEngine.computePixelsFromPhysical(
                                    config.physicalWidth,
                                    config.physicalHeight,
                                    config.physicalUnit,
                                    config.dpi
                                )
                                Text(
                                    text = "→ ${pxW}×${pxH} px @ ${config.dpi} DPI",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (config.resizeOption == BatchResizeOption.EXACT_DIMENSIONS ||
                            config.resizeOption == BatchResizeOption.PHYSICAL_PRINT
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ResizeMode.values().forEach { mode ->
                                    val sel = config.cropMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (sel) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.updateBatchConfig { it.copy(cropMode = mode) } }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = if (sel) Color(0xFF0F172A) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (config.resizeOption != BatchResizeOption.ORIGINAL) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Do Not Enlarge Smaller Images",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                                Switch(
                                    checked = config.doNotUpscale,
                                    onCheckedChange = { viewModel.updateBatchConfig { c -> c.copy(doNotUpscale = it) } },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECTION 2: BATCH COMPRESSION
                        Text(
                            text = "2. BATCH COMPRESSION & TARGET FILE SIZE",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BatchCompressionMode.values().forEach { mode ->
                                FilterChip(
                                    selected = config.compressionMode == mode,
                                    onClick = { viewModel.updateBatchConfig { it.copy(compressionMode = mode) } },
                                    label = { Text(mode.label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF10B981),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        when (config.compressionMode) {
                            BatchCompressionMode.QUALITY -> {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Quality Percentage:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("${config.quality}%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Slider(
                                    value = config.quality.toFloat(),
                                    onValueChange = { viewModel.updateBatchConfig { c -> c.copy(quality = it.roundToInt()) } },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF38BDF8),
                                        activeTrackColor = Color(0xFF38BDF8),
                                        inactiveTrackColor = Color(0xFF334155)
                                    )
                                )
                            }
                            BatchCompressionMode.PRESET -> {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompressionPreset.values().filter { it != CompressionPreset.CUSTOM }.forEach { preset ->
                                        FilterChip(
                                            selected = config.compressionPreset == preset,
                                            onClick = {
                                                viewModel.updateBatchConfig {
                                                    it.copy(
                                                        compressionPreset = preset,
                                                        quality = preset.defaultQuality
                                                    )
                                                }
                                            },
                                            label = { Text("${preset.displayName} (${preset.defaultQuality}%)", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A),
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFE2E8F0)
                                            )
                                        )
                                    }
                                }
                            }
                            BatchCompressionMode.EXACT_TARGET_KB,
                            BatchCompressionMode.MAX_CEILING_KB -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(20, 50, 100, 200, 500, 1000).forEach { kb ->
                                        FilterChip(
                                            selected = config.targetMaxKb == kb,
                                            onClick = {
                                                customKbInput = kb.toString()
                                                viewModel.updateBatchConfig { it.copy(targetMaxKb = kb) }
                                            },
                                            label = { Text("$kb KB", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF10B981),
                                                selectedLabelColor = Color(0xFF0F172A),
                                                containerColor = Color(0xFF0F172A),
                                                labelColor = Color(0xFFE2E8F0)
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customKbInput,
                                        onValueChange = {
                                            customKbInput = it
                                            val parsed = it.toIntOrNull()
                                            viewModel.updateBatchConfig { c -> c.copy(targetMaxKb = parsed?.takeIf { v -> v > 0 }) }
                                        },
                                        label = { Text("Custom KB Limit", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = config.allowDimensionDownscalingForKb,
                                        onClick = {
                                            viewModel.updateBatchConfig {
                                                it.copy(allowDimensionDownscalingForKb = !it.allowDimensionDownscalingForKb)
                                            }
                                        },
                                        label = { Text("Auto-Downscale if needed", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECTION 3: BATCH FORMAT CONVERSION
                        Text(
                            text = "3. BATCH FORMAT CONVERSION",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = config.keepOriginalFormat,
                                onClick = { viewModel.updateBatchConfig { it.copy(keepOriginalFormat = true) } },
                                label = { Text("Keep Original Format", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF38BDF8),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFE2E8F0)
                                )
                            )
                            ExportFormat.values().forEach { fmt ->
                                FilterChip(
                                    selected = !config.keepOriginalFormat && config.format == fmt,
                                    onClick = {
                                        viewModel.updateBatchConfig {
                                            it.copy(keepOriginalFormat = false, format = fmt)
                                        }
                                    },
                                    label = { Text(fmt.displayName, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        if (!config.keepOriginalFormat && config.format == ExportFormat.JPEG) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Alpha Matte (PNG→JPG):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                listOf(
                                    "White" to android.graphics.Color.WHITE,
                                    "Black" to android.graphics.Color.BLACK,
                                    "Light Gray" to android.graphics.Color.rgb(240, 240, 240)
                                ).forEach { (lbl, col) ->
                                    FilterChip(
                                        selected = config.jpegBackgroundColor == col,
                                        onClick = { viewModel.updateBatchConfig { it.copy(jpegBackgroundColor = col) } },
                                        label = { Text(lbl, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A),
                                            containerColor = Color(0xFF0F172A),
                                            labelColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECTION 4: BATCH METADATA, PRIVACY & DPI
                        Text(
                            text = "4. BATCH METADATA, PRIVACY & BINARY DPI",
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MetadataPolicy.values().forEach { policy ->
                                FilterChip(
                                    selected = config.metadataPolicy == policy,
                                    onClick = { viewModel.updateBatchConfig { it.copy(metadataPolicy = policy) } },
                                    label = { Text(policy.label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF59E0B),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        if (config.metadataPolicy == MetadataPolicy.CUSTOM_SELECTIVE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = config.removeGps,
                                    onClick = { viewModel.updateBatchConfig { it.copy(removeGps = !it.removeGps) } },
                                    label = { Text("Scrub GPS", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEF4444),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                                FilterChip(
                                    selected = config.removeCameraInfo,
                                    onClick = { viewModel.updateBatchConfig { it.copy(removeCameraInfo = !it.removeCameraInfo) } },
                                    label = { Text("Scrub Camera", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEF4444),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                                FilterChip(
                                    selected = config.removeDeviceInfo,
                                    onClick = { viewModel.updateBatchConfig { it.copy(removeDeviceInfo = !it.removeDeviceInfo) } },
                                    label = { Text("Scrub Serial", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEF4444),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                                FilterChip(
                                    selected = config.removeDateMetadata,
                                    onClick = { viewModel.updateBatchConfig { it.copy(removeDateMetadata = !it.removeDateMetadata) } },
                                    label = { Text("Scrub Dates", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEF4444),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = config.customArtist,
                                onValueChange = { artist -> viewModel.updateBatchConfig { it.copy(customArtist = artist) } },
                                label = { Text("Batch Artist Tag", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = config.customCopyright,
                                onValueChange = { copy -> viewModel.updateBatchConfig { it.copy(customCopyright = copy) } },
                                label = { Text("Batch Copyright ©", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DPI:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            listOf(72, 96, 150, 200, 300, 600).forEach { dpiVal ->
                                FilterChip(
                                    selected = config.dpi == dpiVal,
                                    onClick = { viewModel.updateBatchConfig { it.copy(dpi = dpiVal) } },
                                    label = { Text("$dpiVal DPI", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECTION 5: ROTATE, FLIP, OUTPUT NAMING & DESTINATION
                        Text(
                            text = "5. ROTATION & OUTPUT DESTINATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0, 90, 180, 270).forEach { deg ->
                                val sel = config.rotationDegrees == deg
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (sel) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.updateBatchConfig { it.copy(rotationDegrees = deg) } }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$deg°",
                                        color = if (sel) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            FilterChip(
                                selected = config.flipHorizontal,
                                onClick = { viewModel.updateBatchConfig { it.copy(flipHorizontal = !it.flipHorizontal) } },
                                label = { Text("Flip H", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFE2E8F0)
                                )
                            )

                            FilterChip(
                                selected = config.flipVertical,
                                onClick = { viewModel.updateBatchConfig { it.copy(flipVertical = !it.flipVertical) } },
                                label = { Text("Flip V", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                NamingMode.AUTOMATIC_TIMESTAMP,
                                NamingMode.ORIGINAL_PRESERVED,
                                NamingMode.PATTERN_TEMPLATE
                            ).forEach { mode ->
                                FilterChip(
                                    selected = outputConfig.namingMode == mode,
                                    onClick = { viewModel.setNamingMode(mode) },
                                    label = { Text(mode.displayName, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF38BDF8),
                                        selectedLabelColor = Color(0xFF0F172A),
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFFE2E8F0)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { viewModel.setOutputDestinationType(OutputDestinationType.PUBLIC_MEDIASTORE) },
                                color = if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color(0xFF10B981) else Color(0xFF334155)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Media Gallery", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                onClick = { openBatchFolderLauncher.launch(null) },
                                color = if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color(0xFF10B981) else Color(0xFF334155)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = outputConfig.safFolderDisplayName ?: "SAF Folder",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Multi-Select Toolbar & Filter/Sort Strip (when items exist)
            if (state.batchItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedCount == totalCount && totalCount > 0,
                            onClick = {
                                if (selectedCount == totalCount) viewModel.deselectAllBatchItems()
                                else viewModel.selectAllBatchItems()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedCount == totalCount) Icons.Default.CheckBox else Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(if (selectedCount == totalCount) "Deselect All" else "Select All ($totalCount)", fontSize = 10.sp) },
                            modifier = Modifier.testTag("batch_select_all_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )

                        FilterChip(
                            selected = false,
                            onClick = { viewModel.invertBatchSelection() },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Invert", fontSize = 10.sp) },
                            modifier = Modifier.testTag("batch_invert_selection_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )

                        if (selectedCount > 0) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.removeSelectedBatchItems() },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp)) },
                                label = { Text("Remove Selected ($selectedCount)", fontSize = 10.sp, color = Color(0xFFF87171)) },
                                modifier = Modifier.testTag("batch_remove_selected_chip"),
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF1E293B))
                            )
                        }

                        FilterChip(
                            selected = config.processSelectedOnly,
                            onClick = { viewModel.updateBatchConfig { it.copy(processSelectedOnly = !it.processSelectedOnly) } },
                            label = { Text("Process Selected Only", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )

                        // Filter options
                        BatchFilterOption.values().forEach { filterOpt ->
                            FilterChip(
                                selected = state.batchFilterOption == filterOpt,
                                onClick = { viewModel.setBatchFilterOption(filterOpt) },
                                label = { Text(filterOpt.label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0EA5E9),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }

                        // Sort options
                        BatchSortOption.values().forEach { sortOpt ->
                            FilterChip(
                                selected = state.batchSortOption == sortOpt,
                                onClick = { viewModel.setBatchSortOption(sortOpt) },
                                label = { Text("Sort: ${sortOpt.label}", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color(0xFF0F172A),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }

            // 4. Primary Action Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        multiPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("batch_add_photos_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Photos", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        multiDocumentPicker.launch(arrayOf("image/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("batch_add_files_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Files", color = Color.White, fontSize = 11.sp)
                }

                if (failedCount > 0 || cancelledCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.retryFailedBatchItems() },
                        enabled = !state.isBatchProcessing,
                        modifier = Modifier
                            .weight(0.95f)
                            .testTag("batch_retry_failed_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry (${failedCount + cancelledCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.isBatchProcessing) {
                    Button(
                        onClick = { viewModel.cancelBatchProcessing() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("batch_cancel_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.runBatchProcessing() },
                        enabled = state.batchItems.isNotEmpty() && eligiblePendingCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("run_batch_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Process ($eligiblePendingCount)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // 5. Batch Queue List
            if (state.batchItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No images in batch queue",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Photos' or 'Files' above to multi-select images for batch resize, compression, format conversion, metadata scrubbing, and binary DPI injection.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredItems,
                        key = { it.id }
                    ) { item ->
                        BatchItemRow(
                            item = item,
                            onToggleSelect = { viewModel.toggleBatchItemSelection(item.id) },
                            onRetry = { viewModel.retrySingleBatchItem(item.id) },
                            onCancel = { viewModel.cancelSingleBatchItem(item.id) },
                            onRemove = { viewModel.removeBatchItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatByteCount(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb >= 1024.0) {
        String.format(Locale.US, "%.2f MB", kb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

@Composable
fun StatusBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

@Composable
fun BatchItemRow(
    item: BatchItem,
    onToggleSelect: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.selected) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = if (item.selected) BorderStroke(1.dp, Color(0xFF334155)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_item_row_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF38BDF8),
                    uncheckedColor = Color(0xFF64748B),
                    checkmarkColor = Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .size(28.dp)
                    .testTag("batch_item_checkbox_${item.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = item.uri,
                contentDescription = item.originalName,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.outputFileName ?: item.originalName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val inFmt = item.detectedFormat.shortName
                    val outFmt = item.outputFormat?.extension?.uppercase()
                    val fmtBadge = if (outFmt != null) "$inFmt→$outFmt" else inFmt
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = fmtBadge,
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                if (item.status == BatchStatus.COMPLETED && item.outputWidth != null && item.outputHeight != null) {
                    Text(
                        text = "${item.originalWidth}×${item.originalHeight} → ${item.outputWidth}×${item.outputHeight} px",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val origSizeStr = formatByteCount(item.originalSize)
                    val outSizeStr = formatByteCount(item.outputSize)
                    val savingsPart = if (item.savedPercent > 0.5f) {
                        String.format(Locale.US, " (-%.0f%%)", item.savedPercent)
                    } else ""
                    Text(
                        text = "$origSizeStr → $outSizeStr$savingsPart • ${item.metadataSummary ?: "Verified"}",
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val origSizeStr = if (item.originalSize > 0) " • ${formatByteCount(item.originalSize)}" else ""
                    val exifTag = when {
                        item.hasGps -> " • GPS+EXIF"
                        item.hasExif -> " • EXIF"
                        else -> ""
                    }
                    Text(
                        text = "${item.originalWidth}×${item.originalHeight} px$origSizeStr$exifTag",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                if ((item.status == BatchStatus.FAILED || item.status == BatchStatus.CANCELLED) && !item.errorMessage.isNullOrBlank()) {
                    Text(
                        text = item.errorMessage,
                        color = if (item.status == BatchStatus.FAILED) Color(0xFFF87171) else Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                if (item.status == BatchStatus.PROCESSING) {
                    Spacer(modifier = Modifier.height(3.dp))
                    if (item.stageLabel.isNotBlank()) {
                        Text(
                            text = item.stageLabel,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF334155)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            when (item.status) {
                BatchStatus.PENDING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pending",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("batch_item_remove_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                BatchStatus.PROCESSING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("batch_item_cancel_${item.id}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Item", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                BatchStatus.COMPLETED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("batch_item_remove_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                BatchStatus.FAILED,
                BatchStatus.CANCELLED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("batch_item_retry_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry Item",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("batch_item_remove_${item.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
