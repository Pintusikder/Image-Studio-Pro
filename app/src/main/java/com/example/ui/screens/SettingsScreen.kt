package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.viewmodel.UtilityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: UtilityViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.appSettings
    val context = LocalContext.current

    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLocalProcessingDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showSecurityAuditDialog by remember { mutableStateOf(false) }
    var showPrivacyResetConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.securityAuditReport == null) {
            viewModel.runSecurityPrivacyAudit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Preferences",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val adaptive = com.example.ui.theme.rememberStudioAdaptiveInfo()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = adaptive.contentMaxWidth)
                    .testTag("settings_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Appearance (Theme)
            item {
                SettingsSectionCard(
                    title = "Appearance & Theme",
                    icon = Icons.Default.Palette
                ) {
                    Text(
                        text = "Choose app theme:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val selected = settings.themeMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.DARK -> Icons.Default.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // General
            item {
                SettingsSectionCard(
                    title = "General Defaults",
                    icon = Icons.Default.Tune
                ) {
                    // Default Format
                    Text(
                        text = "Default Output Format",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(ExportFormat.JPEG, ExportFormat.PNG, ExportFormat.WEBP_LOSSY).forEach { fmt ->
                            val isSel = settings.defaultExportFormat == fmt
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateAppSettings { it.copy(defaultExportFormat = fmt) } },
                                label = { Text(fmt.displayName.substringBefore('(')) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("default_format_${fmt.extension}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    // Default Quality Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default Export Quality",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${settings.defaultQuality}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.defaultQuality.toFloat(),
                        onValueChange = { viewModel.updateAppSettings { s -> s.copy(defaultQuality = it.toInt()) } },
                        valueRange = 10f..100f,
                        steps = 17,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_quality_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    // Default Save Location
                    Text(
                        text = "Default Save Location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutputDestinationType.values().forEach { dest ->
                            val isSel = settings.defaultDestinationType == dest
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateAppSettings { it.copy(defaultDestinationType = dest) } },
                                label = { Text(dest.displayName.substringBefore('(').trim(), maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("default_dest_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Editor
            item {
                SettingsSectionCard(
                    title = "Editor Behavior & Guides",
                    icon = Icons.Default.Crop
                ) {
                    SettingToggleRow(
                        title = "Show Grid Overlays by Default",
                        subtitle = "Displays 3x3 Rule of Thirds or Golden Ratio grids when opening images",
                        checked = settings.showGridByDefault,
                        onCheckedChange = { checked ->
                            viewModel.updateAppSettings { it.copy(showGridByDefault = checked) }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Show Horizon & Alignment Guides",
                        subtitle = "Displays leveling horizon bar and biometric alignment markers",
                        checked = settings.showGuidesByDefault,
                        onCheckedChange = { checked ->
                            viewModel.updateAppSettings { it.copy(showGuidesByDefault = checked) }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Lock Aspect Ratio by Default",
                        subtitle = "Maintains proportional width and height during manual dimension scaling",
                        checked = settings.defaultAspectRatioLock,
                        onCheckedChange = { checked ->
                            viewModel.updateAppSettings { it.copy(defaultAspectRatioLock = checked) }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingToggleRow(
                        title = "Live Interactive Preview Rendering",
                        subtitle = "Renders instant high-speed canvas preview transforms during adjustments",
                        checked = settings.liveInteractivePreview,
                        onCheckedChange = { checked ->
                            viewModel.updateAppSettings { it.copy(liveInteractivePreview = checked) }
                        }
                    )
                }
            }

            // Compression
            item {
                SettingsSectionCard(
                    title = "Compression Defaults",
                    icon = Icons.Default.Compress
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default Compression Quality",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${settings.defaultCompressionQuality}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.defaultCompressionQuality.toFloat(),
                        onValueChange = { q ->
                            viewModel.updateAppSettings { it.copy(defaultCompressionQuality = q.toInt()) }
                        },
                        valueRange = 10f..100f,
                        steps = 17,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Target Size Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 200, 500, 1024).forEach { kb ->
                            val isSel = settings.defaultTargetSizeKb == kb
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    viewModel.updateAppSettings { it.copy(defaultTargetSizeKb = kb) }
                                },
                                label = { Text(if (kb >= 1024) "1 MB" else "$kb KB") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Export & Filename Rules
            item {
                SettingsSectionCard(
                    title = "Export & Filename Rules",
                    icon = Icons.Default.SaveAlt
                ) {
                    Text(
                        text = "Default Filename Strategy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NamingMode.values().forEach { mode ->
                            val isSel = settings.defaultNamingMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateAppSettings { it.copy(defaultNamingMode = mode) }
                                    }
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSel,
                                        onClick = {
                                            viewModel.updateAppSettings { it.copy(defaultNamingMode = mode) }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = mode.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = "Automatic Filename Sanitization",
                        subtitle = "Strips illegal OS path symbols, spaces, control codes, and reserved DOS words",
                        checked = settings.autoSanitizeFilenames,
                        onCheckedChange = { checked ->
                            viewModel.updateAppSettings { it.copy(autoSanitizeFilenames = checked) }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Default Save Folder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = settings.defaultCustomFolderDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Privacy & Local Processing Information
            item {
                SettingsSectionCard(
                    title = "Privacy & Local Processing",
                    icon = Icons.Default.Security
                ) {
                    Text(
                        text = "Default Metadata Policy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            MetadataPolicy.KEEP_ALL to "Keep All",
                            MetadataPolicy.STRIP_ALL to "Strip All",
                            MetadataPolicy.CUSTOM_SELECTIVE to "Selective"
                        ).forEach { (policy, label) ->
                            val isSel = settings.defaultMetadataPolicy == policy
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    viewModel.updateAppSettings { it.copy(defaultMetadataPolicy = policy) }
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingToggleRow(
                        title = "Scrub EXIF & GPS Before Sharing",
                        subtitle = "Creates a privacy-scrubbed temporary copy in isolated cache before opening Android Share Sheet",
                        checked = uiState.scrubMetadataBeforeShare,
                        onCheckedChange = { checked ->
                            viewModel.setScrubMetadataBeforeShare(checked)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val auditReport = uiState.securityAuditReport
                    if (auditReport != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "8-Domain Security & Privacy Score",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(
                                            text = "${auditReport.overallScore}% ${auditReport.overallStatus.name}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Temp Cache: ${auditReport.tempCacheSummary.totalFileCount} file(s) (${auditReport.tempCacheSummary.totalBytes / 1024} KB) • Network Permissions: ${if (auditReport.hasInternetPermission) "Present" else "0 (100% Offline)"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.runSecurityPrivacyAudit()
                            showSecurityAuditDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_security_audit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspect Full 8-Domain Security & Privacy Audit")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.purgeTemporaryCache(zeroFill = true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purge_temp_cache_button")
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wipe Temp Cache", maxLines = 1, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showPrivacyResetConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("full_privacy_reset_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Privacy Reset", maxLines = 1, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showLocalProcessingDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local Processing Architecture Details")
                    }
                }
            }

            // About
            item {
                SettingsSectionCard(
                    title = "About Image Studio",
                    icon = Icons.Default.Info
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Image Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 1.0.0 (Build 2026.09)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = "100% Offline & Private",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyPolicyDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Privacy Policy")
                        }
                        OutlinedButton(
                            onClick = { showLicensesDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Licenses")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Help & Support")
                    }
                }
            }
        }
    }
    }

    // Dialog: Local Processing Information
    if (showLocalProcessingDialog) {
        AlertDialog(
            onDismissRequest = { showLocalProcessingDialog = false },
            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("100% Local Device Processing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Image Studio runs completely on your Android device hardware.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Zero Cloud Uploads: Your photos, sensitive documents, passports, and signatures never leave your phone.\n" +
                                "• No External Servers: All filtering, resizing, perspective correction, background manipulation, and compression algorithms execute entirely on local CPU/GPU buffers.\n" +
                                "• Sandboxed Persistence: Project history and temporary edit cache remain strictly within your device's private application storage."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocalProcessingDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // Dialog: Privacy Policy
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = { Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = null) },
            title = { Text("Privacy Policy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "At Image Studio, your privacy is our top priority.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. No Data Collection: We do not collect, transmit, analyze, or sell personal image data, EXIF metadata, or biometric metrics.\n" +
                                "2. On-Device Storage: Saved photos are written directly to your chosen Android gallery or folders via standard MediaStore and Storage Access Framework.\n" +
                                "3. Metadata Control: You have granular control to strip GPS, camera serials, and dates with our built-in EXIF privacy scrubber."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Open-Source Licenses
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            icon = { Icon(imageVector = Icons.Default.Code, contentDescription = null) },
            title = { Text("Open-Source Licenses") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Image Studio is built using open-source software:")
                    Text(
                        text = "• Android Jetpack & Compose (Apache 2.0 License)\n" +
                                "• Kotlin & Kotlinx Coroutines (Apache 2.0 License)\n" +
                                "• AndroidX Room SQLite Persistence (Apache 2.0 License)\n" +
                                "• AndroidX ExifInterface (Apache 2.0 License)\n" +
                                "• Material Design 3 Components (Apache 2.0 License)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Support
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            icon = { Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null) },
            title = { Text("Support & Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Need help or want to suggest a feature?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "For user inquiries, bug reports, or feature recommendations, feel free to reach out to our team at support@imagestudio.local or via your AI Studio developer dashboard."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Full 8-Domain Security & Privacy Audit Report
    if (showSecurityAuditDialog) {
        val report = uiState.securityAuditReport
        AlertDialog(
            onDismissRequest = { showSecurityAuditDialog = false },
            icon = { Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Security & Privacy Audit Report") },
            text = {
                if (report == null) {
                    Text("Running security & privacy audit...")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Overall Score: ${report.overallScore}% (${report.overallStatus.name})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(report.domainReports.size) { idx ->
                            val dom = report.domainReports[idx]
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${dom.domain.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dom.checksPassed}/${dom.totalChecks} Passed (${dom.score}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = dom.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    dom.findings.forEach { finding ->
                                        Text(
                                            text = finding,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityAuditDialog = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.purgeTemporaryCache(zeroFill = true) }) {
                    Text("Zero-Fill Temp Cache")
                }
            }
        )
    }

    // Dialog: Confirm Full Privacy Reset
    if (showPrivacyResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyResetConfirmDialog = false },
            icon = { Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Complete Local Privacy Reset?") },
            text = {
                Text(
                    text = "This will permanently clear all local processing history, remove saved favorite items, and securely overwrite & delete all temporary cache files."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performFullPrivacyReset()
                        showPrivacyResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wipe History & Temp Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                role = androidx.compose.ui.semantics.Role.Switch,
                onClickLabel = title,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
