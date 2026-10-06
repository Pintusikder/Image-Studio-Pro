package com.example.domain.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.FavoriteItemEntity
import com.example.data.local.FavoriteType
import com.example.data.local.HistoryEntity
import com.example.data.local.PresetEntity
import com.example.data.local.UtilityRepository
import com.example.model.ExportFormat
import com.example.model.MetadataPrivacyConfig
import com.example.model.OutputDestinationType
import com.example.model.OutputFileConfiguration
import com.example.model.OverwriteConflictAction
import com.example.processing.ExportEngine
import com.example.processing.FileSizeMode
import com.example.processing.OutputFileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Clean Architecture Use Case for Phase 11 — File Management:
 * Coordinates History, Favorites, Presets, Save As, Rename, Share, and Delete operations
 * with strict filename sanitization, non-destructive conflict resolution, and database consistency.
 */
class FileManagementUseCase {

    enum class HistorySortMode(val label: String) {
        NEWEST_FIRST("Newest First"),
        OLDEST_FIRST("Oldest First"),
        LARGEST_SIZE("Largest File Size"),
        SMALLEST_SIZE("Smallest File Size"),
        OPERATION_TYPE("Operation Type"),
        NAME_ASC("Name (A–Z)")
    }

    data class HistoryDeleteOutcome(
        val recordDeleted: Boolean,
        val physicalFileDeleted: Boolean,
        val freedBytes: Long = 0L,
        val message: String
    )

    data class HistoryRenameOutcome(
        val success: Boolean,
        val updatedEntity: HistoryEntity,
        val renameResult: OutputFileManager.FileRenameResult,
        val message: String
    )

    // ==========================================
    // 1. HISTORY MANAGEMENT
    // ==========================================

    fun filterAndSortHistory(
        allHistory: List<HistoryEntity>,
        category: String = "ALL",
        searchQuery: String = "",
        sortMode: HistorySortMode = HistorySortMode.NEWEST_FIRST
    ): List<HistoryEntity> {
        val baseList = when (category.uppercase(Locale.US)) {
            "FAVORITES" -> allHistory.filter { it.isFavorite }
            "RESIZE" -> allHistory.filter {
                it.operationType.contains("Resize", ignoreCase = true) ||
                    it.operationType.contains("Dimension", ignoreCase = true) ||
                    it.operationType.contains("Exact", ignoreCase = true)
            }
            "CROP" -> allHistory.filter { it.operationType.contains("Crop", ignoreCase = true) }
            "PRESETS" -> allHistory.filter {
                it.operationType.contains("Preset", ignoreCase = true) ||
                    it.operationType.contains("Social", ignoreCase = true) ||
                    it.operationType.contains("Passport", ignoreCase = true)
            }
            "BATCH" -> allHistory.filter { it.operationType.contains("Batch", ignoreCase = true) }
            else -> allHistory
        }

        val searchFiltered = if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase(Locale.US)
            baseList.filter { item ->
                item.title.lowercase(Locale.US).contains(q) ||
                    item.outputFileName.lowercase(Locale.US).contains(q) ||
                    (item.originalFileName?.lowercase(Locale.US)?.contains(q) == true) ||
                    item.operationType.lowercase(Locale.US).contains(q) ||
                    item.operationDetails.lowercase(Locale.US).contains(q) ||
                    item.format.lowercase(Locale.US).contains(q)
            }
        }

        return when (sortMode) {
            HistorySortMode.NEWEST_FIRST -> searchFiltered.sortedByDescending { it.timestamp }
            HistorySortMode.OLDEST_FIRST -> searchFiltered.sortedBy { it.timestamp }
            HistorySortMode.LARGEST_SIZE -> searchFiltered.sortedByDescending { it.fileSizeBytes }
            HistorySortMode.SMALLEST_SIZE -> searchFiltered.sortedBy { it.fileSizeBytes }
            HistorySortMode.OPERATION_TYPE -> searchFiltered.sortedBy { it.operationType.lowercase(Locale.US) }
            HistorySortMode.NAME_ASC -> searchFiltered.sortedBy {
                it.outputFileName.ifBlank { it.title }.lowercase(Locale.US)
            }
        }
    }

    suspend fun recordHistory(
        repository: UtilityRepository,
        item: HistoryEntity
    ): Long {
        val id = repository.insertHistory(item)
        return id
    }

    suspend fun toggleHistoryFavorite(
        repository: UtilityRepository,
        item: HistoryEntity
    ): HistoryEntity {
        val updated = item.copy(isFavorite = !item.isFavorite)
        repository.updateHistory(updated)
        return updated
    }

    suspend fun renameHistoryEntry(
        context: Context,
        repository: UtilityRepository,
        item: HistoryEntity,
        newRawName: String
    ): HistoryRenameOutcome {
        val ext = item.format.lowercase(Locale.US).let {
            if (it == "jpeg") "jpg" else it
        }
        val renameRes = if (item.outputUriString.isNotBlank()) {
            OutputFileManager.renameOutputFile(
                context = context,
                targetPathOrUri = item.outputUriString,
                newRawName = newRawName,
                fallbackExtension = ext
            )
        } else {
            val san = OutputFileManager.sanitizeFilename(newRawName.substringBeforeLast('.'))
            OutputFileManager.FileRenameResult(
                success = true,
                oldName = item.outputFileName.ifBlank { item.title },
                newName = "${san.sanitizedName}.$ext",
                sanitization = san
            )
        }

        val finalDisplayName = renameRes.newName
        val newUriStr = when {
            renameRes.newFile != null -> renameRes.newFile.absolutePath
            renameRes.newUri != null -> renameRes.newUri.toString()
            else -> item.outputUriString
        }

        val updatedEntity = item.copy(
            title = finalDisplayName.substringBeforeLast('.'),
            outputFileName = finalDisplayName,
            outputUriString = newUriStr
        )
        repository.updateHistory(updatedEntity)

        return HistoryRenameOutcome(
            success = true,
            updatedEntity = updatedEntity,
            renameResult = renameRes,
            message = "Renamed to '$finalDisplayName'"
        )
    }

    suspend fun deleteHistoryEntry(
        context: Context,
        repository: UtilityRepository,
        item: HistoryEntity,
        deletePhysicalFile: Boolean = false
    ): HistoryDeleteOutcome {
        var physicalDeleted = false
        var freedBytes = 0L
        if (deletePhysicalFile && item.outputUriString.isNotBlank()) {
            val delRes = OutputFileManager.deleteOutputFile(context, item.outputUriString)
            physicalDeleted = delRes.success
            freedBytes = delRes.freedBytes
        }
        repository.deleteHistory(item)
        val msg = if (deletePhysicalFile && physicalDeleted) {
            "Deleted history record and physical file '${item.outputFileName.ifBlank { item.title }}'"
        } else {
            "Deleted history record '${item.outputFileName.ifBlank { item.title }}'"
        }
        return HistoryDeleteOutcome(
            recordDeleted = true,
            physicalFileDeleted = physicalDeleted,
            freedBytes = freedBytes,
            message = msg
        )
    }

    // ==========================================
    // 2. FAVORITES MANAGEMENT
    // ==========================================

    fun filterFavorites(
        favorites: List<FavoriteItemEntity>,
        type: FavoriteType? = null,
        searchQuery: String = ""
    ): List<FavoriteItemEntity> {
        return favorites.filter { item ->
            val matchType = type == null || item.type == type
            val matchQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.dataKey.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }
    }

    suspend fun renameFavoriteItem(
        repository: UtilityRepository,
        item: FavoriteItemEntity,
        newTitle: String
    ): FavoriteItemEntity {
        val cleanTitle = OutputFileManager.sanitizeFilename(newTitle, fallback = item.title).sanitizedName
        val updated = item.copy(title = cleanTitle)
        repository.updateFavorite(updated)
        return updated
    }

    suspend fun recordFavoriteUsage(
        repository: UtilityRepository,
        item: FavoriteItemEntity
    ): FavoriteItemEntity {
        val updated = item.copy(
            usageCount = item.usageCount + 1,
            timestamp = System.currentTimeMillis()
        )
        repository.updateFavorite(updated)
        return updated
    }

    suspend fun deleteFavoriteItem(
        repository: UtilityRepository,
        item: FavoriteItemEntity
    ) {
        repository.deleteFavorite(item)
    }

    // ==========================================
    // 3. PRESETS MANAGEMENT
    // ==========================================

    suspend fun createCustomPreset(
        repository: UtilityRepository,
        name: String,
        category: String = "Custom",
        targetWidth: Int,
        targetHeight: Int,
        dpi: Int = 300,
        format: String = "JPG",
        maxFileSizeKb: Int? = null,
        quality: Int = 90,
        cropMode: String = "FIT"
    ): PresetEntity {
        val cleanName = name.trim().ifBlank { "Custom ${targetWidth}x${targetHeight}" }
        val entity = PresetEntity(
            name = cleanName,
            category = category,
            targetWidth = targetWidth.coerceIn(16, 16384),
            targetHeight = targetHeight.coerceIn(16, 16384),
            dpi = dpi.coerceIn(72, 1200),
            format = format.uppercase(Locale.US),
            maxFileSizeKb = maxFileSizeKb?.takeIf { it > 0 },
            quality = quality.coerceIn(5, 100),
            cropMode = cropMode
        )
        val id = repository.insertPreset(entity)
        return entity.copy(id = id)
    }

    suspend fun renameCustomPreset(
        repository: UtilityRepository,
        preset: PresetEntity,
        newName: String
    ): PresetEntity {
        val cleanName = newName.trim().ifBlank { preset.name }
        val updated = preset.copy(name = cleanName)
        repository.updatePreset(updated)
        return updated
    }

    suspend fun duplicateCustomPreset(
        repository: UtilityRepository,
        preset: PresetEntity,
        customName: String? = null
    ): PresetEntity {
        val dupName = customName?.trim()?.takeIf { it.isNotEmpty() } ?: "${preset.name} (Copy)"
        val copyEntity = preset.copy(id = 0, name = dupName)
        val newId = repository.insertPreset(copyEntity)
        return copyEntity.copy(id = newId)
    }

    suspend fun deleteCustomPreset(
        repository: UtilityRepository,
        preset: PresetEntity
    ) {
        repository.deletePreset(preset)
    }

    fun exportPresetsToJson(presets: List<PresetEntity>): String {
        val array = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject().apply {
                put("name", preset.name)
                put("category", preset.category)
                put("targetWidth", preset.targetWidth)
                put("targetHeight", preset.targetHeight)
                put("dpi", preset.dpi)
                put("format", preset.format)
                if (preset.maxFileSizeKb != null) put("maxFileSizeKb", preset.maxFileSizeKb)
                put("quality", preset.quality)
                put("cropMode", preset.cropMode)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    suspend fun importPresetsFromJson(
        repository: UtilityRepository,
        jsonString: String
    ): List<PresetEntity> {
        val imported = mutableListOf<PresetEntity>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val entity = PresetEntity(
                name = obj.optString("name", "Imported Preset ${i + 1}"),
                category = obj.optString("category", "Custom"),
                targetWidth = obj.optInt("targetWidth", 1080).coerceIn(16, 16384),
                targetHeight = obj.optInt("targetHeight", 1080).coerceIn(16, 16384),
                dpi = obj.optInt("dpi", 300).coerceIn(72, 1200),
                format = obj.optString("format", "JPG"),
                maxFileSizeKb = if (obj.has("maxFileSizeKb") && !obj.isNull("maxFileSizeKb")) obj.optInt("maxFileSizeKb") else null,
                quality = obj.optInt("quality", 90).coerceIn(5, 100),
                cropMode = obj.optString("cropMode", "FIT")
            )
            val id = repository.insertPreset(entity)
            imported.add(entity.copy(id = id))
        }
        return imported
    }

    // ==========================================
    // 4. SAVE AS
    // ==========================================

    fun executeSaveAs(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int = 90,
        targetSizeKb: Int? = null,
        fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
        allowDimensionAdjustment: Boolean = true,
        dpi: Int = 300,
        privacyConfig: MetadataPrivacyConfig = MetadataPrivacyConfig(),
        sourceUri: Uri? = null,
        originalFileName: String = "image",
        outputConfig: OutputFileConfiguration = OutputFileConfiguration(),
        forceOverwrite: Boolean = false
    ): ExportEngine.ExportResult {
        var resolvedFileName = OutputFileManager.generateFormattedFilename(
            config = outputConfig,
            originalName = originalFileName.ifBlank { "image" },
            width = bitmap.width,
            height = bitmap.height,
            dpi = dpi,
            format = format
        )

        if (!forceOverwrite && outputConfig.overwriteAction == OverwriteConflictAction.AUTO_RENAME_KEEP_BOTH) {
            resolvedFileName = OutputFileManager.generateUniqueFilename(
                context = context,
                destinationType = outputConfig.destinationType,
                safTreeUri = outputConfig.safTreeUri,
                fullFileName = resolvedFileName,
                mimeType = format.mimeType
            )
        }

        val baseNameWithoutExt = resolvedFileName.substringBeforeLast('.')

        return ExportEngine.exportImage(
            context = context,
            bitmap = bitmap,
            format = format,
            quality = quality,
            targetSizeKb = targetSizeKb,
            fileSizeMode = fileSizeMode,
            allowDimensionAdjustment = allowDimensionAdjustment,
            fileNamePrefix = baseNameWithoutExt,
            customExactFileName = baseNameWithoutExt,
            dpi = dpi,
            privacyConfig = privacyConfig,
            sourceUri = sourceUri,
            overwriteOriginal = forceOverwrite || outputConfig.overwriteAction == OverwriteConflictAction.REPLACE_OVERWRITE,
            destinationType = outputConfig.destinationType,
            safTreeUri = outputConfig.safTreeUri
        )
    }

    // ==========================================
    // 5. RENAME
    // ==========================================

    fun renameFile(
        context: Context,
        targetPathOrUri: String,
        newRawName: String,
        fallbackExtension: String = "jpg",
        allowOverwrite: Boolean = false
    ): OutputFileManager.FileRenameResult {
        return OutputFileManager.renameOutputFile(
            context = context,
            targetPathOrUri = targetPathOrUri,
            newRawName = newRawName,
            fallbackExtension = fallbackExtension,
            allowOverwrite = allowOverwrite
        )
    }

    // ==========================================
    // 6. SHARE
    // ==========================================

    fun prepareShareIntent(
        context: Context,
        targetPathOrUri: String,
        format: ExportFormat = ExportFormat.JPEG
    ): Intent? {
        return OutputFileManager.buildShareIntent(
            context = context,
            targetPathOrUri = targetPathOrUri,
            mimeType = format.mimeType
        )
    }

    fun shareFile(
        context: Context,
        targetPathOrUri: String,
        mimeType: String = "image/jpeg",
        title: String = "Share Image"
    ): Boolean {
        return OutputFileManager.shareUriOrFile(
            context = context,
            targetPathOrUri = targetPathOrUri,
            mimeType = mimeType,
            title = title
        )
    }

    // ==========================================
    // 7. DELETE
    // ==========================================

    fun deleteFile(
        context: Context,
        targetPathOrUri: String
    ): OutputFileManager.FileDeleteResult {
        return OutputFileManager.deleteOutputFile(
            context = context,
            targetPathOrUri = targetPathOrUri
        )
    }
}
