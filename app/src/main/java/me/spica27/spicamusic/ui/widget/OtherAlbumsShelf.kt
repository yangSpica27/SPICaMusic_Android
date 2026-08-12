package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing

private val CARD_WIDTH = 132.dp

/**
 * "来自某歌手的其他内容"横向专辑列表。
 */
@Composable
fun OtherAlbumsShelf(
    artistName: String,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.more_from_artist, artistName),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Large)
                    .padding(bottom = Spacing.Small),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            items(
                items = albums,
                key = { it.id },
            ) { album ->
                OtherAlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) },
                )
            }
        }
    }
}

@Composable
private fun OtherAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverUri = remember(album.id) { album.getCoverUri() }
    Column(
        modifier =
            modifier
                .width(CARD_WIDTH)
                .clip(Shapes.LargeCornerBasedShape)
                .clickHighlight(onClick = onClick)
                .padding(Spacing.ExtraSmall),
    ) {
        AudioCover(
            uri = coverUri,
            placeHolder = { OtherAlbumCoverPlaceholder() },
            modifier =
                Modifier
                    .size(CARD_WIDTH - Spacing.ExtraSmall * 2)
                    .clip(Shapes.LargeCornerBasedShape),
        )
        Text(
            text = album.title,
            modifier = Modifier.padding(top = Spacing.Small),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.songs_count_format, album.numberOfSongs),
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OtherAlbumCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}
