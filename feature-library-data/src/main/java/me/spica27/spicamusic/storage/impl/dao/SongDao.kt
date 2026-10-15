package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.SongEntity

@Dao
interface SongDao {
    @Query("UPDATE song SET `like` = (CASE WHEN `like` == 1 THEN 0 ELSE 1 END) WHERE (songId == :id)")
    suspend fun toggleLike(id: Long)

    @Query("UPDATE song SET `like` = (CASE WHEN `like` == 1 THEN 0 ELSE 1 END) WHERE (mediaStoreId == :mediaStoreId)")
    suspend fun toggleLikeByMediaStoreId(mediaStoreId: Long)

    @Query("UPDATE song SET `like` = :like WHERE( songId == :id)")
    suspend fun likeSongs(id: Long, like: Boolean)

    @Query("UPDATE song SET `like` = :like WHERE( songId IN (:ids))")
    suspend fun likeSongs(ids: List<Long>, like: Boolean)

    @Query("UPDATE song SET `like` = :like WHERE( mediaStoreId IN (:mediaStoreIds))")
    suspend fun likeSongsByMediaStoreIds(mediaStoreIds: List<Long>, like: Boolean)

    @Query("SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%' OR artist LIKE '%' ||:name|| '%'")
    fun getsSongsFromName(name: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%' OR artist LIKE '%' ||:name|| '%' AND (isIgnore == 0)")
    fun getsSongsFromNameSync(name: String): List<SongEntity>

    @Query("SELECT * FROM song WHERE songId == :id")
    fun getSongWithId(id: Long): SongEntity?

    @Query("SELECT * FROM song WHERE mediaStoreId == :id")
    fun getSongWithMediaStoreId(id: Long): SongEntity?

    @Query("UPDATE song SET waveformData = :waveformData WHERE mediaStoreId == :mediaStoreId")
    suspend fun updateWaveformDataByMediaStoreId(mediaStoreId: Long, waveformData: String)

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

    @Query("SELECT * FROM song WHERE (isIgnore == 0) LIMIT :limit")
    fun getAllLimited(limit: Int): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM song WHERE isIgnore == 0")
    fun getSongsCountFlow(): Flow<Int>

    @Query("SELECT * FROM song WHERE (isIgnore == 0)")
    fun getAllSync(): List<SongEntity>

    @Query("SELECT * FROM song WHERE `like` == 1")
    fun getAllLikeSong(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE `like` == 1 LIMIT 10")
    fun getAllLikeSong10(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE mediaStoreId NOT IN (SELECT mediaId FROM playlistsongcrossref WHERE playlistId = :playlistId) AND (isIgnore == 0)")
    fun getSongsNotInPlayListFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE mediaStoreId NOT IN (SELECT mediaId FROM playlistsongcrossref WHERE playlistId = :playlistId) AND (isIgnore == 0)")
    fun getSongsNotInPlayList(playlistId: Long): List<SongEntity>

    // 改读按歌聚合的 SongPlayStat（每首歌一行），去掉对整张 PlayHistory 的
    // GROUP BY 全表扫描——成本从 O(播放事件数) 降到 O(曲库大小)，不随历史累计变慢。
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT s.*, COALESCE(sps.playCount, 0) as play_count
        FROM song s
        LEFT JOIN SongPlayStat sps ON s.mediaStoreId = sps.mediaId
        WHERE s.isIgnore == 0
        ORDER BY play_count DESC
    """
    )
    fun getOftenListenSongs(): Flow<List<SongEntity>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT s.*, COALESCE(sps.playCount, 0) as play_count
        FROM song s
        LEFT JOIN SongPlayStat sps ON s.mediaStoreId = sps.mediaId
        WHERE s.isIgnore == 0
        ORDER BY play_count DESC
        LIMIT 10
    """
    )
    fun getOftenListenSong10(): Flow<List<SongEntity>>

    // ===== 扫描写入 =====

    /**
     * 增量扫描判断所需的最少字段，以及 upsert 时要保留的用户状态
     */
    data class SongScanInfo(
        val songId: Long,
        val mediaStoreId: Long,
        val albumId: Long?,
        val dateModified: Long,
        val size: Long,
        val tagVersion: Int,
        val waveformData: String?,
        val like: Boolean,
        val isIgnore: Boolean,
        val isImported: Boolean,
        val sort: Int,
    )

    @Query(
        "SELECT songId, mediaStoreId, albumId, dateModified, size, tagVersion, waveformData, `like`, isIgnore, isImported, sort FROM song",
    )
    suspend fun getAllScanInfo(): List<SongScanInfo>

    @Query(
        "SELECT songId, mediaStoreId, albumId, dateModified, size, tagVersion, waveformData, `like`, isIgnore, isImported, sort " +
            "FROM song WHERE mediaStoreId IN (:mediaStoreIds)",
    )
    suspend fun getScanInfoByMediaStoreIds(mediaStoreIds: List<Long>): List<SongScanInfo>

    @Query("SELECT EXISTS(SELECT 1 FROM song WHERE tagVersion < :tagVersion)")
    suspend fun hasStaleTags(tagVersion: Int): Boolean

    /**
     * 按 songId 主键 upsert（新歌 songId = 0 时插入）。
     * 不能改成 REPLACE：REPLACE 先删旧行，会级联删除这首歌的艺人关系
     */
    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM song WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteSongsByMediaStoreIds(mediaStoreIds: List<Long>)

    /** 明确导入的歌曲不受扫描规则过滤 */
    @Query("UPDATE song SET isImported = 1 WHERE songId = :songId")
    suspend fun markImported(songId: Long)

    @Query("SELECT * FROM song WHERE songId IN (SELECT songId FROM song ORDER BY RANDOM() LIMIT 15) AND (isIgnore == 0)")
    fun randomSong(): Flow<List<SongEntity>>

    /** 随机取 [limit] 首歌曲，不含已忽略 */
    @Query("SELECT * FROM song WHERE isIgnore == 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomSongs(limit: Int): List<SongEntity>

    /** 随机取 [limit] 首歌曲，排除 [excludeIds] 中的 mediaStoreId，不含已忽略 */
    @Query("SELECT * FROM song WHERE mediaStoreId NOT IN (:excludeIds) AND isIgnore == 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomSongsExcluding(excludeIds: List<Long>, limit: Int): List<SongEntity>

    @Query("SELECT * FROM song WHERE isIgnore == 1")
    fun getIgnoreSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM song WHERE isIgnore == 1")
    fun getIgnoredSongsCountFlow(): Flow<Int>

    @Query("SELECT * FROM song WHERE isIgnore == 1")
    fun getIgnoreSongs(): List<SongEntity>

    @Query("UPDATE song SET isIgnore = :isIgnore WHERE songId == :id")
    fun ignore(id: Long, isIgnore: Boolean)


    @Query("UPDATE song SET isIgnore = :isIgnore WHERE mediaStoreId == :mediaStoreId")
    fun ignoreByMediaStoreId(mediaStoreId: Long, isIgnore: Boolean)

    @Query("UPDATE song SET isIgnore = :isIgnore WHERE songId IN (:ids)")
    fun ignoreSongs(ids: List<Long>,isIgnore: Boolean)

    @Query("UPDATE song SET isIgnore = :isIgnore WHERE mediaStoreId IN (:mediaStoreIds)")
    fun ignoreSongsByMediaStoreIds(mediaStoreIds: List<Long>, isIgnore: Boolean)

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
     * 匹配歌名、歌手和专辑，与搜索框提示文案一致
     */
    @Query(
        """
        SELECT * FROM song
        WHERE isIgnore == 0
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%'
            OR artist LIKE '%' || :keyword || '%'
            OR album LIKE '%' || :keyword || '%'
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
        AND mediaStoreId NOT IN (SELECT mediaId FROM playlistsongcrossref WHERE playlistId = :playlistId)
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
     * 获取不在指定歌单中的符合条件歌曲总数（条件须与 [getSongsNotInPlaylistPaging] 一致）
     * 歌单条目的 mediaId 存的是 MediaStore ID，要与 mediaStoreId 比较，不能用 songId
     */
    @Query(
        """
        SELECT COUNT(*) FROM song
        WHERE isIgnore == 0
        AND mediaStoreId NOT IN (SELECT mediaId FROM playlistsongcrossref WHERE playlistId = :playlistId)
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
        SELECT mediaStoreId FROM song 
        WHERE isIgnore == 0
        AND mediaStoreId NOT IN (SELECT mediaId FROM playlistsongcrossref WHERE playlistId = :playlistId)
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

    // ===== 收藏歌曲分页查询 =====

    /**
     * 分页获取喜欢的歌曲（支持关键词过滤）
     */
    @Query(
        """
        SELECT * FROM song
        WHERE `like` == 1
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%'
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY displayName COLLATE NOCASE ASC
    """
    )
    fun getLikeSongsPaging(keyword: String? = null): PagingSource<Int, SongEntity>

    /**
     * 获取喜欢的歌曲总数（支持关键词过滤）
     */
    @Query(
        """
        SELECT COUNT(*) FROM song
        WHERE `like` == 1
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%'
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    fun countLikeSongs(keyword: String? = null): Flow<Int>

    /**
     * 获取所有喜欢的歌曲 ID（用于全选）
     */
    @Query(
        """
        SELECT songId FROM song
        WHERE `like` == 1
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%'
            OR artist LIKE '%' || :keyword || '%'
        )
    """
    )
    suspend fun getLikeSongIds(keyword: String? = null): List<Long>

    /**
     * 获取所有喜欢的歌曲 mediaStoreId（用于播放）
     */
    @Query(
        """
        SELECT mediaStoreId FROM song
        WHERE `like` == 1
        AND (
            :keyword IS NULL OR :keyword = ''
            OR displayName LIKE '%' || :keyword || '%'
            OR artist LIKE '%' || :keyword || '%'
        )
        ORDER BY displayName COLLATE NOCASE ASC
    """,
    )
    suspend fun getLikeMediaStoreIds(keyword: String? = null): List<Long>
}
