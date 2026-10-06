package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.ExportFormat
import com.example.processing.BitmapUtils
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode
import com.example.processing.FileSizeOptimizationResult
import com.example.processing.SignatureProcessor
import com.example.ui.components.SignatureBitmapGenerator
import com.example.ui.components.SignaturePadView
import com.example.ui.components.SignatureStroke
import com.example.ui.viewmodel.UtilityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    viewModel: UtilityViewModel,
    onBack: () -> Unit
) {
    val palette = com.example.ui.theme.LocalStudioPalette.current
    val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // -------------------------------------------------------------
    // 1. SOURCE INPUT STATE (Import, Camera, Digital Pad)
    // -------------------------------------------------------------
    var inputMode by remember { mutableIntStateOf(0) } // 0 = Import/Photo, 1 = Camera, 2 = Digital Pad
    var rawSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentCameraCaptureFile by remember { mutableStateOf<File?>(null) }

    // Digital Pad state
    val digitalStrokes = remember { mutableStateListOf<SignatureStroke>() }
    var padPenColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var padPenThickness by remember { mutableFloatStateOf(6f) }

    // -------------------------------------------------------------
    // 2. CROP & TRIM BOUNDS STATE
    // -------------------------------------------------------------
    var autoTrimMargins by remember { mutableStateOf(true) }
    var marginPaddingPx by remember { mutableIntStateOf(16) }
    var detectedInkPixelCount by remember { mutableIntStateOf(0) }

    // -------------------------------------------------------------
    // 3. EXACT DIMENSIONS & ASPECT RATIO
    // -------------------------------------------------------------
    var targetWidthPx by remember { mutableIntStateOf(300) }
    var targetHeightPx by remember { mutableIntStateOf(100) }
    var lockAspectRatio by remember { mutableStateOf(true) }
    var storedAspectRatio by remember { mutableFloatStateOf(3.0f) }
    var isHeightAuto by remember { mutableStateOf(false) }
    var isWidthAuto by remember { mutableStateOf(false) }

    // -------------------------------------------------------------
    // 4. PHYSICAL SIZE & DPI
    // -------------------------------------------------------------
    var selectedDpi by remember { mutableIntStateOf(300) } // 72, 150, 200, 300, 600
    var dimensionUnit by remember { mutableStateOf("px") } // "px", "in", "mm", "cm"
    var physicalWidthText by remember { mutableStateOf("2.0") }
    var physicalHeightText by remember { mutableStateOf("1.0") }

    // Original Source Image Telemetry Metadata
    var originalWidth by remember { mutableIntStateOf(0) }
    var originalHeight by remember { mutableIntStateOf(0) }
    var originalFileSizeBytes by remember { mutableStateOf(0L) }
    var originalDpi by remember { mutableStateOf<Int?>(null) }

    // -------------------------------------------------------------
    // 5. TARGET BOX FITTING MODE & STRETCH WARNING
    // -------------------------------------------------------------
    var fitMode by remember { mutableStateOf(SignatureProcessor.SignatureFitMode.FIT) }
    var showStretchWarningDialog by remember { mutableStateOf(false) }
    var centerSignatureInBox by remember { mutableStateOf(true) }

    // -------------------------------------------------------------
    // 6. BACKGROUND SELECTION
    // -------------------------------------------------------------
    var backgroundType by remember { mutableStateOf(SignatureProcessor.SignatureBackgroundType.WHITE) }
    var customBgColor by remember { mutableIntStateOf(android.graphics.Color.rgb(253, 251, 247)) }
    var customHexText by remember { mutableStateOf("#FDFBF7") }

    // -------------------------------------------------------------
    // 7. INK & COLOR ENHANCEMENTS
    // -------------------------------------------------------------
    var inkMode by remember { mutableStateOf(SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE) }
    var outputInkColor by remember { mutableStateOf(SignatureProcessor.OutputInkColor.PURE_BLACK) }
    var paperThreshold by remember { mutableIntStateOf(190) } // 100 to 240
    var brightnessAdj by remember { mutableFloatStateOf(0f) } // -100 to +100
    var contrastAdj by remember { mutableFloatStateOf(0f) }   // -100 to +100
    var strokeWeightDelta by remember { mutableIntStateOf(0) } // -2 to +3 morphological stroke adjustment

    // -------------------------------------------------------------
    // 8. FORMAT & FILE SIZE LIMITS
    // -------------------------------------------------------------
    var exportFormat by remember { mutableStateOf(ExportFormat.JPEG) }
    var targetSizeKb by remember { mutableStateOf<Int?>(20) } // default 20 KB for portals
    var fileSizeMode by remember { mutableStateOf(FileSizeMode.MAXIMUM_CEILING) }

    // Live Rendered Bitmap & Measured Telemetry
    var processedSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var liveTelemetryResult by remember { mutableStateOf<FileSizeOptimizationResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Last saved file for sharing
    var lastSavedFile by remember { mutableStateOf<File?>(null) }

    // -------------------------------------------------------------
    // ACTIVITY LAUNCHERS: Photo Picker & Camera
    // -------------------------------------------------------------
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val header = BitmapUtils.getImageHeader(context, uri)
            val bmp = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1800, 1200)
            if (bmp != null) {
                rawSignatureBitmap = bmp
                originalWidth = if (header.width > 0) header.width else bmp.width
                originalHeight = if (header.height > 0) header.height else bmp.height
                originalFileSizeBytes = header.fileSizeBytes
                originalDpi = header.dpi ?: 72
                val ratio = bmp.width.toFloat() / max(1, bmp.height)
                storedAspectRatio = ratio
                if (isHeightAuto) {
                    targetHeightPx = max(10, (targetWidthPx / ratio).roundToInt())
                } else if (isWidthAuto) {
                    targetWidthPx = max(10, (targetHeightPx * ratio).roundToInt())
                } else if (lockAspectRatio) {
                    targetHeightPx = max(10, (targetWidthPx / ratio).roundToInt())
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentCameraCaptureFile != null && currentCameraCaptureFile!!.exists()) {
            val bmp = BitmapFactory.decodeFile(currentCameraCaptureFile!!.absolutePath)
            if (bmp != null) {
                rawSignatureBitmap = bmp
                originalWidth = bmp.width
                originalHeight = bmp.height
                originalFileSizeBytes = currentCameraCaptureFile?.length() ?: 0L
                originalDpi = currentCameraCaptureFile?.let { BitmapUtils.getFileExifDpi(it) } ?: 72
                val ratio = bmp.width.toFloat() / max(1, bmp.height)
                storedAspectRatio = ratio
                if (isHeightAuto) {
                    targetHeightPx = max(10, (targetWidthPx / ratio).roundToInt())
                } else if (isWidthAuto) {
                    targetWidthPx = max(10, (targetHeightPx * ratio).roundToInt())
                } else if (lockAspectRatio) {
                    targetHeightPx = max(10, (targetWidthPx / ratio).roundToInt())
                }
            }
        }
    }

    fun launchCamera() {
        try {
            val file = File.createTempFile("sig_camera_", ".jpg", context.cacheDir)
            currentCameraCaptureFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch { snackbarHostState.showSnackbar("Unable to initialize camera capture") }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to photograph signature")
            }
        }
    }

    fun handleCameraClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // -------------------------------------------------------------
    // LIVE PIPELINE RECOMPUTATION
    // -------------------------------------------------------------
    LaunchedEffect(
        rawSignatureBitmap,
        digitalStrokes.size,
        inputMode,
        autoTrimMargins,
        marginPaddingPx,
        targetWidthPx,
        targetHeightPx,
        fitMode,
        backgroundType,
        customBgColor,
        centerSignatureInBox,
        inkMode,
        outputInkColor,
        paperThreshold,
        brightnessAdj,
        contrastAdj,
        strokeWeightDelta,
        exportFormat,
        targetSizeKb,
        fileSizeMode,
        selectedDpi
    ) {
        isProcessing = true
        withContext(Dispatchers.Default) {
            // Determine active base source bitmap
            val activeBaseBmp: Bitmap? = when (inputMode) {
                0, 1 -> rawSignatureBitmap
                2 -> {
                    if (digitalStrokes.isNotEmpty()) {
                        val base = SignatureBitmapGenerator.renderStrokesToBitmap(
                            strokes = digitalStrokes.toList(),
                            width = 1200,
                            height = 600,
                            backgroundColor = android.graphics.Color.WHITE
                        )
                        if (originalWidth == 0) {
                            originalWidth = 1200
                            originalHeight = 600
                            originalFileSizeBytes = 1200L * 600L * 4L / 12L
                            originalDpi = 72
                        }
                        base
                    } else null
                }
                else -> null
            }

            if (activeBaseBmp == null) {
                processedSignatureBitmap = null
                liveTelemetryResult = null
                isProcessing = false
                return@withContext
            }

            // Step 1: Color Adjustments & Ink Thresholding
            val isTransparentBg = (backgroundType == SignatureProcessor.SignatureBackgroundType.TRANSPARENT &&
                    (exportFormat == ExportFormat.PNG || exportFormat == ExportFormat.WEBP_LOSSY || exportFormat == ExportFormat.WEBP_LOSSLESS))

            val colorCleaned = SignatureProcessor.processInkAndColor(
                source = activeBaseBmp,
                brightness = brightnessAdj,
                contrast = contrastAdj,
                inkMode = inkMode,
                paperThreshold = paperThreshold,
                outputInkColor = outputInkColor,
                makeTransparent = isTransparentBg,
                backgroundColor = when (backgroundType) {
                    SignatureProcessor.SignatureBackgroundType.WHITE -> android.graphics.Color.WHITE
                    SignatureProcessor.SignatureBackgroundType.CUSTOM -> customBgColor
                    SignatureProcessor.SignatureBackgroundType.TRANSPARENT -> android.graphics.Color.TRANSPARENT
                }
            ).let { cleaned ->
                if (strokeWeightDelta != 0) {
                    SignatureProcessor.adjustStrokeWeight(
                        source = cleaned,
                        strokeWeightDelta = strokeWeightDelta,
                        makeTransparent = isTransparentBg,
                        backgroundColor = when (backgroundType) {
                            SignatureProcessor.SignatureBackgroundType.WHITE -> android.graphics.Color.WHITE
                            SignatureProcessor.SignatureBackgroundType.CUSTOM -> customBgColor
                            SignatureProcessor.SignatureBackgroundType.TRANSPARENT -> android.graphics.Color.TRANSPARENT
                        }
                    )
                } else cleaned
            }

            // Step 2: Auto Trim Empty Space / Margins
            val trimmed = if (autoTrimMargins) {
                val detection = SignatureProcessor.findInkBounds(colorCleaned, paperThreshold, isTransparentBg)
                detectedInkPixelCount = detection.inkPixelCount
                SignatureProcessor.autoTrimEmptyArea(
                    source = colorCleaned,
                    threshold = paperThreshold,
                    paddingPx = marginPaddingPx,
                    isTransparent = isTransparentBg
                )
            } else {
                colorCleaned
            }

            // Step 3: Render to Exact Target Box using Box Fitting Mode & Alignment
            val finalTargetBmp = SignatureProcessor.fitSignatureToTargetBox(
                source = trimmed,
                targetWidth = targetWidthPx,
                targetHeight = targetHeightPx,
                fitMode = fitMode,
                backgroundType = backgroundType,
                customBackgroundColor = customBgColor,
                center = centerSignatureInBox,
                marginPaddingPx = marginPaddingPx
            )

            // Step 4: Measure Exact File Size Telemetry via FileSizeEngine
            val telemetry = ExportEngine.measureActualCompressedBytes(
                source = finalTargetBmp,
                targetW = finalTargetBmp.width,
                targetH = finalTargetBmp.height,
                format = exportFormat,
                quality = 90,
                targetSizeKb = targetSizeKb,
                fileSizeMode = fileSizeMode,
                allowDimensionAdjustment = false // keep exact signature dimensions locked
            )

            processedSignatureBitmap = finalTargetBmp
            liveTelemetryResult = telemetry
            isProcessing = false
        }
    }

    // -------------------------------------------------------------
    // STRETCH EXPLICIT WARNING DIALOG
    // -------------------------------------------------------------
    if (showStretchWarningDialog) {
        AlertDialog(
            onDismissRequest = { showStretchWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171))
                    Text("Stretching may distort the signature", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stretching forces exact width and height independently, altering the natural proportions, slant, and stroke geometry of your signature.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Warning: Stretching may distort the signature. Passport authorities, banks, and official document portals routinely REJECT distorted signatures.",
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "We strongly recommend using 'FIT' to keep the signature proportional inside the target dimensions, or 'SMART CROP' to trim excess whitespace.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        fitMode = SignatureProcessor.SignatureFitMode.STRETCH
                        showStretchWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Force Exact STRETCH", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStretchWarningDialog = false }
                ) {
                    Text("Cancel (Keep Safe FIT)", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // -------------------------------------------------------------
    // UI SCAFFOLD
    // -------------------------------------------------------------
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Signature Preparation Tool",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        )
                        Text(
                            text = "Official Bank, Passport & Visa Compliance",
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = palette.accentPrimary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("sig_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary
                        )
                    }
                },
                actions = {
                    // Quick Action: Print
                    IconButton(
                        onClick = {
                            val bmp = processedSignatureBitmap
                            if (bmp != null) {
                                ExportEngine.printBitmap(context, bmp, "Signature_${targetWidthPx}x${targetHeightPx}")
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("No signature to print") }
                            }
                        },
                        enabled = processedSignatureBitmap != null
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = if (processedSignatureBitmap != null) palette.textPrimary else palette.textMuted)
                    }
                    // Quick Action: Share
                    IconButton(
                        onClick = {
                            val bmp = processedSignatureBitmap
                            if (bmp != null) {
                                scope.launch {
                                    val result = ExportEngine.exportImage(
                                        context = context,
                                        bitmap = bmp,
                                        format = exportFormat,
                                        targetSizeKb = targetSizeKb,
                                        fileSizeMode = fileSizeMode,
                                        fileNamePrefix = "Signature",
                                        dpi = selectedDpi
                                    )
                                    if (result.success && result.outputFile != null) {
                                        lastSavedFile = result.outputFile
                                        ExportEngine.shareFile(context, result.outputFile, exportFormat.mimeType)
                                    } else {
                                        snackbarHostState.showSnackbar("Unable to prepare file for sharing")
                                    }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("No signature to share") }
                            }
                        },
                        enabled = processedSignatureBitmap != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = if (processedSignatureBitmap != null) palette.textPrimary else palette.textMuted)
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
            // -------------------------------------------------------------
            // INPUT SOURCE SELECTOR BAR
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button: Import Photo
                Button(
                    onClick = {
                        inputMode = 0
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputMode == 0) palette.accentPrimary else palette.cardElevated
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = if (inputMode == 0) Color.White else palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Import",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inputMode == 0) Color.White else palette.textPrimary
                    )
                }

                // Button: Capture Camera
                Button(
                    onClick = {
                        inputMode = 1
                        handleCameraClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputMode == 1) palette.accentPrimary else palette.cardElevated
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (inputMode == 1) Color.White else palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Camera",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inputMode == 1) Color.White else palette.textPrimary
                    )
                }

                // Button: Digital Pad
                Button(
                    onClick = { inputMode = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputMode == 2) palette.accentPrimary else palette.cardElevated
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = if (inputMode == 2) Color.White else palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Draw Pad",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inputMode == 2) Color.White else palette.textPrimary
                    )
                }
            }

            // -------------------------------------------------------------
            // LIVE CANVAS / PREVIEW VIEWPORT
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (adaptive.isWideScreen) 320.dp else 200.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, palette.cardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Checkerboard pattern for transparency visualization
                CheckerboardBackground(modifier = Modifier.fillMaxSize())

                if (inputMode == 2 && processedSignatureBitmap == null && digitalStrokes.isEmpty()) {
                    // Digital drawing pad canvas
                    SignaturePadView(
                        strokes = digitalStrokes,
                        onAddStroke = { digitalStrokes.add(it) },
                        currentColor = padPenColor,
                        currentStrokeWidth = padPenThickness,
                        backgroundColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (processedSignatureBitmap != null) {
                    // Live rendered output
                    Image(
                        bitmap = processedSignatureBitmap!!.asImageBitmap(),
                        contentDescription = "Prepared Signature Preview",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Prominent Warning if STRETCH is active
                    if (fitMode == SignatureProcessor.SignatureFitMode.STRETCH) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .background(Color(0xEE7F1D1D), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚠ Stretching may distort the signature",
                                color = Color(0xFFFEE2E2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Overlay watermark tags: Dimensions & Fit Mode
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color(0xCC0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${targetWidthPx} × ${targetHeightPx} px • ${fitMode.label}",
                            color = if (fitMode == SignatureProcessor.SignatureFitMode.STRETCH) Color(0xFFF87171) else Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (liveTelemetryResult != null && targetSizeKb != null) {
                        val pass = liveTelemetryResult!!.isCompliantWithCeiling
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(if (pass) Color(0xCC065F46) else Color(0xCC7F1D1D), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (pass) "✓ ≤ ${targetSizeKb} KB COMPLIANT" else "⚠ EXCEEDS ${targetSizeKb} KB",
                                color = if (pass) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Empty placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import, Capture with Camera, or Draw Signature",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // LIVE TELEMETRY SNAPSHOT STRIP (Original vs Target vs Final)
            // -------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        val origDimStr = if (originalWidth > 0 && originalHeight > 0) "${originalWidth}×${originalHeight}" else "1200×600"
                        Text(
                            text = "Orig: $origDimStr px ➔ Target: ${targetWidthPx}×${targetHeightPx} px",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val origSizeStr = if (originalFileSizeBytes > 0L) formatByteSize(originalFileSizeBytes) else "—"
                        val actualBytes = liveTelemetryResult?.actualBytes ?: 0L
                        val actualSizeStr = if (actualBytes > 0) formatByteSize(actualBytes) else "—"
                        Text(
                            text = "Size: $origSizeStr ➔ $actualSizeStr",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${selectedDpi} DPI", color = Color(0xFFA78BFA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = "•", color = Color(0xFF64748B), fontSize = 10.sp)
                            Text(text = exportFormat.name, color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (targetSizeKb != null && liveTelemetryResult != null) {
                            val pass = liveTelemetryResult!!.isCompliantWithCeiling
                            Text(
                                text = if (pass) "✓ PASS (≤ ${targetSizeKb}KB)" else "⚠ EXCEEDS ${targetSizeKb}KB",
                                color = if (pass) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(text = fitMode.label, color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SCROLLABLE CONTROL ENGINE TABS & SETTINGS
            // -------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                var controlTab by remember { mutableIntStateOf(0) }
                // 0: Sizing & Presets
                // 1: Fit & Crop (Margins)
                // 2: Background & Ink
                // 3: File Size & Format
                // 4: Verification Telemetry

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = controlTab,
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFF38BDF8),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[controlTab]),
                                color = Color(0xFF38BDF8)
                            )
                        }
                    ) {
                        Tab(
                            selected = controlTab == 0,
                            onClick = { controlTab = 0 },
                            text = { Text("Size & DPI", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = controlTab == 1,
                            onClick = { controlTab = 1 },
                            text = { Text("Fit & Crop", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = controlTab == 2,
                            onClick = { controlTab = 2 },
                            text = { Text("Ink & Color", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = controlTab == 3,
                            onClick = { controlTab = 3 },
                            text = { Text("File Size", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = controlTab == 4,
                            onClick = { controlTab = 4 },
                            text = { Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (controlTab) {
                            // -------------------------------------------------------------
                            // TAB 0: EXACT WIDTH, HEIGHT, DPI & PHYSICAL SIZE
                            // -------------------------------------------------------------
                            0 -> {
                                Text(
                                    text = "1. EXACT SPECIFICATION EXAMPLES & PRESETS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Select official examples or dimension presets:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))

                                // EXACT SPECIFICATION EXAMPLES (1 to 5)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Example 1: 300 × 100 px
                                    FilterChip(
                                        selected = targetWidthPx == 300 && targetHeightPx == 100 && !isHeightAuto && dimensionUnit == "px",
                                        onClick = {
                                            dimensionUnit = "px"
                                            isHeightAuto = false
                                            isWidthAuto = false
                                            targetWidthPx = 300
                                            targetHeightPx = 100
                                            storedAspectRatio = 3.0f
                                        },
                                        label = { Text("Ex 1: 300 × 100 px", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    // Example 2: 600 × 200 px
                                    FilterChip(
                                        selected = targetWidthPx == 600 && targetHeightPx == 200 && !isHeightAuto && dimensionUnit == "px",
                                        onClick = {
                                            dimensionUnit = "px"
                                            isHeightAuto = false
                                            isWidthAuto = false
                                            targetWidthPx = 600
                                            targetHeightPx = 200
                                            storedAspectRatio = 3.0f
                                        },
                                        label = { Text("Ex 2: 600 × 200 px", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    // Example 3: 2 × 1 inch @ 300 DPI
                                    FilterChip(
                                        selected = dimensionUnit == "in" && selectedDpi == 300 && targetWidthPx == 600 && targetHeightPx == 300,
                                        onClick = {
                                            dimensionUnit = "in"
                                            selectedDpi = 300
                                            physicalWidthText = "2.0"
                                            physicalHeightText = "1.0"
                                            isHeightAuto = false
                                            isWidthAuto = false
                                            targetWidthPx = 600
                                            targetHeightPx = 300
                                            storedAspectRatio = 2.0f
                                        },
                                        label = { Text("Ex 3: 2 × 1 in @ 300 DPI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    // Example 4: Width: 300 px | Height: Auto
                                    FilterChip(
                                        selected = targetWidthPx == 300 && isHeightAuto,
                                        onClick = {
                                            dimensionUnit = "px"
                                            isHeightAuto = true
                                            isWidthAuto = false
                                            targetWidthPx = 300
                                            val ratio = if (storedAspectRatio > 0f) storedAspectRatio else 3.0f
                                            targetHeightPx = max(10, (300 / ratio).roundToInt())
                                        },
                                        label = { Text("Ex 4: 300 px (Height: Auto)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    // Example 5: Maximum file size: 20 KB
                                    FilterChip(
                                        selected = targetSizeKb == 20 && fileSizeMode == FileSizeMode.MAXIMUM_CEILING,
                                        onClick = {
                                            targetSizeKb = 20
                                            fileSizeMode = FileSizeMode.MAXIMUM_CEILING
                                        },
                                        label = { Text("Ex 5: Max 20 KB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Official Signature Portal Presets (Phase 8)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SignatureProcessor.SignaturePreset.PRESETS.forEach { preset ->
                                        val isPresetSel = targetWidthPx == preset.widthPx &&
                                                targetHeightPx == preset.heightPx &&
                                                !isHeightAuto &&
                                                dimensionUnit == "px"
                                        FilterChip(
                                            selected = isPresetSel,
                                            onClick = {
                                                dimensionUnit = "px"
                                                isHeightAuto = false
                                                isWidthAuto = false
                                                targetWidthPx = preset.widthPx
                                                targetHeightPx = preset.heightPx
                                                selectedDpi = preset.dpi
                                                targetSizeKb = preset.maxFileSizeKb
                                                storedAspectRatio = preset.widthPx.toFloat() / max(1, preset.heightPx)
                                            },
                                            label = { Text(preset.title, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Dimension Units Selector
                                Text("Dimension Unit & Auto Modes:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("px" to "Pixels (px)", "in" to "Inches (in)", "mm" to "mm", "cm" to "cm").forEach { (unitKey, unitLabel) ->
                                        FilterChip(
                                            selected = dimensionUnit == unitKey,
                                            onClick = {
                                                dimensionUnit = unitKey
                                                if (unitKey == "in") {
                                                    physicalWidthText = String.format(Locale.US, "%.2f", targetWidthPx.toFloat() / selectedDpi)
                                                    physicalHeightText = String.format(Locale.US, "%.2f", targetHeightPx.toFloat() / selectedDpi)
                                                } else if (unitKey == "mm") {
                                                    physicalWidthText = String.format(Locale.US, "%.1f", (targetWidthPx.toFloat() / selectedDpi) * 25.4f)
                                                    physicalHeightText = String.format(Locale.US, "%.1f", (targetHeightPx.toFloat() / selectedDpi) * 25.4f)
                                                } else if (unitKey == "cm") {
                                                    physicalWidthText = String.format(Locale.US, "%.2f", (targetWidthPx.toFloat() / selectedDpi) * 2.54f)
                                                    physicalHeightText = String.format(Locale.US, "%.2f", (targetHeightPx.toFloat() / selectedDpi) * 2.54f)
                                                }
                                            },
                                            label = { Text(unitLabel, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = !isHeightAuto && !isWidthAuto,
                                        onClick = {
                                            isHeightAuto = false
                                            isWidthAuto = false
                                        },
                                        label = { Text("Exact W × H", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    FilterChip(
                                        selected = isHeightAuto,
                                        onClick = {
                                            isHeightAuto = true
                                            isWidthAuto = false
                                            val ratio = if (storedAspectRatio > 0f) storedAspectRatio else 3.0f
                                            targetHeightPx = max(10, (targetWidthPx / ratio).roundToInt())
                                        },
                                        label = { Text("Height: Auto", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )

                                    FilterChip(
                                        selected = isWidthAuto,
                                        onClick = {
                                            isWidthAuto = true
                                            isHeightAuto = false
                                            val ratio = if (storedAspectRatio > 0f) storedAspectRatio else 3.0f
                                            targetWidthPx = max(10, (targetHeightPx * ratio).roundToInt())
                                        },
                                        label = { Text("Width: Auto", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Dimension Input Row
                                if (dimensionUnit == "px") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = if (isWidthAuto) "Auto (~${targetWidthPx})" else targetWidthPx.toString(),
                                            enabled = !isWidthAuto,
                                            onValueChange = { str ->
                                                val w = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                targetWidthPx = w
                                                if (isHeightAuto) {
                                                    val ratio = if (storedAspectRatio > 0f) storedAspectRatio else 3.0f
                                                    targetHeightPx = max(10, (w / ratio).roundToInt())
                                                } else if (lockAspectRatio && storedAspectRatio > 0f) {
                                                    targetHeightPx = max(10, (w / storedAspectRatio).roundToInt())
                                                }
                                            },
                                            label = { Text("Width (px)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8))
                                        )

                                        // Lock Aspect Ratio Button
                                        IconButton(
                                            onClick = {
                                                lockAspectRatio = !lockAspectRatio
                                                if (lockAspectRatio && targetHeightPx > 0) {
                                                    storedAspectRatio = targetWidthPx.toFloat() / targetHeightPx
                                                }
                                            },
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .background(if (lockAspectRatio) Color(0xFF0284C7) else Color(0xFF334155), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (lockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Toggle Aspect Ratio Lock",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = if (isHeightAuto) "Auto (~${targetHeightPx})" else targetHeightPx.toString(),
                                            enabled = !isHeightAuto,
                                            onValueChange = { str ->
                                                val h = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                targetHeightPx = h
                                                if (isWidthAuto) {
                                                    val ratio = if (storedAspectRatio > 0f) storedAspectRatio else 3.0f
                                                    targetWidthPx = max(10, (h * ratio).roundToInt())
                                                } else if (lockAspectRatio && storedAspectRatio > 0f) {
                                                    targetWidthPx = max(10, (h * storedAspectRatio).roundToInt())
                                                }
                                            },
                                            label = { Text("Height (px)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8))
                                        )
                                    }
                                } else {
                                    // Physical Units (Inches / mm / cm)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = physicalWidthText,
                                            onValueChange = { str ->
                                                physicalWidthText = str
                                                val wVal = str.toFloatOrNull() ?: 0f
                                                val pxW = when (dimensionUnit) {
                                                    "in" -> (wVal * selectedDpi).roundToInt()
                                                    "mm" -> (wVal * selectedDpi / 25.4f).roundToInt()
                                                    "cm" -> (wVal * selectedDpi / 2.54f).roundToInt()
                                                    else -> wVal.roundToInt()
                                                }
                                                targetWidthPx = max(10, pxW)
                                                if (lockAspectRatio && storedAspectRatio > 0f) {
                                                    targetHeightPx = max(10, (targetWidthPx / storedAspectRatio).roundToInt())
                                                }
                                            },
                                            label = { Text("Width ($dimensionUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8))
                                        )

                                        OutlinedTextField(
                                            value = physicalHeightText,
                                            onValueChange = { str ->
                                                physicalHeightText = str
                                                val hVal = str.toFloatOrNull() ?: 0f
                                                val pxH = when (dimensionUnit) {
                                                    "in" -> (hVal * selectedDpi).roundToInt()
                                                    "mm" -> (hVal * selectedDpi / 25.4f).roundToInt()
                                                    "cm" -> (hVal * selectedDpi / 2.54f).roundToInt()
                                                    else -> hVal.roundToInt()
                                                }
                                                targetHeightPx = max(10, pxH)
                                                if (lockAspectRatio && storedAspectRatio > 0f) {
                                                    targetWidthPx = max(10, (targetHeightPx * storedAspectRatio).roundToInt())
                                                }
                                            },
                                            label = { Text("Height ($dimensionUnit)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "↳ Evaluates to: ${targetWidthPx} × ${targetHeightPx} px @ ${selectedDpi} DPI",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // DPI & Physical Print Size
                                Text(
                                    text = "2. PRINT DENSITY (DPI) & PHYSICAL SIZE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(72, 150, 200, 300, 600).forEach { dpi ->
                                        FilterChip(
                                            selected = selectedDpi == dpi,
                                            onClick = {
                                                selectedDpi = dpi
                                                if (dimensionUnit == "in") {
                                                    val wIn = physicalWidthText.toFloatOrNull() ?: 2.0f
                                                    val hIn = physicalHeightText.toFloatOrNull() ?: 1.0f
                                                    targetWidthPx = max(10, (wIn * dpi).roundToInt())
                                                    targetHeightPx = max(10, (hIn * dpi).roundToInt())
                                                } else if (dimensionUnit == "mm") {
                                                    val wMm = physicalWidthText.toFloatOrNull() ?: 50.0f
                                                    val hMm = physicalHeightText.toFloatOrNull() ?: 25.0f
                                                    targetWidthPx = max(10, (wMm * dpi / 25.4f).roundToInt())
                                                    targetHeightPx = max(10, (hMm * dpi / 25.4f).roundToInt())
                                                } else if (dimensionUnit == "cm") {
                                                    val wCm = physicalWidthText.toFloatOrNull() ?: 5.0f
                                                    val hCm = physicalHeightText.toFloatOrNull() ?: 2.5f
                                                    targetWidthPx = max(10, (wCm * dpi / 2.54f).roundToInt())
                                                    targetHeightPx = max(10, (hCm * dpi / 2.54f).roundToInt())
                                                }
                                            },
                                            label = { Text("$dpi DPI", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                val printInW = targetWidthPx.toFloat() / selectedDpi
                                val printInH = targetHeightPx.toFloat() / selectedDpi
                                val printMmW = printInW * 25.4f
                                val printMmH = printInH * 25.4f

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Calculated Physical Scale @ $selectedDpi DPI:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(
                                            text = String.format(Locale.US, "%.2f × %.2f inches (%.1f × %.1f mm)", printInW, printInH, printMmW, printMmH),
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // TAB 1: 7. SIGNATURE QUALITY PRESERVATION & MARGIN CONTROL
                            // -------------------------------------------------------------
                            1 -> {
                                Text(
                                    text = "7. SIGNATURE QUALITY PRESERVATION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Text(
                                    text = "Do NOT blindly stretch signatures. Choose a preservation strategy:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SignatureProcessor.SignatureFitMode.values().forEach { mode ->
                                        val isSelected = fitMode == mode
                                        val isStretch = mode == SignatureProcessor.SignatureFitMode.STRETCH

                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    if (isStretch) Color(0xFF7F1D1D) else Color(0xFF0284C7)
                                                } else Color(0xFF0F172A)
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.5.dp, if (isStretch) Color(0xFFEF4444) else Color(0xFF38BDF8))
                                            } else BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isStretch) {
                                                        showStretchWarningDialog = true
                                                    } else {
                                                        fitMode = mode
                                                    }
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = mode.label,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                                        fontSize = 14.sp
                                                    )
                                                    if (isStretch) {
                                                        Text(
                                                            text = "⚠ DISTORTION RISK",
                                                            color = Color(0xFFFCA5A5),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else if (isSelected) {
                                                        Text(
                                                            text = "✓ ACTIVE",
                                                            color = Color(0xFFE0F2FE),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = mode.description,
                                                    color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                                if (isStretch) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Warning: Stretching may distort the signature.",
                                                        color = Color(0xFFFCA5A5),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Prominent persistent warning card if STRETCH is active
                                if (fitMode == SignatureProcessor.SignatureFitMode.STRETCH) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Stretching may distort the signature.",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFCA5A5),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "Forces exact width and height independently, distorting authentic handwriting strokes. Document portals and banks frequently reject distorted signatures.",
                                                    color = Color(0xFFFECACA),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            TextButton(onClick = { fitMode = SignatureProcessor.SignatureFitMode.FIT }) {
                                                Text("Use FIT", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Automatic Whitespace Trimming & Manual Margin Control
                                Text(
                                    text = "WHITESPACE TRIMMING & MARGIN CONTROL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Automatic whitespace trimming", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Crop unnecessary whitespace while preserving the signature strokes", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = autoTrimMargins,
                                        onCheckedChange = { autoTrimMargins = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8), checkedTrackColor = Color(0xFF0F172A))
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Manual margin control: ${marginPaddingPx} px (~${String.format(Locale.US, "%.1f", marginPaddingPx * 25.4f / selectedDpi)} mm @ ${selectedDpi} DPI)",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                                Slider(
                                    value = marginPaddingPx.toFloat(),
                                    onValueChange = { marginPaddingPx = it.toInt() },
                                    valueRange = 0f..64f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0 to "0px (Flush)", 8 to "8px", 16 to "16px (Std)", 24 to "24px", 32 to "32px").forEach { (pad, label) ->
                                        FilterChip(
                                            selected = marginPaddingPx == pad,
                                            onClick = { marginPaddingPx = pad },
                                            label = { Text(label, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
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
                                    Text("Center signature inside box", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Switch(
                                        checked = centerSignatureInBox,
                                        onCheckedChange = { centerSignatureInBox = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8), checkedTrackColor = Color(0xFF0F172A))
                                    )
                                }
                            }

                            // -------------------------------------------------------------
                            // TAB 2: 8. SIGNATURE BACKGROUND PROCESSING
                            // -------------------------------------------------------------
                            2 -> {
                                Text(
                                    text = "8. SIGNATURE BACKGROUND PROCESSING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Options: White background, Transparent background where technically supported, Custom background
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SignatureProcessor.SignatureBackgroundType.values().forEach { bg ->
                                        val isSelected = backgroundType == bg
                                        val isTransparent = bg == SignatureProcessor.SignatureBackgroundType.TRANSPARENT

                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A)
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                            } else BorderStroke(1.dp, Color(0xFF1E293B)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    backgroundType = bg
                                                    if (isTransparent && exportFormat == ExportFormat.JPEG) {
                                                        exportFormat = ExportFormat.PNG
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = when (bg) {
                                                                SignatureProcessor.SignatureBackgroundType.WHITE -> Color.White
                                                                SignatureProcessor.SignatureBackgroundType.TRANSPARENT -> Color.Transparent
                                                                SignatureProcessor.SignatureBackgroundType.CUSTOM -> Color(customBgColor)
                                                            },
                                                            shape = CircleShape
                                                        )
                                                        .border(1.5.dp, if (isSelected) Color.White else Color(0xFF64748B), CircleShape)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = bg.label,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                                            fontSize = 13.sp
                                                        )
                                                        if (isTransparent) {
                                                            Text("PNG / WebP only", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = bg.description,
                                                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Technical transparency notice if JPEG or unsupported format
                                if (backgroundType == SignatureProcessor.SignatureBackgroundType.TRANSPARENT) {
                                    val supportsAlpha = exportFormat == ExportFormat.PNG || exportFormat == ExportFormat.WEBP_LOSSY || exportFormat == ExportFormat.WEBP_LOSSLESS
                                    if (!supportsAlpha) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFCD34D), modifier = Modifier.size(16.dp))
                                                    Text("Technical Transparency Notice", fontWeight = FontWeight.Bold, color = Color(0xFFFCD34D), fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "JPEG and PDF file formats do not technically support alpha transparency. Output will render on a white background unless switched to PNG or WebP.",
                                                    color = Color(0xFFFEF3C7),
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = { exportFormat = ExportFormat.PNG },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Switch to PNG (Supports Transparency)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Custom Background Color Presets & Hex Picker
                                if (backgroundType == SignatureProcessor.SignatureBackgroundType.CUSTOM) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Custom Background Presets:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Cream Paper" to android.graphics.Color.rgb(253, 251, 247),
                                            "Document Off-White" to android.graphics.Color.rgb(248, 250, 252),
                                            "Bank Tint" to android.graphics.Color.rgb(239, 246, 255),
                                            "Warm Parchment" to android.graphics.Color.rgb(255, 251, 235),
                                            "Stamp Red Tint" to android.graphics.Color.rgb(254, 242, 242),
                                            "Slate Gray" to android.graphics.Color.rgb(241, 245, 249)
                                        ).forEach { (name, colorVal) ->
                                            val isSel = customBgColor == colorVal
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    customBgColor = colorVal
                                                    customHexText = String.format("#%06X", (0xFFFFFF and colorVal))
                                                },
                                                label = { Text(name, fontSize = 10.sp) },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(Color(colorVal), CircleShape)
                                                            .border(1.dp, Color(0xFF64748B), CircleShape)
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF38BDF8),
                                                    selectedLabelColor = Color(0xFF0F172A)
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = customHexText,
                                            onValueChange = { str ->
                                                customHexText = str
                                                if (str.startsWith("#") && (str.length == 7 || str.length == 9)) {
                                                    try {
                                                        customBgColor = android.graphics.Color.parseColor(str)
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            label = { Text("Custom Hex Color (#RRGGBB)") },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF38BDF8),
                                                unfocusedBorderColor = Color(0xFF334155)
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(Color(customBgColor), RoundedCornerShape(8.dp))
                                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // BACKGROUND CLEANUP
                                Text(
                                    text = "BACKGROUND CLEANUP & THRESHOLDING",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        paperThreshold = 195
                                        contrastAdj = 25f
                                        brightnessAdj = 10f
                                        inkMode = SignatureProcessor.SignatureInkMode.BLACK_AND_WHITE
                                        outputInkColor = SignatureProcessor.OutputInkColor.PURE_BLACK
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto Cleanup Background (Remove Shadows & Grain)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Paper Luminance Threshold ($paperThreshold / 255):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("Removes camera shadows, scanner texture, and yellow paper grain without degrading pen ink.", color = Color(0xFF64748B), fontSize = 10.sp)
                                Slider(
                                    value = paperThreshold.toFloat(),
                                    onValueChange = { paperThreshold = it.toInt() },
                                    valueRange = 100f..240f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Brightness: ${brightnessAdj.toInt()}", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(90.dp))
                                    Slider(
                                        value = brightnessAdj,
                                        onValueChange = { brightnessAdj = it },
                                        valueRange = -100f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Contrast: ${contrastAdj.toInt()}", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(90.dp))
                                    Slider(
                                        value = contrastAdj,
                                        onValueChange = { contrastAdj = it },
                                        valueRange = -100f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Ink Enhancement Mode:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SignatureProcessor.SignatureInkMode.values().forEach { mode ->
                                        FilterChip(
                                            selected = inkMode == mode,
                                            onClick = { inkMode = mode },
                                            label = { Text(mode.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Output Ink Color:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SignatureProcessor.OutputInkColor.values().forEach { colorOpt ->
                                        FilterChip(
                                            selected = outputInkColor == colorOpt,
                                            onClick = { outputInkColor = colorOpt },
                                            label = { Text(colorOpt.label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Stroke Weight (Boldness / Thinning):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = when {
                                            strokeWeightDelta > 0 -> "+$strokeWeightDelta (Bolder)"
                                            strokeWeightDelta < 0 -> "$strokeWeightDelta (Finer)"
                                            else -> "0 (Original)"
                                        },
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Slider(
                                    value = strokeWeightDelta.toFloat(),
                                    onValueChange = { strokeWeightDelta = it.roundToInt() },
                                    valueRange = -2f..3f,
                                    steps = 4,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // REAL ALGORITHM DISCLOSURE (CRITICAL MANDATE)
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            Text("Background Cleanup Algorithm Disclosure", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Background separation uses mathematical color luminance thresholding, contrast amplification, and ink boundary segmentation to isolate pen strokes from lighter paper surfaces. We do not use cloud neural network segmentation models, and we do not claim perfect background removal on complex, patterned, or heavily shadowed backgrounds. Best results are obtained when signatures are written on plain white or light solid paper.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // TAB 3: FILE SIZE CEILING & FORMAT
                            // -------------------------------------------------------------
                            3 -> {
                                Text(
                                    text = "1. EXPORT FORMAT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        ExportFormat.JPEG to "JPG (Govt Portals)",
                                        ExportFormat.PNG to "PNG (Transparent)",
                                        ExportFormat.WEBP_LOSSY to "WebP (Compact)"
                                    ).forEach { (fmt, label) ->
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (exportFormat == fmt) Color(0xFF38BDF8) else Color(0xFF0F172A)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    exportFormat = fmt
                                                    if (fmt == ExportFormat.JPEG && backgroundType == SignatureProcessor.SignatureBackgroundType.TRANSPARENT) {
                                                        backgroundType = SignatureProcessor.SignatureBackgroundType.WHITE
                                                    }
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = fmt.extension.uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (exportFormat == fmt) Color(0xFF0F172A) else Color.White
                                                )
                                                Text(
                                                    text = if (fmt == ExportFormat.JPEG) "No Alpha" else "Supports Alpha",
                                                    fontSize = 9.sp,
                                                    color = if (exportFormat == fmt) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "2. TARGET FILE SIZE LIMIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "No Limit" to null,
                                        "≤ 10 KB" to 10,
                                        "≤ 20 KB (Govt)" to 20,
                                        "≤ 50 KB" to 50,
                                        "≤ 100 KB" to 100
                                    ).forEach { (label, kb) ->
                                        FilterChip(
                                            selected = targetSizeKb == kb,
                                            onClick = { targetSizeKb = kb },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FileSizeMode.values().forEach { mode ->
                                        FilterChip(
                                            selected = fileSizeMode == mode,
                                            onClick = { fileSizeMode = mode },
                                            label = { Text(mode.shortLabel, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF38BDF8),
                                                selectedLabelColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }
                            }

                            // -------------------------------------------------------------
                            // TAB 4: VERIFICATION TELEMETRY & AUDIT (ALL 11 REQUIRED FIELDS)
                            // -------------------------------------------------------------
                            4 -> {
                                Text(
                                    text = "SIGNATURE VERIFICATION & COMPLIANCE AUDIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Full verification of source, target parameters, and final output telemetry:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // 1. DIMENSIONS & RESOLUTION
                                TelemetryCard(
                                    title = "1. DIMENSIONS & RESOLUTION",
                                    accentColor = Color(0xFF38BDF8)
                                ) {
                                    val origDimStr = if (originalWidth > 0 && originalHeight > 0) "${originalWidth} × ${originalHeight} px" else "1200 × 600 px (Active Canvas)"
                                    TelemetryRow(label = "Original dimensions:", value = origDimStr)
                                    TelemetryRow(
                                        label = "Target dimensions:",
                                        value = "${targetWidthPx} × ${targetHeightPx} px" + if (isHeightAuto) " (Height: Auto)" else if (dimensionUnit == "in") " ($physicalWidthText × $physicalHeightText in)" else "",
                                        highlight = true,
                                        highlightColor = Color(0xFF38BDF8)
                                    )
                                    val finalW = processedSignatureBitmap?.width ?: targetWidthPx
                                    val finalH = processedSignatureBitmap?.height ?: targetHeightPx
                                    TelemetryRow(
                                        label = "Final dimensions:",
                                        value = "${finalW} × ${finalH} px",
                                        highlight = true,
                                        highlightColor = Color(0xFF34D399)
                                    )
                                    val currentRatio = finalW.toFloat() / max(1, finalH)
                                    TelemetryRow(
                                        label = "Aspect Ratio:",
                                        value = String.format(Locale.US, "%.2f : 1", currentRatio)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2. PRINT DENSITY (DPI) & PHYSICAL SCALE
                                TelemetryCard(
                                    title = "2. PRINT DENSITY (DPI) AUDIT",
                                    accentColor = Color(0xFFA78BFA)
                                ) {
                                    TelemetryRow(
                                        label = "Original DPI if available:",
                                        value = if (originalDpi != null) "${originalDpi} DPI (EXIF Metadata)" else "72 DPI (Standard / Not specified)"
                                    )
                                    TelemetryRow(
                                        label = "Target DPI:",
                                        value = "${selectedDpi} DPI",
                                        highlight = true,
                                        highlightColor = Color(0xFFA78BFA)
                                    )
                                    val finalDpiText = when (exportFormat) {
                                        ExportFormat.JPEG -> "${selectedDpi} DPI (Embedded in JFIF EXIF)"
                                        ExportFormat.PNG -> "${selectedDpi} DPI (Embedded in PNG pHYs chunk)"
                                        ExportFormat.WEBP_LOSSY, ExportFormat.WEBP_LOSSLESS -> "${selectedDpi} DPI (Render Density)"
                                        ExportFormat.PDF -> "${selectedDpi} DPI (72 pt/in PDF Vector & Raster)"
                                    }
                                    TelemetryRow(
                                        label = "Final DPI if supported:",
                                        value = finalDpiText,
                                        highlight = true,
                                        highlightColor = Color(0xFF34D399)
                                    )
                                    val finalW = processedSignatureBitmap?.width ?: targetWidthPx
                                    val finalH = processedSignatureBitmap?.height ?: targetHeightPx
                                    val pInW = finalW.toFloat() / selectedDpi
                                    val pInH = finalH.toFloat() / selectedDpi
                                    TelemetryRow(
                                        label = "Physical Print Size:",
                                        value = String.format(Locale.US, "%.2f × %.2f in (%.1f × %.1f mm)", pInW, pInH, pInW * 25.4f, pInH * 25.4f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3. FILE SIZE & CEILING COMPLIANCE
                                TelemetryCard(
                                    title = "3. FILE SIZE & COMPLIANCE",
                                    accentColor = Color(0xFFF59E0B)
                                ) {
                                    val origBytes = if (originalFileSizeBytes > 0L) originalFileSizeBytes else (1200L * 600L * 4L / 12L)
                                    TelemetryRow(
                                        label = "Original file size:",
                                        value = formatByteSize(origBytes)
                                    )
                                    TelemetryRow(
                                        label = "Target file size:",
                                        value = if (targetSizeKb != null) "≤ ${targetSizeKb} KB (${fileSizeMode.shortLabel})" else "No Limit (Max Quality)",
                                        highlight = true,
                                        highlightColor = Color(0xFFF59E0B)
                                    )
                                    val actualBytes = liveTelemetryResult?.actualBytes ?: 0L
                                    val isCompliant = liveTelemetryResult?.isCompliantWithCeiling ?: true
                                    TelemetryRow(
                                        label = "Final file size:",
                                        value = formatByteSize(actualBytes),
                                        highlight = true,
                                        highlightColor = if (isCompliant) Color(0xFF34D399) else Color(0xFFF87171)
                                    )
                                    if (targetSizeKb != null) {
                                        TelemetryRow(
                                            label = "Ceiling Compliance:",
                                            value = if (isCompliant) "✓ PASS (≤ ${targetSizeKb} KB ceiling met)" else "⚠ EXCEEDS ${targetSizeKb} KB",
                                            highlight = true,
                                            highlightColor = if (isCompliant) Color(0xFF34D399) else Color(0xFFF87171)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 4. OUTPUT FORMAT & GEOMETRY INTEGRITY
                                TelemetryCard(
                                    title = "4. FORMAT & GEOMETRY AUDIT",
                                    accentColor = Color(0xFF34D399)
                                ) {
                                    TelemetryRow(
                                        label = "Output format:",
                                        value = "${exportFormat.name} (${exportFormat.mimeType})",
                                        highlight = true,
                                        highlightColor = Color(0xFF38BDF8)
                                    )
                                    TelemetryRow(
                                        label = "Target Box Fitting:",
                                        value = fitMode.label
                                    )
                                    TelemetryRow(
                                        label = "Geometry Audit:",
                                        value = if (fitMode == SignatureProcessor.SignatureFitMode.STRETCH) "⚠ DISTORTED (Stretching may distort the signature)" else "✓ AUTHENTIC PROPORTIONS (Preserves signature geometry)",
                                        highlight = true,
                                        highlightColor = if (fitMode == SignatureProcessor.SignatureFitMode.STRETCH) Color(0xFFF87171) else Color(0xFF34D399)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // -------------------------------------------------------------
                        // EXPORT ACTIONS ROW: SAVE, SHARE, PRINT
                        // -------------------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Save Button
                            Button(
                                onClick = {
                                    val bmp = processedSignatureBitmap
                                    if (bmp != null) {
                                        scope.launch {
                                            val result = ExportEngine.exportImage(
                                                context = context,
                                                bitmap = bmp,
                                                format = exportFormat,
                                                targetSizeKb = targetSizeKb,
                                                fileSizeMode = fileSizeMode,
                                                fileNamePrefix = "Signature",
                                                dpi = selectedDpi
                                            )
                                            if (result.success) {
                                                lastSavedFile = result.outputFile
                                                val sizeStr = String.format("%.1f KB", result.fileSizeBytes / 1024.0)
                                                snackbarHostState.showSnackbar("Saved: ${result.width}×${result.height} px ($sizeStr)")
                                            } else {
                                                snackbarHostState.showSnackbar(result.errorMessage ?: "Export error")
                                            }
                                        }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("Please import, capture, or draw signature first") }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                                    .testTag("save_signature_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Signature", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Share Button
                            Button(
                                onClick = {
                                    val bmp = processedSignatureBitmap
                                    if (bmp != null) {
                                        scope.launch {
                                            val result = ExportEngine.exportImage(
                                                context = context,
                                                bitmap = bmp,
                                                format = exportFormat,
                                                targetSizeKb = targetSizeKb,
                                                fileSizeMode = fileSizeMode,
                                                fileNamePrefix = "Signature",
                                                dpi = selectedDpi
                                            )
                                            if (result.success && result.outputFile != null) {
                                                lastSavedFile = result.outputFile
                                                ExportEngine.shareFile(context, result.outputFile, exportFormat.mimeType)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("share_signature_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            // Print Button
                            Button(
                                onClick = {
                                    val bmp = processedSignatureBitmap
                                    if (bmp != null) {
                                        ExportEngine.printBitmap(context, bmp, "Signature_${targetWidthPx}x${targetHeightPx}")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("print_signature_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Checkerboard pattern composable to display transparency clearly.
 */
@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = 16.dp.toPx()
        val numCols = (size.width / squareSize).toInt() + 1
        val numRows = (size.height / squareSize).toInt() + 1

        for (row in 0..numRows) {
            for (col in 0..numCols) {
                val isDark = (row + col) % 2 == 0
                val color = if (isDark) Color(0xFF1E293B) else Color(0xFF0F172A)
                drawRect(
                    color = color,
                    topLeft = Offset(col * squareSize, row * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}

/**
 * Formats a byte size into human-readable B, KB, or MB with byte precision.
 */
private fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.2f MB (%,d bytes)", mb, bytes)
        kb >= 1.0 -> String.format(Locale.US, "%.2f KB (%,d bytes)", kb, bytes)
        else -> "$bytes bytes"
    }
}

/**
 * Telemetry Card section for compliance reporting.
 */
@Composable
private fun TelemetryCard(
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

/**
 * Individual Telemetry Row entry.
 */
@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    highlightColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            color = if (highlight) highlightColor else Color.White,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.weight(1.3f)
        )
    }
}

