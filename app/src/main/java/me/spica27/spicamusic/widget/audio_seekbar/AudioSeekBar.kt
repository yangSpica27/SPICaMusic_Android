@file:OptIn(ExperimentalComposeUiApi::class)

package me.spica27.spicamusic.widget.audio_seekbar

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.rememberVibrator
import me.spica27.spicamusic.utils.tick


private val MinSpikeWidthDp: Dp = 1.dp
private val MaxSpikeWidthDp: Dp = 24.dp
private val MinSpikePaddingDp: Dp = 0.dp
private val MaxSpikePaddingDp: Dp = 12.dp
private val MinSpikeRadiusDp: Dp = 0.dp
private val MaxSpikeRadiusDp: Dp = 12.dp

private const val MinProgress: Float = 0F
private const val MaxProgress: Float = 1F

private val MinSpikeHeight: Float = 1f
private const val DefaultGraphicsLayerAlpha: Float = .99F


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioWaveSlider(
  modifier: Modifier = Modifier,
  style: DrawStyle = Fill,
  waveformBrush: Brush = SolidColor(Color.White),
  progressBrush: Brush = SolidColor(Color.Blue),
  waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
  amplitudeType: AmplitudeType = AmplitudeType.Avg,
  onProgressChangeFinished: (() -> Unit)? = null,
  spikeAnimationSpec: AnimationSpec<Float> = tween(500),
  spikeWidth: Dp = 4.dp,
  spikeRadius: Dp = 2.dp,
  spikePadding: Dp = 1.dp,
  progress: Float = 0F,
  amplitudes: List<Int>,
  onProgressChange: (Float) -> Unit,
) {
  val progress = animateFloatAsState(
    progress.coerceIn(MinProgress, MaxProgress),
    tween(125, easing = LinearEasing),
    label = "",
  ).value

  val spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(MinSpikeWidthDp, MaxSpikeWidthDp) }
  val spikePadding =
    remember(spikePadding) { spikePadding.coerceIn(MinSpikePaddingDp, MaxSpikePaddingDp) }
  val spikeRadius =
    remember(spikeRadius) { spikeRadius.coerceIn(MinSpikeRadiusDp, MaxSpikeRadiusDp) }
  val spikeTotalWidth = remember(spikeWidth, spikePadding) {
    derivedStateOf {
      spikeWidth + spikePadding
    }
  }.value
  var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
  var spikes by remember { mutableFloatStateOf(0F) }

  val spikesAmplitudes = remember(amplitudes, spikes, amplitudeType) {
    amplitudes.toDrawableAmplitudes(
      amplitudeType = amplitudeType,
      spikes = spikes.toInt(),
      minHeight = MinSpikeHeight,
      maxHeight = canvasSize.height.coerceAtLeast(MinSpikeHeight)
    )
  }
    .map { animateFloatAsState(it, spikeAnimationSpec, label = "").value }

  val vibrator = rememberVibrator()

  val lastTickTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

  val coroutineScope = rememberCoroutineScope()

  Slider(
    modifier = modifier,
    value = progress,
    thumb = {

    },
    track = {
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .graphicsLayer(alpha = DefaultGraphicsLayerAlpha)
      ) {
        canvasSize = size
        spikes = size.width / spikeTotalWidth.toPx()
        spikesAmplitudes.forEachIndexed { index, amplitude ->
          drawRoundRect(
            brush = waveformBrush,
            topLeft = Offset(
              x = index * spikeTotalWidth.toPx(),
              y = when (waveformAlignment) {
                WaveformAlignment.Top -> 0F
                WaveformAlignment.Bottom -> size.height - amplitude
                WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
              }
            ),
            size = Size(
              width = spikeWidth.toPx(),
              height = amplitude
            ),
            cornerRadius = CornerRadius(spikeRadius.toPx(), spikeRadius.toPx()),
            style = style
          )
          drawRect(
            brush = progressBrush,
            size = Size(
              width = progress * size.width,
              height = size.height
            ),
            blendMode = BlendMode.SrcAtop
          )
        }
      }
    },
    valueRange = MinProgress..MaxProgress,
    onValueChange = {
      onProgressChange(it)
      coroutineScope.launch(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val i = currentTime - lastTickTime.longValue
        if (i < 20) return@launch
        lastTickTime.longValue = currentTime
        vibrator.tick()
      }
    },
    onValueChangeFinished = {
      onProgressChangeFinished?.invoke()
    },
  )


}

private fun List<Int>.toDrawableAmplitudes(
  amplitudeType: AmplitudeType,
  spikes: Int,
  minHeight: Float,
  maxHeight: Float
): List<Float> {
  val amplitudes = map { it * 1f }
  if (amplitudes.isEmpty() || spikes == 0) {
    return List(spikes) { minHeight.fastCoerceAtLeast(20f) }
  }
  val transform = { data: List<Float> ->
    when (amplitudeType) {
      AmplitudeType.Avg -> data.average()
      AmplitudeType.Max -> data.max()
      AmplitudeType.Min -> data.min()
    }.toFloat().coerceIn(minHeight, maxHeight)
  }
  val res = when {
    spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
    else -> amplitudes.chunkToSize(spikes, transform)
  }.normalize(minHeight, maxHeight)


  return res
}