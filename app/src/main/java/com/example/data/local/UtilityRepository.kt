package com.example.data.local

import kotlinx.coroutines.flow.Flow

class UtilityRepository(private val database: AppDatabase) {
    val allHistory: Flow<List<HistoryEntity>> = database.historyDao().getAllHistory()
    val favorites: Flow<List<HistoryEntity>> = database.historyDao().getFavorites()
    val allPresets: Flow<List<PresetEntity>> = database.presetDao().getAllPresets()
    val allFavoriteItems: Flow<List<FavoriteItemEntity>> = database.favoriteDao().getAllFavorites()

    fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteItemEntity>> {
        return database.favoriteDao().getFavoritesByType(type)
    }

    fun isFavorite(type: FavoriteType, dataKey: String): Flow<Boolean> {
        return database.favoriteDao().isFavorite(type, dataKey)
    }

    suspend fun getFavoriteByTypeAndKey(type: FavoriteType, dataKey: String): FavoriteItemEntity? {
        return database.favoriteDao().getFavoriteByTypeAndKey(type, dataKey)
    }

    suspend fun insertFavorite(item: FavoriteItemEntity): Long {
        return database.favoriteDao().insertFavorite(item)
    }

    suspend fun updateFavorite(item: FavoriteItemEntity) {
        database.favoriteDao().updateFavorite(item)
    }

    suspend fun deleteFavorite(item: FavoriteItemEntity) {
        database.favoriteDao().deleteFavorite(item)
    }

    suspend fun deleteFavoriteByTypeAndKey(type: FavoriteType, dataKey: String) {
        database.favoriteDao().deleteFavoriteByTypeAndKey(type, dataKey)
    }

    suspend fun deleteFavoriteById(id: Long) {
        database.favoriteDao().deleteFavoriteById(id)
    }

    suspend fun clearFavoritesByType(type: FavoriteType) {
        database.favoriteDao().clearFavoritesByType(type)
    }

    suspend fun clearAllFavorites() {
        database.favoriteDao().clearAllFavorites()
    }

    suspend fun insertHistory(item: HistoryEntity): Long {

        return database.historyDao().insertHistory(item)
    }

    suspend fun updateHistory(item: HistoryEntity) {
        database.historyDao().updateHistory(item)
    }

    suspend fun deleteHistory(item: HistoryEntity) {
        database.historyDao().deleteHistory(item)
    }

    suspend fun deleteHistoryById(id: Long) {
        database.historyDao().deleteHistoryById(id)
    }

    suspend fun clearAllHistory() {
        database.historyDao().clearAllHistory()
    }

    suspend fun insertPreset(preset: PresetEntity): Long {
        return database.presetDao().insertPreset(preset)
    }

    suspend fun updatePreset(preset: PresetEntity) {
        database.presetDao().updatePreset(preset)
    }

    suspend fun deletePreset(preset: PresetEntity) {
        database.presetDao().deletePreset(preset)
    }

    suspend fun deletePresetById(id: Long) {
        database.presetDao().deletePresetById(id)
    }
}
