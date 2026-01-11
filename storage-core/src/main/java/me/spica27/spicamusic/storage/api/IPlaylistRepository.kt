package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song

/**
 * 歌单仓库接口
 * 提供歌单数据的增删改查操作
 */
interface IPlaylistRepository {
    /**
     * 根据歌单ID获取歌曲列表 Flow
     */
    fun getSongsByPlaylistIdFlow(playlistId: Long): Flow<List<Song>>

    /**
     * 根据歌单ID获取歌单信息 Flow
     */
    fun getPlaylistByIdFlow(playlistId: Long): Flow<Playlist?>

    /**
     * 获取所有歌单 Flow
     */
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>

    /**
     * 获取包含指定歌曲的歌单列表
     */
    fun getPlaylistsHavingSong(songId: Long): Flow<List<Playlist>>

    /**
     * 获取不包含指定歌曲的歌单列表
     */
    fun getPlaylistsNotHavingSong(songId: Long): Flow<List<Playlist>>

    /**
     * 获取歌单详情（包含歌曲）Flow
     */
    fun getPlaylistWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?>

    /**
     * 增加歌单播放次数
     */
    suspend fun incrementPlaylistPlayTime(playlistId: Long)

    /**
     * 创建新歌单
     * @return 新创建的歌单ID
     */
    suspend fun createPlaylist(name: String): Long

    /**
     * 删除歌单
     */
    suspend fun deletePlaylist(id: Long)

    /**
     * 重命名歌单
     */
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    /**
     * 添加歌曲到歌单
     */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    /**
     * 从歌单中移除歌曲
     */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    /**
     * 批量添加歌曲到歌单
     */
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>)
}
