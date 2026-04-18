package me.spica27.spicamusic.ui.allsong

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioQualityBadges
import me.spica27.spicamusic.utils.navSharedBounds
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * 所有歌曲页面
 */
@Composable
fun AllSongsScreen(
    modifier: Modifier = Modifier,
    viewModel: AllSongsViewModel = koinViewModel(),
) {
    val filteredSongs: LazyPagingItems<Song> = viewModel.filteredSongs.collectAsLazyPagingItems()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val currentPlayingMediaStoreId by viewModel.currentPlayingMediaStoreId.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val songCount = filteredSongs.itemCount

    val scrollBehavior = MiuixScrollBehavior()

    var showMultipleSelectMenu by remember { mutableStateOf(false) }

    val hazeSource = rememberHazeState()

    BackHandler(isMultiSelectMode || showMultipleSelectMenu) {
        if (showMultipleSelectMenu) {
            showMultipleSelectMenu = false
        } else if (isMultiSelectMode) {
            viewModel.exitMultiSelectMode()
        }
    }

    Scaffold(
        modifier =
            modifier
                .navSharedBounds(Screen.AllSongs)
                .fillMaxSize(),
        topBar = {
            Column(
                modifier =
                    Modifier
                        .hazeEffect(
                            hazeSource,
                            HazeMaterials.ultraThick(MiuixTheme.colorScheme.surface),
                        ) {
                            progressive =
                                HazeProgressive.verticalGradient(
                                    startIntensity = 1f,
                                    endIntensity = 0f,
                                )
                        }.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = isMultiSelectMode,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    },
                    label = "topbar_mode",
                ) { inMultiSelect ->
                    if (inMultiSelect) {
                        TopAppBar(
                            color = Color.Transparent,
                            title = "已选择 ${selectedSongIds.size} 首",
                            actions = {
                                // 全选按钮
                                IconButton(onClick = {
                                    if (selectedSongIds.size == songCount) {
                                        viewModel.deselectAll()
                                    } else {
                                        viewModel.selectAll()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.SelectAll,
                                        contentDescription = "全选",
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                // 确定按钮
                                IconButton(onClick = {
                                    if (selectedSongIds.isNotEmpty()) {
                                        showMultipleSelectMenu = true
                                    } else {
                                        viewModel.exitMultiSelectMode()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.confirm),
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                        )
                    } else {
                        TopAppBar(
                            scrollBehavior = scrollBehavior,
                            color = Color.Transparent,
                            title = "所有歌曲 ($songCount)",
                        )
                    }
                }
                // 功能按钮组
                AnimatedVisibility(!isMultiSelectMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Spacer(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                        )
                        TextField(
                            value = searchKeyword,
                            cornerRadius = 12.dp,
                            onValueChange = { viewModel.updateSearchKeyword(it) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            borderColor = MiuixTheme.colorScheme.surfaceContainer,
                            backgroundColor =
                                MiuixTheme.colorScheme.surfaceContainer.copy(
                                    alpha = 0.6f,
                                ),
                            insideMargin = DpSize(22.dp, 16.dp),
                            label = "搜索歌曲或艺术家",
                            maxLines = 1,
                            useLabelAsPlaceholder = true,
                            leadingIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "搜索",
                                        tint = MiuixTheme.colorScheme.onSurfaceContainer,
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchKeyword.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "清空",
                                            tint = MiuixTheme.colorScheme.onSurfaceContainer,
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                        )
                        FunctionButtonGroup(
                            songCount = songCount,
                            onPlayAll = {
                                viewModel.playAllSongs()
                            },
                            onMultiSelect = { viewModel.enterMultiSelectMode() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 歌曲列表
                LazyColumn(
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = paddingValues.calculateTopPadding(),
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .hazeSource(hazeSource)
                            .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .weight(1f),
                ) {
                    items(
                        count = filteredSongs.itemCount,
                        key = filteredSongs.itemKey { it.songId ?: it.mediaStoreId },
                    ) { index ->
                        val song = filteredSongs[index] ?: return@items
                        SongItemCard(
                            song = song,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedSongIds.contains(song.songId),
                            isPlaying = currentPlayingMediaStoreId == song.mediaStoreId,
                            onItemClick = {
                                if (isMultiSelectMode) {
                                    song.songId?.let { viewModel.toggleSongSelection(it) }
                                } else {
                                    viewModel.playAllSongs(song.mediaStoreId)
                                }
                            },
                            onItemLongClick = {
                                if (!isMultiSelectMode) {
                                    song.songId?.let { viewModel.enterMultiSelectMode(it) }
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    // 建议10：搜索无结果时显示空态提示
                    if (
                        filteredSongs.itemCount == 0 &&
                        searchKeyword.isNotEmpty() &&
                        filteredSongs.loadState.refresh !is LoadState.Loading
                    ) {
                        item {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                )
                                Text(
                                    text = "未找到「$searchKeyword」相关歌曲",
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = "尝试搜索歌曲名或艺术家名",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // 底部占位，避免被浮动菜单遮挡
                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }

            // 多选模式底部浮动菜单
            AnimatedVisibility(
                visible = showMultipleSelectMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                MultiSelectBottomMenu(
                    selectedCount = selectedSongIds.size,
                    onFavorite = {
                        //  收藏选中歌曲
                        viewModel.likeSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onUnfavorite = {
                        //  取消收藏
                        viewModel.dislikeSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onPlayAll = {
                        //  全部播放
                        viewModel.playSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onAddToQueen = {
                        //  下一首播放
                        viewModel.addToQueueSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onCreatePlaylist = {
                        // 创建歌单
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onHide = {
                        // 隐藏歌曲
                        viewModel.hideSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .clip(Shapes.SmallCornerBasedShape)
                            .hazeEffect(
                                hazeSource,
                                HazeMaterials.thin(
                                    MiuixTheme.colorScheme.primaryContainer,
                                ),
                            ) {
                                blurRadius = 20.dp
                            }.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 功能按钮组（播放全部、多选模式）
 */
@Composable
private fun FunctionButtonGroup(
    songCount: Int,
    onPlayAll: () -> Unit,
    onMultiSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 播放全部按钮
        Card(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
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
                        .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.play_all),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // 多选模式按钮
        Card(
            onClick = onMultiSelect,
            modifier = Modifier.weight(1f),
            colors =
                CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.tertiaryContainer,
                    contentColor = MiuixTheme.colorScheme.tertiaryContainer,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(R.string.multi_select),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

/**
 * 歌曲列表项卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemCard(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MiuixTheme.colorScheme.surface
            },
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        colors =
            CardDefaults.defaultColors(
                backgroundColor,
                backgroundColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 多选模式下的选中图标
            if (isMultiSelectMode) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint =
                        if (isSelected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    modifier =
                        Modifier
                            .size(28.dp)
                            .padding(end = 12.dp),
                )
            }

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 建议8：正在播放指示图标
                    AnimatedVisibility(
                        visible = isPlaying,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = song.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color =
                            if (isSelected || isPlaying) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (song.like) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    AudioQualityBadges(song)
                    Text(
                        text = song.artist,
                        fontSize = 14.sp,
                        color =
                            if (isSelected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 时长
            Text(
                text = formatDuration(song.duration),
                fontSize = 14.sp,
                color =
                    if (isSelected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * 多选模式底部浮动菜单
 */
@Composable
private fun MultiSelectBottomMenu(
    selectedCount: Int,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onPlayAll: () -> Unit,
    onAddToQueen: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 菜单项分两行显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MenuActionItem(
                    icon = Icons.Default.Favorite,
                    label = stringResource(R.string.favorite),
                    onClick = onFavorite,
                    modifier = Modifier.weight(1f),
                )
                MenuActionItem(
                    icon = Icons.Default.FavoriteBorder,
                    label = stringResource(R.string.remove_from_favorites),
                    onClick = onUnfavorite,
                    modifier = Modifier.weight(1f),
                )
                MenuActionItem(
                    icon = Icons.Default.QueueMusic,
                    label = stringResource(R.string.play_all),
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MenuActionItem(
                    icon = Icons.Default.SkipNext,
                    label = stringResource(R.string.add_to_queue),
                    onClick = onAddToQueen,
                    modifier = Modifier.weight(1f),
                )
                MenuActionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = stringResource(R.string.create_playlist),
                    onClick = onCreatePlaylist,
                    modifier = Modifier.weight(1f),
                )
                MenuActionItem(
                    icon = Icons.Default.VisibilityOff,
                    label = stringResource(R.string.hide),
                    onClick = onHide,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 菜单操作项
 */
@Composable
private fun MenuActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(8.dp)
                .combinedClickable(
                    onClick = onClick,
                    indication = ripple(bounded = false, radius = 32.dp),
                    interactionSource = remember { MutableInteractionSource() },
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * 格式化时长（毫秒 -> mm:ss）
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
