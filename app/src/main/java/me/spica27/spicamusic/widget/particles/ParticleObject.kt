package me.spica27.spicamusic.widget.particles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

class ParticleObject(
  var animationParams: AnimationParams = AnimationParams()
) {

  data class AnimationParams(
    var locationX: Dp = Dp(-1f),
    var locationY: Dp = Dp(-1f),
    var alpha: Float = -1f,
    var isFilled: Boolean = false,
    var currentColor: Color = Color(0),
    var particleSize: Dp = Dp(0f),
    var currentAngle: Float = 1f,
    var progressModifier: Float = 1f
  )
}