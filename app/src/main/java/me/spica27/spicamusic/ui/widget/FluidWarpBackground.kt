package me.spica27.spicamusic.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 【动态流体封面】背景。
 *
 * 移植自 better-lyrics/kawarp（MIT License, https://github.com/better-lyrics/kawarp）：
 * "Fluid animated backgrounds powered by WebGL, Kawase blur & domain warping."
 *
 * @param coverColor 封面主色，作为暗部 tint 色与无封面时的渐变源
 * @param fftDrawData FFT 频谱数据（31 频段，0..1）
 * @param isDarkMode 暗色模式；null 时按当前主题背景亮度自动判断
 * @param coverUri 封面 Uri 提供器
 */
@Composable
fun FluidWarpBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    fftDrawData: FloatArray = FloatArray(0),
    isDarkMode: Boolean? = null,
    coverUri: () -> Uri? = { null },
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { FluidWarpRenderer() }
    val surfaceViewHolder = remember { mutableStateOf<FluidWarpSurfaceView?>(null) }
    val effectiveIsDarkMode = isDarkMode ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)

    SideEffect {
        val (level, beat) = analyzeFluidSpectrum(fftDrawData)
        renderer.updateAudio(level = level, beat = beat)
        renderer.updateTintColor(coverColor)
        renderer.updateDarkMode(effectiveIsDarkMode)
    }

    // 封面变化时在 IO 线程解码小图（≤256px），失败/缺封面时回退主色渐变。
    // 仅 uri 为空时才让 coverColor 参与 key：避免调色板异步就绪触发无谓的重解码
    val uri = coverUri()
    val fallbackColorKey = if (uri == null) coverColor else null
    LaunchedEffect(uri, fallbackColorKey) {
        val bitmap =
            withContext(Dispatchers.IO) {
                decodeCoverBitmap(context, uri) ?: createGradientFallback(coverColor)
            }
        renderer.submitCover(bitmap)
    }

    DisposableEffect(lifecycleOwner, surfaceViewHolder.value) {
        val surfaceView = surfaceViewHolder.value
        if (surfaceView == null) {
            onDispose {}
        } else {
            val observer =
                object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        surfaceView.onResume()
                    }

                    override fun onPause(owner: LifecycleOwner) {
                        surfaceView.onPause()
                    }
                }

            lifecycleOwner.lifecycle.addObserver(observer)
            surfaceView.onResume()

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                surfaceView.onPause()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FluidWarpSurfaceView(ctx, renderer).also {
                surfaceViewHolder.value = it
            }
        },
        update = { surfaceView ->
            surfaceViewHolder.value = surfaceView
        },
    )
}

// ──────────────────────────────────────────────────────────────────────────
// 频谱分析 / 封面解码
// ──────────────────────────────────────────────────────────────────────────

/** 返回 (整体响度 RMS, 低频节拍能量)，均为 0..1 */
private fun analyzeFluidSpectrum(fftDrawData: FloatArray): Pair<Float, Float> {
    if (fftDrawData.isEmpty()) return 0f to 0f

    var rmsSum = 0f
    var lowSum = 0f
    val lowEnd = (fftDrawData.size * 0.2f).toInt().coerceAtLeast(1)
    for (index in fftDrawData.indices) {
        val sample = fftDrawData[index].coerceIn(0f, 1f)
        rmsSum += sample * sample
        if (index < lowEnd) lowSum += sample
    }
    val level = (sqrt(rmsSum / fftDrawData.size) * 1.2f).coerceIn(0f, 1f)
    val beat = (lowSum / lowEnd * 1.4f).coerceIn(0f, 1f)
    return level to beat
}

/** 两段式采样解码封面到 ≤256px（模糊管线会进一步降到 128，无需高分辨率） */
private fun decodeCoverBitmap(
    context: Context,
    uri: Uri?,
): Bitmap? {
    if (uri == null) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= 256 && bounds.outHeight / (sampleSize * 2) >= 256) {
            sampleSize *= 2
        }
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (_: Exception) {
        null
    }
}

/** 无封面回退：主色调对角三段渐变（等价 kawarp loadGradient） */
private fun createGradientFallback(color: Color): Bitmap {
    val size = 128
    val base = color.toArgb()
    val lighter = ColorUtils.blendARGB(base, android.graphics.Color.WHITE, 0.35f)
    val darker = ColorUtils.blendARGB(base, android.graphics.Color.BLACK, 0.35f)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
                LinearGradient(
                    0f,
                    0f,
                    size.toFloat(),
                    size.toFloat(),
                    intArrayOf(lighter, base, darker),
                    null,
                    Shader.TileMode.CLAMP,
                )
        }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    return bitmap
}

// ──────────────────────────────────────────────────────────────────────────
// GLSurfaceView / Renderer
// ──────────────────────────────────────────────────────────────────────────

private class FluidWarpSurfaceView(
    context: Context,
    renderer: FluidWarpRenderer,
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

private class FluidWarpRenderer : GLSurfaceView.Renderer {
    // ── UI 线程写入、GL 线程读取的输入 ──
    private val pendingCover = AtomicReference<Bitmap?>(null)

    @Volatile
    private var rawLevel = 0f

    @Volatile
    private var rawBeat = 0f

    @Volatile
    private var tintR = 0.157f

    @Volatile
    private var tintG = 0.157f

    @Volatile
    private var tintB = 0.235f

    @Volatile
    private var tintDirty = false

    @Volatile
    private var isDarkMode = true

    fun submitCover(bitmap: Bitmap) {
        // 覆盖尚未消费的旧封面即可，Bitmap 交给 GC 回收（256px 小图，无手动 recycle 的竞态风险）
        pendingCover.set(bitmap)
    }

    fun updateAudio(
        level: Float,
        beat: Float,
    ) {
        rawLevel = level
        rawBeat = beat
    }

    fun updateTintColor(color: Color) {
        // 暗部 tint 色取主色调压暗版本，接近 kawarp 默认深色 tint 的观感
        val r = color.red * 0.55f
        val g = color.green * 0.55f
        val b = color.blue * 0.55f
        if (r != tintR || g != tintG || b != tintB) {
            tintR = r
            tintG = g
            tintB = b
            tintDirty = true
        }
    }

    fun updateDarkMode(dark: Boolean) {
        isDarkMode = dark
    }

    // ── 以下状态仅 GL 线程访问 ──
    private var tintProgram = 0
    private var blurProgram = 0
    private var blendProgram = 0
    private var warpProgram = 0
    private var outputProgram = 0

    // uniform 位置
    private var uTintTexture = 0
    private var uTintColor = 0
    private var uTintIntensity = 0
    private var uBlurTexture = 0
    private var uBlurResolution = 0
    private var uBlurOffset = 0
    private var uBlendTexture1 = 0
    private var uBlendTexture2 = 0
    private var uBlendFactor = 0
    private var uWarpTexture = 0
    private var uWarpTime = 0
    private var uWarpIntensity = 0
    private var uOutTexture = 0
    private var uOutSaturation = 0
    private var uOutDithering = 0
    private var uOutTime = 0
    private var uOutScale = 0
    private var uOutResolution = 0
    private var uOutDark = 0

    private var sourceTexture = 0
    private var blurFbo1 = Fbo()
    private var blurFbo2 = Fbo()
    private var albumFboA = Fbo()
    private var albumFboB = Fbo()
    private var warpFbo = Fbo()

    /** kawarp 语义：nextAlbum 持有当前展示图，currentAlbum 持有淡出的旧图 */
    private var currentAlbum = albumFboA
    private var nextAlbum = albumFboB

    private var halfFloatSupported = false
    private var hasContent = false
    private var isTransitioning = false
    private var transitionStartNs = 0L

    private var surfaceWidth = 1
    private var surfaceHeight = 1

    /** 上一次成功处理的封面，用于 EGL 上下文重建后恢复画面 */
    private var lastCover: Bitmap? = null
    private var snapNextCover = false

    // 动画状态（kawarp 的 accumulatedTime / animationSpeed 平滑）
    private var lastFrameNs = 0L
    private var accumulatedTime = 0f
    private var animationSpeed = BASE_SPEED
    private var smoothedBeat = 0f
    private var smoothedLevel = 0f

    private val positionBuffer = createFloatBuffer(QUAD_POSITIONS)
    private val texCoordBuffer = createFloatBuffer(QUAD_TEXCOORDS)
    private val flippedTexCoordBuffer = createFloatBuffer(QUAD_TEXCOORDS_FLIPPED)

    private class Fbo {
        var framebuffer = 0
        var texture = 0
        var width = 0
        var height = 0
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?,
    ) {
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS).orEmpty()
        halfFloatSupported =
            extensions.contains("GL_OES_texture_half_float") &&
            extensions.contains("GL_OES_texture_half_float_linear")

        tintProgram = createProgram(VERTEX_SHADER, TINT_SHADER)
        blurProgram = createProgram(VERTEX_SHADER, KAWASE_BLUR_SHADER)
        blendProgram = createProgram(VERTEX_SHADER, BLEND_SHADER)
        warpProgram = createProgram(VERTEX_SHADER, DOMAIN_WARP_SHADER)
        outputProgram = createProgram(VERTEX_SHADER, OUTPUT_SHADER)

        uTintTexture = GLES20.glGetUniformLocation(tintProgram, "uTexture")
        uTintColor = GLES20.glGetUniformLocation(tintProgram, "uTintColor")
        uTintIntensity = GLES20.glGetUniformLocation(tintProgram, "uTintIntensity")
        uBlurTexture = GLES20.glGetUniformLocation(blurProgram, "uTexture")
        uBlurResolution = GLES20.glGetUniformLocation(blurProgram, "uResolution")
        uBlurOffset = GLES20.glGetUniformLocation(blurProgram, "uOffset")
        uBlendTexture1 = GLES20.glGetUniformLocation(blendProgram, "uTexture1")
        uBlendTexture2 = GLES20.glGetUniformLocation(blendProgram, "uTexture2")
        uBlendFactor = GLES20.glGetUniformLocation(blendProgram, "uBlend")
        uWarpTexture = GLES20.glGetUniformLocation(warpProgram, "uTexture")
        uWarpTime = GLES20.glGetUniformLocation(warpProgram, "uTime")
        uWarpIntensity = GLES20.glGetUniformLocation(warpProgram, "uIntensity")
        uOutTexture = GLES20.glGetUniformLocation(outputProgram, "uTexture")
        uOutSaturation = GLES20.glGetUniformLocation(outputProgram, "uSaturation")
        uOutDithering = GLES20.glGetUniformLocation(outputProgram, "uDithering")
        uOutTime = GLES20.glGetUniformLocation(outputProgram, "uTime")
        uOutScale = GLES20.glGetUniformLocation(outputProgram, "uScale")
        uOutResolution = GLES20.glGetUniformLocation(outputProgram, "uResolution")
        uOutDark = GLES20.glGetUniformLocation(outputProgram, "uDark")

        sourceTexture = createTexture()

        blurFbo1 = createFbo(BLUR_SIZE, BLUR_SIZE)
        blurFbo2 = createFbo(BLUR_SIZE, BLUR_SIZE)
        albumFboA = createFbo(BLUR_SIZE, BLUR_SIZE)
        albumFboB = createFbo(BLUR_SIZE, BLUR_SIZE)
        currentAlbum = albumFboA
        nextAlbum = albumFboB
        warpFbo = Fbo()

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)

        // EGL 上下文重建（或首帧）：恢复上一张封面，直接落定不播过渡
        hasContent = false
        isTransitioning = false
        lastFrameNs = 0L
        lastCover?.let {
            pendingCover.compareAndSet(null, it)
            snapNextCover = true
        }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int,
    ) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        // 域扭曲 FBO 用半分辨率：内容本身是重度模糊色场，线性放大无可见差异，
        // 但 warp 通道的填充率成本降为 1/4
        val warpW = (surfaceWidth / 2).coerceAtLeast(1)
        val warpH = (surfaceHeight / 2).coerceAtLeast(1)
        if (warpFbo.framebuffer != 0) deleteFbo(warpFbo)
        warpFbo = createFbo(warpW, warpH)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt =
            if (lastFrameNs == 0L) {
                0f
            } else {
                ((now - lastFrameNs) / 1_000_000_000.0f).coerceIn(0f, 0.05f)
            }
        lastFrameNs = now

        // 1. 有新封面则上传并重建模糊色场（仅换图时执行，等价 kawarp processNewImage）。
        // 首张封面直接落定：此时另一块 album FBO 内容未初始化，不能作为淡出源
        pendingCover.getAndSet(null)?.let { bitmap ->
            uploadAndProcessCover(bitmap, snap = snapNextCover || !hasContent)
            snapNextCover = false
        }

        // 2. tint 色变化（调色板异步就绪）时原地重新模糊，不触发过渡（kawarp reblurCurrentImage）
        if (tintDirty && hasContent) {
            tintDirty = false
            blurSourceInto(nextAlbum)
        }

        // 3. 音频包络：快攻慢放平滑，节拍推高流速与扭曲强度
        smoothedBeat += (rawBeat - smoothedBeat) * smoothingFactor(dt, if (rawBeat > smoothedBeat) 12f else 3f)
        smoothedLevel += (rawLevel - smoothedLevel) * smoothingFactor(dt, 4f)
        val targetSpeed = BASE_SPEED + smoothedBeat * BEAT_SPEED_BOOST
        animationSpeed += (targetSpeed - animationSpeed) * smoothingFactor(dt, 3f)
        accumulatedTime += dt * animationSpeed
        // float 精度保护：连续渲染 ~68 分钟后回卷一次（噪声场瞬移，感知极低）
        if (accumulatedTime > TIME_WRAP) accumulatedTime -= TIME_WRAP

        // 清屏仅用于尚无内容的空窗期；有内容时输出通道覆盖全屏无需 clear。
        // 必须先显式绑回默认 framebuffer：封面处理路径（blurSourceInto）会把绑定
        // 留在 128×128 的 album FBO 上，直接 clear 会把刚生成的色场抹成纯色
        if (!hasContent) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            if (isDarkMode) {
                GLES20.glClearColor(0.05f, 0.05f, 0.06f, 1f)
            } else {
                GLES20.glClearColor(0.96f, 0.96f, 0.97f, 1f)
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }

        // 4. 过渡混合（换歌交叉淡化）
        var blendFactor = 1f
        if (isTransitioning) {
            val elapsedMs = (now - transitionStartNs) / 1_000_000f
            blendFactor = (elapsedMs / TRANSITION_DURATION_MS).coerceAtMost(1f)
            if (blendFactor >= 1f) isTransitioning = false
        }

        val warpSourceTexture =
            if (isTransitioning) {
                GLES20.glUseProgram(blendProgram)
                bindFbo(blurFbo1)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentAlbum.texture)
                GLES20.glUniform1i(uBlendTexture1, 0)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, nextAlbum.texture)
                GLES20.glUniform1i(uBlendTexture2, 1)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glUniform1f(uBlendFactor, blendFactor)
                drawQuad(texCoordBuffer)
                blurFbo1.texture
            } else {
                nextAlbum.texture
            }

        // 5. 域扭曲：小色场 → 半分辨率 warp FBO
        GLES20.glUseProgram(warpProgram)
        bindFbo(warpFbo)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, warpSourceTexture)
        GLES20.glUniform1i(uWarpTexture, 0)
        GLES20.glUniform1f(uWarpTime, accumulatedTime)
        GLES20.glUniform1f(uWarpIntensity, WARP_INTENSITY_BASE + smoothedBeat * WARP_INTENSITY_BEAT)
        drawQuad(texCoordBuffer)

        // 6. 输出：暗角 + 饱和度 + 明暗适配 + 时间抖动
        GLES20.glUseProgram(outputProgram)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, warpFbo.texture)
        GLES20.glUniform1i(uOutTexture, 0)
        GLES20.glUniform1f(uOutSaturation, SATURATION_BASE + smoothedLevel * SATURATION_LEVEL_BOOST)
        GLES20.glUniform1f(uOutDithering, DITHERING)
        GLES20.glUniform1f(uOutTime, accumulatedTime)
        GLES20.glUniform1f(uOutScale, OUTPUT_SCALE)
        GLES20.glUniform2f(uOutResolution, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(uOutDark, if (isDarkMode) 1f else 0f)
        drawQuad(texCoordBuffer)
    }

    /** 上传封面 → tint+模糊到 nextAlbum，必要时启动交叉淡化（kawarp processNewImage） */
    private fun uploadAndProcessCover(
        bitmap: Bitmap,
        snap: Boolean,
    ) {
        if (bitmap.isRecycled) return
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTexture)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        lastCover = bitmap

        // 交换 current/next：current 变为淡出的旧图
        val swap = currentAlbum
        currentAlbum = nextAlbum
        nextAlbum = swap

        blurSourceInto(nextAlbum)
        hasContent = true

        if (snap || TRANSITION_DURATION_MS <= 0f) {
            isTransitioning = false
        } else {
            isTransitioning = true
            transitionStartNs = System.nanoTime()
        }
    }

    /** tint → N 次 Kawase 模糊 → 拷入目标 FBO（kawarp blurSourceInto） */
    private fun blurSourceInto(target: Fbo) {
        // Step 1: 暗部着色。使用垂直翻转纹理坐标修正 Bitmap 上传方向
        GLES20.glUseProgram(tintProgram)
        bindFbo(blurFbo1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTexture)
        GLES20.glUniform1i(uTintTexture, 0)
        GLES20.glUniform3f(uTintColor, tintR, tintG, tintB)
        GLES20.glUniform1f(uTintIntensity, TINT_INTENSITY)
        drawQuad(flippedTexCoordBuffer)

        // Step 2: Kawase 模糊 ping-pong
        GLES20.glUseProgram(blurProgram)
        GLES20.glUniform2f(uBlurResolution, BLUR_SIZE.toFloat(), BLUR_SIZE.toFloat())
        GLES20.glUniform1i(uBlurTexture, 0)

        var read = blurFbo1
        var write = blurFbo2
        repeat(BLUR_PASSES) { pass ->
            bindFbo(write)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, read.texture)
            GLES20.glUniform1f(uBlurOffset, pass + 0.5f)
            drawQuad(texCoordBuffer)
            val tmp = read
            read = write
            write = tmp
        }

        // Step 3: 结果拷入目标（offset=0 的模糊即等价拷贝）
        bindFbo(target)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, read.texture)
        GLES20.glUniform1f(uBlurOffset, 0f)
        drawQuad(texCoordBuffer)
    }

    // ── GL 基础设施 ──

    private fun drawQuad(texCoords: FloatBuffer) {
        positionBuffer.position(0)
        GLES20.glEnableVertexAttribArray(ATTRIB_POSITION)
        GLES20.glVertexAttribPointer(ATTRIB_POSITION, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
        texCoords.position(0)
        GLES20.glEnableVertexAttribArray(ATTRIB_TEXCOORD)
        GLES20.glVertexAttribPointer(ATTRIB_TEXCOORD, 2, GLES20.GL_FLOAT, false, 0, texCoords)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    }

    private fun bindFbo(fbo: Fbo) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo.framebuffer)
        GLES20.glViewport(0, 0, fbo.width, fbo.height)
    }

    private fun createTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        return ids[0]
    }

    /**
     * 创建 FBO；支持时优先半浮点纹理（多次模糊下防色带，kawarp 同款优化）。
     * OES_texture_half_float 不保证可渲染，attach 后校验完整性，失败回退 UNSIGNED_BYTE。
     */
    private fun createFbo(
        width: Int,
        height: Int,
    ): Fbo {
        if (halfFloatSupported) {
            val fbo = tryCreateFbo(width, height, GL_HALF_FLOAT_OES)
            if (fbo != null) return fbo
        }
        return tryCreateFbo(width, height, GLES20.GL_UNSIGNED_BYTE)
            ?: Fbo().also {
                it.width = width
                it.height = height
            }
    }

    private fun tryCreateFbo(
        width: Int,
        height: Int,
        type: Int,
    ): Fbo? {
        val texture = createTexture()
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            type,
            null,
        )
        val ids = IntArray(1)
        GLES20.glGenFramebuffers(1, ids, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, ids[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texture,
            0,
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            GLES20.glDeleteFramebuffers(1, ids, 0)
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            return null
        }
        return Fbo().also {
            it.framebuffer = ids[0]
            it.texture = texture
            it.width = width
            it.height = height
        }
    }

    private fun deleteFbo(fbo: Fbo) {
        if (fbo.framebuffer != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fbo.framebuffer), 0)
        }
        if (fbo.texture != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(fbo.texture), 0)
        }
        fbo.framebuffer = 0
        fbo.texture = 0
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        // 显式绑定 attribute 槽位，保证所有 program 共享同一套顶点布局
        GLES20.glBindAttribLocation(program, ATTRIB_POSITION, "aPosition")
        GLES20.glBindAttribLocation(program, ATTRIB_TEXCOORD, "aTexCoord")
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val message = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            throw IllegalStateException("Failed to link fluid warp program: $message")
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Failed to compile fluid warp shader: $message")
        }
        return shader
    }

    private fun smoothingFactor(
        dt: Float,
        rate: Float,
    ): Float = 1f - exp(-dt * rate)

    private companion object {
        /** GLES2 扩展常量（GLES20 类未定义） */
        const val GL_HALF_FLOAT_OES = 0x8D61

        const val ATTRIB_POSITION = 0
        const val ATTRIB_TEXCOORD = 1

        /** 模糊工作纹理尺寸（kawarp BLUR_SIZE） */
        const val BLUR_SIZE = 128

        /** Kawase 模糊次数（kawarp 默认 8） */
        const val BLUR_PASSES = 8

        /** 换歌交叉淡化时长（kawarp transitionDuration 默认 1000ms） */
        const val TRANSITION_DURATION_MS = 900f

        /** 暗部着色强度（kawarp tintIntensity 默认 0.15，略调高让主色更显） */
        const val TINT_INTENSITY = 0.35f

        /** 基础流速与节拍增益：无声时缓慢漂移，重拍时明显加速 */
        const val BASE_SPEED = 0.55f
        const val BEAT_SPEED_BOOST = 2.6f

        /** 域扭曲强度（kawarp warpIntensity 默认 1.0）与节拍增益 */
        const val WARP_INTENSITY_BASE = 0.85f
        const val WARP_INTENSITY_BEAT = 0.3f

        /** 饱和度（kawarp 默认 1.5）与响度增益 */
        const val SATURATION_BASE = 1.45f
        const val SATURATION_LEVEL_BOOST = 0.35f

        /** 抖动幅度（kawarp 默认 0.008，防色带） */
        const val DITHERING = 0.008f

        /** 输出缩放（kawarp scale，1.0 = 不缩放） */
        const val OUTPUT_SCALE = 1f

        /** accumulatedTime 回卷周期（秒），防 float 精度退化 */
        const val TIME_WRAP = 4096f

        val QUAD_POSITIONS =
            floatArrayOf(
                -1f,
                -1f,
                1f,
                -1f,
                -1f,
                1f,
                -1f,
                1f,
                1f,
                -1f,
                1f,
                1f,
            )
        val QUAD_TEXCOORDS =
            floatArrayOf(
                0f,
                0f,
                1f,
                0f,
                0f,
                1f,
                0f,
                1f,
                1f,
                0f,
                1f,
                1f,
            )

        /** Bitmap 上传后纹理原点在左上，翻转 V 修正到 GL 坐标 */
        val QUAD_TEXCOORDS_FLIPPED =
            floatArrayOf(
                0f,
                1f,
                1f,
                1f,
                0f,
                0f,
                0f,
                0f,
                1f,
                1f,
                1f,
                0f,
            )

        fun createFloatBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer
                .allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(data)
                    position(0)
                }

        const val PRECISION_HEADER =
            """
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            """

        const val VERTEX_SHADER =
            """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
                vTexCoord = aTexCoord;
            }
            """

        /** 暗部向主色调着色（kawarp TINT_SHADER） */
        const val TINT_SHADER =
            PRECISION_HEADER + """
            uniform sampler2D uTexture;
            uniform vec3 uTintColor;
            uniform float uTintIntensity;
            varying vec2 vTexCoord;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                float darkMask = 1.0 - smoothstep(0.0, 0.5, luma);
                color.rgb = mix(color.rgb, uTintColor, darkMask * uTintIntensity);
                gl_FragColor = color;
            }
            """

        /** Kawase 模糊单 pass（kawarp KAWASE_BLUR_SHADER） */
        const val KAWASE_BLUR_SHADER =
            PRECISION_HEADER + """
            uniform sampler2D uTexture;
            uniform vec2 uResolution;
            uniform float uOffset;
            varying vec2 vTexCoord;

            void main() {
                vec2 texelSize = 1.0 / uResolution;
                vec4 color = vec4(0.0);
                color += texture2D(uTexture, vTexCoord + vec2(-uOffset, -uOffset) * texelSize);
                color += texture2D(uTexture, vTexCoord + vec2(uOffset, -uOffset) * texelSize);
                color += texture2D(uTexture, vTexCoord + vec2(-uOffset, uOffset) * texelSize);
                color += texture2D(uTexture, vTexCoord + vec2(uOffset, uOffset) * texelSize);
                gl_FragColor = color * 0.25;
            }
            """

        /** 双纹理交叉淡化（kawarp BLEND_SHADER） */
        const val BLEND_SHADER =
            PRECISION_HEADER + """
            uniform sampler2D uTexture1;
            uniform sampler2D uTexture2;
            uniform float uBlend;
            varying vec2 vTexCoord;

            void main() {
                vec4 color1 = texture2D(uTexture1, vTexCoord);
                vec4 color2 = texture2D(uTexture2, vTexCoord);
                gl_FragColor = mix(color1, color2, uBlend);
            }
            """

        /** 双八度 simplex 噪声域扭曲（kawarp DOMAIN_WARP_SHADER，原样移植） */
        const val DOMAIN_WARP_SHADER =
            PRECISION_HEADER + """
            uniform sampler2D uTexture;
            uniform float uTime;
            uniform float uIntensity;
            varying vec2 vTexCoord;

            vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
            vec3 permute(vec3 x) { return mod289(((x*34.0)+1.0)*x); }

            float snoise(vec2 v) {
                const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                                    -0.577350269189626, 0.024390243902439);
                vec2 i  = floor(v + dot(v, C.yy));
                vec2 x0 = v - i + dot(i, C.xx);
                vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
                vec4 x12 = x0.xyxy + C.xxzz;
                x12.xy -= i1;
                i = mod289(i);
                vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
                vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
                m = m*m; m = m*m;
                vec3 x = 2.0 * fract(p * C.www) - 1.0;
                vec3 h = abs(x) - 0.5;
                vec3 ox = floor(x + 0.5);
                vec3 a0 = x - ox;
                m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
                vec3 g;
                g.x = a0.x * x0.x + h.x * x0.y;
                g.yz = a0.yz * x12.xz + h.yz * x12.yw;
                return 130.0 * dot(m, g);
            }

            void main() {
                vec2 uv = vTexCoord;
                float t = uTime * 0.05;

                vec2 center = uv - 0.5;
                float centerWeight = 1.0 - smoothstep(0.0, 0.7, length(center));

                float n1 = snoise(uv * 0.35 + vec2(t, t * 0.7));
                float n2 = snoise(uv * 0.35 + vec2(-t * 0.8, t * 0.5) + vec2(50.0, 50.0));

                float n3 = snoise(uv * 0.9 + vec2(t * 1.2, -t) + vec2(100.0, 0.0));
                float n4 = snoise(uv * 0.9 + vec2(-t, t * 1.1) + vec2(0.0, 100.0));

                vec2 warp = vec2(
                    n1 * 0.65 + n3 * 0.35,
                    n2 * 0.65 + n4 * 0.35
                ) * centerWeight;

                vec2 warpedUV = uv + warp * uIntensity;
                warpedUV = clamp(warpedUV, 0.0, 1.0);

                gl_FragColor = texture2D(uTexture, warpedUV);
            }
            """

        /** 输出：暗角 + 饱和度 + 抖动（kawarp OUTPUT_SHADER）+ 本地明暗适配 */
        const val OUTPUT_SHADER =
            PRECISION_HEADER + """
            uniform sampler2D uTexture;
            uniform float uSaturation;
            uniform float uDithering;
            uniform float uTime;
            uniform float uScale;
            uniform vec2 uResolution;
            uniform float uDark;
            varying vec2 vTexCoord;

            float hash(vec3 p) {
                p = fract(p * 0.1031);
                p += dot(p, p.zyx + 31.32);
                return fract((p.x + p.y) * p.z);
            }

            void main() {
                vec2 uv = (vTexCoord - 0.5) / uScale + 0.5;
                uv = clamp(uv, 0.0, 1.0);

                vec4 color = texture2D(uTexture, uv);

                vec2 center = vTexCoord - 0.5;
                float vignette = 1.0 - dot(center, center) * 0.3;
                color.rgb *= vignette;

                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                color.rgb = mix(vec3(gray), color.rgb, uSaturation);

                // 明暗模式适配：暗色压暗保证前景可读，亮色向白提亮成柔和水彩
                vec3 lightened = mix(color.rgb, vec3(1.0), 0.42);
                vec3 darkened = color.rgb * 0.6;
                color.rgb = mix(lightened, darkened, uDark);

                vec2 pixelPos = floor(vTexCoord * uResolution);
                float noise = hash(vec3(pixelPos, floor(uTime * 60.0)));
                color.rgb += (noise - 0.5) * uDithering;

                gl_FragColor = vec4(color.rgb, 1.0);
            }
            """
    }
}
