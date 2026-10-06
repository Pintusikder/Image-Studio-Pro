package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.HistoryEntity
import com.example.processing.BitmapUtils
import com.example.processing.ExportEngine
import com.example.processing.OutputFileManager
import com.example.ui.viewmodel.UtilityViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistorySortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    LARGEST_SIZE("Largest File Size"),
    SMALLEST_SIZE("Smallest File Size"),
    OPERATION_TYPE("Operation Type"),
    NAME_ASC("Name (A–Z)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: UtilityViewModel,
    onNavigateToEditor: () -> Unit = {},
    onBack: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    var selectedFilterCategory by remember { mutableStateOf("ALL") } // ALL, FAVORITES, RESIZE, CROP, PRESETS, BATCH
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(HistorySortOrder.NEWEST_FIRST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<HistoryEntity?>(null) }
    var deletePhysicalFileOnRemove by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<HistoryEntity?>(null) }
    var detailItem by remember { mutableStateOf<HistoryEntity?>(null) }

    // Filter and Sort Logic
    val filteredItems = remember(allHistory, favorites, selectedFilterCategory, searchQuery, sortOrder) {
        val baseList = when (selectedFilterCategory) {
            "FAVORITES" -> favorites
            "RESIZE" -> allHistory.filter { it.operationType.contains("Resize", ignoreCase = true) || it.operationType.contains("Dimension", ignoreCase = true) }
            "CROP" -> allHistory.filter { it.operationType.contains("Crop", ignoreCase = true) }
            "PRESETS" -> allHistory.filter { it.operationType.contains("Preset", ignoreCase = true) || it.operationType.contains("Social", ignoreCase = true) }
            "BATCH" -> allHistory.filter { it.operationType.contains("Batch", ignoreCase = true) }
            else -> allHistory
        }

        val searchFiltered = if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.title.lowercase().contains(q) ||
                it.operationType.lowercase().contains(q) ||
                it.operationDetails.lowercase().contains(q) ||
                it.format.lowercase().contains(q) ||
                (it.outputFileName.lowercase().contains(q)) ||
                (it.originalFileName?.lowercase()?.contains(q) == true)
            }
        }

        when (sortOrder) {
            HistorySortOrder.NEWEST_FIRST -> searchFiltered.sortedByDescending { it.timestamp }
            HistorySortOrder.OLDEST_FIRST -> searchFiltered.sortedBy { it.timestamp }
            HistorySortOrder.LARGEST_SIZE -> searchFiltered.sortedByDescending { it.fileSizeBytes }
            HistorySortOrder.SMALLEST_SIZE -> searchFiltered.sortedBy { it.fileSizeBytes }
            HistorySortOrder.OPERATION_TYPE -> searchFiltered.sortedBy { it.operationType }
            HistorySortOrder.NAME_ASC -> searchFiltered.sortedBy { it.outputFileName.ifBlank { it.title }.lowercase() }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Operation History",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        )
                        Text(
                            text = "${allHistory.size} operations recorded",
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
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier.testTag("history_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded) Color(0xFF38BDF8) else Color.White
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("history_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            HistorySortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = order.label,
                                                color = if (sortOrder == order) Color(0xFF38BDF8) else Color.White,
                                                fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (sortOrder == order) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (allHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All History",
                                tint = Color(0xFFF87171)
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
            // Search field when expanded
            AnimatedVisibility(visible = isSearchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = { Text("Search by operation, format, file...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Category Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterOptions = listOf(
                    "ALL" to "All (${allHistory.size})",
                    "FAVORITES" to "★ Favorites (${favorites.size})",
                    "RESIZE" to "Resize",
                    "CROP" to "Crop",
                    "PRESETS" to "Presets",
                    "BATCH" to "Batch"
                )

                filterOptions.forEach { (catKey, label) ->
                    val isSelected = selectedFilterCategory == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterCategory = catKey },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (catKey == "FAVORITES") Color(0xFFF43F5E) else Color(0xFF38BDF8),
                            selectedLabelColor = if (catKey == "FAVORITES") Color.White else Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color.Transparent else Color(0xFF334155),
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Content List or Empty State
            if (filteredItems.isEmpty()) {
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
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedFilterCategory == "FAVORITES") Icons.Default.FavoriteBorder else Icons.Default.Collections,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = when {
                                searchQuery.isNotEmpty() -> "No operations matching \"$searchQuery\""
                                selectedFilterCategory == "FAVORITES" -> "No starred favorite operations yet"
                                else -> "No operation history yet"
                            },
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Operations like resize, crop, convert, and batch export will automatically record here with full metadata.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            onOpen = { detailItem = item },
                            onReuse = {
                                viewModel.reuseHistorySettings(item) {
                                    scope.launch {
                                        val snack = snackbarHostState.showSnackbar(
                                            message = "Settings applied: ${item.width}×${item.height} px (${item.format})",
                                            actionLabel = "Open Editor",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (snack == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            onNavigateToEditor()
                                        }
                                    }
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onRename = { itemToRename = item },
                            onDelete = {
                                deletePhysicalFileOnRemove = false
                                itemToDelete = item
                            },
                            onShare = {
                                shareHistoryItem(context, item)
                            }
                        )
                    }
                }
            }
        }
    }

    // Rename History File & Record Dialog
    itemToRename?.let { item ->
        val initialBase = item.outputFileName.ifBlank { item.title }.substringBeforeLast('.')
        var newNameInput by remember(item.id) { mutableStateOf(initialBase) }
        val sanitization = remember(newNameInput) { OutputFileManager.sanitizeFilename(newNameInput) }
        val ext = item.format.lowercase(Locale.US).let { if (it == "jpeg") "jpg" else it }

        AlertDialog(
            onDismissRequest = { itemToRename = null },
            containerColor = Color(0xFF1E293B),
            modifier = Modifier.testTag("history_rename_dialog"),
            title = { Text("Rename Exported File", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a new filename for this exported image and history record:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("New Filename") },
                        suffix = { Text(".$ext", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_rename_input")
                    )
                    Text(
                        text = "Preview: ${sanitization.sanitizedName}.$ext",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameHistoryItem(item, newNameInput) { outcome ->
                            if (detailItem?.id == item.id) {
                                detailItem = outcome.updatedEntity
                            }
                            itemToRename = null
                            scope.launch {
                                snackbarHostState.showSnackbar(outcome.message)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("history_rename_confirm_button")
                ) {
                    Text("Rename", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Single item delete confirmation dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete Operation Record?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Remove history entry '${item.outputFileName.ifBlank { item.title }}' (${item.operationType})?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    if (item.outputUriString.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .clickable { deletePhysicalFileOnRemove = !deletePhysicalFileOnRemove }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("delete_physical_file_checkbox")
                        ) {
                            Checkbox(
                                checked = deletePhysicalFileOnRemove,
                                onCheckedChange = { deletePhysicalFileOnRemove = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFEF4444),
                                    uncheckedColor = Color(0xFF64748B)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Also delete exported file from device storage",
                                color = if (deletePhysicalFileOnRemove) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistory(item, deletePhysicalFile = deletePhysicalFileOnRemove) { outcome ->
                            itemToDelete = null
                            scope.launch {
                                snackbarHostState.showSnackbar(outcome.message)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(if (deletePhysicalFileOnRemove) "Delete File & Record" else "Delete Record", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Clear All confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Clear All Operation History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will remove all ${allHistory.size} operation history records from the app database. Your exported files on disk will not be deleted.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showDeleteAllDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("All history records cleared")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Full Detailed Inspector / Open Dialog
    detailItem?.let { item ->
        HistoryDetailDialog(
            item = item,
            onDismiss = { detailItem = null },
            onOpenInEditor = {
                viewModel.openHistoryImageInEditor(item) {
                    detailItem = null
                    onNavigateToEditor()
                }
            },
            onReuseSettings = {
                viewModel.reuseHistorySettings(item) {
                    detailItem = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Operation settings applied to workspace")
                    }
                    onNavigateToEditor()
                }
            },
            onShare = {
                shareHistoryItem(context, item)
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(item)
                detailItem = item.copy(isFavorite = !item.isFavorite)
            },
            onRename = {
                itemToRename = item
            },
            onDelete = {
                deletePhysicalFileOnRemove = false
                itemToDelete = item
                detailItem = null
            }
        )
    }
}

@Composable
fun HistoryCard(
    item: HistoryEntity,
    onOpen: () -> Unit,
    onReuse: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${item.id}")
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Operation Type Badge & Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Operation Pill
                val operationBg = getOperationColor(item.operationType)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = operationBg.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, operationBg.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.operationType.ifBlank { "Export" },
                        color = operationBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Date & Time
                val dateStr = formatHistoryTimestamp(item.timestamp)
                Text(
                    text = dateStr,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Section: Visual Thumbnails & Metadata comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dual Thumbnails (Original ➔ Output)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    val outBmp = item.previewThumbnailBase64?.let { BitmapUtils.decodeThumbnailFromBase64(it) }
                    if (outBmp != null) {
                        Image(
                            bitmap = outBmp.asImageBitmap(),
                            contentDescription = "Output Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF475569)
                        )
                    }

                    // Format Badge Overlay on Output
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(topStart = 6.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = item.format.uppercase(),
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.outputFileName.ifBlank { item.title },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Dimensions comparison: Original -> Output
                    val origDim = if (item.originalWidth > 0 && item.originalHeight > 0) "${item.originalWidth}×${item.originalHeight}" else "Original"
                    val outDim = "${item.width}×${item.height} px"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = origDim,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .size(12.dp)
                                .padding(horizontal = 2.dp)
                        )
                        Text(
                            text = outDim,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // File size comparison & savings
                    val outKb = item.fileSizeBytes / 1024.0
                    val outSizeStr = if (outKb >= 1024) String.format("%.2f MB", outKb / 1024.0) else String.format("%.1f KB", outKb)
                    val origKb = item.originalFileSizeBytes / 1024.0
                    val origSizeStr = if (origKb > 0) {
                        if (origKb >= 1024) String.format("%.2f MB", origKb / 1024.0) else String.format("%.1f KB", origKb)
                    } else null

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = outSizeStr,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (origSizeStr != null && item.originalFileSizeBytes > item.fileSizeBytes && item.fileSizeBytes > 0) {
                            val reduction = ((item.originalFileSizeBytes - item.fileSizeBytes).toDouble() / item.originalFileSizeBytes * 100).toInt()
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "-$reduction%",
                                    color = Color(0xFF34D399),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions Bar (Open, Reuse, Share, Favorite, Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open action
                FilledTonalButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("history_open_button_${item.id}"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF334155),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Reuse action
                FilledTonalButton(
                    onClick = onReuse,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("history_reuse_button_${item.id}"),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF0284C7).copy(alpha = 0.25f),
                        contentColor = Color(0xFF7DD3FC)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reuse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Share action
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("history_share_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Rename action
                IconButton(
                    onClick = onRename,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("history_rename_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Favorite action
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("history_favorite_button_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete action
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("history_delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFF87171).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDetailDialog(
    item: HistoryEntity,
    onDismiss: () -> Unit,
    onOpenInEditor: () -> Unit,
    onReuseSettings: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.outputFileName.ifBlank { item.title },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatHistoryTimestamp(item.timestamp),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    val outBmp = item.previewThumbnailBase64?.let { BitmapUtils.decodeThumbnailFromBase64(it) }
                    if (outBmp != null) {
                        Image(
                            bitmap = outBmp.asImageBitmap(),
                            contentDescription = "Full Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Format & Dimensions pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${item.width} × ${item.height} px • ${item.format}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Breakdown Grid
                Text(
                    text = "OPERATION METADATA",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataRow("Operation", item.operationType)
                        if (item.operationDetails.isNotBlank()) {
                            MetadataRow("Details", item.operationDetails)
                        }

                        val origDim = if (item.originalWidth > 0 && item.originalHeight > 0) "${item.originalWidth} × ${item.originalHeight} px" else "N/A"
                        MetadataRow("Original Size", origDim)
                        MetadataRow("Output Size", "${item.width} × ${item.height} px")

                        val kb = item.fileSizeBytes / 1024.0
                        val sizeStr = if (kb >= 1024) String.format("%.2f MB (%d bytes)", kb / 1024.0, item.fileSizeBytes) else String.format("%.1f KB (%d bytes)", kb, item.fileSizeBytes)
                        MetadataRow("File Size", sizeStr)

                        if (item.originalFileSizeBytes > 0) {
                            val origKb = item.originalFileSizeBytes / 1024.0
                            val origSizeStr = if (origKb >= 1024) String.format("%.2f MB", origKb / 1024.0) else String.format("%.1f KB", origKb)
                            MetadataRow("Original File Size", origSizeStr)
                        }

                        MetadataRow("Format", item.format.uppercase())

                        if (item.outputUriString.isNotBlank()) {
                            MetadataRow("Path", item.outputUriString.substringAfterLast('/'), isMono = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenInEditor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_open_in_editor"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onReuseSettings,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_reuse_settings"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color(0xFF7DD3FC)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reuse Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (item.isFavorite) Color(0xFFF43F5E) else Color(0xFFCBD5E1)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.isFavorite) "Favorited" else "Favorite", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRename,
                        modifier = Modifier.weight(1f).testTag("dialog_rename_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatHistoryTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        diff < 172800_000L -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

fun getOperationColor(opType: String): Color {
    val lower = opType.lowercase()
    return when {
        lower.contains("crop") -> Color(0xFF10B981) // Emerald
        lower.contains("resize") || lower.contains("dimension") -> Color(0xFFF59E0B) // Amber
        lower.contains("social") || lower.contains("preset") -> Color(0xFFA855F7) // Purple
        lower.contains("batch") -> Color(0xFF38BDF8) // Sky Blue
        lower.contains("background") || lower.contains("bg") -> Color(0xFFEC4899) // Pink
        lower.contains("compress") || lower.contains("quality") -> Color(0xFF06B6D4) // Cyan
        lower.contains("sheet") || lower.contains("passport") -> Color(0xFF6366F1) // Indigo
        else -> Color(0xFF38BDF8)
    }
}

fun shareHistoryItem(context: Context, item: HistoryEntity) {
    val mime = when (item.format.uppercase()) {
        "PNG" -> "image/png"
        "WEBP" -> "image/webp"
        "PDF" -> "application/pdf"
        else -> "image/jpeg"
    }
    if (!OutputFileManager.shareUriOrFile(context, item.outputUriString, mime, "Share Image")) {
        val uri = Uri.parse(item.outputUriString)
        val file = uri.path?.let { File(it) }
        if (file != null && file.exists()) {
            ExportEngine.shareImage(context, file, mime)
        }
    }
}
