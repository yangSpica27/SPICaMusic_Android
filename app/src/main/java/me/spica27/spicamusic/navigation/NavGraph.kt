package me.spica27.spicamusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.spica27.spicamusic.ui.home.HomeScreen

/**
 * 应用导航图
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        // TODO: 添加更多路由
    }
}
