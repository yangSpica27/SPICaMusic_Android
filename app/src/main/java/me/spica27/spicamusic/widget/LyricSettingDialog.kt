package me.spica27.spicamusic.widget


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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingDialog(
  onDismissRequest: () -> Unit,
  sliderBackground: Color = MaterialTheme.colorScheme.primaryContainer,
  sliderForeground: Color = MaterialTheme.colorScheme.primary,
) {

  var fontSize by remember { mutableIntStateOf(16) }

  var fontWeight by remember { mutableIntStateOf(100) }

  AlertDialog(
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
            fontSize = it.toInt()
          },
          onValueChangeFinished = {

          },
          valueRange = 12f..24f,
        )
        TitleText("字重 ${fontWeight}")
        SimpleSlider(
          modifier = Modifier
            .fillMaxWidth(),
          value = fontWeight.toFloat(),
          onValueChange = {
            fontWeight = it.toInt()
          },
          valueRange = 100f..900f,
          steps = 100,
        )
        TitleText("延迟 ${fontWeight}")
        SimpleSlider(
          modifier = Modifier
            .fillMaxWidth(),
          value = fontWeight.toFloat(),
          onValueChange = {
            fontWeight = it.toInt()
          },
          valueRange = 100f..900f,
          steps = 100,
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