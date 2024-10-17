package me.spica27.spicamusic.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.PlaylistWithSongs
import me.spica27.spicamusic.db.entity.Song


@Dao
interface PlaylistDao {


  @Transaction
  @Query("SELECT * FROM Playlist")
  fun getPlaylistsWithSongs(): List<PlaylistWithSongs>

  @Transaction
  @Query("SELECT * FROM Playlist")
  fun getAllPlaylist(): Flow<List<Playlist>>


  @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
  fun getPlayListById(playlistId: Long): Flow<Playlist>


  @Transaction
  @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
  fun getPlaylistsWithSongsWithPlayListId(playlistId: Long): PlaylistWithSongs?


  @Transaction
  @Query("SELECT * FROM Playlist WHERE playlistId == :playlistId")
  fun getPlaylistsWithSongsWithPlayListIdFlow(playlistId: Long): Flow<PlaylistWithSongs?>


  @Query("SELECT * FROM Song WHERE songId IN (SELECT songId FROM PlaylistSongCrossRef WHERE playlistId == :playlistId)")
  fun getSongsByPlaylistId(playlistId: Long): Flow<List<Song>>

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
  suspend fun saveNewNameById(playlistId: Long, newName: String)

  @Transaction
  @Delete
  suspend fun deleteListItem(song: PlaylistSongCrossRef)


  @Transaction
  @Delete
  suspend fun deleteListItems(songs: List<PlaylistSongCrossRef>)

}