package me.spica27.spicamusic.widget

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.readRawResource
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.wrapper.activityViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(UnstableApi::class)
@Composable
fun TunEffectBackground(modifier: Modifier = Modifier) {

  val context = LocalContext.current
  // 从资源加载GLSL字符串或使用传入的字符串
  val glsl = remember {
    readRawResource(context, R.raw.tunnel)
  }

  val playBackViewModel = activityViewModel<PlayBackViewModel>()

  val currentPlayingSong = playBackViewModel.currentSongFlow.collectAsState().value

  val currentCoverBitmap = remember {
    BitmapFactory.decodeResource(App.getInstance().resources, R.drawable.default_cover)
  }


  val bitmapShader = remember(currentCoverBitmap) {
    BitmapShader(currentCoverBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
  }


  val shader = remember(glsl, bitmapShader) {
    RuntimeShader(glsl).also {
      it.setFloatUniform("u_time", 0f)
      it.setFloatUniform("u_speed", 1.5f)
      it.setFloatUniform("u_blend", 0f)
      it.setFloatUniform("u_center_radius", 0.1f)
      it.setFloatUniform("u_center_color", 1f, 1f, 1f)
      it.setInputShader("u_tex0", bitmapShader)
//      it.setInputShader("u_tex1", bitmapShader2)
    }
  }

  var animTimeState by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(Unit) {
    val startTime = withFrameNanos { it }
    do {
      val now = withFrameNanos { it }
      animTimeState = (now - startTime) / 1.0E9f
      delay(16)
      awaitFrame()
    } while (true)
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    shader.setFloatUniform("u_resolution", size.width, size.height)
    shader.setFloatUniform("u_time", animTimeState)
    val brush = ShaderBrush(shader)
    drawRect(brush = brush)
  }

}