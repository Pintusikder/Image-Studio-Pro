package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.OutputFileConfiguration
import com.example.model.NamingMode
import com.example.model.OutputDestinationType
import com.example.model.OverwriteConflictAction
import com.example.processing.ExportEngine
import com.example.processing.OutputFileManager
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

/**
 * Top Controls Bar providing the 6 required controls:
 * Undo, Redo, Reset, Before/After, Preview, Save As.
 */
@Composable
fun EditorControlsBar(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    var showRenameActiveDialog by remember { mutableStateOf(false) }
    var showDeleteActiveDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = palette.cardElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, palette.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Undo
            val canUndo = state.globalUndoStack.isNotEmpty()
            EditorControlActionItem(
                icon = Icons.Default.Undo,
                label = if (canUndo) "Undo (${state.globalUndoStack.size})" else "Undo",
                contentDescription = "Undo",
                enabled = canUndo,
                iconTint = if (canUndo) palette.accentPrimary else palette.textMuted,
                textColor = if (canUndo) palette.textPrimary else palette.textMuted,
                testTag = "control_undo_button",
                onClick = { viewModel.undo() }
            )

            // 2. Redo
            val canRedo = state.globalRedoStack.isNotEmpty()
            EditorControlActionItem(
                icon = Icons.Default.Redo,
                label = if (canRedo) "Redo (${state.globalRedoStack.size})" else "Redo",
                contentDescription = "Redo",
                enabled = canRedo,
                iconTint = if (canRedo) palette.accentPrimary else palette.textMuted,
                textColor = if (canRedo) palette.textPrimary else palette.textMuted,
                testTag = "control_redo_button",
                onClick = { viewModel.redo() }
            )

            // 3. Reset
            EditorControlActionItem(
                icon = Icons.Default.RestartAlt,
                label = "Reset",
                contentDescription = "Reset to Original",
                enabled = true,
                iconTint = palette.error,
                textColor = palette.error,
                testTag = "control_reset_button",
                onClick = { viewModel.resetToOriginal() }
            )

            // 4. Before / After Comparison Toggle
            val isComparing = state.isBeforeAfterActive
            EditorControlActionItem(
                icon = Icons.Default.Compare,
                label = if (isComparing) "Before" else "Before/After",
                contentDescription = "Before/After",
                enabled = true,
                iconTint = if (isComparing) palette.warning else palette.accentPrimary,
                textColor = if (isComparing) palette.warning else palette.textSecondary,
                isBold = isComparing,
                testTag = "control_before_after_button",
                onClick = { viewModel.toggleBeforeAfter() }
            )

            // 5. Full-Screen Preview
            EditorControlActionItem(
                icon = Icons.Default.Visibility,
                label = "Preview",
                contentDescription = "Preview",
                enabled = true,
                iconTint = palette.accentSecondary,
                textColor = palette.textPrimary,
                testTag = "control_preview_button",
                onClick = { viewModel.setFullScreenPreview(true) }
            )

            // 6. Rename Active File
            EditorControlActionItem(
                icon = Icons.Default.AutoFixHigh,
                label = "Rename",
                contentDescription = "Rename File",
                enabled = true,
                iconTint = palette.accentPrimary,
                textColor = palette.textPrimary,
                testTag = "control_rename_button",
                onClick = { showRenameActiveDialog = true }
            )

            // 7. Delete / Clear File
            EditorControlActionItem(
                icon = Icons.Default.Close,
                label = "Delete",
                contentDescription = "Delete File",
                enabled = true,
                iconTint = palette.error,
                textColor = palette.error,
                testTag = "control_delete_button",
                onClick = { showDeleteActiveDialog = true }
            )

            // 8. Save As
            Button(
                onClick = { viewModel.setShowSaveAsDialog(true) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.success),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("control_save_as_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SaveAs,
                    contentDescription = "Save As",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Save As",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            if (showRenameActiveDialog) {
                val initialBase = state.saveAsFileName.ifBlank {
                    state.originalFileName.substringBeforeLast('.').ifBlank { "image_export" }
                }
                var renameInput by remember { mutableStateOf(initialBase) }
                val san = remember(renameInput) { OutputFileManager.sanitizeFilename(renameInput) }
                AlertDialog(
                    onDismissRequest = { showRenameActiveDialog = false },
                    containerColor = Color(0xFF1E293B),
                    modifier = Modifier.testTag("editor_rename_file_dialog"),
                    title = {
                        Text("Rename Working / Output File", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Set a new safe filename for your active document and any exported file:",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = { renameInput = it },
                                label = { Text("New Filename") },
                                suffix = {
                                    Text(
                                        ".${state.exportFormat.extension}",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_rename_file_input")
                            )
                            Text(
                                text = "Sanitized Output: ${san.sanitizedName}.${state.exportFormat.extension}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.renameCurrentWorkingFile(renameInput) {
                                    showRenameActiveDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("editor_rename_confirm_button")
                        ) {
                            Text("Rename", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showRenameActiveDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                )
            }

            if (showDeleteActiveDialog) {
                val hasExportedFile = state.lastExportResult?.outputFile?.exists() == true || state.lastExportResult?.outputUri != null
                AlertDialog(
                    onDismissRequest = { showDeleteActiveDialog = false },
                    containerColor = Color(0xFF1E293B),
                    modifier = Modifier.testTag("editor_delete_file_dialog"),
                    title = {
                        Text("Delete Exported File / Reset?", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            text = if (hasExportedFile) {
                                "Delete the last exported output file (${state.lastExportResult?.outputFile?.name ?: "exported image"}) from storage and reset active edits?"
                            } else {
                                "Discard current uncommitted workspace edits and revert to original source image?"
                            },
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteCurrentOrLastExportedFile(
                                    deleteExportedFile = hasExportedFile,
                                    clearWorkspace = true
                                ) {
                                    showDeleteActiveDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.testTag("editor_delete_confirm_button")
                        ) {
                            Text(if (hasExportedFile) "Delete File & Reset" else "Discard Edits", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteActiveDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EditorControlActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean,
    iconTint: Color,
    textColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBold: Boolean = false
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            )
        }
    }
}


/**
 * Full-screen zoomable preview modal.
 */
@Composable
fun FullScreenPreviewDialog(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    onDismissRequest: () -> Unit
) {
    val bitmapToDisplay = if (state.isBeforeAfterActive) state.originalBitmap ?: state.workingBitmap else state.workingBitmap
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020617))
        ) {
            // Interactive Pan & Zoom Image Canvas
            if (bitmapToDisplay != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmapToDisplay.asImageBitmap(),
                        contentDescription = "Preview Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }

            // Top Overlay Bar: Info & Dismiss
            Surface(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val label = if (state.isBeforeAfterActive) "ORIGINAL (BEFORE)" else "EDITED (AFTER)"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (state.isBeforeAfterActive) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                            )
                        )
                        val w = bitmapToDisplay?.width ?: 0
                        val h = bitmapToDisplay?.height ?: 0
                        Text(
                            text = "$w × $h px • @ ${state.dpi} DPI • Zoom: ${(scale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Before / After toggle
                        IconButton(
                            onClick = { viewModel.toggleBeforeAfter() },
                            modifier = Modifier.testTag("preview_toggle_before_after")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Toggle Before/After",
                                tint = if (state.isBeforeAfterActive) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag("preview_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Actions: Quick Save / Share
            Surface(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Zoom")
                    }

                    Button(
                        onClick = {
                            viewModel.setShowSaveAsDialog(true)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preview_save_as_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAs,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save As", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Save As modal dialog allowing full customization of:
 * - Output File Management (Custom / Auto / Template Filename, Path Traversal & Invalid Character Prevention)
 * - Save Location (Public MediaStore Gallery, Custom SAF Folder via OpenDocumentTree, System Save As via CreateDocument)
 * - Overwrite Conflict Protection (Ask before overwrite, Auto-Rename, Cancel, Force Replace)
 * - Output format (JPEG, PNG, WEBP Lossy, WEBP Lossless, PDF)
 * - Quality slider & DPI embedding
 * - Metadata privacy scrubbing (Keep All, Strip GPS, Strip All)
 * - Instant Share integration
 */
@Composable
fun SaveAsDialog(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    onDismissRequest: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val outputConfig = state.outputFileConfig

    // SAF Directory Picker (OpenDocumentTree)
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if persistable permission fails
            }
            viewModel.setSafTreeUri(uri)
        }
    }

    // SAF Save As Document Picker (CreateDocument)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(state.saveAsFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.executeSaveDirectlyToDocumentUri(uri) { result ->
                if (result.success) {
                    onSaveSuccess("Saved successfully to chosen file location!")
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SaveAs,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save & Export Manager",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Filename Management & Naming Modes
                Text(
                    text = "1. FILENAME GENERATION & SECURITY",
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
                    NamingMode.values().forEach { mode ->
                        val isSelected = outputConfig.namingMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setNamingMode(mode) },
                            label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("naming_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (outputConfig.namingMode) {
                    NamingMode.CUSTOM_EXACT -> {
                        OutlinedTextField(
                            value = outputConfig.customExactName,
                            onValueChange = { viewModel.setSaveAsFileName(it) },
                            label = { Text("Exact Filename", color = Color(0xFF94A3B8)) },
                            suffix = {
                                Text(
                                    text = ".${state.saveAsFormat.extension}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (state.filenameSanitizationResult.isValid) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_as_file_name_input")
                        )
                    }
                    NamingMode.PATTERN_TEMPLATE -> {
                        OutlinedTextField(
                            value = outputConfig.patternTemplate,
                            onValueChange = { newPattern ->
                                viewModel.updateOutputFileConfig { it.copy(customTemplate = newPattern) }
                            },
                            label = { Text("Pattern Template (e.g. {name}_{date}_{w}x{h})", color = Color(0xFF94A3B8)) },
                            suffix = {
                                Text(
                                    text = ".${state.saveAsFormat.extension}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Quick Tokens:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tokens = listOf("{name}", "{date}", "{time}", "{w}", "{h}", "{dpi}", "{quality}", "{counter}")
                            tokens.forEach { token ->
                                Surface(
                                    onClick = {
                                        viewModel.updateOutputFileConfig {
                                            it.copy(customTemplate = "${it.customTemplate}_$token")
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF475569)),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = token,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // AUTOMATIC or PRESET
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-naming format: ${outputConfig.namingMode.displayName}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                                )
                            }
                        }
                    }
                }

                // Live Filename Preview & Sanitization Indicator
                val resolvedName = OutputFileManager.generateFormattedFilename(
                    config = outputConfig,
                    originalName = state.originalFileName.ifBlank { "image" },
                    width = state.targetWidthPx,
                    height = state.targetHeightPx,
                    dpi = state.saveAsDpi,
                    format = state.saveAsFormat
                )

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF020617),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.filenameSanitizationResult.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (state.filenameSanitizationResult.isValid) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Output Name:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$resolvedName.${state.saveAsFormat.extension}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (!state.filenameSanitizationResult.isValid) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Note: ${state.filenameSanitizationResult.warningMessage}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Storage Location & Android Storage Access Framework (SAF)
                Text(
                    text = "2. SAVE LOCATION & STORAGE ACCESS (SAF)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option A: MediaStore / Gallery
                    Surface(
                        onClick = { viewModel.setOutputDestinationType(OutputDestinationType.PUBLIC_MEDIASTORE) },
                        color = if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color(0xFF10B981) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("destination_gallery")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color(0xFF10B981) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Media Gallery",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (outputConfig.destinationType == OutputDestinationType.PUBLIC_MEDIASTORE) Color.White else Color(0xFF94A3B8)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pictures/ImageStudio",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                            )
                        }
                    }

                    // Option B: User-Selected SAF Directory
                    Surface(
                        onClick = {
                            openDocumentTreeLauncher.launch(null)
                        },
                        color = if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color(0xFF10B981) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("destination_saf_folder")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color(0xFF10B981) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Choose Folder",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) Color.White else Color(0xFF94A3B8)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = outputConfig.safFolderDisplayName ?: "Select with SAF",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (outputConfig.safTreeUri != null) Color(0xFFA7F3D0) else Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button for Native Android "Save As" (CreateDocument SAF flow)
                OutlinedButton(
                    onClick = {
                        val fileNameWithExt = "$resolvedName.${state.saveAsFormat.extension}"
                        createDocumentLauncher.launch(fileNameWithExt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_saf_save_as"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7))
                ) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "System 'Save As...' (Pick Specific Location)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Overwrite Protection & Conflict Strategy
                Text(
                    text = "3. OVERWRITE PROTECTION & CONFLICT STRATEGY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OverwriteConflictAction.values().forEach { action ->
                        val selected = outputConfig.overwriteConflictAction == action
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setOverwriteConflictAction(action) },
                            label = { Text(action.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("overwrite_action_${action.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: Target Format Selector
                Text(
                    text = "4. TARGET FORMAT",
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
                    val formats = listOf(
                        ExportFormat.JPEG,
                        ExportFormat.PNG,
                        ExportFormat.WEBP_LOSSY,
                        ExportFormat.WEBP_LOSSLESS,
                        ExportFormat.PDF
                    )
                    formats.forEach { fmt ->
                        val selected = state.saveAsFormat == fmt
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSaveAsFormat(fmt) },
                            label = { Text(fmt.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("save_as_format_${fmt.extension.lowercase()}")
                        )
                    }
                }

                // Quality Slider (for Lossy formats)
                if (state.saveAsFormat == ExportFormat.JPEG || state.saveAsFormat == ExportFormat.WEBP_LOSSY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPRESSION QUALITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        )
                        Text(
                            text = "${state.saveAsQuality}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                    Slider(
                        value = state.saveAsQuality.toFloat(),
                        onValueChange = { viewModel.setSaveAsQuality(it.toInt()) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("save_as_quality_slider")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target DPI Embedding
                Text(
                    text = "TARGET PRINT DPI",
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
                    val dpis = listOf(72, 96, 150, 300, 600)
                    dpis.forEach { d ->
                        val selected = state.saveAsDpi == d
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSaveAsDpi(d) },
                            label = { Text("$d DPI", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("save_as_dpi_$d")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Privacy Options
                Text(
                    text = "METADATA PRIVACY SCRUBBING",
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
                    val policies = listOf(
                        Pair(MetadataPolicy.KEEP_ALL, "Keep All EXIF"),
                        Pair(MetadataPolicy.CUSTOM_SELECTIVE, "Scrub GPS & Sensitive"),
                        Pair(MetadataPolicy.STRIP_ALL, "Strip All Metadata")
                    )
                    policies.forEach { (pol, label) ->
                        val selected = state.saveAsPrivacyMode == pol
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSaveAsPrivacyMode(pol) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("save_as_privacy_${pol.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Card
                val w = state.workingBitmap?.width ?: 0
                val h = state.workingBitmap?.height ?: 0
                val estKb = state.estimatedBytes / 1024.0
                val estStr = if (estKb > 1024) String.format("%.2f MB", estKb / 1024.0) else String.format("%.1f KB", estKb)
                Surface(
                    color = Color(0xFF020617),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Export Summary",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "File Name: $resolvedName.${state.saveAsFormat.extension}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Resolution: $w × $h px (${String.format("%.1f", (w.toFloat() * h.toFloat()) / 1_000_000f)} MP)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                        Text(
                            text = "Physical Size: ${String.format("%.2f", w.toFloat() / state.saveAsDpi)} × ${String.format("%.2f", h.toFloat() / state.saveAsDpi)} in @ ${state.saveAsDpi} DPI",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8))
                        )
                        Text(
                            text = "Estimated File Size: ~$estStr",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981))
                        )
                        Text(
                            text = "Destination: ${if (outputConfig.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY) (outputConfig.safFolderDisplayName ?: "SAF Folder") else "Public Gallery"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF34D399))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons: Cancel, Share, and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    // Direct Share Button
                    OutlinedButton(
                        onClick = {
                            viewModel.executeSaveAs { result ->
                                if (result.success && result.outputFile != null) {
                                    viewModel.shareCurrentResult(context)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_as_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    // Save / Export Button
                    Button(
                        onClick = {
                            viewModel.executeSaveAs { result ->
                                if (result.success && result.outputFile != null) {
                                    val msg = if (result.isOverwritten) {
                                        "Original photo overwritten successfully!"
                                    } else {
                                        "Saved copy to: ${result.outputFile.name} (${result.verifiedFileWidth}×${result.verifiedFileHeight} px)"
                                    }
                                    onSaveSuccess(msg)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_as_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isExporting) "Saving..." else "Save Image",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }

        // File Conflict Alert Dialog (Ask before overwrite)
        if (state.showFileConflictDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowFileConflictDialog(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "File Already Exists",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "A file named '${state.pendingConflictFileName}' already exists in the selected destination folder.\n\nHow would you like to proceed?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setShowFileConflictDialog(false)
                            viewModel.executeSaveAs(forceOverwrite = true) { result ->
                                if (result.success) onSaveSuccess("File replaced successfully!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Replace File")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setShowFileConflictDialog(false)
                            }
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.setShowFileConflictDialog(false)
                                viewModel.updateOutputFileConfig {
                                    it.copy(
                                        customExactName = OutputFileManager.generateUniqueFilename(it.customExactName)
                                    )
                                }
                                viewModel.executeSaveAs { result ->
                                    if (result.success) onSaveSuccess("Saved as unique copy!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Keep Both (Auto-Rename)")
                        }
                    }
                }
            )
        }

        // Overwrite Original Confirm Dialog
        if (state.showOverwriteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowOverwriteConfirmDialog(false) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Overwrite Original File?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "This will permanently replace the original photo on your device with your edited version.\n\nNon-destructive editing is recommended: saving as a new copy allows you to keep your original master photo unharmed.\n\nDo you want to explicitly overwrite the original file?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setSaveAsOverwriteOriginal(true)
                            viewModel.setShowOverwriteConfirmDialog(false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Yes, Overwrite Original")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.setSaveAsOverwriteOriginal(false)
                            viewModel.setShowOverwriteConfirmDialog(false)
                        }
                    ) {
                        Text("Keep Original Untouched")
                    }
                }
            )
        }
    }
}
