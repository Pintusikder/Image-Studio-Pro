package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.CameraAlt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.ExifPrivacyControls
import com.example.ui.components.ExactSizeControls
import com.example.ui.components.StraightenControls
import com.example.ui.components.EditorControlsBar
import com.example.ui.components.FullScreenPreviewDialog
import com.example.ui.components.SaveAsDialog
import com.example.ui.components.ExportVerificationDialog
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import com.example.model.CutLineStyle
import com.example.model.PhotoSheetRotation
import com.example.model.SheetAlignment
import com.example.model.SheetOrientation
import com.example.processing.BitmapUtils
import com.example.processing.DpiPrintEngine
import com.example.processing.PassportIdDocumentEngine
import com.example.processing.PhotoSheetGenerator
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.model.IdDocumentCategory
import com.example.model.PassportDimensionUnit
import com.example.model.PassportExportTarget
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CompressionPreset
import com.example.model.CropAspectRatio
import com.example.model.CustomCompressionMode
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.FilterPreset
import com.example.model.FormatConversionPair
import com.example.model.PassportPreset
import com.example.model.PrintUnit
import com.example.model.SheetPaperPreset
import com.example.model.StandardPrintPreset
import com.example.processing.BackgroundProcessor
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode
import com.example.processing.FileSizeOptimizationResult
import com.example.processing.FileSizeEngine
import com.example.ui.components.BackgroundControls
import com.example.ui.components.BackgroundPreviewOverlay
import com.example.ui.components.BiometricGuideOverlay
import com.example.ui.components.CropControls
import com.example.ui.components.CropOverlayView
import com.example.ui.components.EnhanceControls
import com.example.ui.components.EnhancePreviewOverlay
import com.example.ui.components.FlipControls
import com.example.ui.components.FlipPreviewOverlay
import com.example.ui.components.ImageInfoControls
import com.example.ui.components.ImageInfoDialog
import com.example.ui.components.ImageStatsBar
import com.example.ui.components.AspectRatioControls
import com.example.ui.components.AspectRatioPreviewOverlay
import com.example.ui.components.PerspectiveControls
import com.example.ui.components.PerspectivePreviewOverlay
import com.example.ui.components.RotateStraightenControls
import com.example.ui.components.RotateStraightenPreviewOverlay
import com.example.processing.AspectRatioApplyMode
import com.example.processing.AspectRatioEngine
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.UtilityViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: UtilityViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()

    val workingBitmap = state.workingBitmap
    val previewBitmap = state.previewBitmap ?: workingBitmap

    var currentCameraCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraTargetTab by remember { mutableStateOf<StudioTab?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentCameraCaptureFile != null && currentCameraCaptureFile!!.exists()) {
            val uri = Uri.fromFile(currentCameraCaptureFile)
            viewModel.loadSourceImage(uri)
            if (pendingCameraTargetTab == StudioTab.DOCUMENT) {
                viewModel.setPassportDocumentMode(PassportIdDocumentEngine.DocumentProcessMode.MAGIC_COLOR_CLEAN)
                viewModel.setEnhancements(filter = FilterPreset.DOCUMENT_MAGIC)
            }
            Toast.makeText(context, "Captured photo loaded into Studio", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val file = File.createTempFile("studio_edit_cam_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
                currentCameraCaptureFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to capture photos and scan documents.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val launchCameraCapture = { targetTab: StudioTab? ->
        pendingCameraTargetTab = targetTab
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val file = File.createTempFile("studio_edit_cam_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.activeTab.label,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        )
                        if (workingBitmap != null) {
                            Text(
                                text = "${workingBitmap.width}x${workingBitmap.height} px",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { launchCameraCapture(state.activeTab) },
                        modifier = Modifier.testTag("top_camera_capture_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture with Camera",
                            tint = Color(0xFF10B981)
                        )
                    }

                    if (state.lastVerificationReport != null) {
                        IconButton(
                            onClick = { viewModel.openVerificationDialog() },
                            modifier = Modifier.testTag("top_verify_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verification Audit Report",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setShowImageInfoDialog(true) },
                        modifier = Modifier.testTag("top_image_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Image Information",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.resetToOriginal() },
                        modifier = Modifier.testTag("reset_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset to Original",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            viewModel.exportCurrent { result ->
                                if (result.success && result.outputFile != null) {
                                    ExportEngine.shareImage(
                                        context,
                                        result.outputFile,
                                        result.format.mimeType
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("editor_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // Save / Export
                    Button(
                        onClick = {
                            viewModel.exportCurrent { result ->
                                scope.launch {
                                    if (result.success) {
                                        val kb = result.fileSizeBytes / 1024.0
                                        val sizeStr = if (kb > 1024) String.format("%.2f MB", kb / 1024.0) else String.format("%.2f KB", kb)
                                        val targetKb = state.targetSizeKb
                                        val exportMsg = when {
                                            result.isExactMatch -> "Saved: Exact ${sizeStr} Verified (${result.verifiedFileWidth} × ${result.verifiedFileHeight} px)"
                                            targetKb != null && state.fileSizeMode == FileSizeMode.MAXIMUM_CEILING ->
                                                if (result.isCompliantWithCeiling) "Saved: ${result.verifiedFileWidth} × ${result.verifiedFileHeight} px • ${sizeStr} (≤ $targetKb KB)"
                                                else "Saved: ${result.verifiedFileWidth} × ${result.verifiedFileHeight} px • ${sizeStr} (Over ceiling)"
                                            targetKb != null -> "Saved: ${result.verifiedFileWidth} × ${result.verifiedFileHeight} px • ${sizeStr}"
                                            else -> "Saved & Verified: ${result.verifiedFileWidth} × ${result.verifiedFileHeight} px ($sizeStr)"
                                        }
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = exportMsg,
                                            actionLabel = if (result.verificationReport != null) "View Audit" else null,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            viewModel.openVerificationDialog(result.verificationReport)
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar(result.errorMessage ?: "Export error")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.topBarBackground)
            )
        },
        containerColor = palette.appBackground
    ) { innerPadding ->
        if (adaptive.isWideScreen) {
            // ==========================================
            // TABLET / FOLDABLE / LANDSCAPE: 2-PANE LAYOUT
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left Pane: EditorControlsBar, Canvas, and Stats Bar
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    EditorControlsBar(
                        viewModel = viewModel,
                        state = state
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(palette.canvasBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        EditorCanvasOverlay(viewModel, state, workingBitmap, previewBitmap)
                    }

                    if (workingBitmap != null) {
                        ImageStatsBar(
                            width = state.targetWidthPx,
                            height = state.targetHeightPx,
                            fileSizeBytes = state.estimatedBytes,
                            dpi = state.dpi,
                            format = state.exportFormat,
                            targetSizeKb = state.targetSizeKb,
                            fileSizeMode = state.fileSizeMode,
                            fileSizeResult = state.fileSizeOptimizationResult,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Right Pane: Studio Tabs and Tool Drawer
                Surface(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight(),
                    color = palette.cardSurface,
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.cardElevated)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StudioTab.values().forEach { tab ->
                                val isSelected = state.activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) palette.accentPrimary else palette.cardSurface)
                                        .border(1.dp, if (isSelected) palette.accentPrimary else palette.cardBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.selectTab(tab) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("tab_${tab.name.lowercase()}")
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) palette.accentOnPrimary else palette.textSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            EditorToolDrawerContent(viewModel, state)
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // COMPACT PORTRAIT: STACKED ADAPTIVE LAYOUT
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EditorControlsBar(
                    viewModel = viewModel,
                    state = state
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(palette.canvasBackground),
                    contentAlignment = Alignment.Center
                ) {
                    EditorCanvasOverlay(viewModel, state, workingBitmap, previewBitmap)
                }

                if (workingBitmap != null) {
                    ImageStatsBar(
                        width = state.targetWidthPx,
                        height = state.targetHeightPx,
                        fileSizeBytes = state.estimatedBytes,
                        dpi = state.dpi,
                        format = state.exportFormat,
                        targetSizeKb = state.targetSizeKb,
                        fileSizeMode = state.fileSizeMode,
                        fileSizeResult = state.fileSizeOptimizationResult,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardElevated)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudioTab.values().forEach { tab ->
                        val isSelected = state.activeTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) palette.accentPrimary else palette.cardSurface)
                                .border(1.dp, if (isSelected) palette.accentPrimary else palette.cardBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectTab(tab) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) palette.accentOnPrimary else palette.textSecondary
                                )
                            )
                        }
                    }
                }

                val drawerHeight = if (adaptive.isCompactHeight) 220.dp else if (adaptive.isLargeFontScale) 340.dp else 300.dp
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(drawerHeight),
                    color = palette.cardSurface,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                    border = BorderStroke(1.dp, palette.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        EditorToolDrawerContent(
                            viewModel = viewModel,
                            state = state,
                            onCaptureCamera = { tab -> launchCameraCapture(tab) }
                        )
                    }
                }
            }
        }
    }

    if (state.showImageInfoDialog) {
        ImageInfoDialog(
            viewModel = viewModel,
            state = state,
            onDismissRequest = { viewModel.setShowImageInfoDialog(false) }
        )
    }

    if (state.isFullScreenPreviewOpen && workingBitmap != null) {
        FullScreenPreviewDialog(
            viewModel = viewModel,
            state = state,
            onDismissRequest = { viewModel.setFullScreenPreview(false) }
        )
    }

    if (state.showSaveAsDialog) {
        SaveAsDialog(
            viewModel = viewModel,
            state = state,
            onDismissRequest = { viewModel.setShowSaveAsDialog(false) },
            onSaveSuccess = { msg ->
                viewModel.setShowSaveAsDialog(false)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = if (state.lastVerificationReport != null) "View Audit" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.openVerificationDialog()
                    }
                }
            }
        )
    }

    val verificationReport = state.lastVerificationReport
    val exportResult = state.lastExportResult
    if (state.showVerificationDialog && verificationReport != null && exportResult != null) {
        ExportVerificationDialog(
            report = verificationReport,
            exportResult = exportResult,
            onDismiss = { viewModel.setShowVerificationDialog(false) },
            onShare = {
                val file = exportResult.outputFile
                if (file != null) {
                    ExportEngine.shareImage(context, file, exportResult.format.mimeType)
                }
            }
        )
    }
}

@Composable
private fun EditorToolDrawerContent(
    viewModel: UtilityViewModel,
    state: com.example.ui.viewmodel.StudioUiState,
    onCaptureCamera: (StudioTab?) -> Unit = {}
) {
    if (state.activeTab == StudioTab.PIXEL_SIZE || state.activeTab == StudioTab.PRINT_SIZE || state.activeTab == StudioTab.FILE_SIZE) {
        ExactSizeConceptExplainer(viewModel, state)
        Spacer(modifier = Modifier.height(14.dp))
    }

    when (state.activeTab) {
        StudioTab.CROP -> CropControls(viewModel, state)
        StudioTab.RESIZE -> PixelSizeControls(viewModel, state)
        StudioTab.EXACT_SIZE -> ExactSizeControls(viewModel, state)
        StudioTab.COMPRESS -> FileSizeControls(viewModel, state)
        StudioTab.CONVERT -> FormatControls(viewModel, state)
        StudioTab.ROTATE -> RotateControls(viewModel, state)
        StudioTab.FLIP -> FlipControls(viewModel, state)
        StudioTab.STRAIGHTEN -> StraightenControls(viewModel, state)
        StudioTab.PERSPECTIVE -> PerspectiveControls(viewModel, state)
        StudioTab.DPI -> PrintSizeControls(viewModel, state)
        StudioTab.METADATA -> ExifControls(viewModel, state)
        StudioTab.ENHANCE -> EnhanceControls(viewModel, state)
        StudioTab.BACKGROUND -> BackgroundControls(viewModel, state)
        StudioTab.ASPECT_RATIO -> AspectRatioControls(viewModel, state)
        StudioTab.PASSPORT -> PassportControls(viewModel, state, onCaptureCamera = { onCaptureCamera(StudioTab.PASSPORT) })
        StudioTab.DOCUMENT -> DocumentControls(viewModel, state, onCaptureCamera = { onCaptureCamera(StudioTab.DOCUMENT) })
        StudioTab.SHEET -> PhotoSheetControls(viewModel, state)
        StudioTab.INFO -> ImageInfoControls(viewModel, state)
    }
}

@Composable
private fun EditorCanvasOverlay(
    viewModel: UtilityViewModel,
    state: com.example.ui.viewmodel.StudioUiState,
    workingBitmap: Bitmap?,
    previewBitmap: Bitmap?
) {
    if (workingBitmap != null) {
        val origBmp = state.originalBitmap
        if (state.isBeforeAfterActive && origBmp != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = origBmp.asImageBitmap(),
                    contentDescription = "Original Reference Image",
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xCCEF4444),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "BEFORE (ORIGINAL) • Tap Before/After to toggle",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        } else when (state.activeTab) {
            StudioTab.CROP -> {
                CropOverlayView(
                    bitmap = workingBitmap,
                    aspectRatio = state.selectedCropRatio,
                    cropShape = state.cropShape,
                    guideGrid = state.cropGuideGrid,
                    onCropBoundsChanged = { viewModel.setCropBounds(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudioTab.ASPECT_RATIO -> {
                if (state.aspectApplyMode == AspectRatioApplyMode.INTERACTIVE_CROP) {
                    CropOverlayView(
                        bitmap = workingBitmap,
                        aspectRatio = if (state.isCustomAspectSelected) {
                            CropAspectRatio("Custom", state.customAspectX, state.customAspectY)
                        } else {
                            state.activeAspectPreset.toCropAspectRatio()
                        },
                        onCropBoundsChanged = { viewModel.setCropBounds(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AspectRatioPreviewOverlay(
                        bitmap = workingBitmap,
                        state = state,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            StudioTab.PASSPORT -> {
                Box(contentAlignment = Alignment.Center) {
                    val baseBmp = previewBitmap ?: workingBitmap
                    val displayBmp = if (state.passportFineRotation != 0f && baseBmp != null) {
                        remember(baseBmp, state.passportFineRotation) {
                            BitmapUtils.rotateBitmapFloat(baseBmp, state.passportFineRotation)
                        }
                    } else {
                        baseBmp
                    }
                    if (displayBmp != null) {
                        Image(
                            bitmap = displayBmp.asImageBitmap(),
                            contentDescription = "Passport & ID Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (state.showBiometricGuides) {
                        BiometricGuideOverlay(
                            modifier = Modifier.fillMaxSize(),
                            showFaceOval = state.showFaceOvalGuide,
                            showCrownChin = state.showHeadCrownChinGuides,
                            showEyeLine = state.showEyeLineGuide,
                            showCenterAxis = state.showCenterAxisGuide,
                            showGrid = state.showGridGuide
                        )
                    }
                }
            }
            StudioTab.SHEET -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sheetBmp = state.sheetResult?.bitmap ?: previewBitmap ?: workingBitmap
                    if (sheetBmp != null) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Image(
                                bitmap = sheetBmp.asImageBitmap(),
                                contentDescription = "Photo Sheet Live Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Surface(
                        color = Color(0xDD0F172A),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        val res = state.sheetResult
                        val count = res?.totalCopies ?: state.sheetConfig.copies
                        val paperName = state.sheetConfig.paperPreset.name
                        val rotStr = if (state.sheetConfig.rotation.degrees > 0f) " • Rotated ${state.sheetConfig.rotation.degrees.toInt()}°" else ""
                        Text(
                            text = "$count Copies • $paperName (${state.sheetConfig.orientation.label})$rotStr",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            StudioTab.ROTATE, StudioTab.STRAIGHTEN -> {
                RotateStraightenPreviewOverlay(
                    bitmap = previewBitmap ?: workingBitmap,
                    state = state,
                    onHorizonOffsetChanged = { viewModel.setHorizonGuideYOffset(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudioTab.FLIP -> {
                FlipPreviewOverlay(
                    viewModel = viewModel,
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudioTab.PERSPECTIVE -> {
                PerspectivePreviewOverlay(
                    viewModel = viewModel,
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudioTab.ENHANCE -> {
                EnhancePreviewOverlay(
                    viewModel = viewModel,
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
            StudioTab.BACKGROUND -> {
                BackgroundPreviewOverlay(
                    viewModel = viewModel,
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Image(
                    bitmap = (previewBitmap ?: workingBitmap).asImageBitmap(),
                    contentDescription = "Preview Image",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        Text(
            text = "No image loaded",
            color = Color(0xFF64748B),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (state.isLoading || state.isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.statusMessage ?: "Processing image...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}


@Composable
fun ExactSizeConceptExplainer(
    viewModel: UtilityViewModel,
    state: com.example.ui.viewmodel.StudioUiState
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "3 DIFFERENT MEANINGS OF \"SIZE\"",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Exact Size refers to 3 fundamentally different operations. Switch between them directly:",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 3-Way Quick Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isA = state.activeTab == StudioTab.PIXEL_SIZE
                val isB = state.activeTab == StudioTab.PRINT_SIZE
                val isC = state.activeTab == StudioTab.FILE_SIZE

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isA) Color(0xFF38BDF8) else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectTab(StudioTab.PIXEL_SIZE) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "A: PIXELS",
                            color = if (isA) Color(0xFF0F172A) else Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "300x100 px",
                            color = if (isA) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isB) Color(0xFF34D399) else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectTab(StudioTab.PRINT_SIZE) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "B: PRINT & DPI",
                            color = if (isB) Color(0xFF0F172A) else Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "2x1 in @ 300 DPI",
                            color = if (isB) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isC) Color(0xFFF59E0B) else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectTab(StudioTab.FILE_SIZE) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "C: FILE SIZE",
                            color = if (isC) Color(0xFF0F172A) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        val pillSubtitle = if (state.targetSizeKb != null) {
                            when (state.fileSizeMode) {
                                FileSizeMode.MAXIMUM_CEILING -> "≤ ${state.targetSizeKb} KB"
                                FileSizeMode.TARGET_CLOSEST -> "~${state.targetSizeKb} KB"
                                FileSizeMode.EXACT_BYTE_ALIGNMENT -> "Exact ${state.targetSizeKb} KB"
                            }
                        } else "Quality ${state.quality}%"
                        Text(
                            text = pillSubtitle,
                            color = if (isC) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelSizeControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    var widthText by remember(state.targetWidthPx) { mutableStateOf(state.targetWidthPx.toString()) }
    var heightText by remember(state.targetHeightPx) { mutableStateOf(state.targetHeightPx.toString()) }

    Column {
        Text(
            text = "A. EXACT PIXEL DIMENSIONS (DIGITAL GRID)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )
        Text(
            text = "Controls the exact digital raster matrix (e.g. 300 × 100 px). Dictates display resolution on screens, websites, and apps.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF94A3B8))
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = widthText,
            onValueChange = {
                widthText = it
                val w = it.toIntOrNull()
                if (w != null && w > 0) {
                    viewModel.setTargetDimensions(w, state.targetHeightPx, state.keepAspectRatio)
                }
            },
            label = { Text("Width (px)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        IconButton(
            onClick = {
                viewModel.setTargetDimensions(state.targetWidthPx, state.targetHeightPx, !state.keepAspectRatio)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = if (state.keepAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock Aspect Ratio",
                tint = if (state.keepAspectRatio) Color(0xFF38BDF8) else Color(0xFF94A3B8)
            )
        }

        OutlinedTextField(
            value = heightText,
            onValueChange = {
                heightText = it
                val h = it.toIntOrNull()
                if (h != null && h > 0) {
                    viewModel.setTargetDimensions(state.targetWidthPx, h, state.keepAspectRatio)
                }
            },
            label = { Text("Height (px)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Professional Resize Modes (FIT, FILL, STRETCH, SMART CROP)
    Text(
        text = "RESIZE MODE",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            com.example.model.ResizeMode.FIT to Icons.Default.FitScreen,
            com.example.model.ResizeMode.FILL to Icons.Default.CropFree,
            com.example.model.ResizeMode.STRETCH to Icons.Default.OpenWith,
            com.example.model.ResizeMode.SMART_CROP to Icons.Default.CenterFocusStrong
        ).forEach { (mode, icon) ->
            val isSelected = state.resizeMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setResizeMode(mode) },
                label = { Text(mode.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (mode == com.example.model.ResizeMode.STRETCH) Color(0xFFF59E0B) else Color(0xFF0284C7),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0),
                    iconColor = Color(0xFF94A3B8)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Mode description subtext
    Text(
        text = state.resizeMode.description,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF94A3B8))
    )

    // Distortion Warning for STRETCH mode or when proportions change
    val srcBmp = state.workingBitmap
    val isAspectDifferent = if (srcBmp != null && state.targetHeightPx > 0) {
        val srcAspect = srcBmp.width.toFloat() / srcBmp.height.toFloat()
        val targetAspect = state.targetWidthPx.toFloat() / state.targetHeightPx.toFloat()
        kotlin.math.abs(srcAspect - targetAspect) > 0.02f
    } else false

    if (state.resizeMode == com.example.model.ResizeMode.STRETCH && isAspectDifferent) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x33F59E0B)),
            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Distortion Warning",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Distortion Warning",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCD34D),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Stretch mode forces the image into non-native aspect ratios, causing visible image deformation. Switch to Fit, Fill, or Smart Crop to maintain natural proportions.",
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    if (state.resizeMode == com.example.model.ResizeMode.SMART_CROP) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "✨ AI Smart Crop analyzes face coordinates & salient visual gradients to center the key subjects automatically.",
            color = Color(0xFF38BDF8),
            fontSize = 11.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "SCALE BY PERCENTAGE",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(25, 50, 75, 150, 200).forEach { pct ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .clickable { viewModel.applyPercentageScale(pct) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pct%",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "EXACT PIXEL & PASSPORT PRESETS",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "1080 × 1080 px (1:1 Square)" to (1080 to 1080),
            "1920 × 1080 px (Full HD)" to (1920 to 1080),
            "300 × 100 px (Header / Banner)" to (300 to 100),
            "600 × 600 px (Passport US 2x2\" @ 300 DPI)" to (600 to 600),
            "413 × 531 px (Passport EU/UK 35x45mm @ 300 DPI)" to (413 to 531),
            "413 × 531 px (India PAN/Passport 3.5x4.5cm)" to (413 to 531),
            "1080 × 1920 px (Story 9:16)" to (1080 to 1920),
            "1200 × 630 px (Social Share)" to (1200 to 630)
        ).forEach { (label, dims) ->
            FilterChip(
                selected = state.targetWidthPx == dims.first && state.targetHeightPx == dims.second,
                onClick = {
                    viewModel.setTargetDimensions(dims.first, dims.second, false)
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.applyExactWidth(state.targetWidthPx) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("pixel_exact_width_button")
        ) {
            Text("Exact Width", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Button(
            onClick = { viewModel.applyExactHeight(state.targetHeightPx) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("pixel_exact_height_button")
        ) {
            Text("Exact Height", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = { viewModel.applyResize() },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apply_pixel_resize_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Apply Exact Pixel Resize (${state.targetWidthPx} × ${state.targetHeightPx} px)",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }

    state.lastResizeReport?.let { report ->
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (report.isValid) Color(0x2210B981) else Color(0x22EF4444)
            ),
            border = BorderStroke(
                1.dp,
                if (report.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (report.isValid) "✓ RESIZE VERIFIED" else "⚠ RESIZE WARNING",
                        color = if (report.isValid) Color(0xFF34D399) else Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${report.actualWidth} × ${report.actualHeight} px",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mode: ${report.mode.label} | Non-empty: ${report.isNonEmpty} | Memory: ${report.memoryFootprintBytes / 1024} KB",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PrintSizeControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    var widthText by remember(state.printWidthPhysical, state.printUnit) {
        mutableStateOf(String.format("%.2f", state.printWidthPhysical))
    }
    var heightText by remember(state.printHeightPhysical, state.printUnit) {
        mutableStateOf(String.format("%.2f", state.printHeightPhysical))
    }
    var customDpiText by remember(state.dpi) {
        mutableStateOf(state.dpi.toString())
    }

    // Direct pixel inputs for interactive calculation
    var pixelWidthText by remember(state.targetWidthPx) {
        mutableStateOf(state.targetWidthPx.toString())
    }
    var pixelHeightText by remember(state.targetHeightPx) {
        mutableStateOf(state.targetHeightPx.toString())
    }

    // Source image dimensions
    val sourceW = state.workingBitmap?.width ?: state.originalWidth.takeIf { it > 0 } ?: state.targetWidthPx
    val sourceH = state.workingBitmap?.height ?: state.originalHeight.takeIf { it > 0 } ?: state.targetHeightPx

    // Physical dimensions in inches for universal density & effective DPI calculation
    val physicalWidthInches = when (state.printUnit) {
        PrintUnit.INCHES -> state.printWidthPhysical
        PrintUnit.CENTIMETERS -> state.printWidthPhysical / 2.54f
        PrintUnit.MILLIMETERS -> state.printWidthPhysical / 25.4f
    }.coerceAtLeast(0.01f)

    val physicalHeightInches = when (state.printUnit) {
        PrintUnit.INCHES -> state.printHeightPhysical
        PrintUnit.CENTIMETERS -> state.printHeightPhysical / 2.54f
        PrintUnit.MILLIMETERS -> state.printHeightPhysical / 25.4f
    }.coerceAtLeast(0.01f)

    // Conversions in all physical units
    val widthIn = physicalWidthInches
    val heightIn = physicalHeightInches
    val widthCm = physicalWidthInches * 2.54f
    val heightCm = physicalHeightInches * 2.54f
    val widthMm = physicalWidthInches * 25.4f
    val heightMm = physicalHeightInches * 25.4f

    // Effective DPI if source bitmap is printed at target physical dimensions
    val effectiveDpiW = (sourceW / physicalWidthInches).roundToInt()
    val effectiveDpiH = (sourceH / physicalHeightInches).roundToInt()
    val effectiveDpi = minOf(effectiveDpiW, effectiveDpiH)

    // Aspect Ratio Calculation
    val aspectDecimal = physicalWidthInches / physicalHeightInches
    val gcdVal = gcd(state.targetWidthPx, state.targetHeightPx)
    val ratioFraction = if (gcdVal > 0) "${state.targetWidthPx / gcdVal}:${state.targetHeightPx / gcdVal}" else "${String.format("%.2f", aspectDecimal)}:1"

    // Max recommended physical print size at standard 300 DPI
    val maxRecommendedWidthIn = sourceW / 300.0f
    val maxRecommendedHeightIn = sourceH / 300.0f

    Column {
        Text(
            text = "18. PRINT SIZE CALCULATOR",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
        )
        Text(
            text = "Calculate required pixels, physical paper dimensions across units (in, cm, mm), DPI density, and aspect ratios with resolution quality verification.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF94A3B8))
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1. STANDARD PRINT SIZE PRESETS
    Text(
        text = "PRINT SIZE PRESETS",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            StandardPrintPreset.PHOTO_4X6,
            StandardPrintPreset.PHOTO_5X7,
            StandardPrintPreset.ISO_A4,
            StandardPrintPreset.ISO_A5,
            StandardPrintPreset.US_LETTER,
            StandardPrintPreset.US_LEGAL,
            StandardPrintPreset.CUSTOM
        ).forEach { preset ->
            val isSelected = state.selectedPrintPreset == preset
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.applyStandardPrintPreset(preset) },
                label = { Text(preset.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A),
                    labelColor = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. PHYSICAL MEASUREMENT UNIT SELECTOR (Inches, Centimeters, Millimeters)
    Text(
        text = "PHYSICAL MEASUREMENT UNIT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PrintUnit.values().forEach { unit ->
            val selected = state.printUnit == unit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) Color(0xFF34D399) else Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .border(1.dp, if (selected) Color(0xFF34D399) else Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable {
                        val currentWInches = when (state.printUnit) {
                            PrintUnit.INCHES -> state.printWidthPhysical
                            PrintUnit.CENTIMETERS -> state.printWidthPhysical / 2.54f
                            PrintUnit.MILLIMETERS -> state.printWidthPhysical / 25.4f
                        }
                        val currentHInches = when (state.printUnit) {
                            PrintUnit.INCHES -> state.printHeightPhysical
                            PrintUnit.CENTIMETERS -> state.printHeightPhysical / 2.54f
                            PrintUnit.MILLIMETERS -> state.printHeightPhysical / 25.4f
                        }
                        val convertedW = when (unit) {
                            PrintUnit.INCHES -> currentWInches
                            PrintUnit.CENTIMETERS -> currentWInches * 2.54f
                            PrintUnit.MILLIMETERS -> currentWInches * 25.4f
                        }
                        val convertedH = when (unit) {
                            PrintUnit.INCHES -> currentHInches
                            PrintUnit.CENTIMETERS -> currentHInches * 2.54f
                            PrintUnit.MILLIMETERS -> currentHInches * 25.4f
                        }
                        viewModel.setPrintDimensions(
                            width = convertedW,
                            height = convertedH,
                            unit = unit,
                            dpi = state.dpi,
                            keepRatio = state.keepAspectRatio
                        )
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${unit.label} (${unit.symbol})",
                    color = if (selected) Color(0xFF0F172A) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. TARGET DPI / PPI PRESETS & INPUT (72, 96, 150, 300, 600, Custom)
    Text(
        text = "PRINT DENSITY (DPI / PPI)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            72 to "72",
            96 to "96",
            150 to "150",
            300 to "300",
            600 to "600"
        ).forEach { (dpiVal, dpiLabel) ->
            val selected = state.dpi == dpiVal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) Color(0xFF34D399) else Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .border(1.dp, if (selected) Color(0xFF34D399) else Color(0xFF334155), RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.setDpiForPrint(dpiVal)
                        customDpiText = dpiVal.toString()
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dpiLabel,
                    color = if (selected) Color(0xFF0F172A) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. PHYSICAL DIMENSION INPUT FIELDS (Width & Height)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = widthText,
            onValueChange = {
                widthText = it
                val w = it.toFloatOrNull()
                if (w != null && w > 0f) {
                    val targetH = if (state.keepAspectRatio && state.printWidthPhysical > 0) {
                        (w / state.printWidthPhysical) * state.printHeightPhysical
                    } else state.printHeightPhysical
                    viewModel.setPrintDimensions(
                        width = w,
                        height = targetH,
                        unit = state.printUnit,
                        dpi = state.dpi,
                        keepRatio = state.keepAspectRatio
                    )
                }
            },
            label = { Text("Width (${state.printUnit.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF34D399),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        IconButton(
            onClick = {
                viewModel.setPrintDimensions(
                    width = state.printWidthPhysical,
                    height = state.printHeightPhysical,
                    unit = state.printUnit,
                    dpi = state.dpi,
                    keepRatio = !state.keepAspectRatio
                )
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = if (state.keepAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock Aspect Ratio",
                tint = if (state.keepAspectRatio) Color(0xFF34D399) else Color(0xFF94A3B8)
            )
        }

        OutlinedTextField(
            value = heightText,
            onValueChange = {
                heightText = it
                val h = it.toFloatOrNull()
                if (h != null && h > 0f) {
                    val targetW = if (state.keepAspectRatio && state.printHeightPhysical > 0) {
                        (h / state.printHeightPhysical) * state.printWidthPhysical
                    } else state.printWidthPhysical
                    viewModel.setPrintDimensions(
                        width = targetW,
                        height = h,
                        unit = state.printUnit,
                        dpi = state.dpi,
                        keepRatio = state.keepAspectRatio
                    )
                }
            },
            label = { Text("Height (${state.printUnit.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF34D399),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 5. DIRECT PIXELS / DPI CALCULATOR FIELDS
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = pixelWidthText,
            onValueChange = {
                pixelWidthText = it
                val pxW = it.toIntOrNull()
                if (pxW != null && pxW > 0) {
                    val newInchesW = pxW.toFloat() / state.dpi
                    val newPhysicalW = when (state.printUnit) {
                        PrintUnit.INCHES -> newInchesW
                        PrintUnit.CENTIMETERS -> newInchesW * 2.54f
                        PrintUnit.MILLIMETERS -> newInchesW * 25.4f
                    }
                    viewModel.setPrintDimensions(
                        width = newPhysicalW,
                        height = state.printHeightPhysical,
                        unit = state.printUnit,
                        dpi = state.dpi,
                        keepRatio = state.keepAspectRatio
                    )
                }
            },
            label = { Text("Target W (px)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        OutlinedTextField(
            value = pixelHeightText,
            onValueChange = {
                pixelHeightText = it
                val pxH = it.toIntOrNull()
                if (pxH != null && pxH > 0) {
                    val newInchesH = pxH.toFloat() / state.dpi
                    val newPhysicalH = when (state.printUnit) {
                        PrintUnit.INCHES -> newInchesH
                        PrintUnit.CENTIMETERS -> newInchesH * 2.54f
                        PrintUnit.MILLIMETERS -> newInchesH * 25.4f
                    }
                    viewModel.setPrintDimensions(
                        width = state.printWidthPhysical,
                        height = newPhysicalH,
                        unit = state.printUnit,
                        dpi = state.dpi,
                        keepRatio = state.keepAspectRatio
                    )
                }
            },
            label = { Text("Target H (px)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )

        OutlinedTextField(
            value = customDpiText,
            onValueChange = {
                customDpiText = it
                val d = it.toIntOrNull()
                if (d != null && d in 10..2400) {
                    viewModel.setDpiForPrint(d)
                }
            },
            label = { Text("DPI") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFBBF24),
                unfocusedBorderColor = Color(0xFF475569)
            )
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 6. CALCULATED OUTPUTS (Required Pixels, Physical Dimensions Across All Units, Aspect Ratio)
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CALCULATED OUTPUT SUMMARY",
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // A: Required Pixels
            val mp = (state.targetWidthPx.toLong() * state.targetHeightPx) / 1_000_000.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Required Pixel Canvas:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(
                    text = "${state.targetWidthPx} × ${state.targetHeightPx} px (${String.format("%.2f", mp)} MP)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // B: Physical Dimensions Across Units
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Inches:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("${String.format("%.2f", widthIn)}\" × ${String.format("%.2f", heightIn)}\"", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Centimeters:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("${String.format("%.1f", widthCm)} × ${String.format("%.1f", heightCm)} cm", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Millimeters:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("${String.format("%.1f", widthMm)} × ${String.format("%.1f", heightMm)} mm", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // C: Aspect Ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aspect Ratio:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(
                    text = "$ratioFraction (${String.format("%.2f", aspectDecimal)}:1)",
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // D: Print Bleed Margin & Viewing Distance (Phase 8 Print Calculator)
            val calcReport = state.lastPrintReport
            if (calcReport != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min Viewing Distance:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text(
                        text = "${String.format("%.0f", calcReport.minViewingDistanceCm)} cm (${calcReport.qualityGrade.label})",
                        color = Color(0xFFA78BFA),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
                if (state.printBleedMm > 0f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("With ${state.printBleedMm}mm Bleed:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "${calcReport.totalWithBleedWidthPx} × ${calcReport.totalWithBleedHeightPx} px",
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Print Bleed Margin Selector (0mm, 2mm, 3mm, 5mm)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Bleed:", color = Color(0xFF94A3B8), fontSize = 11.sp)
        listOf(0f to "0mm", 2f to "2mm", 3f to "3mm (Std)", 5f to "5mm").forEach { (bleedMm, label) ->
            FilterChip(
                selected = kotlin.math.abs(state.printBleedMm - bleedMm) < 0.1f,
                onClick = { viewModel.setPrintBleedMm(bleedMm) },
                label = { Text(label, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 7. PRINT QUALITY & RESOLUTION HEALTH WARNING SYSTEM
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                effectiveDpi < 150 -> Color(0xFF450A0A) // Red alert
                effectiveDpi < 200 -> Color(0xFF451A03) // Amber warning
                effectiveDpi < 300 -> Color(0xFF0C2440) // Blue good
                else -> Color(0xFF064E3B) // Green excellent
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                effectiveDpi < 150 -> Color(0xFFEF4444)
                effectiveDpi < 200 -> Color(0xFFF59E0B)
                effectiveDpi < 300 -> Color(0xFF38BDF8)
                else -> Color(0xFF10B981)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (effectiveDpi < 150) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = "Resolution Health",
                        tint = when {
                            effectiveDpi < 150 -> Color(0xFFEF4444)
                            effectiveDpi < 200 -> Color(0xFFF59E0B)
                            effectiveDpi < 300 -> Color(0xFF38BDF8)
                            else -> Color(0xFF34D399)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            effectiveDpi < 150 -> "⚠️ INSUFFICIENT RESOLUTION FOR PRINT"
                            effectiveDpi < 200 -> "MODERATE PRINT RESOLUTION"
                            effectiveDpi < 300 -> "GOOD PRINT RESOLUTION"
                            else -> "EXCELLENT HIGH-RES PRINT QUALITY"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                effectiveDpi < 150 -> Color(0xFFFCA5A5)
                                effectiveDpi < 200 -> Color(0xFFFDE68A)
                                effectiveDpi < 300 -> Color(0xFFBAE6FD)
                                else -> Color(0xFFA7F3D0)
                            }
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "$effectiveDpi Effective DPI",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (effectiveDpi < 150) {
                Text(
                    text = "WARNING: Source resolution ($sourceW × $sourceH px) is too low for this ${String.format("%.1f", widthIn)}\" × ${String.format("%.1f", heightIn)}\" print size. Output will appear visibly pixelated, blurry, or blocky.",
                    color = Color(0xFFFEE2E2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Required at 300 DPI: ${state.targetWidthPx} × ${state.targetHeightPx} px\n• Available in Source: $sourceW × $sourceH px\n• Max Recommended Print Size at 300 DPI: ${String.format("%.2f", maxRecommendedWidthIn)}\" × ${String.format("%.2f", maxRecommendedHeightIn)}\"",
                    color = Color(0xFFFECACA),
                    fontSize = 10.sp
                )
            } else if (effectiveDpi < 200) {
                Text(
                    text = "Source image yields $effectiveDpi DPI. Suitable for casual prints or viewing from a distance, but fine details and text may exhibit slight softness.",
                    color = Color(0xFFFEF3C7),
                    fontSize = 11.sp
                )
            } else if (effectiveDpi < 300) {
                Text(
                    text = "Source image yields $effectiveDpi DPI. Clean, clear print quality suitable for standard photo albums and framed wall art.",
                    color = Color(0xFFE0F2FE),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "Source image provides $effectiveDpi DPI (≥ 300 DPI). Meets commercial photo lab and high-end archival printing standards for razor-sharp clarity.",
                    color = Color(0xFFD1FAE5),
                    fontSize = 11.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 8. APPLY BUTTON
    Button(
        onClick = { viewModel.applyPrintResize() },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apply_print_size_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Apply Print Size (${String.format("%.2f", state.printWidthPhysical)} × ${String.format("%.2f", state.printHeightPhysical)} ${state.printUnit.symbol} @ ${state.dpi} DPI)",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun FileSizeControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    var customKbText by remember { mutableStateOf(state.targetSizeKb?.toString() ?: "") }
    var customQualityText by remember { mutableStateOf(state.quality.toString()) }
    var customCompText by remember { mutableStateOf(state.compressionPercentage.toString()) }
    var showEntropyExplanation by remember { mutableStateOf(false) }

    val opt = state.fileSizeOptimizationResult

    // Compute Before Stats
    val origBytes = if (state.originalFileSizeBytes > 0) state.originalFileSizeBytes else kotlin.math.max(1L, state.estimatedBytes * 2)
    val origW = if (state.originalWidth > 0) state.originalWidth else (state.workingBitmap?.width ?: state.targetWidthPx)
    val origH = if (state.originalHeight > 0) state.originalHeight else (state.workingBitmap?.height ?: state.targetHeightPx)
    val origFormatStr = if (state.originalMimeType.isNotBlank()) {
        state.originalMimeType.substringAfter("/").uppercase()
    } else "JPEG"
    val origSizeFormatted = FileSizeEngine.formatBytesToReadable(origBytes)

    // Compute After Stats
    val afterBytes = opt?.actualBytes ?: state.estimatedBytes
    val afterW = opt?.outputWidth ?: state.targetWidthPx
    val afterH = opt?.outputHeight ?: state.targetHeightPx
    val afterFormatStr = state.exportFormat.displayName
    val afterSizeFormatted = FileSizeEngine.formatBytesToReadable(afterBytes)

    val savedBytes = origBytes - afterBytes
    val savedPercentage = if (origBytes > 0) {
        ((savedBytes.toFloat() / origBytes.toFloat()) * 100f).coerceIn(-999f, 99.9f)
    } else 0f

    val savedAmountText = if (savedBytes >= 0) {
        "${FileSizeEngine.formatBytesToReadable(savedBytes)} saved"
    } else {
        "+${FileSizeEngine.formatBytesToReadable(abs(savedBytes))} increase"
    }

    val compressionPercentageText = if (savedBytes >= 0) {
        String.format("%.1f%% reduction", savedPercentage)
    } else {
        String.format("+%.1f%% expansion", abs(savedPercentage))
    }

    Column {
        Text(
            text = "IMAGE COMPRESSION & SIZE OPTIMIZATION",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
        )
        Text(
            text = "Fine-tune output file size, quality percentage, compression level, and compliance limits with real-time before/after statistics.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF94A3B8))
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1. BEFORE & AFTER STATS COMPARISON DISPLAY
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BEFORE vs AFTER COMPARISON",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (savedBytes >= 0) Color(0xFF065F46) else Color(0xFF78350F),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (savedBytes >= 0) "-${String.format("%.1f%%", savedPercentage)}" else "+${String.format("%.1f%%", abs(savedPercentage))}",
                        color = if (savedBytes >= 0) Color(0xFF34D399) else Color(0xFFFDE68A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two Columns: BEFORE vs AFTER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BEFORE CARD
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BEFORE",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ORIGINAL",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // File size
                        Text("File size", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = origSizeFormatted,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${origBytes} B",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Dimensions
                        Text("Dimensions", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = "$origW × $origH px",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Format
                        Text("Format", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = origFormatStr,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // AFTER CARD
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .background(Color(0xFF131D31), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AFTER",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "COMPRESSED",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // File size
                        Text("File size", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = afterSizeFormatted,
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${afterBytes} B",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Dimensions
                        Text("Dimensions", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = "$afterW × $afterH px",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Format
                        Text("Format", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = afterFormatStr,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Saved Amount & Compression %
                        Text("Saved amount", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = savedAmountText,
                            color = if (savedBytes >= 0) Color(0xFF34D399) else Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text("Compression percentage", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        Text(
                            text = compressionPercentageText,
                            color = if (savedBytes >= 0) Color(0xFF34D399) else Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reduction visual meter bar
            val reductionFraction = if (origBytes > 0) (afterBytes.toFloat() / origBytes.toFloat()).coerceIn(0.01f, 1.0f) else 1.0f
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Space Retained: ${String.format("%.1f%%", reductionFraction * 100f)}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Space Saved: ${String.format("%.1f%%", (1f - reductionFraction) * 100f)}",
                        fontSize = 10.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFF065F46), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = reductionFraction)
                            .height(6.dp)
                            .background(Color(0xFF38BDF8), CircleShape)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. PRESET QUALITY SELECTOR
    Text(
        text = "PRESET QUALITY",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompressionPreset.values().forEach { preset ->
            val isSelected = state.compressionPreset == preset && (preset != CompressionPreset.CUSTOM || state.targetSizeKb != null || state.quality != 85)
            FilterChip(
                selected = isSelected,
                onClick = {
                    viewModel.setCompressionPreset(preset)
                    if (preset != CompressionPreset.CUSTOM) {
                        customQualityText = preset.defaultQuality.toString()
                        customCompText = (100 - preset.defaultQuality).toString()
                    }
                },
                label = {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = preset.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (preset != CompressionPreset.CUSTOM) {
                            Text(
                                text = "Q: ${preset.defaultQuality}%",
                                fontSize = 9.sp,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF59E0B),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    // Preset description banner
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = state.compressionPreset.description,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. CUSTOM COMPRESSION & FINE-TUNING CONTROLS
    Text(
        text = "CUSTOM COMPRESSION CONTROLS",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Mode Selector (Quality % vs Compression % vs Target File Size vs Maximum File Size)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CustomCompressionMode.values().forEach { mode ->
            val isSelected = state.customCompressionMode == mode
            FilterChip(
                selected = isSelected,
                onClick = {
                    viewModel.setCustomCompressionMode(mode)
                },
                label = { Text(mode.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (state.customCompressionMode) {
        CustomCompressionMode.QUALITY_PERCENT -> {
            // QUALITY PERCENTAGE CONTROL
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Output Quality Percentage",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${state.quality}%",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.quality.toFloat(),
                        onValueChange = {
                            viewModel.setQuality(it.toInt())
                            customQualityText = it.toInt().toString()
                            customCompText = (100 - it.toInt()).toString()
                        },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B)
                        )
                    )

                    // Quick percentage chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(20, 40, 60, 75, 85, 95, 100).forEach { qVal ->
                            val sel = state.quality == qVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (sel) Color(0xFFF59E0B) else Color(0xFF0F172A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.setQuality(qVal)
                                        customQualityText = qVal.toString()
                                        customCompText = (100 - qVal).toString()
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$qVal%",
                                    fontSize = 10.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) Color(0xFF0F172A) else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customQualityText,
                        onValueChange = {
                            customQualityText = it
                            val q = it.toIntOrNull()
                            if (q != null && q in 1..100) {
                                viewModel.setQuality(q)
                                customCompText = (100 - q).toString()
                            }
                        },
                        label = { Text("Custom Quality % (1-100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }
            }
        }

        CustomCompressionMode.COMPRESSION_PERCENT -> {
            // COMPRESSION PERCENTAGE CONTROL
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compression Reduction Level",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${state.compressionPercentage}%",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.compressionPercentage.toFloat(),
                        onValueChange = {
                            viewModel.setCompressionPercentage(it.toInt())
                            customCompText = it.toInt().toString()
                            customQualityText = (100 - it.toInt()).toString()
                        },
                        valueRange = 0f..99f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        )
                    )

                    // Quick reduction chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 15, 30, 50, 70, 85, 90).forEach { cVal ->
                            val sel = state.compressionPercentage == cVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (sel) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.setCompressionPercentage(cVal)
                                        customCompText = cVal.toString()
                                        customQualityText = (100 - cVal).toString()
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cVal%",
                                    fontSize = 10.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) Color(0xFF0F172A) else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCompText,
                        onValueChange = {
                            customCompText = it
                            val c = it.toIntOrNull()
                            if (c != null && c in 0..99) {
                                viewModel.setCompressionPercentage(c)
                                customQualityText = (100 - c).toString()
                            }
                        },
                        label = { Text("Custom Compression % (0-99% reduction)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }
            }
        }

        CustomCompressionMode.TARGET_SIZE, CustomCompressionMode.MAX_CEILING -> {
            // TARGET & MAXIMUM FILE SIZE CONTROLS
            val isCeiling = state.customCompressionMode == CustomCompressionMode.MAX_CEILING

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick Size Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "20 KB (Portals)" to 20,
                        "50 KB (Govt Forms)" to 50,
                        "100 KB (Visa)" to 100,
                        "200 KB" to 200,
                        "500 KB" to 500,
                        "1 MB" to 1024,
                        "2 MB" to 2048
                    ).forEach { (label, kb) ->
                        val prefix = if (isCeiling) "≤ " else "~"
                        val selected = state.targetSizeKb == kb
                        FilterChip(
                            selected = selected,
                            onClick = {
                                viewModel.setTargetFileSizeKb(kb)
                                customKbText = kb.toString()
                            },
                            label = { Text("$prefix$label", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF59E0B),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                // Custom KB Field
                OutlinedTextField(
                    value = customKbText,
                    onValueChange = {
                        customKbText = it
                        val kb = it.toIntOrNull()
                        if (kb != null && kb > 0) {
                            viewModel.setTargetFileSizeKb(kb)
                        }
                    },
                    label = { Text(if (isCeiling) "Enter Maximum Ceiling File Size (KB)" else "Enter Target File Size (KB)") },
                    placeholder = { Text("e.g. 50 for 50 KB") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                // Dimension Downscaling Permission Switch
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Allow Dimension Downscaling",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (state.allowDimensionDownscaling)
                                    "Permits downscaling pixels if quality 5% cannot reach small size limit"
                                else
                                    "Locked: Strict ${state.targetWidthPx} × ${state.targetHeightPx} px (quality only)",
                                color = if (state.allowDimensionDownscaling) Color(0xFF34D399) else Color(0xFFF59E0B),
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = state.allowDimensionDownscaling,
                            onCheckedChange = { viewModel.setAllowDimensionDownscaling(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF59E0B),
                                checkedTrackColor = Color(0xFF78350F),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. REAL ITERATIVE ENCODING & MEASURED VERIFICATION CARD
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME COMPRESSION TELEMETRY",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                val badgeText: String
                val badgeBg: Color
                val badgeFg: Color

                if (opt != null && state.targetSizeKb != null) {
                    if (opt.isExactByteMatch) {
                        badgeText = "✓ EXACT VERIFIED"
                        badgeBg = Color(0xFF065F46)
                        badgeFg = Color(0xFF34D399)
                    } else if (state.fileSizeMode == FileSizeMode.MAXIMUM_CEILING && opt.isCompliantWithCeiling) {
                        badgeText = "✓ CEILING MET"
                        badgeBg = Color(0xFF065F46)
                        badgeFg = Color(0xFF34D399)
                    } else if (state.fileSizeMode == FileSizeMode.MAXIMUM_CEILING && !opt.isCompliantWithCeiling) {
                        badgeText = "⚠ EXCEEDS CEILING"
                        badgeBg = Color(0xFF7F1D1D)
                        badgeFg = Color(0xFFFCA5A5)
                    } else {
                        badgeText = "~ CLOSEST SIZE"
                        badgeBg = Color(0xFF78350F)
                        badgeFg = Color(0xFFFDE68A)
                    }
                } else {
                    badgeText = "QUALITY ${state.quality}%"
                    badgeBg = Color(0xFF1E293B)
                    badgeFg = Color(0xFF38BDF8)
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (state.targetSizeKb != null && opt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Target Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("TARGET", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${state.targetSizeKb} KB",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${opt.targetBytes} B", color = Color(0xFF64748B), fontSize = 9.sp)
                        }
                    }

                    // Actual Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ACTUAL", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val actualKb = opt.actualBytes / 1024.0
                            val actualStr = if (actualKb > 1024) String.format("%.2f MB", actualKb / 1024.0) else String.format("%.2f KB", actualKb)
                            Text(
                                text = actualStr,
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${opt.actualBytes} B", color = Color(0xFF64748B), fontSize = 9.sp)
                        }
                    }

                    // Difference Box
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("DIFFERENCE", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val diffSign = if (opt.differenceBytes > 0) "+" else ""
                            val diffColor = when {
                                opt.isExactByteMatch -> Color(0xFF34D399)
                                opt.differenceBytes <= 0 -> Color(0xFF34D399)
                                else -> Color(0xFFF87171)
                            }
                            Text(
                                text = "$diffSign${opt.differenceBytes} B",
                                color = diffColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format("%.1f%%", opt.percentDifference),
                                color = diffColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6-Metric Verified Readout Panel
                val actualKbVal = opt.actualBytes / 1024.0
                val actualKbFormatted = if (actualKbVal >= 1024) String.format("%.2f MB", actualKbVal / 1024.0) else String.format("%.1f KB", actualKbVal)
                val targetKbFormatted = "${state.targetSizeKb} KB"
                val diffKbVal = abs(opt.differenceBytes) / 1024.0
                val diffKbFormatted = if (abs(opt.differenceBytes) < 1024) "${abs(opt.differenceBytes)} B" else String.format("%.1f KB", diffKbVal)
                val formatShort = when (opt.format) {
                    ExportFormat.JPEG -> "JPG"
                    ExportFormat.PNG -> "PNG"
                    ExportFormat.WEBP_LOSSY -> "WEBP"
                    ExportFormat.WEBP_LOSSLESS -> "WEBP (LL)"
                    ExportFormat.PDF -> "PDF"
                }

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(targetKbFormatted, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Actual:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(actualKbFormatted, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Difference:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(diffKbFormatted, color = if (opt.differenceBytes <= 0) Color(0xFF34D399) else Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dimensions:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${opt.outputWidth} × ${opt.outputHeight}", color = Color(0xFFE2E8F0), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Format:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(formatShort, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Quality:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${opt.quality}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131D31), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Binary Search Result: Quality ${opt.quality}% across ${opt.iterationsCount} iterative passes",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Output Grid: ${opt.outputWidth} × ${opt.outputHeight} px ${if (opt.dimensionsAdjusted) "(Adaptive downscaling applied)" else "(Original dimensions preserved)"}",
                        color = if (opt.dimensionsAdjusted) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Format Bitstream: ${opt.format.displayName} (${opt.format.mimeType})",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (opt.isExactByteMatch) Color(0xFF064E3B) else Color(0xFF1E293B),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (opt.isExactByteMatch)
                            "VERIFIED: Certified exact byte match (${opt.actualBytes} / ${opt.targetBytes} bytes). Clean metadata comment padding embedded."
                        else if (state.fileSizeMode == FileSizeMode.MAXIMUM_CEILING && opt.isCompliantWithCeiling)
                            "VERIFIED: Strictly compliant ceiling (${opt.actualBytes} ≤ ${opt.targetBytes} B). Headroom: ${abs(opt.differenceBytes)} bytes."
                        else if (state.fileSizeMode == FileSizeMode.MAXIMUM_CEILING && !opt.isCompliantWithCeiling)
                            "UNVERIFIED: File is ${opt.differenceBytes} B over ceiling. Enable dimension downscaling or choose a lower target."
                        else
                            "CLOSEST ATTAINABLE: Reached ${opt.actualBytes} bytes without destructive distortion. Not claimed as exact.",
                        color = if (opt.isExactByteMatch || opt.isCompliantWithCeiling) Color(0xFF34D399) else Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val actualKb = state.estimatedBytes / 1024.0
                val actualStr = if (actualKb > 1024) String.format("%.2f MB", actualKb / 1024.0) else String.format("%.1f KB", actualKb)
                Text(
                    text = "• Resulting Compressed Size: $actualStr (${state.estimatedBytes} bytes)",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Compression Quality: ${state.quality}%  |  Compression Level: ${state.compressionPercentage}%",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            state.lastCompressionReport?.let { compReport ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (compReport.isValid) Color(0x2210B981) else Color(0x22EF4444)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (compReport.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (compReport.isValid) "✓ COMPRESSION VERIFIED" else "⚠ VERIFICATION WARNING",
                                color = if (compReport.isValid) Color(0xFF34D399) else Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${compReport.actualBytes / 1024} KB (${compReport.actualBytes} B)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ratio: ${String.format("%.1fx", compReport.compressionRatio)} | Iterations: ${compReport.iterationsCount} | Magic Bytes: ${if (compReport.magicBytesValid) "Valid" else "Invalid"} | Decode Safe: ${compReport.isDecodeSafe}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // 5. EDUCATIONAL CARD: WHY EXACT MATCHING IS CONTENT-DEPENDENT
    Spacer(modifier = Modifier.height(10.dp))
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEntropyExplanation = !showEntropyExplanation }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Why exact matching may not be possible",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (showEntropyExplanation) "Hide ▲" else "Read ▼",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (showEntropyExplanation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Content-Dependent Entropy: Natural images contain variable high-frequency details. JPEG and WebP convert pixel blocks via Discrete Cosine Transform (DCT) into entropy-coded variable-length bitstreams. High-contrast images take more bits than flat backgrounds.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2. Discrete Quantization Steps: JPEG quality levels (1-100) alter compression tables in coarse mathematical leaps (often 500-2,000 bytes per integer step). Natural compression rarely lands on an exact byte count on the dot.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3. Image Studio Integrity Guarantee: We never falsely claim 'Exact 20 KB' unless byte-level verification passes. In 'Ceiling' mode, we guarantee the file is ≤ target. In 'Closest' mode, we find the nearest non-damaging compression. In 'Exact' mode, we pad the stream with clean container metadata comments.",
                    color = Color(0xFF34D399),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun FormatControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    // 1. Source Image Format & Alpha Status Card
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOURCE IMAGE DETECTED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = state.detectedInputFormat.displayName,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alpha Channel / Transparency:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                if (state.hasTransparency) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "✓ Transparent Layer Present",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Solid (No Transparency)",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. One-Tap Quick Conversion Matrix
    Text(
        text = "QUICK FORMAT CONVERSION PRESETS",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormatConversionPair.values().forEach { pair ->
            val isSelected = state.exportFormat == pair.toFormat
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF0369A1) else Color(0xFF0F172A)
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                modifier = Modifier.clickable {
                    viewModel.applyConversionPair(pair)
                }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = pair.label,
                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = pair.description,
                        color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Export Target Format Selector
    Text(
        text = "TARGET EXPORT FORMAT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExportFormat.values().forEach { fmt ->
            val selected = state.exportFormat == fmt
            FilterChip(
                selected = selected,
                onClick = { viewModel.setFormat(fmt) },
                label = { Text(fmt.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    // 4. Transparent PNG/WebP -> JPG Background Color Resolver
    if (state.exportFormat == ExportFormat.JPEG && (state.hasTransparency || state.detectedInputFormat.supportsAlpha)) {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            border = BorderStroke(1.dp, Color(0xFF6366F1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Background Matte",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BACKGROUND MATTE FOR JPG CONVERSION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "JPG does not support transparent alpha. Choose the background color to automatically composite underneath transparent regions:",
                    color = Color(0xFFC7D2FE),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Color Swatches
                val bgPalette = listOf(
                    Triple(android.graphics.Color.WHITE, "Pure White", Color(0xFFFFFFFF)),
                    Triple(android.graphics.Color.BLACK, "Deep Black", Color(0xFF000000)),
                    Triple(android.graphics.Color.rgb(241, 245, 249), "Light Gray", Color(0xFFF1F5F9)),
                    Triple(android.graphics.Color.rgb(254, 243, 199), "Warm Cream", Color(0xFFFEF3C7)),
                    Triple(android.graphics.Color.rgb(15, 23, 42), "Navy Slate", Color(0xFF0F172A)),
                    Triple(android.graphics.Color.rgb(236, 253, 245), "Soft Mint", Color(0xFFECFDF5))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bgPalette.forEach { (colorVal, name, displayColor) ->
                        val isPicked = state.jpegBackgroundColor == colorVal
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPicked) Color(0xFF4338CA) else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(if (isPicked) 2.dp else 1.dp, if (isPicked) Color(0xFFA5B4FC) else Color(0xFF475569)),
                            modifier = Modifier.clickable {
                                viewModel.setJpegBackgroundColor(colorVal, name)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(displayColor)
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    color = if (isPicked) Color.White else Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Matte: ${state.jpegBackgroundName}",
                        color = Color(0xFFA5B4FC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = { viewModel.setFormat(ExportFormat.PNG) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3730A3)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Keep Alpha (Switch to PNG)", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }

    state.lastConversionReport?.let { convReport ->
        Spacer(modifier = Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (convReport.isValid) Color(0x2210B981) else Color(0x22EF4444)
            ),
            border = BorderStroke(
                1.dp,
                if (convReport.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (convReport.isValid) "✓ FORMAT & MIME VERIFIED" else "⚠ CONVERSION ISSUE DETECTED",
                        color = if (convReport.isValid) Color(0xFF34D399) else Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = convReport.detectedMimeType,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${convReport.fromFormat.shortName} → ${convReport.toFormat.displayName} | Size: ${convReport.actualBytes / 1024} KB | Magic Header: ${if (convReport.isMagicHeaderValid) "Valid" else "Invalid"} | Decode: ${if (convReport.isDecodeSafe) "Safe (${convReport.decodedWidth}×${convReport.decodedHeight})" else "Failed"}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. DPI / PPI TOOL & PHYSICAL PRINT DENSITY
    DpiPpiToolCard(viewModel = viewModel, state = state)
}

@Composable
fun DpiPpiToolCard(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    var customDpiText by remember(state.dpi) { mutableStateOf(state.customDpiInput) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    val currentW = state.workingBitmap?.width ?: state.targetWidthPx
    val currentH = state.workingBitmap?.height ?: state.targetHeightPx
    val currentDpi = state.dpi.coerceAtLeast(1)

    val printWidthInches = currentW.toFloat() / currentDpi
    val printHeightInches = currentH.toFloat() / currentDpi
    val printWidthCm = printWidthInches * 2.54f
    val printHeightCm = printHeightInches * 2.54f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111C30)),
        border = BorderStroke(1.dp, Color(0xFF0284C7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "DPI Info",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DPI / PPI DENSITY TOOL",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0369A1).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "${state.dpi} DPI",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // A: Read DPI / PPI Metadata
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Source Metadata DPI:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        val readDpi = state.originalReadDpi
                        if (readDpi != null) {
                            Text(
                                text = "$readDpi DPI (From Container EXIF / pHYs)",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "Unspecified (Default 72/96 Screen)",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (state.exportFormat) {
                            ExportFormat.JPEG -> "✓ Supported: Embedded via JPEG JFIF density & EXIF X/Y-Resolution tags"
                            ExportFormat.PNG -> "✓ Supported: Embedded via PNG pHYs chunk (pixels per metre)"
                            ExportFormat.PDF -> "✓ Supported: Rendered using standardized 72 pt/inch vector coordinate system"
                            ExportFormat.WEBP_LOSSY, ExportFormat.WEBP_LOSSLESS -> "✓ Supported: Embedded in WebP container metadata"
                            else -> "✓ Supported: Tagged in export stream metadata"
                        },
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // B: Preset Buttons (72, 96, 150, 300, 600)
            Text(
                text = "SELECT DPI PRESET",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 10.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    72 to "72",
                    96 to "96",
                    150 to "150",
                    300 to "300",
                    600 to "600"
                ).forEach { (dpiVal, label) ->
                    val isSelected = state.dpi == dpiVal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setDpi(dpiVal)
                                customDpiText = dpiVal.toString()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = when (dpiVal) {
                                    72 -> "Web"
                                    96 -> "Screen"
                                    150 -> "Draft"
                                    300 -> "Photo"
                                    600 -> "Pro"
                                    else -> ""
                                },
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // C: Custom DPI Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customDpiText,
                    onValueChange = {
                        customDpiText = it
                        viewModel.setCustomDpiText(it)
                    },
                    label = { Text("Custom DPI (10 - 2400)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Button(
                    onClick = {
                        val newDpi = (state.dpi - 50).coerceAtLeast(10)
                        viewModel.setDpi(newDpi)
                        customDpiText = newDpi.toString()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("-50", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        val newDpi = (state.dpi + 50).coerceAtMost(2400)
                        viewModel.setDpi(newDpi)
                        customDpiText = newDpi.toString()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("+50", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // D: Live Physical Print Size Computation Card
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LIVE PHYSICAL PRINT OUTPUT SIZE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Pixel Dimensions:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text("$currentW × $currentH px", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Print Size (Inches):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "${String.format("%.2f", printWidthInches)}\" × ${String.format("%.2f", printHeightInches)}\"",
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Print Size (Centimeters):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "${String.format("%.1f", printWidthCm)} cm × ${String.format("%.1f", printHeightCm)} cm",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // E: CRITICAL EDUCATIONAL EXPLANATIONS
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = BorderStroke(1.dp, Color(0xFF6366F1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Important Note",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRITICAL PRINCIPLES TO REMEMBER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. DPI metadata does not create additional image detail.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Changing the DPI number in an image's metadata (e.g. from 72 to 300) only instructs a printer how closely to pack existing pixels on paper. It does NOT invent, sharpen, or resample new pixel detail into your image.",
                        color = Color(0xFFC7D2FE),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "2. Do not confuse: Pixel Dimensions ≠ DPI ≠ Physical Print Size.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Pixel Dimensions ($currentW × $currentH px): The total digital pixel grid.\n• DPI (${state.dpi} Dots Per Inch): The physical density multiplier for printing.\n• Physical Print Size (${String.format("%.2f", printWidthInches)}\" × ${String.format("%.2f", printHeightInches)}\"): Resulting paper size = Pixels ÷ DPI.",
                        color = Color(0xFFC7D2FE),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live comparison across all presets for the active image
                    Text(
                        text = "Interactive Scale Comparison For Current Image ($currentW × $currentH px):",
                        color = Color(0xFFE0E7FF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(72, 96, 150, 300, 600).forEach { dpiOption ->
                        val wIn = currentW.toFloat() / dpiOption
                        val hIn = currentH.toFloat() / dpiOption
                        val isCurrent = state.dpi == dpiOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) Color(0xFF3730A3) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• $dpiOption DPI:",
                                color = if (isCurrent) Color.White else Color(0xFFA5B4FC),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${String.format("%.2f", wIn)}\" × ${String.format("%.2f", hIn)}\" (${String.format("%.1f", wIn * 2.54f)} × ${String.format("%.1f", hIn * 2.54f)} cm)",
                                color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassportControls(
    viewModel: UtilityViewModel,
    state: com.example.ui.viewmodel.StudioUiState,
    onCaptureCamera: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCustomPresetDialog by remember { mutableStateOf(false) }
    var customCategory by remember { mutableStateOf(IdDocumentCategory.CUSTOM) }
    var customCountryName by remember { mutableStateOf("") }
    var customDocTypeName by remember { mutableStateOf("") }
    var customWidthInput by remember { mutableStateOf("35") }
    var customHeightInput by remember { mutableStateOf("45") }
    var customDpiInput by remember { mutableStateOf("300") }
    var customMaxKbInput by remember { mutableStateOf("") }

    // Disclaimer & Compliance Notice
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Standard Dimension Template",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Standard Dimension Template",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Specifications vary across issuing agencies and consulates. Standard dimension guidelines are provided for formatting and biometric alignment. Verify specific official authority rules before submission.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 15.sp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = onCaptureCamera,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("passport_camera_capture_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Take Live Portrait Photo (Camera)",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Category Filter Tabs (ID photo, Visa, Application, Job, Exam, Document, Custom)
    Text(
        text = "DOCUMENT CATEGORY",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IdDocumentCategory.values().forEach { cat ->
            val selected = state.selectedIdCategory == cat
            FilterChip(
                selected = selected,
                onClick = { viewModel.setIdCategory(cat) },
                label = { Text(cat.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Presets Selector for Category
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "OFFICIAL SPECIFICATIONS & PRESETS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        )
        TextButton(
            onClick = { showCustomPresetDialog = !showCustomPresetDialog },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Custom Preset", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showCustomPresetDialog) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Create Custom Document Preset", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                // Category selector for custom preset
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IdDocumentCategory.values().filter { it != IdDocumentCategory.ALL }.forEach { cat ->
                        FilterChip(
                            selected = customCategory == cat,
                            onClick = { customCategory = cat },
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
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customCountryName,
                    onValueChange = { customCountryName = it },
                    label = { Text("Country / Portal / Authority Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customDocTypeName,
                    onValueChange = { customDocTypeName = it },
                    label = { Text("Document Type (e.g. Visa, Exam Photo, Job Application)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customWidthInput,
                        onValueChange = { customWidthInput = it },
                        label = { Text("Width (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = customHeightInput,
                        onValueChange = { customHeightInput = it },
                        label = { Text("Height (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customDpiInput,
                        onValueChange = { customDpiInput = it },
                        label = { Text("Target DPI") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = customMaxKbInput,
                        onValueChange = { customMaxKbInput = it },
                        label = { Text("Max Size (KB, opt.)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val w = customWidthInput.toFloatOrNull() ?: 35f
                        val h = customHeightInput.toFloatOrNull() ?: 45f
                        val d = customDpiInput.toIntOrNull() ?: 300
                        val kb = customMaxKbInput.toIntOrNull()
                        viewModel.saveCustomPassportPreset(
                            country = customCountryName,
                            docType = customDocTypeName,
                            widthMm = w,
                            heightMm = h,
                            dpi = d,
                            maxFileSizeKb = kb,
                            category = customCategory
                        )
                        showCustomPresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Custom Preset", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    val allPresets = PassportPreset.PRESETS + state.customPassportPresets
    val visiblePresets = if (state.selectedIdCategory == IdDocumentCategory.ALL) {
        allPresets
    } else {
        allPresets.filter { it.category == state.selectedIdCategory }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visiblePresets.forEach { preset ->
            val selected = state.selectedPassportPreset == preset
            FilterChip(
                selected = selected,
                onClick = { viewModel.selectPassportPreset(preset) },
                label = {
                    val sizeBadge = if (preset.maxFileSizeKb != null) " [≤${preset.maxFileSizeKb}KB]" else ""
                    Text(
                        if (preset.isCustom) "★ ${preset.country} - ${preset.documentType}$sizeBadge"
                        else "${preset.country} - ${preset.documentType}$sizeBadge"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Exact Physical Dimensions (Width, Height, Unit)
    Text(
        text = "EXACT PHYSICAL SIZE & UNITS",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PassportDimensionUnit.values().forEach { unit ->
            val selected = state.passportUnit == unit
            FilterChip(
                selected = selected,
                onClick = { viewModel.setPassportUnit(unit) },
                label = { Text(unit.symbol.uppercase()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var wText by remember(state.passportWidthPhysical, state.passportUnit) {
            mutableStateOf(String.format(java.util.Locale.US, "%.2f", state.passportWidthPhysical).trimEnd('0').trimEnd('.'))
        }
        var hText by remember(state.passportHeightPhysical, state.passportUnit) {
            mutableStateOf(String.format(java.util.Locale.US, "%.2f", state.passportHeightPhysical).trimEnd('0').trimEnd('.'))
        }

        OutlinedTextField(
            value = wText,
            onValueChange = {
                wText = it
                val parsed = it.toFloatOrNull()
                if (parsed != null && parsed > 0) {
                    viewModel.setPassportDimensions(parsed, state.passportHeightPhysical, state.passportUnit, state.passportDpi)
                }
            },
            label = { Text("Width (${state.passportUnit.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = hText,
            onValueChange = {
                hText = it
                val parsed = it.toFloatOrNull()
                if (parsed != null && parsed > 0) {
                    viewModel.setPassportDimensions(state.passportWidthPhysical, parsed, state.passportUnit, state.passportDpi)
                }
            },
            label = { Text("Height (${state.passportUnit.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Exact Pixel Size & Portal Resolution Presets
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXACT PIXEL SIZE (PORTALS)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                )
                IconButton(
                    onClick = { viewModel.setPassportLockAspectRatio(!state.passportLockAspectRatio) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (state.passportLockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Aspect Ratio",
                        tint = if (state.passportLockAspectRatio) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var pxWText by remember(state.targetWidthPx) { mutableStateOf(state.targetWidthPx.toString()) }
                var pxHText by remember(state.targetHeightPx) { mutableStateOf(state.targetHeightPx.toString()) }

                OutlinedTextField(
                    value = pxWText,
                    onValueChange = {
                        pxWText = it
                        val parsed = it.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            val newH = if (state.passportLockAspectRatio && state.targetWidthPx > 0) {
                                (parsed.toFloat() * state.targetHeightPx / state.targetWidthPx).roundToInt()
                            } else {
                                state.targetHeightPx
                            }
                            viewModel.setPassportExactPixels(parsed, newH)
                        }
                    },
                    label = { Text("Width (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = pxHText,
                    onValueChange = {
                        pxHText = it
                        val parsed = it.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            val newW = if (state.passportLockAspectRatio && state.targetHeightPx > 0) {
                                (parsed.toFloat() * state.targetWidthPx / state.targetHeightPx).roundToInt()
                            } else {
                                state.targetWidthPx
                            }
                            viewModel.setPassportExactPixels(newW, parsed)
                        }
                    },
                    label = { Text("Height (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quick Portal Pixel Sizes",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val portalSizes = listOf(
                    Triple("600×600", 600, 600),
                    Triple("413×531", 413, 531),
                    Triple("200×230", 200, 230),
                    Triple("350×450", 350, 450),
                    Triple("400×400", 400, 400),
                    Triple("295×413", 295, 413)
                )
                portalSizes.forEach { (label, w, h) ->
                    val isSelected = state.targetWidthPx == w && state.targetHeightPx == h
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPassportExactPixels(w, h, lockAspectRatio = false) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. DPI (Print Resolution) & Summary
    Text(
        text = "PRINT RESOLUTION (DPI)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(72, 96, 150, 200, 300, 400, 600).forEach { d ->
            val selected = state.passportDpi == d
            FilterChip(
                selected = selected,
                onClick = { viewModel.setPassportDpi(d) },
                label = { Text("${d} DPI") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TARGET RESOLUTION",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.targetWidthPx} × ${state.targetHeightPx} px",
                    color = Color(0xFF38BDF8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DENSITY",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.passportDpi} DPI",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 6. File Size Limit (Ceiling mode for portal compliance)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (state.passportTargetSizeKb != null) Color(0xFF38BDF8) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PORTAL FILE SIZE LIMIT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Text(
                        text = if (state.passportTargetSizeKb != null) "Strict ceiling: ≤ ${state.passportTargetSizeKb} KB" else "No file size limit (Standard export)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = state.passportTargetSizeKb != null,
                    onCheckedChange = { checked ->
                        viewModel.setPassportTargetSizeKb(if (checked) 50 else null)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }

            if (state.passportTargetSizeKb != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(20, 50, 100, 200, 240, 500).forEach { kb ->
                        val selected = state.passportTargetSizeKb == kb
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPassportTargetSizeKb(kb) },
                            label = { Text("≤ $kb KB") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7. Crop to Document Ratio
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOCUMENT CROP & RATIO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Text(
                        text = "Target ratio: ${state.targetWidthPx} : ${state.targetHeightPx}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    )
                }
                Button(
                    onClick = { viewModel.cropToPassportAspectRatio() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Crop Ratio", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Biometric Head Framing (ICAO 70–80%)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Detect face & center head height to official passport bounds", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                Switch(
                    checked = state.passportAutoBiometricCrop,
                    onCheckedChange = { viewModel.setPassportAutoBiometricCrop(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }

            // Biometric Compliance Report
            val bioReport = state.biometricComplianceReport
            if (bioReport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (bioReport.overallCompliant) Color(0xFF064E3B) else Color(0xFF451A03)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (bioReport.overallCompliant) "✓ ICAO Biometric Check: PASS" else "ℹ Biometric Framing Check",
                            color = if (bioReport.overallCompliant) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        val headStatus = if (bioReport.isHeadSizeCompliant) "OK" else "Adjust"
                        Text(
                            text = "Head: ${String.format("%.0f%%", bioReport.headHeightPercent)} ($headStatus) • Eye Level: ${String.format("%.0f%%", bioReport.eyeLevelFromBottomPercent)} • BG Uniformity: ${String.format("%.0f%%", bioReport.backgroundUniformityPercent)}",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7B. Name & Date Slate Stamp + Thin Cut Border (Exam & Official Portals)
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NAME & DATE SLATE STAMP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Text(
                        text = "Required by UPSC, SSC, NEET & JEE exam portals",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = state.passportSlateConfig.enabled,
                    onCheckedChange = { viewModel.setPassportSlateConfig(enabled = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    ),
                    modifier = Modifier.testTag("passport_name_date_stamp_switch")
                )
            }

            if (state.passportSlateConfig.enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.passportSlateConfig.applicantName,
                        onValueChange = { name -> viewModel.setPassportSlateConfig(applicantName = name) },
                        label = { Text("Candidate Full Name", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.passportSlateConfig.photoDateText,
                        onValueChange = { dateStr -> viewModel.setPassportSlateConfig(photoDateText = dateStr) },
                        label = { Text("DOP (DD-MM-YYYY)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1px Cutting Border Around Photo", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                Switch(
                    checked = state.passportAddThinBorder,
                    onCheckedChange = { viewModel.setPassportAddThinBorder(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 8. Alignment, Biometric Guides & Fine Tilt Rotation
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALIGNMENT & BIOMETRIC GUIDES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Text(
                        text = "Biometric facial bounds and leveling alignment",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = state.showBiometricGuides,
                    onCheckedChange = { viewModel.toggleBiometricGuides() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }

            if (state.showBiometricGuides) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Face Oval Contour (Head-position guide)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Switch(
                        checked = state.showFaceOvalGuide,
                        onCheckedChange = { viewModel.toggleFaceOvalGuide() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crown & Chin Bounds (70-80% height)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Switch(
                        checked = state.showHeadCrownChinGuides,
                        onCheckedChange = { viewModel.toggleCrownChinGuide() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Eye Level Guideline (Horizontal level)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Switch(
                        checked = state.showEyeLineGuide,
                        onCheckedChange = { viewModel.toggleEyeLineGuide() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vertical Center Axis (Symmetry)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Switch(
                        checked = state.showCenterAxisGuide,
                        onCheckedChange = { viewModel.toggleCenterAxisGuide() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3x3 Rule-of-Thirds Grid", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Switch(
                        checked = state.showGridGuide,
                        onCheckedChange = { viewModel.toggleGridGuide() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF38BDF8),
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Tilt Leveling Slider (Alignment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Head Alignment & Leveling",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f°", state.passportFineRotation),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.passportFineRotation != 0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.resetPassportFineRotation() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Reset 0°", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            }
            Slider(
                value = state.passportFineRotation,
                onValueChange = { viewModel.setPassportFineRotation(it) },
                valueRange = -15f..15f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF38BDF8)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 9. Background Color Replacement
    Text(
        text = "BACKGROUND COLOR REPLACEMENT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackgroundProcessor.PassportBgColor.values().forEach { bg ->
            val selected = state.passportBgColor == bg
            FilterChip(
                selected = selected,
                onClick = { viewModel.setPassportBgColor(bg) },
                label = { Text(bg.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Edge Tolerance", color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text("${state.passportTolerance.roundToInt()}", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Slider(
        value = state.passportTolerance,
        onValueChange = { viewModel.setPassportTolerance(it) },
        valueRange = 10f..80f,
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF38BDF8),
            activeTrackColor = Color(0xFF38BDF8)
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 10. Multiple Copies & Print Sheet Generator
    Text(
        text = "EXPORT DESTINATION & PRINT SHEET",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PassportExportTarget.values().forEach { target ->
            val selected = state.passportExportTarget == target
            FilterChip(
                selected = selected,
                onClick = { viewModel.setPassportExportTarget(target) },
                label = { Text(target.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (state.passportExportTarget == PassportExportTarget.PRINT_SHEET) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Paper Size", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SheetPaperPreset.ALL.forEach { paper ->
                        val selected = state.passportSheetPaper == paper
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPassportSheetPaper(paper) },
                            label = { Text(paper.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Number of Copies", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 4, 6, 8, 12, 16, 24).forEach { count ->
                        val selected = state.passportCopies == count
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPassportCopies(count) },
                            label = { Text("$count Copies") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Cutting Guide Lines", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CutLineStyle.values().forEach { style ->
                        val selected = state.passportCutLineStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPassportCutLineStyle(style) },
                            label = { Text(style.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 11. Export Format (JPG / PNG / WebP / PDF)
    Text(
        text = "EXPORT FORMAT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(ExportFormat.JPEG, ExportFormat.PNG, ExportFormat.WEBP_LOSSY, ExportFormat.PDF).forEach { fmt ->
            val selected = state.passportExportFormat == fmt
            val labelText = when (fmt) {
                ExportFormat.JPEG -> "JPG"
                ExportFormat.PNG -> "PNG"
                ExportFormat.WEBP_LOSSY -> "WebP"
                ExportFormat.PDF -> "PDF"
                else -> fmt.extension.uppercase()
            }
            FilterChip(
                selected = selected,
                onClick = { viewModel.setPassportExportFormat(fmt) },
                label = { Text(labelText) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 12. Action Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.applyPassportProcessing() },
            modifier = Modifier
                .weight(1f)
                .testTag("apply_passport_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Format Canvas", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
        }

        Button(
            onClick = {
                viewModel.exportPassportModule { result ->
                    val msg = if (result.success) {
                        val ceilingText = if (state.passportTargetSizeKb != null) " (under ${state.passportTargetSizeKb}KB limit)" else ""
                        if (state.passportExportTarget == PassportExportTarget.PRINT_SHEET)
                            "Exported Print Sheet (${state.passportCopies} copies) as ${state.passportExportFormat.name}$ceilingText"
                        else
                            "Exported ${state.targetWidthPx}×${state.targetHeightPx} px Document Photo as ${state.passportExportFormat.name}$ceilingText"
                    } else {
                        result.errorMessage ?: "Export error"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .weight(1.3f)
                .testTag("export_passport_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (state.passportExportTarget == PassportExportTarget.PRINT_SHEET) "Save Print Sheet" else "Save Document Photo",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun DocumentControls(
    viewModel: UtilityViewModel,
    state: com.example.ui.viewmodel.StudioUiState,
    onCaptureCamera: () -> Unit = {}
) {
    Button(
        onClick = onCaptureCamera,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("document_camera_scan_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6))
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Scan Document with Camera",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "DOCUMENT PHOTO & SCANNER MODES",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PassportIdDocumentEngine.DocumentProcessMode.values().forEach { mode ->
            val selected = state.passportDocumentMode == mode
            FilterChip(
                selected = selected,
                onClick = {
                    viewModel.setPassportDocumentMode(mode)
                    when (mode) {
                        PassportIdDocumentEngine.DocumentProcessMode.MAGIC_COLOR_CLEAN -> viewModel.setEnhancements(filter = FilterPreset.DOCUMENT_MAGIC)
                        PassportIdDocumentEngine.DocumentProcessMode.HIGH_CONTRAST_BW -> viewModel.setEnhancements(filter = FilterPreset.DOCUMENT_BW)
                        PassportIdDocumentEngine.DocumentProcessMode.GRAYSCALE_ARCHIVE -> viewModel.setEnhancements(filter = FilterPreset.GRAYSCALE)
                        else -> viewModel.setEnhancements(filter = FilterPreset.ORIGINAL)
                    }
                },
                label = { Text(mode.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("doc_mode_${mode.name.lowercase()}")
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.passportDocumentMode.description,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp
    )

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.applyPassportProcessing() },
            modifier = Modifier
                .weight(1f)
                .testTag("apply_document_scan_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Document Scan (${state.passportDocumentMode.label})", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 12.sp)
        }
    }
}

@Composable
fun RotateControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    RotateStraightenControls(
        viewModel = viewModel,
        state = state,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PhotoSheetControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cfg = state.sheetConfig
    val res = state.sheetResult

    // 1. COPIES
    Text(
        text = "1. COPIES (${cfg.copies} SELECTED)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))

    val copyPresets = listOf(2, 4, 6, 8, 10)
    val isCustomCopies = !copyPresets.contains(cfg.copies)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        copyPresets.forEach { count ->
            val isSelected = (!isCustomCopies && cfg.copies == count)
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetCopies(count) },
                label = { Text("$count Copies") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("copies_${count}_chip")
            )
        }

        FilterChip(
            selected = isCustomCopies,
            onClick = {
                val current = cfg.customCopiesInput.toIntOrNull() ?: 12
                viewModel.setSheetCopies(current, cfg.customCopiesInput)
            },
            label = { Text("Custom") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF38BDF8),
                selectedLabelColor = Color(0xFF0F172A),
                containerColor = Color(0xFF0F172A),
                labelColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier.testTag("copies_custom_chip")
        )
    }

    if (isCustomCopies) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    val next = (cfg.copies - 1).coerceAtLeast(1)
                    viewModel.setSheetCopies(next)
                },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .size(44.dp)
                    .testTag("copies_decrement_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Copies", tint = Color(0xFF38BDF8))
            }

            OutlinedTextField(
                value = cfg.customCopiesInput,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(3)
                    val parsed = filtered.toIntOrNull() ?: 1
                    viewModel.setSheetCopies(parsed, filtered)
                },
                label = { Text("Number of Copies", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_copies_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            IconButton(
                onClick = {
                    val next = (cfg.copies + 1).coerceAtMost(100)
                    viewModel.setSheetCopies(next)
                },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .size(44.dp)
                    .testTag("copies_increment_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Copies", tint = Color(0xFF38BDF8))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. PAPER SIZE
    Text(
        text = "2. PAPER SIZE (${cfg.paperPreset.name})",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetPaperPreset.ALL.forEach { paper ->
            val selected = cfg.paperPreset == paper
            FilterChip(
                selected = selected,
                onClick = { viewModel.setSheetPaper(paper) },
                label = { Text(paper.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.testTag("paper_${paper.name.take(4)}_chip")
            )
        }
    }

    // Custom dimensions if Custom Paper is selected
    if (cfg.paperPreset.isCustom) {
        Spacer(modifier = Modifier.height(8.dp))
        var customWInput by remember(cfg.customWidthInches) { mutableStateOf(cfg.customWidthInches.toString()) }
        var customHInput by remember(cfg.customHeightInches) { mutableStateOf(cfg.customHeightInches.toString()) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customWInput,
                onValueChange = {
                    customWInput = it
                    it.toFloatOrNull()?.let { w ->
                        viewModel.setSheetCustomDimensions(w, cfg.customHeightInches)
                    }
                },
                label = { Text("Width (in)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = customHInput,
                onValueChange = {
                    customHInput = it
                    it.toFloatOrNull()?.let { h ->
                        viewModel.setSheetCustomDimensions(cfg.customWidthInches, h)
                    }
                },
                label = { Text("Height (in)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }

    // Orientation selector
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetOrientation.values().forEach { orient ->
            val isSelected = cfg.orientation == orient
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetOrientation(orient) },
                label = { Text(orient.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2B. TRUE PHYSICAL PHOTO SIZE MODE & AUTO-FILL MAX COPIES
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TRUE PHYSICAL PHOTO SIZE MODE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    )
                    Text(
                        text = if (cfg.useExactPhotoPhysicalSize)
                            "Locked to exact ${cfg.targetPhotoWidthMm}×${cfg.targetPhotoHeightMm} mm per photo"
                        else
                            "Auto-fit grid to fill paper (enable to lock exact mm size)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = cfg.useExactPhotoPhysicalSize,
                    onCheckedChange = { enabled ->
                        viewModel.setSheetExactPhotoSizeMode(
                            enabled = enabled,
                            photoWidthMm = cfg.targetPhotoWidthMm,
                            photoHeightMm = cfg.targetPhotoHeightMm,
                            autoFillMax = cfg.autoFillMaxCopies
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    ),
                    modifier = Modifier.testTag("sheet_exact_physical_size_switch")
                )
            }

            if (cfg.useExactPhotoPhysicalSize) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("35×45 mm (ICAO)", 35f, 45f),
                        Triple("50.8×50.8 mm (2×2 in)", 50.8f, 50.8f),
                        Triple("25×35 mm (PAN/Mini)", 25f, 35f),
                        Triple("33×48 mm (Visa)", 33f, 48f)
                    ).forEach { (label, wMm, hMm) ->
                        val isSel = kotlin.math.abs(cfg.targetPhotoWidthMm - wMm) < 0.5f &&
                                kotlin.math.abs(cfg.targetPhotoHeightMm - hMm) < 0.5f
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setSheetExactPhotoSizeMode(true, wMm, hMm, cfg.autoFillMaxCopies) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Fill Max Copies That Fit Paper", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Switch(
                        checked = cfg.autoFillMaxCopies,
                        onCheckedChange = { autoMax ->
                            viewModel.setSheetExactPhotoSizeMode(
                                enabled = cfg.useExactPhotoPhysicalSize,
                                photoWidthMm = cfg.targetPhotoWidthMm,
                                photoHeightMm = cfg.targetPhotoHeightMm,
                                autoFillMax = autoMax
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF34D399),
                            checkedTrackColor = Color(0xFF065F46)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Print 10mm Scale Calibration Ruler Footer", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                Switch(
                    checked = cfg.showCalibrationFooter,
                    onCheckedChange = { viewModel.setSheetShowCalibrationFooter(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF38BDF8),
                        checkedTrackColor = Color(0xFF0284C7)
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. CONTROLS: MARGINS & SPACING
    Text(
        text = "3. MARGINS & SPACING",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Margins
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Outer Margin:", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text("${cfg.marginMm.toInt()} mm (${String.format("%.2f", cfg.marginMm / 25.4f)} in)", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(0f to "None (0mm)", 5f to "Narrow (5mm)", 10f to "Standard (10mm)", 15f to "Wide (15mm)", 20f to "20mm").forEach { (m, label) ->
            FilterChip(
                selected = kotlin.math.abs(cfg.marginMm - m) < 0.5f,
                onClick = { viewModel.setSheetMarginMm(m) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
    Slider(
        value = cfg.marginMm,
        onValueChange = { viewModel.setSheetMarginMm(it) },
        valueRange = 0f..30f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
        modifier = Modifier.testTag("sheet_margin_slider")
    )

    // Spacing between photos
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Photo Spacing (Gap):", color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text("${cfg.spacingMm.toInt()} mm", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(0f to "None (0mm)", 2f to "Tight (2mm)", 4f to "Standard (4mm)", 8f to "Loose (8mm)", 12f to "12mm").forEach { (s, label) ->
            FilterChip(
                selected = kotlin.math.abs(cfg.spacingMm - s) < 0.5f,
                onClick = { viewModel.setSheetSpacingMm(s) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
    Slider(
        value = cfg.spacingMm,
        onValueChange = { viewModel.setSheetSpacingMm(it) },
        valueRange = 0f..20f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8)),
        modifier = Modifier.testTag("sheet_spacing_slider")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 4. ALIGNMENT & ROTATION
    Text(
        text = "4. ALIGNMENT & PHOTO ROTATION",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))

    // Alignment
    Text("Sheet Alignment:", color = Color(0xFF94A3B8), fontSize = 12.sp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SheetAlignment.values().forEach { align ->
            val isSelected = cfg.alignment == align
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetAlignment(align) },
                label = { Text(align.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Rotation
    Text("Photo Rotation on Paper:", color = Color(0xFF94A3B8), fontSize = 12.sp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PhotoSheetRotation.values().forEach { rot ->
            val isSelected = cfg.rotation == rot
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetRotation(rot) },
                label = { Text(rot.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. CROP MARKS & CUTTING LINES
    Text(
        text = "5. CROP MARKS & CUTTING GUIDES",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CutLineStyle.values().forEach { style ->
            val isSelected = cfg.cutLineStyle == style
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetCutLineStyle(style) },
                label = { Text(style.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.testTag("crop_mark_${style.name.lowercase()}_chip")
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 6. RESOLUTION (DPI)
    Text(
        text = "6. PRINT RESOLUTION (DPI)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(150 to "150 DPI (Draft)", 300 to "300 DPI (Standard)", 600 to "600 DPI (HD)").forEach { (dpi, label) ->
            val isSelected = cfg.dpi == dpi
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetDpi(dpi) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFCBD5E1)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7. PRINT SHEET SUMMARY & SPECS CARD
    if (res != null) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PRINT SPECIFICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Photos Placed:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${res.totalCopies} copies (${res.cols} cols × ${res.rows} rows)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Single Photo Print Size:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${String.format("%.1f", res.photoWidthMm)} × ${String.format("%.1f", res.photoHeightMm)} mm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paper Output Dimensions:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${String.format("%.1f", res.paperWidthMm)} × ${String.format("%.1f", res.paperHeightMm)} mm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Render Resolution:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("${res.bitmap.width} × ${res.bitmap.height} px @ ${res.dpi} DPI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    // 8. EXPORT SECTION (JPG, PNG, PDF)
    Text(
        text = "8. EXPORT FORMAT (JPG, PNG, PDF)",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            ExportFormat.PDF to "PDF Document",
            ExportFormat.JPEG to "JPG Image",
            ExportFormat.PNG to "PNG Image"
        ).forEach { (fmt, label) ->
            val isSelected = state.sheetExportFormat == fmt
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.setSheetExportFormat(fmt) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF38BDF8),
                    selectedLabelColor = Color(0xFF0F172A),
                    containerColor = Color(0xFF0F172A),
                    labelColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_format_${fmt.name.lowercase()}_chip")
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Primary Action Button
    Button(
        onClick = {
            viewModel.exportPhotoSheet { result ->
                scope.launch {
                    if (result.success) {
                        Toast.makeText(context, "Exported ${cfg.copies}-Copy Sheet as ${result.format.displayName} (${result.width}×${result.height} px)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, result.errorMessage ?: "Export failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("export_photo_sheet_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
        shape = RoundedCornerShape(10.dp)
    ) {
        val icon = if (state.sheetExportFormat == ExportFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Save
        Icon(icon, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        val count = res?.totalCopies ?: cfg.copies
        val fmtName = state.sheetExportFormat.displayName
        Text(
            text = "Export $count-Copy Sheet ($fmtName)",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Share Button
    Button(
        onClick = {
            viewModel.exportPhotoSheet { result ->
                if (result.success && result.outputFile != null) {
                    ExportEngine.shareImage(context, result.outputFile, result.format.mimeType)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("share_photo_sheet_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Share Photo Sheet",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun ExifControls(viewModel: UtilityViewModel, state: com.example.ui.viewmodel.StudioUiState) {
    ExifPrivacyControls(viewModel = viewModel, state = state)
}

private fun gcd(a: Int, b: Int): Int {
    var num1 = kotlin.math.abs(a)
    var num2 = kotlin.math.abs(b)
    while (num2 != 0) {
        val temp = num2
        num2 = num1 % num2
        num1 = temp
    }
    return if (num1 == 0) 1 else num1
}
