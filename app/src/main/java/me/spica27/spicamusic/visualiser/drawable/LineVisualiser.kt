package me.spica27.spicamusic.visualiser.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.OptIn
import androidx.core.graphics.ColorUtils
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.utils.dp
import me.spica27.spicamusic.visualiser.MusicVisualiser
import timber.log.Timber


@OptIn(UnstableApi::class)
class LineVisualiser : VisualiserDrawable() {


  // 上次采样的时间
  private var lastSampleTime = 0L

  // 采样间隔 
  private val interval = 125


  // 采集到的数据
  private val yList by lazy {
    arrayListOf<Int>().apply {
      for (i in 0 until MusicVisualiser.FREQUENCY_BAND_LIMITS.size) {
        add(radius)
        add(radius)
      }
    }
  }

  // 前一次采集的数据
  private val lastYList by lazy {
    arrayListOf<Int>().apply {
      for (i in 0 until MusicVisualiser.FREQUENCY_BAND_LIMITS.size) {
        add(radius)
        add(radius)
      }
    }
  }

  // 记录最高的Y值 用于回落动画
  private val maxYList by lazy {
    arrayListOf<Int>().apply {
      for (i in 0 until MusicVisualiser.FREQUENCY_BAND_LIMITS.size) {
        add(radius)
        add(radius)
      }
    }
  }


  private val pointPaint = Paint().apply {
    // set paint
    color = Color.BLACK
    // set style
    style = Paint.Style.FILL
    strokeWidth = 8.dp
    strokeCap = Paint.Cap.ROUND
  }


  private val decelerateInterpolator by lazy {
    android.view.animation.DecelerateInterpolator()
  }

  override fun draw(canvas: Canvas) {
    canvas.translate(width / 2f, height / 2f)
    val fraction =
      decelerateInterpolator.getInterpolation(((System.currentTimeMillis() - lastSampleTime).toFloat() / interval))
        .coerceIn(
          0f, 1f
        )

    if (lastYList.size == yList.size) {
      canvas.save()
      for (i in 0 until lastYList.size) {
        canvas.rotate(360f / lastYList.size)
        val lastY = lastYList[i]
        val y = yList[i]

        val curY = lastY + (y - lastY) * fraction

        if (maxYList.size == yList.size) {
          val maxY = maxYList[i]
          if (maxY < curY) {
            maxYList[i] = curY.toInt()
          } else {
            maxYList[i] = (maxYList[i] - (1.dp) / 60f).toInt()
          }
          pointPaint.color = ColorUtils.setAlphaComponent(themeColor, 100)
          canvas.drawLine(
            0f, maxYList[i] * 1f, 0f, radius * 1f, pointPaint
          )
          pointPaint.color = themeColor
        }


        canvas.drawLine(
          0f, curY * 1f, 0f, radius * 1f, pointPaint
        )
      }
      canvas.restore()
    }
  }

  override fun update(list: List<Float>) {
    if (((System.currentTimeMillis() - lastSampleTime) < interval) &&
      yList.isNotEmpty() && lastYList.isNotEmpty()
    ) {
      return
    }

    //  记录上次的结果
    if (yList.isNotEmpty()) {
      lastYList.clear()
      lastYList.addAll(yList)
    }

    // 计算
    yList.clear()
    list.forEachIndexed { index, value ->
      val cur = radius + 30.dp * value
      val next = if (index == list.size - 1) {
        radius + 30.dp * list[0]
      } else {
        radius + 30.dp * list[index + 1]
      }
      yList.add(cur.toInt())
      yList.add(next.toInt())
    }
    if (maxYList.size != yList.size) {
      maxYList.clear()
      maxYList.addAll(yList)
    }

    lastSampleTime = System.currentTimeMillis()
  }

}