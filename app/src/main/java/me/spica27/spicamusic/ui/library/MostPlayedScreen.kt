package me.spica27.spicamusic.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.utils.navSharedBounds
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 最常播放页面
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MostPlayedScreen(
    modifier: Modifier = Modifier,
    viewModel: MostPlayedViewModel = koinViewModel(),
) {
    val backStack = LocalNavBackStack.current
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberHazeState()

    // 消费 snackbar 消息（简单展示后清除）
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier =
            modifier
                .navSharedBounds(Screen.MostPlayed)
                .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.most_played_title),
                largeTitle = stringResource(R.string.most_played_title),
                scrollBehavior = scrollBehavior,
                color = Color.Transparent,
                modifier =
                    Modifier.hazeEffect(
                        state = hazeState,
                        style =
                            HazeMaterials.ultraThick(
                                MiuixTheme.colorScheme.surface,
                            ),
                    ) {
                        progressive =
                            HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f,
                            )
                    },
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic()
                        .overScrollVertical(),
                contentPadding =
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 88.dp,
                    ),
            ) {
                // 时间范围选择器
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(MostPlayedRange.entries) { range ->
                            RangeChip(
                                label = stringResource(range.labelRes),
                                selected = range == selectedRange,
                                onClick = { viewModel.selectRange(range) },
                            )
                        }
                    }
                }

                // 操作栏：播放全部 + 保存为歌单
                if (songs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { viewModel.playAll() },
                                colors =
                                    ButtonColors(
                                        MiuixTheme.colorScheme.primary,
                                        MiuixTheme.colorScheme.primary,
                                    ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.play_all), color = MiuixTheme.colorScheme.onPrimary)
                            }
                            Button(
                                onClick = { viewModel.saveAsPlaylist() },
                                colors =
                                    ButtonColors(
                                        MiuixTheme.colorScheme.secondaryContainer,
                                        MiuixTheme.colorScheme.secondaryContainer,
                                    ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.save_as_playlist), color = MiuixTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }

                // 歌曲列表
                if (!isLoading) {
                    itemsIndexed(songs, key = { _, item -> item.songId }) { index, item ->
                        MostPlayedSongRow(
                            rank = index + 1,
                            item = item,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { viewModel.playSong(item) },
                        )
                    }
                    if (songs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "暂无播放记录",
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                    style = MiuixTheme.textStyles.body1,
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "加载中…",
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                                style = MiuixTheme.textStyles.body1,
                            )
                        }
                    }
                }
            }

            // Snackbar 提示
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
            ) {
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = Shapes.ExtraLarge1CornerBasedShape,
                    color = MiuixTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = snackbarMessage ?: "",
                        color = MiuixTheme.colorScheme.surface,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.secondaryContainer
    val textColor = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSecondaryContainer

    Surface(
        color = backgroundColor,
        shape = Shapes.ExtraLargeCornerBasedShape,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MiuixTheme.textStyles.body2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MostPlayedSongRow(
    rank: Int,
    item: TopSongDisplayItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent,
        shape = Shapes.MediumCornerBasedShape,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 排名
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    style = MiuixTheme.textStyles.body2,
                    color =
                        if (rank <= 3) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceContainerVariant
                        },
                    fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal,
                )
            }

            // 封面
            val coverUri = item.song?.getCoverUri()
            AudioCover(
                uri = coverUri,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(Shapes.SmallCornerBasedShape),
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(Shapes.SmallCornerBasedShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.song?.displayName ?: "未知歌曲",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.song?.artist ?: "未知艺术家",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 播放时长 + 次数
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = mostPlayedFormatDuration(item.totalDuration),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCount(item.playCount)}次",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun mostPlayedFormatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

private fun formatCount(count: Long): String =
    when {
        count >= 1_000_000L -> String.format("%.1fm", count / 1_000_000.0).trimEnd('0').trimEnd('.')
        count >= 1_000L -> String.format("%.1fk", count / 1_000.0).trimEnd('0').trimEnd('.')
        else -> count.toString()
    }
