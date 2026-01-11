package me.spica27.spicamusic.service

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

/**
 * 媒体播放后台服务
 * 使用 Media3 MediaLibraryService 实现后台播放
 */
@UnstableApi
class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        player =
            ExoPlayer
                .Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .build()

        mediaSession =
            MediaLibrarySession
                .Builder(this, player, object : MediaLibrarySession.Callback {})
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
