package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.*

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

    @Query("SELECT s.* FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.songId = psc.songId WHERE psc.playlistId = :playlistId ORDER BY psc.insertTime DESC")
    fun getSongsByPlaylistIdFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM Song as s JOIN PlaylistSongCrossRef as psc ON s.songId = psc.songId WHERE psc.playlistId = :playlistId ORDER BY psc.insertTime DESC")
    fun getSongsByPlaylistId(playlistId: Long): List<SongEntity>

    @Query("SELECT * FROM Playlist WHERE playlistId IN (SELECT playlistId FROM PlaylistSongCrossRef WHERE songId == :songId)")
    fun getPlaylistsHaveSong(songId: Long): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM Playlist WHERE playlistId NOT IN (SELECT playlistId FROM PlaylistSongCrossRef WHERE songId == :songId)")
    fun getPlaylistsNotHavingSong(songId: Long): Flow<List<PlaylistEntity>>

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
}
