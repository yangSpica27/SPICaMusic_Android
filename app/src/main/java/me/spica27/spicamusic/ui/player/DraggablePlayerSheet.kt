package me.spica27.spicamusic.ui.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * 可拖拽的播放器面板
 * 支持三种状态：收起、迷你播放条、全屏
 *
 * @param viewModel 播放器 ViewModel
 * @param bottomBarHeight 底部导航栏高度（像素）
 * @param content 主内容区域
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

    // 进度值：0 = 迷你模式，1 = 完全展开
    val progress = remember { Animatable(0f) }

    // 动画规格
    val animationSpec: AnimationSpec<Float> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 300f,
        )

    // 根据 isExpanded 状态更新进度（仅在非拖拽时）
    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            if (isExpanded) {
                progress.animateTo(1f, animationSpec)
            } else {
                progress.animateTo(0f, animationSpec)
            }
        }
    }

    // 返回键处理
    BackHandler(enabled = isExpanded) {
        scope.launch {
            isExpanded = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()

        SubcomposeLayout(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                isDragging = false
                                scope.launch {
                                    // 根据当前进度决定是展开还是收起
                                    // 阈值设为 0.5（中点）
                                    val targetProgress = if (progress.value > 0.5f) 1f else 0f
                                    progress.animateTo(targetProgress, animationSpec)
                                    isExpanded = targetProgress == 1f
                                }
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch {
                                    val targetProgress = if (isExpanded) 1f else 0f
                                    progress.animateTo(targetProgress, animationSpec)
                                }
                                dragOffset = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                                scope.launch {
                                    // 计算可拖动的总距离（从迷你模式到完全展开）
                                    val totalDragDistance = size.height - bottomPadding

                                    // 根据拖动偏移更新进度
                                    // 向上拖动（负值）增加进度，向下拖动（正值）减少进度
                                    val currentProgress = progress.value
                                    val deltaProgress = -dragOffset / totalDragDistance
                                    val newProgress = (currentProgress + deltaProgress).coerceIn(0f, 1f)

                                    progress.snapTo(newProgress)
                                    dragOffset = 0f // 重置偏移量，避免累积
                                }
                            },
                        )
                    },
        ) { constraints: Constraints ->

            val looseConstraints =
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                )

            val miniPlayerPlaceables =
                subcompose("MiniPlayer") {
                    BottomPlayerBar(
                        onExpand = {
                            isExpanded = !isExpanded
                        },
                    )
                }.map { it.measure(looseConstraints) }

            val fullPlayerPlaceables =
                subcompose("FullPlayer") {
                    ExpandedPlayerScreen(
                        onCollapse = {
                            isExpanded = !isExpanded
                        },
                    )
                }.map {
                    it.measure(
                        looseConstraints.copy(
                            maxHeight =
                                miniPlayerPlaceables.first().height +
                                    ((constraints.maxHeight - miniPlayerPlaceables.first().height) * progress.value).toInt(),
                        ),
                    )
                }

            layout(
                width = constraints.maxWidth,
                height = constraints.maxHeight,
            ) {
                miniPlayerPlaceables.forEach {
                    // 播放条应该紧贴 NavigationBar 上方
                    // Y坐标 = 屏幕高度 - NavigationBar高度 - 播放条实际高度
                    val yPosition = constraints.maxHeight - bottomPadding.toInt() - it.height
                    it.place(0, yPosition)
                }

                val yPosition2 =
                    (constraints.maxHeight - miniPlayerPlaceables.first().height - bottomPadding.toInt()) -
                        ((constraints.maxHeight - miniPlayerPlaceables.first().height - bottomPadding.toInt()) * progress.value).toInt()

                Log.d("DraggablePlayerSheet", "yPosition2: $yPosition2")

                fullPlayerPlaceables.forEach {
                    it.place(0, yPosition2)
                }
            }
        }
    }
}
