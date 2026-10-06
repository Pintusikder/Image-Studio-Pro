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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import com.example.data.local.FavoriteType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.HistoryEntity
import com.example.model.PassportPreset
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.UtilityViewModel

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: UtilityViewModel,
    onNavigateToEditor: (StudioTab) -> Unit,
    onNavigateToSignature: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()

    val historyItems by viewModel.allHistory.collectAsStateWithLifecycle()
    val allFavorites by viewModel.allFavoriteItems.collectAsStateWithLifecycle()

    var pendingTab by remember { mutableStateOf(StudioTab.CROP) }
    var showMoreTools by remember { mutableStateOf(true) }

    var currentCameraCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraTab by remember { mutableStateOf(StudioTab.CROP) }
    var showSourceDialogForTab by remember { mutableStateOf<StudioTab?>(null) }
    var sourceDialogTitle by remember { mutableStateOf("Select Image Source") }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadSourceImage(uri)
            onNavigateToEditor(pendingTab)
        }
    }

    val safDocumentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadSourceImage(uri)
            onNavigateToEditor(pendingTab)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentCameraCaptureFile != null && currentCameraCaptureFile!!.exists()) {
            val uri = Uri.fromFile(currentCameraCaptureFile)
            viewModel.loadSourceImage(uri)
            if (pendingCameraTab == StudioTab.DOCUMENT) {
                viewModel.setPassportDocumentMode(com.example.processing.PassportIdDocumentEngine.DocumentProcessMode.MAGIC_COLOR_CLEAN)
                viewModel.setEnhancements(filter = com.example.model.FilterPreset.DOCUMENT_MAGIC)
            }
            onNavigateToEditor(pendingCameraTab)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val file = File.createTempFile("studio_cam_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
                currentCameraCaptureFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to capture photos and scan documents directly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val launchCameraTool = { tab: StudioTab ->
        pendingCameraTab = tab
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val file = File.createTempFile("studio_cam_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
                currentCameraCaptureFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchTool = { tab: StudioTab ->
        pendingTab = tab
        singlePhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val launchSafTool = { tab: StudioTab ->
        pendingTab = tab
        safDocumentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(palette.accentPrimary, palette.accentSecondary)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = "Studio Icon",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Image Studio",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            )
                            Text(
                                text = "Professional Image & Document Suite",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = palette.textSecondary
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToFavorites,
                        modifier = Modifier.testTag("home_favorites_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorites Hub",
                            tint = palette.warning
                        )
                    }
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("home_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = palette.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToPresets,
                        modifier = Modifier.testTag("home_presets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Presets",
                            tint = palette.textPrimary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = palette.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.topBarBackground
                )
            )
        },
        containerColor = palette.appBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = adaptive.horizontalPadding,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Privacy Guarantee Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = palette.cardSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF065F46), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "100% Offline • No Account Required",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Crop, resize, compress, DPI, batch, enhancement, sheets, & presets run entirely on-device without internet or login.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Primary Call-To-Action: Open Image for Studio (Photo Picker, Camera & SAF)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            launchTool(StudioTab.CROP)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Open Photo (Photo Picker)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }

                    Button(
                        onClick = {
                            launchCameraTool(StudioTab.CROP)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_camera_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo with Camera",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Camera Capture & Scan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            launchSafTool(StudioTab.CROP)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_saf_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = Color(0xFF38BDF8)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Open from Files / Storage (SAF)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        )
                    }
                }
            }

            // ==========================================
            // ⭐ FAVORITES & QUICK SHORTCUTS (Request 38)
            // ==========================================
            if (allFavorites.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FAVORITES & SHORTCUTS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B),
                                        letterSpacing = 1.1.sp
                                    )
                                )
                            }
                            TextButton(
                                onClick = onNavigateToFavorites,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "View Hub (${allFavorites.size}) →",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scrollable Favorites Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allFavorites.take(8), key = { "home_fav_${it.id}" }) { fav ->
                                val chipColor = when (fav.type) {
                                    FavoriteType.IMAGE -> Color(0xFF34D399)
                                    FavoriteType.PRESET -> Color(0xFF818CF8)
                                    FavoriteType.RECENT_TOOL -> Color(0xFF38BDF8)
                                    FavoriteType.CUSTOM_DIMENSION -> Color(0xFFA855F7)
                                    FavoriteType.COMPRESSION_TARGET -> Color(0xFFF59E0B)
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, chipColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.applyFavoriteItem(fav) { targetTab ->
                                                if (fav.type == FavoriteType.RECENT_TOOL) {
                                                    launchTool(targetTab)
                                                } else {
                                                    onNavigateToEditor(targetTab)
                                                }
                                            }
                                        }
                                        .testTag("home_fav_chip_${fav.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (fav.type) {
                                                FavoriteType.IMAGE -> Icons.Default.Collections
                                                FavoriteType.PRESET -> Icons.Default.GridOn
                                                FavoriteType.RECENT_TOOL -> Icons.Default.Tune
                                                FavoriteType.CUSTOM_DIMENSION -> Icons.Default.Straighten
                                                FavoriteType.COMPRESSION_TARGET -> Icons.Default.Compress
                                            },
                                            contentDescription = null,
                                            tint = chipColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = fav.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 1
                                            )
                                            if (fav.subtitle.isNotBlank()) {
                                                Text(
                                                    text = fav.subtitle,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.sp
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // MAIN TOOLS (Primary Core Capabilities)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAIN TOOLS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }

            // Grid Row 1: Crop Photo & Aspect Ratio Tool
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Crop Photo",
                        subtitle = "Free, 1:1, 4:5, 16:9, passport",
                        icon = Icons.Default.Crop,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_crop_photo",
                        onClick = { launchTool(StudioTab.CROP) }
                    )
                    ToolModuleCard(
                        title = "Aspect Ratio",
                        subtitle = "Lock ratio, presets & calculator",
                        icon = Icons.Default.AspectRatio,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_aspect_ratio",
                        onClick = { launchTool(StudioTab.ASPECT_RATIO) }
                    )
                }
            }

            // Grid Row 2: Resize Photo & Compress Photo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Resize Photo",
                        subtitle = "Scale % or target W × H px",
                        icon = Icons.Default.Straighten,
                        accentColor = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_resize_photo",
                        onClick = { launchTool(StudioTab.PIXEL_SIZE) }
                    )
                    ToolModuleCard(
                        title = "Compress Photo",
                        subtitle = "Quality slider with live preview",
                        icon = Icons.Default.Compress,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_compress_photo",
                        onClick = { launchTool(StudioTab.FILE_SIZE) }
                    )
                }
            }

            // Grid Row 3: Exact Size & Exact File Size
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Exact Size",
                        subtitle = "A: 300x100 px, exact raster grid",
                        icon = Icons.Default.Straighten,
                        accentColor = Color(0xFF34D399),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_exact_size",
                        onClick = { launchTool(StudioTab.PIXEL_SIZE) }
                    )
                    ToolModuleCard(
                        title = "Exact File Size",
                        subtitle = "Target ≤20KB, 50KB, 100KB portal limits",
                        icon = Icons.Default.Tune,
                        accentColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_exact_file_size",
                        onClick = { launchTool(StudioTab.FILE_SIZE) }
                    )
                }
            }

            // Grid Row 4: Passport Photo & ID Photo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Passport Photo",
                        subtitle = "US 2x2 in, UK/EU 35x45mm, India",
                        icon = Icons.Default.Badge,
                        accentColor = Color(0xFF6366F1),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_passport_photo",
                        onClick = {
                            viewModel.setIdCategory(com.example.model.IdDocumentCategory.ALL)
                            viewModel.selectPassportPreset(PassportPreset.PRESETS.first())
                            sourceDialogTitle = "Passport Photo Source"
                            showSourceDialogForTab = StudioTab.PASSPORT
                        }
                    )
                    ToolModuleCard(
                        title = "ID Photo",
                        subtitle = "ID, Visa, Exam & Portal formats",
                        icon = Icons.Default.Badge,
                        accentColor = Color(0xFF818CF8),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_id_photo",
                        onClick = {
                            viewModel.setIdCategory(com.example.model.IdDocumentCategory.ID_PHOTO)
                            viewModel.selectPassportPreset(
                                PassportPreset.PRESETS.find { it.category == com.example.model.IdDocumentCategory.ID_PHOTO }
                                    ?: PassportPreset.PRESETS[1]
                            )
                            sourceDialogTitle = "ID & Visa Photo Source"
                            showSourceDialogForTab = StudioTab.PASSPORT
                        }
                    )
                }
            }

            // Grid Row 5: Signature & Document Photo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Signature",
                        subtitle = "Camera, crop, transparent, ≤20KB",
                        icon = Icons.Default.Draw,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_signature",
                        onClick = onNavigateToSignature
                    )
                    ToolModuleCard(
                        title = "Perspective Fix",
                        subtitle = "4-corner deskew docs & receipts",
                        icon = Icons.Default.CropFree,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_perspective",
                        onClick = { launchTool(StudioTab.PERSPECTIVE) }
                    )
                }
            }

            // Grid Row 6: Convert Format & DPI / Resolution
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Convert Format",
                        subtitle = "JPEG, PNG, WebP, PDF export",
                        icon = Icons.Default.Transform,
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_convert_format",
                        onClick = { launchTool(StudioTab.FORMAT) }
                    )
                    ToolModuleCard(
                        title = "DPI / Resolution",
                        subtitle = "Set 72, 150, 300, 600 DPI",
                        icon = Icons.Default.Print,
                        accentColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_dpi_resolution",
                        onClick = { launchTool(StudioTab.PRINT_SIZE) }
                    )
                }
            }
            // Grid Row 7: Rotate / Straighten & Flip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Rotate / Straighten",
                        subtitle = "90°, 180°, angle, horizon guide",
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_rotate_straighten",
                        onClick = { launchTool(StudioTab.ROTATE) }
                    )
                    ToolModuleCard(
                        title = "Flip Image",
                        subtitle = "Horizontal & vertical mirroring",
                        icon = Icons.Default.SwapHoriz,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_flip",
                        onClick = { launchTool(StudioTab.FLIP) }
                    )
                }
            }

            // Grid Row 8: Batch Edit & Enhance
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Batch Edit",
                        subtitle = "Bulk resize, convert & compress",
                        icon = Icons.Default.PhotoLibrary,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_batch_edit",
                        onClick = onNavigateToBatch
                    )
                    ToolModuleCard(
                        title = "Enhance",
                        subtitle = "Brightness, contrast, filters",
                        icon = Icons.Default.AutoFixHigh,
                        accentColor = Color(0xFFEAB308),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_enhance",
                        onClick = { launchTool(StudioTab.ENHANCE) }
                    )
                }
            }

            // Grid Row 9: Background Tools & Image Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToolModuleCard(
                        title = "Background",
                        subtitle = "Remove, transparent, white & clean",
                        icon = Icons.Default.Palette,
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_background",
                        onClick = { launchTool(StudioTab.BACKGROUND) }
                    )
                    ToolModuleCard(
                        title = "Image Info",
                        subtitle = "EXIF, camera, privacy scrubber",
                        icon = Icons.Default.Info,
                        accentColor = Color(0xFF64748B),
                        modifier = Modifier.weight(1f),
                        testTag = "tool_image_info",
                        onClick = { launchTool(StudioTab.EXIF) }
                    )
                }
            }

            // ==========================================
            // MORE TOOLS (Collapsible / Extended Section)
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreTools = !showMoreTools }
                        .testTag("more_tools_toggle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "More Tools",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Photo sheets, preset manager & quick utilities",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showMoreTools) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showMoreTools) "Collapse" else "Expand",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            if (showMoreTools) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ToolModuleCard(
                                title = "Photo Sheet",
                                subtitle = "Multi-copy on 4x6 or A4 paper",
                                icon = Icons.Default.Print,
                                accentColor = Color(0xFFEC4899),
                                modifier = Modifier.weight(1f),
                                testTag = "tool_photo_sheet",
                                onClick = { launchTool(StudioTab.SHEET) }
                            )
                            ToolModuleCard(
                                title = "Presets Library",
                                subtitle = "Social media & custom rules",
                                icon = Icons.Default.GridOn,
                                accentColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f),
                                testTag = "tool_presets_library",
                                onClick = onNavigateToPresets
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ToolModuleCard(
                                title = "Doc Scanner Mode",
                                subtitle = "High contrast clean for bills/receipts",
                                icon = Icons.Default.Scanner,
                                accentColor = Color(0xFF14B8A6),
                                modifier = Modifier.weight(1f),
                                testTag = "tool_scanner_mode",
                                onClick = {
                                    sourceDialogTitle = "Document Scanner Source"
                                    showSourceDialogForTab = StudioTab.DOCUMENT
                                }
                            )
                            ToolModuleCard(
                                title = "History & Exports",
                                subtitle = "Saved items with metadata & favorites",
                                icon = Icons.Default.History,
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f),
                                testTag = "tool_history_library",
                                onClick = onNavigateToHistory
                            )
                        }
                    }
                }
            }

            // ==========================================
            // RECENT CREATIONS / HISTORY SECTION
            // ==========================================
            if (historyItems.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CREATIONS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable { onNavigateToHistory() }
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItems.take(6)) { item ->
                            RecentItemCard(
                                item = item,
                                onToggleFavorite = { viewModel.toggleFavorite(item) }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // PRIVACY MANIFEST & TRANSPARENCY CARD (Request 40)
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIVACY & SECURITY COMMITMENT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    letterSpacing = 1.1.sp
                                )
                            )
                        }

                        Text(
                            text = "• Photos, Signatures, Documents & Passports are never uploaded to any cloud server.\n" +
                                    "• 100% On-Device Processing via local Android runtime APIs.\n" +
                                    "• Zero telemetry, zero third-party analytics SDKs, and zero tracking pixels.\n" +
                                    "• Full EXIF metadata scrubber available to remove GPS and camera details before export.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }

    if (showSourceDialogForTab != null) {
        val targetTab = showSourceDialogForTab!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSourceDialogForTab = null },
            title = {
                Text(
                    text = sourceDialogTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose how you want to provide the image:",
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary)
                    )

                    Button(
                        onClick = {
                            showSourceDialogForTab = null
                            launchCameraTool(targetTab)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_camera_capture_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo with Camera", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            showSourceDialogForTab = null
                            launchTool(targetTab)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_photo_picker_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery (Photo Picker)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    OutlinedButton(
                        onClick = {
                            showSourceDialogForTab = null
                            launchSafTool(targetTab)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dialog_saf_storage_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = palette.cardSurface,
                            contentColor = palette.textPrimary
                        )
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Files / Storage (SAF)", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialogForTab = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
            containerColor = palette.cardElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ToolModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                role = androidx.compose.ui.semantics.Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = palette.textSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentItemCard(
    item: HistoryEntity,
    onToggleFavorite: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.cardElevated),
                contentAlignment = Alignment.Center
            ) {
                val thumbBmp = item.previewThumbnailBase64?.let {
                    com.example.processing.BitmapUtils.decodeThumbnailFromBase64(it)
                }
                if (thumbBmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbBmp.asImageBitmap(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFF43F5E) else palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${item.width}x${item.height} • ${item.format}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val kb = item.fileSizeBytes / 1024.0
            val sizeStr = if (kb > 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
            Text(
                text = "${item.operationType} • $sizeStr",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = palette.textSecondary
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

