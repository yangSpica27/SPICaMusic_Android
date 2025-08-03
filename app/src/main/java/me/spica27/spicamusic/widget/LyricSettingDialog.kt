package me.spica27.spicamusic.widget


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.DataStoreUtil
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingDialog(
  onDismissRequest: () -> Unit,
  sliderBackground: Color = MaterialTheme.colorScheme.primaryContainer,
  sliderForeground: Color = MaterialTheme.colorScheme.primary,
  dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>()
) {

  val coroutineScope = rememberCoroutineScope()

  val fontSize = dataStoreUtil.getLyricFontSize().collectAsState(null).value

  val fontWeight = dataStoreUtil.getLyricFontWeight().collectAsState(null).value

  val delay = dataStoreUtil.getLyricDelay().collectAsState(null).value

  var isSeek by remember { mutableStateOf(false) }

  val dialogBackgroundColor = animateColorAsState(
    if (isSeek) {
      MaterialTheme.colorScheme.surfaceContainerLow.copy(
        .65f
      )
    } else {
      MaterialTheme.colorScheme.surfaceContainerLow
    },
    tween(
      durationMillis = 200,
      easing = EaseInOut
    )
  ).value

  if (fontWeight != null && fontSize != null && delay != null) {
    AlertDialog(
      containerColor = dialogBackgroundColor,
      shape = MaterialTheme.shapes.small,
      onDismissRequest = { onDismissRequest() },
      title = {
        Text("歌词显示设置")
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.Start,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TitleText("字号 ${fontSize}")
          SimpleSlider(
            modifier = Modifier
              .fillMaxWidth(),
            value = fontSize.toFloat(),
            onValueChange = {
              coroutineScope.launch {
                dataStoreUtil.setLyricFontSize(it.toInt())
              }
              isSeek = true
            },
            onValueChangeFinished = {
              isSeek = false
            },
            valueRange = 12f..24f,
          )
          TitleText("字重 ${fontWeight}")
          SimpleSlider(
            modifier = Modifier
              .fillMaxWidth(),
            value = fontWeight.toFloat(),
            onValueChange = {
              coroutineScope.launch {
                dataStoreUtil.setLyricFontWeight(it.toInt())
              }
              isSeek = true
            },
            onValueChangeFinished = {
              isSeek = false
            },
            valueRange = 100f..900f,
            steps = 100,
          )
          TitleText(
            if (delay > 0) {
              "延迟${delay}毫秒"
            } else {
              "加快${delay}毫秒"
            }
          )
          SimpleSlider(
            modifier = Modifier
              .fillMaxWidth(),
            value = delay.toFloat(),
            onValueChange = {
              coroutineScope.launch {
                dataStoreUtil.setLyricDelay(it.toInt())
              }
              isSeek = true
            },
            onValueChangeFinished = {
              isSeek = false
            },
            valueRange = -5000f..5000f,
            steps = 1000,
          )
          TitleText("液态玻璃")
          Checkbox(
            checked = true,
            onCheckedChange = {}
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
private fun TitleText(text: String) {
  Text(
    text,
    modifier = Modifier.fillMaxWidth(),
    style = MaterialTheme.typography.titleMedium.copy(
      fontWeight = FontWeight.W500
    )
  )
}