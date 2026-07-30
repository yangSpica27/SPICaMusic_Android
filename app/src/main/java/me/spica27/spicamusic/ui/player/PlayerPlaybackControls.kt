@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.ProgressBarStyle
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.rememberThemeRevealOriginState
import me.spica27.spicamusic.ui.theme.themeRevealOrigin
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioDynamicWaveSlider
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.ui.widget.audio_seekbar.ExpressiveWavySlider

/** 播放器与歌词页共用的三样式进度区。 */
@Composable
fun PlayerProgressSection(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isSeeking: Boolean,
    progressBarStyle: ProgressBarStyle,
    fftDrawData: StateFlow<FloatArray>? = null,
    amplitudes: List<Int> = emptyList(),
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when (progressBarStyle) {
            ProgressBarStyle.ExpressiveWavy ->
                ExpressiveWavySlider(
                    progress = progress.coerceIn(0f, 1f),
                    onProgressChange = onProgressChange,
                    onProgressChangeFinished = onProgressChangeFinished,
                    isPlaying = isPlaying,
                    activeColor = MaterialTheme.colorScheme.onSurface,
                    inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                )

            ProgressBarStyle.DynamicWaveform -> {
                if (fftDrawData != null) {
                    val drawData by fftDrawData.collectAsStateWithLifecycle()
                    AudioDynamicWaveSlider(
                        progress = progress.coerceIn(0f, 1f),
                        fftAmplitudes = drawData,
                        onProgressChange = onProgressChange,
                        onProgressChangeFinished = onProgressChangeFinished,
                        waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                        progressBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                    )
                }
            }

            ProgressBarStyle.TimeDomainWaveform ->
                AudioWaveSlider(
                    progress = progress.coerceIn(0f, 1f),
                    amplitudes = amplitudes,
                    onProgressChange = onProgressChange,
                    onProgressChangeFinished = onProgressChangeFinished,
                    waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                    progressBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                )
        }
        Spacer(modifier = Modifier.height(Spacing.Small))
        val timeStyle =
            MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
            )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = timeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                if (isSeeking) {
                    Text(
                        text = formatTime((progress * duration).toLong()),
                        style = timeStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.inversePrimary,
                                    shape = Shapes.SmallCornerBasedShape,
                                ).padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                }
            }
            Text(
                text = formatTime(duration),
                style = timeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 4.dp),
            )
        }
    }
}

/** 兼容仅需要流动波浪的调用点。 */
@Composable
fun PlayerWavyProgressSection(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isSeeking: Boolean,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerProgressSection(
        progress = progress,
        currentPosition = currentPosition,
        duration = duration,
        isPlaying = isPlaying,
        isSeeking = isSeeking,
        progressBarStyle = ProgressBarStyle.ExpressiveWavy,
        onProgressChange = onProgressChange,
        onProgressChangeFinished = onProgressChangeFinished,
        modifier = modifier,
    )
}

/** 播放器封面页与歌词页共用的完整底部播放模块。 */
@Composable
fun PlayerPlaybackBottomSection(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isSeeking: Boolean,
    playMode: PlayMode,
    isLike: Boolean,
    progressBarStyle: ProgressBarStyle = ProgressBarStyle.ExpressiveWavy,
    fftDrawData: StateFlow<FloatArray>? = null,
    amplitudes: List<Int> = emptyList(),
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
    onSleepTimerSet: (Int) -> Unit = {},
    onSleepTimerCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
    progressModifier: Modifier = Modifier,
    controlsModifier: Modifier = Modifier,
    showControls: Boolean = true,
) {
    Column(modifier = modifier) {
        PlayerProgressSection(
            progress = progress,
            currentPosition = currentPosition,
            duration = duration,
            isPlaying = isPlaying,
            isSeeking = isSeeking,
            progressBarStyle = progressBarStyle,
            fftDrawData = fftDrawData,
            amplitudes = amplitudes,
            onProgressChange = onProgressChange,
            onProgressChangeFinished = onProgressChangeFinished,
            modifier = progressModifier,
        )
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)),
            exit =
                fadeOut(tween(180)) +
                    slideOutVertically(
                        animationSpec = tween(260),
                        targetOffsetY = { it },
                    ),
        ) {
            Column {
                Spacer(modifier = Modifier.height(Spacing.Medium))
                PlayerTransportControls(
                    modifier = controlsModifier.fillMaxWidth(),
                    isPlaying = isPlaying,
                    playMode = playMode,
                    isLike = isLike,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onPlayModeClick = onPlayModeClick,
                    onFavoriteClick = onFavoriteClick,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    onSleepTimerSet = onSleepTimerSet,
                    onSleepTimerCancel = onSleepTimerCancel,
                )
            }
        }
    }
}

private fun playbackControlIconTransform() =
    (
        fadeIn(tween(160)) +
            scaleIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                initialScale = 0.7f,
            )
    ).togetherWith(fadeOut(tween(120)))

/** 播放器与歌词页共用的五按钮控制区。 */
@Composable
private fun LegacyPlayerTransportControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    playMode: PlayMode,
    isLike: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    val previousRevealOrigin = rememberThemeRevealOriginState()
    val nextRevealOrigin = rememberThemeRevealOriginState()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPlayModeClick) {
            AnimatedContent(
                targetState = playMode,
                transitionSpec = { playbackControlIconTransform() },
                label = "playModeIcon",
            ) { mode ->
                Icon(
                    imageVector =
                        when (mode) {
                            PlayMode.LOOP -> Icons.Rounded.Repeat
                            PlayMode.LIST -> Icons.Rounded.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                    contentDescription = stringResource(R.string.play_mode),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            IconButton(
                onClick = {
                    previousRevealOrigin.armFromCenter()
                    onPreviousClick()
                },
                modifier =
                    Modifier
                        .size(56.dp)
                        .themeRevealOrigin(previousRevealOrigin),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.previous_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
            }
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.92f else 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 1100f,
                    ),
                label = "playPressScale",
            )
            IconButton(
                onClick = onPlayPauseClick,
                interactionSource = playInteraction,
                modifier =
                    Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { playbackControlIconTransform() },
                    label = "playPauseIcon",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription =
                            if (playing) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            IconButton(
                onClick = {
                    nextRevealOrigin.armFromCenter()
                    onNextClick()
                },
                modifier =
                    Modifier
                        .size(56.dp)
                        .themeRevealOrigin(nextRevealOrigin),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.next_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(onClick = onFavoriteClick) {
            AnimatedContent(
                targetState = isLike,
                transitionSpec = { playbackControlIconTransform() },
                label = "favoriteIcon",
            ) { liked ->
                Icon(
                    imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint =
                        if (liked) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

private enum class TransportButtonType {
    Previous,
    PlayPause,
    Next,
}

/**
 * Expressive transport controls adapted to the current player's five actions.
 *
 * The main transport buttons expand and contract as a group when pressed. The
 * play-mode and favorite actions stay in a separate tonal capsule so the
 * progress component above remains independent from this layout.
 */
@Composable
fun PlayerTransportControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    playMode: PlayMode,
    isLike: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
    onSleepTimerSet: (Int) -> Unit = {},
    onSleepTimerCancel: () -> Unit = {},
) {
    var activeButton by remember { mutableStateOf<TransportButtonType?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val previousRevealOrigin = rememberThemeRevealOriginState()
    val nextRevealOrigin = rememberThemeRevealOriginState()
    val sleepTimerMinutes =
        sleepTimerRemainingMs?.let { remaining ->
            ((remaining + 59_999L) / 60_000L).coerceAtLeast(1L)
        }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = {
                Text(text = stringResource(R.string.sleep_timer))
            },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(
                            onClick = {
                                onSleepTimerSet(minutes)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.sleep_timer_minutes, minutes))
                        }
                    }
                    if (sleepTimerRemainingMs != null) {
                        TextButton(
                            onClick = {
                                onSleepTimerCancel()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.sleep_timer_cancel))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text(text = stringResource(R.string.sleep_timer_close))
                }
            },
        )
    }

    LaunchedEffect(activeButton) {
        if (activeButton != null) {
            delay(220)
            activeButton = null
        }
    }

    fun weightFor(button: TransportButtonType): Float =
        when (activeButton) {
            null -> 1f
            button -> 1.1f
            else -> 0.65f
        }

    val previousWeight by animateFloatAsState(
        targetValue = weightFor(TransportButtonType.Previous),
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "previousButtonWeight",
    )
    val playPauseWeight by animateFloatAsState(
        targetValue = weightFor(TransportButtonType.PlayPause),
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "playPauseButtonWeight",
    )
    val nextWeight by animateFloatAsState(
        targetValue = weightFor(TransportButtonType.Next),
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "nextButtonWeight",
    )
    val playPauseCorner by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 40.dp,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "playPauseCorner",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(previousWeight)
                        .fillMaxSize()
                        .themeRevealOrigin(previousRevealOrigin)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(role = Role.Button) {
                            activeButton = TransportButtonType.Previous
                            previousRevealOrigin.armFromCenter()
                            onPreviousClick()
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.previous_track),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }

            Box(
                modifier =
                    Modifier
                        .weight(playPauseWeight)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(playPauseCorner))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(role = Role.Button) {
                            activeButton = TransportButtonType.PlayPause
                            onPlayPauseClick()
                        },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { playbackControlIconTransform() },
                    label = "playPauseIcon",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription =
                            if (playing) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(nextWeight)
                        .fillMaxSize()
                        .themeRevealOrigin(nextRevealOrigin)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(role = Role.Button) {
                            activeButton = TransportButtonType.Next
                            nextRevealOrigin.armFromCenter()
                            onNextClick()
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.next_track),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f))
                    .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (playMode == PlayMode.SHUFFLE) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            },
                        ).clickable(role = Role.Button, onClick = onPlayModeClick),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = playMode,
                    transitionSpec = { playbackControlIconTransform() },
                    label = "playModeIcon",
                ) { mode ->
                    Icon(
                        imageVector =
                            when (mode) {
                                PlayMode.LOOP -> Icons.Rounded.Repeat
                                PlayMode.LIST -> Icons.Rounded.RepeatOne
                                PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                            },
                        contentDescription = stringResource(R.string.play_mode),
                        tint =
                            if (mode == PlayMode.SHUFFLE) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isLike) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            },
                        ).clickable(role = Role.Button, onClick = onFavoriteClick),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isLike,
                    transitionSpec = { playbackControlIconTransform() },
                    label = "favoriteIcon",
                ) { liked ->
                    Icon(
                        imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint =
                            if (liked) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (sleepTimerRemainingMs != null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            },
                        ).clickable(role = Role.Button) {
                            showSleepTimerDialog = true
                        },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = sleepTimerMinutes,
                    transitionSpec = { playbackControlIconTransform() },
                    label = "sleepTimerIcon",
                ) { minutes ->
                    if (minutes == null) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = stringResource(R.string.sleep_timer),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    } else {
                        Text(
                            text = minutes.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
