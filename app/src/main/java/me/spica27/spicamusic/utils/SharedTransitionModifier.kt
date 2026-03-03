package me.spica27.spicamusic.utils

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope

/**
 * 将 Modifier 绑定到导航共享元素容器动画。
 * @param key 与路由对应的共享状态 key，通常为 Screen 枚举/对象。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navSharedBounds(key: Any): Modifier {
    val sharedTransitionScope = LocalNavSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    return with(sharedTransitionScope) {
        sharedBounds(
            rememberSharedContentState(key),
            animatedContentScope,
        )
    }
}

/**
 * 将 Modifier 绑定到导航共享元素动画（精确像素级，适合图片等内容完全一致的场景）。
 *
 * @param key 与路由对应的共享状态 key。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.navSharedElement(key: Any): Modifier {
    val sharedTransitionScope = LocalNavSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current
    return with(sharedTransitionScope) {
        sharedElement(
            rememberSharedContentState(key),
            animatedContentScope,
        )
    }
}
