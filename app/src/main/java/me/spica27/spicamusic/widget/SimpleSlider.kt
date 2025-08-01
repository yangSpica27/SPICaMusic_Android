package me.spica27.spicamusic.widget

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.highlight.GlassHighlight
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight
import com.kyant.liquidglass.rememberLiquidGlassProviderState


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

  val sliderBackgroundColor = MaterialTheme.colorScheme.surfaceContainer

  val sliderColor = MaterialTheme.colorScheme.primary

  val lineColor = MaterialTheme.colorScheme.onSurface.copy(.35f)

  val lineCenterColor = MaterialTheme.colorScheme.background

  val providerState = rememberLiquidGlassProviderState(
    backgroundColor = MaterialTheme.colorScheme.background
  )

  var isPlaying by remember { mutableStateOf(false) }

  val scale = animateFloatAsState(
    if (isPlaying) 1.5f else 1f,
    tween(
      200
    )
  )

  Slider(
    modifier = Modifier
      .height((35 * scale.value).dp)
      .clip(MaterialTheme.shapes.small)
      .background(
        color = sliderBackgroundColor,
        shape = MaterialTheme.shapes.extraSmall
      )
      .innerShadow(
        shape = MaterialTheme.shapes.extraSmall, shadow = Shadow(
          radius = 2.dp,
          color = MaterialTheme.colorScheme.primary,
          alpha = .21f
        )
      )
      .then(modifier),
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
          .liquidGlassProvider(providerState)
          .graphicsLayer()
          .drawWithCache {

            val with = size.width / (cutLineSize - 1)

            onDrawWithContent {

              for (i in 0 until cutLineSize) {
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
            .fillMaxHeight()
            .width(35.dp)
            .liquidGlass(
              providerState,
              GlassStyle(
                shape = MaterialTheme.shapes.extraSmall,
                innerRefraction = InnerRefraction(
                  height = RefractionHeight.Half,
                  amount = RefractionAmount.Half
                ),
                material = GlassMaterial.Default.copy(
                  blurRadius = 3.dp,
                  alpha = .06f
                ),
                highlight = GlassHighlight.Default.copy(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.primary
                ),
              ),
              compositingStrategy = CompositingStrategy.Auto
            )
            .background(
              color = MaterialTheme.colorScheme.surfaceContainer.copy(
                .5f
              ),
              shape = MaterialTheme.shapes.extraSmall
            )
            .shadow(
              shape = MaterialTheme.shapes.extraSmall,
              elevation = 2.dp,
              ambientColor = MaterialTheme.colorScheme.onBackground.copy(
                .3f
              ),
              spotColor = MaterialTheme.colorScheme.onBackground.copy(
                .3f
              )
            )
        )
      }
    },
  )
}