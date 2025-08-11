package me.spica27.spicamusic.player

import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.PlayHistory
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.PlayHistoryRepository
import me.spica27.spicamusic.service.MusicServiceConnection
import timber.log.Timber


class SPicaPlayer(
  private val musicServiceConnection: MusicServiceConnection,
  private val playHistoryRepository: PlayHistoryRepository
) : IPlayer {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  override fun loadSong(song: Song?, play: Boolean) {
    if (song == null) return
    if (musicServiceConnection.transportControls==null){
      Timber.e("transportControls is null")
    }
    musicServiceConnection.transportControls?.playFromUri(
      song.getSongUri(),
      null
    )
    musicServiceConnection.transportControls?.play()
    coroutineScope.launch(
      Dispatchers.IO
    ) {
      playHistoryRepository.insertPlayHistory(
        PlayHistory(
          mediaId = song.mediaStoreId,
          time = System.currentTimeMillis(),
          title = song.displayName,
          artist = song.artist,
        )
      )
    }
  }

  override fun getState(durationMs: Long): IPlayer.State {
    val state = musicServiceConnection.playbackState
    if (state == null) {
      return IPlayer.State.from(false, 0)
    }

    return IPlayer.State.from(
      state.state == PlaybackStateCompat.STATE_PLAYING,
      state.position.coerceIn(0, durationMs),
    )

  }

  override fun seekTo(positionMs: Long) {
    musicServiceConnection.transportControls?.seekTo(positionMs)
  }

  override fun setPlaying(isPlaying: Boolean) {
    if (isPlaying) {
      musicServiceConnection.transportControls?.play()
    } else {
      musicServiceConnection.transportControls?.pause()
    }
  }

}