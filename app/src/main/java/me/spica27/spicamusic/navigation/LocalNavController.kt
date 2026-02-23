package me.spica27.spicamusic.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * NavBackStack 的 CompositionLocal (Navigation 3)
 * 通过 LocalNavBackStack.current 在任意 Composable 中获取 NavBackStack
 */
val LocalNavBackStack =
    staticCompositionLocalOf<NavBackStack<NavKey>> {
        error("NavBackStack not provided")
    }
