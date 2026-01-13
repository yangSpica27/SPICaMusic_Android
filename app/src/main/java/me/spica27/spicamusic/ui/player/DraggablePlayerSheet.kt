package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.SubcomposeLayout
import kotlinx.coroutines.launch

// 拖拽常量配置
private const val DRAG_DISTANCE = 800f
private const val POSITION_THRESHOLD = 0.4f
private const val VELOCITY_THRESHOLD = 50f
private const val EDGE_DAMPING = 0.3f
private const val EDGE_THRESHOLD = 0.05f
private const val MAX_DIM_ALPHA = 0.5f
private const val CONTENT_DIM_FACTOR = 0.6f
private const val MINI_SHADOW_ELEVATION = 8f
private const val MIN_SCALE = 0.98f

/**
 * 可拖拽的播放器面板 - 支持手势拖拽的迷你播放条与全屏播放器切换
 *
 * @param viewModel 播放器 ViewModel（当前未使用，保留接口用于后续扩展）
 * @param bottomPadding 底部偏移量（通常是 NavigationBar 高度）
 * @param content 应用主内容区域
 */
@Composable
fun DraggablePlayerSheet(
    viewModel: PlayerViewModel,
    bottomPadding: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    val backgroundDimAlpha by remember {
        derivedStateOf { (progress.value * MAX_DIM_ALPHA).coerceIn(0f, MAX_DIM_ALPHA) }
    }

    val springAnimationSpec: AnimationSpec<Float> =
        remember {
            spring(dampingRatio = 0.85f, stiffness = 400f)
        }

    val quickAnimationSpec: AnimationSpec<Float> =
        remember {
            tween(durationMillis = 300)
        }

    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            progress.animateTo(
                targetValue = if (isExpanded) 1f else 0f,
                animationSpec = springAnimationSpec,
            )
        }
    }

    BackHandler(enabled = isExpanded) {
        scope.launch { isExpanded = false }
    }

    val handleDragStart: () -> Unit =
        remember {
            {
                isDragging = true
                dragOffset = 0f
            }
        }

    val handleDragEnd: () -> Unit =
        remember {
            {
                isDragging = false
                scope.launch {
                    val targetProgress =
                        when {
                            progress.value > 0.2f && dragOffset < -VELOCITY_THRESHOLD -> 1f
                            progress.value < 0.8f && dragOffset > VELOCITY_THRESHOLD -> 0f
                            else -> if (progress.value > POSITION_THRESHOLD) 1f else 0f
                        }
                    progress.animateTo(targetProgress, springAnimationSpec)
                    isExpanded = targetProgress == 1f
                }
                dragOffset = 0f
            }
        }

    val handleDragCancel: () -> Unit =
        remember {
            {
                isDragging = false
                scope.launch {
                    progress.animateTo(
                        targetValue = if (isExpanded) 1f else 0f,
                        animationSpec = springAnimationSpec,
                    )
                }
                dragOffset = 0f
            }
        }

    val handleDrag: (Float) -> Unit =
        remember {
            { dragAmount ->
                scope.launch {
                    val currentProgress = progress.value
                    val deltaProgress = -dragAmount / DRAG_DISTANCE

                    val damping =
                        when {
                            currentProgress <= EDGE_THRESHOLD && deltaProgress < 0 -> EDGE_DAMPING
                            currentProgress >= (1f - EDGE_THRESHOLD) && deltaProgress > 0 -> EDGE_DAMPING
                            else -> 1f
                        }

                    val newProgress = (currentProgress + deltaProgress * damping).coerceIn(0f, 1f)
                    progress.snapTo(newProgress)
                }
                dragOffset += dragAmount
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(1f - backgroundDimAlpha * CONTENT_DIM_FACTOR),
        ) {
            content()
        }

        if (progress.value > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(backgroundDimAlpha),
            )
        }

        SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            val miniPlayerPlaceables =
                subcompose("MiniPlayer") {
                    BottomPlayerBar(
                        onExpand = {
                            scope.launch {
                                isExpanded = true
                                progress.animateTo(1f, quickAnimationSpec)
                            }
                        },
                        onDragStart = handleDragStart,
                        onDragEnd = handleDragEnd,
                        onDragCancel = handleDragCancel,
                        onDrag = handleDrag,
                        progress = progress.value,
                    )
                }.map { it.measure(looseConstraints) }

            val miniPlayerHeight = miniPlayerPlaceables.first().height
            val fullPlayerMaxHeight = constraints.maxHeight
            val fullPlayerHeight = miniPlayerHeight + ((fullPlayerMaxHeight - miniPlayerHeight) * progress.value).toInt()

            val fullPlayerPlaceables =
                subcompose("FullPlayer") {
                    ExpandedPlayerScreen(
                        onCollapse = {
                            scope.launch {
                                isExpanded = false
                                progress.animateTo(0f, quickAnimationSpec)
                            }
                        },
                        onDragStart = handleDragStart,
                        onDragEnd = handleDragEnd,
                        onDragCancel = handleDragCancel,
                        onDrag = handleDrag,
                        progress = progress.value,
                    )
                }.map { it.measure(looseConstraints.copy(maxHeight = fullPlayerHeight)) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val miniPlayerY = constraints.maxHeight - bottomPadding.toInt() - miniPlayerHeight
                val fullPlayerY = (miniPlayerY * (1f - progress.value)).toInt()

                if (progress.value < 0.99f) {
                    miniPlayerPlaceables.forEach {
                        it.placeWithLayer(0, miniPlayerY) {
                            shadowElevation = MINI_SHADOW_ELEVATION * (1f - progress.value)
                            alpha = 1f - progress.value
                        }
                    }
                }

                if (progress.value > 0.01f) {
                    fullPlayerPlaceables.forEach {
                        it.placeWithLayer(0, fullPlayerY) {
                            alpha = progress.value
                            val scale = MIN_SCALE + progress.value * (1f - MIN_SCALE)
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                }
            }
        }
    }
}
