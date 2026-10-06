package com.example.data.local

/**
 * Manages History persistence, undo snapshots, and recents cache.
 */
class HistoryManager(private val database: AppDatabase) {
    val allHistory = database.historyDao().getAllHistory()
    val favorites = database.historyDao().getFavorites()

    suspend fun insertHistory(item: HistoryEntity) = database.historyDao().insertHistory(item)
    suspend fun updateHistory(item: HistoryEntity) = database.historyDao().updateHistory(item)
    suspend fun deleteHistory(item: HistoryEntity) = database.historyDao().deleteHistory(item)
    suspend fun clearHistory() = database.historyDao().clearAllHistory()
}
