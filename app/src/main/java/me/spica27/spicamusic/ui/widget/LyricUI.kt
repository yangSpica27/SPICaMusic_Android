package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.findPlayingIndex
import me.spica27.spicamusic.common.entity.toNormal
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.*

@Composable
fun LryricUI(
    modifier: Modifier = Modifier,
    lyric: List<LyricItem>,
    currentTime: Long,
    onSeekToTime: (Long) -> Unit = {},
) {
    val lyricLines =
        lyric
            .sortedBy { it.time }
            .mapNotNull { it.toNormal() }

    if (lyricLines.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "暂无歌词",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }

    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    val playingIndex by remember(currentTime, lyricLines) {
        derivedStateOf { lyricLines.findPlayingIndex(currentTime) }
    }

    val previewIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset) + (layoutInfo.viewportSize.height / 2)
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                playingIndex
            } else {
                visible
                    .minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2
                        return@minByOrNull kotlin.math.abs(itemCenter - viewportCenter)
                    }?.index ?: playingIndex
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collectLatest { inProgress ->
                if (inProgress && !isAutoScrolling) {
                    showSeekOverlay = true
                } else if (!inProgress) {
                    if (!isAutoScrolling) {
                        delay(450)
                    }
                    showSeekOverlay = false
                    isAutoScrolling = false
                }
            }
    }

    LaunchedEffect(playingIndex, showSeekOverlay, lyricLines) {
        val target = playingIndex
        if (showSeekOverlay || target == Int.MAX_VALUE || target !in lyricLines.indices) {
            return@LaunchedEffect
        }
        isAutoScrolling = true
        try {
            lazyListState.animateScrollToItem((target - 2).coerceAtLeast(0))
        } finally {
            isAutoScrolling = false
        }
    }

    val highlightedIndex = if (showSeekOverlay) previewIndex else playingIndex
    val gradientColors =
        listOf(
            MiuixTheme.colorScheme.surface,
            Color.Transparent,
        )

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 96.dp, horizontal = 24.dp),
            state = lazyListState,
        ) {
            itemsIndexed(
                items = lyricLines,
                key = { _, line -> line.key },
            ) { index, line ->
                val distanceFromActive =
                    if (highlightedIndex == Int.MAX_VALUE) {
                        Int.MAX_VALUE
                    } else {
                        kotlin.math.abs(index - highlightedIndex)
                    }
                val emphasis = (1f - distanceFromActive * 0.18f).coerceIn(0.35f, 1f)
                val scale by animateFloatAsState(
                    targetValue = if (distanceFromActive == 0) 1.12f else 1f,
                    label = "lyricScale",
                )
                val alpha by animateFloatAsState(
                    targetValue = emphasis,
                    label = "lyricAlpha",
                )

                LyricLine(
                    lyric = line,
                    isActive = distanceFromActive == 0,
                    alpha = alpha,
                    scale = scale,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(gradientColors)),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(gradientColors.reversed())),
        )

        if (showSeekOverlay) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = showSeekOverlay && previewIndex in lyricLines.indices,
        ) {
            val previewLine = lyricLines.getOrNull(previewIndex)
            if (previewLine != null) {
                SeekPreview(
                    timeText = formatLyricTime(previewLine.time),
                    onSeek = { onSeekToTime(previewLine.time) },
                )
            }
        }
    }
}

@Composable
private fun LyricLine(
    lyric: LyricItem.NormalLyric,
    isActive: Boolean,
    alpha: Float,
    scale: Float,
) {
    val activeBackground = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
    val inactiveTextColor = MiuixTheme.colorScheme.onSurface.copy(alpha = alpha)
    val activeTextColor = MiuixTheme.colorScheme.onSurface

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }.background(if (isActive) activeBackground else Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = lyric.content.ifBlank { " · · · " },
            style = MiuixTheme.textStyles.title2,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) activeTextColor else inactiveTextColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!lyric.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = lyric.translation!!,
                style = MiuixTheme.textStyles.body2,
                color = if (isActive) activeTextColor.copy(alpha = 0.85f) else inactiveTextColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeekPreview(
    timeText: String,
    onSeek: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = timeText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        IconButton(
            onClick = onSeek,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(48.dp))
                    .background(MiuixTheme.colorScheme.primary),
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "定位到此行",
                tint = MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun formatLyricTime(time: Long): String {
    val totalSeconds = (time / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
