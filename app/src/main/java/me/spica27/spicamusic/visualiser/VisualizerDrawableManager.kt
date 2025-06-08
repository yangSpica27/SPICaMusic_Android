package me.spica27.spicamusic.visualiser

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.visualiser.drawable.BlurVisualiser
import me.spica27.spicamusic.visualiser.drawable.CircleVisualiser
import me.spica27.spicamusic.visualiser.drawable.LineVisualiser
import me.spica27.spicamusic.visualiser.drawable.VisualiserDrawable

@OptIn(UnstableApi::class)
class VisualizerDrawableManager {

  enum class VisualiserType {
    LINE, CIRCLE, BLUR, RAIN
  }


  companion object {
    private val visualiserDrawables = hashMapOf<VisualiserType, VisualiserDrawable>(
      VisualiserType.CIRCLE to CircleVisualiser(),
      VisualiserType.BLUR to BlurVisualiser(),
      VisualiserType.LINE to LineVisualiser(),
      VisualiserType.RAIN to BlurVisualiser(),
    )
  }

  private var currentVisualiserType = VisualiserType.LINE


  fun nextVisualiserType() {
    currentVisualiserType = when (currentVisualiserType) {
      VisualiserType.LINE -> VisualiserType.CIRCLE
      VisualiserType.CIRCLE -> VisualiserType.BLUR
      VisualiserType.BLUR -> VisualiserType.RAIN
      VisualiserType.RAIN -> VisualiserType.LINE
    }
  }


  fun setVisualiserType(type: VisualiserType) {
    currentVisualiserType = type
  }

  fun setSecondaryColor(color: Int) {
    visualiserDrawables.values.forEach {
      it.secondaryColor = color
    }
  }

  fun setRadius(radius: Int) {
    visualiserDrawables.values.forEach {
      it.radius = radius
    }
  }

  fun setThemeColor(color: Int) {
    visualiserDrawables.values.forEach {
      it.themeColor = color
    }
  }


  fun setVisualiserBounds(width: Int, height: Int) {
    visualiserDrawables.values.forEach {
      it.setBounds(width, height)
    }
  }


  fun getVisualiserDrawable(): VisualiserDrawable {
    return visualiserDrawables[currentVisualiserType]
      ?: throw IllegalStateException("VisualiserDrawable not found")
  }

}