package me.spica27.spicamusic.ui.player.scene

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.LyricsPanel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.FluidMusicBackground
import me.spica27.spicamusic.utils.rememberDominantColorFromUri

@Composable
fun LyricScreen(heroArtworkUri: Uri? = null) {
    val backStack = LocalBackStack.current

    BackHandler(true) {
        backStack.removeLastOrNull()
    }

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
    val coverColor =
        rememberDominantColorFromUri(
            uri = artworkUri,
            fallbackColor = MaterialTheme.colorScheme.primary,
        )

    Box(modifier = Modifier.fillMaxSize()) {
        FluidMusicBackground(
            modifier = Modifier.fillMaxSize(),
            coverColor = coverColor,
            isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
            coverUri = { artworkUri },
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LyricsHeader(
                    title = title,
                    artist = artist,
                    artworkUri = artworkUri,
                    onBack = { backStack.removeLastOrNull() },
                )
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                LyricsPanel(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    title: String,
    artist: String,
    artworkUri: Uri?,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        IconButton(
            onClick = onBack,
            colors =
                IconButtonDefaults.iconButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(Shapes.MediumCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AudioCover(
                uri = artworkUri,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
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
