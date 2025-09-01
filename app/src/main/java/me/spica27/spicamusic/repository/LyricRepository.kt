package me.spica27.spicamusic.repository

import androidx.annotation.WorkerThread
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.entity.Lyric
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.network.LyricApi
import me.spica27.spicamusic.network.bean.LyricResponse

class LyricRepository(
  private val lyricApi: LyricApi,
  private val lyricDao: LyricDao
) {


  suspend fun fetchLyric(title: String, artist: String?): ApiResponse<List<LyricResponse>> =
    lyricApi.fetchLyric(title, artist)

  suspend fun saveSongLyric(lyric: LyricResponse, song: Song) = withContext(Dispatchers.IO) {
    lyricDao.deleteLyric(
      song.songId ?: -1
    )
    lyricDao.insertLyric(
      Lyric(
        mediaId = song.mediaStoreId,
        lyrics = lyric.lyrics,
        cover = lyric.cover ?: ""
      )
    )
  }


  fun getLyrics(mediaId: Long) = lyricDao.getLyricsFlow(mediaId)
    .distinctUntilChanged()

  fun getDelay(mediaId: Long) = lyricDao.getDelayFlow(mediaId)
    .map {
      it ?: 0
    }
    .distinctUntilChanged()

  @WorkerThread
  fun setDelay(mediaId: Long, delay: Long) {
    lyricDao.updateDelay(mediaId, delay)
  }

}