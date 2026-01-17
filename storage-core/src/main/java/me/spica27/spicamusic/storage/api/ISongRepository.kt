package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongGroup
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.common.entity.SongSortOrder

/**
 * 歌曲仓库接口
 * 提供歌曲数据的增删改查操作
 */
interface ISongRepository {
    /**
     * 获取所有歌曲的 Flow
     */
    fun getAllSongsFlow(): Flow<List<Song>>
    
    /**
     * 获取所有歌曲（同步）
     */
    suspend fun getAllSongs(): List<Song>

    /**
     * 获取所有喜欢的歌曲的 Flow
     */
    fun getAllLikeSongsFlow(): Flow<List<Song>>

    /**
     * 获取常听的前10首歌曲
     */
    fun getOftenListenSong10Flow(): Flow<List<Song>>

    /**
     * 获取常听的歌曲
     */
    fun getOftenListenSongsFlow(): Flow<List<Song>>

    /**
     * 获取随机歌曲
     */
    fun getRandomSongFlow(): Flow<List<Song>>

    /**
     * 根据ID获取歌曲 Flow
     */
    fun getSongFlowById(id: Long): Flow<Song?>

    /**
     * 根据 MediaStore ID 获取歌曲
     */
    suspend fun getSongByMediaStoreId(mediaStoreId: Long): Song?

    /**
     * 获取不在指定歌单中的歌曲
     */
    fun getSongsNotInPlaylistFlow(playlistId: Long): Flow<List<Song>>

    /**
     * 切换歌曲的喜欢状态
     */
    suspend fun toggleLike(id: Long)

    /**
     * 设置歌曲忽略状态
     */
    suspend fun setIgnoreStatus(id: Long, isIgnore: Boolean)

    /**
     * 获取歌曲的喜欢状态 Flow
     */
    fun getSongLikeStatusFlow(id: Long): Flow<Boolean>

    /**
     * 获取被忽略的歌曲列表 Flow
     */
    fun getIgnoreSongsFlow(): Flow<List<Song>>

    /**
     * 获取歌曲列表（支持排序和筛选）
     * @param sortOrder 排序方式
     * @param filter 筛选条件
     * @return 歌曲列表 Flow
     */
    fun getSongsFlow(
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT,
        filter: SongFilter = SongFilter.EMPTY
    ): Flow<List<Song>>

    /**
     * 获取歌曲列表（支持排序和筛选，同步）
     * @param sortOrder 排序方式
     * @param filter 筛选条件
     * @return 歌曲列表
     */
    suspend fun getSongs(
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT,
        filter: SongFilter = SongFilter.EMPTY
    ): List<Song>

    /**
     * 搜索歌曲（关键词匹配歌名或艺术家）
     * @param keyword 搜索关键词
     * @param sortOrder 排序方式
     * @return 匹配的歌曲列表 Flow
     */
    fun searchSongsFlow(
        keyword: String,
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT
    ): Flow<List<Song>>

    /**
     * 获取按首字母分组的歌曲列表（A-Z + #）
     * 在数据库层面完成排序和分组，减少内存操作
     * @param keyword 模糊查询关键字（可选），搜索歌曲名称、艺术家
     * @return 分组后的歌曲列表 Flow
     */
    fun getSongsGroupedBySortNameFlow(keyword: String? = null): Flow<List<SongGroup>>

    /**
     * 获取按首字母分组的歌曲列表（A-Z + #，同步）
     * @param keyword 模糊查询关键字（可选），搜索歌曲名称、艺术家
     * @return 分组后的歌曲列表
     */
    suspend fun getSongsGroupedBySortName(keyword: String? = null): List<SongGroup>
}
