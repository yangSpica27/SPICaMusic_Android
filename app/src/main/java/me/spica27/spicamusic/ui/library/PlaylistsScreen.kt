package me.spica27.spicamusic.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollOutOfBound
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 歌单页面
 */
@Composable
fun PlaylistsScreen(modifier: Modifier = Modifier) {
    val viewModel: PlaylistViewModel = koinViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val showCreateDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val backStack = LocalNavBackStack.current

    val scrollerBehavior = MiuixScrollBehavior()

    val hazeState = rememberHazeState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        popupHost = { MiuixPopupHost() },
        topBar = {
            TopAppBar(
                color = Color.Transparent,
                modifier =
                    Modifier.hazeEffect(
                        hazeState,
                        HazeMaterials.ultraThick(
                            MiuixTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ),
                    ) {
                        progressive =
                            HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f,
                            )
                    },
                title = stringResource(R.string.title_playlist),
                actions = {
                    // 新增歌单按钮
                    IconButton(
                        onClick = { viewModel.showCreateDialog() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_playlist),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                scrollBehavior = scrollerBehavior,
            )
        },
    ) { paddingValues ->
        // 歌单列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .hazeSource(hazeState)
                    .fillMaxSize()
                    .nestedScroll(scrollerBehavior.nestedScrollConnection)
                    .overScrollOutOfBound(),
            contentPadding =
                PaddingValues(
                    16.dp,
                    paddingValues.calculateTopPadding() + 12.dp,
                    16.dp,
                    paddingValues.calculateBottomPadding() + 200.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (playlists.isEmpty()) {
                item(
                    span = { GridItemSpan(2) },
                ) {
                    EmptyPlaylistState(
                        onCreatePlaylist = { viewModel.showCreateDialog() },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp),
                    )
                }
            }

            items(playlists, key = { it.playlistId ?: 0 }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    viewModel = viewModel,
                    onClick = {
                        playlist.playlistId?.let { id ->
                            backStack.add(Screen.PlaylistDetail(id))
                        }
                    },
                    onLongClick = {
                        viewModel.showDeleteDialog(playlist)
                    },
                )
            }
            item(
                span = { GridItemSpan(2) },
            ) {
                Spacer(modifier = Modifier.height(150.dp))
            }
        }
    }

    // 创建歌单对话框
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name -> viewModel.createPlaylist(name) },
            show = showCreateDialog,
        )
    }

    // 删除确认对话框
    showDeleteDialog?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.playlistName,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deletePlaylist(playlist) },
            show = true,
        )
    }
}

/**
 * 歌单卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    viewModel: PlaylistViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 获取歌单内的歌曲
    val songs by viewModel
        .getPlaylistSongs(playlist.playlistId ?: 0L)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    ContinuousRoundedRectangle(
                        12.dp,
                    ),
                ).pressable(null)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        // Grid 组合封面
        PlaylistGridCover(
            songs = songs,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(
                        ContinuousRoundedRectangle(
                            8.dp,
                        ),
                    ),
        )

        // 歌单名称
        Text(
            text = playlist.playlistName,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * 歌单 Grid 组合封面
 * 显示歌单内最多4首歌曲的封面，排列为2x2网格
 */
@Composable
private fun PlaylistGridCover(
    songs: List<Song>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (songs.isEmpty()) {
            // 空歌单默认封面
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
            )
        } else {
            // 2x2 网格布局显示前4首歌曲封面
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val displaySongs = songs.take(4)

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // 第一行第一个
                    GridCoverItem(
                        song = displaySongs.getOrNull(0),
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    )
                    // 第一行第二个
                    GridCoverItem(
                        song = displaySongs.getOrNull(1),
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // 第二行第一个
                    GridCoverItem(
                        song = displaySongs.getOrNull(2),
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    )
                    // 第二行第二个
                    GridCoverItem(
                        song = displaySongs.getOrNull(3),
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Grid 封面单个项
 */
@Composable
private fun GridCoverItem(
    song: Song?,
    modifier: Modifier = Modifier,
) {
    AudioCover(
        uri = song?.getCoverUri(),
        modifier = modifier,
        placeHolder = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MiuixTheme.colorScheme.tertiaryContainer,
                                        MiuixTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            ),
                        ),
            ) {
                if (song != null) {
                    Text(
                        text = song.displayName.take(1),
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                        modifier =
                            Modifier.align(
                                Alignment.Center,
                            ),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                    )
                }
            }
        },
    )
}

/**
 * 空状态显示
 */
@Composable
private fun EmptyPlaylistState(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_playlists),
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.create_first_playlist),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreatePlaylist,
            modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.create_playlist))
        }
    }
}

/**
 * 创建歌单对话框
 */
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    show: Boolean,
) {
    var playlistName by remember { mutableStateOf("") }

    val showState = remember { mutableStateOf(true) }

    LaunchedEffect(show) {
        showState.value = show
    }

    SuperDialog(
        title = stringResource(R.string.create_playlist),
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = stringResource(R.string.hint_input_playlist_name),
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreate(playlistName)
                        }
                    },
                    modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                    enabled = playlistName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        }
    }
}

/**
 * 删除歌单确认对话框
 */
@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    show: Boolean,
) {
    val showState = remember { mutableStateOf(true) }

    LaunchedEffect(show) {
        showState.value = show
    }

    SuperDialog(
        title = stringResource(R.string.delete_playlist_title),
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.confirm_delete_playlist, playlistName),
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}
