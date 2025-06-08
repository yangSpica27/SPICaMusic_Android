package me.spica27.spicamusic.visualiser.drawable

import android.R.attr.x
import android.R.attr.y
import android.graphics.Canvas
import android.graphics.Color
import com.google.fpl.liquidfun.Body
import com.google.fpl.liquidfun.BodyDef
import com.google.fpl.liquidfun.BodyType
import com.google.fpl.liquidfun.CircleShape
import com.google.fpl.liquidfun.FixtureDef
import com.google.fpl.liquidfun.ParticleGroupDef
import com.google.fpl.liquidfun.ParticleSystem
import com.google.fpl.liquidfun.ParticleSystemDef
import com.google.fpl.liquidfun.PolygonShape
import com.google.fpl.liquidfun.Vec2
import com.google.fpl.liquidfun.World
import me.spica27.spicamusic.utils.dp
import timber.log.Timber
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder


class RainVisualiserDrawable : VisualiserDrawable(), Closeable {

  private var world: World? = null


  companion object {
    const val ParticleMaxCount = 3550

    // 模拟世界和view坐标的转化比例
    const val mProportion = 200f
  }


  private val rainItemDef = ParticleGroupDef()
  private val rainItemShape = PolygonShape()

  var system: ParticleSystem? = null



  override fun setBounds(width: Int, height: Int) {
    super.setBounds(width, height)
    synchronized(this) {
      if (world != null) {
        try {
          world?.delete()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
      world = World(0f, 10f)
      val psd = ParticleSystemDef()
      psd.dampingStrength = 1f
      psd.density = 1f
      psd.maxCount = ParticleMaxCount
      psd.radius = mappingView2Body(1.dp)
      psd.strictContactCheck = false
      psd.particleWithBodyPressureStrength = 0.1f
      psd.springStrength = 0.01f
      psd.viscousStrength = 20f
      system = world?.createParticleSystem(psd)
      psd.delete()

      rainItemDef.shape = rainItemShape
      rainItemDef.linearVelocity = Vec2(0f, 10f)

      rainItemShape.set(
        arrayOf(
          Vec2(mappingView2Body(0f), mappingView2Body(0f)),
          Vec2(mappingView2Body(0f + width), mappingView2Body(0f)),
          Vec2(mappingView2Body(0f + width), mappingView2Body(0f + height/2f)),
          Vec2(mappingView2Body(0f), mappingView2Body(0f + height/2f)),
        ),
        4,
      )
      system?.createParticleGroup(rainItemDef)


      val coverDef = BodyDef()
      coverDef.type = BodyType.staticBody
      val coverShape = CircleShape()

      coverShape.radius = mappingView2Body(radius * 1f - 12.dp)
      val coverFixtureDef = FixtureDef()
      coverFixtureDef.shape = coverShape
      coverDef.setPosition(mappingView2Body(width / 2f), mappingView2Body(height / 2f))
      val coverBody = world?.createBody(coverDef)
      coverBody?.createFixture(coverFixtureDef)

      // 创建底部
      val bodyDef = BodyDef()
      // 创建静止刚体
      bodyDef.type = BodyType.staticBody
      // 定义的形状
      val box = PolygonShape()
      box.setAsBox(mappingView2Body(width * 1f), mappingView2Body(1f))

      val fixtureDef = FixtureDef()
      fixtureDef.shape = box

      bodyDef.setPosition(
        0f,
        mappingView2Body(height * 1f)
      )
      val bottomBody: Body? = world?.createBody(bodyDef)

      bottomBody?.createFixture(fixtureDef)

      // 创建左侧
      box.setAsBox(1 * 1f, mappingView2Body(height * 1f))
      fixtureDef.shape = box

      bodyDef.setPosition(
        0f,
        0f,
      )
      fixtureDef.shape = box
      val leftBody = world?.createBody(bodyDef)

      leftBody?.createFixture(fixtureDef)
      // 创建右侧
      box.setAsBox(1 * 1f, mappingView2Body(height * 1f))
      fixtureDef.shape = box

      bodyDef.setPosition(
        mappingView2Body(width * 1f),
        0f,
      )
      fixtureDef.shape = box
      val rightBody = world?.createBody(bodyDef)
      rightBody?.createFixture(fixtureDef)

    }
  }


  // 粒子的位置缓冲区
  private var mParticlePositionBuffer: ByteBuffer =
    ByteBuffer
      .allocateDirect(2 * 4 * ParticleMaxCount)
      .order(ByteOrder.nativeOrder())

  // 粒子的位置数组
  private val positionArray = FloatArray(ParticleMaxCount * 2)

  // 过滤屏幕之外的点
  private val positionArray2 = FloatArray(ParticleMaxCount * 2)

  override fun draw(canvas: Canvas) {
    rainPaint.color = themeColor
    synchronized(this) {
      if (world == null || system == null) return
      world?.step(1f / 30f, 8, 3, 3)
      mParticlePositionBuffer.rewind()
      system?.copyPositionBuffer(
        0,
        system?.particleCount!!,
        mParticlePositionBuffer,
      )
      mParticlePositionBuffer.asFloatBuffer().get(positionArray)

      var index = 0

      for (i in positionArray.indices step 2) {
        // 去除超出屏幕范围外的点
        positionArray[i] = positionArray[i] * mProportion
        positionArray[i + 1] = positionArray[i + 1] * mProportion

        if (positionArray[i] < 0 ||
          positionArray[i] > width ||
          positionArray[i + 1] < 0 ||
          positionArray[i + 1] > height
        ) {
          // 超出屏幕范围的点不做绘制
        } else {
          positionArray2[index] = positionArray[i]
          positionArray2[index + 1] = positionArray[i + 1]
          index += 2
        }
      }
      canvas.drawPoints(positionArray2, 0, index / 2, rainPaint)
    }
  }

  private val rainPaint = android.graphics.Paint().apply {
    color = themeColor
    style = android.graphics.Paint.Style.FILL
    strokeCap = android.graphics.Paint.Cap.ROUND
    strokeWidth = 2.dp
  }

  override fun update(list: List<Float>) {

  }


  // view坐标系转化为模拟世界坐标系
  private fun mappingView2Body(view: Float): Float = view / mProportion

  // 模拟世界坐标系转化为view坐标系
  private fun mappingBody2View(body: Float): Float = body * mProportion


  override fun close() {
    synchronized(this) {
      if (world != null) {
        try {
          system?.delete()
          world?.delete()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }
}