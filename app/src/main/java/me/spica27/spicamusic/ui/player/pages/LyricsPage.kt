package me.spica27.spicamusic.ui.player.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.widget.FloatingLyricsToolbar
import me.spica27.spicamusic.ui.widget.LyricsUI
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全屏歌词页面
 */
@Composable
fun FullScreenLyricsPage(modifier: Modifier = Modifier) {
    val playerViewModel = koinActivityViewModel<PlayerViewModel>()
    val apiClient: ApiClient = koinInject()

    // 监听应用生命周期状态
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isAppInForeground =
        remember(lifecycleState) {
            lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        }

    // 状态管理
    var currentTime by remember { mutableLongStateOf(0L) }
    var lyric by remember { mutableStateOf<List<LyricItem>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 歌词偏移量（毫秒）
    var lyricsOffsetMs by remember { mutableLongStateOf(0L) }

    // 多歌词源管理
    var allLyricSources by remember { mutableStateOf<List<SongLyrics>>(emptyList()) }
    var currentSourceIndex by remember { mutableIntStateOf(0) }

    // 观察当前歌曲变化
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

    // 歌曲变化时搜索所有歌词源
    LaunchedEffect(currentMediaItem?.mediaId) {
        val mediaItem = currentMediaItem
        if (mediaItem == null) {
            lyric = null
            errorMessage = null
            allLyricSources = emptyList()
            currentSourceIndex = 0
            lyricsOffsetMs = 0L
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        lyricsOffsetMs = 0L

        try {
            val title = mediaItem.mediaMetadata.title?.toString() ?: ""

            if (title.isBlank()) {
                errorMessage = "歌曲信息缺失"
                lyric = null
                allLyricSources = emptyList()
                return@LaunchedEffect
            }

            // 搜索所有歌词源
            val results = apiClient.searchAllLyrics(title)
            allLyricSources = results
            currentSourceIndex = 0

            if (results.isEmpty()) {
                errorMessage = "暂无歌词"
                lyric = null
            } else {
                lyric = parseLyrics(results.first().lyrics)
                if (lyric.isNullOrEmpty()) {
                    errorMessage = "歌词解析失败"
                    lyric = null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lyrics")
            errorMessage = "加载歌词失败: ${e.message ?: "未知错误"}"
            lyric = null
            allLyricSources = emptyList()
        } finally {
            isLoading = false
        }
    }

    // 切换歌词源时重新解析
    LaunchedEffect(currentSourceIndex) {
        if (allLyricSources.isEmpty()) return@LaunchedEffect
        val index = currentSourceIndex.coerceIn(0, allLyricSources.lastIndex)
        val source = allLyricSources[index]

        Timber.d("切换到歌词源 ${index + 1}/${allLyricSources.size}: ${source.name} - ${source.artist}")

        val parsed = parseLyrics(source.lyrics)
        if (parsed.isNullOrEmpty()) {
            errorMessage = "歌词解析失败"
            lyric = null
        } else {
            errorMessage = null
            lyric = parsed
        }
    }

    // 持续更新播放时间（仅前台时更新，节省电量）
    LaunchedEffect(isAppInForeground) {
        if (!isAppInForeground) return@LaunchedEffect

        while (isAppInForeground) {
            awaitFrame()
            currentTime = playerViewModel.getCurrentPositionMs()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage!!,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            lyric != null -> {
                // 歌词显示（应用偏移量）
                LyricsUI(
                    modifier = Modifier.fillMaxSize(),
                    lyric = lyric!!,
                    currentTime = currentTime + lyricsOffsetMs,
                    onSeekToTime = {
                        playerViewModel.seekTo(it - lyricsOffsetMs)
                    },
                )
            }
            else -> {
                Text(
                    text = "等待播放",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 浮动工具栏（右下角）
        if (lyric != null || errorMessage != null) {
            FloatingLyricsToolbar(
                offsetMs = lyricsOffsetMs,
                onOffsetChange = { lyricsOffsetMs = it },
                onSwitchLyrics = {
                    if (allLyricSources.isNotEmpty()) {
                        currentSourceIndex = (currentSourceIndex + 1) % allLyricSources.size
                    }
                },
                currentLyricIndex = currentSourceIndex,
                totalLyricSources = allLyricSources.size,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * 解析歌词文本为 LyricItem 列表
 */
private fun parseLyrics(lyricsText: String): List<LyricItem>? {
    if (lyricsText.isBlank()) return null

    Timber.d("歌词前100字符: ${lyricsText.take(100)}")

    // 检测 YRC 格式（包含字级时间戳）
    val isYrcFormat =
        lyricsText.contains("](") &&
            lyricsText.contains("[") &&
            lyricsText.matches(Regex(".*\\[\\d+.*\\]\\(\\d+.*\\).*"))

    Timber.d("检测到歌词格式: ${if (isYrcFormat) "YRC" else "LRC"}")

    return if (isYrcFormat) {
        try {
            val yrcLines = YrcParser.parse(lyricsText)
            LrcParser.parse(YrcParser.toLrc(yrcLines))
        } catch (e: Exception) {
            Timber.w(e, "YRC parse failed, fallback to LRC")
            LrcParser.parse(lyricsText)
        }
    } else {
        LrcParser.parse(lyricsText)
    }
}
