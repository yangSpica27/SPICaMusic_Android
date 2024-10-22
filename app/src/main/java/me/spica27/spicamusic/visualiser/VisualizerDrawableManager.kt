package me.spica27.spicamusic.visualiser

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.visualiser.drawable.CircleVisualiser
import me.spica27.spicamusic.visualiser.drawable.LineVisualiser
import me.spica27.spicamusic.visualiser.drawable.VisualiserDrawable

@OptIn(UnstableApi::class)
class VisualizerDrawableManager {

  enum class VisualiserType {
    LINE, CIRCLE
  }


  private val visualiserDrawables = HashMap<VisualiserType, VisualiserDrawable>()

  private var currentVisualiserType = VisualiserType.CIRCLE


  fun nextVisualiserType() {
    currentVisualiserType = when (currentVisualiserType) {
      VisualiserType.LINE -> VisualiserType.CIRCLE
      VisualiserType.CIRCLE -> VisualiserType.LINE
    }
  }

  init {
    visualiserDrawables[VisualiserType.LINE] = LineVisualiser()
    visualiserDrawables[VisualiserType.CIRCLE] = CircleVisualiser()
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

  fun setBackgroundColor(color: Int) {
    visualiserDrawables.values.forEach {
      it.backgroundColor = color
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