package me.spica27.spicamusic.widget.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.unit.Dp
import me.spica27.spicamusic.utils.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


private fun ParticleObject.drawOnCanvas(paint: Paint, canvas: Canvas) {
  canvas.apply {
    paint.color = animationParams.currentColor
    paint.alpha = animationParams.alpha
    val centerW = animationParams.locationX.value
    val centerH = animationParams.locationY.value
    if (animationParams.isFilled) {
      paint.style = PaintingStyle.Fill
    } else {
      paint.style = PaintingStyle.Stroke

    }
    drawCircle(
      Offset(centerW, centerH),
      animationParams.particleSize.value / 2f,
      paint
    )
  }
}


private fun ParticleObject.randomize(
  random: Random,
  pxSize: Size
) {

  val randomAngleOffset =
    randomFloat(0f, 360f, random)
  val randomizedAngle = randomAngleOffset
  val centerX = (pxSize.width / 2) + randomFloat(-10f, 10f, random)
  val centerY = (pxSize.height / 2) + randomFloat(-10f, 10f, random)
  val radius = kotlin.math.min(centerX, centerY)
  val randomLength =
    randomFloat(60.dp * radius, 60.dp * radius, random)
  val x = randomLength * cos(randomizedAngle)
  val y = randomLength * sin(randomizedAngle)
  val color = Color.DarkGray
  animationParams = ParticleObject.AnimationParams(
    isFilled = false,
    alpha = (random.nextFloat()).coerceAtLeast(0f),
    locationX = Dp(centerX + x),
    locationY = Dp(centerY + y),
    particleSize = Dp(randomFloat(12f, 20f)),
    currentAngle = randomizedAngle.toFloat(),
    progressModifier = randomFloat(1f, 2f, random),
    currentColor = color
  )
}