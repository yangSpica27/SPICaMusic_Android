package me.spica27.spicamusic.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import me.spica27.spicamusic.R
import me.spica27.spicamusic.dsp.Equalizer
import me.spica27.spicamusic.dsp.EqualizerBand
import me.spica27.spicamusic.utils.dp
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class EqSettingView : View {


  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  constructor(context: Context) : super(context)

  private val gainArray = FloatArray(10)

  private val xs = FloatArray(10)

  private val ys = FloatArray(10)

  private val path = Path()

  private var listener: ((List<EqualizerBand>) -> Unit)? = null

  fun setListener(listener: (List<EqualizerBand>) -> Unit) {
    this.listener = listener
  }

  fun setGainArray(eq: List<EqualizerBand>) {
    for ((index, equalizerBand) in eq.withIndex()) {
      gainArray[index] = equalizerBand.gain.toFloat()
      Timber.tag(equalizerBand.centerFrequency.toString()).e("# ${gainArray[index]}")
    }
    val xl = width - paddingLeft - paddingRight
    itemWith = xl / 9f

    val yl = height - paddingTop - paddingBottom
    itemHeight = yl / 2f
    for ((index, f) in gainArray.withIndex()) {
      xs[index] = paddingLeft + itemWith * index
      ys[index] = height / 2f - f / 10f * itemHeight
    }
    path.reset()
    for (i in 0 until xs.size) {
      if (i == 0) {
        path.moveTo(xs[i], ys[i])
      } else {
        path.lineTo(xs[i], ys[i])
      }
    }
    postInvalidateOnAnimation()
  }


  init {
    setPadding(16.dp.toInt(), 20, 16.dp.toInt(), 20)
  }

  private var bgRowLineColor = "#E5e5e5".toColorInt()

  private var bgColumnLineColor = "#999999".toColorInt()

  private var centerRowLineColor = "#333333".toColorInt()

  private var indicatorColor = "#4096ff".toColorInt()

  private var indicatorCenterColor = "#ffffff".toColorInt()

  private var indicatorLineColor = "#666666".toColorInt()


  fun setColors(
    bgRowLineColor: Int,
    bgColumnLineColor: Int,
    centerRowLineColor: Int,
    indicatorColor: Int,
    indicatorCenterColor: Int,
    indicatorLineColor: Int
  ) {
    this.bgRowLineColor = bgRowLineColor
    this.bgColumnLineColor = bgColumnLineColor
    this.centerRowLineColor = centerRowLineColor
    this.indicatorColor = indicatorColor
    this.indicatorCenterColor = indicatorCenterColor
    this.indicatorLineColor = indicatorLineColor
  }

  private var itemWith = 0f

  private var itemHeight = 0f

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    val xl = width - paddingLeft - paddingRight
    itemWith = xl / 9f

    val yl = height - paddingTop - paddingBottom
    itemHeight = yl / 2f
    for ((index, f) in gainArray.withIndex()) {
      xs[index] = paddingLeft + itemWith * index
      ys[index] = height / 2f - f / 10f * itemHeight
    }
    path.reset()
    for (i in 0 until xs.size) {
      if (i == 0) {
        path.moveTo(xs[i], ys[i])
      } else {
        path.lineTo(xs[i], ys[i])
      }
    }
    postInvalidateOnAnimation()
  }


  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN,
      MotionEvent.ACTION_MOVE -> {

        if (event.x < paddingLeft) return false
        if (event.x > width - paddingRight) return false
        if (event.y > bottom - paddingBottom) return false
        if (event.y < paddingTop) return false

        val index = xs.indexOfFirst {
          max(event.x, it) - min(event.x, it) <= itemWith / 2
        }

        if (index == -1) return true

        gainArray[index] =
          if (event.y > height / 2) {
            0f - ((event.y - height / 2f) / itemHeight) * 10f
          } else {
            0f + ((height / 2f - event.y) / itemHeight) * 10f
          }

        ys[index] = height / 2f - gainArray[index] / 10f * itemHeight

        path.reset()
        for (i in xs.indices) {
          if (i == 0) {
            path.moveTo(xs[i], ys[i])
          } else {
            path.lineTo(xs[i], ys[i])
          }
        }
        postInvalidateOnAnimation()
      }

      MotionEvent.ACTION_CANCEL,
      MotionEvent.ACTION_UP -> {
        listener?.invoke(
          Equalizer.centerFrequency.mapIndexed { i, f ->
            EqualizerBand(f, gainArray[i].toDouble())
          }
        )
      }

    }
    return true
  }

  private val bgPaint = Paint().apply {
    strokeWidth = 14f
    color = ContextCompat.getColor(context, R.color.black)
    strokeCap = Paint.Cap.SQUARE
  }

  private val linePaint = Paint().apply {
    pathEffect = CornerPathEffect(50f)
    strokeCap = Paint.Cap.ROUND
    strokeWidth = 14f
    style = Paint.Style.STROKE
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    bgPaint.strokeWidth = 2f
    bgPaint.color = bgRowLineColor
    val h = height / 2 - paddingTop / 2 - paddingBottom / 2

    for (i in 1 until 6) {
      canvas.drawLine(
        0f, height / 2f - h / 5 * i,
        width * 1f, height / 2f - h / 5 * i,
        bgPaint
      )
      canvas.drawLine(
        0f, height / 2f + h / 5 * i,
        width * 1f, height / 2f + h / 5 * i,
        bgPaint
      )
    }


    // 绘制背景
    bgPaint.strokeCap = Paint.Cap.ROUND
    bgPaint.strokeWidth = 25.dp
    bgPaint.color = bgColumnLineColor
    for (i in 0 until xs.size) {
      canvas.drawLine(
        xs[i], paddingTop * 1f + bgPaint.strokeWidth,
        xs[i], (height - paddingBottom) * 1f - bgPaint.strokeWidth,
        bgPaint
      )
    }
//    bgPaint.strokeWidth = 4.dp
//    bgPaint.color = centerRowLineColor
//    canvas.drawLine(
//      0f, height / 2f,
//      width * 1f, height / 2f,
//      bgPaint
//    )


    for ((index, f) in xs.withIndex()) {
      linePaint.strokeWidth = 25.dp
      linePaint.color = indicatorLineColor
      canvas.drawLine(
        xs[index], ys[index],
        xs[index], height / 2f,
        linePaint
      )


      linePaint.strokeWidth = 15.dp
      linePaint.color = indicatorColor
      canvas.drawCircle(
        xs[index],
        ys[index],
        4.dp,
        linePaint
      )

      linePaint.strokeWidth = 2.dp
      linePaint.color = indicatorCenterColor
      canvas.drawCircle(
        xs[index],
        ys[index],
        4.dp,
        linePaint
      )
    }

  }

  fun release() {
    listener = null
  }

}