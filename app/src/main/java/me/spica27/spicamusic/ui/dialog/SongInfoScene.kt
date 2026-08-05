package me.spica27.spicamusic.ui.dialog

import android.content.ClipData
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.launch
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.player.formatTime
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.CoverFallback
import java.util.Locale

class SongInfoScene(
    val song: Song,
) : DialogScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val density = LocalDensity.current
        val slideOffsetPx = with(density) { 72.dp.toPx() }
        val closeLabel = stringResource(R.string.close)

        BackHandler { path.pop(scene) }

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize(),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = enterProgress.value }
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = closeLabel,
                        ) { path.pop(scene) },
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            val progress = enterProgress.value
                            alpha = progress
                            translationY = (1f - progress) * slideOffsetPx
                        },
            ) {
                DialogContent()
            }
        }
    }

    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val context = LocalContext.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val screenHeight =
            with(density) {
                LocalWindowInfo.current.containerSize.height
                    .toDp()
            }
        val title = stringResource(R.string.song_info_dialog_title)
        val copySuccess = stringResource(R.string.copy_success)
        val copyLabel = stringResource(R.string.copy_field_format)
        val formattedFileSize = Formatter.formatFileSize(context, song.size)
        val onCopy: (String, String) -> Unit = { label, value ->
            scope.launch {
                clipboard.setClipEntry(ClipData.newPlainText(label, value).toClipEntry())
                Toast.makeText(context, copySuccess, Toast.LENGTH_SHORT).show()
            }
        }

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.ExtraLarge),
            ) {
                Spacer(
                    Modifier.statusBarsPadding(),
                )
                SongInfoHeader(
                    song = song,
                    title = title,
                    onClose = { path.pop(scene) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.ExtraLarge),
                ) {
                    InfoSection(title = stringResource(R.string.song_details)) {
                        InfoItem(
                            Icons.Default.MusicNote,
                            stringResource(R.string.song_displayname),
                            song.displayName,
                            copyLabel,
                            onCopy,
                        )
                        InfoItem(
                            Icons.Default.Person,
                            stringResource(R.string.song_artist),
                            song.artist,
                            copyLabel,
                            onCopy,
                        )
                        InfoItem(
                            Icons.Default.Album,
                            stringResource(R.string.song_album),
                            song.album,
                            copyLabel,
                            onCopy,
                        )
                        InfoItem(
                            Icons.Default.Schedule,
                            stringResource(R.string.song_duration),
                            formatTime(song.duration),
                            copyLabel,
                            onCopy,
                        )
                    }
                    InfoSection(title = stringResource(R.string.file_details)) {
                        InfoItem(
                            Icons.Default.Folder,
                            stringResource(R.string.info_file_path),
                            song.path,
                            copyLabel,
                            onCopy,
                            isMultiline = true,
                        )
                        InfoItem(
                            Icons.Default.DataUsage,
                            stringResource(R.string.info_file_size),
                            formattedFileSize,
                            copyLabel,
                            onCopy,
                        )
                        InfoItem(
                            Icons.Default.Info,
                            stringResource(R.string.info_file_format),
                            song.codec.ifBlank { song.mimeType },
                            copyLabel,
                            onCopy,
                        )
                    }
                    if (song.sampleRate > 0 || song.bitRate > 0 || song.channels > 0 || song.digit > 0) {
                        InfoSection(title = stringResource(R.string.audio_info)) {
                            if (song.sampleRate > 0) {
                                InfoItem(
                                    Icons.Default.GraphicEq,
                                    stringResource(R.string.sample_rate_label),
                                    stringResource(
                                        R.string.sample_rate_format,
                                        formatSampleRate(song.sampleRate),
                                    ),
                                    copyLabel,
                                    onCopy,
                                )
                            }
                            if (song.bitRate > 0) {
                                InfoItem(
                                    Icons.Default.GraphicEq,
                                    stringResource(R.string.bitrate_label),
                                    stringResource(R.string.kbps_format, song.bitRate / 1000),
                                    copyLabel,
                                    onCopy,
                                )
                            }
                            if (song.channels > 0) {
                                val channels =
                                    when (song.channels) {
                                        1 -> stringResource(R.string.mono)
                                        2 -> stringResource(R.string.stereo)
                                        else ->
                                            stringResource(
                                                R.string.channels_format,
                                                song.channels,
                                            )
                                    }
                                InfoItem(
                                    Icons.Default.GraphicEq,
                                    stringResource(R.string.channel_count_label),
                                    channels,
                                    copyLabel,
                                    onCopy,
                                )
                            }
                            if (song.digit > 0) {
                                InfoItem(
                                    Icons.Default.GraphicEq,
                                    stringResource(R.string.bit_depth_label),
                                    stringResource(R.string.bit_depth_format, song.digit),
                                    copyLabel,
                                    onCopy,
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { path.pop(scene) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.Large),
                    shape = Shapes.LargeCornerBasedShape,
                ) {
                    Text(stringResource(R.string.close))
                }
                Spacer(
                    Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun SongInfoHeader(
    song: Song,
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = Shapes.ExtraLargeCornerBasedShape,
            tonalElevation = 3.dp,
        ) {
            LandscapistImage(
                imageModel = { song.getCoverUri() },
                modifier = Modifier.fillMaxSize(),
                failure = {
                    CoverFallback(
                        fallbackUri = song.getAlbumCoverUri(),
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            shape = Shapes.MediumCornerBasedShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            text = title,
            modifier =
                Modifier
                    .padding(horizontal = Spacing.ExtraSmall)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    title: String,
    content: String,
    copyLabelFormat: String,
    onCopy: (String, String) -> Unit,
    isMultiline: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = Shapes.MediumCornerBasedShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isMultiline) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                modifier = Modifier.size(48.dp),
                shape = Shapes.MediumCornerBasedShape,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                onClick = { onCopy(title, content) },
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = String.format(copyLabelFormat, title),
                )
            }
        }
    }
}

private fun formatSampleRate(sampleRate: Int): String =
    if (sampleRate % 1000 == 0) {
        (sampleRate / 1000).toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", sampleRate / 1000f)
    }
