package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.mapper.toCommon

class SongRepositoryImpl(
    private val songDao: SongDao,
) : ISongRepository {
    override fun getAllSongsFlow(): Flow<List<Song>> = 
        songDao.getAll().map { list -> list.map { it.toCommon() } }
    
    override suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        songDao.getAllSync().map { it.toCommon() }
    }

    override fun getAllLikeSongsFlow(): Flow<List<Song>> = 
        songDao.getAllLikeSong().map { list -> list.map { it.toCommon() } }

    override fun getOftenListenSong10Flow(): Flow<List<Song>> = 
        songDao.getOftenListenSong10().map { list -> list.map { it.toCommon() } }

    override fun getOftenListenSongsFlow(): Flow<List<Song>> = 
        songDao.getOftenListenSongs().map { list -> list.map { it.toCommon() } }

    override fun getRandomSongFlow(): Flow<List<Song>> = 
        songDao.randomSong().map { list -> list.map { it.toCommon() } }

    override fun getSongFlowById(id: Long): Flow<Song?> = 
        songDao.getSongFlowWithId(id).map { it?.toCommon() }

    override suspend fun getSongByMediaStoreId(mediaStoreId: Long): Song? = withContext(Dispatchers.IO) {
        songDao.getSongWithMediaStoreId(mediaStoreId)?.toCommon()
    }

    override fun getSongsNotInPlaylistFlow(playlistId: Long): Flow<List<Song>> = 
        songDao.getSongsNotInPlayListFlow(playlistId).map { list -> list.map { it.toCommon() } }

    override suspend fun toggleLike(id: Long) = withContext(Dispatchers.IO) {
        songDao.toggleLike(id)
    }

    override suspend fun setIgnoreStatus(id: Long, isIgnore: Boolean) = withContext(Dispatchers.IO) {
        songDao.ignore(id, isIgnore)
    }

    override fun getSongLikeStatusFlow(id: Long): Flow<Boolean> = 
        songDao.getSongIsLikeFlowWithId(id).distinctUntilChanged().map { it == 1 }

    override fun getIgnoreSongsFlow(): Flow<List<Song>> = 
        songDao.getIgnoreSongsFlow().distinctUntilChanged().map { list -> list.map { it.toCommon() } }
}
