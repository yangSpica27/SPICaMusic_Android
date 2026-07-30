@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.player.scene

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.geometry.geometryTarget
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.ProgressBarStyle
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.LyricsPanel
import me.spica27.spicamusic.ui.player.PlayerPlaybackBottomSection
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover

/**
 * 全屏歌词页面。
 *
 */
class LyricScene(
    private val heroArtworkUri: Uri? = null,
    private val coverTransition: GeometryTransition? = null,
    private val progressBarStyleProvider: () -> ProgressBarStyle = {
        ProgressBarStyle.ExpressiveWavy
    },
    private val amplitudesProvider: () -> List<Int> = { emptyList() },
) : StackScene() {
    override val transitionShadowEnabled: Boolean = false
    override val transitionScaleEnabled: Boolean = false
    override val transitionSlideEnabled: Boolean = true
    override val transitionFadeEnabled: Boolean = false
    override val compressesPreviousScene: Boolean = false

    override val geometryTransitions: List<GeometryTransition> =
        listOfNotNull(coverTransition)

    /** push 开始时重置全部过渡进度，保证多次进入都能完整播放飞行动画 */
    override suspend fun onPush() {
        super.onPush()
        geometryTransitions.forEach { it.reset() }
    }

    /** 进场：屏幕滑入与三个共享元素的飞行并发执行 */
    override suspend fun onAppear() {
        coroutineScope {
            launch { super.onAppear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateForward() }
            }
        }
    }

    /** 退场：屏幕滑出与共享元素反向飞回并发执行 */
    override suspend fun onDisappear() {
        coroutineScope {
            launch { super.onDisappear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateReverse() }
            }
        }
    }

    @Composable
    override fun FloatingContent(key: String) {
        val playerViewModel = LocalPlayerViewModel.current
        val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
        val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: heroArtworkUri
        when (key) {
            coverTransition?.key -> FlyingCover(uri = artworkUri)
        }
    }

    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current

        BackHandler(true) {
            path.popTop()
        }

        val playerViewModel = LocalPlayerViewModel.current
        val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

        // header 展示实时播放内容（歌曲切换时跟随更新），飞行浮层则使用 push 时捕获的快照
        val title =
            currentMediaItem
                ?.mediaMetadata
                ?.title
                ?.toString()
                ?: stringResource(R.string.unknown_song)
        val artist =
            currentMediaItem
                ?.mediaMetadata
                ?.artist
                ?.toString()
                ?: stringResource(R.string.unknown_artist)
        val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: heroArtworkUri
        val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
        val playMode by playerViewModel.playMode.collectAsStateWithLifecycle()
        val isLike by playerViewModel.currentSongIsLike.collectAsStateWithLifecycle()
        val position by playerViewModel.currentPosition.collectAsStateWithLifecycle()
        val duration by playerViewModel.currentDuration.collectAsStateWithLifecycle()
        val sleepTimerRemainingMs by playerViewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
        val playerColor by playerViewModel.playerThemeColor.collectAsStateWithLifecycle()

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            AudioCover(
                uri = artworkUri,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.28f
                            scaleY = 1.28f
                            alpha = 0.42f
                        }.blur(56.dp),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        playerColor.copy(alpha = 0.72f),
                                        playerColor.copy(alpha = 0.58f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    ),
                            ),
                        ),
            )

            Column(modifier = Modifier.fillMaxSize()) {
                LyricsHeader(
                    title = title,
                    artist = artist,
                    artworkUri = artworkUri,
                    coverTransition = coverTransition,
                    enterProgressProvider = { enterProgress.value },
                    onBack = { path.popTop() },
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    LyricsPanel(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
                    LyricsPlaybackControls(
                        position = position,
                        duration = duration,
                        isPlaying = isPlaying,
                        playMode = playMode,
                        isLike = isLike,
                        progressBarStyle = progressBarStyleProvider(),
                        amplitudes = amplitudesProvider(),
                        fftDrawData = playerViewModel.fftDrawData,
                        enterProgressProvider = { enterProgress.value },
                        onSeek = playerViewModel::seekTo,
                        onPrevious = playerViewModel::skipToPrevious,
                        onTogglePlay = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::skipToNext,
                        onPlayMode = playerViewModel::togglePlayMode,
                        onFavorite = playerViewModel::toggleLikeCurrentSong,
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        onSleepTimerSet = playerViewModel::setSleepTimer,
                        onSleepTimerCancel = playerViewModel::cancelSleepTimer,
                    )
                }
            }
        }
    }
}

/**
 * Lyrics page embedded beside the artwork page.
 *
 * [transitionProgressProvider] is driven directly by the horizontal pager, so
 * the header rotation and bottom-control handoff can follow the user's finger
 * and reverse cleanly when the gesture is cancelled.
 */
@Composable
fun LyricsPlayerPage(
    modifier: Modifier = Modifier,
    heroArtworkUri: Uri? = null,
    artworkPainter: Painter? = null,
    transitionProgressProvider: () -> Float,
    artworkModifier: Modifier = Modifier,
    showNavigationButton: Boolean = true,
    onBack: () -> Unit,
) {
    val playerViewModel = LocalPlayerViewModel.current
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val title =
        currentMediaItem
            ?.mediaMetadata
            ?.title
            ?.toString()
            ?: stringResource(R.string.unknown_song)
    val artist =
        currentMediaItem
            ?.mediaMetadata
            ?.artist
            ?.toString()
            ?: stringResource(R.string.unknown_artist)
    val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: heroArtworkUri
    val playerColor by playerViewModel.playerThemeColor.collectAsStateWithLifecycle()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                // The 56.dp ambient artwork blur must stay inside this pager page.
                // Without clipping it paints a vertical band over the player page
                // while the two pages are being dragged between.
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        val backdropModifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.28f
                    scaleY = 1.28f
                    alpha = 0.42f
                }.blur(56.dp)
        if (artworkPainter != null) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = backdropModifier,
            )
        } else {
            AudioCover(
                uri = artworkUri,
                modifier = backdropModifier,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    playerColor.copy(alpha = 0.72f),
                                    playerColor.copy(alpha = 0.58f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                ),
                        ),
                    ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            LyricsHeader(
                title = title,
                artist = artist,
                artworkUri = artworkUri,
                artworkPainter = artworkPainter,
                coverTransition = null,
                enterProgressProvider = transitionProgressProvider,
                artworkModifier = artworkModifier,
                showNavigationButton = showNavigationButton,
                onBack = onBack,
            )

            Column(modifier = Modifier.fillMaxSize()) {
                LyricsPanel(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LyricsPlaybackControls(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    playMode: PlayMode,
    isLike: Boolean,
    progressBarStyle: ProgressBarStyle,
    amplitudes: List<Int>,
    fftDrawData: kotlinx.coroutines.flow.StateFlow<FloatArray>,
    enterProgressProvider: () -> Float,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPlayMode: () -> Unit,
    onFavorite: () -> Unit,
    sleepTimerRemainingMs: Long?,
    onSleepTimerSet: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    val transitionProgress = enterProgressProvider().coerceIn(0f, 1f)
    val controlsExitProgress =
        smoothStep(
            ((transitionProgress - 0.12f) / 0.88f).coerceIn(0f, 1f),
        )
    val playbackProgress =
        if (duration > 0L) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val displayedProgress = if (isSeeking) seekProgress else playbackProgress

    PlayerPlaybackBottomSection(
        progress = displayedProgress,
        currentPosition = position,
        duration = duration,
        isPlaying = isPlaying,
        isSeeking = isSeeking,
        playMode = playMode,
        isLike = isLike,
        progressBarStyle = progressBarStyle,
        amplitudes = amplitudes,
        fftDrawData = fftDrawData,
        onProgressChange = {
            isSeeking = true
            seekProgress = it
        },
        onProgressChangeFinished = {
            onSeek((seekProgress * duration).toLong())
            isSeeking = false
        },
        onPlayPauseClick = onTogglePlay,
        onPreviousClick = onPrevious,
        onNextClick = onNext,
        onPlayModeClick = onPlayMode,
        onFavoriteClick = onFavorite,
        sleepTimerRemainingMs = sleepTimerRemainingMs,
        onSleepTimerSet = onSleepTimerSet,
        onSleepTimerCancel = onSleepTimerCancel,
        showControls = true,
        controlsModifier =
            Modifier.graphicsLayer {
                alpha = 1f - controlsExitProgress
                translationY = controlsExitProgress * 156.dp.toPx()
                scaleX = 1f - controlsExitProgress * 0.035f
                scaleY = 1f - controlsExitProgress * 0.035f
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = Spacing.Medium, bottom = Spacing.Large)
                .graphicsLayer {
                    translationY = -(1f - transitionProgress) * 48.dp.toPx()
                    alpha = 0.72f + transitionProgress * 0.28f
                },
    )
}

// ============================================
// Header
// ============================================

/**
 * 全屏歌词 header：返回按钮 + 封面缩略图 + 歌名 / 作者。
 *
 * 封面、歌名、作者是三个几何过渡的目标节点；
 * 飞行期间由浮层接管显示（[GeometryTransition.shouldShowTarget]），落定后才显示本体。
 */
@Composable
private fun LyricsHeader(
    title: String,
    artist: String,
    artworkUri: Uri?,
    artworkPainter: Painter? = null,
    coverTransition: GeometryTransition?,
    enterProgressProvider: () -> Float,
    artworkModifier: Modifier = Modifier,
    showNavigationButton: Boolean = true,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.Large)
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        IconButton(
            onClick = onBack,
            enabled = showNavigationButton,
            modifier =
                Modifier.graphicsLayer {
                    alpha = if (showNavigationButton) 1f else 0f
                },
            colors =
                IconButtonDefaults.iconButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.back),
                modifier =
                    Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            rotationZ = 90f * enterProgressProvider().coerceIn(0f, 1f)
                        },
            )
        }

        // 封面缩略图（飞行目标）
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .then(artworkModifier)
                    .graphicsLayer {
                        alpha =
                            if (coverTransition == null || coverTransition.shouldShowTarget()) 1f else 0f
                    }.then(
                        if (coverTransition != null) {
                            Modifier.geometryTarget(coverTransition)
                        } else {
                            Modifier
                        },
                    ).clip(Shapes.ExtraSmallCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (artworkPainter != null) {
                Image(
                    painter = artworkPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AudioCover(
                    uri = artworkUri,
                    placeHolder = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = stringResource(R.string.cover_placeholder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            // 歌名（飞行目标）——与播放器页面同一文字样式，飞行为纯位移，无字号突变
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 作者（飞行目标）
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee(),
            )
        }
    }
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

// ============================================
// 飞行浮层内容
// ============================================

/**
 * 飞行中的封面：跟随浮层矩形缩放，与源/目标使用同一图像模型保证视觉连续。
 */
@Composable
private fun FlyingCover(uri: Uri?) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AudioCover(
            progressiveEnabled = false,
            uri = uri,
            placeHolder = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
