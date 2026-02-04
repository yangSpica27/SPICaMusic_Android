package me.spica27.spicamusic.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
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
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.impl.utils.MediaLibrary
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * 媒体播放后台服务
 * 使用 Media3 MediaLibraryService 实现后台播放
 * 集成 FFT 音频处理器进行频谱分析
 */
@UnstableApi
class PlaybackService : MediaLibraryService() {
    private val player: IMusicPlayer by inject()

    private var mediaSession: MediaLibrarySession? = null
    private lateinit var exoPlayer: ExoPlayer

    // 服务级别的协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            SpicaNotificationProvider(this),
        )
        // 创建自定义渲染器工厂，添加音频处理器（FFT、EQ、混响）
        val renderersFactory =
            object : DefaultRenderersFactory(this) {
                override fun buildAudioSink(
                    context: android.content.Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean,
                ): AudioSink =
                    DefaultAudioSink
                        .Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(
                            // 音频处理链: FFT -> EQ -> Reverb
                            (player as? me.spica27.spicamusic.player.impl.SpicaPlayer)
                                ?.getAudioProcessors()
                                ?: arrayOf(player.fftAudioProcessor),
                        ).build()
                        .apply {
                            setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
                        }
            }

        exoPlayer =
            ExoPlayer
                .Builder(this, renderersFactory)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)
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
                                        .build(),
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
                                    Timber.tag("PlaybackService").d("onGetItem: Found item ${item.mediaMetadata.title}")
                                    LibraryResult.ofItem(item, null)
                                } else {
                                    Timber.tag("PlaybackService").e("onGetItem: Item not found for mediaId=$mediaId")
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
                ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            exoPlayer.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
