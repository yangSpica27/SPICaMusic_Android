package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * PlayerViewModel 的 CompositionLocal
 * 在 Activity 级别创建，全局共享
 * 通过 LocalPlayerViewModel.current 在任意 Composable 中获取
 */
val LocalPlayerViewModel =
    staticCompositionLocalOf<PlayerViewModel> {
        error("PlayerViewModel not provided")
    }

/**
 * 全局底部 Padding 状态
 * 用于各页面动态设置底部间距（如有 NavigationBar 时）
 *
 * 使用方式：
 * 1. 在有 NavigationBar 的页面：
 *    val bottomPaddingState = LocalBottomPaddingState.current
 *    NavigationBar(modifier = Modifier.onSizeChanged { bottomPaddingState.floatValue = it.height.toFloat() })
 *
 * 2. 在没有 NavigationBar 的页面：
 *    val bottomPaddingState = LocalBottomPaddingState.current
 *    LaunchedEffect(Unit) { bottomPaddingState.floatValue = 0f }
 */
val LocalBottomPaddingState =
    staticCompositionLocalOf<MutableFloatState> {
        error("BottomPaddingState not provided")
    }
