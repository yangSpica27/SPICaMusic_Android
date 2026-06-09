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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import kotlin.time.Duration.Companion.milliseconds

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
                delay(360.milliseconds)
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
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
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
                    .clip(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        .align(Alignment.CenterHorizontally),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 18.dp,
                            bottom = 14.dp,
                            start = 20.dp,
                            end = 12.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier =
                        Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 3.dp,
                ) {
                    LandscapistImage(
                        imageModel = { song.getCoverUri() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = song.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlButton(
                    title = "下一首播放",
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayNext,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                )
                ControlButton(
                    title = "加入队列",
                    icon = Icons.Default.PlaylistPlay,
                    modifier = Modifier.weight(1f),
                    onClick = onAddToQueue,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                )
                ControlButton(
                    title = if (isLiked) "取消收藏" else "收藏",
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleLike,
                    containerColor =
                        if (isLiked) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    iconTint =
                        if (isLiked) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            ControlItem(
                title = "添加到歌单",
                subtitle = "保存到已有歌单或新建歌单",
                icon = Icons.AutoMirrored.Default.PlaylistAdd,
                onClick = onShowPlaylistDialog,
            )
            ControlItem(
                title = "查看专辑",
                subtitle = song.album,
                icon = Icons.Default.Album,
                onClick = onOpenAlbum,
            )
            ControlItem(
                title = "查看歌手",
                subtitle = song.artist,
                icon = Icons.Default.SportsMartialArts,
                onClick = onOpenArtist,
            )
            ControlItem(
                title = "查看歌曲信息",
                subtitle = "文件、专辑与音频信息",
                icon = Icons.Default.Info,
                onClick = onOpenSongInfo,
            )
        }
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor.copy(alpha = 0.72f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ControlItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            DialogTitle(
                title = "添加到歌单",
                subtitle = "选择一个歌单保存这首歌曲",
                icon = Icons.AutoMirrored.Default.PlaylistAdd,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (playlists.isEmpty()) {
                    EmptyDialogCard(
                        icon = Icons.Default.LibraryMusic,
                        title = "还没有歌单",
                        subtitle = "可以先新建一个歌单，再把当前歌曲添加进去。",
                    )
                } else {
                    playlists.forEach { playlist ->
                        PlaylistDialogRow(
                            title = playlist.playlistName,
                            subtitle = "点击添加到此歌单",
                            onClick = { onSelectPlaylist(playlist) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreatePlaylist,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
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
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            DialogTitle(
                title = "新建歌单",
                subtitle = "创建后会自动添加当前歌曲",
                icon = Icons.Default.Add,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("歌单名称") },
                    placeholder = { Text("我的歌单") },
                    supportingText = { Text("输入一个便于识别的名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
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
            Button(
                onClick = { onConfirm(playlistName) },
                enabled = playlistName.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
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

@Composable
private fun DialogTitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistDialogRow(
    title: String,
    subtitle: String,
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
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyDialogCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
