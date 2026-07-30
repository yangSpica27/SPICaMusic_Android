package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.theme.rememberThemeRevealOriginState
import me.spica27.spicamusic.ui.theme.themeRevealOrigin
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.MusicCoverPlaceholder

/**
 * 底部迷你播放条
 * 显示当前播放歌曲信息和基本控制按钮
 */
@Composable
fun LargeBottomPlayerBar(
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    artistModifier: Modifier = Modifier,
    infoModifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    nextButtonModifier: Modifier = Modifier,
    coverShape: Shape,
    coverPainter: Painter? = null,
    onCoverPainterReady: (Painter) -> Unit = {},
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
    onExpand: () -> Unit,
    onNext: () -> Unit = viewModel::skipToNext,
) {
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val metadata = currentMediaItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata?.artist?.toString() ?: stringResource(R.string.unknown_artist)
    val artworkUri = metadata?.artworkUri
    val nextRevealOrigin = rememberThemeRevealOriginState()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // 点击态的 indication 默认按整个矩形布局绘制；先裁成胶囊，
                // 避免按下时出现没有圆角的长方形灰影。
                .clip(CircleShape)
                .clickable { onExpand() },
    ) {
        Column {
            // 进度条
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 封面

                val resolvedCoverModifier =
                    coverModifier
                        .graphicsLayer(
                            compositingStrategy = CompositingStrategy.Offscreen,
                        ).size(48.dp)
                        .clip(coverShape)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                        )
                if (coverPainter != null) {
                    Image(
                        painter = coverPainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = resolvedCoverModifier,
                    )
                } else {
                    AudioCover(
                        uri = artworkUri,
                        modifier = resolvedCoverModifier,
                        onPainterReady = onCoverPainterReady,
                        placeHolder = {
                            MusicCoverPlaceholder(
                                modifier = Modifier.fillMaxSize(),
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                contentDescription = stringResource(R.string.cover_placeholder),
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 歌曲信息
                Column(
                    modifier =
                        infoModifier
                            .weight(1f)
                            .clipToBounds(),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = titleModifier,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = artistModifier,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 播放/暂停按钮
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = playButtonModifier.size(40.dp),
                ) {
                    MorphingPlayPauseIcon(
                        isPlaying = isPlaying,
                        playContentDescription = stringResource(R.string.play),
                        pauseContentDescription = stringResource(R.string.pause),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // 下一首
                IconButton(
                    onClick = {
                        nextRevealOrigin.armFromCenter()
                        onNext()
                    },
                    modifier =
                        nextButtonModifier
                            .size(40.dp)
                            .themeRevealOrigin(nextRevealOrigin),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun MorphingPlayPauseIcon(
    isPlaying: Boolean,
    playContentDescription: String,
    pauseContentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (
                fadeIn(tween(durationMillis = 150)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 220),
                        initialScale = 0.35f,
                    )
            ).togetherWith(
                fadeOut(tween(durationMillis = 110)) +
                    scaleOut(
                        animationSpec = tween(durationMillis = 180),
                        targetScale = 1.35f,
                    ),
            )
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "playPauseMorph",
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playing) pauseContentDescription else playContentDescription,
            tint = tint,
        )
    }
}
