package me.spica27.spicamusic.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.utils.dp
import me.spica27.spicamusic.visualiser.MusicVisualiser
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock


@UnstableApi
class VisualizerSurfaceView : SurfaceView, SurfaceHolder.Callback,
  MusicVisualiser.Listener {

  private lateinit var drawThread: HandlerThread

  private lateinit var drawHandler: Handler

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  init {
    holder.addCallback(this)
  }

  private var backgroundColor = Color.WHITE

  private var radius = 0

  fun setBgColor(color: Int) {
    backgroundColor = color
  }

  private val pointPaint = Paint().apply {
    // set paint
    color = Color.BLACK
    // set style
    style = Paint.Style.FILL
    strokeWidth = 8.dp
    strokeCap = Paint.Cap.ROUND
  }

  private var lineColor = Color.BLACK

  fun setColor(color: Int) {
    pointPaint.color = color
    lineColor = color
  }

  private val lock = ReentrantLock()

  private var isWork = false

  private val drawRunnable = Runnable {
    while (isWork && !Thread.interrupted()) {
      val canvas = holder.lockCanvas()
      if (canvas != null) {
        canvas.drawColor(backgroundColor)
        canvas.translate(width / 2f, height / 2f)
        lock.lock()

        val fraction =
          decelerateInterpolator.getInterpolation(((System.currentTimeMillis() - lastSampleTime).toFloat() / interval))

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
              pointPaint.color = ColorUtils.setAlphaComponent(lineColor, 100)
              canvas.drawLine(
                0f,
                maxYList[i] * 1f,
                0f,
                radius * 1f,
                pointPaint
              )
              pointPaint.color = lineColor
            }


            canvas.drawLine(
              0f,
              curY * 1f,
              0f,
              radius * 1f,
              pointPaint
            )


          }
          canvas.restore()
        }

        lock.unlock()
      }
      holder.unlockCanvasAndPost(canvas)
      SystemClock.sleep(16)
    }
  }

  // 记录最高的Y值 用于回落动画
  private val maxYList = arrayListOf<Int>()


  private val musicVisualiser: MusicVisualiser = MusicVisualiser()

  override fun surfaceCreated(holder: SurfaceHolder) {
    musicVisualiser.setListener(this)
    musicVisualiser.ready()
    drawThread = HandlerThread("drawThread")
    drawThread.start()
    drawHandler = Handler(drawThread.looper)
    isWork = true
    drawHandler.post(drawRunnable)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    radius = (width / 2 - 40.dp).toInt()
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    musicVisualiser.dispose()
    isWork = false
    drawHandler.removeCallbacksAndMessages(null)
    drawThread.quitSafely()
    drawThread.join(32)
    drawThread.interrupt()
    Timber.tag("VisualizerSurfaceView").e("surfaceDestroyed")
  }



  // 上次采样的时间
  private var lastSampleTime = 0L

  // 采样间隔
  private val interval = 500


  // 采集到的数据
  private val yList = arrayListOf<Int>()

  // 前一次采集的数据
  private val lastYList = arrayListOf<Int>()


  private val decelerateInterpolator = DecelerateInterpolator()

  override fun getDrawData(list: List<Float>) {
    lock.lock()

    if (
      ((System.currentTimeMillis() - lastSampleTime) < interval)
      && yList.isNotEmpty()
      && lastYList.isNotEmpty()
    ) {
      lock.unlock()
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
    lock.unlock()
  }

}