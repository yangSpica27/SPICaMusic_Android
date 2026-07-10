package me.spica27.spicamusic.ui.player

import android.os.FileUtils
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.common.entity.ProgressBarStyle
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.player.pages.CurrentPlaylistPage
import me.spica27.spicamusic.ui.player.scene.LyricScene
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.FluidMusicBackground
import me.spica27.spicamusic.ui.widget.LyricsDisplayMode
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioDynamicWaveSlider
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.ui.widget.materialSharedAxisYIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisYOut
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.koinInject
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import androidx.compose.ui.util.lerp as floatLerp

// ============================================
// 常量定义
// ============================================

// 展开动画透明度阈值常量
private const val COVER_FADE_THRESHOLD = 0.8f
private const val CONTROLS_FADE_THRESHOLD = 0.5f
private const val PAGE_COUNT = 2
const val DEFAULT_PAGE = 0
private const val HERO_REVEAL_THRESHOLD = 0.08f
private const val META_REVEAL_THRESHOLD = 0.18f
private const val TAGS_REVEAL_THRESHOLD = 0.28f
private const val SEEKBAR_REVEAL_THRESHOLD = 0.34f
private const val PLAYER_CONTROLS_REVEAL_THRESHOLD = 0.48f
private const val COLLAPSED_HERO_SCALE = 0.82f
private val EmptyFftDrawData = FloatArray(0)

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
    progressProvider: () -> Float = { 1f }, // 展开进度提供器，避免整棵树每帧重组
    initialPage: Int = DEFAULT_PAGE, // 初始页面索引
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val duration by viewModel.currentDuration.collectAsStateWithLifecycle()
    val audioQualityInfo =
        remember(currentMediaItem?.mediaId, currentMediaItem?.mediaMetadata?.extras) {
            currentMediaItem.toAudioQualityInfo()
        }

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
    // 注意：保留 State 引用而非用 by 解包，避免每秒的播放位置更新触发整个播放器树重组。
    // 位置读取下沉到 SeekBarSection 叶子组件，通过 provider lambda 局部消费。
    val seekValueState = remember(mediaId) { mutableFloatStateOf(0f) }
    var isSeekingState by remember(mediaId) { mutableStateOf(false) }

    val positionState = viewModel.currentPosition.collectAsStateWithLifecycle()

    val songLikeState by viewModel.currentSongIsLike.collectAsStateWithLifecycle()

    LaunchedEffect(songLikeState) {
        Timber.tag("ExpandedPlayerScreen").d("当前歌曲收藏状态: $songLikeState")
    }

    BackHandler(progressProvider.invoke() > .99f) {
        onCollapse.invoke()
    }

    // 将播放位置同步到 seekbar：用 snapshotFlow 在协程中观察位置变化，
    // 避免在组合作用域读取高频 state 而导致重组。
    LaunchedEffect(mediaId) {
        snapshotFlow { positionState.value }
            .collect { position ->
                if (!isSeekingState) {
                    seekValueState.floatValue = position.toFloat()
                }
            }
    }

    val coroutineScope = rememberCoroutineScope()

    val path = LocalNavigationPath.current

    // Pager 状态，使用传入的初始页面
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { PAGE_COUNT })

    BackHandler(pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                0,
                animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
            )
        }
    }

    // 兜底吸附：快速向下滑动 / 手指滑出屏幕边缘导致手势被取消、未触发正常 fling 时，
    // Pager 可能停在两页之间（offsetFraction != 0）。这里监听滚动结束，
    // 若仍处于中间态则强制吸附到最近的页面，避免卡死在中间态。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling && abs(pagerState.currentPageOffsetFraction) > 0.01f) {
                    pagerState.animateScrollToPage(
                        pagerState.currentPage,
                        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
                    )
                }
            }
    }

    // 从封面提取主色调
    val coverColor =
        rememberDominantColorFromUri(
            uri = currentMediaItem?.mediaMetadata?.artworkUri,
            fallbackColor = MaterialTheme.colorScheme.primary,
        )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    alpha = progressProvider()
                }.background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
    ) {
        // 流动背景（仅前台时启用，节省电量）
        FluidMusicBackground(
            modifier =
                Modifier
                    .fillMaxSize(),
            coverColor = coverColor,
            isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
            coverUri = { currentMediaItem?.mediaMetadata?.artworkUri },
        )

        // 内容层
        VerticalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            key = { it },
            overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
            flingBehavior =
                PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = .2f,
                ),
        ) {
            if (it == 0) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 顶部工具栏（带页面指示器）
                    TopBar(
                        modifier = Modifier,
                        currentPage = pagerState.currentPage,
                        onCollapse = onCollapse,
                        progressProvider = progressProvider,
                        onLyricBtnClick = {
                            path.push(LyricScene())
                        },
                        onPlaylistBtnClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    1,
                                    animationSpec =
                                        tween(
                                            durationMillis = 300,
                                            easing = EaseOutCubic,
                                        ),
                                )
                            }
                        },
                    )

                    // 水平 Pager 内容区域
                    // 播放器页面
                    ShowOnIdleContent(
                        modifier = Modifier.weight(1f),
                        visible = progressProvider.invoke() > .4f,
                    ) {
                        PlayerPage(
                            playerViewModel = viewModel,
                            isSeekingState = isSeekingState,
                            currentMediaItem = { currentMediaItem },
                            audioQualityInfo = audioQualityInfo,
                            realPositionProvider = { positionState.value.toFloat() },
                            seekPositionProvider = { seekValueState.floatValue },
                            duration = duration,
                            isPlaying = isPlaying,
                            isLike = songLikeState,
                            playMode = playMode,
                            onValueChange = {
                                isSeekingState = true
                                seekValueState.floatValue = it * duration
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(seekValueState.floatValue.toLong())
                                isSeekingState = false
                            },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.skipToPrevious() },
                            onNextClick = { viewModel.skipToNext() },
                            onPlayModeClick = { viewModel.togglePlayMode() },
                            onFavoriteClick = {
                                viewModel.toggleLikeCurrentSong()
                            },
                            progressProvider = progressProvider,
                            isAppInForeground = isAppInForeground,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                // 播放列表
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            colors =
                                TopAppBarDefaults.topAppBarColors().copy(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                0,
                                                animationSpec =
                                                    tween(
                                                        durationMillis = 550,
                                                        easing = EaseOutCubic,
                                                    ),
                                            )
                                        }
                                    },
                                    colors =
                                        IconButtonDefaults.iconButtonColors().copy(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                            title = {
                                Text(stringResource(R.string.now_playinglist))
                            },
                        )
                    },
                ) {
                    ShowOnIdleContent(true) {
                        Box(
                            modifier = Modifier.padding(it),
                        ) {
                            CurrentPlaylistPage(
                                modifier = Modifier.fillMaxSize(),
//                        scrollBehavior = scrollBehavior,
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
    progressProvider: () -> Float,
    modifier: Modifier,
    onPlaylistBtnClick: () -> Unit = {},
    onLyricBtnClick: () -> Unit = {},
) {
    val path = LocalNavigationPath.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.Small))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = progressProvider()
                        val barAlpha = calculateFadeAlpha(progress, HERO_REVEAL_THRESHOLD)
                        alpha = barAlpha
                        translationY = (1f - barAlpha) * -20f
                    }.padding(horizontal = Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier,
                onClick = {
                    onCollapse.invoke()
                },
                colors =
                    IconButtonDefaults.iconButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.collapse),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topStartPercent = 50,
                                bottomStartPercent = 50,
                                topEndPercent = 15,
                                bottomEndPercent = 15,
                            ),
                        ).background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            shape =
                                RoundedCornerShape(
                                    topStartPercent = 50,
                                    bottomStartPercent = 50,
                                    topEndPercent = 15,
                                    bottomEndPercent = 15,
                                ),
                        ).clickable {
                            onPlaylistBtnClick.invoke()
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.queue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topEndPercent = 50,
                                bottomEndPercent = 50,
                                topStartPercent = 15,
                                bottomStartPercent = 15,
                            ),
                        ).background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            shape =
                                RoundedCornerShape(
                                    topEndPercent = 50,
                                    bottomEndPercent = 50,
                                    topStartPercent = 15,
                                    bottomStartPercent = 15,
                                ),
                        ).clickable {
                            onLyricBtnClick.invoke()
                        }.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tab_lrc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
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

@Immutable
private data class AudioQualityInfo(
    val sampleRate: Int,
    val bitRate: Int,
    val isLossless: Boolean,
)

private fun MediaItem?.toAudioQualityInfo(): AudioQualityInfo {
    val extras = this?.mediaMetadata?.extras
    val sampleRate = extras?.getInt("sampleRate") ?: 0
    val bitRate = extras?.getInt("bitRate") ?: 0
    val mimeType = extras?.getString("mimeType").orEmpty()
    val isLossless =
        mimeType.contains("flac", ignoreCase = true) ||
            mimeType.contains("alac", ignoreCase = true) ||
            mimeType.contains("wav", ignoreCase = true)

    return AudioQualityInfo(
        sampleRate = sampleRate,
        bitRate = bitRate,
        isLossless = isLossless,
    )
}

// ---------- 播放器页面 ----------

/**
 * 播放器页面（原有的播放器内容）
 */
@Composable
private fun PlayerPage(
    playerViewModel: PlayerViewModel,
    currentMediaItem: () -> MediaItem?,
    audioQualityInfo: AudioQualityInfo,
    seekPositionProvider: () -> Float,
    realPositionProvider: () -> Float,
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
    progressProvider: () -> Float,
    isAppInForeground: Boolean,
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
    val progressBarStyleValue by preferencesManager
        .getString(PreferencesManager.Keys.PROGRESS_BAR_STYLE, ProgressBarStyle.TimeDomainWaveform.value)
        .collectAsStateWithLifecycle(initialValue = ProgressBarStyle.TimeDomainWaveform.value)
    val progressBarStyle =
        remember(progressBarStyleValue) {
            ProgressBarStyle.fromString(progressBarStyleValue)
        }
    val songUseCases = koinInject<SongUseCases>()
    // 翻转动画状态
    var isCoverFlipped by rememberSaveable { mutableStateOf(false) }
    val animDuration = 600
    val cameraDistance = 12f

    // Y轴旋转角度动画
    val rotateY by animateFloatAsState(
        targetValue = if (isCoverFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOut),
        label = "coverRotation",
    )

    Column(
        modifier =
            modifier.padding(
                vertical = Spacing.ExtraLarge,
                horizontal = Spacing.ExtraLarge,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 封面（带翻转动画）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val heroReveal =
                            calculateFadeAlpha(progressProvider(), HERO_REVEAL_THRESHOLD)
                        alpha = heroReveal
                        translationY = (1f - heroReveal) * 48f
                        scaleX = floatLerp(COLLAPSED_HERO_SCALE, 1f, heroReveal)
                        scaleY = floatLerp(COLLAPSED_HERO_SCALE, 1f, heroReveal)
                        rotationY = rotateY
                        this.cameraDistance = cameraDistance * density
                    }.clip(Shapes.LargeCornerBasedShape)
                    .then(
                        Modifier.clickable { isCoverFlipped = !isCoverFlipped },
                    ),
        ) {
            // 根据旋转角度显示正面或背面
            if (rotateY <= 90f) {
                // 正面：封面

                AnimatedContent(
                    currentMediaItem.invoke(),
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
                    },
                    contentKey = { it?.mediaId ?: "-1" },
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
                // 背面：歌词（卡片场景用紧凑排版）
                LyricsPanel(
                    displayMode = LyricsDisplayMode.Compact,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(Shapes.LargeCornerBasedShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                            .graphicsLayer {
                                rotationY = 180f
                            }.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        // 歌曲信息
        SongInfo(
            title =
                currentMediaItem
                    .invoke()
                    ?.mediaMetadata
                    ?.title
                    ?.toString()
                    ?: stringResource(R.string.unknown_song),
            artist =
                currentMediaItem
                    .invoke()
                    ?.mediaMetadata
                    ?.artist
                    ?.toString()
                    ?: stringResource(R.string.unknown_artist),
            modifier =
                Modifier.graphicsLayer {
                    val metaReveal = calculateFadeAlpha(progressProvider(), META_REVEAL_THRESHOLD)
                    alpha = metaReveal
                    translationY = (1f - metaReveal) * 24f
                },
        )

        val amplituda: Amplituda = koinInject<Amplituda>()

        // 优化：使用缓存机制避免重复加载波形数据
        val amplitudeCache = remember { mutableMapOf<String, List<Int>>() }
        var ampState by remember { mutableStateOf(listOf<Int>()) }

        // 音频波形数据
        LaunchedEffect(currentMediaItem.invoke()?.mediaId, progressBarStyle) {
            if (progressBarStyle != ProgressBarStyle.TimeDomainWaveform) {
                ampState = emptyList()
                return@LaunchedEffect
            }
            val mediaId = currentMediaItem.invoke()?.mediaId ?: return@LaunchedEffect

            // 检查缓存
            if (amplitudeCache.containsKey(mediaId)) {
                ampState = amplitudeCache[mediaId] ?: emptyList()
                return@LaunchedEffect
            }
            launch(Dispatchers.IO) {
                val data = loadAmplitudeData(currentMediaItem.invoke(), amplituda, songUseCases)

                // 保存到缓存，最多保留3首歌曲的数据
                if (amplitudeCache.size >= 3) {
                    // 移除最旧的项
                    amplitudeCache.remove(amplitudeCache.keys.first())
                }
                amplitudeCache[mediaId] = data
                ampState = data
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Row(
            Modifier
                .background(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    shape = Shapes.LargeCornerBasedShape,
                ).padding(horizontal = 10.dp, vertical = 4.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 采样率标签
            if (audioQualityInfo.sampleRate > 0) {
                Text(
                    text = "${audioQualityInfo.sampleRate / 1000}kHz",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            // 比特率标签
            if (audioQualityInfo.bitRate > 0) {
                val bitRateKbps = audioQualityInfo.bitRate / 1000
                Text(
                    text = "${bitRateKbps}kbps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            // 无损标签
            if (audioQualityInfo.isLossless) {
                Text(
                    text = stringResource(R.string.lossless),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            // 高品质标签
            if (audioQualityInfo.bitRate >= 320000 && !audioQualityInfo.isLossless) {
                Text(
                    text = stringResource(R.string.high_quality),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        // 进度条（位置读取下沉到该子组件，避免每秒重组整个 PlayerPage）
        SeekBarSection(
            seekPositionProvider = seekPositionProvider,
            realPositionProvider = realPositionProvider,
            duration = duration,
            amplitudes = ampState,
            progressBarStyle = progressBarStyle,
            playerViewModel = playerViewModel,
            isAppInForeground = isAppInForeground,
            isSeekingState = isSeekingState,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            progressProvider = progressProvider,
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        // 控制按钮
        PlayerControls(
            modifier =
                Modifier
                    .weight(1f),
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

/**
 * 进度条区块。
 * 单独抽出该叶子组件，使每秒的播放位置更新只重组这里，
 * 而不是连带整个 [PlayerPage] 一起重组。
 */
@Composable
private fun SeekBarSection(
    seekPositionProvider: () -> Float,
    realPositionProvider: () -> Float,
    duration: Long,
    amplitudes: List<Int>,
    progressBarStyle: ProgressBarStyle,
    playerViewModel: PlayerViewModel,
    isAppInForeground: Boolean,
    isSeekingState: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    progressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val seekPosition = seekPositionProvider()
    val realPosition = realPositionProvider()
    val useDynamicWaveform = progressBarStyle == ProgressBarStyle.DynamicWaveform && isAppInForeground
    LaunchedEffect(useDynamicWaveform, playerViewModel) {
        if (!useDynamicWaveform) {
            return@LaunchedEffect
        }
        playerViewModel.subscribeFFTDrawData()
        try {
            awaitCancellation()
        } finally {
            playerViewModel.unsubscribeFFTDrawData()
        }
    }
    Column(
        modifier =
            modifier.graphicsLayer {
                val seekbarReveal =
                    calculateFadeAlpha(progressProvider(), SEEKBAR_REVEAL_THRESHOLD)
                alpha = seekbarReveal
                translationY = (1f - seekbarReveal) * 24f
            },
    ) {
        val sliderProgress = if (duration > 0) (seekPosition / duration).coerceIn(0f, 1f) else 0f
        when (progressBarStyle) {
            ProgressBarStyle.DynamicWaveform -> {
                val fftDrawData =
                    if (useDynamicWaveform) {
                        val drawData by playerViewModel.fftDrawData.collectAsStateWithLifecycle()
                        drawData
                    } else {
                        EmptyFftDrawData
                    }
                AudioDynamicWaveSlider(
                    progress = sliderProgress,
                    fftAmplitudes = fftDrawData,
                    onProgressChange = {
                        onValueChange.invoke(it)
                    },
                    onProgressChangeFinished = {
                        onValueChangeFinished.invoke()
                    },
                    waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                    progressBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                )
            }

            ProgressBarStyle.TimeDomainWaveform -> {
                AudioWaveSlider(
                    progress = sliderProgress,
                    amplitudes = amplitudes,
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
                            .height(80.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Small))
        // 当前位置 和 总时长
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
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
            }
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 4.dp, horizontal = 8.dp),
            )
        }
    }
}

// ---------- 播放器控制组件 ----------

/**
 * 歌曲信息
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
    songUseCases: SongUseCases,
): List<Int> =
    withContext(Dispatchers.IO) {
        val config = mediaItem?.localConfiguration ?: return@withContext emptyList()

        // ALAC 和 MP4 格式不支持波形提取
        if (config.mimeType == MimeTypes.AUDIO_ALAC || config.mimeType == MimeTypes.AUDIO_MP4) {
            return@withContext emptyList()
        }

        val cache = mediaItem.mediaMetadata.extras?.getString("waveformData")

        if (!TextUtils.isEmpty(cache)) {
            val cachedList = cache!!.split(",").mapNotNull { it.toIntOrNull() }
            if (cachedList.isNotEmpty()) {
                Timber.tag("ExpandedPlayerScreen").d("使用缓存的波形数据，长度: ${cachedList.size}")
                return@withContext cachedList
            }
        }
        Timber.tag("ExpandedPlayerScreen").d("没有缓存的波形数据，开始提取...")
        return@withContext try {
            val uri = config.uri
            App.getInstance().contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile =
                    File.createTempFile("amplitude_cache", null, App.getInstance().cacheDir)

                try {
                    tempFile.outputStream().use { outputStream ->
                        FileUtils.copy(inputStream, outputStream)
                    }

                    var result = emptyList<Int>()
                    Timber.tag("ExpandedPlayerScreen").d("处理缓存文件: ${tempFile.absolutePath}")
                    amplituda.processAudio(tempFile).get(
                        {
                            result = it.amplitudesAsList()
                        },
                        { result = emptyList() },
                    )
                    songUseCases.updateSongWaveform(
                        mediaId = mediaItem.mediaId.toLongOrNull() ?: 0L,
                        waveformData = result.joinToString(","),
                    )
                    mediaItem.mediaMetadata.extras?.putString(
                        "waveformData",
                        result.joinToString(","),
                    )
                    result
                } finally {
                    tempFile.delete()
                }
            } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }
