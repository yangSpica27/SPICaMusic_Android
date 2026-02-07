package me.spica27.spicamusic.ui.widget

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import org.intellij.lang.annotations.Language
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val context = LocalContext.current

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
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun EffectShaderBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    fftDrawData: FloatArray = FloatArray(0),
    isDarkMode: Boolean? = false,
) {
    val context = LocalContext.current

    // 增强的流体效果着色器 - 支持频谱响应
    @Language("AGSL")
    val shaderSource =
        """
        uniform vec2 uResolution;
        uniform float uAnimTime;
        uniform float uMusicLevel;
        uniform float uBeat;
        uniform float uMidFreq;
        uniform float uHighFreq;
        uniform vec4 uColor;
        uniform float uIsDarkMode;
        
        // Simplex 2D noise
        vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }
        
        float snoise(vec2 v) {
            const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                               -0.577350269189626, 0.024390243902439);
            vec2 i  = floor(v + dot(v, C.yy));
            vec2 x0 = v -   i + dot(i, C.xx);
            vec2 i1;
            i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
            vec4 x12 = x0.xyxy + C.xxzz;
            x12.xy -= i1;
            i = mod(i, 289.0);
            vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
                + i.x + vec3(0.0, i1.x, 1.0));
            vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
                dot(x12.zw,x12.zw)), 0.0);
            m = m*m;
            m = m*m;
            vec3 x = 2.0 * fract(p * C.www) - 1.0;
            vec3 h = abs(x) - 0.5;
            vec3 ox = floor(x + 0.5);
            vec3 a0 = x - ox;
            m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
            vec3 g;
            g.x  = a0.x  * x0.x  + h.x  * x0.y;
            g.yz = a0.yz * x12.xz + h.yz * x12.yw;
            return 130.0 * dot(m, g);
        }
        
        // RGB转HSV
        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }
        
        // HSV转RGB
        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }
        
        vec4 main(vec2 fragCoord) {
            vec2 uv = fragCoord / uResolution.xy;
            vec2 p = uv * 2.0 - 1.0;
            p.x *= uResolution.x / uResolution.y;
            
            float time = uAnimTime * 0.1;
            float dist = length(p);
            
            // 多层噪声 - 响应不同频段
            // 低频影响大尺度运动
            vec2 offset1 = vec2(time * 0.5 * (1.0 + uBeat * 0.3), time * 0.3);
            float n1 = snoise(p * (2.0 + uBeat * 0.3) + offset1);
            
            // 中频影响中等尺度纹理
            vec2 offset2 = vec2(-time * 0.3, time * 0.4 * (1.0 + uMidFreq * 0.2));
            float n2 = snoise(p * (3.0 + uMidFreq * 0.5) + offset2 + vec2(n1 * 0.5));
            
            // 高频影响细节和闪烁
            vec2 offset3 = vec2(time * 0.4, -time * 0.35);
            float n3 = snoise(p * (4.0 + uHighFreq * 0.7) + offset3 + vec2(n2 * 0.3));
            
            // 组合噪声层 - 各频段贡献
            float ripple = n1 * (0.5 + uBeat * 0.12) + 
                          n2 * (0.3 + uMidFreq * 0.08) + 
                          n3 * (0.2 + uHighFreq * 0.05);
            
            // 归一化涟漪值到 0-1
            float rippleNormalized = (ripple + 1.0) * 0.5;
            
            // 动态涟漪强度 - 综合音频响应
            float rippleStrength = 0.15 + uMusicLevel * 0.3 + uBeat * 0.15 + uMidFreq * 0.08;
            
            // 根据音频动态调整色相
            vec3 hsv = rgb2hsv(uColor.rgb);
            float hueShift = (uBeat * 0.06 + uMidFreq * 0.04 + uHighFreq * 0.03) * 
                            sin(time * 0.5 + dist * 2.0);
            hsv.x = fract(hsv.x + hueShift);
            hsv.y = clamp(hsv.y + uMusicLevel * 0.12, 0.3, 1.0); // 增强饱和度
            vec3 dynamicColor = hsv2rgb(hsv);
            
            vec3 col;
            if (uIsDarkMode > 0.5) {
                // 暗色模式：深色背景 + 动态涟漪
                vec3 darkBg = vec3(0.08, 0.08, 0.08);
                
                // 径向渐变效果（节拍响应）
                float radialGrad = 1.0 - smoothstep(0.0, 1.2, dist);
                vec3 rippleColor = mix(dynamicColor, uColor.rgb, 0.5);
                
                // 混合背景和涟漪
                col = mix(darkBg, rippleColor, rippleNormalized * rippleStrength);
                
                // 中心高光（低频节拍）
                float centerGlow = radialGrad * radialGrad;
                col += dynamicColor * centerGlow * (0.15 + uBeat * 0.25);
                
                // 高频闪烁效果
                float sparkle = pow(max(0.0, n3), 3.0) * uHighFreq;
                col += vec3(1.0, 0.9, 0.8) * sparkle * 0.18;
                
            } else {
                // 亮色模式：浅色背景 + 柔和涟漪
                vec3 lightBg = vec3(0.98, 0.98, 0.98);
                
                // 柔化颜色
                vec3 softColor = mix(lightBg, dynamicColor, 0.6);
                
                // 混合背景和涟漪
                col = mix(lightBg, softColor, rippleNormalized * rippleStrength * 0.65);
                
                // 中心柔光（节拍响应）
                float centerGlow = 1.0 - smoothstep(0.0, 1.0, dist);
                col = mix(col, softColor, centerGlow * (0.1 + uBeat * 0.12));
            }
            
            return vec4(col, 1.0);
        }
        """.trimIndent()

    val infiniteTransition = rememberInfiniteTransition(label = "effect_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20_000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1_000 * 60 * 60, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time",
    )

    // 增强的音频驱动参数 - 分析不同频段
    val musicLevel =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 整体音量响应（使用RMS计算更准确）
                val rms = kotlin.math.sqrt(fftDrawData.map { it * it }.average()).toFloat()
                (rms * 1.2f).coerceIn(0f, 1f)
            }
        }

    val beat =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 低频（20-250Hz）- 节拍和低音
                val lowFreqCount = (fftDrawData.size * 0.2f).toInt().coerceAtLeast(1)
                val lowFreq = fftDrawData.take(lowFreqCount)
                (lowFreq.average().toFloat() * 0.1f).coerceIn(0f, 1f)
            }
        }

    val midFreq =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 中频（250Hz-4kHz）- 人声和主要乐器
                val startIdx = (fftDrawData.size * 0.2f).toInt()
                val endIdx = (fftDrawData.size * 0.6f).toInt().coerceAtMost(fftDrawData.size)
                if (startIdx >= endIdx) {
                    0f
                } else {
                    val midFreqBand = fftDrawData.slice(startIdx until endIdx)
                    (midFreqBand.average().toFloat() * 1.7f).coerceIn(0f, 1f)
                }
            }
        }

    val highFreq =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 高频（4kHz+）- 镲片、气息音等细节
                val startIdx = (fftDrawData.size * 0.6f).toInt()
                if (startIdx >= fftDrawData.size) {
                    0f
                } else {
                    val highFreqBand = fftDrawData.slice(startIdx until fftDrawData.size)
                    (highFreqBand.average().toFloat() * 1.2f).coerceIn(0f, 1f)
                }
            }
        }

    val shader = remember { RuntimeShader(shaderSource) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    // 自动判断暗色模式
    val effectiveIsDarkMode = isDarkMode ?: (MiuixTheme.colorScheme.background.luminance() < 0.5f)

    Box(
        modifier =
            modifier
                .drawWithCache {
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uAnimTime", time)
                    shader.setFloatUniform("uMusicLevel", musicLevel)
                    shader.setFloatUniform("uBeat", beat)
                    shader.setFloatUniform("uMidFreq", midFreq)
                    shader.setFloatUniform("uHighFreq", highFreq)
                    shader.setFloatUniform("uIsDarkMode", if (effectiveIsDarkMode) 1f else 0f)
                    // 设置颜色为 vec4 (RGBA)
                    shader.setFloatUniform(
                        "uColor",
                        coverColor.red,
                        coverColor.green,
                        coverColor.blue,
                        coverColor.alpha,
                    )

                    onDrawBehind {
                        drawRect(shaderBrush)
                    }
                },
    )
}
