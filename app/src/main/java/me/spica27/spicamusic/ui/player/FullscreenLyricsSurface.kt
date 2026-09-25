package me.spica27.spicamusic.ui.player

import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover

private val LyricsTopChromeHeight = 128.dp
private val LyricsBottomChromeHeight = 168.dp
private val LyricsToolbarBottomInset = 112.dp

/**
 * 展开播放器内部的全屏歌词前景。
 *
 * 该组件只负责展示与交互，不拥有导航状态；关闭歌词由播放器宿主处理。
 */
@Composable
internal fun FullscreenLyricsSurface(
    title: String,
    artist: String,
    artworkUri: Uri?,
    hazeState: HazeState,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxSize()) {
        LyricsPanel(
            modifier = Modifier.fillMaxSize(),
            toolbarBottomInset = LyricsToolbarBottomInset,
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(LyricsTopChromeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    surfaceColor.copy(alpha = 0.56f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(LyricsBottomChromeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = 0.64f),
                                ),
                        ),
                    ),
        )

        FullscreenLyricsHeader(
            title = title,
            artist = artist,
            artworkUri = artworkUri,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        TransportControls(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Medium),
            hazeState = hazeState,
            isPlaying = isPlaying,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
        )
    }
}

@Composable
private fun FullscreenLyricsHeader(
    title: String,
    artist: String,
    artworkUri: Uri?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        IconButton(
            onClick = onBack,
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.back_to_player),
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(Shapes.MediumCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AudioCover(
                uri = artworkUri,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }
    }
}
