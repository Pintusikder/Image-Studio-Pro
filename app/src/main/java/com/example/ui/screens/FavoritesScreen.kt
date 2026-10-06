package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FavoriteItemEntity
import com.example.data.local.FavoriteType
import com.example.processing.BitmapUtils
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.UtilityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: UtilityViewModel,
    onNavigateToEditor: (StudioTab) -> Unit,
    onBack: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()

    val allFavorites by viewModel.allFavoriteItems.collectAsStateWithLifecycle()
    var selectedTabType by remember { mutableStateOf<FavoriteType?>(null) } // null = ALL
    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FavoriteItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<FavoriteItemEntity?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addFavoriteImage("Favorited Photo", uri, null)
        }
    }

    val filteredList = remember(allFavorites, selectedTabType, searchQuery) {
        allFavorites.filter { item ->
            val matchType = selectedTabType == null || item.type == selectedTabType
            val matchQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.dataKey.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Favorites Hub",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            )
                            Text(
                                text = "${allFavorites.size} items bookmarked",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = palette.textSecondary
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("favorites_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                actions = {
                    if (allFavorites.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllConfirm = true },
                            modifier = Modifier.testTag("clear_favorites_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All",
                                tint = palette.textSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.topBarBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomDialog = true },
                containerColor = palette.accentPrimary,
                contentColor = palette.accentOnPrimary,
                modifier = Modifier.testTag("add_custom_favorite_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Favorite",
                        fontWeight = FontWeight.Bold
                    )
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
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("favorites_search_input"),
                placeholder = { Text("Search favorites (names, dimensions, targets)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FavoriteFilterChip(
                    label = "⭐ All (${allFavorites.size})",
                    selected = selectedTabType == null,
                    onClick = { selectedTabType = null },
                    testTag = "filter_chip_all"
                )
                FavoriteFilterChip(
                    label = "🖼️ Images (${allFavorites.count { it.type == FavoriteType.IMAGE }})",
                    selected = selectedTabType == FavoriteType.IMAGE,
                    onClick = { selectedTabType = FavoriteType.IMAGE },
                    testTag = "filter_chip_images"
                )
                FavoriteFilterChip(
                    label = "📐 Presets (${allFavorites.count { it.type == FavoriteType.PRESET }})",
                    selected = selectedTabType == FavoriteType.PRESET,
                    onClick = { selectedTabType = FavoriteType.PRESET },
                    testTag = "filter_chip_presets"
                )
                FavoriteFilterChip(
                    label = "🛠️ Recent Tools (${allFavorites.count { it.type == FavoriteType.RECENT_TOOL }})",
                    selected = selectedTabType == FavoriteType.RECENT_TOOL,
                    onClick = { selectedTabType = FavoriteType.RECENT_TOOL },
                    testTag = "filter_chip_tools"
                )
                FavoriteFilterChip(
                    label = "📏 Dimensions (${allFavorites.count { it.type == FavoriteType.CUSTOM_DIMENSION }})",
                    selected = selectedTabType == FavoriteType.CUSTOM_DIMENSION,
                    onClick = { selectedTabType = FavoriteType.CUSTOM_DIMENSION },
                    testTag = "filter_chip_dimensions"
                )
                FavoriteFilterChip(
                    label = "📦 Compression (${allFavorites.count { it.type == FavoriteType.COMPRESSION_TARGET }})",
                    selected = selectedTabType == FavoriteType.COMPRESSION_TARGET,
                    onClick = { selectedTabType = FavoriteType.COMPRESSION_TARGET },
                    testTag = "filter_chip_compression"
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No favorites match '$searchQuery'" else "No Favorites in this category",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Star images, presets, tools, custom dimensions, or compression targets to access them quickly here.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                        Button(
                            onClick = { showAddCustomDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Custom Favorite", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { "${it.type}_${it.id}_${it.dataKey}" }) { favorite ->
                        FavoriteItemCard(
                            item = favorite,
                            onApply = {
                                viewModel.applyFavoriteItem(favorite) { tab ->
                                    onNavigateToEditor(tab)
                                }
                            },
                            onRename = { itemToRename = favorite },
                            onDelete = { itemToDelete = favorite }
                        )
                    }
                }
            }
        }
    }

    // Add Custom Favorite Dialog
    if (showAddCustomDialog) {
        AddCustomFavoriteDialog(
            onDismiss = { showAddCustomDialog = false },
            onAddDimension = { title, w, h, unit, dpi ->
                viewModel.toggleFavoriteDimension(title, w, h, unit, dpi)
                showAddCustomDialog = false
            },
            onAddCompression = { title, targetKb, quality, format ->
                viewModel.toggleFavoriteCompressionTarget(title, targetKb, quality, format)
                showAddCustomDialog = false
            },
            onPickImage = {
                showAddCustomDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }

    // Rename Favorite Dialog
    itemToRename?.let { item ->
        var newTitleInput by remember(item.id) { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            containerColor = Color(0xFF1E293B),
            modifier = Modifier.testTag("rename_favorite_dialog"),
            title = {
                Text(
                    text = "Rename Favorite",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a new label for this bookmarked favorite:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = newTitleInput,
                        onValueChange = { newTitleInput = it },
                        label = { Text("Favorite Name") },
                        singleLine = true,
                        colors = favFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_favorite_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitleInput.isNotBlank()) {
                            viewModel.renameFavoriteItem(item, newTitleInput)
                            itemToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("confirm_rename_favorite_button")
                ) {
                    Text("Save Name", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Text(
                    text = "Remove Favorite?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${item.title}' from your favorites?",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFavorite(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Clear All Favorites?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will remove all saved images, presets, tools, dimensions, and compression targets from favorites.",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTabType == null) {
                            viewModel.clearAllFavorites()
                        } else {
                            viewModel.clearFavoritesByType(selectedTabType!!)
                        }
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun FavoriteFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF38BDF8),
            selectedLabelColor = Color(0xFF0F172A),
            containerColor = Color(0xFF1E293B),
            labelColor = Color(0xFF94A3B8)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
fun FavoriteItemCard(
    item: FavoriteItemEntity,
    onApply: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val (typeColor, typeIcon, typeLabel) = when (item.type) {
        FavoriteType.IMAGE -> Triple(Color(0xFF34D399), Icons.Default.Collections, "IMAGE")
        FavoriteType.PRESET -> Triple(Color(0xFF818CF8), Icons.Default.GridOn, "PRESET")
        FavoriteType.RECENT_TOOL -> Triple(Color(0xFF38BDF8), Icons.Default.Tune, "TOOL")
        FavoriteType.CUSTOM_DIMENSION -> Triple(Color(0xFFA855F7), Icons.Default.Straighten, "DIMENSION")
        FavoriteType.COMPRESSION_TARGET -> Triple(Color(0xFFF59E0B), Icons.Default.Compress, "COMPRESSION")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() }
            .testTag("fav_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon or Image Thumbnail
            if (item.type == FavoriteType.IMAGE && !item.thumbnailBase64.isNullOrBlank()) {
                val thumb = BitmapUtils.decodeThumbnailFromBase64(item.thumbnailBase64)
                if (thumb != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = item.title,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    FavoriteTypeIconBox(icon = typeIcon, color = typeColor)
                }
            } else {
                FavoriteTypeIconBox(icon = typeIcon, color = typeColor)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = typeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.width != null && item.height != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.width}×${item.height} ${item.unit ?: "px"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (item.targetFileSizeKb != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "≤ ${item.targetFileSizeKb} KB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (item.usageCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${item.usageCount}×",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button: Apply / Launch
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeColor.copy(alpha = 0.2f),
                    contentColor = typeColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("apply_fav_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Rename Button
            IconButton(
                onClick = onRename,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("rename_fav_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename Favorite",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Remove Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_fav_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Starred",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FavoriteTypeIconBox(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun AddCustomFavoriteDialog(
    onDismiss: () -> Unit,
    onAddDimension: (title: String, w: Int, h: Int, unit: String, dpi: Int) -> Unit,
    onAddCompression: (title: String, targetKb: Int, quality: Int, format: String) -> Unit,
    onPickImage: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Dimension, 1 = Compression, 2 = Photo

    var dimTitle by remember { mutableStateOf("") }
    var dimWidth by remember { mutableStateOf("1920") }
    var dimHeight by remember { mutableStateOf("1080") }
    var dimUnit by remember { mutableStateOf("PX") }
    var dimDpi by remember { mutableStateOf("300") }

    var compTitle by remember { mutableStateOf("") }
    var compTargetKb by remember { mutableStateOf("100") }
    var compQuality by remember { mutableStateOf("90") }
    var compFormat by remember { mutableStateOf("JPEG") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("Create Custom Favorite", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Type selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("📏 Dimension", "📦 Compress", "🖼️ Photo")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = mode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF38BDF8) else Color.Transparent)
                                .clickable { mode = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                if (mode == 0) {
                    // Custom Dimension
                    OutlinedTextField(
                        value = dimTitle,
                        onValueChange = { dimTitle = it },
                        label = { Text("Favorite Name (e.g. My Custom Banner)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = favFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dimWidth,
                            onValueChange = { dimWidth = it.filter { c -> c.isDigit() } },
                            label = { Text("Width") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                        OutlinedTextField(
                            value = dimHeight,
                            onValueChange = { dimHeight = it.filter { c -> c.isDigit() } },
                            label = { Text("Height") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dimDpi,
                            onValueChange = { dimDpi = it.filter { c -> c.isDigit() } },
                            label = { Text("DPI (e.g. 300)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                        OutlinedTextField(
                            value = dimUnit,
                            onValueChange = { dimUnit = it.uppercase() },
                            label = { Text("Unit (PX/MM/IN)") },
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                    }
                } else if (mode == 1) {
                    // Custom Compression Target
                    OutlinedTextField(
                        value = compTitle,
                        onValueChange = { compTitle = it },
                        label = { Text("Target Name (e.g. Exam Portal Limit)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = favFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = compTargetKb,
                            onValueChange = { compTargetKb = it.filter { c -> c.isDigit() } },
                            label = { Text("Max Size (KB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                        OutlinedTextField(
                            value = compQuality,
                            onValueChange = { compQuality = it.filter { c -> c.isDigit() } },
                            label = { Text("Quality % (1-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = favFieldColors()
                        )
                    }
                    OutlinedTextField(
                        value = compFormat,
                        onValueChange = { compFormat = it.uppercase() },
                        label = { Text("Format (JPEG, PNG, WEBP)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = favFieldColors()
                    )
                } else {
                    // Pick photo
                    Text(
                        text = "Pick an image from your device gallery to add directly to your Starred Favorites collection for fast 1-tap workspace loading.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                    Button(
                        onClick = onPickImage,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image from Gallery", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (mode != 2) {
                Button(
                    onClick = {
                        if (mode == 0) {
                            val w = dimWidth.toIntOrNull() ?: 1080
                            val h = dimHeight.toIntOrNull() ?: 1080
                            val dpi = dimDpi.toIntOrNull() ?: 300
                            onAddDimension(dimTitle, w, h, dimUnit, dpi)
                        } else if (mode == 1) {
                            val kb = compTargetKb.toIntOrNull() ?: 100
                            val q = compQuality.toIntOrNull() ?: 90
                            onAddCompression(compTitle, kb, q, compFormat)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Save to Favorites", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun favFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF38BDF8),
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0F172A)
)
