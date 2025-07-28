package me.spica27.spicamusic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.launch
import me.spica27.spicamusic.dsp.Equalizer
import me.spica27.spicamusic.dsp.NyquistBand
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.widget.EqSettingView
import me.spica27.spicamusic.widget.SimpleTopBar
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(
  navigator: NavBackStack? = null
) {

  val dataStoreUtil = DataStoreUtil()

  val replayGain = dataStoreUtil.getReplayGain.collectAsState(0)

  val eq = dataStoreUtil.getEqualizerBand().collectAsState(Equalizer.Presets.flat.bands)

  val scope = rememberCoroutineScope()

  Scaffold(
    topBar = {
      SimpleTopBar(
        onBack = {
          navigator?.removeLastOrNull()
        },
        title = "音效"
      )
    }) { it ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(it)
    ) {

      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        Text(
          "音频增益", style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.W600
          ), modifier = Modifier.padding(horizontal = 16.dp)
        )




        Text(
          "调节音量的额外的增益强度", style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 15.sp
          ), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        )


        Slider(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          value = replayGain.value * 1f,
          onValueChange = {
            scope.launch {
              dataStoreUtil.saveReplayGain(it.roundToInt())
            }
          },
          valueRange = -10f..10f,
          steps = 21
        )



        Text(
          "${replayGain.value} DB", style = TextStyle().copy(
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
          ), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        )


        Spacer(modifier = Modifier.height(20.dp))


        Text(
          "均衡效果器(EQ)", style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.W600
          ), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        )


        Text(
          "通过滑杆控制不同频段增益，选择喜欢的频响曲线",
          style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 15.sp
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        )


        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          userScrollEnabled = false,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(Equalizer.Presets.all, key = { _, item ->
            item.name
          }) { _, item ->
            EqItem(isSelected = false, onClick = {
              scope.launch {
                dataStoreUtil.saveEq(
                  item.bands
                )
              }
            }, name = stringResource(item.nameResId))
          }
        }
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
        )
        val bgRowLineColor = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()
        val bgColumnLineColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
        val centerRowLineColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
        val indicatorColor = MaterialTheme.colorScheme.primary.toArgb()
        val indicatorCenterColor = MaterialTheme.colorScheme.primary.toArgb()
        val indicatorLineColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
        AndroidView(
          factory = { context ->
            EqSettingView(context).apply {
              setColors(
                bgRowLineColor = bgRowLineColor,
                bgColumnLineColor = bgColumnLineColor,
                centerRowLineColor = centerRowLineColor,
                indicatorColor = indicatorColor,
                indicatorLineColor = indicatorLineColor,
                indicatorCenterColor = indicatorCenterColor
              )
              setListener {
                scope.launch {
                  dataStoreUtil.saveEq(it)
                }
              }
            }
          },
          update = { view ->
            view.setGainArray(eq.value)
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(170.dp),
          onRelease = {
            it.release()
          }
        )
      }
    }
  }
}


@Composable
fun ItemEq(
  modifier: Modifier = Modifier,
  band: NyquistBand,
  onValueChange: (Float) -> Unit,
) {

  val gain = remember { mutableFloatStateOf(band.gain.toFloat()) }

//  val showGain = animateFloatAsState(gain.floatValue, label = "", animationSpec = tween(500)).value

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    VerticalSlider(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f), value = gain.floatValue, onValueChange = {
        gain.floatValue = it
        onValueChange(it)
      }, valueRange = -10f..10f
    )
  }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,/*@IntRange(from = 0)*/
  steps: Int = 0,
  onValueChangeFinished: (() -> Unit)? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  colors: SliderColors = SliderDefaults.colors()
) {
  val containerColor = MaterialTheme.colorScheme.surfaceContainer
  val thumbColor = MaterialTheme.colorScheme.primary
  Slider(
    colors = colors,
    interactionSource = interactionSource,
    onValueChangeFinished = onValueChangeFinished,
    steps = steps,
    valueRange = valueRange,
    enabled = enabled,
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier
      .graphicsLayer {
        rotationZ = 270f
        transformOrigin = TransformOrigin(0f, 0f)
      }
      .layout { measurable, constraints ->
        val placeable = measurable.measure(
          Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxHeight,
          )
        )
        layout(placeable.height, placeable.width) {
          placeable.place(-placeable.width, 0)
        }
      }
      .height(14.dp)
      .then(modifier),
    track = {
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
      ) {
        drawLine(
          cap = androidx.compose.ui.graphics.StrokeCap.Round,
          color = containerColor,
          start = Offset(0f, size.height / 2),
          end = Offset(size.width, size.height / 2),
          strokeWidth = 14.dp.toPx()
        )
        drawLine(
          color = thumbColor,
          start = Offset(size.width / 2f, size.height / 2f),
          end = Offset(size.width * ((value + 10f) * 1f / 20f), size.height / 2f),
          strokeWidth = 12.dp.toPx(),
          cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
      }
    },
    thumb = {

    })
}

@Composable
private fun EqItem(isSelected: Boolean, onClick: () -> Unit, name: String) {

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
      )
      .noRippleClickable(onClick = {
        onClick.invoke()
      })
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      name, style = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
  }
}