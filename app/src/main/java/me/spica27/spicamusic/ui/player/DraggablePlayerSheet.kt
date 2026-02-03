package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

// 容器过渡动画配置
private const val CONTAINER_TRANSFORM_DURATION = 450

// 提供 SharedTransitionScope 的 CompositionLocal
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * 可拖拽的播放器面板 - 容器过渡版本
 * 底部固定播放条，点击展开全屏播放器，使用 Material 容器过渡动画
 *
 * @param bottomPadding 底部偏移量（通常是 NavigationBar 高度）
 * @param content 应用主内容区域
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DraggablePlayerSheet(
    bottomPadding: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 展开全屏播放器
    val expandPlayer: (Int) -> Unit =
        remember {
            { page ->
                initialPage = page
                isExpanded = true
            }
        }

    // 收起播放器
    val collapsePlayer: () -> Unit =
        remember {
            {
                isExpanded = false
            }
        }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 主内容区域
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }

                // 使用 AnimatedContent 实现容器过渡
                AnimatedContent(
                    targetState = isExpanded,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(2f),
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = CONTAINER_TRANSFORM_DURATION,
                                    delayMillis = if (targetState) 0 else 100,
                                ),
                        ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(
                                        durationMillis = CONTAINER_TRANSFORM_DURATION / 2,
                                    ),
                            )
                    },
                    label = "PlayerContainerTransform",
                ) { expanded ->
                    if (expanded) {
                        // 返回键处理 - 放在展开的播放器内部，确保优先级最高
                        BackHandler(enabled = true) {
                            isExpanded = false
                        }

                        // 全屏播放器
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "player_container"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = { _, _ ->
                                            spring(
                                                dampingRatio = 0.9f,
                                                stiffness = 380f,
                                            )
                                        },
                                    ),
                        ) {
                            ExpandedPlayerScreen(
                                onCollapse = collapsePlayer,
                                progress = 1f,
                                initialPage = initialPage,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        // 底部播放条
                        Box(
                            modifier =
                                Modifier
                                    .offset {
                                        IntOffset(0, -bottomPadding.roundToInt())
                                    }.sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "player_container"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = { _, _ ->
                                            spring(
                                                dampingRatio = 0.9f,
                                                stiffness = 380f,
                                            )
                                        },
                                    ),
                        ) {
                            BottomPlayerBar(
                                onExpand = { expandPlayer(DEFAULT_PAGE) },
                                onExpandToPlaylist = { expandPlayer(0) },
                            )
                        }
                    }
                }
            }
        }
    }
}
