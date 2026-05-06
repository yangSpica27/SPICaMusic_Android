package me.spica27.spicamusic.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope
import kotlin.math.pow

private enum class PlayerSheetValue { Collapsed, Expanded }

/**
 * 可拖拽的播放器面板
 * 底部固定播放条，用户可通过上划手势连续过渡到全屏播放器
 * @param content 应用主内容区域
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DraggablePlayerSheet(content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }

    // 记录迷你播放条实际高度（含导航栏内边距），首帧用估算值避免闪烁
    var miniBarHeightPx by remember { mutableStateOf(with(density) { 120.dp.roundToPx() }) }

    val draggableState =
        remember {
            AnchoredDraggableState(
                initialValue = PlayerSheetValue.Collapsed,
                snapAnimationSpec =
                    tween(
                        durationMillis = 400,
                        easing = { fraction ->
                            // 自定义缓动曲线：前半段加速，后半段减速
                            if (fraction < 0.5f) {
                                4 * fraction * fraction * fraction // 加速立方曲线
                            } else {
                                1 - (-2 * fraction + 2).pow(3) / 2 // 减速立方曲线
                            }
                        },
                    ),
                decayAnimationSpec = exponentialDecay(),
                positionalThreshold = { totalDistance -> totalDistance * 0.45f },
                velocityThreshold = { with(density) { 150.dp.toPx() } },
            )
        }

    with(LocalNavSharedTransitionScope.current) {
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

            BackHandler(enabled = isExpanded) {
                coroutineScope.launch { draggableState.animateTo(PlayerSheetValue.Collapsed) }
            }

            // 主内容区域（在 Sheet 背后）
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }

            // 可拖拽播放器面板（通过 translationY 在屏幕上滑入/滑出）
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
                // 全屏播放器（随进度淡入，progress > 0 时才合成以节省开销）
                if (progress > 0.01f) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = ((progress - 0.15f) / 0.55f).coerceIn(0f, 1f) }
                                .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 11f),
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

                // 迷你播放条 —— 位于面板顶部
                // 当面板收起时，面板整体下移使迷你条恰好出现在屏幕底部
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .navigationBarsPadding()
                            .onSizeChanged { size -> miniBarHeightPx = size.height }
                            .renderInSharedTransitionScopeOverlay(zIndexInOverlay = 12f)
                            .graphicsLayer { alpha = (1f - progress / 0.25f).coerceIn(0f, 1f) }
                            // 拖拽手势：上划展开，下划收起
                            .anchoredDraggable(draggableState, Orientation.Vertical),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                        LargeBottomPlayerBar(
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
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
