package me.spica27.spicamusic.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/**
 * NavController 的 CompositionLocal
 * 通过 LocalNavController.current 在任意 Composable 中获取 NavController
 */
val LocalNavController =
    staticCompositionLocalOf<NavHostController> {
        error("NavController not provided")
    }
