package me.spica27.spicamusic.navigator

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription

abstract class Navigator {
  val navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = Int.MAX_VALUE)

  // We use a StateFlow here to allow ViewModels to start observing navigation results before the initial composition,
  // and still get the navigation result later
  val navControllerFlow = MutableStateFlow<NavController?>(null)

  fun navigateUp() {
    navigationCommands.tryEmit(NavigationCommand.NavigateUp)
  }
}

abstract class AppComposeNavigator : Navigator() {
  abstract fun navigate(route: String, optionsBuilder: (NavOptionsBuilder.() -> Unit)? = null)
  abstract fun <T> navigateBackWithResult(key: String, result: T, route: String?)

  abstract fun popUpTo(route: String, inclusive: Boolean)
  abstract fun navigateAndClearBackStack(route: String)

  suspend fun handleNavigationCommands(navController: NavController) {
    navigationCommands
      .onSubscription { this@AppComposeNavigator.navControllerFlow.value = navController }
      .onCompletion { this@AppComposeNavigator.navControllerFlow.value = null }
      .collect { navController.handleComposeNavigationCommand(it) }
  }

  private fun NavController.handleComposeNavigationCommand(navigationCommand: NavigationCommand) {
    when (navigationCommand) {
      is ComposeNavigationCommand.NavigateToRoute -> {
        navigate(navigationCommand.route, navigationCommand.options)
      }

      NavigationCommand.NavigateUp -> navigateUp()
      is ComposeNavigationCommand.PopUpToRoute -> popBackStack(
        navigationCommand.route,
        navigationCommand.inclusive
      )

      is ComposeNavigationCommand.NavigateUpWithResult<*> -> {
        navUpWithResult(navigationCommand)
      }
    }
  }

  private fun NavController.navUpWithResult(
    navigationCommand: ComposeNavigationCommand.NavigateUpWithResult<*>
  ) {
    val backStackEntry =
      navigationCommand.route?.let { getBackStackEntry(it) }
        ?: previousBackStackEntry
    backStackEntry?.savedStateHandle?.set(
      navigationCommand.key,
      navigationCommand.result
    )

    navigationCommand.route?.let {
      popBackStack(it, false)
    } ?: run {
      navigateUp()
    }
  }
}
