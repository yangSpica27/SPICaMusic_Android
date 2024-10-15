package me.spica27.spicamusic.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.PlaylistWithSongs


@Dao
interface PlaylistDao {


    @Transaction
    @Query("SELECT * FROM Playlist")
    fun getPlaylistsWithSongs(): List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM Playlist")
    fun getAllPlaylist(): Flow<List<Playlist>>


    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlaylistsWithSongsWithPlayListId(playlistId: Long): PlaylistWithSongs?


    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
    fun getPlaylistsWithSongsWithPlayListIdFlow(playlistId: Long): Flow<PlaylistWithSongs?>

    @Transaction
    @Query("DELETE FROM playlist WHERE playlistId ==:playlistId")
    fun deleteList(playlistId: Long)

    @Transaction
    @Insert
    suspend fun insertListItems(songs: List<PlaylistSongCrossRef>)

    @Transaction
    @Insert
    suspend fun insertListItem(songs: PlaylistSongCrossRef)

    @Transaction
    @Insert
    suspend fun insertPlaylist(list: Playlist)

    @Query("DELETE FROM playlist WHERE playlistId == :playlistId")
    suspend fun deleteById(playlistId: Long)

    @Query("UPDATE playlist SET playlistName = :newName WHERE playlistId == :playlistId")
    suspend fun saveNewNameById(playlistId: Long,newName:String)

    @Transaction
    @Delete
    suspend fun deleteListItem(song: PlaylistSongCrossRef)


    @Transaction
    @Delete
    suspend fun deleteListItems(songs: List<PlaylistSongCrossRef>)

}