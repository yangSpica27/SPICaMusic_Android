package me.spica27.spicamusic.service

import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import me.spica27.spicamusic.dsp.InteractiveAudioTrackBufferSizeProvider
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.impl.utils.MediaLibrary
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * 媒体播放后台服务
 * 使用 Media3 MediaLibraryService 实现后台播放
 * 通过 Rust/OxiMedia 引擎进行频谱分析、EQ 和响度处理
 */
@UnstableApi
class PlaybackService : MediaLibraryService() {
    private val player: IMusicPlayer by inject()

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var notificationProvider: SpicaNotificationProvider

    // 服务级别的协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalApi::class)
    override fun onCreate() {
        super.onCreate()
        notificationProvider = SpicaNotificationProvider(this)
        setMediaNotificationProvider(notificationProvider)
        // 创建自定义渲染器工厂，在公共 PCM 输出层接入 Rust/OxiMedia 引擎。
        val renderersFactory =
            object : DefaultRenderersFactory(this) {
                init {
                    setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
                }

                override fun buildAudioSink(
                    context: android.content.Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink =
                    player.wrapAudioSink(
                        DefaultAudioSink
                            .Builder(context)
                            .setEnableFloatOutput(true)
                            .setEnableAudioOutputPlaybackParameters(true)
                            .setAudioOutputProvider(
                                AudioTrackAudioOutputProvider
                                    .Builder(context)
                                    // 倍速上限与缓冲策略必须一致，否则加速时 AudioTrack 缓冲会欠载。
                                    .setMaxPlaybackSpeed(InteractiveAudioTrackBufferSizeProvider.MAX_PLAYBACK_SPEED)
                                    .setAudioTrackBufferSizeProvider(InteractiveAudioTrackBufferSizeProvider())
                                    .build(),
                            ).build(),
                    )
            }
        exoPlayer =
            ExoPlayer
                .Builder(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        createAttributionContext("audioPlayback")
                    } else {
                        this
                    },
                    renderersFactory,
                ).setWakeMode(C.WAKE_MODE_LOCAL)
                .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)
                .experimentalSetDynamicSchedulingEnabled(false)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
                        .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                ).setUsePlatformDiagnostics(false)
                .build()

        mediaSession =
            MediaLibrarySession
                .Builder(
                    this,
                    exoPlayer,
                    object : MediaLibrarySession.Callback {
                        override fun onGetLibraryRoot(
                            session: MediaLibrarySession,
                            browser: MediaSession.ControllerInfo,
                            params: LibraryParams?,
                        ): ListenableFuture<LibraryResult<MediaItem>> =
                            Futures.immediateFuture(
                                LibraryResult.ofItem(
                                    MediaItem
                                        .Builder()
                                        .setMediaId(MediaLibrary.ROOT)
                                        .setMediaMetadata(
                                            MediaMetadata
                                                .Builder()
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                                .build(),
                                        ).build(),
                                    params,
                                ),
                            )

                        override fun onGetItem(
                            session: MediaLibrarySession,
                            browser: MediaSession.ControllerInfo,
                            mediaId: String,
                        ): ListenableFuture<LibraryResult<MediaItem>> {
                            Timber.tag("PlaybackService").d("onGetItem: mediaId=$mediaId")
                            return serviceScope.future {
                                val item = MediaLibrary.getItem(mediaId)
                                if (item != null) {
                                    Timber
                                        .tag("PlaybackService")
                                        .d("onGetItem: Found item ${item.mediaMetadata.title}")
                                    LibraryResult.ofItem(item, null)
                                } else {
                                    Timber
                                        .tag("PlaybackService")
                                        .e("onGetItem: Item not found for mediaId=$mediaId")
                                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                                }
                            }
                        }

                        override fun onGetChildren(
                            session: MediaLibrarySession,
                            browser: MediaSession.ControllerInfo,
                            parentId: String,
                            page: Int,
                            pageSize: Int,
                            params: LibraryParams?,
                        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
                            serviceScope.future {
                                val children = MediaLibrary.getChildren(parentId)
                                LibraryResult.ofItemList(children, params)
                            }

//                        override fun onPlaybackResumption(
//                            session: MediaSession,
//                            controller: MediaSession.ControllerInfo,
//                        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
//                            Timber.tag("PlaybackService").d("onPlaybackResumption called")
//                            // 从播放历史恢复最后播放的歌曲列表
//                            val items = MediaLibrary.getChildren(MediaLibrary.ALL_SONGS)
//                            return Futures.immediateFuture(
//                                MediaSession.MediaItemsWithStartPosition(
//                                    items,
//                                    0,
//                                    0,
//                                ),
//                            )
//                        }
                    },
                ).setBitmapLoader(ArtworkBitmapLoader())
                .setSessionActivity(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        packageManager.getLaunchIntentForPackage(packageName),
                        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        notificationProvider.cancel()
        exoPlayer.release()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
