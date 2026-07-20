package me.spica27.spicamusic.ui.player

import android.os.FileUtils
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.geometry.GeometryTransition.GeometryPhase
import me.spica27.navkit.geometry.geometrySource
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
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioDynamicWaveSlider
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.ui.widget.clickHighlight
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
private const val PAGE_COUNT = 2
const val DEFAULT_PAGE = 0
private const val HERO_REVEAL_THRESHOLD = 0.08f
private const val META_REVEAL_THRESHOLD = 0.18f
private const val MINI_LYRIC_REVEAL_THRESHOLD = 0.24f
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
                    // 顶部工具栏
                    TopBar(
                        modifier = Modifier,
                        onCollapse = onCollapse,
                        progressProvider = progressProvider,
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
 * 顶部工具栏
 */
@Composable
private fun TopBar(
    onCollapse: () -> Unit,
    progressProvider: () -> Float,
    modifier: Modifier,
    onPlaylistBtnClick: () -> Unit = {},
) {
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
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            shape = CircleShape,
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
        }
    }
}

// ---------- 音频信息组件 ----------

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

    val path = LocalNavigationPath.current

    val coverTransition =
        remember {
            GeometryTransition(
                key = "lyric_hero_cover",
                sourceClipRadius = 16.dp,
                targetClipRadius = 12.dp,
            )
        }
    val titleTransition = remember { GeometryTransition(key = "lyric_hero_title", sourceClipRadius = 0.dp) }
    val artistTransition = remember { GeometryTransition(key = "lyric_hero_artist", sourceClipRadius = 0.dp) }

    val title =
        currentMediaItem
            .invoke()
            ?.mediaMetadata
            ?.title
            ?.toString()
            ?: stringResource(R.string.unknown_song)
    val artist =
        currentMediaItem
            .invoke()
            ?.mediaMetadata
            ?.artist
            ?.toString()
            ?: stringResource(R.string.unknown_artist)

    Column(
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = Spacing.Medium, bottom = Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 封面区：弹性吸收剩余高度——矮屏上封面自动缩小而不挤压下方控件，
        // 高屏上封面在区域内垂直居中，信息与控制区始终贴底对齐
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = Spacing.Medium),
            contentAlignment = Alignment.Center,
        ) {
            // 封面（跳转全屏歌词时作为共享元素飞入歌词页 header）
            Box(
                modifier =
                    Modifier
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val heroReveal =
                                calculateFadeAlpha(progressProvider(), HERO_REVEAL_THRESHOLD)
                            // 飞行期间本体隐藏，由浮层接管显示
                            alpha = if (coverTransition.shouldShowSource()) heroReveal else 0f
                            translationY = (1f - heroReveal) * 48f
                            scaleX = floatLerp(COLLAPSED_HERO_SCALE, 1f, heroReveal)
                            scaleY = floatLerp(COLLAPSED_HERO_SCALE, 1f, heroReveal)
                        }.geometrySource(coverTransition)
                        .clip(Shapes.LargeCornerBasedShape),
            ) {
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
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickHighlight {
                                    // 防止重复点击！！
                                    if (path.scenes.none { it is LyricScene } &&
                                        coverTransition.phase.value == GeometryPhase.Source
                                    ) {
                                        path.push(
                                            LyricScene(
                                                heroArtworkUri =
                                                    currentMediaItem
                                                        ?.mediaMetadata
                                                        ?.artworkUri,
                                                coverTransition = coverTransition,
                                            ),
                                        )
                                    }
                                },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        // 歌曲信息
        SongInfo(
            title = title,
            artist = artist,
            titleTransition = titleTransition,
            artistTransition = artistTransition,
            modifier =
                Modifier.graphicsLayer {
                    val metaReveal = calculateFadeAlpha(progressProvider(), META_REVEAL_THRESHOLD)
                    alpha = metaReveal
                    translationY = (1f - metaReveal) * 24f
                },
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

        // mini 歌词：点击跳转全屏歌词页面
        MiniLyric(
            onClick = {
                // 防止重复点击！！
                if (path.scenes.none { it is LyricScene } &&
                    coverTransition.phase.value == GeometryPhase.Source
                ) {
                    path.push(
                        LyricScene(
                            heroArtworkUri =
                                currentMediaItem
                                    .invoke()
                                    ?.mediaMetadata
                                    ?.artworkUri,
                            coverTransition = coverTransition,
                        ),
                    )
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val lyricReveal =
                            calculateFadeAlpha(progressProvider(), MINI_LYRIC_REVEAL_THRESHOLD)
                        alpha = lyricReveal
                        translationY = (1f - lyricReveal) * 24f
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
        Spacer(modifier = Modifier.height(Spacing.Small))
        // 音质标签：固定高度槽位保持版面节奏，无标签时留白而不是显示空药丸
        val qualityTags =
            buildList {
                if (audioQualityInfo.sampleRate > 0) {
                    add("${audioQualityInfo.sampleRate / 1000}kHz")
                }
                if (audioQualityInfo.bitRate > 0) {
                    add("${audioQualityInfo.bitRate / 1000}kbps")
                }
                if (audioQualityInfo.isLossless) {
                    add(stringResource(R.string.lossless))
                }
                if (audioQualityInfo.bitRate >= 320000 && !audioQualityInfo.isLossless) {
                    add(stringResource(R.string.high_quality))
                }
            }
        Box(
            modifier =
                Modifier
                    .height(28.dp)
                    .graphicsLayer {
                        val tagsReveal =
                            calculateFadeAlpha(progressProvider(), TAGS_REVEAL_THRESHOLD)
                        alpha = tagsReveal
                        translationY = (1f - tagsReveal) * 24f
                    },
            contentAlignment = Alignment.Center,
        ) {
            if (qualityTags.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = Spacing.Medium, vertical = 5.dp)
                            .animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    qualityTags.forEachIndexed { index, tag ->
                        if (index > 0) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.55f),
                            )
                        }
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
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
        Spacer(modifier = Modifier.height(Spacing.Large))
        // 控制按钮（补全展开揭示编排，与封面/信息/进度条同一套节奏）
        PlayerControls(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val controlsReveal =
                            calculateFadeAlpha(progressProvider(), PLAYER_CONTROLS_REVEAL_THRESHOLD)
                        alpha = controlsReveal
                        translationY = (1f - controlsReveal) * 24f
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
    // FFT 插值计算随下方 collectAsStateWithLifecycle 收集自动启停，无需手动订阅/解绑
    val useDynamicWaveform = progressBarStyle == ProgressBarStyle.DynamicWaveform && isAppInForeground
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
                    waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest),
                    progressBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Small))
        // 当前位置 和 总时长（等宽数字避免走时跳动，两端与波形边缘对齐）
        val timeStyle =
            MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
            )
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = formatTime(realPosition.toLong()),
                    style = timeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
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
                        style = timeStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = formatTime(duration),
                style = timeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 4.dp),
            )
        }
    }
}

// ---------- 播放器控制组件 ----------

/**
 * 歌曲信息
 *
 * [titleTransition] / [artistTransition] 非 null 时，歌名与作者文本作为
 * 跳转全屏歌词的共享元素源节点：飞行期间本体隐藏，静止阶段记录源矩形。
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    titleTransition: GeometryTransition? = null,
    artistTransition: GeometryTransition? = null,
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
            modifier = Modifier.geometrySourceFor(titleTransition),
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        AnimatedContent(
            artist,
            contentKey = { it },
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
            modifier = Modifier.geometrySourceFor(artistTransition),
        ) { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun Modifier.geometrySourceFor(transition: GeometryTransition?): Modifier =
    if (transition == null) {
        this
    } else {
        this
            .graphicsLayer {
                alpha = if (transition.shouldShowSource()) 1f else 0f
            }.geometrySource(transition)
    }

/** 控制按钮图标切换：淡入 + 弹性缩放（全屏播放器统一节奏） */
private fun controlIconTransform() =
    (
        fadeIn(tween(160)) +
            scaleIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                initialScale = 0.7f,
            )
    ).togetherWith(fadeOut(tween(120)))

/**
 * 播放控制按钮：两端为次级操作（播放模式/收藏），中间为主传输组，
 * 避免此前 Center 排列下按钮总宽超出容器、次级按钮与传输键粘连的问题
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 播放模式
        IconButton(onClick = onPlayModeClick) {
            AnimatedContent(
                targetState = playMode,
                transitionSpec = { controlIconTransform() },
                label = "playModeIcon",
            ) { mode ->
                Icon(
                    imageVector =
                        when (mode) {
                            PlayMode.LOOP -> Icons.Rounded.Repeat
                            PlayMode.LIST -> Icons.Rounded.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                    contentDescription = stringResource(R.string.play_mode),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.previous_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
            }
            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.92f else 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 1100f,
                    ),
                label = "playPressScale",
            )
            IconButton(
                onClick = onPlayPauseClick,
                interactionSource = playInteraction,
                modifier =
                    Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { controlIconTransform() },
                    label = "playPauseIcon",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription =
                            if (playing) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            },
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.next_track),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // 收藏
        IconButton(onClick = onFavoriteClick) {
            AnimatedContent(
                targetState = isLike,
                transitionSpec = { controlIconTransform() },
                label = "favoriteIcon",
            ) { liked ->
                Icon(
                    imageVector =
                        if (liked) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                    contentDescription = stringResource(R.string.favorite),
                    tint =
                        if (liked) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.size(26.dp),
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
