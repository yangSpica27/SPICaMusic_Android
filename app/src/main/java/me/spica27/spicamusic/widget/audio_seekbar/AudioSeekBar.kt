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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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


private val MinSpikeWidthDp: Dp = 1.dp
private val MaxSpikeWidthDp: Dp = 24.dp
private val MinSpikePaddingDp: Dp = 0.dp
private val MaxSpikePaddingDp: Dp = 12.dp
private val MinSpikeRadiusDp: Dp = 0.dp
private val MaxSpikeRadiusDp: Dp = 12.dp

private const val MinProgress: Float = 0F
private const val MaxProgress: Float = 1F

private const val MinSpikeHeight: Float = 1F
private const val DefaultGraphicsLayerAlpha: Float = 0.99F


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
  val _progress = animateFloatAsState(
    progress.coerceIn(MinProgress, MaxProgress),
    tween(125, easing = LinearEasing),
    label = "",
  ).value

  val _spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(MinSpikeWidthDp, MaxSpikeWidthDp) }
  val _spikePadding =
    remember(spikePadding) { spikePadding.coerceIn(MinSpikePaddingDp, MaxSpikePaddingDp) }
  val _spikeRadius =
    remember(spikeRadius) { spikeRadius.coerceIn(MinSpikeRadiusDp, MaxSpikeRadiusDp) }
  val _spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }
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

  Slider(
    modifier = modifier,
    value = _progress,
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
        spikes = size.width / _spikeTotalWidth.toPx()
        spikesAmplitudes.forEachIndexed { index, amplitude ->
          drawRoundRect(
            brush = waveformBrush,
            topLeft = Offset(
              x = index * _spikeTotalWidth.toPx(),
              y = when (waveformAlignment) {
                WaveformAlignment.Top -> 0F
                WaveformAlignment.Bottom -> size.height - amplitude
                WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
              }
            ),
            size = Size(
              width = _spikeWidth.toPx(),
              height = amplitude
            ),
            cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx()),
            style = style
          )
          drawRect(
            brush = progressBrush,
            size = Size(
              width = _progress * size.width,
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
  val amplitudes = map(Int::toFloat)
  if (amplitudes.isEmpty() || spikes == 0) {
    return List(spikes) { minHeight }
  }
  val transform = { data: List<Float> ->
    when (amplitudeType) {
      AmplitudeType.Avg -> data.average()
      AmplitudeType.Max -> data.max()
      AmplitudeType.Min -> data.min()
    }.toFloat().coerceIn(minHeight, maxHeight)
  }
  return when {
    spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
    else -> amplitudes.chunkToSize(spikes, transform)
  }.normalize(minHeight, maxHeight)
}