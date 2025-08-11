package me.spica27.spicamusic.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.MutableStateFlow
import me.spica27.spicamusic.App
import timber.log.Timber

class MusicServiceConnection(
  context: Context = App.getInstance(),
  serviceComponent: ComponentName = ComponentName(App.getInstance(), MusicService::class.java)
) {

  private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)


  private val mediaBrowser = MediaBrowserCompat(
    context,
    serviceComponent,
    mediaBrowserConnectionCallback,
    null
  ).apply {
    connect()
  }

  private var _mediaController: MediaControllerCompat? = null

  private val _isConnected = MutableStateFlow(false)

  private val mediaControllerCallback = MediaControllerCallback()

  var playbackState: PlaybackStateCompat? = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()
    private set


  val transportControls: MediaControllerCompat.TransportControls?
    get() = _mediaController?.transportControls

  private inner class MediaBrowserConnectionCallback(private val context: Context) :
    MediaBrowserCompat.ConnectionCallback() {

    override fun onConnected() {
      _mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
        registerCallback(mediaControllerCallback)
      }
      _isConnected.value = true
      Timber.tag("MediaBrowser").e("onConnected")
    }

    override fun onConnectionSuspended() {
      _mediaController = null
      _isConnected.value = false
      Timber.tag("MediaBrowser").e("onConnectionSuspended")
    }

    override fun onConnectionFailed() {
      _mediaController = null
      _isConnected.value = false
      Timber.tag("MediaBrowser").e("onConnectionFailed")
    }
  }


  private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      playbackState = state
    }

    override fun onSessionReady() {

    }

    override fun onSessionEvent(event: String?, extras: Bundle?) {
      playbackState = _mediaController?.playbackState
    }

    override fun onSessionDestroyed() {
      mediaBrowserConnectionCallback.onConnectionSuspended()
    }

  }

}