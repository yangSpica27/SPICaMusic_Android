package me.spica27.spicamusic.ui.library

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 听歌统计页面
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun ListeningStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: ListeningStatsViewModel = koinViewModel(),
) {
    val backStack = LocalNavBackStack.current
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val allTimeStats by viewModel.allTimeStats.collectAsStateWithLifecycle()
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = rememberHazeState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "听歌统计",
                largeTitle = "听歌统计",
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
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
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
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                ),
        ) {
            // 本周概览
            item {
                SectionHeader(title = "本周概览")
            }
            item {
                StatsCard(
                    stats = weeklyStats,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 累计统计
            item {
                SectionHeader(title = "累计统计")
            }
            item {
                StatsCard(
                    stats = allTimeStats,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // 最常听的歌曲
            if (topSongs.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    SectionHeader(title = "最常听的歌曲")
                }
                itemsIndexed(topSongs, key = { _, item -> item.songId }) { index, item ->
                    TopSongRow(
                        rank = index + 1,
                        item = item,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.W600,
        modifier =
            Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatsCard(
    stats: PlayStats?,
    modifier: Modifier = Modifier,
) {
    Card(
        cornerRadius = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                value = stats?.let { statsFormatDuration(it.totalPlayedDuration) } ?: "—",
                label = "听歌时长",
            )
            StatItem(
                value = stats?.playEventCount?.toString() ?: "—",
                label = "播放次数",
            )
            StatItem(
                value = stats?.uniqueSongCount?.toString() ?: "—",
                label = "听过歌曲",
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}

@Composable
private fun TopSongRow(
    rank: Int,
    item: TopSongDisplayItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 排名
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center,
        ) {
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

        // 时长 + 次数
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = statsFormatDuration(item.totalDuration),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                fontWeight = FontWeight.W500,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.playCount}次",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                fontSize = 11.sp,
            )
        }
    }
}

private fun statsFormatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}
