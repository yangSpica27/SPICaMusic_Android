package me.spica27.spicamusic.ui.home

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.ui.player.DEFAULT_PAGE
import me.spica27.spicamusic.ui.player.ExpandedPlayerScreen
import me.spica27.spicamusic.ui.player.LargeBottomPlayerBar
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScene
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.widget.highLightClickable
import org.koin.compose.viewmodel.koinActivityViewModel

/** 播放器面板的两个锚点状态 */
private enum class PlayerSheetValue { Collapsed, Expanded }

/**
 * 底部媒体控制栏
 * - 收起状态：显示迷你播放条 + 首页 Tab 切换区
 * - 展开状态：全屏播放器
 * 支持点击展开和上划/下划连续拖拽手势
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BottomMediaBar(sky: Sky) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val navigationPath = LocalNavigationPath.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 记录迷你播放条实际高度（含导航栏内边距），首帧用估算值避免闪烁
    var miniBarHeightPx by remember { mutableStateOf(with(density) { 120.dp.roundToPx() }) }

    // 记录跳转到播放器的初始页（默认主页 or 播放列表页）
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }

    // 可拖拽锚点状态
    val draggableState =
        remember {
            AnchoredDraggableState(
                initialValue = PlayerSheetValue.Collapsed,
                snapAnimationSpec = tween(400, easing = EaseInOutCubic),
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

        // 展开进度：0.0 = 完全收起，1.0 = 完全展开
        val progress by remember {
            derivedStateOf {
                val offset = draggableState.offset
                val maxOffset = screenHeightPx - miniBarHeightPx
                if (offset.isNaN() || maxOffset <= 0f) return@derivedStateOf 0f
                (1f - offset / maxOffset).coerceIn(0f, 1f)
            }
        }

        val isExpanded by remember {
            derivedStateOf { draggableState.currentValue == PlayerSheetValue.Expanded }
        }

        // 展开时拦截系统返回键，将播放器收起
        BackHandler(enabled = isExpanded) {
            coroutineScope.launch { draggableState.animateTo(PlayerSheetValue.Collapsed) }
        }

        // 可拖拽播放器面板（通过 translationY 在屏幕上滑入 / 滑出）
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .graphicsLayer {
                        translationY =
                            if (draggableState.offset.isNaN()) {
                                (screenHeightPx - miniBarHeightPx)
                            } else {
                                draggableState.offset
                            }
                    },
        ) {
            // 全屏播放器（随进度淡入，progress > 0.01 时才合成以节省开销）
            if (progress > 0.01f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {},
                ) {
                    ExpandedPlayerScreen(
                        onCollapse = {
                            coroutineScope.launch {
                                draggableState.animateTo(PlayerSheetValue.Collapsed)
                            }
                        },
                        progress = progress,
                        initialPage = initialPage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 迷你播放条 + Tab 切换区 —— 位于面板顶部
            // 当面板收起时，面板整体下移使迷你条恰好出现在屏幕底部
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // onSizeChanged 必须在 animateContentSize 外侧：
                        // 这样它看到的是动画插值后的居间高度，miniBarHeightPx
                        // 随动画逐帧平滑更新，锚点也跟着平滑变化，消除闪现
                        .onSizeChanged { size -> miniBarHeightPx = size.height }
                        .animateContentSize(
                            animationSpec =
                                spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                ),
                        ).align(Alignment.TopCenter)
                        .navigationBarsPadding()
                        .graphicsLayer { alpha = (1f - progress / 0.25f).coerceIn(0f, 1f) }
                        // 拖拽手势：上划展开，下划收起
                        .anchoredDraggable(draggableState, Orientation.Vertical),
            ) {
                // 实际播放条
                LargeBottomPlayerBar(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .clip(CircleShape)
                            .cloudy(
                                sky = sky,
                                radius = 64,
                                tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                            ),
                    onExpand = {
                        initialPage = DEFAULT_PAGE
                        coroutineScope.launch {
                            draggableState.animateTo(PlayerSheetValue.Expanded)
                        }
                    },
                    onExpandToPlaylist = {
                        initialPage = 0
                        coroutineScope.launch {
                            draggableState.animateTo(PlayerSheetValue.Expanded)
                        }
                    },
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
                        modifier = Modifier.weight(1f),
                        sky = sky,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .cloudy(
                                    sky = sky,
                                    radius = 62,
                                    tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                                ).highLightClickable {
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
    }
}

@Composable
private fun HomePageSwitcher(
    modifier: Modifier = Modifier,
    sky: Sky,
) {
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
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier =
            modifier
                .height(48.dp)
                .padding(end = 12.dp)
                .clip(CircleShape)
                .cloudy(
                    sky = sky,
                    radius = 62,
                    tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                ).drawWithCache {
                    onDrawBehind {
                        if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) {
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft =
                                    Offset(
                                        indicatorOffset.toPx(),
                                        0f,
                                    ),
                                size =
                                    Size(
                                        indicatorWidth.toPx(),
                                        indicatorHeight.toPx(),
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
                        Icons.Default.Home,
                        contentDescription = "Discover",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                title = page.title,
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
                .highLightClickable {
                    if (!isSelected) {
                        homeViewModel.navigateToPage(bandHomePage)
                    }
                }.height(48.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
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
