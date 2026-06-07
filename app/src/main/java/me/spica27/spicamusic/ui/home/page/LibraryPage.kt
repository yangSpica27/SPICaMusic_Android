package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.ImmutableList
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScene
import me.spica27.spicamusic.ui.playlistdetail.PlaylistDetailScene
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.settings.SettingsScene
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.concurrent.TimeUnit

@Composable
fun LibraryPage() {
    val path = LocalNavigationPath.current
    val viewModel: LibraryPageViewModel = koinActivityViewModel()

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val extraFolders by viewModel.extraFolders.collectAsStateWithLifecycle()
    val ignoreFolders by viewModel.ignoreFolders.collectAsStateWithLifecycle()

    var selectTab by remember { mutableStateOf(LibraryPageTab.Playlist) }
    val tabs = remember { LibraryPageTab.entries }
    val pagerState = rememberPagerState { tabs.size }

    LaunchedEffect(selectTab) {
        val index = tabs.indexOf(selectTab)
        if (index != pagerState.targetPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.targetPage) {
        val tab = tabs[pagerState.targetPage]
        if (tab != selectTab) {
            selectTab = tab
        }
    }

    val summaryText =
        remember(playlists.size, extraFolders.size, ignoreFolders.size, weeklyStats) {
            buildString {
                append("${playlists.size} 个歌单")
                if (extraFolders.isNotEmpty()) append(" · ${extraFolders.size} 个扫描目录")
                if (ignoreFolders.isNotEmpty()) append(" · ${ignoreFolders.size} 个忽略目录")
                weeklyStats?.let { stats ->
                    if (stats.totalPlayedDuration > 0) {
                        append(" · 本周 ${formatPlayDuration(stats.totalPlayedDuration)}")
                    }
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        LibraryPageHeader(
            summaryText = summaryText,
            tabs = ImmutableList.copyOf(tabs),
            selectTab = selectTab,
            onSelectTab = { selectTab = it },
            onScanClick = { path.push(ScannerScene()) },
            onCreatePlaylistClick = { path.push(PlaylistCreatorScene()) },
            extraText = { tab ->
                when (tab) {
                    LibraryPageTab.Playlist -> if (playlists.isNotEmpty()) "${playlists.size}个" else null
                    LibraryPageTab.Folder -> {
                        val total = extraFolders.size + ignoreFolders.size
                        if (total > 0) "${total}个" else "空空如也"
                    }
                }
            },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Horizontal),
        ) { page ->
            when (tabs[page]) {
                LibraryPageTab.Playlist ->
                    PlaylistPage(
                        playlists = ImmutableList.copyOf(playlists),
                        weeklyStats = weeklyStats,
                    )

                LibraryPageTab.Folder ->
                    FolderPage(
                        extraFolders = ImmutableList.copyOf(extraFolders),
                        ignoreFolders = ImmutableList.copyOf(ignoreFolders),
                        onScanClick = { path.push(ScannerScene()) },
                        onManageClick = { path.push(SettingsScene()) },
                    )
            }
        }
    }
}

@Composable
private fun LibraryPageHeader(
    modifier: Modifier = Modifier,
    summaryText: String,
    tabs: ImmutableList<LibraryPageTab>,
    selectTab: LibraryPageTab,
    onSelectTab: (LibraryPageTab) -> Unit,
    onScanClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    extraText: (LibraryPageTab) -> String? = { null },
) {
    val scrollConnection = LocalBottomBarScrollConnection.current

    val headerBackground =
        animateColorAsState(
            if (scrollConnection.isInline) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surface
            },
        ).value

    val progress =
        animateFloatAsState(
            targetValue = if (scrollConnection.isInline) 1f else 0f,
            label = "header_progress",
            animationSpec = tween(durationMillis = 155, easing = EaseInOutCubic),
        ).value

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 185),
                ).background(
                    headerBackground,
                ).statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 180))
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                    .padding(
                        top = LayoutTokens.MusicHeaderTopPadding,
                        bottom = LayoutTokens.MusicHeaderBottomPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(
                modifier =
                    Modifier
                        .animateContentSize(animationSpec = tween(durationMillis = 220))
                        .followHeaderSection(progress)
                        .graphicsLayer {
                            translationY = -28f * progress
                            alpha = 1f - progress
                        },
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = "资料库",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier =
                    Modifier
                        .animateContentSize(animationSpec = tween(durationMillis = 255))
                        .followHeaderSection(progress)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                ElevatedButton(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Scanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    Text("扫描音乐")
                }
                FilledTonalButton(
                    onClick = onCreatePlaylistClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    Text("新建歌单")
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(Shapes.ExtraLarge1CornerBasedShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(LayoutTokens.MusicTabContainerPadding),
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                tabs.forEach { tab ->
                    LibraryTabItem(
                        modifier = Modifier.weight(1f),
                        selectTab = selectTab,
                        onSelectTab = onSelectTab,
                        tab = tab,
                        extraText = extraText(tab),
                        progress = progress,
                    )
                }
            }
        }
    }
}

@Immutable
private enum class LibraryPageTab(
    val title: String,
) {
    Playlist("歌单"),
    Folder("文件夹"),
}

@Composable
private fun LibraryTabItem(
    modifier: Modifier = Modifier,
    selectTab: LibraryPageTab,
    onSelectTab: (LibraryPageTab) -> Unit,
    extraText: String? = null,
    tab: LibraryPageTab,
    progress: Float = 0f,
) {
    val isSelected = remember(tab, selectTab) { selectTab == tab }
    val containerColor =
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
            label = "library_tab_container",
        ).value
    val textColor =
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "library_tab_text",
        ).value
    val secondaryTextColor =
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "library_tab_secondary_text",
        ).value

    Column(
        modifier =
            modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 225),
                ).clip(Shapes.LargeCornerBasedShape)
                .background(containerColor)
                .clickable { onSelectTab(tab) }
                .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tab.title,
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (progress < 0.1f && extraText != null) {
            Text(
                text = extraText,
                color = secondaryTextColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaylistPage(
    playlists: ImmutableList<Playlist>,
    weeklyStats: PlayStats?,
    modifier: Modifier = Modifier,
) {
    val navigationPath = LocalNavigationPath.current

    LazyVerticalGrid(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(LocalBottomBarScrollConnection.current),
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                start = LayoutTokens.MusicHeaderHorizontalPadding,
                end = LayoutTokens.MusicHeaderHorizontalPadding,
                top = Spacing.Small,
                bottom = 200.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            WeeklyStatsCard(weeklyStats = weeklyStats)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(
                title = "我的歌单",
                subtitle = "${playlists.size} 个",
            )
        }

        if (playlists.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyLibraryCard(
                    title = "还没有歌单",
                    subtitle = "点击上方「新建歌单」创建第一张歌单。",
                )
            }
        } else {
            items(
                items = playlists,
                key = { it.playlistId ?: it.playlistName.hashCode().toLong() },
            ) { playlist ->
                PlaylistItem(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { navigationPath.push(PlaylistDetailScene(playlist)) },
                    playlist = playlist,
                )
            }
        }
        item {
            Spacer(Modifier.height(150.dp))
        }
    }
}

@Composable
private fun FolderPage(
    extraFolders: ImmutableList<ScanFolder>,
    ignoreFolders: ImmutableList<ScanFolder>,
    onScanClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(LocalBottomBarScrollConnection.current),
        contentPadding =
            PaddingValues(
                start = LayoutTokens.MusicHeaderHorizontalPadding,
                end = LayoutTokens.MusicHeaderHorizontalPadding,
                top = Spacing.Small,
                bottom = 200.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
    ) {
        item {
            SectionTitle(
                title = "额外扫描目录",
                subtitle = "${extraFolders.size} 个",
            )
        }

        if (extraFolders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    title = "没有额外扫描目录",
                    subtitle = "你可以在设置中添加更多资料库来源。",
                )
            }
        } else {
            items(extraFolders.size) { index ->
                FolderRow(folder = extraFolders[index])
            }
        }

        item {
            SectionTitle(
                title = "忽略目录",
                subtitle = "${ignoreFolders.size} 个",
            )
        }

        if (ignoreFolders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    title = "没有忽略目录",
                    subtitle = "设置忽略目录后，这里的列表会自动更新。",
                )
            }
        } else {
            items(ignoreFolders.size) { index ->
                FolderRow(folder = ignoreFolders[index])
            }
        }

        item {
            Spacer(Modifier.height(150.dp))
        }
    }
}

@Composable
private fun WeeklyStatsCard(
    weeklyStats: PlayStats?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            text = "本周听歌概览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (weeklyStats == null) {
            Text(
                text = "暂无统计数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "播放时长",
                    value = formatPlayDuration(weeklyStats.totalPlayedDuration),
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "播放次数",
                    value = "${weeklyStats.playEventCount}",
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = "不同歌曲",
                    value = "${weeklyStats.uniqueSongCount}",
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(Shapes.LargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 2,
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderRow(
    folder: ScanFolder,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Icon(
            imageVector = if (folder.isAccessible) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (folder.isAccessible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = folder.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = folder.pathPrefix ?: folder.uriString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (folder.isAccessible) "可用" else "失效",
            style = MaterialTheme.typography.labelMedium,
            color =
                if (folder.isAccessible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
    }
}

@Composable
private fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
) {
    val playlistUseCases = koinInject<PlaylistUseCases>()
    val albumIds =
        playlistUseCases
            .getPlaylistCoverAlbumIds(playlist.playlistId ?: 0L)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    val size =
        playlistUseCases
            .getSongSizeInPlaylist(playlist.playlistId ?: 0L)
            .collectAsStateWithLifecycle(initialValue = 0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        PlaylistCoverView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(Shapes.ExtraLargeCornerBasedShape),
            albumIds = albumIds.value,
        )
        Text(
            text = playlist.playlistName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${size.value} 首歌曲",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatPlayDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return when {
        hours > 0 -> "${hours}小时${minutes}分"
        minutes > 0 -> "${minutes}分钟"
        else -> "少于1分钟"
    }
}
