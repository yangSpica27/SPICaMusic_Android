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
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import org.intellij.lang.annotations.Language
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max

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

    // 简化的流体效果着色器
    @Language("AGSL")
    val shaderSource =
        """
        uniform vec2 uResolution;
        uniform float uAnimTime;
        uniform float uMusicLevel;
        uniform float uBeat;
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
        
        vec4 main(vec2 fragCoord) {
            vec2 uv = fragCoord / uResolution.xy;
            vec2 p = uv * 2.0 - 1.0;
            p.x *= uResolution.x / uResolution.y;
            
            float time = uAnimTime * 0.1;
            
            // 多层噪声 - 创建涟漪效果
            float n1 = snoise(p * 2.0 + time * 0.5);
            float n2 = snoise(p * 3.0 - time * 0.3 + vec2(n1));
            float n3 = snoise(p * 4.0 + time * 0.4 + vec2(n2));
            
            // 音乐响应 - 涟漪强度（增强节拍影响）
            float pulse = 1.0 + uBeat * 2.0;  // 从0.5增加到2.0
            float ripple = (n1 * 0.5 + n2 * 0.3 + n3 * 0.2) * pulse;
            
            // 归一化涟漪值到 0-1
            float rippleNormalized = (ripple + 1.0) * 0.5;
            
            // 根据音乐动态调整涟漪强度（增强变化范围）
            float rippleStrength = 0.2 + uMusicLevel * 0.7 + uBeat * 0.3;  // 从0.3-0.7增加到0.2-1.2
            
            vec3 col;
            if (uIsDarkMode > 0.5) {
                // 暗色模式：深色背景 + 封面色涟漪
                vec3 darkBg = vec3(0.141, 0.141, 0.141); // #FF242424
                // 涟漪使用固定封面色
                vec3 rippleColor = uColor.rgb;
                // 混合背景和涟漪（音乐响应体现在强度上）
                col = mix(darkBg, rippleColor, rippleNormalized * rippleStrength);
                
                // 中心高光效果（音乐响应）
                float centerGlow = 1.0 - smoothstep(0.0, 0.8, length(p));
                col += uColor.rgb * centerGlow * (0.2 * uMusicLevel + 0.3 * uBeat);
            } else {
                // 亮色模式：浅色背景 + 封面色涟漪
                vec3 lightBg = vec3(1.0, 1.0, 1.0); // #FFFFFFFF
                // 涟漪使用固定封面色
                vec3 rippleColor = uColor.rgb;
                // 混合背景和涟漪（音乐响应体现在强度上）
                col = mix(lightBg, rippleColor, rippleNormalized * rippleStrength * 0.7);
            }
            
            return vec4(col, 1.0);
        }
        """.trimIndent()

    val infiniteTransition = rememberInfiniteTransition(label = "effect_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1_000 * 60 * 60, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time",
    )

    // 计算音频驱动参数（放大响应度）
    val musicLevel =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 放大平均值并限制范围
                (fftDrawData.average().toFloat() * 2.5f).coerceIn(0f, 1f)
            }
        }

    val beat =
        remember(fftDrawData) {
            if (fftDrawData.isEmpty()) {
                0f
            } else {
                // 使用低频部分计算节拍，放大响应
                val lowFreq = fftDrawData.take(fftDrawData.size / 4)
                (max(0f, lowFreq.maxOrNull() ?: 0f) * 1.5f).coerceIn(0f, 1f)
            }
        }

    val shader = remember { RuntimeShader(shaderSource) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    // 自动判断暗色模式
    val effectiveIsDarkMode = isDarkMode ?: (MiuixTheme.colorScheme.background.luminance() < 0.5f)

    Box(
        modifier =
            modifier
                .hazeEffect {
                    blurRadius = 12.dp
                }.drawWithCache {
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uAnimTime", time)
                    shader.setFloatUniform("uMusicLevel", musicLevel)
                    shader.setFloatUniform("uBeat", beat)
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
