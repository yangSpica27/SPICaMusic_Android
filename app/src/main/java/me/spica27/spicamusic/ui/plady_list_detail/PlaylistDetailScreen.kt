package me.spica27.spicamusic.ui.plady_list_detail

import android.content.ClipData
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.TimeUtils
import me.spica27.spicamusic.utils.ToastUtils
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import me.spica27.spicamusic.utils.clickableWithVibration
import me.spica27.spicamusic.utils.getStatusBarHeight
import me.spica27.spicamusic.utils.overScrollVertical
import me.spica27.spicamusic.utils.pressable
import me.spica27.spicamusic.utils.scrollEndHaptic
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.InputTextDialog
import me.spica27.spicamusic.widget.LocalMenuState
import me.spica27.spicamusic.widget.PlaylistCover
import me.spica27.spicamusic.widget.SongItemMenu
import me.spica27.spicamusic.widget.SongItemWithCover
import me.spica27.spicamusic.widget.blur.progressiveBlur
import me.spica27.spicamusic.widget.rememberSongItemMenuDialogState
import me.spica27.spicamusic.wrapper.activityViewModel
import org.koin.compose.koinInject
import java.util.*

// / 歌单详情页面
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel = activityViewModel(),
    navigator: NavController? = null,
    playlistId: Long,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
) {
    val songs = playlistViewModel.songsFlow(playlistId).collectAsStateWithLifecycle(emptyList()).value

    val playlist = playlistViewModel.playlistFlow(playlistId).collectAsStateWithLifecycle(null).value

    val songItemMenuDialogState = rememberSongItemMenuDialogState()

    val listState = rememberLazyListState()

    SongItemMenu(
        songItemMenuDialogState,
        playBackViewModel,
    )

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (songs.isEmpty() || playlist == null) {
            EmptyPage(
                navigator = navigator,
                playlist = playlist,
            )
        } else {
            PlayListView(
                songs,
                listState = listState,
                playlist = playlist,
                navigator = navigator,
                playlistViewModel = playlistViewModel,
            )
        }
        TopBar(
            title = playlist?.playlistName.orEmpty(),
            lazyListState = listState,
            navigator = navigator,
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    lazyListState: LazyListState,
    navigator: NavController? = null,
) {
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    val barHeight =
        64.dp + with(LocalDensity.current) { LocalContext.current.getStatusBarHeight().toDp() }

    val offsetY =
        animateDpAsState(
            if (!transparentAppBar) {
                0.dp
            } else {
                -barHeight
            },
        )

    val alpha =
        animateFloatAsState(
            if (transparentAppBar) {
                0f
            } else {
                1f
            },
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = offsetY.value.toPx()
                }.alpha(alpha.value)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .height(barHeight)
                .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = {
                navigator?.popBackStack()
            },
        ) {
            Icon(
                Icons.AutoMirrored.Default.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            title,
            maxLines = 2,
            style = MaterialTheme.typography.titleLarge.copy(MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun EmptyPage(
    navigator: NavController?,
    playlist: Playlist?,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
        )
        AsyncImage(
            modifier = Modifier.height(130.dp),
            model = R.drawable.load_error,
            contentDescription = null,
        )
        Spacer(
            modifier = Modifier.height(10.dp),
        )
        ElevatedButton(
            onClick = {
                navigator?.navigate(
                    Routes.AddSong(
                        playlist?.playlistId ?: -1,
                    ),
                )
            },
            colors =
                ButtonDefaults.elevatedButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            shape = MaterialTheme.shapes.small,
        ) {
            Text("前往新增歌曲")
        }
    }
}

@Composable
private fun PlayListView(
    songs: List<Song>,
    listState: LazyListState = rememberLazyListState(),
    playlistViewModel: PlaylistViewModel,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    playlistId: Long = -1L,
    playlist: Playlist,
    navigator: NavController? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    val menuState = LocalMenuState.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .scrollEndHaptic()
                .overScrollVertical(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
    ) {
        item {
            Header(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.38f),
                playlist = playlist,
                playlistViewModel = playlistViewModel,
                navigator = navigator,
                songs = songs,
            )
        }

        itemsIndexed(songs, key = { _, song ->
            song.songId ?: -1
        }) { index, song ->

            val isVisible =
                remember {
                    derivedStateOf {
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        visibleItems.any { it.index == index }
                    }
                }
            val scale = remember { Animatable(.8f) }

            LaunchedEffect(isVisible.value) {
                if (isVisible.value && scale.value != 1f) {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                    )
                }
            }

            SongItemWithCover(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .background(MaterialTheme.colorScheme.background),
                song = song,
                onClick = {
                    coroutineScope.launch {
                        playlistViewModel.addPlayCount(playlistId)
                        playBackViewModel.play(song, songs)
                    }
                },
                coverSize = 66.dp,
                showMenu = true,
                onMenuClick = {
                    menuState.show {
                        SongItemMenu(
                            song = song,
                            playlist = playlist,
                            onDismissRequest = { menuState.dismiss() },
                            playlistViewModel = playlistViewModel,
                            songs = songs,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SongItemMenu(
    song: Song,
    playlist: Playlist,
    onDismissRequest: () -> Unit,
    playlistViewModel: PlaylistViewModel,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    songs: List<Song>,
) {
    val songDao = koinInject<SongDao>()

    val isLike = songDao.getSongIsLikeFlowWithId(song.songId ?: -1).collectAsStateWithLifecycle(false)

    val coroutineScope = rememberCoroutineScope()

    val clipboardManager = LocalClipboard.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CoverWidget(
                        song = song,
                        modifier =
                            Modifier
                                .size(66.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.medium,
                                ).clip(MaterialTheme.shapes.medium),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            song.displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        )
                        Text(
                            song.artist,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = .6f,
                                        ),
                                ),
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipData = ClipData.newPlainText("songName", song.displayName)
                            val clipEntry = ClipEntry(clipData)
                            coroutineScope.launch {
                                clipboardManager.setClipEntry(clipEntry)
                            }
                            ToastUtils.showToast(App.getInstance().getString(R.string.copy_success))
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.quick_action),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }

        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .border(
                            width = 1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                            MaterialTheme.shapes.small,
                        ).clip(MaterialTheme.shapes.small),
            ) {
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            coroutineScope.launch(Dispatchers.IO) {
                                songDao.toggleLike(song.songId ?: -1)
                            }
                        },
                    headlineContent = {
                        Text(
                            if (isLike.value == 1) {
                                "取消收藏"
                            } else {
                                "收藏"
                            },
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector =
                                if (isLike.value == 1) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                            contentDescription = null,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (isLike.value == 1) {
                                "已经收藏当前歌曲"
                            } else {
                                "未收藏当前歌曲"
                            },
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onDismissRequest.invoke()
                            playlistViewModel.deletePlaylistItem(
                                playlistId = playlist.playlistId ?: -1,
                                songId = song.songId ?: -1,
                            )
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.remove),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remove),
                        )
                    },
                    supportingContent = {
                        Text(
                            "从此歌单中移除当前歌曲",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            ToastUtils.showToast("敬请期待！")
                            onDismissRequest.invoke()
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.info),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.info),
                        )
                    },
                    supportingContent = {
                        Text(
                            "查看歌曲的文件信息",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
            }
        }

        item {
            Text(
                stringResource(R.string.play_action),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }

        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .border(
                            width = 1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                            MaterialTheme.shapes.small,
                        ).clip(MaterialTheme.shapes.small),
            ) {
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onDismissRequest.invoke()
                            playBackViewModel.play(song)
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.add_to_now_playing_list),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.add_to_now_playing_list),
                        )
                    },
                    supportingContent = {
                        Text(
                            "添加到播放列表，并且立刻播放",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onDismissRequest.invoke()
                            playBackViewModel.play(song, songs)
                        },
                    headlineContent = {
                        Text(
                            "播放全部歌曲",
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    supportingContent = {
                        Text(
                            "用当前歌单替换当前播放列表，并且立刻播放此曲目",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    playlistViewModel: PlaylistViewModel,
    navigator: NavController? = null,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    songs: List<Song>,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        InputTextDialog(
            onDismissRequest = {
                showRenameDialog = false
            },
            title = "重命名歌单",
            onConfirm = {
                playlistViewModel.renamePlaylist(playlist.playlistId, it)
                showRenameDialog = false
            },
            defaultText = playlist.playlistName.orEmpty(),
            placeholder = "请输入歌单名称",
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteSureDialog(
            onDismissRequest = { showDeleteDialog = false },
            playlistId = playlist.playlistId ?: -1,
            playlistViewModel = playlistViewModel,
        )
    }

    val playlistName =
        remember(playlist) {
            derivedStateOf {
                playlist.playlistName
            }
        }.value

    val createTimeTxt =
        remember(playlist) {
            derivedStateOf {
                return@derivedStateOf TimeUtils.prettyTime.format(Date(playlist.createTimestamp))
            }
        }.value

    val menuState = LocalMenuState.current

    Box(
        modifier = modifier,
    ) {
        PlaylistCover(
            playlist = playlist,
            modifier =
                Modifier
                    .fillMaxSize()
                    .progressiveBlur(),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.5f,
                                        ),
                                        MaterialTheme.colorScheme.primaryContainer,
                                    ),
                            ),
                    ),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
            ) {
                Text(
                    text = playlistName,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.W700,
                        ),
                    modifier = Modifier,
                )
                Spacer(
                    modifier = Modifier.width(8.dp),
                )
                Text(
                    text = stringResource(R.string.create_in, createTimeTxt),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal,
                        ),
                    modifier = Modifier,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                        ).clickableNoRippleWithVibration {
                            navigator?.navigate(
                                Routes.AddSong(
                                    playlist.playlistId ?: -1,
                                ),
                            )
                        }.pressable(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.small,
                        ).clickableNoRippleWithVibration {
                            menuState.show {
                                PlaylistMenu(
                                    onDismissRequest = {
                                        menuState.dismiss()
                                    },
                                    onDelete = { showDeleteDialog = true },
                                    onPlayAll = {
                                        playBackViewModel.play(song = songs.first(), songs)
                                    },
                                    onRename = { showRenameDialog = true },
                                    onAddToPlaylist = {
                                        navigator?.navigate(
                                            Routes.AddSong(
                                                playlistId = playlist.playlistId ?: -1,
                                            ),
                                        )
                                    },
                                )
                            }
                        }.pressable(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    }
}

@Composable
private fun PlaylistMenu(
    onDismissRequest: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPlayAll: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .scrollEndHaptic(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.quick_action),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }

        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .border(
                            width = 1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                            MaterialTheme.shapes.small,
                        ).clip(MaterialTheme.shapes.small),
            ) {
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onRename.invoke()
                            onDismissRequest.invoke()
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.rename),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.rename),
                        )
                    },
                    supportingContent = {
                        Text(
                            "取一个新的歌单名字",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onAddToPlaylist()
                            onDismissRequest.invoke()
                        },
                    supportingContent = {
                        Text(
                            "搜索选择新的歌曲到歌单",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                    headlineContent = {
                        Text(
                            stringResource(R.string.add_new_song_to_playlist),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.add_new_song_to_playlist),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onDelete()
                            onDismissRequest.invoke()
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.delete),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                        )
                    },
                    supportingContent = {
                        Text(
                            "彻底删除这个歌单",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
            }
        }

        item {
            Text(
                stringResource(R.string.play_action),
                style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize()
                        .border(
                            width = 1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                            MaterialTheme.shapes.small,
                        ).clip(MaterialTheme.shapes.small),
            ) {
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onPlayAll()
                            onDismissRequest.invoke()
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.play_all),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = stringResource(R.string.play_all),
                        )
                    },
                    supportingContent = {
                        Text(
                            "使用当前的歌单来播放",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f))
                ListItem(
                    modifier =
                        Modifier.clickableWithVibration {
                            onPlayAll()
                            onDismissRequest.invoke()
                        },
                    headlineContent = {
                        Text(
                            stringResource(R.string.add_to_playlist),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    MaterialTheme.colorScheme.onSurface,
                                ),
                        )
                    },
                    supportingContent = {
                        Text(
                            "将当前的歌单添加到正在播放列表中",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                                ),
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_playlist),
                            contentDescription = stringResource(R.string.add_to_playlist),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SongMenu(
    song: Song,
    playlist: Playlist,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .scrollEndHaptic(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Row {
                    CoverWidget(song)
                }
            }
        }
    }
}

/**
 * 确认删除歌单的弹框
 */
@Composable
private fun DeleteSureDialog(
    playlistId: Long,
    onDismissRequest: () -> Unit = { },
    playlistViewModel: PlaylistViewModel,
    navigator: NavController? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = { onDismissRequest() },
        title = {
            Text("删除歌单")
        },
        text = {
            Text("确定要删除这个歌单吗?")
        },
        confirmButton = {
            TextButton(onClick = {
                // 确认删除
                coroutineScope.launch {
                    playlistViewModel.deletePlaylist(playlistId)
                    onDismissRequest()
                    navigator?.popBackStack()
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // 取消删除
                onDismissRequest()
            }) {
                Text("取消")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun TopAppBarScrollBehavior.expandAnimating() {
    AnimationState(
        initialValue = this.state.heightOffset,
    ).animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 500),
    ) { this@expandAnimating.state.heightOffset = value }
}
