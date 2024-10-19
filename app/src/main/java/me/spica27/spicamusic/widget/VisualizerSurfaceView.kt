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
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.processer.VisualizerHelper
import me.spica27.spicamusic.utils.dp
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.cos
import kotlin.math.sin

class VisualizerSurfaceView : SurfaceView, SurfaceHolder.Callback,
  VisualizerHelper.OnVisualizerEnergyCallBack {

  private val drawThread = HandlerThread("VisualizerSurfaceView").apply { start() }

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

  fun setBgColor(color: Int) {
    backgroundColor = color
  }

  private val pointPaint = Paint().apply {
    // set paint
    color = Color.BLACK
    // set style
    style = Paint.Style.FILL
    strokeWidth = 8.dp
  }

  fun setColor(color: Int) {
    pointPaint.color = color
  }

  private val lock = ReentrantLock()

  private var executor: ExecutorService = Executors.newSingleThreadExecutor()

  private var isWork = false

  private val drawRunnable = Runnable {
    while (isWork && !Thread.interrupted()) {
      val canvas = holder.lockCanvas()
      if (canvas != null) {
        // draw something
        lock.lock()
        canvas.drawColor(backgroundColor)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        widths.forEachIndexed { index, width ->
          canvas.rotate(angles[index])
          canvas.drawLine(0f, 0f, width, 0f, pointPaint)

          canvas.rotate(angles[index])
        }
        canvas.restore()
        holder.unlockCanvasAndPost(canvas)
        lock.unlock()

        SystemClock.sleep(16)
      }
    }
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    executor = Executors.newSingleThreadExecutor()
    drawHandler = Handler(drawThread.looper)
    isWork = true
    drawHandler.post(drawRunnable)
    PlaybackStateManager.getInstance().fftAudioProcessor.setFftListener(VisualizerHelper.getInstance().fftListener)
    VisualizerHelper.getInstance().addCallBack(this)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    radius = width / 2
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    PlaybackStateManager.getInstance().fftAudioProcessor.setFftListener(null)
    VisualizerHelper.getInstance().removeCallBack(this)
    isWork = false
    drawHandler.removeCallbacksAndMessages(null)
    drawThread.quitSafely()
    drawThread.join(32)
    drawThread.interrupt()
    executor.awaitTermination(32, java.util.concurrent.TimeUnit.MILLISECONDS)
  }

  private var radius = 100

  private val scope = 50

  private val between = 1

  // 角度
  private val angles = arrayListOf<Float>()

  // 间隔长度
  private val widths = arrayListOf<Float>()

  // 每个点的x坐标
  private val xPoints = arrayListOf<Int>()

  // 每个点的y坐标
  private val yPoints = arrayListOf<Int>()

  // 每个点的高度
  private val lineHeights = arrayListOf<Int>()

  // 上次采样的时间
  private var lastSampleTime = 0L

  // 采样间隔
  private val interval = 1000

  override fun setWaveData(data: FloatArray, totalEnergy: Float) {

    if (System.currentTimeMillis() - lastSampleTime < interval) {
      return
    }

    executor.execute {
      lock.lock()
      angles.clear()
      widths.clear()
      xPoints.clear()
      yPoints.clear()
      lineHeights.clear()

      val total: Int = data.size - scope

      //圆周长
      val totalLength: Float = 3.14f * 2 * radius

      //每个角度所占用的长度
      val eachWidthByAngle = totalLength / 360

      //每个能量所占用的长度
      val eachWidthByDataLength = totalLength / total

      //间隔所占的长度
      val betweenWidth: Float = between * eachWidthByAngle

      for (i in 0 until total) {
        //每个能量在圆环上的角度位置
        val positionAngle = i * 1.0f / total * 360
        angles.add(positionAngle)
        widths.add(eachWidthByDataLength * data[i] + betweenWidth)
        val xy = calcPoint(width / 2, height / 2, radius, positionAngle)
        lineHeights.add((data[i] / totalEnergy * 30.dp).toInt())
        xPoints.add(xy[0])
        yPoints.add(xy[0])
      }
      lastSampleTime = System.currentTimeMillis()
      lock.unlock()
      Timber.tag("dataSize").d("dataSize: ${data.size}")
      data.forEach {
        Timber.tag("data").d("data: $it")
      }
    }
  }


  /**
   * 计算圆弧上的某一点
   */
  private fun calcPoint(centerX: Int, centerY: Int, radius: Int, angle: Float): IntArray {
    val x = (centerX + radius * cos((angle) * Math.PI / 180)).toInt()
    val y = (centerY + radius * sin((angle) * Math.PI / 180)).toInt()
    return intArrayOf(x, y)
  }

}