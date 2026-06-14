package me.spica27.spicamusic.ui.playlistdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ── 滚动驱动常量（像素）────────────────────────────────────────────────────────
private const val SCROLL_ART_RANGE = 460f
private const val SCROLL_HDR_START = 250f
private const val SCROLL_HDR_RANGE = 210f

// ── 布局尺寸常量 ───────────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp
private val COVER_EXPANDED = 180.dp
private val COVER_COLLAPSED = 38.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(playlist: Playlist) {
    val path = LocalNavigationPath.current
    val viewModel =
        koinViewModel<PlaylistDetailViewModel>(
            key = "PlaylistDetailViewModel_${playlist.playlistId}",
        ) { parametersOf(playlist.playlistId) }

    // ── State collection ───────────────────────────────────────────────────
    val currentPlaylist by viewModel.playlist.collectAsStateWithLifecycle()
    val songs = viewModel.displayedSongs.collectAsLazyPagingItems()
    val coverAlbumIds by viewModel.coverAlbumIds.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val isSearchMode by viewModel.isSearchMode.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongs by viewModel.selectedSongs.collectAsStateWithLifecycle()
    val showRenameDialog by viewModel.showRenameDialog.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsStateWithLifecycle()
    val showAddSongsSheet by viewModel.showAddSongsSheet.collectAsStateWithLifecycle()
    val showMoreOptionsMenu by viewModel.showMoreOptionsMenu.collectAsStateWithLifecycle()
    val playlistDeleted by viewModel.playlistDeleted.collectAsStateWithLifecycle()

    // 歌单被删除时返回上一页
    LaunchedEffect(playlistDeleted) {
        if (playlistDeleted) path.popTop()
    }

    BackHandler(isMultiSelectMode) {
        viewModel.toggleMultiSelectMode()
    }

    val displayName = currentPlaylist?.playlistName ?: playlist.playlistName
    BackHandler(enabled = isSearchMode) {
        viewModel.exitSearchMode()
    }

    // ── 滚动状态与动画 ──────────────────────────────────────────────────────
    val lazyListState = rememberLazyListState()
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidthDp = 375.dp

    val rawOffset by remember(lazyListState) {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                SCROLL_ART_RANGE
            } else {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }
    val artProgress by remember {
        derivedStateOf { (rawOffset / SCROLL_ART_RANGE).coerceIn(0f, 1f) }
    }
    val hdrProgress by remember {
        derivedStateOf { ((rawOffset - SCROLL_HDR_START) / SCROLL_HDR_RANGE).coerceIn(0f, 1f) }
    }

    val springDp = remember { spring<Dp>(stiffness = 400f) }
    val springFloat = remember { spring<Float>(stiffness = 400f) }

    val artTopExpanded = statusBarTopDp + HEADER_HEIGHT + 8.dp
    val artTopCollapsed = statusBarTopDp + (HEADER_HEIGHT - COVER_COLLAPSED) / 2f
    val artStartExpanded = (screenWidthDp - COVER_EXPANDED) / 2f
    val artStartCollapsed = 56.dp

    val coverSize by animateDpAsState(
        targetValue = lerp(COVER_EXPANDED.value, COVER_COLLAPSED.value, artProgress).dp,
        animationSpec = springDp,
        label = "coverSize",
    )
    val coverTop by animateDpAsState(
        targetValue = lerp(artTopExpanded.value, artTopCollapsed.value, artProgress).dp,
        animationSpec = springDp,
        label = "coverTop",
    )
    val coverStart by animateDpAsState(
        targetValue = lerp(artStartExpanded.value, artStartCollapsed.value, artProgress).dp,
        animationSpec = springDp,
        label = "coverStart",
    )
    val cornerRad by animateDpAsState(
        targetValue = lerp(16f, 8f, artProgress).dp,
        animationSpec = springDp,
        label = "cornerRad",
    )
    val bigAlpha by animateFloatAsState(
        targetValue = (1f - artProgress * 2.5f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "bigAlpha",
    )
    val smallAlpha by animateFloatAsState(
        targetValue = (hdrProgress * 2f).coerceIn(0f, 1f),
        animationSpec = springFloat,
        label = "smallAlpha",
    )
    val hdrAlpha by animateFloatAsState(
        targetValue = hdrProgress,
        animationSpec = springFloat,
        label = "hdrAlpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── 渐变背景（主色调区域）─────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT + COVER_EXPANDED + 120.dp)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                ),
        )

        // ── 可滚动内容 ────────────────────────────────────────────────────────
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            contentPadding =
                PaddingValues(
                    top = statusBarTopDp + HEADER_HEIGHT + COVER_EXPANDED + 12.dp,
                    bottom = 200.dp,
                ),
        ) {
            // 歌单名称 + 歌曲数（随滚动淡出）
            item(key = "playlist_header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .graphicsLayer { alpha = bigAlpha },
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$songCount 首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 播放 / 添加歌曲按钮行
            item(key = "play_buttons") {
                PlayButtons(
                    onPlayAll = viewModel::playAll,
                    onAddSongs = viewModel::showAddSongsSheet,
                )
            }

            // 歌曲列表
            items(count = songs.itemCount) { index ->
                val song = songs[index]
                if (song != null) {
                    SongRow(
                        song = song,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = selectedSongs.contains(song.mediaStoreId),
                        onClick = {
                            if (isMultiSelectMode) {
                                viewModel.toggleSongSelection(song.mediaStoreId)
                            } else {
                                viewModel.playSongInList(song)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) viewModel.toggleMultiSelectMode()
                            viewModel.toggleSongSelection(song.mediaStoreId)
                        },
                        onMore = { path.push(SongMenuScene(song)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item {
                Spacer(Modifier.height(150.dp))
            }
        }

        // ── 固定顶栏遮罩 ──────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTopDp + HEADER_HEIGHT)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.background.copy(alpha = hdrAlpha)),
        ) {
            AnimatedContent(
                targetState = isSearchMode,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn() + slideInHorizontally { it / 3 }) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 3 })
                    } else {
                        (fadeIn() + slideInHorizontally { -it / 3 }) togetherWith
                            (fadeOut() + slideOutHorizontally { it / 3 })
                    }
                },
                label = "playlistSearchTopBar",
            ) { searching ->
                if (searching) {
                    SearchTopBar(
                        keyword = searchKeyword,
                        onKeywordChange = viewModel::updateSearchKeyword,
                        onClose = viewModel::exitSearchMode,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { path.popTop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = displayName,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(start = 55.dp, end = 4.dp)
                                    .graphicsLayer { alpha = smallAlpha },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = viewModel::enterSearchMode) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box {
                            IconButton(onClick = viewModel::showMoreOptionsMenu) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showMoreOptionsMenu,
                                onDismissRequest = viewModel::hideMoreOptionsMenu,
                                shape = RoundedCornerShape(22.dp),
                                offset = DpOffset(x = (-12).dp, y = 0.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp,
                            ) {
                                PlaylistDropdownMenuItem(
                                    text = "添加歌曲",
                                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    onClick = {
                                        viewModel.hideMoreOptionsMenu()
                                        viewModel.showAddSongsSheet()
                                    },
                                )
                                if (isMultiSelectMode) {
                                    PlaylistDropdownMenuItem(
                                        text = "全选",
                                        icon = Icons.Default.CheckBox,
                                        onClick = {
                                            viewModel.hideMoreOptionsMenu()
                                            viewModel.selectAll()
                                        },
                                    )
                                    PlaylistDropdownMenuItem(
                                        text = "取消全选",
                                        icon = Icons.Default.CheckBoxOutlineBlank,
                                        onClick = {
                                            viewModel.hideMoreOptionsMenu()
                                            viewModel.deselectAll()
                                        },
                                    )
                                } else {
                                    PlaylistDropdownMenuItem(
                                        text = "多选",
                                        icon = Icons.Default.CheckBoxOutlineBlank,
                                        onClick = {
                                            viewModel.hideMoreOptionsMenu()
                                            viewModel.toggleMultiSelectMode()
                                        },
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                )
                                PlaylistDropdownMenuItem(
                                    text = "重命名",
                                    icon = Icons.Default.Edit,
                                    onClick = {
                                        viewModel.hideMoreOptionsMenu()
                                        viewModel.showRenameDialog()
                                    },
                                )
                                PlaylistDropdownMenuItem(
                                    text = "删除歌单",
                                    icon = Icons.Default.Delete,
                                    destructive = true,
                                    onClick = {
                                        viewModel.hideMoreOptionsMenu()
                                        viewModel.showDeleteConfirmDialog()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 浮动歌单封面（随滚动动画缩小、移入顶栏）─────────────────────────────
        Box(
            Modifier
                .padding(start = coverStart, top = coverTop)
                .size(coverSize)
                .clip(RoundedCornerShape(cornerRad)),
        ) {
            PlaylistCoverView(
                albumIds = coverAlbumIds,
                modifier = Modifier.fillMaxSize(),
                iconSize = (coverSize.value * 0.35f).dp,
            )
        }

        // ── 多选底部操作栏 ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isMultiSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            MultiSelectBar(
                selectedCount = selectedSongs.size,
                onClose = viewModel::toggleMultiSelectMode,
                onSelectAll = viewModel::selectAll,
                onDeselectAll = viewModel::deselectAll,
                onRemove = viewModel::removeSelectedSongs,
            )
        }
    }

    // ── 重命名对话框 ───────────────────────────────────────────────────────────
    if (showRenameDialog) {
        RenameDialog(
            initialName = displayName,
            onConfirm = viewModel::renamePlaylist,
            onDismiss = viewModel::hideRenameDialog,
        )
    }

    // ── 删除确认对话框 ─────────────────────────────────────────────────────────
    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            playlistName = displayName,
            onConfirm = viewModel::deletePlaylist,
            onDismiss = viewModel::hideDeleteConfirmDialog,
        )
    }

    // ── 添加歌曲底部弹窗 ───────────────────────────────────────────────────────
    if (showAddSongsSheet) {
        SongPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = viewModel::hideAddSongsSheet,
        )
    }
}

@Composable
private fun PlaylistDropdownMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor =
        if (destructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val iconContainerColor =
        if (destructive) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        }
    val iconColor =
        if (destructive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

    DropdownMenuItem(
        modifier =
            Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .width(220.dp)
                .clip(RoundedCornerShape(16.dp)),
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
        },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(11.dp),
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        colors =
            MenuDefaults.itemColors(
                textColor = contentColor,
                leadingIconColor = iconColor,
            ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        onClick = onClick,
    )
}

// ── 播放 / 添加歌曲操作行 ──────────────────────────────────────────────────────

@Composable
private fun PlayButtons(
    onPlayAll: () -> Unit,
    onAddSongs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedButton(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("播放全部")
        }
        ElevatedButton(onClick = onAddSongs, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("添加歌曲")
        }
    }
}

// ── 歌曲列表行 ────────────────────────────────────────────────────────────────

@Composable
private fun SongRow(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.background
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(rowBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
            )
        }
        LandscapistImage(
            imageModel = { song.getCoverUri() },
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            failure = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isMultiSelectMode) {
            IconButton(onClick = onMore) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

// ── 搜索顶栏 ──────────────────────────────────────────────────────────────────

@Composable
private fun SearchTopBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = keyword,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                onValueChange = onKeywordChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "搜索歌单内歌曲",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                trailingIcon = null,
            )
            AnimatedVisibility(
                visible = keyword.isNotEmpty(),
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
            ) {
                IconButton(onClick = { onKeywordChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── 多选底部操作栏 ─────────────────────────────────────────────────────────────

@Composable
private fun MultiSelectBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "退出多选")
            }
            Text(
                text = "已选 $selectedCount 首",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onSelectAll) { Text("全选") }
            TextButton(onClick = onDeselectAll) { Text("取消") }
            Button(
                onClick = onRemove,
                enabled = selectedCount > 0,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("移除")
            }
        }
    }
}

// ── 重命名对话框 ───────────────────────────────────────────────────────────────

@Composable
fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "重命名歌单",
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                colors =
                    TextFieldDefaults.colors().copy(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                value = name,
                onValueChange = { name = it },
                label = { Text("歌单名称") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// ── 删除确认对话框 ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除歌单") },
        text = { Text("确定要删除歌单「$playlistName」吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// ── 添加歌曲选择器底部弹窗 ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerBottomSheet(
    viewModel: PlaylistDetailViewModel,
    onDismiss: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pickerKeyword by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(pickerKeyword) {
        viewModel.updatePickerKeyword(pickerKeyword)
    }

    val pickerSongs = viewModel.pickerSongsPaging.collectAsLazyPagingItems()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "添加歌曲",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            if (selectedIds.isEmpty()) {
                                "从音乐库中选择要加入歌单的歌曲"
                            } else {
                                "已选择 ${selectedIds.size} 首歌曲"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            TextField(
                value = pickerKeyword,
                colors =
                    TextFieldDefaults.colors().copy(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                onValueChange = { pickerKeyword = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                placeholder = { Text("搜索歌曲、歌手或专辑") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon =
                    if (pickerKeyword.isNotEmpty()) {
                        {
                            IconButton(onClick = { pickerKeyword = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    } else {
                        null
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                shape = RoundedCornerShape(24.dp),
            )

            AnimatedContent(selectedIds.isNotEmpty()) {
                if (it) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "将添加 ${selectedIds.size} 首歌曲",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { selectedIds = emptySet() }) {
                                Text("清空")
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(count = pickerSongs.itemCount, key = { index ->
                        pickerSongs[index]?.mediaStoreId ?: index
                    }) { index ->
                        val song = pickerSongs[index] ?: return@items
                        val isSelected = selectedIds.contains(song.mediaStoreId)
                        PickerSongRow(
                            modifier = Modifier.animateItem(),
                            song = song,
                            isSelected = isSelected,
                            onToggle = {
                                selectedIds =
                                    if (isSelected) {
                                        selectedIds - song.mediaStoreId
                                    } else {
                                        selectedIds + song.mediaStoreId
                                    }
                            },
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { viewModel.addSongsToPlaylist(selectedIds.toList()) },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1.5f),
                        ) {
                            Text(if (selectedIds.isEmpty()) "选择歌曲" else "添加 ${selectedIds.size} 首")
                        }
                    }
                }
            }
        }
    }
}

// ── 选择器歌曲行 ───────────────────────────────────────────────────────────────

@Composable
private fun PickerSongRow(
    song: Song,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = rowBackground,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LandscapistImage(
                imageModel = { song.getCoverUri() },
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                success = { _, painter ->
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                failure = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
            )
        }
    }
}
