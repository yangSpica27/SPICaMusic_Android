package me.spica27.spicamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens

@Composable
fun SplashScreen(modifier: Modifier = Modifier, navigator: AppComposeNavigator) {

  LaunchedEffect(Unit) {
    delay(1000) // 延迟2秒
    navigator.navigate(AppScreens.Main.route)
  }

  Scaffold { padding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
      Text(
        text = "Splash Screen", modifier = Modifier
          .align(alignment = androidx.compose.ui.Alignment.Center)
          .padding(16.dp)
      )
    }

  }

}