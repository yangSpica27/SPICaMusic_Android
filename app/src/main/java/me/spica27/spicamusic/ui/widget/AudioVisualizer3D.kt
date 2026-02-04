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
 * 3D 音频可视化器
 * 改编自 Shadertoy "3D Audio Visualizer" by @kishimisu
 *
 * 使用 AGSL (Android Graphics Shading Language) 实现
 * 灯光效果会实时响应音频的 FFT 频谱数据
 *
 * @param modifier 修饰符
 * @param fftBands FFT 频段数据 (31个频段，0.0-1.0范围)
 * @param baseColor 基础色调
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AudioVisualizer3D(
    modifier: Modifier = Modifier,
    fftBands: FloatArray = FloatArray(31),
    baseColor: Color = Color(0xFF789FFF),
) {
    @Language("AGSL")
    val shaderSource =
        """
        uniform float u_time;
        uniform vec2 u_resolution;
        uniform vec3 u_baseColor;
        uniform float u_volume;
        
        // 使用 8 个 vec4 存储 31 个频段数据 (7*4 + 3 = 31)
        uniform vec4 u_bands0;  // 频段 0-3
        uniform vec4 u_bands1;  // 频段 4-7
        uniform vec4 u_bands2;  // 频段 8-11
        uniform vec4 u_bands3;  // 频段 12-15
        uniform vec4 u_bands4;  // 频段 16-19
        uniform vec4 u_bands5;  // 频段 20-23
        uniform vec4 u_bands6;  // 频段 24-27
        uniform vec4 u_bands7;  // 频段 28-30 (xyz)
        
        // 辅助函数：光照计算
        float light(float d, float att) {
            return 1.0 / (1.0 + pow(abs(d * att), 1.3));
        }
        
        // 辅助函数：音频响应（logistic 曲线）
        float logisticAmp(float amp) {
            float c = 0.9;
            float a = 20.0;
            float expVal1 = exp(-a * (amp - c));
            float expVal2 = exp(-a * (0.0 - c));
            float expVal3 = exp(-a * (1.0 - c));
            
            float logX1 = 1.0 / (expVal1 + 1.0);
            float logX0 = 1.0 / (expVal2 + 1.0);
            float logX_max = 1.0 / (expVal3 + 1.0);
            
            return (logX1 - logX0) / (logX_max - logX0);
        }
        
        // 获取指定频率的响度（使用 vec4 访问频段数据）
        float getPitch(float freq) {
            // 归一化频率到 0-30 范围
            float normalized = clamp(freq, 0.0, 1.0);
            float index = normalized * 30.0;
            
            // 根据索引从对应的 vec4 中提取值
            float bandValue = u_bands0.x;
            
            if (index >= 1.0) bandValue = u_bands0.y;
            if (index >= 2.0) bandValue = u_bands0.z;
            if (index >= 3.0) bandValue = u_bands0.w;
            
            if (index >= 4.0) bandValue = u_bands1.x;
            if (index >= 5.0) bandValue = u_bands1.y;
            if (index >= 6.0) bandValue = u_bands1.z;
            if (index >= 7.0) bandValue = u_bands1.w;
            
            if (index >= 8.0) bandValue = u_bands2.x;
            if (index >= 9.0) bandValue = u_bands2.y;
            if (index >= 10.0) bandValue = u_bands2.z;
            if (index >= 11.0) bandValue = u_bands2.w;
            
            if (index >= 12.0) bandValue = u_bands3.x;
            if (index >= 13.0) bandValue = u_bands3.y;
            if (index >= 14.0) bandValue = u_bands3.z;
            if (index >= 15.0) bandValue = u_bands3.w;
            
            if (index >= 16.0) bandValue = u_bands4.x;
            if (index >= 17.0) bandValue = u_bands4.y;
            if (index >= 18.0) bandValue = u_bands4.z;
            if (index >= 19.0) bandValue = u_bands4.w;
            
            if (index >= 20.0) bandValue = u_bands5.x;
            if (index >= 21.0) bandValue = u_bands5.y;
            if (index >= 22.0) bandValue = u_bands5.z;
            if (index >= 23.0) bandValue = u_bands5.w;
            
            if (index >= 24.0) bandValue = u_bands6.x;
            if (index >= 25.0) bandValue = u_bands6.y;
            if (index >= 26.0) bandValue = u_bands6.z;
            if (index >= 27.0) bandValue = u_bands6.w;
            
            if (index >= 28.0) bandValue = u_bands7.x;
            if (index >= 29.0) bandValue = u_bands7.y;
            if (index >= 30.0) bandValue = u_bands7.z;
            
            return logisticAmp(bandValue);
        }
        
        // 3D 盒子 SDF (Signed Distance Field)
        float sdBox(vec3 p, vec3 b) {
            vec3 q = abs(p) - b;
            return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
        }
        
        // 哈希函数（用于生成伪随机数）
        float hash13(vec3 p3) {
            p3 = fract(p3 * 0.1031);
            p3 += dot(p3, p3.zyx + 31.32);
            return fract((p3.x + p3.y) * p3.z);
        }
        
        vec4 main(vec2 fragCoord) {
            // 归一化坐标，中心为原点
            vec2 uv = (2.0 * fragCoord - u_resolution) / u_resolution.y;
            vec3 col = vec3(0.0);
            
            // Raymarching 主循环
            float t = 0.5; // 从稍微远一点开始，避免相机在盒子内部
            for (float i = 0.0; i < 30.0; i++) {
                // 射线方向
                vec3 p = t * normalize(vec3(uv, 1.0));
                
                // 重复空间划分
                vec3 id = floor(abs(p));
                vec3 q = fract(p) - 0.5;
                
                // 重复的盒子
                float boxRep = sdBox(q, vec3(0.3));
                // 容器盒子（限制范围）
                float boxCtn = sdBox(p, vec3(7.5, 6.5, 16.5));
                
                // 距离场（带音频体积调制）
                float dst = max(boxRep, abs(boxCtn) - u_volume * 0.2);
                
                // 频率映射：远处的灯光对应低频，近处对应高频
                float freq = smoothstep(16.0, 0.0, id.z) * 3.0 + hash13(id) * 1.5;
                float freqIndex = clamp(freq / 4.5, 0.0, 1.0); // 归一化到 0-1
                
                // 颜色动画（基于位置和时间）
                vec3 colorMod = cos(id * 0.4 + vec3(0, 1, 2) + u_time) + 2.0;
                vec3 lightColor = u_baseColor * colorMod;
                
                // 应用光照和音频响应
                // 添加基础亮度 0.3，确保即使没有音频时也能看到效果
                float audioResponse = getPitch(freqIndex);
                float brightness = 0.3 + audioResponse * 0.7;
                
                col += lightColor 
                     * light(dst, 10.0 - u_volume)
                     * brightness;
                
                // 步进，至少前进一小段距离避免卡住
                t += max(dst, 0.01);
                
                // 如果走得太远就停止
                if (t > 30.0) break;
            }
            
            return vec4(col, 1.0);
        }
        """.trimIndent()

    // 时间动画
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_time")
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

    // 计算整体音量（平均值）
    val volume =
        remember(fftBands) {
            if (fftBands.isEmpty()) 0f else fftBands.average().toFloat().coerceIn(0f, 1f)
        }

    val shader = remember { RuntimeShader(shaderSource) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawWithCache {
                    // 更新 uniform 变量
                    shader.setFloatUniform("u_time", time)
                    shader.setFloatUniform("u_resolution", size.width, size.height)
                    shader.setFloatUniform(
                        "u_baseColor",
                        baseColor.red,
                        baseColor.green,
                        baseColor.blue,
                    )
                    shader.setFloatUniform("u_volume", volume)

                    // 将 31 个频段数据打包成 8 个 vec4
                    val bands = if (fftBands.size >= 31) fftBands else FloatArray(31)

                    shader.setFloatUniform("u_bands0", bands[0], bands[1], bands[2], bands[3])
                    shader.setFloatUniform("u_bands1", bands[4], bands[5], bands[6], bands[7])
                    shader.setFloatUniform("u_bands2", bands[8], bands[9], bands[10], bands[11])
                    shader.setFloatUniform("u_bands3", bands[12], bands[13], bands[14], bands[15])
                    shader.setFloatUniform("u_bands4", bands[16], bands[17], bands[18], bands[19])
                    shader.setFloatUniform("u_bands5", bands[20], bands[21], bands[22], bands[23])
                    shader.setFloatUniform("u_bands6", bands[24], bands[25], bands[26], bands[27])
                    shader.setFloatUniform("u_bands7", bands[28], bands[29], bands[30], 0f)

                    onDrawBehind {
                        drawRect(shaderBrush)
                    }
                },
    )
}
