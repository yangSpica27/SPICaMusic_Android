package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.artwork
import me.spica27.spicamusic.common.entity.ArtistCredit
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.ui.component.DialogContainer
import me.spica27.spicamusic.ui.navigation.ArtistDetailRoute
import me.spica27.spicamusic.ui.navigation.ArtistPickerRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.Route
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.displayName

/** 只有一位艺人时直接进入详情页，多位署名时先选择；没有署名返回 null */
fun artistRouteFor(credits: List<ArtistCredit>): Route? =
    when (credits.distinctBy { it.artist.artistId }.size) {
        0 -> null
        1 -> ArtistDetailRoute(credits.first().artist)
        else -> ArtistPickerRoute(credits)
    }

@Composable
fun ArtistPickerDialogContent(credits: List<ArtistCredit>) {
    val backStack = LocalBackStack.current
    DialogContainer {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogTitle(
                title = stringResource(R.string.choose_artist),
                subtitle = stringResource(R.string.choose_artist_subtitle),
                icon = Icons.Default.SportsMartialArts,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                credits.forEach { credit ->
                    ArtistCreditRow(
                        credit = credit,
                        onClick = {
                            backStack.removeLastOrNull()
                            backStack.add(ArtistDetailRoute(credit.artist))
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { backStack.removeLastOrNull() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ArtistCreditRow(
    credit: ArtistCredit,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioCover(
                artwork = credit.artist.artwork(),
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    credit.artist.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        when (credit.role) {
                            ArtistRole.ARTIST -> R.string.artist_role_artist
                            ArtistRole.COMPOSER -> R.string.artist_role_composer
                            ArtistRole.LYRICIST -> R.string.artist_role_lyricist
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
