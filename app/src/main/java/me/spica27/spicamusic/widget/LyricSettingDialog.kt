package me.spica27.spicamusic.widget


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.LyricRepository
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.DataStoreUtil
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

/**
 * 歌词设置弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingDialog(
  onDismissRequest: () -> Unit,
  dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
  dialogBackgroundColor: Color = MaterialTheme.colorScheme.background,
  textColor: Color = MaterialTheme.colorScheme.onBackground,
  song: Song,
  navBackStack: NavBackStack? = null,
  // 窗口是否透明
  dialogBackgroundIsTranslate: (Boolean) -> Unit = {},
) {

  val coroutineScope = rememberCoroutineScope()

  // 字体大小设置
  val fontSize = dataStoreUtil.getLyricFontSize().collectAsState(null).value

  // 字重设置
  val fontWeight = dataStoreUtil.getLyricFontWeight().collectAsState(null).value

  val lyricRepository = koinInject<LyricRepository>()

  // 这首歌用户有没有绑定歌词
  val haveLyric =
    lyricRepository.getLyrics(song.mediaStoreId).map { it != null }.collectAsState(false).value
  val delay = lyricRepository.getDelay(song.mediaStoreId).collectAsState(null).value

  // 有没有在调整字号
  var isSeekFontSize by remember { mutableStateOf(false) }

  // 有没有在调整字重
  var isSeekFontWeight by remember { mutableStateOf(false) }

  // 有没有在调整歌词速度
  var isSeekLrcSpeed by remember { mutableStateOf(false) }

  // 弹窗背景颜色
  var dialogBackgroundColor by remember { mutableStateOf(dialogBackgroundColor) }

  LaunchedEffect(dialogBackgroundColor) {
    dialogBackgroundIsTranslate.invoke(dialogBackgroundColor.alpha == 1f)
  }

  val dialogBackgroundColorAnimValue = animateColorAsState(dialogBackgroundColor, tween(200))

  // 字体颜色
  var textColor by remember { mutableStateOf(textColor) }

  val textColorAnimValue = animateColorAsState(textColor, tween(200))

  var showFontWeightSlider by remember { mutableStateOf(false) }

  var showFontSizeSlider by remember { mutableStateOf(false) }

  var showSpeedSlider by remember { mutableStateOf(false) }

  LaunchedEffect(isSeekFontSize) {
    dialogBackgroundColor = if (isSeekFontSize) {
      dialogBackgroundColor.copy(alpha = 0f)
    } else {
      dialogBackgroundColor.copy(alpha = 1f)
    }
    textColor = if (isSeekFontSize) {
      textColor.copy(alpha = 0f)
    } else {
      textColor.copy(alpha = 1f)
    }
    showFontWeightSlider = !isSeekFontSize
    showSpeedSlider = !isSeekFontSize
  }

  LaunchedEffect(isSeekFontWeight) {
    dialogBackgroundColor = if (isSeekFontWeight) {
      dialogBackgroundColor.copy(alpha = 0f)
    } else {
      dialogBackgroundColor.copy(alpha = 1f)
    }
    textColor = if (isSeekFontWeight) {
      textColor.copy(alpha = 0f)
    } else {
      textColor.copy(alpha = 1f)
    }
    showFontSizeSlider = !isSeekFontWeight
    showSpeedSlider = !isSeekFontWeight
  }

  LaunchedEffect(isSeekLrcSpeed) {

  }

  if (fontWeight != null && fontSize != null) {

    AlertDialog(
      containerColor = dialogBackgroundColorAnimValue.value,
      shape = MaterialTheme.shapes.small,
      onDismissRequest = { onDismissRequest() },
      title = {
        Text("歌词显示设置", color = textColor)
      },
      text = {
        (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.Start,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TitleText("字号 $fontSize", textColor = textColorAnimValue.value)
          SimpleSlider(
            modifier = Modifier
              .fillMaxWidth()
              .alpha(
                if (showFontSizeSlider) {
                  1f
                } else {
                  0f
                }
              ),
            value = fontSize.toFloat(),
            onValueChange = {
              coroutineScope.launch {
                dataStoreUtil.setLyricFontSize(it.toInt())
              }
              isSeekFontSize = true
            },
            onValueChangeFinished = {
              isSeekFontSize = false
            },
            valueRange = 12f..24f,
          )
          TitleText("字重 $fontWeight", textColor = textColorAnimValue.value)
          SimpleSlider(
            modifier = Modifier
              .fillMaxWidth()
              .alpha(
                if (showFontWeightSlider) {
                  1f
                } else {
                  0f
                }
              ),
            value = fontWeight.toFloat(),
            onValueChange = {
              coroutineScope.launch {
                dataStoreUtil.setLyricFontWeight(it.toInt())
              }
              isSeekFontWeight = true
            },
            onValueChangeFinished = {
              isSeekFontWeight = false
            },
            valueRange = 100f..900f,
            steps = 100,
          )

          if (haveLyric && delay != null) {
            TitleText(
              if (delay > 0) {
                "延迟${delay.absoluteValue}毫秒"
              } else {
                "加快${delay.absoluteValue}毫秒"
              },
              textColor = textColorAnimValue.value
            )
            SimpleSlider(
              modifier = Modifier
                .fillMaxWidth()
                .alpha(
                  if (showSpeedSlider) {
                    1f
                  } else {
                    0f
                  }
                ),
              value = delay.toFloat(),
              onValueChange = {
                val i = if (it > 0) {
                  1
                } else {
                  -1
                }
                coroutineScope.launch(Dispatchers.IO) {
                  if (it.absoluteValue < 250) {
                    lyricRepository.setDelay(song.mediaStoreId, 0)
                  } else if (it.absoluteValue < 500 * i) {
                    lyricRepository.setDelay(song.mediaStoreId, 250L * i)
                  } else if (it.absoluteValue < 750 * i) {
                    lyricRepository.setDelay(song.mediaStoreId, 500L * i)
                  } else if (it.absoluteValue < 1000) {
                    lyricRepository.setDelay(song.mediaStoreId, 750L * i)
                  } else {
                    lyricRepository.setDelay(
                      song.mediaStoreId,
                      (it / 1000).toInt() * 1000L
                    )
                  }

                }
                isSeekLrcSpeed = true
              },
              onValueChangeFinished = {
                isSeekLrcSpeed = false
              },
              valueRange = -8000f..8000f,
              steps = 1000,
            )
          }
          TextButton(
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.textButtonColors().copy(
              contentColor = textColorAnimValue.value
            ),
            onClick = {
              onDismissRequest.invoke()
              navBackStack?.add(Routes.LyricsSearch(song = song))
            }
          ) {
            Text("切换其他版本歌词")
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          // 确认
          onDismissRequest.invoke()
        }) {
          Text("确定")
        }
      })
  }

}


@Composable
private fun TitleText(text: String, textColor: Color) {
  Text(
    text,
    modifier = Modifier.fillMaxWidth(),
    style = MaterialTheme.typography.titleMedium.copy(
      fontWeight = FontWeight.W500,
      color = textColor
    )
  )
}