package me.spica27.spicamusic.ui.player

import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.linc.amplituda.Amplituda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.player.pages.CurrentPlaylistPage
import me.spica27.spicamusic.ui.player.pages.FullScreenLyricsPage
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCityVisualizer
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.FluidMusicBackground
import me.spica27.spicamusic.ui.widget.ShiningStarsVisualizer
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.ui.widget.materialSharedAxisYIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisYOut
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================================
// 常量定义
// ============================================

// 展开动画透明度阈值常量
private const val COVER_FADE_THRESHOLD = 0.8f
private const val CONTROLS_FADE_THRESHOLD = 0.5f
private const val PAGE_COUNT = 3
const val DEFAULT_PAGE = 1

// ============================================
// 主屏幕组件
// ============================================

/**
 * 全屏播放器页面
 */
@Composable
fun ExpandedPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
    onCollapse: () -> Unit,
    progress: Float = 1f, // 展开进度，用于视觉效果
    initialPage: Int = DEFAULT_PAGE, // 初始页面索引
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val duration by viewModel.currentDuration.collectAsStateWithLifecycle()

    // 监听应用生命周期状态
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

    // 判断应用是否在前台（可见状态）
    val isAppInForeground =
        remember(lifecycleState) {
            lifecycleState.isAtLeast(Lifecycle.State.STARTED)
        }

    // 当前播放位置（定时更新）
    // 使用 currentMediaItem?.mediaId 作为 key，切歌时自动重置 seek 状态
    val mediaId = currentMediaItem?.mediaId
    var seekValueState by remember(mediaId) { mutableFloatStateOf(0f) }
    var isSeekingState by remember(mediaId) { mutableStateOf(false) }

    val trueTimePosition by viewModel.currentPosition.collectAsStateWithLifecycle()

    val songLikeState by viewModel.currentSongIsLike.collectAsStateWithLifecycle()

    LaunchedEffect(songLikeState) {
        Timber.tag("ExpandedPlayerScreen").d("当前歌曲收藏状态: $songLikeState")
    }

    // 将播放位置同步到 seekbar：trueTimePosition 每秒更新多次，
    // 使用 SideEffect（同步、无协程开销）代替 LaunchedEffect，
    // 避免每次更新都取消并重建一个新的协程。
    LaunchedEffect(trueTimePosition) {
        if (!isSeekingState) {
            seekValueState = trueTimePosition.toFloat()
        }
    }

    // Pager 状态，使用传入的初始页面
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { PAGE_COUNT })

    // 从封面提取主色调
    val coverColor =
        rememberDominantColorFromUri(
            uri = currentMediaItem?.mediaMetadata?.artworkUri,
            fallbackColor = MaterialTheme.colorScheme.primary,
        )

    Box(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
    ) {
        // 流动背景（仅前台时启用，节省电量）
        FluidMusicBackground(
            modifier =
                Modifier
                    .fillMaxSize(),
            coverColor = coverColor,
            enabled = isAppInForeground,
            isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
        )

        // 内容层
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部工具栏（带页面指示器）
            TopBar(
                currentPage = pagerState.currentPage,
                onCollapse = onCollapse,
            )

            // 水平 Pager 内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { it },
            ) { page ->
                when (page) {
                    0 -> {
                        // 当前播放列表页面
                        ShowOnIdleContent(pagerState.currentPage == 0) {
                            CurrentPlaylistPage(
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    1 -> {
                        // 播放器页面
                        ShowOnIdleContent(pagerState.currentPage == 1) {
                            PlayerPage(
                                isSeekingState = isSeekingState,
                                currentMediaItem = currentMediaItem,
                                realPosition = trueTimePosition.toFloat(),
                                seekPosition = seekValueState,
                                duration = duration,
                                isPlaying = isPlaying,
                                isLike = songLikeState,
                                playMode = playMode,
                                onValueChange = {
                                    isSeekingState = true
                                    seekValueState = it * duration
                                },
                                onValueChangeFinished = {
                                    viewModel.seekTo(seekValueState.toLong())
                                    isSeekingState = false
                                },
                                onPlayPauseClick = { viewModel.togglePlayPause() },
                                onPreviousClick = { viewModel.skipToPrevious() },
                                onNextClick = { viewModel.skipToNext() },
                                onPlayModeClick = { viewModel.togglePlayMode() },
                                onFavoriteClick = {
                                    viewModel.toggleLikeCurrentSong()
                                },
                                progress = progress,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    2 -> {
                        // 全屏歌词页面（占位）
                        ShowOnIdleContent(pagerState.currentPage == 2) {
                            FullScreenLyricsPage(
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// 导航栏组件
// ============================================

/**
 * 顶部工具栏（带页面指示器）
 */
@Composable
private fun TopBar(
    currentPage: Int,
    onCollapse: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.collapse),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }

            // 页面指示器
            PageIndicator(
                pageCount = 3,
                currentPage = currentPage,
            )

            // 占位符，保持布局对称
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

/**
 * 页面指示器
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 6.dp,
                label = "indicatorWidth",
            )
            val alpha = if (isSelected) 1f else 0.3f

            Box(
                modifier =
                    Modifier
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)),
            )
        }
    }
}

// ---------- 音频信息组件 ----------

/**
 * 音频标签
 */
@Composable
private fun AudioTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.MediumCornerBasedShape,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 音频质量标签组
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioQualityTags(
    currentMediaItem: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val extras = currentMediaItem?.mediaMetadata?.extras
    val sampleRate = extras?.getInt("sampleRate") ?: 0
    val bitRate = extras?.getInt("bitRate") ?: 0
    val mimeType = extras?.getString("mimeType") ?: ""
    val isLossless =
        mimeType.contains("flac", ignoreCase = true) ||
            mimeType.contains(
                "alac",
                ignoreCase = true,
            ) ||
            mimeType.contains("wav", ignoreCase = true)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 采样率标签
        if (sampleRate > 0) {
            AudioTag(
                text = "${sampleRate / 1000}kHz",
                color = if (sampleRate >= 96000) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 比特率标签
        if (bitRate > 0) {
            val bitRateKbps = bitRate / 1000
            AudioTag(
                text = "${bitRateKbps}kbps",
                color = if (bitRateKbps >= 320) Color(0xFF2196F3) else MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 无损标签
        if (isLossless) {
            AudioTag(
                text = stringResource(R.string.lossless),
                color = Color(0xFFFF9800),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 高品质标签
        if (bitRate >= 320000 && !isLossless) {
            AudioTag(
                text = stringResource(R.string.high_quality),
                color = Color(0xFF9C27B0),
            )
        }
    }
}

// ---------- 播放器页面 ----------

/**
 * 播放器页面（原有的播放器内容）
 */
@Composable
private fun PlayerPage(
    currentMediaItem: MediaItem?,
    seekPosition: Float,
    realPosition: Float,
    duration: Long,
    isPlaying: Boolean,
    isLike: Boolean,
    playMode: PlayMode,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
    isSeekingState: Boolean = false,
) {
    // 读取动态封面设置
    val preferencesManager: PreferencesManager = koinInject()
    val dynamicCoverTypeValue by preferencesManager
        .getString(PreferencesManager.Keys.DYNAMIC_COVER_TYPE, DynamicCoverType.ShiningStars.value)
        .collectAsStateWithLifecycle(initialValue = DynamicCoverType.ShiningStars.value)
    val dynamicCoverType =
        remember(dynamicCoverTypeValue) {
            DynamicCoverType.fromString(dynamicCoverTypeValue)
        }
    val isCoverFlipEnabled = dynamicCoverType !is DynamicCoverType.OFF

    // 翻转动画状态
    var isCoverFlipped by rememberSaveable { mutableStateOf(false) }
    val animDuration = 600
    val cameraDistance = 12f

    // 当设置关闭时，自动重置翻转状态
    LaunchedEffect(isCoverFlipEnabled) {
        if (!isCoverFlipEnabled) {
            isCoverFlipped = false
        }
    }

    // Y轴旋转角度动画
    val rotateY by animateFloatAsState(
        targetValue = if (isCoverFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOut),
        label = "coverRotation",
    )

    Column(
        modifier =
            modifier.padding(
                vertical = 24.dp,
                horizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 封面（带翻转动画）
        Box(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        rotationY = rotateY
                        this.cameraDistance = cameraDistance * density
                    }.clip(Shapes.LargeCornerBasedShape)
                    .then(
                        if (isCoverFlipEnabled) {
                            Modifier.clickable { isCoverFlipped = !isCoverFlipped }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            // 根据旋转角度显示正面或背面
            if (rotateY <= 90f) {
                // 正面：封面

                AnimatedContent(
                    currentMediaItem,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
                    },
                ) { currentMediaItem ->
                    AudioCover(
                        uri = currentMediaItem?.mediaMetadata?.artworkUri,
                        placeHolder = {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(Shapes.LargeCornerBasedShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = stringResource(R.string.cover_placeholder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .size(64.dp)
                                            .align(Alignment.Center),
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(Shapes.LargeCornerBasedShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            } else {
                // 背面：3D 音频可视化器
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = calculateFadeAlpha(progress, COVER_FADE_THRESHOLD)
                                rotationY = 180f // 翻转背面使其正向显示
                            }.clip(Shapes.LargeCornerBasedShape),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+ 使用 AGSL 可视化器

                        val playerViewModel = LocalPlayerViewModel.current
                        val scope = rememberCoroutineScope()

                        // 订阅 FFT 绘制数据
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.Default) {
                                playerViewModel.subscribeFFTDrawData()
                            }
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                scope.launch(Dispatchers.Default) {
                                    playerViewModel.unsubscribeFFTDrawData()
                                }
                            }
                        }

                        val fftBands =
                            playerViewModel.fftDrawData.collectAsStateWithLifecycle().value

                        val coverColor =
                            rememberDominantColorFromUri(
                                uri = currentMediaItem?.mediaMetadata?.artworkUri,
                                fallbackColor = MaterialTheme.colorScheme.primary,
                            )

                        when (dynamicCoverType) {
                            is DynamicCoverType.AudioCity -> {
                                AudioCityVisualizer(
                                    modifier = Modifier.fillMaxSize(),
                                    fftBands = fftBands,
                                    baseColor = coverColor,
                                )
                            }

                            else -> {
                                ShiningStarsVisualizer(
                                    modifier = Modifier.fillMaxSize(),
                                    fftBands = fftBands,
                                    baseColor = coverColor,
                                )
                            }
                        }
                    } else {
                        // Android 13 以下显示占位
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.visualization_requires_android13),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 歌曲信息
        SongInfo(
            title =
                currentMediaItem?.mediaMetadata?.title?.toString()
                    ?: stringResource(R.string.unknown_song),
            artist =
                currentMediaItem?.mediaMetadata?.artist?.toString()
                    ?: stringResource(R.string.unknown_artist),
        )

        Spacer(modifier = Modifier.height(24.dp))

        val amplituda: Amplituda = koinInject<Amplituda>()

        // 优化：使用缓存机制避免重复加载波形数据
        val amplitudeCache = remember { mutableMapOf<String, List<Int>>() }
        var ampState by remember { mutableStateOf(listOf<Int>()) }

        // 音频波形数据
        LaunchedEffect(currentMediaItem?.mediaId) {
            val mediaId = currentMediaItem?.mediaId ?: return@LaunchedEffect

            // 检查缓存
            if (amplitudeCache.containsKey(mediaId)) {
                ampState = amplitudeCache[mediaId] ?: emptyList()
                return@LaunchedEffect
            }

            launch(Dispatchers.IO) {
                val data = loadAmplitudeData(currentMediaItem, amplituda)

                // 保存到缓存，最多保留3首歌曲的数据
                if (amplitudeCache.size >= 3) {
                    // 移除最旧的项
                    amplitudeCache.remove(amplitudeCache.keys.first())
                }
                amplitudeCache[mediaId] = data
                ampState = data
            }
        }
        LaunchedEffect(seekPosition) {
            Log.e("PlayerPage", "duration changed: $seekPosition")
        }
        // 进度条
        AudioWaveSlider(
            progress = if (duration > 0) (seekPosition / duration).coerceIn(0f, 1f) else 0f,
            amplitudes = ampState,
            onProgressChange = {
                onValueChange.invoke(it)
            },
            onProgressChangeFinished = {
                onValueChangeFinished.invoke()
            },
            waveformBrush = SolidColor(MaterialTheme.colorScheme.inverseOnSurface),
            progressBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .graphicsLayer {
                        alpha = calculateFadeAlpha(progress, CONTROLS_FADE_THRESHOLD)
                    },
        )
        // 当前位置 和 总时长
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(realPosition.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 滑动到的地方
            AnimatedVisibility(
                visible = isSeekingState,
            ) {
                Text(
                    modifier =
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.inversePrimary,
                                shape = Shapes.SmallCornerBasedShape,
                            ).padding(vertical = 4.dp, horizontal = 8.dp),
                    text = formatTime(seekPosition.toLong()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        // 控制按钮
        PlayerControls(
            modifier =
                Modifier.graphicsLayer {
                    alpha = calculateFadeAlpha(progress, CONTROLS_FADE_THRESHOLD)
                },
            isPlaying = isPlaying,
            playMode = playMode,
            isLike = isLike,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onPlayModeClick = onPlayModeClick,
            onFavoriteClick = onFavoriteClick,
        )
    }
}

// ============================================
// UI 子组件
// ============================================

// ---------- 播放器控制组件 ----------

/**
 * 歌曲信息
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            title,
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
            contentKey = { it },
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            artist,
            contentKey = { it },
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
        ) { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 播放控制按钮
 */
@Composable
private fun PlayerControls(
    modifier: Modifier,
    isPlaying: Boolean,
    playMode: PlayMode,
    isLike: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 主控制行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 上一首
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.previous_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 播放/暂停
            IconButton(
                onClick = onPlayPauseClick,
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription =
                        if (isPlaying) {
                            stringResource(R.string.pause)
                        } else {
                            stringResource(
                                R.string.play,
                            )
                        },
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 下一首
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.next_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 次要控制行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 播放模式
            IconButton(onClick = onPlayModeClick) {
                Icon(
                    imageVector =
                        when (playMode) {
                            PlayMode.LOOP -> Icons.Rounded.Repeat
                            PlayMode.LIST -> Icons.Rounded.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                    contentDescription = stringResource(R.string.play_mode),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }

            // 收藏
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector =
                        if (isLike) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                    contentDescription = stringResource(R.string.favorite),
                    tint =
                        if (isLike) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

// ============================================
// 工具函数
// ============================================

/**
 * 格式化时间 (毫秒 -> mm:ss)
 */
fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.CHINESE, "%d:%02d", minutes, seconds)
}

/**
 * 计算淡入透明度
 * @param progress 当前进度 (0-1)
 * @param threshold 开始淡入的阈值
 * @return 透明度值 (0-1)
 */
private fun calculateFadeAlpha(
    progress: Float,
    threshold: Float,
): Float =
    if (progress < threshold) {
        0f
    } else {
        ((progress - threshold) / (1f - threshold)).coerceIn(0f, 1f)
    }

/**
 * 加载音频波形数据
 */
private suspend fun loadAmplitudeData(
    mediaItem: MediaItem?,
    amplituda: Amplituda,
): List<Int> =
    withContext(Dispatchers.IO) {
        val config = mediaItem?.localConfiguration ?: return@withContext emptyList()

        // ALAC 和 MP4 格式不支持波形提取
        if (config.mimeType == MimeTypes.AUDIO_ALAC || config.mimeType == MimeTypes.AUDIO_MP4) {
            return@withContext emptyList()
        }

        return@withContext try {
            App.getInstance().contentResolver.openInputStream(config.uri)?.use { inputStream ->
                var result = emptyList<Int>()
                amplituda.processAudio(inputStream).get(
                    { result = it.amplitudesAsList() },
                    { result = emptyList() },
                )
                result
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
