package me.spica27.spicamusic.ui.widget

import android.content.Context
import android.graphics.RuntimeShader
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.intellij.lang.annotations.Language
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

/**
 * 隧道效果着色器背景
 * 基于 tunnel.frag 实现的动态背景效果
 *
 * @param modifier 修饰符
 * @param coverColor 封面主色，用于色彩调整
 * @param fftDrawData FFT 频谱数据
 * @param speed 动画速度
 * @param blend 纹理混合比例
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TunnelShaderBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    fftDrawData: FloatArray = FloatArray(0),
    speed: Float = 1.0f,
    blend: Float = 0.5f,
) {
    // 简化的隧道着色器（移除纹理依赖）
    @Language("AGSL")
    val shaderSource =
        """
        uniform float u_time;
        uniform float u_speed;
        uniform vec2 u_resolution;
        uniform vec4 u_center_color;
        uniform float u_center_radius;
        uniform float u_music_level;
        
        const float kPi = 3.1415927;
        
        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }
        
        vec4 main(vec2 fragCoord) {
            vec2 p = (2.0 * fragCoord.xy - u_resolution.xy) / u_resolution.y;
            float a = atan(p.y, p.x);
            
            vec2 p2 = p * p, p4 = p2 * p2, p8 = p4 * p4;
            float r = pow(p8.x + p8.y, 1.0 / 8.0);
            
            float distortion = 0.3 / r + 0.2 * u_time * u_speed * (1.0 + u_music_level * 0.5);
            // 归一化角度到 0-1 范围，消除断层
            float angle = (a / (2.0 * kPi)) + 0.5;
            
            // 生成渐变色隧道效果，使用 fract 确保色相值在 0-1 范围循环
            float hue1 = fract(distortion * 0.3 + angle * 0.5);
            float hue2 = fract(distortion * 0.3 + angle * 0.5 + 0.1);
            vec3 col1 = hsv2rgb(vec3(hue1, 0.7, 0.8));
            vec3 col2 = hsv2rgb(vec3(hue2, 0.6, 0.9));
            vec3 col = mix(col1, col2, sin(distortion * 3.0) * 0.5 + 0.5);
            
            // 应用封面色调
            col = mix(col, u_center_color.rgb, 0.3);
            
            // 中心淡出效果
            float fadeAmount = 1.0 - smoothstep(0.0, u_center_radius, r);
            col = mix(col, u_center_color.rgb, fadeAmount);
            
            return vec4(col, 1.0);
        }
        """.trimIndent()

    val infiniteTransition = rememberInfiniteTransition(label = "tunnel_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(100000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time",
    )

    // 计算音频驱动参数
    val musicLevel =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                fftDrawData.average().toFloat().coerceIn(0f, 1f)
            }
        }

    val shader = remember { RuntimeShader(shaderSource) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Box(
        modifier =
            modifier.drawWithCache {
                shader.setFloatUniform("u_time", time)
                shader.setFloatUniform("u_speed", speed)
                shader.setFloatUniform("u_resolution", size.width, size.height)
                // 设置颜色为 vec4 (RGBA)
                shader.setFloatUniform(
                    "u_center_color",
                    coverColor.red,
                    coverColor.green,
                    coverColor.blue,
                    coverColor.alpha,
                )
                shader.setFloatUniform("u_center_radius", 0.3f + musicLevel * 0.2f)
                shader.setFloatUniform("u_music_level", musicLevel)

                onDrawBehind {
                    drawRect(shaderBrush)
                }
            },
    )
}

/**
 * 效果着色器背景
 * 基于 effect.glsl 实现的流体效果背景
 *
 * @param modifier 修饰符
 * @param coverColor 封面主色，用于色彩调整
 * @param fftDrawData FFT 频谱数据
 * @param isDarkMode 暗色模式（true）或亮色模式（false），null时自动判断
 */
@Composable
fun EffectShaderBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    fftDrawData: FloatArray = FloatArray(0),
    isDarkMode: Boolean? = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceViewHolder = remember { mutableStateOf<EffectShaderSurfaceView?>(null) }
    val renderer = remember { EffectShaderRenderer() }
    val effectiveIsDarkMode = isDarkMode ?: (MiuixTheme.colorScheme.background.luminance() < 0.5f)

    SideEffect {
        renderer.updateEffect(
            coverColor = coverColor,
            spectrum = analyzeEffectSpectrum(fftDrawData),
            isDarkMode = effectiveIsDarkMode,
        )
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
        factory = { context ->
            EffectShaderSurfaceView(context, renderer).also {
                surfaceViewHolder.value = it
            }
        },
        update = { surfaceView ->
            surfaceViewHolder.value = surfaceView
        },
    )
}

private data class EffectSpectrumLevels(
    val musicLevel: Float,
    val beat: Float,
    val midFreq: Float,
    val highFreq: Float,
)

private fun analyzeEffectSpectrum(fftDrawData: FloatArray): EffectSpectrumLevels {
    if (fftDrawData.isEmpty()) {
        return EffectSpectrumLevels(0f, 0f, 0f, 0f)
    }

    var rmsSum = 0f
    var lowSum = 0f
    var midSum = 0f
    var highSum = 0f
    var lowCount = 0
    var midCount = 0
    var highCount = 0
    val lowEnd = (fftDrawData.size * 0.2f).toInt().coerceAtLeast(1)
    val midEnd = (fftDrawData.size * 0.6f).toInt().coerceIn(lowEnd + 1, fftDrawData.size)

    for (index in fftDrawData.indices) {
        val sample = fftDrawData[index].coerceIn(0f, 1f)
        rmsSum += sample * sample
        when {
            index < lowEnd -> {
                lowSum += sample
                lowCount++
            }

            index < midEnd -> {
                midSum += sample
                midCount++
            }

            else -> {
                highSum += sample
                highCount++
            }
        }
    }

    return EffectSpectrumLevels(
        musicLevel = (sqrt(rmsSum / fftDrawData.size) * 1.2f).coerceIn(0f, 1f),
        beat = ((lowSum / lowCount.coerceAtLeast(1)) * 0.1f).coerceIn(0f, 1f),
        midFreq = ((midSum / midCount.coerceAtLeast(1)) * 0.7f).coerceIn(0f, 1f),
        highFreq = ((highSum / highCount.coerceAtLeast(1)) * 0.6f).coerceIn(0f, 1f),
    )
}

private class EffectShaderSurfaceView(
    context: Context,
    renderer: EffectShaderRenderer,
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

private class EffectShaderRenderer : GLSurfaceView.Renderer {
    @Volatile
    private var musicLevel: Float = 0f

    @Volatile
    private var beat: Float = 0f

    @Volatile
    private var midFreq: Float = 0f

    @Volatile
    private var highFreq: Float = 0f

    @Volatile
    private var colorR: Float = Color(0xFF2196F3).red

    @Volatile
    private var colorG: Float = Color(0xFF2196F3).green

    @Volatile
    private var colorB: Float = Color(0xFF2196F3).blue

    @Volatile
    private var colorA: Float = Color(0xFF2196F3).alpha

    @Volatile
    private var isDarkMode: Boolean = false

    private var program: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var resolutionHandle: Int = 0
    private var animTimeHandle: Int = 0
    private var musicLevelHandle: Int = 0
    private var beatHandle: Int = 0
    private var midFreqHandle: Int = 0
    private var highFreqHandle: Int = 0
    private var colorHandle: Int = 0
    private var darkModeHandle: Int = 0
    private var surfaceWidth: Int = 1
    private var surfaceHeight: Int = 1
    private var startTimeMs: Long = 0L

    private val vertexBuffer: FloatBuffer =
        ByteBuffer
            .allocateDirect(FULLSCREEN_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(FULLSCREEN_VERTICES)
                position(0)
            }

    fun updateEffect(
        coverColor: Color,
        spectrum: EffectSpectrumLevels,
        isDarkMode: Boolean,
    ) {
        musicLevel = spectrum.musicLevel
        beat = spectrum.beat
        midFreq = spectrum.midFreq
        highFreq = spectrum.highFreq
        colorR = coverColor.red
        colorG = coverColor.green
        colorB = coverColor.blue
        colorA = coverColor.alpha
        this.isDarkMode = isDarkMode
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?,
    ) {
        program = createProgram(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution")
        animTimeHandle = GLES20.glGetUniformLocation(program, "uAnimTime")
        musicLevelHandle = GLES20.glGetUniformLocation(program, "uMusicLevel")
        beatHandle = GLES20.glGetUniformLocation(program, "uBeat")
        midFreqHandle = GLES20.glGetUniformLocation(program, "uMidFreq")
        highFreqHandle = GLES20.glGetUniformLocation(program, "uHighFreq")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        darkModeHandle = GLES20.glGetUniformLocation(program, "uIsDarkMode")
        startTimeMs = SystemClock.elapsedRealtime()

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int,
    ) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (program == 0) return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            vertexBuffer,
        )
        GLES20.glEnableVertexAttribArray(positionHandle)

        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            vertexBuffer,
        )
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glUniform2f(resolutionHandle, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        GLES20.glUniform1f(
            animTimeHandle,
            ((SystemClock.elapsedRealtime() - startTimeMs) % 3_600_000L) / 180f,
        )
        GLES20.glUniform1f(musicLevelHandle, musicLevel)
        GLES20.glUniform1f(beatHandle, beat)
        GLES20.glUniform1f(midFreqHandle, midFreq)
        GLES20.glUniform1f(highFreqHandle, highFreq)
        GLES20.glUniform1f(darkModeHandle, if (isDarkMode) 1f else 0f)
        GLES20.glUniform4f(colorHandle, colorR, colorG, colorB, colorA)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun createProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val message = GLES20.glGetProgramInfoLog(shaderProgram)
            GLES20.glDeleteProgram(shaderProgram)
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            throw IllegalStateException("Failed to link effect shader program: $message")
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return shaderProgram
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
            throw IllegalStateException("Failed to compile shader: $message")
        }

        return shader
    }

    private companion object {
        private const val STRIDE_BYTES = 4 * 4

        private val FULLSCREEN_VERTICES =
            floatArrayOf(
                -1f,
                -1f,
                0f,
                1f,
                1f,
                -1f,
                1f,
                1f,
                -1f,
                1f,
                0f,
                0f,
                1f,
                1f,
                1f,
                0f,
            )

        private const val VERTEX_SHADER_SOURCE =
            """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                vTexCoord = aTexCoord;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
            """

        private const val FRAGMENT_SHADER_SOURCE =
            """
            precision highp float;

            varying vec2 vTexCoord;

            uniform vec2 uResolution;
            uniform float uAnimTime;
            uniform float uMusicLevel;
            uniform float uBeat;
            uniform float uMidFreq;
            uniform float uHighFreq;
            uniform vec4 uColor;
            uniform float uIsDarkMode;

            vec3 permute(vec3 x) { return mod(((x * 34.0) + 1.0) * x, 289.0); }

            float snoise(vec2 v) {
                const vec4 C = vec4(
                    0.211324865405187,
                    0.366025403784439,
                    -0.577350269189626,
                    0.024390243902439
                );
                vec2 i = floor(v + dot(v, C.yy));
                vec2 x0 = v - i + dot(i, C.xx);
                vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
                vec4 x12 = x0.xyxy + C.xxzz;
                x12.xy -= i1;
                i = mod(i, 289.0);
                vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
                vec3 m = max(
                    0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)),
                    0.0
                );
                m = m * m;
                m = m * m;
                vec3 x = 2.0 * fract(p * C.www) - 1.0;
                vec3 h = abs(x) - 0.5;
                vec3 ox = floor(x + 0.5);
                vec3 a0 = x - ox;
                m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
                vec3 g;
                g.x = a0.x * x0.x + h.x * x0.y;
                g.yz = a0.yz * x12.xz + h.yz * x12.yw;
                return 130.0 * dot(m, g);
            }

            vec3 rgb2hsv(vec3 c) {
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
            }

            vec3 hsv2rgb(vec3 c) {
                vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
            }

            void main() {
                vec2 fragCoord = vTexCoord * uResolution.xy;
                vec2 uv = fragCoord / uResolution.xy;
                vec2 p = uv * 2.0 - 1.0;
                p.x *= uResolution.x / uResolution.y;

                float time = uAnimTime * 0.1;
                float dist = length(p);

                vec2 offset1 = vec2(time * 0.5 * (1.0 + uBeat * 0.3), time * 0.3);
                float n1 = snoise(p * (2.0 + uBeat * 0.3) + offset1);

                vec2 offset2 = vec2(-time * 0.3, time * 0.4 * (1.0 + uMidFreq * 0.2));
                float n2 = snoise(p * (3.0 + uMidFreq * 0.5) + offset2 + vec2(n1 * 0.5));

                vec2 offset3 = vec2(time * 0.4, -time * 0.35);
                float n3 = snoise(p * (4.0 + uHighFreq * 0.7) + offset3 + vec2(n2 * 0.3));

                float ripple =
                    n1 * (0.5 + uBeat * 0.12) +
                    n2 * (0.3 + uMidFreq * 0.08) +
                    n3 * (0.2 + uHighFreq * 0.05);

                float rippleNormalized = (ripple + 1.0) * 0.5;
                float rippleStrength = 0.15 + uMusicLevel * 0.3 + uBeat * 0.15 + uMidFreq * 0.08;

                vec3 hsv = rgb2hsv(uColor.rgb);
                float hueShift =
                    (uBeat * 0.06 + uMidFreq * 0.04 + uHighFreq * 0.03) *
                    sin(time * 0.5 + dist * 2.0);
                hsv.x = fract(hsv.x + hueShift);
                hsv.y = clamp(hsv.y + uMusicLevel * 0.12, 0.3, 1.0);
                vec3 dynamicColor = hsv2rgb(hsv);

                vec3 col;
                if (uIsDarkMode > 0.5) {
                    vec3 darkBg = vec3(0.08, 0.08, 0.08);
                    float radialGrad = 1.0 - smoothstep(0.0, 1.2, dist);
                    vec3 rippleColor = mix(dynamicColor, uColor.rgb, 0.5);
                    col = mix(darkBg, rippleColor, rippleNormalized * rippleStrength);
                    float centerGlow = radialGrad * radialGrad;
                    col += dynamicColor * centerGlow * (0.15 + uBeat * 0.25);
                    float sparkle = pow(max(0.0, n3), 3.0) * uHighFreq;
                    col += vec3(1.0, 0.9, 0.8) * sparkle * 0.18;
                } else {
                    vec3 lightBg = vec3(0.98, 0.98, 0.98);
                    vec3 softColor = mix(lightBg, dynamicColor, 0.6);
                    col = mix(lightBg, softColor, rippleNormalized * rippleStrength * 0.65);
                    float centerGlow = 1.0 - smoothstep(0.0, 1.0, dist);
                    col = mix(col, softColor, centerGlow * (0.1 + uBeat * 0.12));
                }

                gl_FragColor = vec4(col, 1.0);
            }
            """
    }
}
