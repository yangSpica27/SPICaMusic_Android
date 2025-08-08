package me.spica27.spicamusic.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ObserveLifecycleEvent(onEvent: (Lifecycle.Event) -> Unit = {}) {
  // Safely update the current lambdas when a new one is provided
  val currentOnEvent = rememberUpdatedState(onEvent)
  val lifecycleOwner = LocalLifecycleOwner.current

  // If `lifecycleOwner` changes, dispose and reset the effect
  DisposableEffect(lifecycleOwner) {
    // Create an observer that triggers our remembered callbacks
    // for sending analytics events
    val observer = LifecycleEventObserver { _, event ->
      onEvent(event)
    }

    // Add the observer to the lifecycle
    lifecycleOwner.lifecycle.addObserver(observer)

    // When the effect leaves the Composition, remove the observer
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
}