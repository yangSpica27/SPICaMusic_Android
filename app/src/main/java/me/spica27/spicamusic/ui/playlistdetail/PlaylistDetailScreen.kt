package me.spica27.spicamusic.ui.playlistdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.SongPickerSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.MiuixPopupHost
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollOutOfBound
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup

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

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val displayedSongs by viewModel.displayedSongs.collectAsStateWithLifecycle()
    val isSearchMode by viewModel.isSearchMode.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongs by viewModel.selectedSongs.collectAsStateWithLifecycle()
    val showRenameDialog by viewModel.showRenameDialog.collectAsStateWithLifecycle()
    val showAddSongsSheet by viewModel.showAddSongsSheet.collectAsStateWithLifecycle()
    val pickerSongCount by viewModel.pickerSongCount.collectAsStateWithLifecycle()

    // 添加歌曲选择器
    if (showAddSongsSheet) {
        SongPickerSheet(
            songsPagingFlow = viewModel.pickerSongsPaging,
            songCount = pickerSongCount,
            onKeywordChange = viewModel::updatePickerKeyword,
            onSelectAll = viewModel::getPickerSongIds,
            onDismiss = {
                viewModel.updatePickerKeyword("")
                viewModel.hideAddSongsSheet()
            },
            onConfirm = { selectedSongIds ->
                viewModel.addSongsToPlaylist(selectedSongIds)
            },
            title = stringResource(R.string.add_songs_to_playlist),
        )
    }

    // 处理返回键
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.toggleMultiSelectMode()
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    // 搜索模式 BackHandler（注册在后 = 优先级更高）：
    //   键盘可见 → 先收键盘；键盘已收 → 退出搜索并清空关键字
    BackHandler(enabled = isSearchMode && imeVisible) {
        keyboardController?.hide()
    }
    BackHandler(enabled = isSearchMode && !imeVisible) {
        viewModel.exitSearchMode()
    }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            delay(150) // 等待 AnimatedContent 动画开始后再请求焦点
            searchFocusRequester.requestFocus()
        }
    }

    val localNavSharedTransitionScope = LocalNavSharedTransitionScope.current

    val localNavAnimatedContentScope = LocalNavAnimatedContentScope.current

    val hazeState = rememberHazeState()

    with(localNavSharedTransitionScope) {
        Scaffold(
            modifier =
                modifier
                    .sharedBounds(
                        rememberSharedContentState(playlist ?: ""),
                        animatedVisibilityScope = localNavAnimatedContentScope,
                    ).fillMaxSize(),
            popupHost = { MiuixPopupHost() },
            topBar = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TopBarIconButton(
                            hazeState = hazeState,
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "返回",
                            onClick = { backStack.removeLastOrNull() },
                        )
                        // 右侧区域：普通按钮 ↔ 搜索栏 切换动画
                        AnimatedContent(
                            targetState = isSearchMode && !isMultiSelectMode,
                            modifier = Modifier.weight(1f),
                            transitionSpec = {
                                if (targetState) {
                                    (fadeIn() + slideInHorizontally { it / 2 }) togetherWith
                                        (fadeOut() + slideOutHorizontally { -it / 2 })
                                } else {
                                    (fadeIn() + slideInHorizontally { -it / 2 }) togetherWith
                                        (fadeOut() + slideOutHorizontally { it / 2 })
                                }
                            },
                            contentKey = { it },
                        ) { searchMode ->
                            if (searchMode) {
                                SearchBarField(
                                    keyword = searchKeyword,
                                    onKeywordChange = viewModel::updateSearchKeyword,
                                    focusRequester = searchFocusRequester,
                                    hazeState = hazeState,
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    AnimatedContent(
                                        isMultiSelectMode,
                                        contentKey = { it },
                                        transitionSpec = {
                                            fadeIn() +
                                                slideIntoContainer(
                                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                                    animationSpec = tween(300),
                                                ) togetherWith fadeOut() +
                                                slideOutOfContainer(
                                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                                    animationSpec = tween(300),
                                                )
                                        },
                                    ) { isMultiSelectMode ->
                                        if (isMultiSelectMode) {
                                            MultiSelectRightIcons(
                                                viewModel = viewModel,
                                                hazeState = hazeState,
                                            )
                                        } else {
                                            NormalRightIcons(
                                                viewModel = viewModel,
                                                hazeState = hazeState,
                                                onSearchClick = viewModel::enterSearchMode,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .hazeSource(hazeState)
                        .fillMaxSize(),
            ) {
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
                                .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                                .overScrollOutOfBound(),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 180.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            PlaylistHeader(
                                playlist = playlist ?: return@item,
                                songs = songs,
                                songCount = songs.size,
                                modifier =
                                    Modifier.padding(
                                        top = paddingValues.calculateTopPadding(),
                                        bottom = 16.dp,
                                    ),
                                viewModel = viewModel,
                            )
                        }
                        if (isSearchMode && displayedSongs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "没有找到「$searchKeyword」相关的歌曲",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                displayedSongs,
                                key = { _, song -> song.songId ?: song.mediaStoreId },
                            ) { index, song ->
                                SongItemCard(
                                    modifier = Modifier.animateItem(),
                                    index = index,
                                    song = song,
                                    isMultiSelectMode = isMultiSelectMode,
                                    isSelected = selectedSongs.contains(song.songId),
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            viewModel.toggleSongSelection(song.songId)
                                        } else {
                                            viewModel.playSongInList(song)
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
    }
}

/**
 * 歌单 Header
 */
@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    songs: List<Song>,
    songCount: Int,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel,
) {
    Row(
        modifier =
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Grid 组合封面
        PlaylistGridCover(
            songs = songs,
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(Shapes.MediumCornerBasedShape),
        )

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
                text = stringResource(R.string.songs_count_format, songCount),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            if (playlist.playTimes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.play_count_format, playlist.playTimes),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.playAll() },
                insideMargin =
                    PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
                colors =
                    ButtonDefaults.buttonColors(
                        MiuixTheme.colorScheme.primaryContainer,
                        MiuixTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.play_all),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onPrimary,
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
    onAddSongs: () -> Unit,
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
            cornerRadius = 16.dp,
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
                    text = stringResource(R.string.play_all),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }

        // 多选模式按钮
        Card(
            onClick = onToggleMultiSelect,
            cornerRadius = 16.dp,
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
                    contentDescription = stringResource(R.string.multi_select),
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
        }

        // 菜单按钮
        Box {
            Card(
                cornerRadius = 16.dp,
                onClick = { showMenu = true },
                colors =
                    CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceVariant,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    ),
                modifier = Modifier.size(48.dp),
            ) {
                // 下拉菜单
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                    WindowListPopup(
                        show = showMenu,
                        alignment = PopupPositionProvider.Align.End,
                        onDismissRequest = { showMenu = false },
                    ) {
                        ListPopupColumn {
                            TextButton(
                                cornerRadius = 0.dp,
                                text = stringResource(R.string.add_songs),
                                onClick = {
                                    showMenu = false
                                    onAddSongs()
                                },
                            )
                            TextButton(
                                cornerRadius = 0.dp,
                                text = "重命名歌单",
                                onClick = {
                                    showMenu = false
                                    onShowMenu()
                                },
                            )
                            TextButton(
                                cornerRadius = 0.dp,
                                text = "删除歌单",
                                colors =
                                    ButtonDefaults.textButtonColors().copy(
                                        textColor = MiuixTheme.colorScheme.error,
                                    ),
                                onClick = {
                                    showMenu = false
                                    // 删除歌单逻辑
                                },
                            )
                        }
                    }
                }
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
    index: Int = 0,
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
                .clip(Shapes.LargeCornerBasedShape)
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
                    .padding(
                        top = 10.dp,
                        bottom = 10.dp,
                        start = 0.dp,
                        end = 12.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(55.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.width(12.dp))
            // 封面
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(Shapes.SmallCornerBasedShape)
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
                                    .clip(Shapes.SmallCornerBasedShape)
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

            AnimatedVisibility(
                visible = isMultiSelectMode,
            ) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // 选中状态图标
            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
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
        }
    }
}

@Composable
private fun NormalRightIcons(
    viewModel: PlaylistDetailViewModel,
    hazeState: HazeState,
    onSearchClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBarIconButton(
            hazeState = hazeState,
            imageVector = Icons.Default.Search,
            contentDescription = "搜索歌曲",
            onClick = onSearchClick,
        )
        TopBarIconButton(
            hazeState = hazeState,
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add_songs),
            onClick = { viewModel.showAddSongsSheet() },
        )
        TopBarIconButton(
            hazeState = hazeState,
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.more),
            onClick = { /* TODO: 显示更多操作菜单 */ },
        )
    }
}

/** 顶栏内联搜索框 */
@Composable
private fun SearchBarField(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    focusRequester: FocusRequester,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier =
            modifier
                .height(48.dp)
                .clip(CircleShape)
                .hazeEffect(
                    hazeState,
                    HazeMaterials.ultraThin(containerColor = MiuixTheme.colorScheme.surface),
                ).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            singleLine = true,
            textStyle =
                MiuixTheme.textStyles.body1.copy(
                    color = MiuixTheme.colorScheme.onSurface,
                ),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (keyword.isEmpty()) {
                        Text(
                            text = "搜索歌曲",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.4f),
                        )
                    }
                    innerTextField()
                }
            },
        )
        AnimatedVisibility(
            visible = keyword.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.7f),
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "清除搜索",
                modifier =
                    Modifier
                        .size(18.dp)
                        .clickable { onKeywordChange("") },
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    hazeState: HazeState,
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MiuixTheme.colorScheme.onSurface,
    contentColor: Color = MiuixTheme.colorScheme.surface,
) {
    IconButton(
        onClick = {
            onClick.invoke()
        },
        modifier =
            modifier
                .size(48.dp)
                .clip(CircleShape)
                .hazeEffect(
                    hazeState,
                    HazeMaterials.ultraThin(
                        containerColor = contentColor,
                    ),
                ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tintColor,
        )
    }
}

@Composable
fun MultiSelectRightIcons(
    viewModel: PlaylistDetailViewModel,
    hazeState: HazeState,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBarIconButton(
            hazeState = hazeState,
            imageVector = Icons.Default.SelectAll,
            contentDescription = stringResource(R.string.play_all),
            onClick = { viewModel.selectAll() },
        )
        TopBarIconButton(
            hazeState = hazeState,
            imageVector = Icons.Default.PlaylistRemove,
            contentDescription = null,
            onClick = {
                viewModel.removeSelectedSongs()
            },
            contentColor = MiuixTheme.colorScheme.error,
            tintColor = MiuixTheme.colorScheme.onError,
        )
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
            text = stringResource(R.string.playlist_empty),
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.playlist_empty_hint),
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

    WindowDialog(
        title = stringResource(R.string.rename),
        onDismissRequest = onDismiss,
        show = show.value,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
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
                    modifier =
                        Modifier.pressable(
                            interactionSource = null,
                            indication = SinkFeedback(),
                        ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != currentName) {
                            onRename(newName)
                        }
                    },
                    modifier =
                        Modifier.pressable(
                            interactionSource = null,
                            indication = SinkFeedback(),
                        ),
                    enabled = newName.isNotBlank() && newName != currentName,
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
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
        modifier = modifier.background(MiuixTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (songs.isEmpty()) {
            // 空歌单默认封面
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
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
