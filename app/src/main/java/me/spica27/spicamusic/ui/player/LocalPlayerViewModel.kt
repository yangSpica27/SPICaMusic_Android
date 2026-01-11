package me.spica27.spicamusic.ui.player

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
