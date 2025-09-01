package me.spica27.spicamusic.widget

import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSlider(
  value: Float,
  modifier: Modifier = Modifier,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  enabled: Boolean = true,
  onValueChangeFinished: (() -> Unit)? = null,
  onValueChange: (Float) -> Unit,
  @IntRange(from = 0) steps: Int = 0,
  cutLineSize: Int = 20
) {

  val sliderBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh

  val lineColor = MaterialTheme.colorScheme.onSurface.copy(.35f)


  var isPlaying by remember { mutableStateOf(false) }


  Slider(
    modifier = modifier
      .height((25).dp)
      .background(
        color = sliderBackgroundColor,
        shape = MaterialTheme.shapes.small
      ),
    value = value,
    onValueChange = {
      isPlaying = true
      onValueChange(it)
    },
    valueRange = valueRange,
    steps = steps,
    enabled = enabled,
    onValueChangeFinished = {
      isPlaying = false
      onValueChangeFinished?.invoke()
    },
    track = {
      Spacer(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer()
          .drawWithCache {

            val with = size.width / (cutLineSize - 1)

            val cornerRadius = CornerRadius(20f, 20f)

            onDrawWithContent {
              for (i in 0 until cutLineSize) {
//                if (i == 0) continue
//                if (i == cutLineSize - 1) continue
                drawLine(
                  color = lineColor,
                  Offset(
                    x = i * with,
                    y = size.height / 3
                  ),
                  Offset(
                    x = i * with,
                    y = size.height / 3 * 2
                  ),
                  strokeWidth = 2.dp.toPx()
                )
              }
            }
          }
      )
    },
    thumb = {

      Box(
        modifier = Modifier
          .fillMaxHeight(),
        contentAlignment = androidx.compose.ui.Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .height(25.dp)
            .width(25.dp)
            .background(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape
            )
            .padding(7.dp)
            .background(
              color = MaterialTheme.colorScheme.background,
              shape = CircleShape
            )
        )
      }
    },
  )
}