package me.spica27.spicamusic.media.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX
import androidx.media3.session.DefaultMediaNotificationProvider.GROUP_KEY
import androidx.media3.session.DefaultMediaNotificationProvider.NotificationIdProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaNotification.Provider.Callback
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.R
import androidx.media3.session.SessionCommand
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.lyric.LrcParser
import me.spica27.spicamusic.lyric.LyricItem
import me.spica27.spicamusic.lyric.findPlayingIndex
import me.spica27.spicamusic.lyric.getSentenceContent
import me.spica27.spicamusic.utils.toBitmap
import org.koin.java.KoinJavaComponent.getKoin
import java.util.*
import kotlin.coroutines.CoroutineContext


@UnstableApi
internal class SpicaNotificationProvider(
  private val context: Context
) : MediaNotification.Provider, CoroutineScope {

  companion object {

    private const val FLAG_ALWAYS_SHOW_TICKER = 0x1000000
    private const val FLAG_ONLY_UPDATE_TICKER = 0x2000000
  }

  override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

  private val lyricDao = getKoin().get<LyricDao>()

  private val notificationManager: NotificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val channelId: String = DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID
  private val channelName: String by lazy { context.getString(DefaultMediaNotificationProvider.DEFAULT_CHANNEL_NAME_RESOURCE_ID) }
  private val notificationIdProvider = NotificationIdProvider { session: MediaSession? ->
    DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID
  }

  override fun createNotification(
    mediaSession: MediaSession,
    customLayout: ImmutableList<CommandButton>,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: Callback
  ): MediaNotification {
    ensureNotificationChannel()

    val customLayoutWithEnabledCommandButtonsOnly = ImmutableList.Builder<CommandButton>()
    val player = mediaSession.player
    val builder = NotificationCompat.Builder(context, channelId)
    val notificationId: Int = notificationIdProvider.getNotificationId(mediaSession)
    val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)

    val mediaButtons = getMediaButtons(
      mediaSession,
      player.availableCommands,
      customLayoutWithEnabledCommandButtonsOnly.build(),
      !Util.shouldShowPlayButton(player, mediaSession.showPlayButtonIfPlaybackIsSuppressed)
    )

    val compactViewIndices: IntArray =
      addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)
    mediaStyle.setShowActionsInCompactView(*compactViewIndices)

    if (player.isCommandAvailable(Player.COMMAND_GET_METADATA)) {
      val metadata = player.mediaMetadata
      val mediaItem = player.currentMediaItem

      builder
        .setContentTitle(metadata.title)
        .setSmallIcon(me.spica27.spicamusic.R.mipmap.ic_launcher)
        .setContentText(metadata.artist)

      loadBitmapIntoNotification(
        mediaSession = mediaSession,
        metadata = metadata,
        notificationId = notificationId,
        builder = builder,
        onNotificationChangedCallback = onNotificationChangedCallback
      )

      loadLyricIntoNotification(
        mediaSession = mediaSession,
        mediaItem = mediaItem,
        notificationId = notificationId,
        builder = builder,
        onNotificationChangedCallback = onNotificationChangedCallback
      )
    }


    val playbackStartTimeMs = getPlaybackStartTimeEpochMs(player)
    val displayElapsedTimeWithChronometer = playbackStartTimeMs != C.TIME_UNSET
    builder
      .setWhen(if (displayElapsedTimeWithChronometer) playbackStartTimeMs else 0L)
      .setShowWhen(displayElapsedTimeWithChronometer)
      .setUsesChronometer(displayElapsedTimeWithChronometer)

    if (Build.VERSION.SDK_INT >= 31) {
      builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
    }


    val notification: Notification = builder
      .setContentIntent(mediaSession.sessionActivity)
      .setDeleteIntent(
        actionFactory.createMediaActionPendingIntent(
          mediaSession, Player.COMMAND_STOP.toLong()
        )
      )
      .setOnlyAlertOnce(true)
      .setStyle(mediaStyle)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(false)
      .setGroup(GROUP_KEY)
      .build()
    return MediaNotification(notificationId, notification)
  }

  override fun handleCustomCommand(
    session: MediaSession,
    action: String,
    extras: Bundle
  ): Boolean = false


  private fun addNotificationActions(
    mediaSession: MediaSession,
    mediaButtons: ImmutableList<CommandButton>,
    builder: NotificationCompat.Builder,
    actionFactory: MediaNotification.ActionFactory
  ): IntArray {
    val compactViewIndices = IntArray(3)
    val defaultCompactViewIndices = IntArray(3)
    Arrays.fill(compactViewIndices, C.INDEX_UNSET)
    Arrays.fill(defaultCompactViewIndices, C.INDEX_UNSET)

    mediaButtons.forEachIndexed { index, button ->
      if (button.sessionCommand != null) {
        builder.addAction(
          actionFactory.createCustomActionFromCustomCommandButton(
            mediaSession,
            button
          )
        )
      } else {
        Assertions.checkState(button.playerCommand != Player.COMMAND_INVALID)
        builder.addAction(
          actionFactory.createMediaAction(
            mediaSession,
            IconCompat.createWithResource(context, button.iconResId),
            button.displayName,
            button.playerCommand
          )
        )
      }

      val compactViewIndex = button.extras
        .getInt(COMMAND_KEY_COMPACT_VIEW_INDEX, C.INDEX_UNSET)

      if (compactViewIndex >= 0 && compactViewIndex < compactViewIndices.size) {
        // 将当前展开状态下的元素index存储在，收窄状态数组中的自定义index位置处
        compactViewIndices[compactViewIndex] = index
      }

      // 记录默认元素的下标至默认数组
      when (button.playerCommand) {
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> defaultCompactViewIndices[0] = index

        Player.COMMAND_PLAY_PAUSE -> defaultCompactViewIndices[1] = index
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> defaultCompactViewIndices[2] = index
      }
    }

    // 若compactViewIndices[0]为-1，则说明没有设置自定义下标，则使用默认下标
    return if (compactViewIndices[0] == C.INDEX_UNSET) {
      defaultCompactViewIndices
    } else {
      compactViewIndices
    }.let { indices ->
      val unsetItemIndex = indices.indexOfFirst { it == C.INDEX_UNSET }

      if (unsetItemIndex != -1) indices.copyOf(unsetItemIndex) else indices
    }
  }


  private fun getMediaButtons(
    session: MediaSession?,
    playerCommands: Player.Commands,
    customLayout: ImmutableList<CommandButton>,
    showPauseButton: Boolean
  ): ImmutableList<CommandButton> {
    val commandButtons = ImmutableList.Builder<CommandButton>()

    // Skip to previous action.
    if (playerCommands.containsAny(
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
      )
    ) commandButtons.add(
      CommandButton.Builder(CommandButton.ICON_PREVIOUS)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        .setDisplayName(context.getString(R.string.media3_controls_seek_to_previous_description))
        .setExtras(createCommandButtonExtra())
        .build()
    )

    if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
      if (showPauseButton) commandButtons.add(
        CommandButton.Builder(CommandButton.ICON_PAUSE)
          .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
          .setExtras(createCommandButtonExtra())
          .setDisplayName(context.getString(R.string.media3_controls_pause_description))
          .build()
      ) else commandButtons.add(
        CommandButton.Builder(CommandButton.ICON_PLAY)
          .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
          .setExtras(createCommandButtonExtra())
          .setDisplayName(context.getString(R.string.media3_controls_play_description))
          .build()
      )
    }

    // Skip to next action.
    if (playerCommands.containsAny(
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
      )
    ) commandButtons.add(
      CommandButton.Builder(CommandButton.ICON_NEXT)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        .setExtras(createCommandButtonExtra())
        .setDisplayName(context.getString(R.string.media3_controls_seek_to_next_description))
        .build()
    )

    customLayout.asSequence()
      .filter { it.isEnabled && it.sessionCommand?.commandCode == SessionCommand.COMMAND_CODE_CUSTOM }
      .forEach { commandButtons.add(it) }

    return commandButtons.build()
  }


  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT < 26 || notificationManager.getNotificationChannel(channelId) != null) {
      return
    }

    val channel =
      NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
    if (Build.VERSION.SDK_INT <= 27) {
      channel.setShowBadge(false)
    }
    notificationManager.createNotificationChannel(channel)
  }


  private fun loadBitmapIntoNotification(
    mediaSession: MediaSession,
    metadata: MediaMetadata,
    notificationId: Int,
    builder: NotificationCompat.Builder,
    onNotificationChangedCallback: Callback
  ) {
    val reqBitmap = ImageRequest.Builder(context)
      .data(metadata.artworkUri)
      .size(100)
      .target(
        onSuccess = {
          builder.setLargeIcon(it.toBitmap())
          onNotificationChangedCallback.onNotificationChanged(
            MediaNotification(notificationId, builder.build())
          )
        },
        onError = {
          builder.setLargeIcon(
            ContextCompat.getDrawable(
              context,
              me.spica27.spicamusic.R.drawable.default_cover
            )?.toBitmap()
          )
          onNotificationChangedCallback.onNotificationChanged(
            MediaNotification(notificationId, builder.build())
          )
        }
      )
      .build()
  }

  private var loadLyricJob: Job? = null
  private fun loadLyricIntoNotification(
    mediaSession: MediaSession,
    mediaItem: MediaItem?,
    notificationId: Int,
    builder: NotificationCompat.Builder,
    onNotificationChangedCallback: Callback
  ) {
    loadLyricJob?.cancel()
    if (mediaItem == null) return

    loadLyricJob = launch(Dispatchers.IO) {
      // 加载歌词

      val lyrics = LrcParser.parse(
        lyricDao.getLyricWithMediaId(mediaItem.mediaId.toLongOrNull() ?: -1)?.lyrics ?: ""
      )


      var lastIndex = -1
      while (isActive) {

        val list = lyrics
        val time = withContext(Dispatchers.Main) { mediaSession.player.currentPosition }

        val index = list.findPlayingIndex(time)
        if (lastIndex == index) {
          delay(1000)
          continue
        }

        lastIndex = index
        val current = list.getOrNull(index)

        if (current != null) {
          withContext(Dispatchers.Main) {


            val text = when (current) {
              is LyricItem.NormalLyric -> current.content
              is LyricItem.WordsLyric -> current.getSentenceContent()
            }

            builder.setTicker(text)
            val notification = builder.build().apply {
              flags = flags or FLAG_ALWAYS_SHOW_TICKER or FLAG_ONLY_UPDATE_TICKER
            }

            onNotificationChangedCallback.onNotificationChanged(
              MediaNotification(notificationId, notification)
            )
          }
        }
        delay(1000)
      }
    }
  }


  private fun createCommandButtonExtra() =
    Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, C.INDEX_UNSET) }

  private fun getPlaybackStartTimeEpochMs(player: Player): Long {
    return if ((player.isPlaying
          && !player.isPlayingAd
          && !player.isCurrentMediaItemDynamic) && player.playbackParameters.speed == 1f
    ) {
      System.currentTimeMillis() - player.contentPosition
    } else {
      C.TIME_UNSET
    }
  }

}