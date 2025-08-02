package me.spica27.spicamusic.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.utils.AudioTool
import org.koin.android.ext.android.get
import timber.log.Timber


class RefreshMusicListService : Service() {


  private val songDao: SongDao = get()


  private val lyricDao: LyricDao = get()


  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    doWork()
    return START_STICKY
  }

  private fun doWork() {


    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
      try {
        val songs = AudioTool.getSongsFromPhone(applicationContext, lyricDao)
        Timber.tag("更新曲目成功").e("共${songs.size}首")
        songDao.updateSongs(songs)
        stopSelf()
      } catch (e: Exception) {
        Timber.tag("更新曲目错误").e(e)
      }
    }

  }


  override fun onBind(intent: Intent?): IBinder? = null
}