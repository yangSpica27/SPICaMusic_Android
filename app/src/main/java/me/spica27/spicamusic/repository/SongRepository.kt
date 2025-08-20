package me.spica27.spicamusic.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.dao.SongDao

class SongRepository(
  private val songDao: SongDao
) {

  fun allFlow() = songDao.getAll()

  fun allLikeSongFlow() = songDao.getAllLikeSong()

  fun oftenListenSong10Flow() = songDao.getOftenListenSong10()

  fun oftenListenSongFlow() = songDao.getOftenListenSongs()

  fun randomSongFlow() = songDao.randomSong()

  fun songFlowWithId(id: Long) = songDao.getSongFlowWithId(id)

  suspend fun toggleLike(id: Long) = withContext(Dispatchers.IO) {
    songDao.toggleLike(id)
  }

  suspend fun ignore(id: Long, isIgnore: Boolean) = withContext(Dispatchers.IO) {
    songDao.ignore(id, isIgnore)
  }

  fun songLikeFlowWithId(id: Long) = songDao.getSongIsLikeFlowWithId(id).distinctUntilChanged().map {
    it == 1
  }

  fun ignoreSongFlow() = songDao.getIgnoreSongsFlow().distinctUntilChanged()

}