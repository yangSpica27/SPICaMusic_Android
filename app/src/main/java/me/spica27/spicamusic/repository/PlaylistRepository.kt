package me.spica27.spicamusic.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.PlaylistWithSongs
import me.spica27.spicamusic.db.entity.Song

class PlaylistRepository(
  private val playlistDao: PlaylistDao,
) {


  fun getSongsByPlaylistIdFlow(
    playlistId: Long
  ) = playlistDao.getSongsByPlaylistIdFlow(playlistId)

  fun getPlayListByIdFlow(playlistId: Long) = playlistDao.getPlayListByIdFlow(playlistId)

  fun getAllPlaylistFlow() = playlistDao.getAllPlaylist()


  fun getPlaylistsHaveSong(songId: Long) = playlistDao.getPlaylistsHaveSong(songId)

  fun getPlaylistsNotHaveSong(songId: Long) = playlistDao.getPlaylistsNotHaveSong(songId)

  fun songInfoWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?> {
    return playlistDao.getPlaylistsWithSongsWithPlayListIdFlow(playlistId).flowOn(Dispatchers.IO)
      .distinctUntilChanged()
  }


  suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
    playlistDao.insertPlaylist(Playlist(playlistName = name))
  }

  suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
    playlistDao.deleteById(id)
  }

  suspend fun renamePlaylist(
    newName: String,
    playlistId: Long?
  ) = withContext(Dispatchers.IO) {
    playlistId.let { playlistDao.renamePlaylist(it ?: -1, newName) }
  }

  // 添加歌曲到歌单
  suspend fun addSongToPlaylist(playlistId: Long?, songId: Long) = withContext(Dispatchers.IO) {
    if (playlistId != null) {
      playlistDao.insertListItem(PlaylistSongCrossRef(playlistId, songId))
    }
  }

  // 从歌单中移除歌曲
  suspend fun removeSongFromPlaylist(playlistId: Long?, songId: Long) =
    withContext(Dispatchers.IO) {
      if (playlistId != null) {
        playlistDao.deleteListItem(PlaylistSongCrossRef(playlistId, songId))
      }
    }

  // 从歌单中移除歌曲
  suspend fun removeSongsFromPlaylist(playlistId: Long?, songIds: List<Long>) = withContext(
    Dispatchers.IO
  ) {
    if (playlistId != null) {
      playlistDao.deleteListItems(songIds.map {
        PlaylistSongCrossRef(
          playlistId,
          it
        )
      })
    }
  }

  suspend fun createPlaylistWithSongs(name: String, list: List<Song>) = withContext(
    Dispatchers.IO
  ){
    val playlistId = playlistDao.insertPlaylistAndGetId(
      Playlist(playlistName = name)
    )
    playlistDao.insertListItems(
      list.map {
        PlaylistSongCrossRef(
          playlistId,
          it.songId ?: -1
        )
      }
    )
  }
}