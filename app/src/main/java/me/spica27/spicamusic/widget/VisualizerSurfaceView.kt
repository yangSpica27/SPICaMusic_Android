package me.spica27.spicamusic.widget

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.utils.dp
import me.spica27.spicamusic.visualiser.MusicVisualiser
import me.spica27.spicamusic.visualiser.VisualizerDrawableManager
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock


@UnstableApi
class VisualizerSurfaceView : SurfaceView, SurfaceHolder.Callback, MusicVisualiser.Listener {

  private lateinit var drawThread: HandlerThread

  private lateinit var drawHandler: Handler

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context, attrs, defStyleAttr
  )

  init {
    holder.addCallback(this)
    setOnClickListener {
      nextVisualiserType()
    }
  }


  fun setBgColor(color: Int) {
    visualizerDrawableManager.setBackgroundColor(color)
  }

  fun setThemeColor(color: Int) {
    visualizerDrawableManager.setThemeColor(color)
  }


  private val visualizerDrawableManager = VisualizerDrawableManager()


  private val lock = ReentrantLock()

  private var isWork = false


  fun nextVisualiserType() {
    visualizerDrawableManager.nextVisualiserType()
  }


  private val drawRunnable = Runnable {
    while (isWork && !Thread.interrupted()) {
      val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        holder.lockHardwareCanvas()
      } else {
        holder.lockCanvas()
      }
      if (canvas == null) {
        SystemClock.sleep(8)
        continue
      }
      lock.lock()
      // 绘制
      visualizerDrawableManager.getVisualiserDrawable().draw(canvas)
      lock.unlock()
      holder.unlockCanvasAndPost(canvas)
      SystemClock.sleep(8)
    }
  }


  // 波形图绘制效果集合
  private val musicVisualiser: MusicVisualiser = MusicVisualiser()

  override fun surfaceCreated(holder: SurfaceHolder) {
    musicVisualiser.setListener(this)
    musicVisualiser.ready()
    drawThread = HandlerThread("drawThread")
    drawThread.start()
    drawHandler = Handler(drawThread.looper)
    isWork = true
    drawHandler.post(drawRunnable)
    Timber.tag("VisualizerSurfaceView").e("surfaceCreated")
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    visualizerDrawableManager.setVisualiserBounds(width, height)
    visualizerDrawableManager.setRadius((width / 2 - 60.dp).toInt())
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    musicVisualiser.setListener(null)
    musicVisualiser.dispose()
    isWork = false
    drawHandler.removeCallbacksAndMessages(null)
    drawThread.quitSafely()
    Timber.tag("VisualizerSurfaceView").e("surfaceDestroyed")
  }


  override fun getDrawData(list: List<Float>) {
    lock.lock()
    visualizerDrawableManager.getVisualiserDrawable().update(list)
    lock.unlock()
  }

}