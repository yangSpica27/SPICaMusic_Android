package me.spica27.spicamusic.storage.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.impl.dao.PlaylistDao
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.mapper.toCommon

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao,
    private val database: AppDatabase,
) : IPlaylistRepository {

    companion object {
        private const val PAGE_SIZE = 30
        private const val PREFETCH_DISTANCE = 10
        private const val SORT_ORDER_SCALE = 1_000_000L
    }

    override fun getAllPlaylistsPagingFlow(): Flow<PagingData<Playlist>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { playlistDao.getAllPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toCommon() }
    }

    override fun searchSongsByPlaylistId(playlistId: Long, keyword: String): Flow<List<Song>> =
        playlistDao.searchSongsByPlaylistId(playlistId, keyword)
            .map { list -> list.map { it.toCommon() } }
            .flowOn(Dispatchers.IO)

    override fun getSongsByPlaylistIdFlow(
        playlistId: Long,
        keyword: String
    ): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { playlistDao.getSongsPagingByPlaylistId(playlistId, keyword) }
    ).flow.map { pagingData ->
        pagingData.map { it.toCommon() }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSongsByPlaylistIdList(playlistId: Long): List<Song> =
        withContext(Dispatchers.IO) {
            playlistDao.getSongsByPlaylistId(playlistId).map { it.toCommon() }
        }

    override fun getPlaylistByIdFlow(playlistId: Long): Flow<Playlist?> =
        playlistDao.getPlayListByIdFlow(playlistId).map { it?.toCommon() }

    override fun getAllPlaylistsFlow(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylist().map { list -> list.map { it.toCommon() } }

    override fun getPlaylistsHavingSong(mediaId: Long): Flow<List<Playlist>> =
        playlistDao.getPlaylistsHaveSong(mediaId).map { list -> list.map { it.toCommon() } }

    override fun getPlaylistsNotHavingSong(mediaId: Long): Flow<List<Playlist>> =
        playlistDao.getPlaylistsNotHavingSong(mediaId).map { list -> list.map { it.toCommon() } }

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

    override suspend fun renamePlaylist(playlistId: Long, newName: String) =
        withContext(Dispatchers.IO) {
            playlistDao.renamePlaylist(playlistId, newName)
        }

    override suspend fun addSongToPlaylist(playlistId: Long, mediaId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val sortOrder = createSortOrders(playlistId, 1).first()
                playlistDao.insertListItem(
                    PlaylistSongCrossRefEntity(
                        playlistId = playlistId,
                        mediaId = mediaId,
                        sortOrder = sortOrder,
                    ),
                )
                playlistDao.setNeedUpdate(playlistId)
            }
        }

    override suspend fun removeSongFromPlaylist(playlistId: Long, mediaId: Long) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                playlistDao.deleteListItem(PlaylistSongCrossRefEntity(playlistId, mediaId))
                playlistDao.setNeedUpdate(playlistId)
            }
        }

    override suspend fun removeSongsFromPlaylist(playlistId: Long, mediaIds: List<Long>) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                mediaIds.forEach { mediaId ->
                    playlistDao.deleteListItem(PlaylistSongCrossRefEntity(playlistId, mediaId))
                }
                playlistDao.setNeedUpdate(playlistId)
            }
        }

    override suspend fun addSongsToPlaylist(playlistId: Long, mediaIds: List<Long>) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val sortOrders = createSortOrders(playlistId, mediaIds.size)
                playlistDao.insertListItems(
                    mediaIds.mapIndexed { index, mediaId ->
                        PlaylistSongCrossRefEntity(
                            playlistId = playlistId,
                            mediaId = mediaId,
                            sortOrder = sortOrders[index],
                        )
                    },
                )
                playlistDao.setNeedUpdate(playlistId)
            }
        }

    override suspend fun reorderPlaylistSong(
        playlistId: Long,
        fromMediaId: Long,
        toMediaId: Long,
        insertAfterTarget: Boolean,
    ) = withContext(Dispatchers.IO) {
        if (fromMediaId == toMediaId) return@withContext

        database.withTransaction {
            val orderedMediaIds = playlistDao.getMediaIdsByPlaylistId(playlistId).toMutableList()
            val fromIndex = orderedMediaIds.indexOf(fromMediaId)
            require(fromIndex >= 0) { "Song $fromMediaId is not in playlist $playlistId" }

            val movedMediaId = orderedMediaIds.removeAt(fromIndex)
            val targetIndex = orderedMediaIds.indexOf(toMediaId)
            require(targetIndex >= 0) { "Target song $toMediaId is not in playlist $playlistId" }

            val insertIndex = if (insertAfterTarget) targetIndex + 1 else targetIndex
            orderedMediaIds.add(insertIndex.coerceIn(0, orderedMediaIds.size), movedMediaId)

            val sortOrders = createSortOrders(playlistId, orderedMediaIds.size)
            orderedMediaIds.forEachIndexed { index, mediaId ->
                playlistDao.updateSortOrder(playlistId, mediaId, sortOrders[index])
            }
            playlistDao.setNeedUpdate(playlistId)
        }
    }

    override suspend fun reorderPlaylistSongs(playlistId: Long, orderedMediaIds: List<Long>) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val currentMediaIds = playlistDao.getMediaIdsByPlaylistId(playlistId)
                require(currentMediaIds.size == orderedMediaIds.size && currentMediaIds.toSet() == orderedMediaIds.toSet()) {
                    "Playlist $playlistId changed while sorting"
                }

                val sortOrders = createSortOrders(playlistId, orderedMediaIds.size)
                orderedMediaIds.forEachIndexed { index, mediaId ->
                    playlistDao.updateSortOrder(playlistId, mediaId, sortOrders[index])
                }
                playlistDao.setNeedUpdate(playlistId)
            }
        }

    override fun getPlaylistCoverAlbumIds(playlistId: Long): Flow<List<Long>> =
        playlistDao.getCoverAlbumIds(playlistId)

    override fun getSongSizeInPlaylist(playlistId: Long): Flow<Int> {
        return playlistDao.getSongSizeByPlaylistId(playlistId)
    }

    override suspend fun getSongSizeInPlaylistOnce(playlistId: Long): Int =
        withContext(Dispatchers.IO) {
            playlistDao.getSongSizeByPlaylistIdOnce(playlistId)
        }

    override fun getMediaIdsInPlaylist(playlistId: Long): List<Long> {
        return playlistDao.getMediaIdsByPlaylistId(playlistId)
    }

    private fun createSortOrders(playlistId: Long, count: Int): List<Long> {
        if (count <= 0) return emptyList()

        val nowBucket = System.currentTimeMillis() * SORT_ORDER_SCALE
        val currentMax = playlistDao.getMaxSortOrderByPlaylistId(playlistId)
        val start =
            if (currentMax != null && currentMax >= nowBucket) {
                currentMax + count
            } else {
                nowBucket + count - 1
            }

        return List(count) { index -> start - index }
    }
}
