package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Export",
    val originalUriString: String? = null,
    val originalFileName: String? = null,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalFileSizeBytes: Long = 0,
    val originalThumbnailBase64: String? = null,
    val outputUriString: String = "",
    val outputFileName: String = "",
    val operationType: String = "Export",
    val operationDetails: String = "",
    val operationSettingsJson: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0,
    val format: String = "JPEG",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val previewThumbnailBase64: String? = null
)

@Entity(tableName = "custom_presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Custom",
    val targetWidth: Int,
    val targetHeight: Int,
    val dpi: Int = 300,
    val format: String = "JPG",
    val maxFileSizeKb: Int? = null,
    val quality: Int = 90,
    val cropMode: String = "FIT"
)

enum class FavoriteType {
    IMAGE,
    PRESET,
    RECENT_TOOL,
    CUSTOM_DIMENSION,
    COMPRESSION_TARGET
}

@Entity(tableName = "favorite_items")
data class FavoriteItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: FavoriteType,
    val title: String,
    val subtitle: String = "",
    val dataKey: String, // Unique identifier e.g. "DIM_1920_1080", "COMP_100KB", "TOOL_CROP", "PRESET_IG_POST", URI
    val width: Int? = null,
    val height: Int? = null,
    val dpi: Int? = null,
    val unit: String? = null,
    val targetFileSizeKb: Int? = null,
    val quality: Int? = null,
    val format: String? = null,
    val imageUriString: String? = null,
    val thumbnailBase64: String? = null,
    val extraJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

