package me.spica27.spicamusic.ui.player.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.ui.player.PlayerViewModel
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

    // 观察当前歌曲变化
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

    // 歌曲变化时自动搜索歌词
    LaunchedEffect(currentMediaItem?.mediaId) {
        val mediaItem = currentMediaItem
        if (mediaItem == null) {
            lyric = null
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            // 从 MediaMetadata 提取歌曲信息
            val title = mediaItem.mediaMetadata.title?.toString() ?: ""
            val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""

            if (title.isBlank()) {
                errorMessage = "歌曲信息缺失"
                lyric = null
                return@LaunchedEffect
            }

            // 调用 EAPI 获取歌词（带自动 YRC/LRC 回退）
            val extraInfo = apiClient.fetchExtInfo(title, artist)

            Timber.d("获取到的歌词信息: extraInfo=$extraInfo")
            Timber.d("歌词内容长度: ${extraInfo?.lyrics?.length}")
            Timber.d("歌词前100字符: ${extraInfo?.lyrics?.take(100)}")

            if (extraInfo?.lyrics.isNullOrBlank()) {
                errorMessage = "暂无歌词"
                lyric = null
            } else {
                val lyricsText = extraInfo!!.lyrics!!

                Timber.d("歌词全文: $lyricsText")

                // 检测 YRC 格式（包含字级时间戳）
                val isYrcFormat =
                    lyricsText.contains("](") &&
                        lyricsText.contains("[") &&
                        lyricsText.matches(Regex(".*\\[\\d+.*\\]\\(\\d+.*\\).*"))

                Timber.d("检测到歌词格式: ${if (isYrcFormat) "YRC" else "LRC"}")

                lyric =
                    if (isYrcFormat) {
                        // YRC 格式 - 使用新解析器转换为 LRC
                        try {
                            val yrcLines = YrcParser.parse(lyricsText)
                            LrcParser.parse(YrcParser.toLrc(yrcLines))
                        } catch (e: Exception) {
                            // YRC 解析失败，回退到标准 LRC
                            Timber.w(e, "YRC parse failed, fallback to LRC")
                            LrcParser.parse(lyricsText)
                        }
                    } else {
                        // 标准 LRC 格式
                        LrcParser.parse(lyricsText)
                    }

                Timber.d("解析后的歌词条数: ${lyric?.size}")

                if (lyric.isNullOrEmpty()) {
                    errorMessage = "歌词解析失败"
                    lyric = null
                } else {
                    errorMessage = null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lyrics")
            errorMessage = "加载歌词失败: ${e.message ?: "未知错误"}"
            lyric = null
        } finally {
            isLoading = false
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
                // 加载状态
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            errorMessage != null -> {
                // 错误或无歌词状态
                Text(
                    text = errorMessage!!,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            lyric != null -> {
                // 歌词显示
                LyricsUI(
                    modifier = Modifier.fillMaxSize(),
                    lyric = lyric!!,
                    currentTime = currentTime,
                    onSeekToTime = {
                        playerViewModel.seekTo(it)
                    },
                )
            }
            else -> {
                // 初始状态
                Text(
                    text = "等待播放",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
