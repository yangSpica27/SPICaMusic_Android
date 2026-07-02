package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistWithSongsEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

@Dao
interface PlaylistDao {
    @Query("update Playlist set needUpdate = 1 where playlistId = :playlistId")
    fun setNeedUpdate(playlistId: Long)

    @Transaction
    @Query("SELECT * FROM Playlist")
    fun getPlaylistsWithSongs(): List<PlaylistWithSongsEntity>

    @Transaction
    @Query("SELECT * FROM Playlist")
    fun getAllPlaylist(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlayListByIdFlow(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlayListById(playlistId: Long): PlaylistEntity

    @Query("update Playlist set playlistName = :newName where playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("update Playlist set playTimes = playTimes + 1 where playlistId = :playlistId")
    fun addPlayTimes(playlistId: Long)

    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlaylistsWithSongsWithPlayListId(playlistId: Long): PlaylistWithSongsEntity?

    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlaylistsWithSongsWithPlayListIdFlow(playlistId: Long): Flow<PlaylistWithSongsEntity?>

    @Query("SELECT s.* FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.mediaStoreId = psc.mediaId WHERE psc.playlistId = :playlistId ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC")
    fun getSongsByPlaylistIdFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.mediaStoreId = psc.mediaId WHERE psc.playlistId = :playlistId ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC")
    fun getSongsByPlaylistId(playlistId: Long): List<SongEntity>

    @Query("SELECT * FROM Playlist WHERE playlistId IN (SELECT playlistId FROM PlaylistSongCrossRef WHERE mediaId == :mediaId)")
    fun getPlaylistsHaveSong(mediaId: Long): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM Playlist WHERE playlistId NOT IN (SELECT playlistId FROM PlaylistSongCrossRef WHERE mediaId == :mediaId)")
    fun getPlaylistsNotHavingSong(mediaId: Long): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("DELETE FROM playlist WHERE playlistId ==:playlistId")
    fun deleteList(playlistId: Long)

    @Transaction
    @Insert
    suspend fun insertListItems(songs: List<PlaylistSongCrossRefEntity>)

    @Transaction
    @Insert
    suspend fun insertListItem(songs: PlaylistSongCrossRefEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(list: PlaylistEntity)

    @Insert
    fun insertPlaylistAndGetId(list: PlaylistEntity): Long

    @Query("DELETE FROM playlist WHERE playlistId == :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("UPDATE playlist SET playlistName = :newName WHERE playlistId == :playlistId")
    suspend fun saveNewNameById(playlistId: Long, newName: String)

    @Transaction
    @Delete
    suspend fun deleteListItem(song: PlaylistSongCrossRefEntity)

    @Transaction
    @Delete
    suspend fun deleteListItems(songs: List<PlaylistSongCrossRefEntity>)

    /** 所有歌单分页查询，按创建时间倒序 */
    @Query("SELECT * FROM Playlist ORDER BY createTimestamp DESC")
    fun getAllPaging(): PagingSource<Int, PlaylistEntity>


    /** 获取歌单内歌曲数量 */
    @Query("SELECT COUNT(*) FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.mediaStoreId = psc.mediaId WHERE psc.playlistId = :playlistId")
    fun getSongSizeByPlaylistId(playlistId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.mediaStoreId = psc.mediaId WHERE psc.playlistId = :playlistId")
    fun getSongSizeByPlaylistIdOnce(playlistId: Long): Int

    /**
     * 获取歌单前 4 个不同专辑 ID，用于封面马赛克渲染。
     * 按歌曲加入时间倒序取最新的 4 个不同 albumId。
     */
    @Query(
        """SELECT DISTINCT s.albumId FROM Song AS s
           JOIN PlaylistSongCrossRef AS psc ON s.mediaStoreId = psc.mediaId
           WHERE psc.playlistId = :playlistId
           ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC
           LIMIT 4"""
    )
    fun getCoverAlbumIds(playlistId: Long): Flow<List<Long>>

    /** 在指定歌单内按关键字（曲名 / 艺术家）过滤歌曲，按自定义顺序 */
    @Query(
        """SELECT s.* FROM Song AS s
           JOIN PlaylistSongCrossRef AS psc ON s.mediaStoreId = psc.mediaId
           WHERE psc.playlistId = :playlistId
             AND (s.displayName LIKE '%' || :keyword || '%'
                  OR s.artist LIKE '%' || :keyword || '%')
           ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC"""
    )
    fun searchSongsByPlaylistId(playlistId: Long, keyword: String): Flow<List<SongEntity>>

    @Query(
        """SELECT s.* FROM Song AS s
           JOIN PlaylistSongCrossRef AS psc ON s.mediaStoreId = psc.mediaId
           WHERE psc.playlistId = :playlistId
                      AND (s.displayName LIKE '%' || :keyword || '%'
                  OR s.artist LIKE '%' || :keyword || '%')
           ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC"""
    )
    fun getSongsPagingByPlaylistId(playlistId: Long, keyword: String): PagingSource<Int, SongEntity>

    @Query("SELECT MAX(sortOrder) FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    fun getMaxSortOrderByPlaylistId(playlistId: Long): Long?

    @Query("UPDATE PlaylistSongCrossRef SET sortOrder = :sortOrder WHERE playlistId = :playlistId AND mediaId = :mediaId")
    fun updateSortOrder(playlistId: Long, mediaId: Long, sortOrder: Long)

    @Query("SELECT s.mediaStoreId FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.mediaStoreId = psc.mediaId WHERE psc.playlistId = :playlistId ORDER BY psc.sortOrder DESC, psc.insertTime DESC, psc.mediaId DESC")
    fun getMediaIdsByPlaylistId(playlistId: Long): List<Long>
}
