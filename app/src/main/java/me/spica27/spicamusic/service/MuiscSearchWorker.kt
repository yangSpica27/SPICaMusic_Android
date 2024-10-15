package me.spica27.spicamusic.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.utils.AudioTool

import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class RefreshMusicListService : Service() {

    @Inject
    lateinit var songDao: SongDao


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        doWork()
        return START_STICKY
    }

    private fun doWork() {


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val songs = AudioTool.getSongsFromPhone(applicationContext)
                Timber.tag("更新曲目成功").e("共${songs.size}首")
                songDao.updateSongs(songs.filter { it.displayName.endsWith(".mp3") })
                stopSelf()
            } catch (e: Exception) {
                Timber.tag("更新曲目错误").e(e)
            }
        }

    }


    override fun onBind(intent: Intent?): IBinder? = null
}