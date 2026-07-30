package me.spica27.spicamusic.ui.home.player_bar

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.crossfade.CrossfadePlugin
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.spica27.navkit.geometry.geometryOccluder
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.player.DEFAULT_PAGE
import me.spica27.spicamusic.ui.player.ExpandedPlayerScreen
import me.spica27.spicamusic.ui.player.LargeBottomPlayerBar
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.MorphingPlayPauseIcon
import me.spica27.spicamusic.ui.player.PlayerArtworkMorphOverlay
import me.spica27.spicamusic.ui.player.playerArtworkMorphHost
import me.spica27.spicamusic.ui.player.playerArtworkMorphSource
import me.spica27.spicamusic.ui.player.rememberPlayerArtworkMorphState
import me.spica27.spicamusic.ui.player.sourceArtworkAlpha
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.widget.MusicCoverPlaceholder
import me.spica27.spicamusic.ui.widget.StableAudioCover
import me.spica27.spicamusic.ui.widget.rememberPlayingCoverShape
import me.spica27.spicamusic.utils.albumCoverFallbackUri
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp as floatLerp

/** 播放器面板的两个锚点状态 */
private enum class PlayerSheetValue { Collapsed, Expanded }

private const val BOTTOM_FLOATING_CONTAINER_ALPHA = 1f
private const val COVER_CLEAR_GRACE_PERIOD_MS = 300L

/**
 * 底部媒体控制栏
 * - 收起状态：显示迷你播放条 + 首页 Tab 切换区
 * - 展开状态：全屏播放器
 * 支持点击展开和上划/下划连续拖拽手势
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomMediaBar(bottomBarScrollConnection: BottomBarScrollConnection = LocalBottomBarScrollConnection.current) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current
    val navigationPath = LocalNavigationPath.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentHomePage = homeViewModel.currentPage.collectAsStateWithLifecycle().value
    val nowPlayingSong = playerViewModel.currentMediaItem.collectAsStateWithLifecycle().value
    // 记录迷你播放条实际高度（含导航栏内边距），首帧用估算值避免闪烁
    var miniBarHeightPx by remember { mutableStateOf(with(density) { 120.dp.roundToPx() }) }

    // 记录跳转到播放器的初始页（默认主页 or 播放列表页）
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }

    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val metadata = currentMediaItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata?.artist?.toString() ?: stringResource(R.string.unknown_artist)
    val artworkUri = metadata?.artworkUri ?: metadata?.albumCoverFallbackUri()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val coverShape = rememberPlayingCoverShape(isPlaying)

    // 可拖拽锚点状态
    val draggableState =
        remember {
            AnchoredDraggableState(
                initialValue = PlayerSheetValue.Collapsed,
                snapAnimationSpec = tween(125, easing = EaseOutCubic),
                decayAnimationSpec = exponentialDecay(),
                positionalThreshold = { totalDistance -> totalDistance * 0.45f },
                velocityThreshold = { with(density) { 150.dp.toPx() } },
            )
        }

    // 收起创建菜单的系统返回键拦截（优先级高于播放器收起）
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // 每当屏幕高度或迷你栏高度变化时更新锚点
        LaunchedEffect(screenHeightPx, miniBarHeightPx) {
            draggableState.updateAnchors(
                DraggableAnchors {
                    PlayerSheetValue.Collapsed at (screenHeightPx - miniBarHeightPx)
                    PlayerSheetValue.Expanded at 0f
                },
            )
        }

        val collapsedOffsetPx =
            remember(screenHeightPx, miniBarHeightPx) { screenHeightPx - miniBarHeightPx }
        val collapsedHorizontalInsetPx =
            with(density) { LayoutTokens.PlayerCollapsedHorizontalInset.toPx() }
        val collapsedTopInsetPx = with(density) { LayoutTokens.PlayerCollapsedTopInset.toPx() }
        val progressProvider =
            remember(draggableState, collapsedOffsetPx) {
                {
                    val offset = draggableState.offset
                    if (offset.isNaN() || collapsedOffsetPx <= 0f) {
                        0f
                    } else {
                        (1f - offset / collapsedOffsetPx).coerceIn(0f, 1f)
                    }
                }
            }

        val isExpanded by remember {
            derivedStateOf { draggableState.currentValue == PlayerSheetValue.Expanded }
        }

//        // 展开时拦截系统返回键，将播放器收起
//        BackHandler(enabled = isExpanded) {
//            coroutineScope.launch { draggableState.animateTo(PlayerSheetValue.Collapsed) }
//        }

        // 是否是单列模式
        var isSingleLineMode by rememberSaveable { mutableStateOf(true) }

        LaunchedEffect(bottomBarScrollConnection.isInline) {
            if (bottomBarScrollConnection.isInline != isSingleLineMode) {
                isSingleLineMode = bottomBarScrollConnection.isInline
            }
        }

        // 可拖拽播放器面板（通过 translationY 在屏幕上滑入 / 滑出）
        SharedTransitionLayout {
            AnimatedContent(
                isSingleLineMode,
            ) { lineMode ->

                if (!lineMode) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .zIndex(2f)
                                .offset {
                                    IntOffset(
                                        0,
                                        if (draggableState.offset.isNaN()) {
                                            collapsedOffsetPx.roundToInt()
                                        } else {
                                            draggableState.offset.roundToInt()
                                        },
                                    )
                                },
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val progress = progressProvider()
                                        val sideInsetPx =
                                            floatLerp(collapsedHorizontalInsetPx, 0f, progress)
                                        val topInsetPx =
                                            floatLerp(collapsedTopInsetPx, 0f, progress)
                                        val safeWidth = size.width.coerceAtLeast(1f)
                                        val safeHeight = size.height.coerceAtLeast(1f)

                                        translationX = sideInsetPx
                                        translationY = topInsetPx
                                        scaleX =
                                            ((safeWidth - sideInsetPx * 2f) / safeWidth).coerceIn(
                                                0.85f,
                                                1f,
                                            )
                                        scaleY =
                                            ((safeHeight - topInsetPx) / safeHeight).coerceIn(
                                                0.85f,
                                                1f,
                                            )
                                        transformOrigin =
                                            TransformOrigin(0.5f, 0f)
                                        shape =
                                            RoundedCornerShape(
                                                topStart =
                                                    lerp(
                                                        LayoutTokens.PlayerCollapsedCornerRadius,
                                                        0.dp,
                                                        progress,
                                                    ),
                                                topEnd =
                                                    lerp(
                                                        LayoutTokens.PlayerCollapsedCornerRadius,
                                                        0.dp,
                                                        progress,
                                                    ),
                                            )
                                        clip = true
                                    },
                        ) {
                            if (progressProvider.invoke() > 0.01f) {
                                ExpandedPlayerScreen(
                                    onCollapse = {
                                        coroutineScope.launch {
                                            draggableState.animateTo(PlayerSheetValue.Collapsed)
                                        }
                                    },
                                    progressProvider = progressProvider,
                                    initialPage = initialPage,
                                    modifier =
                                        Modifier
                                            .fillMaxSize(),
                                )
                            }
                        }
                        // 迷你播放条 + Tab 切换区 —— 位于面板顶部
                        // 当面板收起时，面板整体下移使迷你条恰好出现在屏幕底部
                        if (progressProvider.invoke() > 0.99f) return@AnimatedContent
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size -> miniBarHeightPx = size.height }
                                    // 登记为几何过渡遮挡物：封面飞行时被播放条盖住的部分不会突然跳到最顶层
                                    .geometryOccluder("bottom_media_bar")
                                    .animateContentSize(
                                        animationSpec =
                                            spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                            ),
                                    ).align(Alignment.TopCenter)
                                    .navigationBarsPadding()
                                    .graphicsLayer {
                                        alpha = (1f - progressProvider() / 0.25f).coerceIn(0f, 1f)
                                    }
                                    // 拖拽手势：上划展开，下划收起
                                    .anchoredDraggable(draggableState, Orientation.Vertical),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            // 实际播放条
                            LargeBottomPlayerBar(
                                coverShape = coverShape,
                                coverModifier =
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("player_cover"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ),
                                modifier =
                                    Modifier
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState("player_bar"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                        ).padding(horizontal = 16.dp)
                                        .graphicsLayer {
                                            shape = CircleShape
                                            clip = true
                                            shadowElevation = 0f
                                        }.background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                                alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                            ),
                                        ),
                                onExpand = {
                                    initialPage = DEFAULT_PAGE
                                    coroutineScope.launch {
                                        draggableState.animateTo(PlayerSheetValue.Expanded)
                                    }
                                },
                                onNext = playerViewModel::skipToNext,
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .animateContentSize()
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(vertical = 8.dp),
                            ) {
                                HomePageSwitcher(
                                    modifier =
                                        Modifier
                                            .sharedElement(
                                                sharedContentState = rememberSharedContentState("navigation_bar"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                            ).weight(1f),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .sharedElement(
                                                sharedContentState = rememberSharedContentState("plus_icon"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                            ).size(56.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.tertiary.copy(
                                                    alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                                ),
                                            ).clickable {
                                                navigationPath.push(
                                                    PlaylistCreatorScene(),
                                                )
                                            },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (progressProvider.invoke() > 0.99f) return@AnimatedContent
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = {
                                isSingleLineMode = false
                            },
                            modifier =
                                Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState("navigation_bar"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    ).size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                        ),
                                    ),
                        ) {
                            Icon(
                                imageVector = currentHomePage.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState("player_bar"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    ).height(56.dp)
                                    .weight(1f)
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                        shadowElevation = 0f
                                    }.background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                            alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                        ),
                                    ).clickable {
                                        isSingleLineMode = false
                                    }.padding(
                                        8.dp,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LandscapistImage(
                                imageModel = { artworkUri },
                                component =
                                    rememberImageComponent {
                                        +CrossfadePlugin(duration = 550)
                                    },
                                modifier =
                                    Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("player_cover"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                        ).fillMaxHeight()
                                        .aspectRatio(1f)
                                        .clip(coverShape),
                                failure = {
                                    MusicCoverPlaceholder(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        contentDescription = stringResource(R.string.cover_placeholder),
                                    )
                                },
                            )
                            Text(
                                text =
                                    if (nowPlayingSong != null) {
                                        "$title - $artist"
                                    } else {
                                        stringResource(R.string.no_song_playing)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                        .basicMarquee(repeatDelayMillis = 0),
                            )
                        }
                        IconButton(
                            onClick = {
                                navigationPath.push(
                                    PlaylistCreatorScene(),
                                )
                            },
                            modifier =
                                Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("plus_icon"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ).size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary.copy(
                                            alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                        ),
                                    ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底部媒体控制栏（V2）
 *
 * 与 [BottomMediaBar] 行为一致，但两行收起态 ←→ 全屏播放器的拖拽展开改由自定义 Layout
 * 组件 [BottomBarV2] 驱动；单行 inline 模式与共享元素过渡保持不变。
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomMediaBarV2(bottomBarScrollConnection: BottomBarScrollConnection = LocalBottomBarScrollConnection.current) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current
    val navigationPath = LocalNavigationPath.current

    val currentHomePage = homeViewModel.currentPage.collectAsStateWithLifecycle().value
    val nowPlayingSong = playerViewModel.currentMediaItem.collectAsStateWithLifecycle().value

    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val metadata = currentMediaItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song)
    val artworkUri = metadata?.artworkUri ?: metadata?.albumCoverFallbackUri()
    var stableCoverPainter by remember { mutableStateOf<Painter?>(null) }
    LaunchedEffect(artworkUri) {
        if (artworkUri == null) {
            // Media3 may briefly publish a null item while seeking to an adjacent queue entry.
            // Keep the decoded cover through that hand-off and clear it only for a real empty queue.
            delay(COVER_CLEAR_GRACE_PERIOD_MS)
            stableCoverPainter = null
        }
    }
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    // 记录跳转到播放器的初始页（默认主页 or 播放列表页）
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }

    // 全屏展开进度状态（由 BottomBarV2 驱动）
    val sheetState = rememberBottomBarV2State()
    val coverShape =
        rememberPlayingCoverShape(
            isPlaying = isPlaying,
            lockToTransitionShape = sheetState.artworkShapeLocked,
        )
    val artworkMorphState = rememberPlayerArtworkMorphState()
    LaunchedEffect(sheetState, isPlaying) {
        snapshotFlow {
            sheetState.progress >= 0.999f && !sheetState.isMorphInFlight
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                sheetState.syncExpandedArtworkShape(isPlaying)
            }
    }

    // 是否是单列模式
    // The scroll connection is the single source of truth for this transition.
    // Mirroring it through LaunchedEffect lets stale effects race an interrupted transition.
    val isSingleLineMode = bottomBarScrollConnection.isInline

    SharedTransitionLayout {
        AnimatedContent(isSingleLineMode) { lineMode ->
            if (!lineMode) {
                BottomBarV2(
                    modifier =
                        Modifier
                            .zIndex(2f)
                            .playerArtworkMorphHost(artworkMorphState),
                    state = sheetState,
                    isPlaying = isPlaying,
                    onMorphStart = artworkMorphState::freezeSourceBounds,
                    onCollapsed = artworkMorphState::resumeSourceCapture,
                    navigationBar = {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(vertical = 8.dp),
                        ) {
                            HomePageSwitcher(
                                modifier =
                                    Modifier
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState("navigation_bar"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                        ).weight(1f),
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("plus_icon"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                        ).size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary.copy(
                                                alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                            ),
                                        ).clickable {
                                            navigationPath.push(PlaylistCreatorScene())
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                )
                            }
                        }
                    },
                    playBar = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier =
                                    Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("player_surface"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                        ).matchParentSize()
                                        .graphicsLayer {
                                            shape = CircleShape
                                            clip = true
                                            shadowElevation = 0f
                                        }.background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                                alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                            ),
                                        ),
                            )
                            LargeBottomPlayerBar(
                                coverShape = coverShape,
                                coverModifier =
                                    Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("player_cover"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                        ).playerArtworkMorphSource(artworkMorphState)
                                        .graphicsLayer {
                                            alpha =
                                                sourceArtworkAlpha(
                                                    progress = sheetState.progress,
                                                    inFlight = sheetState.isMorphInFlight,
                                                    hasUsableBounds = artworkMorphState.hasUsableBounds,
                                                )
                                        },
                                titleModifier =
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("player_title"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ),
                                artistModifier =
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("player_artist"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ),
                                infoModifier =
                                    Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState("player_info_bounds"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    ),
                                playButtonModifier =
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState("player_play_button"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ),
                                nextButtonModifier =
                                    Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState("player_next_button"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                    ),
                                coverPainter = stableCoverPainter,
                                onCoverPainterReady = { stableCoverPainter = it },
                                onCoverPainterFailed = { stableCoverPainter = null },
                                modifier = Modifier.fillMaxWidth(),
                                onExpand = {
                                    initialPage = DEFAULT_PAGE
                                    artworkMorphState.freezeSourceBounds()
                                    sheetState.expand(isPlaying)
                                },
                                onNext = playerViewModel::skipToNext,
                            )
                        }
                    },
                    fullScreenPlayer = { morphProgress, morphInFlight, onCollapse, dragToCollapseModifier ->
                        ExpandedPlayerScreen(
                            onCollapse = onCollapse,
                            dragToCollapseModifier = dragToCollapseModifier,
                            progressProvider = morphProgress,
                            morphProgressProvider = morphProgress,
                            initialPage = initialPage,
                            isActive = sheetState.contentActive,
                            dynamicEffectsActive = sheetState.dynamicEffectsActive,
                            preloadContent = true,
                            artworkMorphState = artworkMorphState,
                            artworkMorphInFlightProvider = morphInFlight,
                            modifier =
                                Modifier
                                    .fillMaxSize(),
                        )
                    },
                    transitionOverlay = { morphProgress, inFlight ->
                        PlayerArtworkMorphOverlay(
                            state = artworkMorphState,
                            artworkUri = artworkUri,
                            artworkPainter = stableCoverPainter,
                            sourceShape = coverShape,
                            isPlaying = isPlaying,
                            progressProvider = morphProgress,
                            inFlightProvider = inFlight,
                        )
                    },
                )
            } else {
                if (sheetState.progress > 0.99f) return@AnimatedContent
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconButton(
                        onClick = {
                            bottomBarScrollConnection.expand()
                        },
                        modifier =
                            Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState("navigation_bar"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                ).size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                    ),
                                ),
                    ) {
                        Icon(
                            imageVector = currentHomePage.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .height(56.dp)
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable {
                                    bottomBarScrollConnection.expand()
                                },
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .sharedElement(
                                        sharedContentState = rememberSharedContentState("player_surface"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    ).matchParentSize()
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                        shadowElevation = 0f
                                    }.background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                            alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                        ),
                                    ),
                        )
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                    }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StableHeroCover(
                                uri = artworkUri,
                                painter = stableCoverPainter,
                                onPainterReady = { stableCoverPainter = it },
                                onPainterFailed = { stableCoverPainter = null },
                                modifier =
                                    Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState("player_cover"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                        ).fillMaxHeight()
                                        .aspectRatio(1f)
                                        .clip(coverShape),
                                placeHolder = {
                                    MusicCoverPlaceholder(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        contentDescription = stringResource(R.string.cover_placeholder),
                                    )
                                },
                            )
                            if (nowPlayingSong != null) {
                                Row(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                            .sharedBounds(
                                                sharedContentState =
                                                    rememberSharedContentState("player_info_bounds"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                resizeMode =
                                                    SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                            ).clipToBounds()
                                            .clip(CircleShape),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier =
                                            Modifier
                                                .sharedElement(
                                                    sharedContentState = rememberSharedContentState("player_title"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                ).weight(1f),
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_song_playing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                            .basicMarquee(repeatDelayMillis = 0),
                                )
                            }
                            Box(modifier = Modifier.size(40.dp)) {
                                IconButton(
                                    onClick = { playerViewModel.togglePlayPause() },
                                    modifier =
                                        Modifier
                                            .sharedElement(
                                                sharedContentState = rememberSharedContentState("player_play_button"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                            ).fillMaxSize(),
                                ) {
                                    MorphingPlayPauseIcon(
                                        isPlaying = isPlaying,
                                        playContentDescription = stringResource(R.string.play),
                                        pauseContentDescription = stringResource(R.string.pause),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier =
                                        Modifier
                                            .sharedBounds(
                                                sharedContentState =
                                                    rememberSharedContentState("player_next_button"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                resizeMode =
                                                    SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                            ).fillMaxSize()
                                            .graphicsLayer {
                                                alpha = 0f
                                            },
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            navigationPath.push(PlaylistCreatorScene())
                        },
                        modifier =
                            Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState("plus_icon"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                ).size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.tertiary.copy(
                                        alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                                    ),
                                ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StableHeroCover(
    uri: Uri?,
    painter: Painter?,
    onPainterReady: (Painter) -> Unit,
    onPainterFailed: () -> Unit,
    modifier: Modifier,
    placeHolder: @Composable () -> Unit,
) {
    StableAudioCover(
        uri = uri,
        retainedPainter = painter,
        modifier = modifier,
        onPainterReady = onPainterReady,
        onPainterFailed = onPainterFailed,
        placeHolder = placeHolder,
    )
}

@Composable
private fun HomePageSwitcher(modifier: Modifier = Modifier) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val tabs = remember { HomePage.entries.toTypedArray() }
    val selectIndex = homeViewModel.currentPage.collectAsStateWithLifecycle().value
    val tabPositions = remember { mutableStateMapOf<HomePage, Dp>() }
    val tabWidths = remember { mutableStateMapOf<HomePage, Dp>() }
    val tabHeight = remember { mutableStateMapOf<HomePage, Dp>() }
    val density = LocalDensity.current

    val indicatorOffset by animateDpAsState(
        targetValue = tabPositions.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
    )
    val indicatorWidth by animateDpAsState(
        targetValue = tabWidths.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
    )
    val indicatorHeight by animateDpAsState(
        targetValue = tabHeight.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioLowBouncy,
            ),
    )
    val indicatorColor =
        MaterialTheme.colorScheme.primaryContainer.copy(
            alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
        )

    Row(
        modifier =
            modifier
                .height(56.dp)
                .padding(end = 12.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                        alpha = BOTTOM_FLOATING_CONTAINER_ALPHA,
                    ),
                ).drawWithCache {
                    val paddingValues = 6.dp.toPx()
                    onDrawBehind {
                        if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) {
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft =
                                    Offset(
                                        indicatorOffset.toPx() + paddingValues,
                                        paddingValues,
                                    ),
                                size =
                                    Size(
                                        indicatorWidth.toPx() - 2 * paddingValues,
                                        indicatorHeight.toPx() - 2 * paddingValues,
                                    ),
                                cornerRadius =
                                    CornerRadius(
                                        100f,
                                        100f,
                                    ),
                            )
                        }
                    }
                }.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (page in tabs) {
            HomePageSwitchItem(
                modifier =
                    Modifier
                        .onGloballyPositioned {
                            tabPositions[page] = with(density) { it.positionInParent().x.toDp() }
                            tabWidths[page] = with(density) { it.size.width.toDp() }
                            tabHeight[page] = with(density) { it.size.height.toDp() }
                        }.weight(1f),
                icon = {
                    Icon(
                        page.icon,
                        contentDescription = stringResource(page.titleRes),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                title = stringResource(page.titleRes),
                bandHomePage = page,
            )
        }
    }
}

@Composable
private fun HomePageSwitchItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    title: String,
    bandHomePage: HomePage,
) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()

    val currentHomePage = homeViewModel.currentPage.collectAsStateWithLifecycle().value

    val isSelected =
        remember(currentHomePage) {
            currentHomePage == bandHomePage
        }

    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .clickable {
                    if (!isSelected) {
                        homeViewModel.navigateToPage(bandHomePage)
                    }
                }.height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            isSelected,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row {
                icon()
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
fun rememberBottomBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 50.dp,
): BottomBarScrollConnection =
    with(LocalDensity.current) {
        val scrollThresholdPx = scrollThreshold.toPx()
        remember(scrollThresholdPx, initialIsInline) {
            BottomBarScrollConnection(initialIsInline, scrollThresholdPx)
        }
    }

@Stable
class BottomBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float,
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val scrollDelta = available.y
        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }
        accumulatedScroll += scrollDelta
        if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
            isInline = true
            accumulatedScroll = 0f
        } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
            isInline = false
            accumulatedScroll = 0f
        }
        return Offset.Zero
    }
}
