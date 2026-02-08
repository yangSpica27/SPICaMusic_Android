package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.impl.dao.PlaylistDao
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.mapper.toCommon
import me.spica27.spicamusic.storage.impl.mapper.toEntity

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao,
    private val database: AppDatabase,
) : IPlaylistRepository {
    override fun getSongsByPlaylistIdFlow(playlistId: Long): Flow<List<Song>> = 
        playlistDao.getSongsByPlaylistIdFlow(playlistId).map { list -> list.map { it.toCommon() } }

    override fun getPlaylistByIdFlow(playlistId: Long): Flow<Playlist?> = 
        playlistDao.getPlayListByIdFlow(playlistId).map { it?.toCommon() }

    override fun getAllPlaylistsFlow(): Flow<List<Playlist>> = 
        playlistDao.getAllPlaylist().map { list -> list.map { it.toCommon() } }

    override fun getPlaylistsHavingSong(songId: Long): Flow<List<Playlist>> = 
        playlistDao.getPlaylistsHaveSong(songId).map { list -> list.map { it.toCommon() } }

    override fun getPlaylistsNotHavingSong(songId: Long): Flow<List<Playlist>> = 
        playlistDao.getPlaylistsNotHavingSong(songId).map { list -> list.map { it.toCommon() } }

    override fun getPlaylistWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?> = 
        playlistDao.getPlaylistsWithSongsWithPlayListIdFlow(playlistId)
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            .map { it?.toCommon() }

    override suspend fun incrementPlaylistPlayTime(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addPlayTimes(playlistId)
    }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylistAndGetId(PlaylistEntity(playlistName = name))
    }

    override suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deleteById(id)
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            playlistDao.insertListItem(PlaylistSongCrossRefEntity(playlistId, songId))
            playlistDao.setNeedUpdate(playlistId)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            playlistDao.deleteListItem(PlaylistSongCrossRefEntity(playlistId, songId))
            playlistDao.setNeedUpdate(playlistId)
        }
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            playlistDao.insertListItems(songIds.map { PlaylistSongCrossRefEntity(playlistId, it) })
            playlistDao.setNeedUpdate(playlistId)
        }
    }
}
