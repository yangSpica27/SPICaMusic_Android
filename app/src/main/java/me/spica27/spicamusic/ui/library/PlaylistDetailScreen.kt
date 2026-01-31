package me.spica27.spicamusic.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.MainTopBar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollOutOfBound
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 歌单详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    val playlistId =
        (backStack.lastOrNull() as? Screen.PlaylistDetail)?.playlistId
            ?: 0L

    val viewModel: PlaylistDetailViewModel =
        koinViewModel(
            key = "PlaylistDetailViewModel_$playlistId",
        ) {
            parametersOf(playlistId)
        }

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedSongs by viewModel.selectedSongs.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()

    // 处理返回键
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.toggleMultiSelectMode()
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        popupHost = { MiuixPopupHost() },
        topBar = {
            MainTopBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        playlist?.playlistName ?: "歌单详情",
                        maxLines = 1,
                        style = MiuixTheme.textStyles.title2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                largeTitle = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Spacer(
                            modifier = Modifier.height(44.dp),
                        )
                        // Header - 歌单信息
                        playlist?.let { pl ->
                            PlaylistHeader(
                                playlist = pl,
                                songCount = songs.size,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // 操作按钮栏
                        ActionBar(
                            songCount = songs.size,
                            isMultiSelectMode = isMultiSelectMode,
                            onPlayAll = { viewModel.playAll() },
                            onToggleMultiSelect = { viewModel.toggleMultiSelectMode() },
                            onShowMenu = { viewModel.showRenameDialog() },
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    if (isMultiSelectMode && selectedSongs.isNotEmpty()) {
                        // 多选模式下显示删除按钮
                        IconButton(onClick = { viewModel.removeSelectedSongs() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // 歌曲列表
            if (songs.isEmpty()) {
                EmptySongList(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .overScrollOutOfBound(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(songs, key = { it.songId ?: -1 }) { song ->
                        SongItemCard(
                            song = song,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedSongs.contains(song.songId),
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSongSelection(song.songId)
                                } else {
                                    viewModel.playSong(song)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    viewModel.toggleMultiSelectMode()
                                }
                                viewModel.toggleSongSelection(song.songId)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 歌单 Header
 */
@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    songCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 封面
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (playlist.cover.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f),
                )
            } else {
                AsyncImage(
                    model = playlist.cover,
                    contentDescription = playlist.playlistName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // 歌单信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = playlist.playlistName,
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$songCount 首歌曲",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            if (playlist.playTimes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "播放 ${playlist.playTimes} 次",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/**
 * 操作按钮栏
 */
@Composable
private fun ActionBar(
    songCount: Int,
    isMultiSelectMode: Boolean,
    onPlayAll: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onShowMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 播放全部按钮
        Card(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable {
                        onPlayAll()
                    },
            colors =
                CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.primary,
                    contentColor = MiuixTheme.colorScheme.primary,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "播放全部",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }

        // 多选模式按钮
        Card(
            onClick = onToggleMultiSelect,
            colors =
                CardDefaults.defaultColors(
                    color =
                        if (isMultiSelectMode) {
                            MiuixTheme.colorScheme.tertiaryContainer
                        } else {
                            MiuixTheme.colorScheme.surfaceVariant
                        },
                    contentColor =
                        if (isMultiSelectMode) {
                            MiuixTheme.colorScheme.onTertiaryContainer
                        } else {
                            MiuixTheme.colorScheme.onSurfaceContainerVariant
                        },
                ),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "多选",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // 菜单按钮
        Box {
            Card(
                onClick = { showMenu = true },
                colors =
                    CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceVariant,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "菜单",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // 下拉菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = {
                        showMenu = false
                        onShowMenu()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

/**
 * 歌曲列表项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemCard(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor =
        animateColorAsState(
            targetValue =
                if (isSelected) {
                    MiuixTheme.colorScheme.tertiaryContainer
                } else {
                    MiuixTheme.colorScheme.surface
                },
        ).value

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(16.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        colors =
            CardDefaults.defaultColors(
                color = cardColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 选中状态图标
            AnimatedVisibility(
                visible = isMultiSelectMode,
            ) {
                Icon(
                    imageVector =
                        if (isSelected) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint =
                        if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f)
                        },
                )
            }

            AnimatedVisibility(
                visible = isMultiSelectMode,
            ) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 封面
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(ContinuousRoundedRectangle(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AudioCover(
                    uri = song.getCoverUri(),
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(ContinuousRoundedRectangle(8.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "封面占位符",
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .align(
                                            Alignment.Center,
                                        ),
                            )
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // 歌曲信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = song.displayName,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 空歌曲列表提示
 */
@Composable
private fun EmptySongList(modifier: Modifier = Modifier) {
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
            text = "歌单为空",
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "快去添加歌曲吧",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
        )
    }
}

/**
 * 重命名歌单对话框
 */
@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    show: MutableState<Boolean>,
) {
    var newName by remember { mutableStateOf(currentName) }

    SuperDialog(
        title = "重命名歌单",
        onDismissRequest = onDismiss,
        show = show,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = "请输入歌单名称",
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true,
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
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != currentName) {
                            onRename(newName)
                        }
                    },
                    modifier = Modifier.pressable(interactionSource = null, indication = SinkFeedback()),
                    enabled = newName.isNotBlank() && newName != currentName,
                ) {
                    Text("确定")
                }
            }
        }
    }
}
