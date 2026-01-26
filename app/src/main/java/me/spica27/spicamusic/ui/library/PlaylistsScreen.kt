package me.spica27.spicamusic.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.player.LocalBottomPaddingState
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
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
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 歌单页面
 */
@Composable
fun PlaylistsScreen(modifier: Modifier = Modifier) {
    val bottomPaddingState = LocalBottomPaddingState.current
    val viewModel: PlaylistViewModel = koinViewModel()
    val playlists by viewModel.playlists.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val backStack = LocalNavBackStack.current

    LaunchedEffect(Unit) {
        bottomPaddingState.floatValue = -300f
    }

    val scrollerBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        popupHost = { MiuixPopupHost() },
        topBar = {
            TopAppBar(
                title = "歌单",
                actions = {
                    // 新增歌单按钮
                    IconButton(
                        onClick = { viewModel.showCreateDialog() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增歌单",
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
                    .fillMaxSize()
                    .nestedScroll(scrollerBehavior.nestedScrollConnection)
                    .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 封面
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (playlist.cover.isNullOrBlank()) {
                    // 默认封面
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                    )
                } else {
                    AsyncImage(
                        model = playlist.cover,
                        contentDescription = playlist.playlistName,
                        modifier =
                            Modifier
                                .fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

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
            text = "暂无歌单",
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "创建您的第一个歌单",
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
            Text("创建歌单")
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
        title = "创建歌单",
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = "请输入歌单名称",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "取消",
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
                    Text("创建")
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
        title = "删除歌单",
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "确定要删除歌单 \"$playlistName\" 吗？",
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "取消",
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
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("删除")
                }
            }
        }
    }
}
