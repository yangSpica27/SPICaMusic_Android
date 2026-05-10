package me.spica27.spicamusic.ui.player.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.android.awaitFrame
import me.spica27.spicamusic.ui.player.LyricsViewModel
import me.spica27.spicamusic.ui.widget.FloatingLyricsToolbar
import me.spica27.spicamusic.ui.widget.LyricsSwitcherSheet
import me.spica27.spicamusic.ui.widget.LyricsUI
import org.koin.androidx.compose.koinViewModel

/**
 * 全屏歌词页面
 *
 * 功能：
 * - 自动搜索歌词，优先使用缓存
 * - 歌词偏移量调节（持久化到数据库）
 * - 多歌词源切换（通过预览面板选择后缓存）
 */
@Composable
fun FullScreenLyricsPage(modifier: Modifier = Modifier) {
    val viewModel: LyricsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 歌词切换面板的纯 UI 状态（不需要持久化）
    var showSwitcherSheet by remember { mutableStateOf(false) }

    // 监听应用生命周期状态，仅前台时更新播放进度
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isAppInForeground =
        remember(lifecycleState) {
            lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        }

    // 当前播放时间（帧级更新，保留在 Composable 中因为依赖 awaitFrame）
    var currentTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isAppInForeground) {
        if (!isAppInForeground) return@LaunchedEffect
        while (isAppInForeground) {
            awaitFrame()
            currentTime = viewModel.getCurrentPositionMs()
        }
    }

    // 歌词切换面板
    if (showSwitcherSheet && uiState.allLyricSources.isNotEmpty()) {
        LyricsSwitcherSheet(
            lyricSources = uiState.allLyricSources,
            parsedLyrics = uiState.allParsedLyrics,
            currentTime = currentTime + uiState.lyricsOffsetMs,
            initialPage = uiState.currentSourceIndex,
            onConfirm = { selectedIndex ->
                showSwitcherSheet = false
                viewModel.selectAndSaveLyricSource(selectedIndex)
            },
            onDismiss = { showSwitcherSheet = false },
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            uiState.lyrics != null -> {
                LyricsUI(
                    modifier = Modifier.fillMaxSize(),
                    lyric = uiState.lyrics!!,
                    currentTime = currentTime + uiState.lyricsOffsetMs,
                    onSeekToTime = { posMs ->
                        viewModel.seekTo(posMs - uiState.lyricsOffsetMs)
                    },
                )
            }
            else -> {
                Text(
                    text = "等待播放",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 浮动工具栏（右下角）
        if (uiState.lyrics != null || uiState.errorMessage != null) {
            FloatingLyricsToolbar(
                offsetMs = uiState.lyricsOffsetMs,
                onOffsetChange = { viewModel.updateOffset(it) },
                onOpenLyricsSwitcher = { showSwitcherSheet = true },
                hasMultipleSources = uiState.allLyricSources.size > 1,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp),
            )
        }
    }
}
