package com.example.model

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppSettings(
    // General
    val defaultExportFormat: ExportFormat = ExportFormat.JPEG,
    val defaultQuality: Int = 90,
    val defaultDestinationType: OutputDestinationType = OutputDestinationType.PUBLIC_MEDIASTORE,
    // Editor
    val showGridByDefault: Boolean = true,
    val showGuidesByDefault: Boolean = true,
    val defaultAspectRatioLock: Boolean = true,
    val liveInteractivePreview: Boolean = true,
    // Compression
    val defaultCompressionQuality: Int = 85,
    val defaultTargetSizeKb: Int? = 500,
    // Export
    val defaultMetadataPolicy: MetadataPolicy = MetadataPolicy.KEEP_ALL,
    val autoSanitizeFilenames: Boolean = true,
    val defaultNamingMode: NamingMode = NamingMode.AUTOMATIC,
    val defaultCustomFolderDisplayName: String = "Pictures/ImageStudio",
    // Appearance
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)
