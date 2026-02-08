package me.spica27.spicamusic.storage.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongGroup
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.common.entity.SongSortOrder
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

    override suspend fun getSongsByMediaStoreIds(ids: List<Long>): List<Song> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        songDao.getSongsByMediaStoreIds(ids).map { it.toCommon() }
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

    override fun getSongsFlow(sortOrder: SongSortOrder, filter: SongFilter): Flow<List<Song>> {
        val keyword = filter.keyword
        return if (keyword.isNullOrEmpty()) {
            // 无关键词时使用基础查询
            getAllSongsFlow()
        } else {
            // 有关键词时使用搜索
            searchSongsFlow(keyword, sortOrder)
        }.map { songs ->
            applySortAndFilter(songs, sortOrder, filter)
        }
    }

    override suspend fun getSongs(sortOrder: SongSortOrder, filter: SongFilter): List<Song> = 
        withContext(Dispatchers.IO) {
            val keyword = filter.keyword
            val songs = if (keyword.isNullOrEmpty()) {
                getAllSongs()
            } else {
                songDao.searchSongsSync(
                    keyword = keyword,
                    onlyLiked = if (filter.onlyLiked) 1 else 0,
                    excludeIgnored = if (filter.excludeIgnored) 1 else 0
                ).map { it.toCommon() }
            }
            applySortAndFilter(songs, sortOrder, filter)
        }

    override fun searchSongsFlow(keyword: String, sortOrder: SongSortOrder): Flow<List<Song>> = 
        songDao.searchSongs(
            keyword = keyword,
            onlyLiked = 0,
            excludeIgnored = 1
        ).map { list -> 
            val songs = list.map { it.toCommon() }
            applySorting(songs, sortOrder)
        }

    /**
     * 应用排序和筛选
     */
    private fun applySortAndFilter(
        songs: List<Song>,
        sortOrder: SongSortOrder,
        filter: SongFilter
    ): List<Song> {
        var result = songs

        // 应用筛选条件
        filter.minDuration?.let { minDur ->
            result = result.filter { it.duration >= minDur }
        }
        filter.maxDuration?.let { maxDur ->
            result = result.filter { it.duration <= maxDur }
        }
        filter.minSize?.let { minS ->
            result = result.filter { it.size >= minS }
        }
        filter.maxSize?.let { maxS ->
            result = result.filter { it.size <= maxS }
        }
        filter.artists?.let { artistList ->
            if (artistList.isNotEmpty()) {
                result = result.filter { song -> 
                    artistList.any { it.equals(song.artist, ignoreCase = true) }
                }
            }
        }
        filter.albums?.let { albumList ->
            if (albumList.isNotEmpty()) {
                result = result.filter { it.albumId in albumList }
            }
        }
        filter.mimeTypes?.let { mimeList ->
            if (mimeList.isNotEmpty()) {
                result = result.filter { it.mimeType in mimeList }
            }
        }
        if (filter.onlyLiked) {
            result = result.filter { it.like }
        }

        // 应用排序
        return applySorting(result, sortOrder)
    }

    /**
     * 应用排序
     */
    private fun applySorting(songs: List<Song>, sortOrder: SongSortOrder): List<Song> {
        return when (sortOrder) {
            SongSortOrder.DISPLAY_NAME_ASC -> songs.sortedBy { it.displayName }
            SongSortOrder.DISPLAY_NAME_DESC -> songs.sortedByDescending { it.displayName }
            SongSortOrder.ARTIST_ASC -> songs.sortedBy { it.artist }
            SongSortOrder.ARTIST_DESC -> songs.sortedByDescending { it.artist }
            SongSortOrder.DURATION_ASC -> songs.sortedBy { it.duration }
            SongSortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
            SongSortOrder.SIZE_ASC -> songs.sortedBy { it.size }
            SongSortOrder.SIZE_DESC -> songs.sortedByDescending { it.size }
            SongSortOrder.DATE_ADDED_ASC -> songs.sortedBy { it.sort }
            SongSortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.sort }
            SongSortOrder.PLAY_COUNT_DESC -> songs // 播放次数需要从播放历史获取，此处暂不实现
            SongSortOrder.RANDOM -> songs.shuffled()
            SongSortOrder.DEFAULT -> songs
        }
    }

    override fun getSongsGroupedBySortNameFlow(keyword: String?): Flow<List<SongGroup>> =
        songDao.getSongsGroupedBySortName(keyword)
            .map { entities -> 
                // 数据库已按 sortName 排序，直接分组即可
                entities.map { it.toCommon() }
                    .groupBy { it.sortName }
                    .map { (key, songs) -> SongGroup(key, songs) }
            }

    override suspend fun getSongsGroupedBySortName(keyword: String?): List<SongGroup> = 
        withContext(Dispatchers.IO) {
            // 数据库已按 sortName 排序，直接分组即可
            songDao.getSongsGroupedBySortNameSync(keyword)
                .map { it.toCommon() }
                .groupBy { it.sortName }
                .map { (key, songs) -> SongGroup(key, songs) }
        }

    // ===== 分页 API 实现 =====

    companion object {
        private const val PAGE_SIZE = 30
        private const val PREFETCH_DISTANCE = 10
    }

    override fun getFilteredSongsPagingFlow(keyword: String?): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { songDao.getFilteredSongsPaging(keyword) }
        ).flow.map { pagingData -> pagingData.map { it.toCommon() } }

    override fun getSongsBySortNamePagingFlow(keyword: String?): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { songDao.getSongsBySortNamePaging(keyword) }
        ).flow.map { pagingData -> pagingData.map { it.toCommon() } }

    override fun countFilteredSongsFlow(keyword: String?): Flow<Int> =
        songDao.countFilteredSongs(keyword)

    override suspend fun getFilteredSongIds(keyword: String?): List<Long> =
        withContext(Dispatchers.IO) {
            songDao.getFilteredSongIds(keyword)
        }

    // ===== 歌曲选择器分页 API 实现 =====

    override fun getSongsNotInPlaylistPagingFlow(
        playlistId: Long,
        keyword: String?,
    ): Flow<PagingData<Song>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { songDao.getSongsNotInPlaylistPaging(playlistId, keyword) }
        ).flow.map { pagingData -> pagingData.map { it.toCommon() } }

    override fun countSongsNotInPlaylistFlow(
        playlistId: Long,
        keyword: String?,
    ): Flow<Int> =
        songDao.countSongsNotInPlaylist(playlistId, keyword)

    override suspend fun getSongIdsNotInPlaylist(
        playlistId: Long,
        keyword: String?,
    ): List<Long> = withContext(Dispatchers.IO) {
        songDao.getSongIdsNotInPlaylist(playlistId, keyword)
    }
}