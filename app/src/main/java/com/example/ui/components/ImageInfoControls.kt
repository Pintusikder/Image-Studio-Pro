package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.processing.ColorSwatchInfo
import com.example.processing.ImageInfoAnalyzer
import com.example.processing.ImageInfoDetails
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

@Composable
fun ImageInfoControls(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBmp = state.workingBitmap ?: state.originalBitmap
    val info = remember(
        state.currentImageUri,
        currentBmp,
        state.originalWidth,
        state.originalHeight,
        state.originalFileSizeBytes,
        state.originalMimeType,
        state.originalReadDpi,
        state.exifData,
        state.dpi,
        state.detectedInputFormat
    ) {
        ImageInfoAnalyzer.analyze(
            context = context,
            uri = state.currentImageUri,
            bitmap = currentBmp,
            originalWidth = state.originalWidth,
            originalHeight = state.originalHeight,
            originalFileSize = state.originalFileSizeBytes,
            originalMime = state.originalMimeType,
            originalDpi = state.originalReadDpi,
            exif = state.exifData,
            currentDpi = state.dpi,
            detectedFormat = state.detectedInputFormat
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header: File Identity & Quick Summary
        ImageInfoHeroCard(info = info, onCopyFilename = {
            copyToClipboard(context, "Filename", info.fileName)
        })

        // Copy Full Report Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    copyToClipboard(context, "Image Information Report", generateTextReport(info))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("copy_info_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Full Report", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            OutlinedButton(
                onClick = { viewModel.selectTab(StudioTab.EXIF) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                modifier = Modifier.testTag("open_exif_tab_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("EXIF Privacy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Section 1: Dimensions & Geometry
        InfoSectionCard(
            title = "DIMENSIONS & ASPECT RATIO",
            icon = Icons.Default.AspectRatio,
            iconTint = Color(0xFF38BDF8)
        ) {
            InfoDataRow("Current Resolution", "${info.width} × ${info.height} px")
            if (info.originalWidth > 0 && (info.originalWidth != info.width || info.originalHeight != info.height)) {
                InfoDataRow("Original Resolution", "${info.originalWidth} × ${info.originalHeight} px")
            }
            InfoDataRow("Total Pixels", String.format("%.2f Megapixels (%,d pixels)", info.megapixels, info.width.toLong() * info.height.toLong()))
            InfoDataRow("Aspect Ratio", "${info.aspectRatioRatioString} (${String.format("%.2f", info.aspectRatioDecimal)} : 1)")
            if (info.aspectRatioStandardName != null) {
                InfoDataRow("Standard Framing", info.aspectRatioStandardName)
            }
            InfoDataRow("Orientation", info.orientationCategory)
        }

        // Section 2: File & Format
        InfoSectionCard(
            title = "FORMAT & STORAGE",
            icon = Icons.Default.Image,
            iconTint = Color(0xFF34D399)
        ) {
            InfoDataRow("Filename", info.fileName, isMonospace = true)
            InfoDataRow("Container Format", "${info.formatDisplayName} (${info.formatExtension})")
            InfoDataRow("MIME Type", info.mimeType, isMonospace = true)
            InfoDataRow("Disk File Size", "${info.formattedFileSize} (${String.format("%,d", info.fileSizeBytes)} bytes)")
            InfoDataRow("RAM Footprint", "${info.formattedMemoryFootprint} (Uncompressed ARGB)")
        }

        // Section 3: Print & Resolution
        InfoSectionCard(
            title = "PRINT & PHYSICAL DPI",
            icon = Icons.Default.Print,
            iconTint = Color(0xFFA78BFA)
        ) {
            InfoDataRow("Physical Density", "${info.dpi} DPI ${if (info.isDefaultDpi) "(Target Preset)" else "(Embedded in File)"}")
            InfoDataRow("Print Size (Inches)", String.format("%.2f × %.2f inches", info.printWidthInches, info.printHeightInches))
            InfoDataRow("Print Size (Centimeters)", String.format("%.2f × %.2f cm (%.0f × %.0f mm)", info.printWidthCm, info.printHeightCm, info.printWidthCm * 10f, info.printHeightCm * 10f))
            val printQuality = when {
                info.dpi >= 300 -> "Photo Lab Quality (Crisp 300+ DPI)"
                info.dpi >= 200 -> "Commercial Offset Printing (200-300 DPI)"
                info.dpi >= 150 -> "Standard Desktop Printing (150-200 DPI)"
                else -> "Screen Display Density (72-96 DPI)"
            }
            InfoDataRow("Clarity Assessment", printQuality)
        }

        // Section 4: Orientation & EXIF Tags
        InfoSectionCard(
            title = "ORIENTATION & HARDWARE ANGLE",
            icon = Icons.Default.RotateRight,
            iconTint = Color(0xFFFBBF24)
        ) {
            InfoDataRow("EXIF Orientation", "${info.exifOrientationDescription} (Tag ${info.exifOrientationDegree}°)")
            InfoDataRow("Canvas Layout", info.canvasOrientationDescription)
        }

        // Section 5: Color Information & Palette
        InfoSectionCard(
            title = "COLOR INFORMATION & PALETTE",
            icon = Icons.Default.Palette,
            iconTint = Color(0xFFF43F5E)
        ) {
            InfoDataRow("Color Model", info.colorModel)
            InfoDataRow("Bit Depth", info.bitDepth)
            InfoDataRow("Color Space", info.colorSpace)
            InfoDataRow("Alpha Channel", if (info.hasAlphaChannel) "Present (32-bit RGBA)" else "None (Opaque 24-bit RGB)")
            InfoDataRow("Transparency", if (info.hasTransparentPixels) "Active Transparent Pixels Detected" else "100% Solid Pixels (No Alpha Holes)")
            InfoDataRow("Color Mode", if (info.isGrayscale) "Monochrome / Grayscale" else "Full Color Spectrum")

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "DOMINANT PALETTE (Tap to copy hex)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                info.dominantColors.forEach { swatch ->
                    ColorSwatchPill(
                        swatch = swatch,
                        onClick = {
                            copyToClipboard(context, "Hex Color", swatch.hex)
                        }
                    )
                }
            }
        }

        // Section 6: EXIF Availability & Creation Details
        InfoSectionCard(
            title = "EXIF AVAILABILITY & CREATION INFO",
            icon = Icons.Default.CameraAlt,
            iconTint = Color(0xFF38BDF8)
        ) {
            InfoDataRow(
                label = "EXIF Status",
                value = if (info.exifAvailable) "Available (${info.exifTagCount} metadata tags)" else "No EXIF metadata (Clean / Stripped)"
            )
            InfoDataRow("GPS Geotag", if (info.hasGps) "Present (${info.gpsCoordinatesFormatted ?: "Coordinates embedded"})" else "None")
            InfoDataRow("Camera & Lens Info", if (info.hasCameraInfo) "Present" else "None")
            InfoDataRow("Creation Timestamps", if (info.hasDateInfo) "Present" else "None")

            if (info.hasDateInfo) {
                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 6.dp))
                if (!info.creationDateTimeOriginal.isNullOrBlank()) {
                    InfoDataRow("Capture Date & Time", info.creationDateTimeOriginal, isMonospace = true)
                }
                if (!info.creationDateTimeDigitized.isNullOrBlank()) {
                    InfoDataRow("Digitized Date & Time", info.creationDateTimeDigitized, isMonospace = true)
                }
                if (!info.modificationDateTime.isNullOrBlank()) {
                    InfoDataRow("File Modification Time", info.modificationDateTime, isMonospace = true)
                }
            }

            if (info.hasCameraInfo) {
                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 6.dp))
                if (!info.cameraMake.isNullOrBlank()) {
                    InfoDataRow("Camera Make", info.cameraMake)
                }
                if (!info.cameraModel.isNullOrBlank()) {
                    InfoDataRow("Camera Model", info.cameraModel)
                }
                if (!info.lensModel.isNullOrBlank()) {
                    InfoDataRow("Lens Model", info.lensModel)
                }
                if (!info.software.isNullOrBlank()) {
                    InfoDataRow("Software / Engine", info.software)
                }
                if (!info.exposureSummary.isNullOrBlank()) {
                    InfoDataRow("Exposure Settings", info.exposureSummary)
                }
            }

            if (info.hasAuthorCopyright) {
                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 6.dp))
                if (!info.artist.isNullOrBlank()) {
                    InfoDataRow("Author / Artist", info.artist)
                }
                if (!info.copyright.isNullOrBlank()) {
                    InfoDataRow("Copyright Notice", info.copyright)
                }
            }
        }
    }
}

@Composable
fun ImageInfoDialog(
    viewModel: UtilityViewModel,
    state: StudioUiState,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Image Information",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Complete file, geometry, color & EXIF audit",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_info_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ImageInfoControls(viewModel = viewModel, state = state)
                }
            }
        }
    }
}

@Composable
private fun ImageInfoHeroCard(
    info: ImageInfoDetails,
    onCopyFilename: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge for Format
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = info.formatExtension.removePrefix(".").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onCopyFilename() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Filename",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Name",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = info.fileName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(10.dp))

            // 3 KPI Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroKpiItem(
                    label = "RESOLUTION",
                    value = "${info.width}×${info.height}",
                    sub = "${String.format("%.1f", info.megapixels)} MP"
                )
                HeroKpiItem(
                    label = "FILE SIZE",
                    value = info.formattedFileSize,
                    sub = info.mimeType.substringAfterLast("/")
                )
                HeroKpiItem(
                    label = "ASPECT RATIO",
                    value = info.aspectRatioRatioString,
                    sub = info.orientationCategory
                )
                HeroKpiItem(
                    label = "DENSITY",
                    value = "${info.dpi} DPI",
                    sub = "Print Ready"
                )
            }
        }
    }
}

@Composable
private fun HeroKpiItem(label: String, value: String, sub: String) {
    Column {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = Color(0xFFF1F5F9),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = sub,
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoDataRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            color = Color(0xFFF8FAFC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.58f),
            maxLines = 3
        )
    }
}

@Composable
private fun ColorSwatchPill(
    swatch: ColorSwatchInfo,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .testTag("swatch_${swatch.hex.removePrefix("#").lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(swatch.colorInt))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = swatch.hex,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${String.format("%.1f", swatch.percentage)}%",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
        Text(
            text = swatch.label,
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}

private fun generateTextReport(info: ImageInfoDetails): String {
    return buildString {
        appendLine("=== IMAGE INFORMATION AUDIT ===")
        appendLine("Filename: ${info.fileName}")
        appendLine("File Size: ${info.formattedFileSize} (${info.fileSizeBytes} bytes)")
        appendLine("Memory in RAM: ${info.formattedMemoryFootprint}")
        appendLine("Resolution: ${info.width} × ${info.height} pixels (${String.format("%.2f", info.megapixels)} MP)")
        appendLine("Aspect Ratio: ${info.aspectRatioRatioString} (${String.format("%.2f", info.aspectRatioDecimal)}:1) - ${info.orientationCategory}")
        if (info.aspectRatioStandardName != null) {
            appendLine("Framing Standard: ${info.aspectRatioStandardName}")
        }
        appendLine("Format: ${info.formatDisplayName} (${info.formatExtension})")
        appendLine("MIME Type: ${info.mimeType}")
        appendLine("Physical DPI: ${info.dpi} DPI")
        appendLine("Print Size: ${String.format("%.2f × %.2f inches", info.printWidthInches, info.printHeightInches)} / ${String.format("%.2f × %.2f cm", info.printWidthCm, info.printHeightCm)}")
        appendLine("Orientation: ${info.exifOrientationDescription}")
        appendLine("Color Model: ${info.colorModel} (${info.bitDepth})")
        appendLine("Color Space: ${info.colorSpace}")
        appendLine("Alpha Support: ${if (info.hasAlphaChannel) "Yes" else "No"}")
        appendLine("Transparent Pixels: ${if (info.hasTransparentPixels) "Detected" else "None"}")
        appendLine("Spectrum: ${if (info.isGrayscale) "Grayscale / Monochrome" else "Full Color"}")
        appendLine("Dominant Swatches: ${info.dominantColors.joinToString(", ") { "${it.hex} (${String.format("%.1f", it.percentage)}%)" }}")
        appendLine("EXIF Available: ${if (info.exifAvailable) "Yes (${info.exifTagCount} tags)" else "No"}")
        if (info.hasDateInfo) {
            appendLine("Creation Date: ${info.creationDateTimeOriginal ?: info.creationDateTimeDigitized ?: info.modificationDateTime}")
        }
        if (info.hasCameraInfo) {
            appendLine("Camera: ${listOfNotNull(info.cameraMake, info.cameraModel).joinToString(" ")}")
            if (!info.lensModel.isNullOrBlank()) appendLine("Lens: ${info.lensModel}")
            if (!info.software.isNullOrBlank()) appendLine("Software: ${info.software}")
            if (!info.exposureSummary.isNullOrBlank()) appendLine("Exposure: ${info.exposureSummary}")
        }
        if (info.hasGps && !info.gpsCoordinatesFormatted.isNullOrBlank()) {
            appendLine("GPS Coordinates: ${info.gpsCoordinatesFormatted}")
        }
        if (info.hasAuthorCopyright) {
            if (!info.artist.isNullOrBlank()) appendLine("Artist: ${info.artist}")
            if (!info.copyright.isNullOrBlank()) appendLine("Copyright: ${info.copyright}")
        }
    }
}
