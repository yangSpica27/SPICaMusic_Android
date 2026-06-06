package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.artistdetail.ArtistDetailScene
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScene
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class SongMenuScene(
    val song: Song,
) : DialogScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val slideOffsetPx = with(density) { 72.dp.toPx() }
        val viewModel: SongMenuViewModel =
            koinViewModel(
                key = "SongMenuViewModel_${song.mediaStoreId}",
            ) { parametersOf(song) }

        val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
        val playlists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
        val album by viewModel.albumDetail.collectAsStateWithLifecycle()
        val artist by viewModel.artistDetail.collectAsStateWithLifecycle()

        var showPlaylistDialog by remember { mutableStateOf(false) }
        var showCreatePlaylistDialog by remember { mutableStateOf(false) }

        fun closeMenu() {
            path.pop(scene)
        }

        fun closeAndNavigate(navigate: () -> Unit) {
            scope.launch {
                path.pop(scene)
                delay(360)
                navigate()
            }
        }

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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { closeMenu() },
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = enterProgress.value
                            translationY = (1f - p) * slideOffsetPx
                            alpha = p
                        },
            ) {
                SongMenuContent(
                    song = song,
                    isLiked = isLiked,
                    onClose = ::closeMenu,
                    onPlayNext = {
                        viewModel.addToNext()
                        closeMenu()
                    },
                    onAddToQueue = {
                        viewModel.addToQueue()
                        closeMenu()
                    },
                    onToggleLike = {
                        viewModel.toggleLike()
                        closeMenu()
                    },
                    onShowPlaylistDialog = {
                        showPlaylistDialog = true
                    },
                    onOpenAlbum = {
                        closeAndNavigate { path.push(AlbumDetailScene(album)) }
                    },
                    onOpenArtist = {
                        closeAndNavigate { path.push(ArtistDetailScene(artist)) }
                    },
                    onOpenSongInfo = {
                        closeAndNavigate { path.push(SongInfoScene(song)) }
                    },
                )
            }
        }

        if (showPlaylistDialog) {
            PlaylistPickerDialog(
                playlists = playlists,
                onDismiss = { showPlaylistDialog = false },
                onCreatePlaylist = {
                    showPlaylistDialog = false
                    showCreatePlaylistDialog = true
                },
                onSelectPlaylist = { playlist ->
                    playlist.playlistId?.let { playlistId ->
                        viewModel.addToPlaylist(playlistId)
                    }
                    showPlaylistDialog = false
                    closeMenu()
                },
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name ->
                    viewModel.createPlaylistAndAdd(name)
                    showCreatePlaylistDialog = false
                    closeMenu()
                },
                onOpenFullCreator = {
                    showCreatePlaylistDialog = false
                    closeAndNavigate { path.push(PlaylistCreatorScene()) }
                },
            )
        }
    }

    @Composable
    override fun DialogContent() = Unit
}

@Composable
private fun SongMenuContent(
    song: Song,
    isLiked: Boolean,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onShowPlaylistDialog: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenSongInfo: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 15.dp,
                            horizontal = 20.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LandscapistImage(
                    imageModel = { song.getCoverUri() },
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small),
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = song.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlButton(
                    title = "下一首播放",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayNext,
                )
                ControlButton(
                    title = "加入队列",
                    icon = Icons.Default.PlaylistPlay,
                    modifier = Modifier.weight(1f),
                    onClick = onAddToQueue,
                )
                ControlButton(
                    title = if (isLiked) "取消收藏" else "收藏",
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleLike,
                    iconTint =
                        if (isLiked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            ControlItem(
                title = "添加到歌单",
                icon = Icons.AutoMirrored.Default.PlaylistAdd,
                onClick = onShowPlaylistDialog,
            )
            ControlItem(
                title = "查看专辑",
                icon = Icons.Default.Album,
                onClick = onOpenAlbum,
            )
            ControlItem(
                title = "查看歌手",
                icon = Icons.Default.SportsMartialArts,
                onClick = onOpenArtist,
            )
            ControlItem(
                title = "查看歌曲信息",
                icon = Icons.Default.Info,
                onClick = onOpenSongInfo,
            )
            Spacer(modifier = Modifier.height(55.dp))
        }
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = iconTint,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ControlItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<me.spica27.spicamusic.common.entity.Playlist>,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onSelectPlaylist: (me.spica27.spicamusic.common.entity.Playlist) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加到歌单") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (playlists.isEmpty()) {
                    Text(
                        text = "当前没有可添加的歌单，可以先新建一个。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    playlists.forEach { playlist ->
                        Text(
                            text = playlist.playlistName,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onSelectPlaylist(playlist) }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreatePlaylist) {
                Text("新建歌单")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenFullCreator: () -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建歌单并添加歌曲") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("歌单名称") },
                    placeholder = { Text("我的歌单") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            if (playlistName.isNotBlank()) {
                                onConfirm(playlistName)
                            }
                        }),
                )
                TextButton(onClick = onOpenFullCreator) {
                    Text("仅创建空歌单")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(playlistName) },
                enabled = playlistName.isNotBlank(),
            ) {
                Text("创建并添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
