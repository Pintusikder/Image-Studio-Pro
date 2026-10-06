package com.example.data.local

/**
 * Manages Preset persistence and retrieval operations.
 */
class PresetManager(private val database: AppDatabase) {
    val allPresets = database.presetDao().getAllPresets()

    suspend fun insertPreset(preset: PresetEntity) = database.presetDao().insertPreset(preset)
    suspend fun updatePreset(preset: PresetEntity) = database.presetDao().updatePreset(preset)
    suspend fun deletePreset(preset: PresetEntity) = database.presetDao().deletePreset(preset)
    suspend fun deletePresetById(id: Long) = database.presetDao().deletePresetById(id)
}
