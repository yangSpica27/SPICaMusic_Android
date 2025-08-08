package me.spica27.spicamusic.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.View
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.utils.dp
import me.spica27.spicamusic.visualiser.MusicVisualiser
import me.spica27.spicamusic.visualiser.VisualizerDrawableManager
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock


@UnstableApi
class VisualizerView : View, MusicVisualiser.Listener {


  private lateinit var mThread: HandlerThread

  private lateinit var mHandler: Handler

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context, attrs, defStyleAttr
  )

  init {
    setOnClickListener {
//      nextVisualiserType()
    }
  }

  private val infiniteAnim = ValueAnimator.ofFloat(0f,1f).apply {
    repeatCount = ValueAnimator.INFINITE
    duration = 1000
    addUpdateListener {
      postInvalidateOnAnimation()
    }
  }


  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    mThread = HandlerThread("VisualizerSurfaceView")
    mThread.start()
    mHandler = Handler(mThread.looper)
    musicVisualiser.setListener(this)
    infiniteAnim.start()
    musicVisualiser.ready()
    Timber.tag("VisualizerSurfaceView").d("onAttachedToWindow()")
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    musicVisualiser.dispose()
    mHandler.removeCallbacksAndMessages(null)
    mThread.quitSafely()
    musicVisualiser.setListener(null)
    infiniteAnim.cancel()
    Timber.tag("VisualizerSurfaceView").d("onDetachedFromWindow()")
  }


  fun setThemeColor(color: Int) {
    visualizerDrawableManager.setThemeColor(color)
  }


  private val visualizerDrawableManager = VisualizerDrawableManager()


  private val lock = ReentrantLock()

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    lock.lock()
    visualizerDrawableManager.setVisualiserBounds(w, h)
    visualizerDrawableManager.setRadius((width / 2 - 60.dp).toInt())
    lock.unlock()
  }


  fun nextVisualiserType() {
    visualizerDrawableManager.nextVisualiserType()
  }


  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    lock.lock()
    visualizerDrawableManager.getVisualiserDrawable().draw(canvas)
    lock.unlock()
  }


  // 波形图绘制效果集合
  private val musicVisualiser: MusicVisualiser = MusicVisualiser()

  private val fft = arrayListOf<Float>()

  override fun getDrawData(list: List<Float>) {
    Timber.tag("VisualizerSurfaceView").d("getDrawData()")
    lock.lock()
    fft.clear()
    fft.addAll(list)
    lock.unlock()
    mHandler.post {
      lock.lock()
      visualizerDrawableManager.getVisualiserDrawable().update(fft)
      lock.unlock()
      mHandler.removeCallbacksAndMessages(null)
    }
  }

}