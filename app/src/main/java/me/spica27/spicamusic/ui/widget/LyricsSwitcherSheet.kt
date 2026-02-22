package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.LyricItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 歌词切换选择面板
 *
 * 展示多个歌词源的实时预览，用户可左右滑动比较后确认选择。
 *
 * @param lyricSources 所有歌词源列表
 * @param parsedLyrics 各歌词源对应的已解析歌词列表（与 lyricSources 一一对应）
 * @param currentTime 当前播放时间（毫秒），用于歌词高亮预览
 * @param initialPage 初始展示页面索引
 * @param onConfirm 用户确认选择回调，参数为选中的索引
 * @param onDismiss 关闭面板回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSwitcherSheet(
    lyricSources: List<SongLyrics>,
    parsedLyrics: List<List<LyricItem>>,
    currentTime: Long,
    initialPage: Int = 0,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState =
        rememberPagerState(
            initialPage = initialPage.coerceIn(0, (lyricSources.size - 1).coerceAtLeast(0)),
            pageCount = { lyricSources.size },
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MiuixTheme.colorScheme.background,
        contentColor = MiuixTheme.colorScheme.onBackground,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(520.dp),
        ) {
            // 顶部标题栏
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.choose_lyrics),
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lyrics_preview_instruction),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }

                // 确认按钮
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary)
                            .clickable { onConfirm(pagerState.currentPage) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "确认选择",
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // 页面指示器 + 歌词源信息
            if (lyricSources.isNotEmpty()) {
                SourceInfoBar(
                    sources = lyricSources,
                    currentPage = pagerState.currentPage,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 歌词预览 Pager
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                key = { it },
            ) { page ->
                if (page < parsedLyrics.size && parsedLyrics[page].isNotEmpty()) {
                    LyricsUI(
                        modifier = Modifier.fillMaxSize(),
                        lyric = parsedLyrics[page],
                        currentTime = currentTime,
                    )
                } else {
                    // 歌词解析失败
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_parse_failed),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // 底部页面圆点指示器
            if (lyricSources.size > 1) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(lyricSources.size) { index ->
                        val isSelected = index == pagerState.currentPage
                        val color by animateColorAsState(
                            targetValue =
                                if (isSelected) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                },
                            label = "dotColor",
                        )
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(color),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 歌词源信息栏
 */
@Composable
private fun SourceInfoBar(
    sources: List<SongLyrics>,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val source = sources.getOrNull(currentPage) ?: return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号标签
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.pager_format, currentPage + 1, sources.size),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 歌曲信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = source.artist,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
        }
    }
}
