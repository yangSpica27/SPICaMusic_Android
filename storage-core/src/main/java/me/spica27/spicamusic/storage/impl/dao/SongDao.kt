package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.SongEntity

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Query("UPDATE song SET `like` = (CASE WHEN `like` == 1 THEN 0 ELSE 1 END) WHERE (songId == :id)")
    suspend fun toggleLike(id: Long)

    @Query("UPDATE song SET `like` = (CASE WHEN `like` == 1 THEN 0 ELSE 1 END) WHERE (mediaStoreId == :mediaStoreId)")
    suspend fun toggleLikeByMediaStoreId(mediaStoreId: Long)

    @Query("UPDATE song SET `like` = :like WHERE( songId == :id)")
    suspend fun likeSongs(id: Long, like: Boolean)

    @Query("UPDATE song SET `like` = :like WHERE( songId IN (:ids))")
    suspend fun likeSongs(ids: List<Long>, like: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSync(songs: List<SongEntity>)

    @Query("DELETE FROM song WHERE mediaStoreId NOT IN (:mediaIds)")
    suspend fun deleteSongsNotInList(mediaIds: List<Long>)

    @Query("SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%' OR artist LIKE '%' ||:name|| '%'")
    fun getsSongsFromName(name: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%' OR artist LIKE '%' ||:name|| '%' AND (isIgnore == 0)")
    fun getsSongsFromNameSync(name: String): List<SongEntity>

    @Query("SELECT * FROM song WHERE songId == :id")
    fun getSongWithId(id: Long): SongEntity?

    @Query("SELECT * FROM song WHERE mediaStoreId == :id")
    fun getSongWithMediaStoreId(id: Long): SongEntity?

    @Query("SELECT COALESCE(`like`, 0) FROM song WHERE mediaStoreId == :mediaStoreId")
    fun getSongLikeFlowWithMediaId(mediaStoreId: Long): Flow<Int>

    @Query("SELECT * FROM song WHERE mediaStoreId == :id AND (isIgnore == 0)")
    fun getSongFlowWithMediaStoreId(id: Long): Flow<SongEntity?>

    /**
     * 批量根据 MediaStore ID 获取歌曲
     * 用于解决 N+1 查询问题
     */
    @Query("SELECT * FROM song WHERE mediaStoreId IN (:ids) AND (isIgnore == 0)")
    suspend fun getSongsByMediaStoreIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT * FROM song WHERE songId == :id")
    fun getSongFlowWithId(id: Long): Flow<SongEntity?>

    @Query("SELECT `like` FROM song WHERE songId == :id")
    fun getSongIsLikeFlowWithId(id: Long): Flow<Int>

    @Query("SELECT * FROM song WHERE (isIgnore == 0)")
    fun getAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE (isIgnore == 0)")
    fun getAllSync(): List<SongEntity>

    @Query("SELECT * FROM song WHERE `like` == 1")
    fun getAllLikeSong(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE `like` == 1 LIMIT 10")
    fun getAllLikeSong10(): Flow<List<SongEntity>>

    @Query("DELETE FROM song")
    suspend fun deleteAll()

    @Query("DELETE FROM song")
    fun deleteAllSync()

    @Query("SELECT * FROM song WHERE songId NOT IN (SELECT songId FROM playlistsongcrossref WHERE playlistId = :playlistId) AND (isIgnore == 0)")
    fun getSongsNotInPlayListFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE songId NOT IN (SELECT songId FROM playlistsongcrossref WHERE playlistId = :playlistId) AND (isIgnore == 0)")
    fun getSongsNotInPlayList(playlistId: Long): List<SongEntity>

    @Delete
    suspend fun delete(song: SongEntity)

    @Delete
    suspend fun delete(song: List<SongEntity>)

    @Query(
        """
        SELECT s.*, COUNT(ph.mediaId) as play_count 
        FROM song s
        LEFT JOIN PlayHistory ph ON s.mediaStoreId = ph.mediaId
        WHERE s.isIgnore == 0
        GROUP BY s.songId
        ORDER BY play_count DESC
    """
    )
    fun getOftenListenSongs(): Flow<List<SongEntity>>

    @Query(
        """
        SELECT s.*, COUNT(ph.mediaId) as play_count 
        FROM song s
        LEFT JOIN PlayHistory ph ON s.mediaStoreId = ph.mediaId
        WHERE s.isIgnore == 0
        GROUP BY s.songId
        ORDER BY play_count DESC
        LIMIT 10
    """
    )
    fun getOftenListenSong10(): Flow<List<SongEntity>>

    @Transaction
    suspend fun updateSongs(songs: List<SongEntity>) {
        val songIds = songs.map { it.mediaStoreId }
        deleteSongsNotInList(songIds)
        insert(songs)
    }

    // ===== 增量扫描相关方法 =====

    /**
     * 轻量级数据类，仅包含增量扫描判断所需的最少字段
     */
    data class SongScanInfo(
        val songId: Long,
        val mediaStoreId: Long,
        val dateModified: Long,
        val like: Boolean,
        val isIgnore: Boolean,
        val sort: Int,
    )

    /**
     * 获取所有歌曲的扫描摘要信息（用于增量比较）
     * 只查询必要列，避免全量加载
     */
    @Query("SELECT songId, mediaStoreId, dateModified, `like`, isIgnore, sort FROM song")
    suspend fun getAllScanInfo(): List<SongScanInfo>

    /**
     * 使用 Upsert 策略批量插入或更新歌曲
     * 基于 mediaStoreId 唯一索引决定 INSERT 或 UPDATE
     */
    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)

    /**
     * 根据 mediaStoreId 列表批量删除歌曲（已从 MediaStore 中移除的）
     */
    @Query("DELETE FROM song WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteSongsByMediaStoreIds(mediaStoreIds: List<Long>)

    /**
     * 增量更新入口：删除已移除的歌曲 + upsert 变更的歌曲
     */
    @Transaction
    suspend fun incrementalUpdateSongs(
        removedMediaStoreIds: List<Long>,
        changedSongs: List<SongEntity>,
    ) {
        if (removedMediaStoreIds.isNotEmpty()) {
            // Room IN 参数上限 999，分批删除
            removedMediaStoreIds.chunked(500).forEach { chunk ->
                deleteSongsByMediaStoreIds(chunk)
            }
        }
        if (changedSongs.isNotEmpty()) {
            upsertSongs(changedSongs)
        }
    }

    @Query("SELECT * FROM song WHERE songId IN (SELECT songId FROM song ORDER BY RANDOM() LIMIT 15) AND (isIgnore == 0)")
    fun randomSong(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE isIgnore == 1")
    fun getIgnoreSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE isIgnore == 1")
    fun getIgnoreSongs(): List<SongEntity>

    @Query("UPDATE song SET isIgnore = :isIgnore WHERE songId == :id")
    fun ignore(id: Long, isIgnore: Boolean)

    /**
     * 搜索歌曲（关键词匹配）
     * 注意：排序需要在应用层处理
     */
    @Query(
        """
        SELECT * FROM song 
        WHERE (displayName LIKE '%' || :keyword || '%' OR artist LIKE '%' || :keyword || '%')
        AND (:excludeIgnored = 0 OR isIgnore == 0)
        AND (:onlyLiked = 0 OR `like` == 1)
    """
    )
    fun searchSongs(
        keyword: String,
        onlyLiked: Int = 0,
        excludeIgnored: Int = 1
    ): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM song 
        WHERE (displayName LIKE '%' || :keyword || '%' OR artist LIKE '%' || :keyword || '%')
        AND (:excludeIgnored = 0 OR isIgnore == 0)
        AND (:onlyLiked = 0 OR `like` == 1)
    """
    )
    fun searchSongsSync(
        keyword: String,
        onlyLiked: Int = 0,
        excludeIgnored: Int = 1
    ): List<SongEntity>

    /**
     * 获取按 sortName 分组的歌曲（用于搜索页面分组展示）
     * 在数据库层面按 sortName 分组并排序
     * @param keyword 模糊查询关键字（可选），搜索歌曲名称、艺术家
     */
    @Query(
        """
        SELECT * FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY 
            CASE 
                WHEN sortName = '#' THEN 1
                ELSE 0
            END,
            sortName ASC,
            displayName COLLATE NOCASE ASC
    """
    )
    fun getSongsGroupedBySortName(keyword: String? = null): Flow<List<SongEntity>>

    @Query(
        """
        SELECT * FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY 
            CASE 
                WHEN sortName = '#' THEN 1
                ELSE 0
            END,
            sortName ASC,
            displayName COLLATE NOCASE ASC
    """
    )
    suspend fun getSongsGroupedBySortNameSync(keyword: String? = null): List<SongEntity>

    // ===== 分页查询 =====

    /**
     * 分页获取歌曲（支持关键词过滤）
     * 用于 AllSongsScreen 和 SongPickerSheet
     */
    @Query(
        """
        SELECT * FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY displayName COLLATE NOCASE ASC
    """
    )
    fun getFilteredSongsPaging(keyword: String? = null): PagingSource<Int, SongEntity>

    /**
     * 分页获取歌曲（按 sortName 排序，用于 SearchPage 分组）
     */
    @Query(
        """
        SELECT * FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY 
            CASE 
                WHEN sortName = '#' THEN 1
                ELSE 0
            END,
            sortName ASC,
            displayName COLLATE NOCASE ASC
    """
    )
    fun getSongsBySortNamePaging(keyword: String? = null): PagingSource<Int, SongEntity>

    /**
     * 获取符合条件的歌曲总数（用于 UI 显示总数）
     */
    @Query(
        """
        SELECT COUNT(*) FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    fun countFilteredSongs(keyword: String? = null): Flow<Int>

    /**
     * 获取所有符合条件的歌曲 ID（用于全选功能）
     */
    @Query(
        """
        SELECT songId FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    suspend fun getFilteredSongIds(keyword: String? = null): List<Long>

    /**
     * 获取所有符合条件的歌曲 ID（用于全选功能）
     */
    @Query(
        """
        SELECT mediaStoreId FROM song 
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    suspend fun getFilteredMediaStoreIds(keyword: String? = null): List<Long>

    // ===== 歌曲选择器分页查询（排除指定歌单已有歌曲） =====

    /**
     * 分页获取不在指定歌单中的歌曲（支持关键词过滤）
     * 用于 SongPickerSheet
     */
    @Query(
        """
        SELECT * FROM song 
        WHERE isIgnore == 0
        AND songId NOT IN (SELECT songId FROM playlistsongcrossref WHERE playlistId = :playlistId)
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY displayName COLLATE NOCASE ASC
    """
    )
    fun getSongsNotInPlaylistPaging(
        playlistId: Long,
        keyword: String? = null,
    ): PagingSource<Int, SongEntity>

    /**
     * 获取不在指定歌单中的符合条件歌曲总数
     */
    @Query(
        """
        SELECT COUNT(*) FROM song 
        WHERE isIgnore == 0
        AND songId NOT IN (SELECT songId FROM playlistsongcrossref WHERE playlistId = :playlistId)
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    fun countSongsNotInPlaylist(
        playlistId: Long,
        keyword: String? = null,
    ): Flow<Int>

    /**
     * 获取不在指定歌单中的所有符合条件歌曲 ID（用于全选）
     */
    @Query(
        """
        SELECT songId FROM song 
        WHERE isIgnore == 0
        AND songId NOT IN (SELECT songId FROM playlistsongcrossref WHERE playlistId = :playlistId)
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%' 
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    suspend fun getSongIdsNotInPlaylist(
        playlistId: Long,
        keyword: String? = null,
    ): List<Long>
}
