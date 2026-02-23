package me.spica27.spicamusic.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import me.spica27.spicamusic.R
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope
import me.spica27.spicamusic.ui.LocalSurfaceHazeState
import me.spica27.spicamusic.ui.widget.FloatingTabBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 容器过渡动画配置
private const val CONTAINER_TRANSFORM_DURATION = 450

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

    val backStack = LocalNavBackStack.current

    with(LocalNavSharedTransitionScope.current) {
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
                                .renderInSharedTransitionScopeOverlay(
                                    zIndexInOverlay = 11f,
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
                    Column(
                        modifier =
                            Modifier
                                .renderInSharedTransitionScopeOverlay(
                                    zIndexInOverlay = 11f,
                                ).fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                    ) {
                        // TabBar
                        FloatingTabBar(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            scrollConnection = LocalFloatingTabBarScrollConnection.current,
                            selectedTabKey = LocalNavBackStack.current.last(),
                            tabBarContentModifier =
                                Modifier.hazeEffect(
                                    LocalSurfaceHazeState.current,
                                    style =
                                        HazeMaterials.ultraThin(
                                            MiuixTheme.colorScheme.surfaceContainer,
                                        ),
                                ),
                            inlineAccessory = { modifier, animatedVisibilityScope ->
                                SmallBottomPlayerBar(
                                    modifier =
                                        modifier.sharedBounds(
                                            sharedContentState = rememberSharedContentState(key = "player_bar"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ ->
                                                spring(
                                                    dampingRatio = 0.9f,
                                                    stiffness = 380f,
                                                )
                                            },
                                        ),
                                )
                            },
                            expandedAccessory = { modifier, animatedVisibilityScope ->
                                Box(
                                    modifier =
                                        modifier
                                            .renderInSharedTransitionScopeOverlay(
                                                zIndexInOverlay = 10f,
                                            ).sharedBounds(
                                                sharedContentState = rememberSharedContentState(key = "player_bar"),
                                                animatedVisibilityScope = animatedVisibilityScope,
                                                boundsTransform = { _, _ ->
                                                    spring(
                                                        dampingRatio = 0.9f,
                                                        stiffness = 380f,
                                                    )
                                                },
                                            ),
                                ) {
                                    LargeBottomPlayerBar(
                                        onExpand = { expandPlayer(DEFAULT_PAGE) },
                                        onExpandToPlaylist = { expandPlayer(0) },
                                    )
                                }
                            },
                        ) {
                            val tabTint = @Composable { isSelected: Boolean ->
                                if (isSelected) MiuixTheme.colorScheme.onTertiaryContainer else MiuixTheme.colorScheme.onSurface
                            }

                            tab(
                                key = Screen.Library,
                                title = {
                                    Text(
                                        "媒体库",
                                        style = MiuixTheme.textStyles.button,
                                        color =
                                            tabTint(
                                                LocalNavBackStack.current.last() == Screen.Library,
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint =
                                            tabTint(
                                                LocalNavBackStack.current.last() == Screen.Library,
                                            ),
                                    )
                                },
                                onClick = {
                                    backStack.add(Screen.Library)
                                    backStack.removeAt(
                                        backStack.lastIndexOf(Screen.Library) - 1,
                                    )
                                },
                            )

                            tab(
                                key = Screen.Settings,
                                title = {
                                    Text(
                                        stringResource(R.string.settings),
                                        style = MiuixTheme.textStyles.button,
                                        color =
                                            tabTint(
                                                LocalNavBackStack.current.last() == Screen.Settings,
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        tint =
                                            tabTint(
                                                LocalNavBackStack.current.last() == Screen.Settings,
                                            ),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    backStack.add(Screen.Settings)
                                    backStack.removeAt(
                                        backStack.lastIndexOf(Screen.Settings) - 1,
                                    )
                                },
                            )

                            // Standalone tab
                            standaloneTab(
                                key = Screen.Search,
                                icon = {
                                    Icon(
                                        Icons.Default.Search,
                                        tint =
                                            tabTint(
                                                LocalNavBackStack.current.last() == Screen.Search,
                                            ),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    backStack.add(Screen.Search)
                                    backStack.removeAt(
                                        backStack.lastIndexOf(Screen.Search) - 1,
                                    )
                                },
                            )
                        }
                        Spacer(
                            modifier = Modifier.height(16.dp),
                        )
                    }
                }
            }
        }
    }
}
