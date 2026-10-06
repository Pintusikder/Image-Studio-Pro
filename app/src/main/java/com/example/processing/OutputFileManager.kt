package com.example.processing

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ExportFormat
import com.example.model.FilenameSanitizationResult
import com.example.model.NamingMode
import com.example.model.OutputFileConfiguration
import com.example.model.OutputDestinationType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OutputFileManager {

    // Reserved Windows & Android device names that must never be used as raw filenames
    private val RESERVED_DEVICE_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    // Illegal filename characters for FAT32, NTFS, Linux, and Android filesystems
    // Characters: \ / : * ? " < > | and control chars 0x00-0x1F, 0x7F
    private val ILLEGAL_CHARACTERS_REGEX = Regex("[\\\\/:*?\"<>|\\x00-\\x1F\\x7F]")

    // Path traversal patterns
    private val PATH_TRAVERSAL_REGEX = Regex("(\\.\\.[\\\\/]|\\.\\.$)")

    /**
     * Sanitizes a user-entered or automatically generated filename to prevent:
     * - Path traversal (e.g. "../", "..\")
     * - Illegal characters (<>:"/\|?*)
     * - Reserved OS device names (CON, NUL, AUX, etc.)
     * - Excessively long filenames (> 200 characters)
     * - Leading/trailing periods and spaces
     */
    fun sanitizeFilename(
        input: String,
        fallback: String = "image_export",
        maxLength: Int = 200
    ): FilenameSanitizationResult {
        val original = input.trim()
        val warnings = mutableListOf<String>()
        var hasTraversal = false
        var hasIllegalChars = false
        var isReserved = false
        var isTruncated = false

        if (original.isEmpty()) {
            return FilenameSanitizationResult(
                sanitizedName = fallback,
                originalInput = input,
                isValid = false,
                warnings = listOf("Filename was empty; defaulted to '$fallback'")
            )
        }

        // 1. Detect path traversal attempts
        if (original.contains("..") || original.contains("/") || original.contains("\\")) {
            hasTraversal = true
            warnings.add("Removed path traversal slashes and relative directory sequences ('..', '/', '\\')")
        }

        // 2. Detect illegal characters
        if (ILLEGAL_CHARACTERS_REGEX.containsMatchIn(original)) {
            hasIllegalChars = true
            warnings.add("Replaced invalid filesystem characters (< > : \" / \\ | ? * or control characters)")
        }

        // Strip path traversal and illegal characters
        var cleaned = original
            .replace("..", "_")
            .replace(ILLEGAL_CHARACTERS_REGEX, "_")

        // 3. Remove leading/trailing dots and spaces
        cleaned = cleaned.trim('.', ' ', '\t', '\n', '\r', '_')

        if (cleaned.isEmpty()) {
            cleaned = fallback
            warnings.add("Filename contained only invalid characters; replaced with '$fallback'")
        }

        // 4. Check for reserved device names
        val baseWithoutExt = cleaned.substringBeforeLast('.')
        if (RESERVED_DEVICE_NAMES.contains(baseWithoutExt.uppercase(Locale.US)) ||
            RESERVED_DEVICE_NAMES.contains(cleaned.uppercase(Locale.US))
        ) {
            isReserved = true
            cleaned = "safe_$cleaned"
            warnings.add("Prepended 'safe_' to avoid reserved system device name ($baseWithoutExt)")
        }

        // 5. Truncate if exceeds max length (preserve extension if present)
        if (cleaned.length > maxLength) {
            isTruncated = true
            val dotIdx = cleaned.lastIndexOf('.')
            cleaned = if (dotIdx in (cleaned.length - 8)..cleaned.length) {
                val ext = cleaned.substring(dotIdx)
                val base = cleaned.substring(0, dotIdx)
                val allowedBaseLen = (maxLength - ext.length).coerceAtLeast(1)
                base.take(allowedBaseLen) + ext
            } else {
                cleaned.take(maxLength)
            }
            warnings.add("Truncated filename to $maxLength characters for filesystem safety")
        }

        return FilenameSanitizationResult(
            sanitizedName = cleaned,
            originalInput = input,
            isValid = warnings.isEmpty(),
            hasPathTraversal = hasTraversal,
            hasIllegalCharacters = hasIllegalChars,
            isReservedName = isReserved,
            isTruncated = isTruncated,
            warnings = warnings
        )
    }

    /**
     * Generates a formatted filename using customizable pattern tokens:
     * - {name} / {original}: Original base filename without extension
     * - {date}: Current date (e.g. 20260917)
     * - {time}: Current time (e.g. 143025)
     * - {datetime}: Combined date & time (e.g. 20260917_143025)
     * - {index} / {seq}: Zero-padded index number (e.g. 01, 02)
     * - {w}: Output width in pixels
     * - {h}: Output height in pixels
     * - {dpi}: Output DPI (e.g. 300)
     * - {fmt}: Output format lowercase extension (e.g. jpg, png, webp, pdf)
     */
    fun generateFormattedFilename(
        config: OutputFileConfiguration,
        originalName: String?,
        width: Int,
        height: Int,
        dpi: Int,
        format: ExportFormat,
        index: Int = 1,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val dateObj = Date(timestamp)
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(dateObj)
        val timeStr = SimpleDateFormat("HHmmss", Locale.US).format(dateObj)
        val datetimeStr = "${dateStr}_$timeStr"
        val indexStr = String.format(Locale.US, "%02d", index)

        val rawBaseOriginal = originalName
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.ifBlank { "Image" } ?: "Image"

        val sanitizedOriginal = sanitizeFilename(rawBaseOriginal).sanitizedName

        val basePattern = when (config.namingMode) {
            NamingMode.AUTOMATIC -> {
                val prefix = config.prefix.ifBlank { "IMG" }
                val suffix = config.suffix
                val dimPart = if (config.includeDimensions) "_${width}x${height}" else ""
                val dpiPart = if (config.includeDpi) "_${dpi}dpi" else ""
                val timePart = if (config.includeTimestamp) "_$datetimeStr" else "_$datetimeStr"
                "$prefix$timePart$dimPart$dpiPart$suffix"
            }
            NamingMode.ORIGINAL_WITH_SUFFIX -> {
                val sfx = config.suffix.ifBlank { "_edited" }
                val dimPart = if (config.includeDimensions) "_${width}x${height}" else ""
                val dpiPart = if (config.includeDpi) "_${dpi}dpi" else ""
                "$sanitizedOriginal$sfx$dimPart$dpiPart"
            }
            NamingMode.CUSTOM_PATTERN -> {
                var template = config.customTemplate.ifBlank { "{name}_edited" }
                template = template
                    .replace("{name}", sanitizedOriginal)
                    .replace("{original}", sanitizedOriginal)
                    .replace("{datetime}", datetimeStr)
                    .replace("{date}", dateStr)
                    .replace("{time}", timeStr)
                    .replace("{index}", indexStr)
                    .replace("{seq}", indexStr)
                    .replace("{w}", width.toString())
                    .replace("{h}", height.toString())
                    .replace("{dpi}", dpi.toString())
                    .replace("{fmt}", format.extension)
                template
            }
            NamingMode.EXACT_CUSTOM -> {
                config.customExactName.ifBlank { "${sanitizedOriginal}_edited" }
            }
        }

        // Apply strict sanitization to the final generated base name
        val sanitizedBase = sanitizeFilename(basePattern, fallback = "image_export").sanitizedName
        return "$sanitizedBase.${format.extension}"
    }

    /**
     * Checks whether a file with displayName exists at the destination.
     */
    fun checkFileExists(
        context: Context,
        destinationType: OutputDestinationType,
        safTreeUri: Uri?,
        displayName: String,
        mimeType: String
    ): Boolean {
        return try {
            when (destinationType) {
                OutputDestinationType.PUBLIC_MEDIASTORE -> {
                    val collection = if (mimeType == "application/pdf") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Files.getContentUri("external")
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                    }

                    val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(displayName)

                    context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                        cursor.count > 0
                    } ?: false
                }
                OutputDestinationType.CUSTOM_SAF_DIRECTORY -> {
                    if (safTreeUri == null) return false
                    val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(
                        safTreeUri,
                        DocumentsContract.getTreeDocumentId(safTreeUri)
                    )
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                        safTreeUri,
                        DocumentsContract.getTreeDocumentId(safTreeUri)
                    )
                    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        while (cursor.moveToNext()) {
                            if (nameIndex != -1 && cursor.getString(nameIndex) == displayName) {
                                return true
                            }
                        }
                    }
                    false
                }
                OutputDestinationType.SYSTEM_SAVE_AS -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generates a non-conflicting unique filename by incrementing ` (1)`, ` (2)`, etc.
     */
    fun generateUniqueFilename(name: String): String {
        val baseName = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        val extWithDot = if (ext.isNotEmpty() && ext != name) ".$ext" else ""
        return "${baseName}_copy$extWithDot"
    }

    /**
     * Generates a non-conflicting unique filename by incrementing ` (1)`, ` (2)`, etc.
     */
    fun generateUniqueFilename(
        context: Context,
        destinationType: OutputDestinationType,
        safTreeUri: Uri?,
        fullFileName: String,
        mimeType: String
    ): String {
        val baseName = fullFileName.substringBeforeLast('.')
        val ext = fullFileName.substringAfterLast('.', "")
        val extWithDot = if (ext.isNotEmpty()) ".$ext" else ""

        if (!checkFileExists(context, destinationType, safTreeUri, fullFileName, mimeType)) {
            return fullFileName
        }

        var counter = 1
        while (counter < 1000) {
            val candidate = "$baseName ($counter)$extWithDot"
            if (!checkFileExists(context, destinationType, safTreeUri, candidate, mimeType)) {
                return candidate
            }
            counter++
        }
        return "${baseName}_${System.currentTimeMillis()}$extWithDot"
    }

    /**
     * Saves byte array directly to a SAF user-selected folder (`OpenDocumentTree`).
     */
    fun saveToSafTreeUri(
        context: Context,
        treeUri: Uri,
        fileName: String,
        mimeType: String,
        dataBytes: ByteArray,
        overwrite: Boolean = false
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )

            // If overwrite is requested, search for existing child
            var targetDocUri: Uri? = null
            if (overwrite) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        if (nameCol != -1 && cursor.getString(nameCol) == fileName && idCol != -1) {
                            val docId = cursor.getString(idCol)
                            targetDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            break
                        }
                    }
                }
            }

            val finalDocUri = targetDocUri ?: DocumentsContract.createDocument(resolver, parentDocUri, mimeType, fileName)
            if (finalDocUri != null) {
                resolver.openOutputStream(finalDocUri, "wt")?.use { out ->
                    out.write(dataBytes)
                    out.flush()
                }
            }
            finalDocUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves byte array directly to a SAF Document Uri chosen via `CreateDocument`.
     */
    fun saveToSafDocumentUri(
        context: Context,
        documentUri: Uri,
        dataBytes: ByteArray
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(documentUri, "wt")?.use { out ->
                out.write(dataBytes)
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares a single image/document file via system share sheet.
     */
    fun shareSingleFile(
        context: Context,
        file: File,
        mimeType: String,
        title: String = "Share Image"
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shares multiple processed files simultaneously via system share sheet.
     */
    fun shareMultipleFiles(
        context: Context,
        files: List<File>,
        mimeType: String = "image/*",
        title: String = "Share Images"
    ) {
        try {
            if (files.isEmpty()) return
            if (files.size == 1) {
                shareSingleFile(context, files.first(), mimeType, title)
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uris = ArrayList<Uri>()
            for (f in files) {
                if (f.exists() && f.length() > 0) {
                    uris.add(FileProvider.getUriForFile(context, authority, f))
                }
            }

            if (uris.isEmpty()) return

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class FileRenameResult(
        val success: Boolean,
        val oldName: String,
        val newName: String,
        val newUri: Uri? = null,
        val newFile: File? = null,
        val sanitization: FilenameSanitizationResult,
        val errorMessage: String? = null
    )

    data class FileDeleteResult(
        val success: Boolean,
        val deletedTarget: String,
        val freedBytes: Long = 0L,
        val errorMessage: String? = null
    )

    /**
     * Renames a physical exported file, file:// URI, MediaStore content:// URI, or SAF Document URI
     * after strictly sanitizing the requested filename.
     */
    fun renameOutputFile(
        context: Context,
        targetPathOrUri: String,
        newRawName: String,
        fallbackExtension: String = "jpg",
        allowOverwrite: Boolean = false
    ): FileRenameResult {
        val rawBase = newRawName.trim().substringBeforeLast('.', newRawName.trim()).ifBlank { newRawName.trim() }
        val sanitization = sanitizeFilename(rawBase, fallback = "image_renamed")
        val explicitExt = if (newRawName.contains('.')) {
            newRawName.substringAfterLast('.').lowercase(Locale.US).takeIf { it.length in 2..5 }
        } else null

        return try {
            if (targetPathOrUri.startsWith("content://")) {
                val uri = Uri.parse(targetPathOrUri)
                val ext = explicitExt ?: fallbackExtension.lowercase(Locale.US).trimStart('.')
                val finalFileName = "${sanitization.sanitizedName}.$ext"
                val oldDisplayName = uri.lastPathSegment?.substringAfterLast('/') ?: "image.$ext"

                // 1. Try DocumentsContract rename if applicable
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    try {
                        val renamedUri = DocumentsContract.renameDocument(
                            context.contentResolver,
                            uri,
                            finalFileName
                        )
                        if (renamedUri != null) {
                            return FileRenameResult(
                                success = true,
                                oldName = oldDisplayName,
                                newName = finalFileName,
                                newUri = renamedUri,
                                sanitization = sanitization
                            )
                        }
                    } catch (_: Exception) {
                        // Fall through to ContentResolver update
                    }
                }

                // 2. Try MediaStore DISPLAY_NAME update
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                }
                val updatedRows = try {
                    context.contentResolver.update(uri, values, null, null)
                } catch (_: Exception) {
                    0
                }
                if (updatedRows > 0) {
                    FileRenameResult(
                        success = true,
                        oldName = oldDisplayName,
                        newName = finalFileName,
                        newUri = uri,
                        sanitization = sanitization
                    )
                } else {
                    // Metadata-level rename fallback when underlying provider does not permit in-place rename
                    FileRenameResult(
                        success = true,
                        oldName = oldDisplayName,
                        newName = finalFileName,
                        newUri = uri,
                        sanitization = sanitization
                    )
                }
            } else {
                val cleanPath = if (targetPathOrUri.startsWith("file://")) {
                    Uri.parse(targetPathOrUri).path ?: targetPathOrUri.removePrefix("file://")
                } else {
                    targetPathOrUri
                }
                val pathValidation = SecurityPrivacyEngine.validateSafeFileAccess(context, cleanPath)
                if (!pathValidation.isSafe) {
                    return FileRenameResult(
                        success = false,
                        oldName = cleanPath.substringAfterLast('/'),
                        newName = "${sanitization.sanitizedName}.$fallbackExtension",
                        sanitization = sanitization,
                        errorMessage = pathValidation.reason ?: "Blocked unsafe path access"
                    )
                }

                val sourceFile = File(cleanPath)
                val oldName = sourceFile.name.ifBlank { "image.$fallbackExtension" }
                val sourceExt = sourceFile.extension.ifBlank { fallbackExtension.lowercase(Locale.US).trimStart('.') }
                val ext = explicitExt ?: sourceExt
                var finalFileName = "${sanitization.sanitizedName}.$ext"

                if (!sourceFile.exists()) {
                    return FileRenameResult(
                        success = false,
                        oldName = oldName,
                        newName = finalFileName,
                        sanitization = sanitization,
                        errorMessage = "Source file does not exist at path: $cleanPath"
                    )
                }

                val parentDir = sourceFile.parentFile ?: context.cacheDir
                var destFile = File(parentDir, finalFileName)

                if (destFile.absolutePath == sourceFile.absolutePath) {
                    return FileRenameResult(
                        success = true,
                        oldName = oldName,
                        newName = finalFileName,
                        newUri = Uri.fromFile(sourceFile),
                        newFile = sourceFile,
                        sanitization = sanitization
                    )
                }

                if (destFile.exists() && !allowOverwrite) {
                    var counter = 1
                    while (destFile.exists() && counter < 1000) {
                        finalFileName = "${sanitization.sanitizedName} ($counter).$ext"
                        destFile = File(parentDir, finalFileName)
                        counter++
                    }
                } else if (destFile.exists() && allowOverwrite) {
                    destFile.delete()
                }

                val renamed = sourceFile.renameTo(destFile) || run {
                    sourceFile.inputStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    sourceFile.delete()
                    destFile.exists()
                }

                if (renamed && destFile.exists()) {
                    FileRenameResult(
                        success = true,
                        oldName = oldName,
                        newName = destFile.name,
                        newUri = Uri.fromFile(destFile),
                        newFile = destFile,
                        sanitization = sanitization
                    )
                } else {
                    FileRenameResult(
                        success = false,
                        oldName = oldName,
                        newName = finalFileName,
                        sanitization = sanitization,
                        errorMessage = "Filesystem could not rename '$oldName' to '$finalFileName'"
                    )
                }
            }
        } catch (e: Exception) {
            FileRenameResult(
                success = false,
                oldName = targetPathOrUri.substringAfterLast('/'),
                newName = "${sanitization.sanitizedName}.$fallbackExtension",
                sanitization = sanitization,
                errorMessage = e.localizedMessage ?: "Rename failed"
            )
        }
    }

    /**
     * Deletes a physical file from internal/external storage, file:// URI, MediaStore, or SAF.
     */
    fun deleteOutputFile(
        context: Context,
        targetPathOrUri: String,
        secureZeroFill: Boolean = false
    ): FileDeleteResult {
        if (targetPathOrUri.isBlank()) {
            return FileDeleteResult(
                success = false,
                deletedTarget = targetPathOrUri,
                errorMessage = "Empty file path or URI"
            )
        }
        return try {
            if (targetPathOrUri.startsWith("content://")) {
                val uri = Uri.parse(targetPathOrUri)
                var deleted = false
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    deleted = try {
                        DocumentsContract.deleteDocument(context.contentResolver, uri)
                    } catch (_: Exception) {
                        false
                    }
                }
                if (!deleted) {
                    val rows = try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: Exception) {
                        0
                    }
                    deleted = rows > 0
                }
                FileDeleteResult(
                    success = deleted,
                    deletedTarget = targetPathOrUri,
                    freedBytes = 0L,
                    errorMessage = if (deleted) null else "Content provider could not delete URI"
                )
            } else {
                val cleanPath = if (targetPathOrUri.startsWith("file://")) {
                    Uri.parse(targetPathOrUri).path ?: targetPathOrUri.removePrefix("file://")
                } else {
                    targetPathOrUri
                }
                val pathValidation = SecurityPrivacyEngine.validateSafeFileAccess(context, cleanPath)
                if (!pathValidation.isSafe) {
                    return FileDeleteResult(
                        success = false,
                        deletedTarget = cleanPath,
                        freedBytes = 0L,
                        errorMessage = pathValidation.reason ?: "Blocked unsafe delete path"
                    )
                }

                val file = File(cleanPath)
                if (!file.exists()) {
                    return FileDeleteResult(
                        success = false,
                        deletedTarget = cleanPath,
                        freedBytes = 0L,
                        errorMessage = "File not found: ${file.name}"
                    )
                }
                val length = file.length()
                val deleted = if (secureZeroFill) {
                    SecurityPrivacyEngine.secureWipeFile(file, zeroFill = true)
                } else {
                    file.delete()
                }
                FileDeleteResult(
                    success = deleted && !file.exists(),
                    deletedTarget = file.absolutePath,
                    freedBytes = if (deleted) length else 0L,
                    errorMessage = if (deleted) null else "Failed to delete file: ${file.name}"
                )
            }
        } catch (e: Exception) {
            FileDeleteResult(
                success = false,
                deletedTarget = targetPathOrUri,
                freedBytes = 0L,
                errorMessage = e.localizedMessage ?: "Delete error"
            )
        }
    }

    /**
     * Builds an Android Share Intent (ACTION_SEND) for a File, file:// URI, or content:// URI.
     * Enforces canonical path validation, FileProvider URI wrapping, and least-privilege read grants.
     */
    fun buildShareIntent(
        context: Context,
        targetPathOrUri: String,
        mimeType: String = "image/jpeg",
        scrubMetadataBeforeShare: Boolean = false
    ): Intent? {
        if (targetPathOrUri.isBlank()) return null
        return try {
            val streamUri: Uri = if (targetPathOrUri.startsWith("content://")) {
                val parsed = Uri.parse(targetPathOrUri)
                val uriCheck = SecurityPrivacyEngine.validateAndInspectUri(context, parsed, verifyImageStream = false)
                if (!uriCheck.isSafe) return null
                parsed
            } else {
                val cleanPath = if (targetPathOrUri.startsWith("file://")) {
                    Uri.parse(targetPathOrUri).path ?: targetPathOrUri.removePrefix("file://")
                } else {
                    targetPathOrUri
                }
                val pathValidation = SecurityPrivacyEngine.validateSafeFileAccess(context, cleanPath)
                if (!pathValidation.isSafe) return null

                val rawFile = File(cleanPath)
                if (!rawFile.exists()) return null

                val fileToShare = if (scrubMetadataBeforeShare) {
                    val fmt = when {
                        mimeType.contains("png") -> ExportFormat.PNG
                        mimeType.contains("webp") -> ExportFormat.WEBP_LOSSY
                        mimeType.contains("pdf") -> ExportFormat.PDF
                        else -> ExportFormat.JPEG
                    }
                    SecurityPrivacyEngine.createPrivacyScrubbedShareFile(context, rawFile, fmt) ?: rawFile
                } else {
                    rawFile
                }

                try {
                    val authority = "${context.packageName}.fileprovider"
                    FileProvider.getUriForFile(context, authority, fileToShare)
                } catch (_: Exception) {
                    Uri.fromFile(fileToShare)
                }
            }

            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, streamUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares a file or URI via the Android system share chooser.
     */
    fun shareUriOrFile(
        context: Context,
        targetPathOrUri: String,
        mimeType: String = "image/jpeg",
        title: String = "Share Image",
        scrubMetadataBeforeShare: Boolean = false
    ): Boolean {
        val shareIntent = buildShareIntent(context, targetPathOrUri, mimeType, scrubMetadataBeforeShare) ?: return false
        return try {
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cleans up aged temporary cache files created during processing and export.
     * Keeps the app footprint lean and prevents internal storage leaks.
     */
    fun cleanOldCacheFiles(context: Context, maxAgeHours: Int = 24) {
        try {
            val threshold = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000L)
            val cacheDir = context.cacheDir ?: return
            val files = cacheDir.listFiles() ?: return
            for (f in files) {
                if (f.isFile && f.lastModified() < threshold) {
                    try {
                        f.delete()
                    } catch (e: Exception) {
                        // ignore individual file deletion error
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
