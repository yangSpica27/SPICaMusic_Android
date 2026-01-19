package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 流动融合动态背景组件
 * 响应音乐FFT数据和封面颜色
 *
 * @param modifier 修饰符
 * @param coverColor 封面主色，用于色彩调整
 * @param enabled 是否启用动画
 * @param isDarkMode 暗色模式（true）或亮色模式（false），null时自动判断
 */
@Composable
fun FluidMusicBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    enabled: Boolean = true,
    isDarkMode: Boolean? = null,
) {
    val playerViewModel = LocalPlayerViewModel.current
    val settingsViewModel: SettingsViewModel = koinViewModel()

    val scope = rememberCoroutineScope()

    LaunchedEffect(enabled) {
        if (enabled) {
            playerViewModel.subscribeFFTDrawData()
        } else {
            playerViewModel.unsubscribeFFTDrawData()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 组件销毁时取消订阅
            scope.launch(Dispatchers.Default) {
                playerViewModel.unsubscribeFFTDrawData()
            }
        }
    }

    val fftDrawData by playerViewModel.fftDrawData.collectAsStateWithLifecycle()
    val modeValue by settingsViewModel.dynamicSpectrumBackground.collectAsStateWithLifecycle()
    val backgroundMode = remember(modeValue) { DynamicSpectrumBackground.fromString(modeValue) }
    val fftSnapshot = if (enabled) fftDrawData else FloatArray(fftDrawData.size)

    when (backgroundMode) {
        DynamicSpectrumBackground.TopGlow ->
            TopGlowBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
            )

        DynamicSpectrumBackground.LiquidAurora ->
            LiquidAuroraBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.BubblePulse ->
            BubblePulseBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
            )

        DynamicSpectrumBackground.NeonGrid ->
            NeonGridBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.ParticleStarfield ->
            ParticleStarfieldBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.RippleWave ->
            RippleWaveBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
            )

        DynamicSpectrumBackground.SpectrumHelix ->
            SpectrumHelixBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.EnergyPulse ->
            EnergyPulseBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
            )

        DynamicSpectrumBackground.FluidVortex ->
            FluidVortexBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.OFF ->
            Box(modifier = modifier)
    }
}

@Composable
private fun TopGlowBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 72.dp
            },
    ) {
        val bandWidth = if (fftDrawData.isNotEmpty()) size.width / fftDrawData.size else size.width
        val luminance = calculateLuminance(coverColor)
        val hueShift = if (luminance < 0.5f) 24f else -24f
        fftDrawData.forEachIndexed { index, magnitude ->
            val energy = magnitude.coerceIn(0f, 1f)
            val barHeight = size.height * 0.8f * energy + size.height * 0.08f
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                shiftHue(coverColor, hueShift).copy(alpha = 0.85f),
                                shiftHue(coverColor, hueShift * 1.6f).copy(alpha = 0.2f),
                            ),
                    ),
                topLeft = Offset(x = index * bandWidth, y = 0f),
                size = Size(width = max(1f, bandWidth * 0.9f), height = barHeight),
            )
        }
    }
}

@Composable
private fun LiquidAuroraBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora-phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "aurora-anim",
    )

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 40.dp
            },
    ) {
        val layers = 3
        val chunkSize = (fftDrawData.size / layers).coerceAtLeast(1)
        val baseAlpha = if (isDarkMode == true) 0.9f else 0.75f
        repeat(layers) { layer ->
            val startIndex = layer * chunkSize
            val endIndex = min(fftDrawData.size, startIndex + chunkSize)
            if (startIndex >= endIndex) return@repeat
            val path = Path().apply { moveTo(0f, 0f) }
            val steps = endIndex - startIndex
            val amplitude = size.height * (0.28f - layer * 0.05f)
            val phaseShift = (phase + layer * 45f) * (PI / 180f)
            for (i in 0 until steps) {
                val progress = if (steps == 1) 0f else i / (steps - 1f)
                val energy = fftDrawData[startIndex + i].coerceIn(0f, 1f)
                val wave = sin(progress * 6f + phaseShift).toFloat()
                val y = size.height * 0.35f - amplitude * energy - amplitude * 0.2f * wave
                val x = progress * size.width
                if (i == 0) {
                    path.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.lineTo(size.width, 0f)
            path.close()

            val colorA = shiftHue(coverColor, layer * 18f + 120f)
            val colorB = shiftHue(coverColor, layer * -14f - 116f)
            drawPath(
                path = path,
                brush =
                    Brush.verticalGradient(
                        startY = 0f,
                        endY = size.height * 0.5f,
                        colors =
                            listOf(
                                colorA.copy(alpha = baseAlpha - layer * 0.2f),
                                colorB.copy(alpha = (baseAlpha - layer * 0.3f).coerceAtLeast(0.1f)),
                                colorB.copy(alpha = (baseAlpha - layer * 0.3f).coerceAtLeast(0.1f)),
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun BubblePulseBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    val bubbles =
        remember {
            List(14) {
                BubbleSeed(
                    xFactor = Random.nextFloat(),
                    yFactor = Random.nextFloat(),
                    baseSize = 0.08f + Random.nextFloat() * 0.12f,
                )
            }
        }

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 45.dp
            },
    ) {
        val minDimension = min(size.width, size.height)
        bubbles.forEachIndexed { index, bubble ->
            val fftIndex = (index * fftDrawData.size) / bubbles.size
            val energy = fftDrawData.getOrNull(fftIndex)?.coerceIn(0f, 1f) ?: 0f
            val radius = minDimension * bubble.baseSize * (0.8f + energy * 1.5f)
            val center = Offset(size.width * bubble.xFactor, size.height * bubble.yFactor)
            val coreColor = shiftHue(coverColor, (index - bubbles.size / 2) * 3f)
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                coreColor.copy(alpha = 0.5f + energy * 0.4f),
                                coreColor.copy(alpha = 0f),
                            ),
                    ),
                center = center,
                radius = radius,
            )
        }
    }
}

@Composable
private fun NeonGridBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 48.dp
            },
    ) {
        val columns = 12
        val rows = 5
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        val baseColor = if (isDarkMode == true) Color(0xFF050505) else Color(0xFFF8F8FF)
        drawRect(color = baseColor.copy(alpha = if (isDarkMode == true) 0.9f else 0.65f), size = size)

        for (row in 0..rows) {
            val y = row * cellHeight
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }

        for (column in 0..columns) {
            val x = column * cellWidth
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }

        val neonBase = shiftHue(coverColor, if (isDarkMode == true) 8f else -8f)
        val barWidth = cellWidth * 0.45f
        for (column in 0 until columns) {
            val fftIndex = column % max(1, fftDrawData.size)
            val energy = fftDrawData.getOrNull(fftIndex)?.coerceIn(0f, 1f) ?: 0f
            val barHeight = size.height * (0.15f + energy * 0.85f)
            val top = size.height - barHeight
            drawRoundRect(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                neonBase.copy(alpha = 0.9f),
                                shiftHue(neonBase, 24f).copy(alpha = 0.2f),
                            ),
                    ),
                topLeft = Offset(column * cellWidth + (cellWidth - barWidth) / 2f, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = 0.5f + energy * 0.5f,
            )
        }
    }
}

private data class BubbleSeed(
    val xFactor: Float,
    val yFactor: Float,
    val baseSize: Float,
)

private data class ParticleSeed(
    val xFactor: Float,
    val yFactor: Float,
    val speedFactor: Float,
    val sizeFactor: Float,
    val hueOffset: Float,
)

/**
 * 粒子星空背景
 * 根据音乐律动的粒子系统，营造星空般的效果
 */
@Composable
private fun ParticleStarfieldBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val particles =
        remember {
            List(120) {
                ParticleSeed(
                    xFactor = Random.nextFloat(),
                    yFactor = Random.nextFloat(),
                    speedFactor = 0.3f + Random.nextFloat() * 0.7f,
                    sizeFactor = 0.5f + Random.nextFloat() * 1.5f,
                    hueOffset = -30f + Random.nextFloat() * 60f,
                )
            }
        }

    val infiniteTransition = rememberInfiniteTransition(label = "particle-time")
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 60000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time-anim",
    )

    Canvas(
        modifier =
        modifier,
    ) {
        val baseColor = if (isDarkMode == true) Color(0xFF000510) else Color(0xFFF0F0FA)
        drawRect(color = baseColor.copy(alpha = 0.85f), size = size)

        val avgEnergy =
            if (fftDrawData.isNotEmpty()) {
                fftDrawData.average().toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

        particles.forEachIndexed { index, particle ->
            val fftIndex = index % max(1, fftDrawData.size)
            val energy = fftDrawData.getOrNull(fftIndex)?.coerceIn(0f, 1f) ?: 0f

            // 粒子位置随时间移动
            val progress = (timePhase * particle.speedFactor * 0.001f) % 1f
            val x = size.width * particle.xFactor
            val y = (size.height * particle.yFactor + size.height * progress) % size.height

            // 粒子大小受音乐能量影响
            val baseRadius = 1.5f + particle.sizeFactor * 2f
            val radius = baseRadius * (1f + energy * 2f + avgEnergy * 0.5f)

            // 粒子颜色
            val particleColor = shiftHue(coverColor, particle.hueOffset)
            val alpha = (0.3f + energy * 0.7f) * (1f - abs(particle.yFactor - 0.5f) * 0.6f)

            // 绘制粒子光晕
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                particleColor.copy(alpha = alpha),
                                particleColor.copy(alpha = alpha * 0.3f),
                                Color.Transparent,
                            ),
                    ),
                center = Offset(x, y),
                radius = radius * 3f,
            )

            // 绘制粒子核心
            drawCircle(
                color = particleColor.copy(alpha = alpha * 1.2f),
                center = Offset(x, y),
                radius = radius * 0.8f,
            )
        }
    }
}

/**
 * 波纹涟漪背景
 * 从多个中心点向外扩散的同心圆涟漪效果
 */
@Composable
private fun RippleWaveBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    val rippleCenters =
        remember {
            listOf(
                Offset(0.3f, 0.4f),
                Offset(0.7f, 0.3f),
                Offset(0.5f, 0.7f),
            )
        }

    val infiniteTransition = rememberInfiniteTransition(label = "ripple-phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ripple-anim",
    )

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 8.dp
            },
    ) {
        val avgEnergy =
            if (fftDrawData.isNotEmpty()) {
                fftDrawData.average().toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

        val maxRadius = sqrt(size.width * size.width + size.height * size.height)

        rippleCenters.forEachIndexed { centerIndex, centerOffset ->
            val center =
                Offset(
                    size.width * centerOffset.x,
                    size.height * centerOffset.y,
                )

            val basePhase = phase + centerIndex * 120f

            // 每个中心点绘制多圈涟漪
            repeat(5) { ringIndex ->

                val fftIndex = (centerIndex * 5 + ringIndex) % max(1, fftDrawData.size)
                val energy = fftDrawData.getOrNull(fftIndex)?.coerceIn(0f, 1f) ?: 0f

                val progress = ((basePhase + ringIndex * 72f) % 360f) / 360f

                val radius = maxRadius * energy

                val alpha = .1f * (1f - progress) * (0.1f + energy * 0.5f)

                val ringColor = shiftHue(coverColor, centerIndex * 20f + 100f * energy)

                drawCircle(
                    color = ringColor.copy(alpha = alpha),
                    center = center,
                    radius = radius,
                    style =
                        androidx.compose.ui.graphics.drawscope
                            .Fill,
                )
            }
        }
    }
}

/**
 * 光谱螺旋背景
 * 螺旋状的频谱可视化，形成动态旋转效果
 */
@Composable
private fun SpectrumHelixBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "helix-rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 25000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation-anim",
    )

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 14.dp
            },
    ) {
        val baseColor = if (isDarkMode == true) Color(0xFF0A0A0F) else Color(0xFFF5F5F8)
        drawRect(color = baseColor.copy(alpha = 0.7f), size = size)

        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = min(size.width, size.height) * 0.45f

        val barCount = max(1, fftDrawData.size)
        val angleStep = 360f / barCount

        fftDrawData.forEachIndexed { index, magnitude ->
            val energy = magnitude.coerceIn(0f, 1f)
            val angle = (rotation + index * angleStep) * (PI / 180f)

            // 螺旋半径随索引增加
            val spiralFactor = index.toFloat() / barCount
            val radius = maxRadius * (0.3f + spiralFactor * 0.7f)

            // 计算位置
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius

            // 条形长度
            val barLength = 20f + energy * 80f
            val barWidth = 3f + energy * 5f

            // 颜色渐变
            val barColor = shiftHue(coverColor, index * 2f - barCount.toFloat())

            // 绘制发光条
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                barColor.copy(alpha = 0.9f),
                                barColor.copy(alpha = 0.3f),
                                Color.Transparent,
                            ),
                    ),
                center = Offset(x, y),
                radius = barLength,
            )

            // 绘制核心亮点
            drawCircle(
                color = barColor.copy(alpha = 1f),
                center = Offset(x, y),
                radius = barWidth,
            )
        }
    }
}

/**
 * 能量脉冲背景
 * 强烈的脉冲式能量从中心向外传播
 */
@Composable
private fun EnergyPulseBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse-wave")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulse-anim",
    )

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 14.dp
            },
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 3
        val maxRadius = sqrt(size.width * size.width + size.height * size.height) * 0.7f

        val avgEnergy =
            if (fftDrawData.isNotEmpty()) {
                fftDrawData.average().toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

        // 主脉冲波
        repeat(3) { waveIndex ->
            val wavePhase = (pulsePhase + waveIndex * 0.33f) % 1f
            val radius = maxRadius * wavePhase
            val alpha = (1f - wavePhase) * (0.5f + avgEnergy * 0.5f)

            val waveColor = shiftHue(coverColor, waveIndex * 30f)

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                waveColor.copy(alpha = alpha * 0.8f),
                                waveColor.copy(alpha = alpha * 0.4f),
                                Color.Transparent,
                            ),
                        center = Offset(centerX, centerY),
                        radius = radius,
                    ),
                center = Offset(centerX, centerY),
                radius = radius,
            )
        }

        // 方向性能量束
        val beamCount = 12
        fftDrawData.take(beamCount).forEachIndexed { index, magnitude ->
            val energy = magnitude.coerceIn(0f, 1f)
            val angle = (index.toFloat() / beamCount * 360f + pulsePhase * 60f) * (PI / 180f)

            val beamLength = maxRadius * (0.5f + energy * 0.5f)
            val startRadius = maxRadius * 0.15f

            val startX = centerX + cos(angle).toFloat() * startRadius
            val startY = centerY + sin(angle).toFloat() * startRadius
            val endX = centerX + cos(angle).toFloat() * (startRadius + beamLength)
            val endY = centerY + sin(angle).toFloat() * (startRadius + beamLength)

            val beamColor = shiftHue(coverColor, index * 15f)

            drawLine(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                beamColor.copy(alpha = 0.8f * energy),
                                beamColor.copy(alpha = 0.3f * energy),
                                Color.Transparent,
                            ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                    ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f + energy * 6f,
                cap = StrokeCap.Round,
            )
        }

        // 中心发光核心
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            coverColor.copy(alpha = 0.9f),
                            coverColor.copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                ),
            center = Offset(centerX, centerY),
            radius = 30f + avgEnergy * 50f,
        )
    }
}

/**
 * 流体漩涡背景
 * 旋转的流体漩涡效果，多层次叠加
 */
@Composable
private fun FluidVortexBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vortex-spin")
    val spinPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "spin-anim",
    )

    Canvas(
        modifier =
        modifier,
    ) {
        val baseColor = if (isDarkMode == true) Color(0xFF080810) else Color(0xFFF8F8FC)
        drawRect(color = baseColor.copy(alpha = 0.75f), size = size)

        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = min(size.width, size.height) * 0.48f

        // 多层旋转的流体层
        val layers = 4
        repeat(layers) { layer ->
            val layerRadius = maxRadius * (0.3f + layer * 0.2f)
            val rotationOffset = layer * 90f
            val spiralCount = 3 + layer

            val path = Path()
            var firstPoint = true

            // 为每一层创建螺旋路径
            for (i in 0..100) {
                val progress = i / 100f
                val angle =
                    ((spinPhase + rotationOffset + progress * 360f * spiralCount) % 360f) * (PI / 180f)

                // 获取该角度对应的FFT能量
                val fftIndex = (i * fftDrawData.size / 100).coerceIn(0, max(0, fftDrawData.size - 1))
                val energy = fftDrawData.getOrNull(fftIndex)?.coerceIn(0f, 1f) ?: 0f

                // 半径随能量波动
                val radius =
                    layerRadius * (1f + sin(progress * PI * 4f).toFloat() * 0.15f) * (0.9f + energy * 0.3f)

                val x = centerX + cos(angle).toFloat() * radius
                val y = centerY + sin(angle).toFloat() * radius

                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }
            }

            val layerColor = shiftHue(coverColor, layer * 25f - 50f)

            drawPath(
                path = path,
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                layerColor.copy(alpha = 0.7f),
                                layerColor.copy(alpha = 0.4f),
                                layerColor.copy(alpha = 0.7f),
                            ),
                    ),
                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f + layer * 2f,
                        cap = StrokeCap.Round,
                    ),
            )
        }

        // 中心漩涡核心
        val avgEnergy =
            if (fftDrawData.isNotEmpty()) {
                fftDrawData.average().toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

        repeat(5) { ring ->
            val ringRadius = 20f + ring * 15f + avgEnergy * 30f
            drawCircle(
                color = shiftHue(coverColor, ring * 20f).copy(alpha = (0.5f - ring * 0.08f)),
                center = Offset(centerX, centerY),
                radius = ringRadius,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 2f),
            )
        }
    }
}

/**
 * 计算颜色亮度（感知亮度）
 */
private fun calculateLuminance(color: Color): Float = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

/**
 * 色相偏移辅助函数
 * 简化版本：通过RGB分量旋转实现
 */
private fun shiftHue(
    color: Color,
    degrees: Float,
): Color {
    val amount = degrees / 360f

    // 简单的颜色偏移算法
    val r = color.red
    val g = color.green
    val b = color.blue

    return when {
        amount > 0 ->
            Color(
                red = (r + amount * (1 - r)).coerceIn(0f, 1f),
                green = (g - amount * g * 0.5f).coerceIn(0f, 1f),
                blue = (b + amount * (1 - b) * 0.5f).coerceIn(0f, 1f),
                alpha = color.alpha,
            )

        else ->
            Color(
                red = (r + amount * r * 0.5f).coerceIn(0f, 1f),
                green = (g - amount * (1 - g)).coerceIn(0f, 1f),
                blue = (b + amount * (1 - b)).coerceIn(0f, 1f),
                alpha = color.alpha,
            )
    }
}
