package me.spica27.spicamusic.widget

import android.annotation.SuppressLint
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.highlight.GlassHighlight
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.spica27.spicamusic.lyric.LyricItem
import me.spica27.spicamusic.lyric.toNormal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
  modifier: Modifier = Modifier,
  currentLyric: List<LyricItem>,
  currentTime: Long
) {

  val listState = rememberLazyListState()

  val activeIndex = remember { mutableIntStateOf(0) }

  val layoutHeight = remember { mutableIntStateOf(0) }

  LaunchedEffect(currentTime) {
    launch(Dispatchers.Default + SupervisorJob()) {
      var index = 0
      for (item in currentLyric) {
        if (item.time >= currentTime) {
          activeIndex.intValue = index
          listState.animateScrollToItemAndCenter(
            index = index,
            offset = -layoutHeight.intValue / 2
          )
          return@launch
        }
        index++
      }
    }
  }

  val providerState = rememberLiquidGlassProviderState(
    backgroundColor = Color.Transparent
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged {
        layoutHeight.intValue = it.height
      },
  ) {


    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .liquidGlassProvider(providerState),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      state = listState,
      userScrollEnabled = false
    ) {
      itemsIndexed(
        currentLyric.map { it.toNormal() },
        key = { index, _ ->
          index
        }
      ) { index, it ->
        it?.toNormal()?.let { item ->
          LyricsViewLine(
            contentColor = MaterialTheme.colorScheme.onSurface,
            isActive = index == activeIndex.intValue,
            content = item.content,
            inactiveBlur = ((index.coerceAtLeast(activeIndex.intValue) -
                index.coerceAtMost(activeIndex.intValue)) * 0.5f).fastCoerceAtMost(4f),
            onClick = { },
          )
        }
      }
    }

    Box(
      Modifier
        .fillMaxSize()
        .liquidGlass(
          providerState,
          GlassStyle(
            shape = MaterialTheme.shapes.medium,
            innerRefraction = InnerRefraction(
              height = RefractionHeight(24.dp),
              amount = RefractionAmount((-24).dp)
            ),
            material = GlassMaterial.None,
            highlight = GlassHighlight.None,
          ),
          compositingStrategy = CompositingStrategy.Auto
        )
    )

  }

}


@Composable
fun LyricsViewLine(
  isActive: Boolean,
  content: String,
  contentColor: Color = Color.Black,
  fontSize: TextUnit = 18.sp,
  fontWeight: FontWeight = FontWeight.Black,
  lineHeight: TextUnit = 1.2.em,
  onClick: () -> Unit,
  @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
  activeScale: Float = 1.1f,
  inactiveScale: Float = 1f,
  activeAlpha: Float = 1f,
  inactiveAlpha: Float = 0.35f,
  activeBlur: Float = 0f,
  inactiveBlur: Float = 1f,
  itemTranslationY: Float = 0f,
) {

  val scale = remember { mutableFloatStateOf(inactiveScale) }
  val alpha = remember { mutableFloatStateOf(inactiveAlpha) }
  val blur = remember { mutableFloatStateOf(0f) }

  val interactionSource = remember { MutableInteractionSource() }


  LaunchedEffect(
    isActive
  ) {
    launch {
      animate(
        initialValue = scale.floatValue,
        targetValue = if (isActive) {
          activeScale
        } else {
          inactiveScale
        },
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        )
      ) { value, _ ->
        scale.floatValue = value
      }
    }

    launch {
      animate(
        initialValue = alpha.floatValue,
        targetValue = if (isActive) {
          activeAlpha
        } else {
          inactiveAlpha
        },
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        )
      ) { value, _ ->
        alpha.floatValue = value
      }
    }
  }

  LaunchedEffect(isActive, inactiveBlur) {
    launch {
      animate(
        initialValue = blur.floatValue,
        targetValue = if (isActive) {
          activeBlur
        } else {
          inactiveBlur
        },
        animationSpec = tween(
          durationMillis = 300,
          delayMillis = 100,
          easing = EaseInCubic
        )
      ) { value, _ ->
        blur.floatValue = value
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .clip(MaterialTheme.shapes.medium)
      .indication(interactionSource, indication = null)
      .graphicsLayer {
        translationY = itemTranslationY
      }
      .pointerInput(interactionSource) {
        detectTapGestures(
          onPress = {
            val press = PressInteraction.Press(it)
            try {
              // 其他动画进行进行的时候进制波纹动画
              withTimeout(timeMillis = 100) {
                tryAwaitRelease()
              }
            } catch (e: TimeoutCancellationException) {
              e.printStackTrace()
              interactionSource.emit(press)
              tryAwaitRelease()
            }
            interactionSource.emit(PressInteraction.Release(press))
          },
          onTap = { onClick() },
        )
      }
      .padding(
        start = 16.dp,
        top = 8.dp,
        end = 32.dp,
        bottom = 16.dp,
      ),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = content,
      modifier = Modifier
        .graphicsLayer {
          transformOrigin = TransformOrigin(0f, 1f)
          scaleX = scale.floatValue
          scaleY = scale.floatValue
          this.alpha = alpha.floatValue
        }
        .blur(
          radius = blur.floatValue.dp,
        ),
      style = TextStyle(
        color = contentColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        shadow = Shadow(
          color = contentColor.copy(alpha = .2f),
          blurRadius = if (isActive) {
            13f
          } else {
            5f
          }
        )
      )
    )
  }
}


suspend fun LazyListState.animateScrollToItemAndCenter(
  index: Int,
  offset: Int,
) {
  if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
    withContext(Dispatchers.Main) {
      animateScrollToItem(index = index, scrollOffset = offset)
    }
  }
  val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

  if (itemInfo != null) {
    val viewportCenter = layoutInfo.viewportEndOffset / 2f // Use float for precision
    val itemCenter = itemInfo.offset + itemInfo.size / 2f
    val scrollAmount = itemCenter - viewportCenter
    this.animateScrollBy(
      scrollAmount, animationSpec = tween(
        durationMillis = 500,
        easing = EaseInOut
      )
    )
  } else {
    withContext(Dispatchers.Main) {
      animateScrollToItem(index = index, scrollOffset = offset)
    }
  }
}