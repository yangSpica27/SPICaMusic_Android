package me.spica27.spicamusic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Stable
class AppNavigator internal constructor(
    private val screenBackStack: NavBackStack<NavKey>,
) {
    // Callback-bearing dialogs are transient; only screen destinations enter saved state.
    private val dialogBackStack = mutableStateListOf<DialogRoute>()

    val entries: List<Route>
        get() =
            buildList(screenBackStack.size + dialogBackStack.size) {
                screenBackStack.forEach { key ->
                    add(key as? ScreenRoute ?: error("Unexpected navigation key: $key"))
                }
                addAll(dialogBackStack)
            }

    fun add(route: Route): Boolean =
        when (route) {
            is ScreenRoute -> {
                dialogBackStack.clear()
                if (screenBackStack.lastOrNull() == route) {
                    false
                } else {
                    screenBackStack.add(route)
                }
            }

            is DialogRoute -> dialogBackStack.add(route)
        }

    fun removeLastOrNull(): Route? {
        if (dialogBackStack.isNotEmpty()) {
            return dialogBackStack.removeAt(dialogBackStack.lastIndex)
        }
        if (screenBackStack.size <= 1) return null
        return screenBackStack.removeAt(screenBackStack.lastIndex) as ScreenRoute
    }

    fun lastOrNull(): Route? =
        dialogBackStack.lastOrNull()
            ?: lastScreenOrNull()

    /** 最上层全屏页面；弹窗不会遮住整个页面，因此不影响背景特效的可见性判断。 */
    fun lastScreenOrNull(): ScreenRoute? = screenBackStack.lastOrNull() as? ScreenRoute

    fun none(predicate: (Route) -> Boolean): Boolean = entries.none(predicate)
}

@Composable
fun rememberAppNavigator(): AppNavigator {
    val screenBackStack = rememberNavBackStack(HomeRoute)
    return remember(screenBackStack) { AppNavigator(screenBackStack) }
}

val LocalBackStack =
    staticCompositionLocalOf<AppNavigator> {
        error("No AppNavigator provided.")
    }
