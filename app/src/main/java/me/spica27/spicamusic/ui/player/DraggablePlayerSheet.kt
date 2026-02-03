package me.spica27.spicamusic.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import kotlin.math.roundToInt

// 容器过渡动画配置
private const val CONTAINER_TRANSFORM_DURATION = 450

// 底部播放条隐藏时的水平偏移量（屏幕外）
private val PLAYER_BAR_HIDDEN_OFFSET = 400.dp

// 提供 SharedTransitionScope 的 CompositionLocal
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * 可拖拽的播放器面板 - 容器过渡版本
 * 底部固定播放条，点击展开全屏播放器，使用 Material 容器过渡动画
 * @param content 应用主内容区域
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DraggablePlayerSheet(content: @Composable BoxScope.() -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var initialPage by remember { mutableIntStateOf(DEFAULT_PAGE) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 获取导航栈，判断当前路由
    val backStack = LocalNavBackStack.current
    val isOnHomePage by remember {
        derivedStateOf { backStack.lastOrNull() == Screen.Home }
    }

    // NavigationBar 高度 + 系统导航栏高度
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottomPadding = navigationBarPadding.calculateBottomPadding()

    // 底部播放条的垂直偏移
    val bottomBarHeight = 64.dp

    // 播放条位置动画
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val horizontalOffset by animateDpAsState(
        targetValue = if (isOnHomePage || isExpanded) 0.dp else screenWidth + PLAYER_BAR_HIDDEN_OFFSET,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "playerBarHorizontalOffset",
    )

    // 播放条透明度动画
    val playerBarAlpha by animateFloatAsState(
        targetValue = if (isOnHomePage || isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "playerBarAlpha",
    )

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
                                        // 主页时：向上偏移 NavigationBar 高度
                                        // 其他页面时：向右偏移到屏幕外
                                        val verticalOffset = -(bottomBarHeight + navBarBottomPadding).toPx()
                                        IntOffset(
                                            x = with(density) { horizontalOffset.toPx().roundToInt() },
                                            y = verticalOffset.roundToInt(),
                                        )
                                    }.graphicsLayer {
                                        alpha = playerBarAlpha
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
