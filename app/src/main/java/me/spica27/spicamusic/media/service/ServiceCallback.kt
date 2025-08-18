package me.spica27.spicamusic.media.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import me.spica27.spicamusic.media.common.CustomCommand
import me.spica27.spicamusic.media.common.registerCustomCommands
import me.spica27.spicamusic.media.common.toCustomCommendOrNull
import me.spica27.spicamusic.media.utils.MediaLibrary
import me.spica27.spicamusic.media.utils.PlayerKVUtils
import org.koin.java.KoinJavaComponent.getKoin


@OptIn(UnstableApi::class)
class ServiceCallback(
  private val player: Player
) : MediaLibrarySession.Callback {

  private val playerKVUtils = getKoin().get<PlayerKVUtils>()


  override fun onConnect(
    session: MediaSession,
    controller: MediaSession.ControllerInfo
  ): MediaSession.ConnectionResult {
    val sessionCommands = MediaSession.ConnectionResult
      .DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
      .registerCustomCommands()
      .build()

    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
      .setAvailableSessionCommands(sessionCommands)
      .build()
  }

  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle
  ): ListenableFuture<SessionResult> {
    val action = customCommand.toCustomCommendOrNull()
      ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))

    when (action) {
      CustomCommand.ACTION_SKIP_NEXT -> player.seekToNext()
      CustomCommand.ACTION_SKIP_PREV -> player.seekToPrevious()
      CustomCommand.ACTION_PLAY_PAUSE -> player.playWhenReady = !player.playWhenReady
      else -> {}
    }

    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
  }

  private fun buildBrowsableItem(id: String, title: String): MediaItem {
    val metadata = MediaMetadata.Builder()
      .setTitle(title)
      .setIsBrowsable(true)
      .setIsPlayable(false)
      .build()

    return MediaItem.Builder()
      .setMediaId(id)
      .setMediaMetadata(metadata)
      .build()
  }

  private fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
    return mediaItems.mapNotNull { item -> MediaLibrary.getItem(item.mediaId) }
  }

  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: MediaLibraryService.LibraryParams?
  ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
    LibraryResult.ofItem(buildBrowsableItem(MediaLibrary.ROOT, "SPICa Music Library"), params)
  )

  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    if (parentId == MediaLibrary.ROOT) {
      return Futures.immediateFuture(
        LibraryResult.ofItemList(
          listOf(
            buildBrowsableItem(MediaLibrary.ALL_SONGS, "All Songs"),
          ),
          params
        )
      )
    }

    return Futures.submitAsync<LibraryResult<ImmutableList<MediaItem>>>({
      return@submitAsync Futures.immediateFuture(
        LibraryResult.ofItemList(
          MediaLibrary.getChildren(
            parentId
          ), params
        )
      )
    }, Dispatchers.IO.asExecutor())
  }

  override fun onGetItem(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String
  ): ListenableFuture<LibraryResult<MediaItem>> {


    return Futures.submitAsync<LibraryResult<MediaItem>>({
      val item = MediaLibrary.getItem(mediaId)
      Futures.immediateFuture(
        if (item != null) LibraryResult.ofItem(item, null)
        else LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      )
    }, Dispatchers.IO.asExecutor())
  }

  override fun onSetMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
    startIndex: Int,
    startPositionMs: Long
  ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
    Futures.submitAsync<MediaSession.MediaItemsWithStartPosition>({
      val mediaItems = resolveMediaItems(mediaItems)
      Futures.immediateFuture(
        MediaSession.MediaItemsWithStartPosition(
          mediaItems,
          startIndex,
          startPositionMs
        )
      )
    }, Dispatchers.IO.asExecutor())

  override fun onAddMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>
  ): ListenableFuture<MutableList<MediaItem>> = Futures.submitAsync({
    val mediaItems = resolveMediaItems(mediaItems)
    Futures.immediateFuture(
      mediaItems.toMutableList()
    )
  }, Dispatchers.IO.asExecutor())

  override fun onPlaybackResumption(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo
  ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
    return Futures.submitAsync({
      val historyItems = playerKVUtils.getHistoryItems().map {
        it.toMediaItem()
      }
      Futures.immediateFuture(
        MediaSession.MediaItemsWithStartPosition(historyItems, 0, 0L)
      )
    }, Dispatchers.IO.asExecutor())
  }

}