package me.spica27.spicamusic.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import me.spica27.spicamusic.route.Routes


//  Splash Screen
@Composable
fun SplashScreen(modifier: Modifier = Modifier, navigator: NavController) {

  val visibilityState = remember { mutableStateOf(false) }

  // 透明度动画
  val textAlphaState = animateFloatAsState(
    targetValue =
      if (visibilityState.value) 1f else 0f, label = "textAlphaState"
  )

  LaunchedEffect(Unit) {
    delay(1000) // 延迟2秒
    navigator.navigate(Routes.Main)
  }

  Scaffold { padding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Splash Screen", modifier = Modifier
          .alpha(textAlphaState.value)
          .align(alignment = Alignment.Center)
          .padding(16.dp)
      )
    }

  }

}