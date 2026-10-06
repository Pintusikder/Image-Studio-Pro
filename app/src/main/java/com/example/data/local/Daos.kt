package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity): Long

    @Update
    suspend fun updateHistory(item: HistoryEntity)

    @Delete
    suspend fun deleteHistory(item: HistoryEntity)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history_items")
    suspend fun clearAllHistory()
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM custom_presets ORDER BY id DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("DELETE FROM custom_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_items ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteItemEntity>>

    @Query("SELECT * FROM favorite_items WHERE type = :type ORDER BY timestamp DESC")
    fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_items WHERE type = :type AND dataKey = :dataKey)")
    fun isFavorite(type: FavoriteType, dataKey: String): Flow<Boolean>

    @Query("SELECT * FROM favorite_items WHERE type = :type AND dataKey = :dataKey LIMIT 1")
    suspend fun getFavoriteByTypeAndKey(type: FavoriteType, dataKey: String): FavoriteItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(item: FavoriteItemEntity): Long

    @Update
    suspend fun updateFavorite(item: FavoriteItemEntity)

    @Delete
    suspend fun deleteFavorite(item: FavoriteItemEntity)

    @Query("DELETE FROM favorite_items WHERE type = :type AND dataKey = :dataKey")
    suspend fun deleteFavoriteByTypeAndKey(type: FavoriteType, dataKey: String)

    @Query("DELETE FROM favorite_items WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("DELETE FROM favorite_items WHERE type = :type")
    suspend fun clearFavoritesByType(type: FavoriteType)

    @Query("DELETE FROM favorite_items")
    suspend fun clearAllFavorites()
}

