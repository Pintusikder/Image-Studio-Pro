package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExportFormat
import com.example.model.ImageExifData
import com.example.model.MetadataPolicy
import com.example.processing.ExifManager
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.UtilityViewModel

/**
 * Full-featured UI component for EXIF Metadata Inspector, Privacy Scrubber, and Format Disclosures.
 * Complies with Request 28:
 * - Keep metadata
 * - Remove metadata
 * - Remove GPS data
 * - Remove camera information
 * - Remove device information
 * - Remove date metadata where supported
 * - Transparent & honest disclosure of format limitations
 */
@Composable
fun ExifPrivacyControls(viewModel: UtilityViewModel, state: StudioUiState) {
    val exif = state.exifData
    val privacyConfig = state.metadataPrivacyConfig
    val disclosure = ExifManager.getFormatDisclosure(state.exportFormat)
    var showRawInspector by remember { mutableStateOf(false) }
    var showCustomAuthorEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exif_privacy_controls_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title & Privacy Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "METADATA & EXIF PRIVACY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Granular privacy scrubber & metadata policy",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Posture Badge
            val (badgeBg, badgeBorder, badgeText, badgeIcon) = when {
                privacyConfig.policy == MetadataPolicy.STRIP_ALL -> {
                    Tuple4(Color(0xFF064E3B), Color(0xFF10B981), "100% Scrubbed", Icons.Default.Shield)
                }
                privacyConfig.policy == MetadataPolicy.CUSTOM_SELECTIVE -> {
                    Tuple4(Color(0xFF1E3A8A), Color(0xFF38BDF8), "${privacyConfig.scrubbedCategoriesCount}/4 Scrubbed", Icons.Default.Security)
                }
                else -> {
                    Tuple4(Color(0xFF451A03), Color(0xFFF59E0B), "Metadata Retained", Icons.Default.Info)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeBorder, modifier = Modifier.size(12.dp))
                    Text(badgeText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Action Buttons (Strip All vs Keep All)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.stripAllMetadataNow() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_strip_all_metadata"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) Color(0xFF0F766E).copy(alpha = 0.3f) else Color(0xFF0F172A),
                    contentColor = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) Color(0xFF2DD4BF) else Color(0xFFE2E8F0)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) Color(0xFF2DD4BF) else Color(0xFF334155)
                )
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { viewModel.keepAllMetadataNow() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_keep_all_metadata"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) Color(0xFFB45309).copy(alpha = 0.3f) else Color(0xFF0F172A),
                    contentColor = if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) Color(0xFFFBBF24) else Color(0xFFE2E8F0)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) Color(0xFFFBBF24) else Color(0xFF334155)
                )
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Keep All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Policy Mode Tabs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIVACY POLICY SELECTION",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataPolicy.values().forEach { policy ->
                        val isSelected = privacyConfig.policy == policy
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setMetadataPolicy(policy) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = policy.label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(
                    text = privacyConfig.policy.description,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }

        // Granular Privacy Scrubber Switches (When in CUSTOM_SELECTIVE or expanded)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SELECTIVE FIELD SCRUBBING",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. GPS / Geolocation Data
                PrivacyToggleRow(
                    title = "Remove GPS & Location Data",
                    subtitle = if (exif?.hasGps == true) "Source image contains GPS coordinates!" else "Latitude, longitude, altitude, GPS datestamps",
                    icon = Icons.Default.LocationOn,
                    isChecked = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) true else if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) false else privacyConfig.removeGps,
                    isEnabled = privacyConfig.policy == MetadataPolicy.CUSTOM_SELECTIVE,
                    warning = exif?.hasGps == true && privacyConfig.policy == MetadataPolicy.KEEP_ALL,
                    testTag = "toggle_remove_gps",
                    onCheckedChange = { viewModel.setRemoveGps(it) }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                // 2. Camera Information
                PrivacyToggleRow(
                    title = "Remove Camera Information",
                    subtitle = "Make, model, lens model, software, serial numbers",
                    icon = Icons.Default.CameraAlt,
                    isChecked = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) true else if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) false else privacyConfig.removeCameraInfo,
                    isEnabled = privacyConfig.policy == MetadataPolicy.CUSTOM_SELECTIVE,
                    testTag = "toggle_remove_camera",
                    onCheckedChange = { viewModel.setRemoveCameraInfo(it) }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                // 3. Device Information
                PrivacyToggleRow(
                    title = "Remove Device Information",
                    subtitle = "Hardware settings, sensor details, maker notes",
                    icon = Icons.Default.PhoneAndroid,
                    isChecked = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) true else if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) false else privacyConfig.removeDeviceInfo,
                    isEnabled = privacyConfig.policy == MetadataPolicy.CUSTOM_SELECTIVE,
                    testTag = "toggle_remove_device",
                    onCheckedChange = { viewModel.setRemoveDeviceInfo(it) }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                // 4. Date Metadata
                PrivacyToggleRow(
                    title = "Remove Date & Timestamps",
                    subtitle = "Capture date, digitized time, sub-second timestamps",
                    icon = Icons.Default.DateRange,
                    isChecked = if (privacyConfig.policy == MetadataPolicy.STRIP_ALL) true else if (privacyConfig.policy == MetadataPolicy.KEEP_ALL) false else privacyConfig.removeDateMetadata,
                    isEnabled = privacyConfig.policy == MetadataPolicy.CUSTOM_SELECTIVE,
                    testTag = "toggle_remove_date",
                    onCheckedChange = { viewModel.setRemoveDateMetadata(it) }
                )
            }
        }

        // Live EXIF Inspector of Current Image
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Text("DETECTED SOURCE EXIF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = if (showRawInspector) "Hide Details" else "Inspect All",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showRawInspector = !showRawInspector }
                            .padding(4.dp)
                    )
                }

                if (exif != null) {
                    // Summary grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExifSummaryPill(
                            label = "Camera",
                            value = if (exif.make.isNullOrBlank() && exif.model.isNullOrBlank()) "None" else "${exif.make ?: ""} ${exif.model ?: ""}".trim(),
                            icon = Icons.Default.CameraAlt,
                            modifier = Modifier.weight(1f)
                        )
                        ExifSummaryPill(
                            label = "Location",
                            value = if (exif.hasGps) "GPS Embedded" else "Private (No GPS)",
                            icon = Icons.Default.LocationOn,
                            color = if (exif.hasGps) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExifSummaryPill(
                            label = "Date Taken",
                            value = exif.dateTimeOriginal ?: exif.dateTime ?: "Not Specified",
                            icon = Icons.Default.DateRange,
                            modifier = Modifier.weight(1f)
                        )
                        ExifSummaryPill(
                            label = "Optics",
                            value = if (exif.focalLength != null) "${exif.focalLength} • f/${exif.aperture ?: "?"}" else "Standard",
                            icon = Icons.Default.Info,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(
                        visible = showRawInspector,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B14))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("EXIF Field Breakdown:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            ExifDetailRow("Make / Manufacturer", exif.make)
                            ExifDetailRow("Camera Model", exif.model)
                            ExifDetailRow("Lens Model", exif.lensModel)
                            ExifDetailRow("Software", exif.software)
                            ExifDetailRow("Serial Number", exif.serialNumber)
                            ExifDetailRow("Shutter Speed", exif.shutterSpeed)
                            ExifDetailRow("Aperture (f-stop)", exif.aperture?.let { "f/$it" })
                            ExifDetailRow("ISO Sensitivity", exif.iso)
                            ExifDetailRow("Focal Length", exif.focalLength)
                            ExifDetailRow("Flash", exif.flash)
                            ExifDetailRow("White Balance", exif.whiteBalance)
                            ExifDetailRow("Date Time Original", exif.dateTimeOriginal)
                            ExifDetailRow("Date Time Modified", exif.dateTime)
                            ExifDetailRow("GPS Latitude", exif.latitude?.let { String.format("%.6f°", it) })
                            ExifDetailRow("GPS Longitude", exif.longitude?.let { String.format("%.6f°", it) })
                            ExifDetailRow("GPS Altitude", exif.altitude?.let { String.format("%.1f m", it) })
                            ExifDetailRow("GPS Date Stamp", exif.gpsDateStamp)
                            ExifDetailRow("Device Setting Desc", exif.deviceSettingDescription)
                            ExifDetailRow("Artist / Author", exif.artist)
                            ExifDetailRow("Copyright", exif.copyright)
                            ExifDetailRow("User Comment", exif.userComment)
                        }
                    }
                } else {
                    Text(
                        text = "No image loaded or no EXIF metadata detected in source file.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Custom Author & Rights Editor (Optional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM AUTHOR & COPYRIGHT TAGS",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (showCustomAuthorEditor) "Collapse" else "Edit Tags",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showCustomAuthorEditor = !showCustomAuthorEditor }
                            .padding(4.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showCustomAuthorEditor,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = privacyConfig.customArtist,
                            onValueChange = {
                                viewModel.setCustomAuthorInfo(
                                    artist = it,
                                    copyright = privacyConfig.customCopyright,
                                    comment = privacyConfig.customComment
                                )
                            },
                            label = { Text("Artist / Photographer Name", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = privacyConfig.customCopyright,
                            onValueChange = {
                                viewModel.setCustomAuthorInfo(
                                    artist = privacyConfig.customArtist,
                                    copyright = it,
                                    comment = privacyConfig.customComment
                                )
                            },
                            label = { Text("Copyright Notice (e.g., © 2026 Studio)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = privacyConfig.customComment,
                            onValueChange = {
                                viewModel.setCustomAuthorInfo(
                                    artist = privacyConfig.customArtist,
                                    copyright = privacyConfig.customCopyright,
                                    comment = it
                                )
                            },
                            label = { Text("User Comment / Description", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Format-Aware Transparency & Audit Box
        // Complies with: "Never falsely claim that every metadata field has been removed if the selected format or library retains some metadata."
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                    Text(
                        text = "FORMAT METADATA SPECIFICATION DISCLOSURE (${state.exportFormat.name})",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = disclosure.privacySummary,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF070B14))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Binary Format Headers Retained:",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = disclosure.formatStructuralHeadersRetained,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Technical Transparency Note:",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = disclosure.technicalHonestyNote,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    isEnabled: Boolean,
    warning: Boolean = false,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) Color(0xFF0F766E).copy(alpha = 0.2f) else Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) Color(0xFF2DD4BF) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (warning) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                    }
                }
                Text(
                    text = subtitle,
                    color = if (warning) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = if (isEnabled) onCheckedChange else { _ -> },
            enabled = isEnabled,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF2DD4BF),
                checkedTrackColor = Color(0xFF0F766E),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
private fun ExifSummaryPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B1120))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun ExifDetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
        Text(value, color = Color(0xFFE2E8F0), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
