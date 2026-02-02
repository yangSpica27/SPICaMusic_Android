package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

// 拖拽常量配置
private const val VELOCITY_THRESHOLD = 800f // 速度阈值
private const val POSITION_THRESHOLD = 0.5f // 位置阈值
private const val MAX_DIM_ALPHA = 0.6f // 最大遮罩透明度

/**
 * 可拖拽的播放器面板 - Spotify 风格
 * 底部固定播放条，点击展开全屏播放器，从底部到全屏动画
 *
 * @param bottomPadding 底部偏移量（通常是 NavigationBar 高度）
 * @param content 应用主内容区域
 */
@Composable
fun DraggablePlayerSheet(
    bottomPadding: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    var initialPage by remember { mutableStateOf(DEFAULT_PAGE) }
    val scope = rememberCoroutineScope()

    // 进度：0 = 收起，1 = 展开
    val progress = remember { Animatable(0f) }

    // 背景遮罩透明度
    val backgroundDimAlpha by remember {
        derivedStateOf { (progress.value * MAX_DIM_ALPHA).coerceIn(0f, MAX_DIM_ALPHA) }
    }

    // 动画配置
    val springAnimationSpec: AnimationSpec<Float> =
        remember {
            spring(dampingRatio = 0.85f, stiffness = 400f)
        }

    val quickAnimationSpec: AnimationSpec<Float> =
        remember {
            tween(durationMillis = 250)
        }

    // 同步进度状态
    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            progress.animateTo(
                targetValue = if (isExpanded) 1f else 0f,
                animationSpec = springAnimationSpec,
            )
        }
    }

    // 返回键处理
    BackHandler(enabled = isExpanded) {
        scope.launch {
            isExpanded = false
        }
    }

    // 展开全屏播放器
    val expandPlayer: (Int) -> Unit =
        remember {
            { page ->
                scope.launch {
                    initialPage = page
                    isExpanded = true
                }
            }
        }

    // 收起播放器
    val collapsePlayer: () -> Unit =
        remember {
            {
                scope.launch {
                    isExpanded = false
                }
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区域
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }

        // 背景遮罩（仅全屏时显示）
        if (progress.value > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backgroundDimAlpha))
                        .zIndex(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = progress.value > 0.95f,
                            onClick = collapsePlayer,
                        ),
            )
        }

        // 底部播放条（固定位置）
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = -bottomPadding
                    }.zIndex(2f),
        ) {
            BottomPlayerBar(
                onExpand = { expandPlayer(DEFAULT_PAGE) },
                onExpandToPlaylist = { expandPlayer(0) },
            )
        }

        // 全屏播放器（从底部滑入/滑出）
        if (progress.value > 0f || isExpanded) {
            val density = LocalDensity.current
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragVelocity = 0f
                                },
                                onDragEnd = {
                                    isDragging = false
                                    scope.launch {
                                        // 根据速度和位置决定展开或收起
                                        val shouldCollapse =
                                            when {
                                                dragVelocity > VELOCITY_THRESHOLD -> true
                                                dragVelocity < -VELOCITY_THRESHOLD -> false
                                                else -> progress.value < POSITION_THRESHOLD
                                            }

                                        if (shouldCollapse) {
                                            progress.animateTo(0f, springAnimationSpec)
                                            isExpanded = false
                                        } else {
                                            progress.animateTo(1f, springAnimationSpec)
                                            isExpanded = true
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    scope.launch {
                                        progress.animateTo(
                                            targetValue = if (isExpanded) 1f else 0f,
                                            animationSpec = springAnimationSpec,
                                        )
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    dragVelocity = dragAmount
                                    scope.launch {
                                        // 只允许向下拖拽收起
                                        if (dragAmount > 0 || progress.value < 1f) {
                                            val delta = dragAmount / size.height
                                            val newProgress = (progress.value - delta).coerceIn(0f, 1f)
                                            progress.snapTo(newProgress)
                                        }
                                    }
                                },
                            )
                        }.graphicsLayer {
                            // 从底部向上滑动：0% = 完全在底部，100% = 完全展开
                            translationY = (1f - progress.value) * size.height
                        },
            ) {
                ExpandedPlayerScreen(
                    onCollapse = collapsePlayer,
                    progress = progress.value,
                    initialPage = initialPage,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
