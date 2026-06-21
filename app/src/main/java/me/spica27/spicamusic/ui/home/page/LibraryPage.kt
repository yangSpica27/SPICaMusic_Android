package me.spica27.spicamusic.ui.home.page

import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.ImmutableList
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScene
import me.spica27.spicamusic.ui.playlistdetail.PlaylistDetailScene
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.clickHighlight
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

    val playlistsSummaryFormat = stringResource(R.string.library_summary_playlists)
    val scanFoldersSummaryFormat = stringResource(R.string.library_summary_scan_folders)
    val ignoreFoldersSummaryFormat = stringResource(R.string.library_summary_ignore_folders)
    val thisWeekSummaryFormat = stringResource(R.string.library_summary_this_week)
    val hoursMinutesFormat = stringResource(R.string.hours_minutes)
    val minutesFormat = stringResource(R.string.minutes)
    val lessThan1Min = stringResource(R.string.less_than_1_minute)
    val playlistsCountFormat = stringResource(R.string.playlists_count_format)
    val noPlaylistsCreatedText = stringResource(R.string.no_playlists_created)
    val emptyText = stringResource(R.string.empty)

    val summaryText =
        remember(playlists.size, extraFolders.size, ignoreFolders.size, weeklyStats) {
            buildString {
                append(playlistsSummaryFormat.format(playlists.size))
                if (extraFolders.isNotEmpty()) append(" · ${scanFoldersSummaryFormat.format(extraFolders.size)}")
                if (ignoreFolders.isNotEmpty()) append(" · ${ignoreFoldersSummaryFormat.format(ignoreFolders.size)}")
                weeklyStats?.let { stats ->
                    if (stats.totalPlayedDuration > 0) {
                        append(" · ${thisWeekSummaryFormat.format(formatPlayDuration(stats.totalPlayedDuration, hoursMinutesFormat, minutesFormat, lessThan1Min))}")
                    }
                }
            }
        }
    val density = LocalDensity.current
    val headerFollowDistancePx = with(density) { LayoutTokens.PageHeaderFollowDistance.toPx() }
    val playlistState = rememberLazyGridState()
    val folderListState = rememberLazyListState()
    val headerProgress by remember(headerFollowDistancePx) {
        derivedStateOf {
            when (selectTab) {
                LibraryPageTab.Playlist ->
                    headerFollowProgress(
                        playlistState,
                        headerFollowDistancePx / 2,
                    )

                LibraryPageTab.Folder ->
                    headerFollowProgress(
                        folderListState,
                        headerFollowDistancePx / 2,
                    )
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
            progress = headerProgress,
            summaryText = summaryText,
            tabs = ImmutableList.copyOf(tabs),
            selectTab = selectTab,
            onSelectTab = { selectTab = it },
            onScanClick = { path.push(ScannerScene()) },
            onCreatePlaylistClick = { path.push(PlaylistCreatorScene()) },
            extraText = { tab ->
                val playlistsCountFmt = playlistsCountFormat
                when (tab) {
                    LibraryPageTab.Playlist -> if (playlists.isNotEmpty()) playlistsCountFmt.format(playlists.size) else noPlaylistsCreatedText
                    LibraryPageTab.Folder -> {
                        val total = extraFolders.size + ignoreFolders.size
                        if (total > 0) playlistsCountFmt.format(total) else emptyText
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
                        gridState = playlistState,
                    )

                LibraryPageTab.Folder ->
                    FolderPage(
                        extraFolders = ImmutableList.copyOf(extraFolders),
                        ignoreFolders = ImmutableList.copyOf(ignoreFolders),
                        lazyListState = folderListState,
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
    progress: Float = 0f,
) {
    val heroAlpha = (1f - progress * 1.35f).coerceIn(0f, 1f)
    val backgroundColor =
        animateColorAsState(
            if (progress < 1f) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        )

    val tabPositions = remember { mutableStateMapOf<LibraryPageTab, Dp>() }
    val tabWidths = remember { mutableStateMapOf<LibraryPageTab, Dp>() }
    val tabHeight = remember { mutableStateMapOf<LibraryPageTab, Dp>() }
    val density = LocalDensity.current
    val indicatorOffset by animateDpAsState(
        targetValue = tabPositions.getOrElse(selectTab) { 0.dp },
        label = "",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = tabWidths.getOrElse(selectTab) { 0.dp },
        label = "",
    )
    val indicatorHeight by animateDpAsState(
        targetValue = tabHeight.getOrElse(selectTab) { 0.dp },
        label = "",
    )

    val indicatorColor =
        animateColorAsState(
            targetValue = if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "",
        ).value

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    backgroundColor.value,
                ).statusBarsPadding()
                .padding(
                    bottom = 12.dp,
                ),
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
                        .animateContentSize()
                        .followHeaderSection(progress)
                        .graphicsLayer {
                            translationY = -28f * progress
                            alpha = heroAlpha
                        },
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    ElevatedButton(
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(
                                topStartPercent = 50,
                                bottomStartPercent = 50,
                                topEndPercent = 12,
                                bottomEndPercent = 12,
                            ),
                    ) {
                        Icon(
                            Icons.Default.Scanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.ExtraSmall))
                        Text(stringResource(R.string.scan_music))
                    }
                    FilledTonalButton(
                        onClick = onCreatePlaylistClick,
                        modifier =
                            Modifier
                                .weight(1f),
                        shape =
                            RoundedCornerShape(
                                topEndPercent = 50,
                                bottomEndPercent = 50,
                                topStartPercent = 12,
                                bottomStartPercent = 12,
                            ),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.ExtraSmall))
                        Text(stringResource(R.string.create_playlist))
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(Shapes.ExtraLarge1CornerBasedShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(LayoutTokens.MusicTabContainerPadding)
                        .drawWithCache {
                            onDrawWithContent {
                                drawRoundRect(
                                    color = indicatorColor,
                                    topLeft = Offset(indicatorOffset.toPx(), 0f),
                                    size = Size(indicatorWidth.toPx(), indicatorHeight.toPx()),
                                    cornerRadius =
                                        CornerRadius(
                                            12.dp.toPx(),
                                            12.dp.toPx(),
                                        ),
                                )
                                drawContent()
                            }
                        },
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                tabs.forEach { tab ->
                    LibraryTabItem(
                        modifier =
                            Modifier
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInParent()
                                    val size = coordinates.size.toSize()
                                    tabPositions[tab] = with(density) { position.x.toDp() }
                                    tabWidths[tab] = with(density) { size.width.toDp() }
                                    tabHeight[tab] = with(density) { size.height.toDp() }
                                }.weight(1f),
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
    @StringRes val titleRes: Int,
) {
    Playlist(R.string.tab_playlists),
    Folder(R.string.tab_folders),
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
                ).clickable { onSelectTab(tab) }
                .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(tab.titleRes),
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
    gridState: LazyGridState,
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
        state = gridState,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            WeeklyStatsCard(weeklyStats = weeklyStats)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle(
                title = stringResource(R.string.my_playlists),
                subtitle = stringResource(R.string.playlists_count_format, playlists.size),
            )
        }

        if (playlists.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyLibraryCard(
                    title = stringResource(R.string.no_playlists_yet),
                    subtitle = stringResource(R.string.create_first_playlist_hint),
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
        item(
            span = { GridItemSpan(maxLineSpan) },
        ) {
            Spacer(Modifier.height(250.dp))
        }
    }
}

@Composable
private fun FolderPage(
    extraFolders: ImmutableList<ScanFolder>,
    ignoreFolders: ImmutableList<ScanFolder>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
) {
    val context = LocalContext.current
    val sourceViewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
    var pendingReauthFolderId by remember { mutableStateOf<Long?>(null) }

    val addExtraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { sourceViewModel.addExtraFolder(context, it) }
        }
    val addIgnoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { sourceViewModel.addIgnoreFolder(context, it) }
        }
    val reauthLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val id = pendingReauthFolderId
            if (uri != null && id != null) {
                sourceViewModel.reAuthorizeFolder(context, id, uri)
            }
            pendingReauthFolderId = null
        }

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
        state = lazyListState,
    ) {
        item {
            SectionTitle(
                title = stringResource(R.string.extra_scan_folders),
                subtitle = stringResource(R.string.playlists_count_format, extraFolders.size),
                onAddClick = { addExtraLauncher.launch(null) },
            )
        }

        if (extraFolders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    title = stringResource(R.string.no_extra_scan_folders),
                    subtitle = stringResource(R.string.add_extra_folder_hint),
                )
            }
        } else {
            items(
                items = extraFolders,
                key = { "extra_${it.id}" },
            ) { folder ->
                FolderRow(
                    folder = folder,
                    onRemove = { sourceViewModel.removeFolder(context, folder) },
                    onReAuthorize = {
                        pendingReauthFolderId = folder.id
                        reauthLauncher.launch(null)
                    },
                )
            }
        }

        item {
            SectionTitle(
                title = stringResource(R.string.ignore_folders),
                subtitle = stringResource(R.string.playlists_count_format, ignoreFolders.size),
                onAddClick = { addIgnoreLauncher.launch(null) },
            )
        }

        if (ignoreFolders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    title = stringResource(R.string.library_no_ignore_folders),
                    subtitle = stringResource(R.string.add_ignore_folder_hint),
                )
            }
        } else {
            items(
                items = ignoreFolders,
                key = { "ignore_${it.id}" },
            ) { folder ->
                FolderRow(
                    folder = folder,
                    onRemove = { sourceViewModel.removeFolder(context, folder) },
                )
            }
        }

        item {
            Spacer(Modifier.height(250.dp))
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
                .clickHighlight {
                }.background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.weekly_listening_overview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (weeklyStats == null) {
            Text(
                text = stringResource(R.string.no_stats_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                val hoursMinutesFmt = stringResource(R.string.hours_minutes)
                val minutesFmt = stringResource(R.string.minutes)
                val lessThan1MinText = stringResource(R.string.less_than_1_minute)

                StatPill(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.play_duration),
                    value = formatPlayDuration(weeklyStats.totalPlayedDuration, hoursMinutesFmt, minutesFmt, lessThan1MinText),
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.play_count),
                    value = "${weeklyStats.playEventCount}",
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.unique_songs),
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
    onAddClick: (() -> Unit)? = null,
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
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onAddClick != null) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_folder),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
            color = MaterialTheme.colorScheme.onSurface,
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
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onReAuthorize: (() -> Unit)? = null,
) {
    val reauthorizeText = stringResource(R.string.reauthorize)
    val accessibleText = stringResource(R.string.accessible)
    val inaccessibleText = stringResource(R.string.inaccessible)
    val removeFolderText = stringResource(R.string.remove_folder)

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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = folder.pathPrefix ?: folder.uriString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!folder.isAccessible && onReAuthorize != null) {
            Text(
                text = reauthorizeText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .clip(Shapes.LargeCornerBasedShape)
                        .clickable(onClick = onReAuthorize)
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
            )
        } else {
            Text(
                text = if (folder.isAccessible) accessibleText else inaccessibleText,
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (folder.isAccessible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = removeFolderText,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
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
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.songs_count, size.value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatPlayDuration(
    durationMs: Long,
    hoursMinutesFormat: String,
    minutesFormat: String,
    lessThan1Min: String,
): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    return when {
        hours > 0 -> hoursMinutesFormat.format(hours, minutes)
        minutes > 0 -> minutesFormat.format(minutes)
        else -> lessThan1Min
    }
}
