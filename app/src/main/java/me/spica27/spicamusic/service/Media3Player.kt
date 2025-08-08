package me.spica27.spicamusic.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.player.IPlayer
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class Media3Player(
  private val context: Context
) : IPlayer, CoroutineScope {


  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main+ SupervisorJob()

  private val sessionToken by lazy {
    SessionToken(context, ComponentName(context, MediaSessionService2::class.java))
  }


  private val controller by lazy {
    MediaController.Builder(context, sessionToken)
      .setListener(
        @UnstableApi
        object : MediaController.Listener {


          override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
          ): ListenableFuture<SessionResult> {
            return super.onCustomCommand(controller, command, args)
            Timber.tag("Media3Player").e("onCustomCommand commands = ${command}")
          }

          override fun onError(controller: MediaController, sessionError: SessionError) {
            super.onError(controller, sessionError)
          }

          override fun onAvailableSessionCommandsChanged(
            controller: MediaController,
            commands: SessionCommands
          ) {
            super.onAvailableSessionCommandsChanged(controller, commands)
            Timber.tag("Media3Player")
              .e("onAvailableSessionCommandsChanged commands = ${commands.commands}")
          }
        })
      .buildAsync()
  }


  override fun loadSong(song: Song?, play: Boolean) {
    launch {
      val controller = controller.await()
      if (song != null) {
        controller.setMediaItem(
          MediaItem.Builder()
            .setUri(song.getSongUri())
            .build()
        )
        controller.playWhenReady = play
        if (play){
          controller.play()
        }
      } else {
        controller.stop()
      }
    }
  }

  override fun getState(durationMs: Long,callback:(IPlayer.State)-> Unit) {
   launch {
     val controller = controller.await()
     Timber.tag("Media3Player").e("getState isPlaying = ${controller.isPlaying}")
     callback.invoke(IPlayer.State(
       controller.isPlaying,
       controller.currentPosition
         .coerceAtLeast(0)
         .coerceAtMost(durationMs)
     ))
   }
  }

  override fun seekTo(positionMs: Long) {
    launch {
      val controller = controller.await()
      controller.seekTo(positionMs)
    }
  }

  override fun setPlaying(isPlaying: Boolean) {
    launch {
      val controller = controller.await()
      if (isPlaying) {
        controller.play()
      } else {
        controller.pause()
      }
    }

  }

}