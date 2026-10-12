package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.MusicArtwork
import me.spica27.spicamusic.common.entity.BottomBarLayout
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.widget.AudioCover

@Composable
internal fun MiniPlayerBar(
    title: String,
    artist: String,
    artwork: MusicArtwork?,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    progress: () -> Float = { 0f },
    hazeState: HazeState? = null,
    glassEnabled: Boolean = LocalLiquidGlassConfig.current.enabled,
    layout: BottomBarLayout = BottomBarLayout.Traditional,
) {
    val colors = MaterialTheme.colorScheme
    val appearance = BottomBarAppearance.forLayout(layout)
    val container = appearance.miniPlayerContainerColor(colors)
    val foreground = appearance.miniPlayerContentColor(colors)
    Column(
        modifier
            .fillMaxWidth()
            .playerBarSurface(hazeState, container, glassEnabled, appearance.miniPlayerShape, appearance.isFloating)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_player), onClick = onExpand),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (appearance.isFloating) {
                        Modifier
                    } else {
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    },
                ).heightIn(min = appearance.miniPlayerMinHeight)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioCover(artwork = artwork, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = foreground, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    artist,
                    color = foreground.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenQueue, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.queue), tint = foreground)
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.primary),
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = colors.onPrimary,
                    )
                }
            }
        }
        Spacer(
            Modifier.fillMaxWidth().height(2.dp).drawBehind {
                drawRect(foreground.copy(alpha = 0.12f))
                val width = size.width * progress().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
                val left = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width
                drawRect(
                    foreground.copy(alpha = 0.7f),
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(left, 0f),
                    size = Size(width, size.height),
                )
            },
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun MiniPlayerBarPreview() {
    MaterialTheme {
        MiniPlayerBar("Unsayable", "Brambles", null, true, {}, {}, {}, progress = { 0.35f })
    }
}
