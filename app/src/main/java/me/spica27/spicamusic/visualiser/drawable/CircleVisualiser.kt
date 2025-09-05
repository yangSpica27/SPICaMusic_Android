package me.spica27.spicamusic.visualiser.drawable

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withScale
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.utils.dp
import me.spica27.spicamusic.visualiser.MusicVisualiser
import kotlin.math.cos
import kotlin.math.sin

@UnstableApi
class CircleVisualiser : VisualiserDrawable() {
    private val paint =
        Paint().apply {

            pathEffect = CornerPathEffect(10f)
            style = Paint.Style.FILL_AND_STROKE
            setMaskFilter(
                BlurMaskFilter(
                    43f,
                    BlurMaskFilter.Blur.SOLID,
                ),
            )
            pathEffect = CornerPathEffect(10f)
            strokeCap = Paint.Cap.ROUND
        }

    private val paint2 =
        Paint().apply {
            pathEffect = CornerPathEffect(10f)
            style = Paint.Style.FILL_AND_STROKE
            setMaskFilter(
                BlurMaskFilter(
                    103f,
                    BlurMaskFilter.Blur.NORMAL,
                ),
            )
            pathEffect = CornerPathEffect(10f)
            strokeCap = Paint.Cap.ROUND
        }

    private val paint3 = Paint()

    private val path1 = Path()

    private val path2 = Path()

    private val decelerateInterpolator by lazy {
        android.view.animation.DecelerateInterpolator()
    }

    override fun setBounds(
        width: Int,
        height: Int,
    ) {
        super.setBounds(width, height)
        paint.setShader(
            SweepGradientShader(
                center = Offset(width / 2f, height / 2f),
                colors =
                    listOf(
                        Color("#D0F2FB".toColorInt()),
                        Color("#D3E6F7".toColorInt()),
                        Color("#9FC1E0".toColorInt()),
                        Color("#DCDEF6".toColorInt()),
                        Color("#F3F1FC".toColorInt()),
                        Color("#D0F2FB".toColorInt()),
                    ),
                colorStops =
                    listOf(
                        0.0f,
                        .25f,
                        .5f,
                        .75f,
                        85f,
                        1f,
                    ),
            ),
        )
        paint2.setShader(
            SweepGradientShader(
                center = Offset(width / 2f, height / 2f),
                colors =
                    listOf(
                        Color("#A7D0F0".toColorInt()),
                        Color("#D3E6F7".toColorInt()),
                        Color("#9FC1E0".toColorInt()),
                        Color("#DCDEF6".toColorInt()),
                        Color("#F3F1FC".toColorInt()),
                        Color("#D0F2FB".toColorInt()),
                    ),
                colorStops =
                    listOf(
                        0.0f,
                        .25f,
                        .5f,
                        .75f,
                        85f,
                        1f,
                    ),
            ),
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.translate(width / 2f, height / 2f)
        if (yList.size != lastYList.size) {
            return
        }
        canvas.save()
        path1.reset()
        path2.reset()
        paint.color = themeColor

        val fraction =
            decelerateInterpolator
                .getInterpolation(((System.currentTimeMillis() - lastSampleTime).toFloat() / interval))
                .coerceIn(
                    0f,
                    1f,
                )

        for (index in 0 until yList.size) {
            val lastY = lastYList[index]
            val y = yList[index]

            val curY = lastY + (y - lastY) * fraction

            val curY2 = radius - (curY - radius)

            val angle = 360f / yList.size * index

            val p1 = calcPoint(0, 0, curY.toInt(), angle)

            val p2 = calcPoint(0, 0, curY2.toInt(), angle)

//      canvas.drawLine(p1[0].toFloat(), p1[1].toFloat(), p2[0].toFloat(), p2[1].toFloat(), paint)

            if (index == 0) {
                path1.moveTo(p1[0].toFloat(), p1[1].toFloat())
                path2.moveTo(p2[0].toFloat(), p2[1].toFloat())
            } else {
                path1.lineTo(p1[0].toFloat(), p1[1].toFloat())
                path2.lineTo(p2[0].toFloat(), p2[1].toFloat())
            }
        }

        path1.close()
        path2.close()

        canvas.drawPath(path1, paint)

        canvas.withScale(1.1f, 1.1f, 0f, 0f) {
            canvas.drawPath(path1, paint2)
        }

//    canvas.drawPath(path2, paint)

        canvas.restore()
    }

    /**
     * 计算圆弧上的某一点
     */
    private fun calcPoint(
        centerX: Int,
        centerY: Int,
        radius: Int,
        angle: Float,
    ): IntArray {
        val x = (centerX + radius * cos((angle) * Math.PI / 180)).toInt()
        val y = (centerY + radius * sin((angle) * Math.PI / 180)).toInt()
        return intArrayOf(x, y)
    }

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

    // 上次采样的时间
    private var lastSampleTime = 0L

    // 采样间隔
    private val interval = 125

    override fun update(list: List<Float>) {
        if (((System.currentTimeMillis() - lastSampleTime) < interval) && yList.isNotEmpty() && lastYList.isNotEmpty()) {
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
            val next =
                if (index == list.size - 1) {
                    radius + 30.dp * list[0]
                } else {
                    radius + 30.dp * list[index + 1]
                }
            yList.add(cur.toInt())
            yList.add(next.toInt())
        }
        lastSampleTime = System.currentTimeMillis()
    }
}
