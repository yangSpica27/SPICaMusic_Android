package me.spica27.spicamusic.ui.player

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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.common.collect.ImmutableList
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.widget.FloatingLyricsToolbar
import me.spica27.spicamusic.ui.widget.LyricsDisplayMode
import me.spica27.spicamusic.ui.widget.LyricsSourceSheet
import me.spica27.spicamusic.ui.widget.LyricsUI
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 歌词面板
 *
 * 通用歌词展示组件，通过 [displayMode] 适配不同容器：
 * 全屏页面（[LyricsDisplayMode.Fullscreen]）与封面卡片等小尺寸场景（[LyricsDisplayMode.Compact]）
 *
 * 功能：
 * - 自动搜索歌词，优先使用缓存
 * - 歌词偏移量调节（持久化到数据库）
 * - 多歌词源切换（通过预览面板选择后缓存）
 */
@Composable
fun LyricsPanel(
    modifier: Modifier = Modifier,
    displayMode: LyricsDisplayMode = LyricsDisplayMode.Fullscreen,
) {
    // Activity 作用域共享实例：与 mini 歌词同源，
    // 此处切换歌词源 / 调整偏移量会同步反映到 mini 歌词
    val viewModel: LyricsViewModel = koinActivityViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 歌词切换面板的纯 UI 状态（不需要持久化）
    var showSwitcherSheet by remember { mutableStateOf(false) }

    // 当前播放时间（帧级更新，保留在 Composable 中因为依赖逐帧对齐）
    // 首帧直接使用播放器的真实位置，避免先以 0ms 完成一次错误的歌词定位，
    // 随后又把实际当前行当成普通 index 切换从视口底部动画进入。
    var currentTime by remember { mutableLongStateOf(viewModel.getCurrentPositionMs()) }
    // 仅前台时更新播放进度。repeatOnLifecycle(STARTED) 切后台真正取消、回前台重启；
    // withFrameNanos 走 Compose 可暂停帧时钟。（详见 MiniLyric 同处注释。）
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                withFrameNanos { }
                currentTime = viewModel.getCurrentPositionMs()
            }
        }
    }

    // 歌词切换面板
    if (showSwitcherSheet) {
        LyricsSourceSheet(
            embedded = uiState.embeddedSource,
            local = uiState.localSource,
            online = uiState.onlineSources,
            onlineLoading = uiState.onlineLoading,
            currentSourceType = uiState.currentSourceType,
            currentRawText = uiState.displayedRawText,
            currentTime = currentTime + uiState.lyricsOffsetMs,
            onSelect = { source ->
                showSwitcherSheet = false
                viewModel.selectSource(source)
            },
            onImportLocalFile = { uri -> viewModel.importLocalFile(uri) },
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
            uiState.displayed != null -> {
                val displayed = uiState.displayed!!
                // 面板每帧随 currentTime 重组，若在此每帧 copyOf 会全量拷贝整份歌词。
                // 按 items 身份缓存：只有歌词本身变化才重建。
                val lyricList = remember(displayed) { ImmutableList.copyOf(displayed.items) }
                LyricsUI(
                    modifier = Modifier.fillMaxSize(),
                    lyric = lyricList,
                    currentTime = currentTime + uiState.lyricsOffsetMs,
                    displayMode = displayMode,
                    isSynced = displayed.isSynced,
                    onSeekToTime = { posMs ->
                        viewModel.seekTo(posMs - uiState.lyricsOffsetMs)
                    },
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.waiting_to_play),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 浮动工具栏（右下角）：切换入口常驻，保证无内嵌/在线时仍可进入选择本地文件
        FloatingLyricsToolbar(
            offsetMs = uiState.lyricsOffsetMs,
            onOffsetChange = { viewModel.updateOffset(it) },
            onOpenLyricsSwitcher = {
                viewModel.openPanel()
                showSwitcherSheet = true
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = if (displayMode == LyricsDisplayMode.Compact) 12.dp else 16.dp,
                        bottom = if (displayMode == LyricsDisplayMode.Compact) 12.dp else 24.dp,
                    ),
        )
    }
}
