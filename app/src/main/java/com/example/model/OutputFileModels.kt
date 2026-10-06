package com.example.model

import android.net.Uri

enum class NamingMode(val displayName: String, val description: String) {
    AUTOMATIC("Smart Auto", "Generates clean descriptive name based on image properties and time"),
    ORIGINAL_WITH_SUFFIX("Original + Suffix", "Keeps original base name and appends custom suffix (e.g. _edited)"),
    CUSTOM_PATTERN("Pattern Template", "Custom tokenized template (e.g. {name}_{w}x{h}_{date})"),
    EXACT_CUSTOM("Exact Name", "User-specified exact base filename");

    val label: String get() = displayName

    companion object {
        val AUTOMATIC_TIMESTAMP: NamingMode get() = AUTOMATIC
        val ORIGINAL_PRESERVED: NamingMode get() = ORIGINAL_WITH_SUFFIX
        val PATTERN_TEMPLATE: NamingMode get() = CUSTOM_PATTERN
        val CUSTOM_EXACT: NamingMode get() = EXACT_CUSTOM
    }
}

enum class OutputDestinationType(val displayName: String, val description: String) {
    PUBLIC_MEDIASTORE("Public MediaStore (Gallery)", "Standard Pictures/ImageStudio & Documents/ImageStudio"),
    CUSTOM_SAF_DIRECTORY("Custom Folder (SAF)", "User-selected directory using Android Storage Access Framework"),
    SYSTEM_SAVE_AS("System Save As Picker", "Always prompt using Android System File Picker (CreateDocument)");

    val label: String get() = displayName
}

enum class OverwriteConflictAction(val displayName: String, val description: String) {
    ASK_BEFORE_OVERWRITE("Ask Confirmation", "Prompt user before overwriting if a file with same name exists"),
    AUTO_RENAME_KEEP_BOTH("Keep Both (Auto-Rename)", "Automatically append (1), (2) to guarantee non-destructive saving"),
    REPLACE_OVERWRITE("Overwrite / Replace", "Directly replace existing file with same name");

    val label: String get() = displayName

    companion object {
        val CANCEL: OverwriteConflictAction get() = ASK_BEFORE_OVERWRITE
    }
}

data class FilenameSanitizationResult(
    val sanitizedName: String = "image_export",
    val originalInput: String = "",
    val isValid: Boolean = true,
    val hasPathTraversal: Boolean = false,
    val hasIllegalCharacters: Boolean = false,
    val isReservedName: Boolean = false,
    val isTruncated: Boolean = false,
    val warnings: List<String> = emptyList()
) {
    val isClean: Boolean get() = warnings.isEmpty() && sanitizedName == originalInput
    val warningMessage: String get() = warnings.joinToString(", ")
}

data class OutputFileConfiguration(
    val namingMode: NamingMode = NamingMode.AUTOMATIC,
    val customTemplate: String = "{name}_edited",
    val customExactName: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val includeDimensions: Boolean = false,
    val includeTimestamp: Boolean = false,
    val includeDpi: Boolean = false,
    val destinationType: OutputDestinationType = OutputDestinationType.PUBLIC_MEDIASTORE,
    val safTreeUriString: String? = null,
    val safTreeDisplayName: String? = null,
    val overwriteAction: OverwriteConflictAction = OverwriteConflictAction.ASK_BEFORE_OVERWRITE
) {
    val safTreeUri: Uri? get() = safTreeUriString?.let { Uri.parse(it) }
    val safFolderDisplayName: String? get() = safTreeDisplayName
    val overwriteConflictAction: OverwriteConflictAction get() = overwriteAction
    val patternTemplate: String get() = customTemplate

    fun copyWithPattern(pattern: String): OutputFileConfiguration = copy(customTemplate = pattern)
    fun copyWithSafTree(uri: String?, displayName: String?): OutputFileConfiguration =
        copy(safTreeUriString = uri, safTreeDisplayName = displayName)
}

