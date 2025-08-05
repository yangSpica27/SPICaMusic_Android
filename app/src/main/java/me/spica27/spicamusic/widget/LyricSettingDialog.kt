package me.spica27.spicamusic.widget


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.DataStoreUtil
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingDialog(
  onDismissRequest: () -> Unit,
  dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
  dialogBackgroundColor: Color = MaterialTheme.colorScheme.background,
  textColor: Color = MaterialTheme.colorScheme.onBackground
) {

  val coroutineScope = rememberCoroutineScope()

  val fontSize = dataStoreUtil.getLyricFontSize().collectAsState(null).value

  val fontWeight = dataStoreUtil.getLyricFontWeight().collectAsState(null).value


  val delay = dataStoreUtil.getLyricDelay().collectAsState(null).value

  var isSeekFontSize by remember { mutableStateOf(false) }

  var isSeekFontWeight by remember { mutableStateOf(false) }

  var isSeekLrcSpeed by remember { mutableStateOf(false) }


  var dialogBackgroundColor by remember { mutableStateOf(dialogBackgroundColor) }

  val dialogBackgroundColorAnimValue = animateColorAsState(dialogBackgroundColor, tween(200))

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

  if (fontWeight != null && fontSize != null && delay != null) {

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
          TitleText("字号 ${fontSize}", textColor = textColorAnimValue.value)
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
          TitleText("字重 ${fontWeight}", textColor = textColorAnimValue.value)
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
          TitleText(
            if (delay > 0) {
              "延迟${delay}毫秒"
            } else {
              "加快${delay}毫秒"
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
              coroutineScope.launch {
                if (it.absoluteValue < 250) {
                  dataStoreUtil.setLyricDelay(0)
                } else if (it.absoluteValue < 500 * i) {
                  dataStoreUtil.setLyricDelay(250)
                } else if (it.absoluteValue < 750 * i) {
                  dataStoreUtil.setLyricDelay(500 * i)
                } else if (it.absoluteValue < 1000) {
                  dataStoreUtil.setLyricDelay(750 * i)
                } else {
                  dataStoreUtil.setLyricDelay(
                    (it / 1000).toInt() * 1000
                  )
                }

              }
              isSeekLrcSpeed = true
            },
            onValueChangeFinished = {
              isSeekLrcSpeed = false
            },
            valueRange = -5000f..5000f,
            steps = 1000,
          )
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