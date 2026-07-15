package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.android.awaitFrame
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.findPlayingIndex
import me.spica27.spicamusic.common.entity.getSentenceContent
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.materialSharedAxisYIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisYOut
import org.koin.compose.viewmodel.koinActivityViewModel

/** 加载中占位：保持歌词入口可见且可点击 */
private const val LOADING_PLACEHOLDER = "· · ·"

/**
 * mini 歌词：单行展示当前播放到的歌词，跟随播放进度自动切换。
 *
 * 直接嵌入播放器页面，点击后跳转全屏歌词页面；
 * 无歌词时显示占位文案（仍可点击进入全屏页搜索/切换歌词源）。
 */
@Composable
fun MiniLyric(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Activity 作用域共享实例：与全屏歌词页同源，
    // 全屏页内切换歌词源 / 调整偏移量后 mini 歌词同步生效
    val viewModel: LyricsViewModel = koinActivityViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 仅前台时驱动时间轮询，节省电量
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isAppInForeground =
        remember(lifecycleState) {
            lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        }

    // 当前播放时间（帧级更新）；行索引经 derivedStateOf 收敛，仅切行时触发重组
    val currentTimeState = remember { mutableLongStateOf(0L) }
    LaunchedEffect(isAppInForeground) {
        if (!isAppInForeground) return@LaunchedEffect
        while (true) {
            awaitFrame()
            currentTimeState.longValue = viewModel.getCurrentPositionMs()
        }
    }

    val lyrics = uiState.lyrics
    val offsetMs = uiState.lyricsOffsetMs
    // 先派生行索引（Int 等值比较，每帧计算零分配），
    // 行文本仅在索引变化时拼接，避免逐字歌词每帧 joinToString
    val playingIndex by remember(lyrics, offsetMs) {
        derivedStateOf {
            val list = lyrics ?: return@derivedStateOf -1
            if (list.isEmpty()) return@derivedStateOf -1
            val index = list.findPlayingIndex(currentTimeState.longValue + offsetMs)
            // 尚未唱到第一句时预告首句
            if (index == Int.MAX_VALUE) 0 else index
        }
    }
    val currentLineText =
        remember(lyrics, playingIndex) {
            lyrics?.getOrNull(playingIndex)?.let { item ->
                when (item) {
                    is LyricItem.NormalLyric -> item.content
                    is LyricItem.WordsLyric -> item.getSentenceContent()
                }
            }
        }

    // 加载中显示可见的占位符（mini 歌词是全屏歌词的唯一入口，槽位不能消失），
    // 无歌词显示占位文案
    val displayText =
        when {
            !lyrics.isNullOrEmpty() -> currentLineText.orEmpty()
            uiState.isLoading -> LOADING_PLACEHOLDER
            else -> null
        }

    Box(
        modifier =
            modifier
                .clip(Shapes.MediumCornerBasedShape)
                .clickable(onClickLabel = stringResource(R.string.tab_lrc)) { onClick.invoke() }
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                .heightIn(min = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = displayText,
            contentKey = { it ?: "placeholder" },
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
            label = "miniLyricLine",
        ) { line ->
            val isPlaceholder = line == null || line == LOADING_PLACEHOLDER
            Text(
                text = line ?: stringResource(R.string.no_lyrics),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isPlaceholder) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
