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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.widget.FloatingLyricsToolbar
import me.spica27.spicamusic.ui.widget.LyricsSwitcherSheet
import me.spica27.spicamusic.ui.widget.LyricsUI
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val playerViewModel = koinActivityViewModel<PlayerViewModel>()
    val apiClient: ApiClient = koinInject()
    val extraInfoDao: ExtraInfoDao = koinInject()

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

    // 当前歌曲的 mediaId（MediaStore ID，用于缓存 key）
    var currentMediaStoreId by remember { mutableLongStateOf(0L) }

    // 多歌词源管理
    var allLyricSources by remember { mutableStateOf<List<SongLyrics>>(emptyList()) }
    var allParsedLyrics by remember { mutableStateOf<List<List<LyricItem>>>(emptyList()) }
    var currentSourceIndex by remember { mutableIntStateOf(0) }

    // 歌词切换面板状态
    var showSwitcherSheet by remember { mutableStateOf(false) }

    // 观察当前歌曲变化
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

    // 歌曲变化时加载歌词（优先缓存）
    LaunchedEffect(currentMediaItem?.mediaId) {
        val mediaItem = currentMediaItem
        if (mediaItem == null) {
            lyric = null
            errorMessage = null
            allLyricSources = emptyList()
            allParsedLyrics = emptyList()
            currentSourceIndex = 0
            lyricsOffsetMs = 0L
            currentMediaStoreId = 0L
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        val title = mediaItem.mediaMetadata.title?.toString() ?: ""
        val mediaStoreId = mediaItem.mediaId.toLongOrNull() ?: 0L
        currentMediaStoreId = mediaStoreId

        try {
            if (title.isBlank()) {
                errorMessage = "歌曲信息缺失"
                lyric = null
                allLyricSources = emptyList()
                allParsedLyrics = emptyList()
                return@LaunchedEffect
            }

            // 1. 尝试从缓存读取歌词和偏移量
            val cached =
                withContext(Dispatchers.IO) {
                    extraInfoDao.getLyricWithMediaId(mediaStoreId)
                }

            if (cached != null && cached.lyrics.isNotBlank()) {
                Timber.d("使用缓存歌词: mediaId=$mediaStoreId, source=${cached.lyricSourceName}")
                lyricsOffsetMs = cached.delay
                lyric = parseLyrics(cached.lyrics)
                if (lyric.isNullOrEmpty()) {
                    errorMessage = "歌词解析失败"
                    lyric = null
                }
            } else {
                lyricsOffsetMs = 0L
            }

            // 2. 同时搜索所有歌词源（用于切换面板）
            val results = apiClient.searchAllLyrics(title)
            allLyricSources = results

            // 预解析所有歌词源
            allParsedLyrics =
                results.map { source ->
                    parseLyrics(source.lyrics) ?: emptyList()
                }

            // 如果没有缓存，使用第一个结果
            if (cached == null || cached.lyrics.isBlank()) {
                lyricsOffsetMs = 0L
                currentSourceIndex = 0
                if (results.isEmpty()) {
                    errorMessage = "暂无歌词"
                    lyric = null
                } else {
                    lyric = allParsedLyrics.firstOrNull()?.ifEmpty { null }
                    if (lyric == null) {
                        errorMessage = "歌词解析失败"
                    }
                }
            } else {
                // 缓存存在时，尝试匹配当前使用的歌词源索引
                currentSourceIndex =
                    results
                        .indexOfFirst {
                            "${it.artist} - ${it.name}" == cached.lyricSourceName
                        }.coerceAtLeast(0)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lyrics")
            // 如果网络失败但有缓存，保持缓存
            if (lyric == null) {
                errorMessage = "加载歌词失败: ${e.message ?: "未知错误"}"
            }
            allLyricSources = emptyList()
            allParsedLyrics = emptyList()
        } finally {
            isLoading = false
        }
    }

    // 持续更新播放时间（仅前台时更新）
    LaunchedEffect(isAppInForeground) {
        if (!isAppInForeground) return@LaunchedEffect

        while (isAppInForeground) {
            awaitFrame()
            currentTime = playerViewModel.getCurrentPositionMs()
        }
    }

    // 偏移量变化时持久化到数据库
    LaunchedEffect(lyricsOffsetMs, currentMediaStoreId) {
        if (currentMediaStoreId <= 0L) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val existing = extraInfoDao.getLyricWithMediaId(currentMediaStoreId)
            if (existing != null) {
                extraInfoDao.updateDelay(currentMediaStoreId, lyricsOffsetMs)
            }
            // 如果记录不存在，偏移量将在选择歌词时一并保存
        }
    }

    // 用于处理确认选择后的缓存保存
    var pendingSaveIndex by remember { mutableIntStateOf(-1) }

    // 歌词切换面板
    if (showSwitcherSheet && allLyricSources.isNotEmpty()) {
        LyricsSwitcherSheet(
            lyricSources = allLyricSources,
            parsedLyrics = allParsedLyrics,
            currentTime = currentTime + lyricsOffsetMs,
            initialPage = currentSourceIndex,
            onConfirm = { selectedIndex ->
                showSwitcherSheet = false
                currentSourceIndex = selectedIndex

                // 应用选中的歌词
                val selectedParsed = allParsedLyrics.getOrNull(selectedIndex)
                if (!selectedParsed.isNullOrEmpty()) {
                    lyric = selectedParsed
                    errorMessage = null
                }

                // 触发缓存保存
                pendingSaveIndex = selectedIndex
            },
            onDismiss = { showSwitcherSheet = false },
        )
    }

    LaunchedEffect(pendingSaveIndex) {
        if (pendingSaveIndex < 0) return@LaunchedEffect
        val index = pendingSaveIndex
        pendingSaveIndex = -1

        val source = allLyricSources.getOrNull(index) ?: return@LaunchedEffect
        val sourceName = "${source.artist} - ${source.name}"
        val lyricsText = source.lyrics

        withContext(Dispatchers.IO) {
            val existing = extraInfoDao.getLyricWithMediaId(currentMediaStoreId)
            if (existing != null) {
                extraInfoDao.updateLyricsAndSource(currentMediaStoreId, lyricsText, sourceName)
            } else {
                extraInfoDao.insertLyric(
                    ExtraInfoEntity(
                        mediaId = currentMediaStoreId,
                        lyrics = lyricsText,
                        lyricSourceName = sourceName,
                        delay = lyricsOffsetMs,
                    ),
                )
            }
            Timber.d("已缓存歌词: mediaId=$currentMediaStoreId, source=$sourceName")
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
                onOpenLyricsSwitcher = { showSwitcherSheet = true },
                hasMultipleSources = allLyricSources.size > 1,
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

    // 检测 YRC 格式（包含字级时间戳）
    val isYrcFormat =
        lyricsText.contains("](") &&
            lyricsText.contains("[") &&
            lyricsText.matches(Regex(".*\\[\\d+.*\\]\\(\\d+.*\\).*"))

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
