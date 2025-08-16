package me.spica27.spicamusic.widget

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay


@Composable
fun BackPress(
  navigator: NavBackStack? = null
) {
  var backPress: BackPressState by remember { mutableStateOf(BackPressState.Idle) }

  // 显示是否退出的提示
  var showToast by remember { mutableStateOf(false) }

  // 获取当前的context
  val context = LocalContext.current

  LaunchedEffect(showToast) {
    if (showToast) {
      Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
      showToast = false
    }
  }

  LaunchedEffect(key1 = backPress) {
    if (backPress == BackPressState.InitialTouch) {
      delay(2000)
      backPress = BackPressState.Idle
    }
  }


  DisposableEffect(Unit) {
    onDispose {
      backPress = BackPressState.Idle
    }
  }

  BackHandler(
    enabled = backPress == BackPressState.Idle && navigator?.size == 1,
    onBack = {
      backPress = BackPressState.InitialTouch
      showToast = true
    }
  )
}

private sealed class BackPressState {
  object Idle : BackPressState()
  object InitialTouch : BackPressState()
}
