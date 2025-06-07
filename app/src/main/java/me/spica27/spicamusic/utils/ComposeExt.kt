package me.spica27.spicamusic.utils

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.navigation3.ui.NavDisplay


fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
  this.clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }) {
    onClick()
  }
}

fun sliderFromBottomRouteAnim() = NavDisplay.transitionSpec {
  slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(450)
  ) togetherWith ExitTransition.KeepUntilTransitionsFinished
} + NavDisplay.popTransitionSpec {
  EnterTransition.None togetherWith
      slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(450)
      )
} + NavDisplay.predictivePopTransitionSpec {
  EnterTransition.None togetherWith
      slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(450)
      )
}