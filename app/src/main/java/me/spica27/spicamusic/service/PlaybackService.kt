package me.spica27.spicamusic.service

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import me.spica27.spicamusic.player.api.IMusicPlayer
import org.koin.android.ext.android.inject

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

    override fun onCreate() {
        super.onCreate()

        // 创建自定义渲染器工厂，添加 FFT 音频处理器
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
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(
                            arrayOf(
                                player.audioEffectProcessor.getAudioProcessor(),
                                player.fftAudioProcessor,
                            ),
                        ).build()
            }

        exoPlayer =
            ExoPlayer
                .Builder(this, renderersFactory)
                .setHandleAudioBecomingNoisy(true)
                .build()

        mediaSession =
            MediaLibrarySession
                .Builder(
                    this,
                    exoPlayer,
                    object : MediaLibrarySession.Callback {},
                ).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            exoPlayer.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
