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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.intellij.lang.annotations.Language

/**
 * 音频城市可视化器
 *
 * 俯瞰视角的 3D 柱体网格城市，摄像机绕 Y 轴旋转。
 * 每根柱体高度由对应频段 FFT 数据驱动（近处低频 → 远处高频），
 * 音量驱动近处柱体膨胀，颜色从粉红/绿 → 紫色渐变，
 * 并融入封面主色调。
 *
 * 灵感来源: @kishimisu - 2023
 *
 * @param modifier 修饰符
 * @param fftBands FFT 频段数据 (31个频段, 0.0-1.0)
 * @param baseColor 基础色调（来自封面主色）
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AudioCityVisualizer(
    modifier: Modifier = Modifier,
    fftBands: FloatArray = FloatArray(31),
    baseColor: Color = Color(0xFF8844FF),
) {
    @Language("AGSL")
    val shaderSource =
        """
        uniform float u_time;
        uniform float2 u_resolution;
        uniform float3 u_baseColor;
        uniform float u_volume;
        uniform float4 u_bands0;
        uniform float4 u_bands1;
        uniform float4 u_bands2;
        uniform float4 u_bands3;

        // ---------- 频段采样 (无分支) ----------

        float sampleBands(float fi) {
            float s = clamp(fi, 0.0, 15.0);
            float g = floor(s / 4.0);
            float l = s - g * 4.0;
            float4 v = mix(mix(u_bands0, u_bands1, step(1.0, g)),
                           mix(u_bands2, u_bands3, step(3.0, g)),
                           step(2.0, g));
            return mix(mix(v.x, v.y, step(1.0, l)),
                       mix(v.z, v.w, step(3.0, l)),
                       step(2.0, l));
        }

        float getBand(float freq) {
            float idx = clamp(freq, 0.0, 1.0) * 15.0;
            float fi = floor(idx);
            float fr = idx - fi;
            return mix(sampleBands(fi), sampleBands(min(fi + 1.0, 15.0)), fr);
        }

        // ---------- 音频函数 ----------

        // 直接使用 FFT 数据，sqrt 拉伸低值区间的动态范围
        float getPitch(float freq) {
            float norm = clamp(freq / 5.0, 0.0, 1.0);
            float raw = getBand(norm);
            return raw * 0.8;
        }

        // ---------- 工具函数 ----------

        float sdBox(float3 p, float3 b) {
            float3 q = abs(p) - b;
            return length(max(q, float3(0.0))) + min(max(q.x, max(q.y, q.z)), 0.0);
        }

        float hash13(float3 p3) {
            p3 = fract(p3 * 0.1031);
            p3 += dot(p3, p3.zyx + 31.32);
            return fract((p3.x + p3.y) * p3.z);
        }

        float light(float d, float att) {
            return 1.0 / (1.0 + pow(abs(d * att), 1.5));
        }

        // 2D 旋转 (替代 GLSL mat2)
        float2 rot2d(float2 v, float a) {
            float ca = cos(a);
            float sa = sin(a);
            return float2(v.x * ca - v.y * sa, v.x * sa + v.y * ca);
        }

        // ---------- 主渲染 ----------

        half4 main(float2 fragCoord) {
            float2 uv = (2.0 * fragCoord - u_resolution) / u_resolution.y;
            uv.y = -uv.y;  // AGSL Y轴向下，Shadertoy Y轴向上，需翻转

            // 背景
            float3 col = float3(0.1, 0.0, 0.14);

            float vol = u_volume;

            // 相机位置：适中俯角俯瞰城市
            float3 ro = float3(0.0, 4.0, 10.0) * (1.0 + vol * 0.3);
            // Y 轴旋转
            float2 rotXZ = rot2d(float2(ro.x, ro.z), u_time * 0.15);
            ro = float3(rotXZ.x, ro.y, rotXZ.y);

            // 相机坐标系
            float3 f = normalize(-ro);
            float3 worldUp = float3(0.0, 1.0, 0.0);
            float3 r = normalize(cross(worldUp, f));
            float3 u = cross(f, r);
            float3 rd = normalize(f + uv.x * r + uv.y * u);

            // 三色：粉红 → 绿 → 紫蓝
            float3 warmCol = float3(0.8, 0.2, 0.4);
            float3 greenCol = float3(0.0, 1.0, 0.0);
            float3 coolCol = float3(0.5, 0.3, 1.2);

            float t = 0.0;

            // 25 步 raymarching（原版 30，移动端优化）
            for (float i = 0.0; i < 25.0; i += 1.0) {
                float3 p = ro + t * rd;

                // xz 平面网格划分
                float2 cen = floor(p.xz) + 0.5;
                float3 id = abs(float3(cen.x, 0.0, cen.y));
                float d = length(id);

                // 频率映射：近处低频，远处高频 + hash 随机偏移
                float freq = smoothstep(0.0, 20.0, d) * 3.0 + hash13(id) * 2.0;
                float pitch = getPitch(freq);

                // 音量对近处柱体的膨胀效果
                float v = vol * smoothstep(2.0, 0.0, d);
                // 柱体高度：适中律动，不超过相机
                float h = d * 0.15 + pitch * 1.5 + v * 1.0 + 0.3;

                // 柱体 SDF（宽度0.25留出间距减少边界伪影）
                float me = sdBox(
                    p - float3(cen.x, -50.0, cen.y),
                    float3(0.25, 50.0 + h, 0.25)
                ) - 0.03;

                // 颜色公式 — 完全一致
                float3 baseCol = mix(
                    mix(warmCol, greenCol, min(v * 2.0, 1.0)),
                    coolCol,
                    smoothstep(10.0, 30.0, d)
                );

                col += baseCol
                     * (cos(id) + 1.5)
                     * (pitch * d * 0.04 + v * 0.4 + 0.12)
                     * light(me, 20.0)
                     * (1.0 + vol * 0.8)
                     * 0.5;

                t += max(me, 0.02);
                if (t > 40.0) { break; }
            }

            // 限制输出范围，保持饱和度
//            col = clamp(col, float3(0.0), float3(1.0));

            return half4(half3(col), 1.0);
        }
        """.trimIndent()

    // 时间动画
    val infiniteTransition = rememberInfiniteTransition(label = "audio_city_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1_000_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time",
    )

    val shader = remember { RuntimeShader(shaderSource) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawWithCache {
                    val src = if (fftBands.size >= 31) fftBands else FloatArray(31)
                    val volume = if (src.isEmpty()) 0f else src.average().toFloat().coerceIn(0f, 1f)
                    val b = FloatArray(16) { i -> src[(i * 30f / 15f).toInt().coerceAtMost(30)] }

                    shader.setFloatUniform("u_time", time)
                    shader.setFloatUniform("u_resolution", size.width, size.height)
                    shader.setFloatUniform(
                        "u_baseColor",
                        baseColor.red,
                        baseColor.green,
                        baseColor.blue,
                    )
                    shader.setFloatUniform("u_volume", volume)

                    shader.setFloatUniform("u_bands0", b[0], b[1], b[2], b[3])
                    shader.setFloatUniform("u_bands1", b[4], b[5], b[6], b[7])
                    shader.setFloatUniform("u_bands2", b[8], b[9], b[10], b[11])
                    shader.setFloatUniform("u_bands3", b[12], b[13], b[14], b[15])

                    onDrawBehind {
                        drawRect(shaderBrush)
                    }
                },
    )
}
