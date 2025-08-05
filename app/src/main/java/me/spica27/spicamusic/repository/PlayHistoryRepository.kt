package me.spica27.spicamusic.repository

import androidx.annotation.WorkerThread
import me.spica27.spicamusic.db.dao.PlayHistoryDao
import me.spica27.spicamusic.db.entity.PlayHistory

class PlayHistoryRepository(
  private val playHistoryDao: PlayHistoryDao,
) {


  /**
   * 获取播放次数
   */
  @WorkerThread
  fun getSongPlayCount(mediaId: Long): Long {
    return playHistoryDao.getPlayCount(mediaId)
  }

  /**
   * 获取上次播放时间
   */
  @WorkerThread
  fun getLastPlayTime(mediaId: Long): Long {
    return playHistoryDao.getLasePlayHistory(mediaId)?.time ?: 0L
  }


  @WorkerThread
  fun insertPlayHistory(item: PlayHistory) {
    playHistoryDao.insert(playHistory = item)
  }

}