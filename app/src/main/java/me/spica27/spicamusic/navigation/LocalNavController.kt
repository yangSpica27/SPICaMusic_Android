package me.spica27.spicamusic.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * NavBackStack 的 CompositionLocal (Navigation 3)
 * 通过 LocalNavBackStack.current 在任意 Composable 中获取 NavBackStack
 */
val LocalNavBackStack =
    staticCompositionLocalOf<SnapshotStateList<Screen>> {
        error("NavBackStack not provided")
    }
