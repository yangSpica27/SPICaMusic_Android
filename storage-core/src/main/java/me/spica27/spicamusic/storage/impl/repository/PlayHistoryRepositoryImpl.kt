package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.impl.dao.PlayHistoryDao
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.mapper.toCommon

class PlayHistoryRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao,
) : IPlayHistoryRepository {
    override fun getAllPlayHistoryFlow(): Flow<List<PlayHistory>> = flowOf(emptyList())

    override fun getRecentPlayHistoryFlow(limit: Int): Flow<List<PlayHistory>> = flowOf(emptyList())

    override suspend fun addPlayHistory(songId: Long) = withContext(Dispatchers.IO) {
        playHistoryDao.insert(
            PlayHistoryEntity(
                mediaId = songId,
                time = System.currentTimeMillis()
            )
        )
    }
    
    override fun insertPlayHistory(item: PlayHistory) {
        // Sync wrapper for addPlayHistory
    }

    override suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        playHistoryDao.deleteAll()
    }

    override suspend fun deletePlayHistory(songId: Long) = withContext(Dispatchers.IO) {
        playHistoryDao.deleteByMediaId(songId)
    }
}
