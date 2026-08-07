package me.spica27.spicamusic.player.impl.dsp

import android.media.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.common.audio.Bs1770
import me.spica27.spicamusic.common.audio.KWeightingFilter
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 响度归一化处理器（Loudness Normalization），基于实时 AGC（自动增益控制）。
 *
 * 使用 400ms 滑动窗口估算当前响度，动态调整增益以达到目标响度。
 * 结合逐采样反馈限幅器与软削波兜底防止削波。
 *
 * ⚠️ 注意：实时 AGC 只能做到曲内动态压缩，无法保证曲间响度一致。
 * 如需精确的曲间响度归一化，应使用 EBU R128 积分响度测量。
 */
@UnstableApi
class LoudnessNormalizationAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "LoudnessNormalization"

        // 默认目标响度（LUFS）。-14 LUFS 是 Spotify/YouTube 等主流平台的常用目标
        private const val DEFAULT_TARGET_LUFS = -14.0

        // BS.1770 响度换算常数，与离线测量共用同一份定义
        private const val LUFS_OFFSET = Bs1770.LUFS_OFFSET

        // 低于此响度视为静音/极弱，不调整增益（约对应 -70 LUFS 门限）
        private const val SILENCE_LUFS = Bs1770.ABSOLUTE_GATE_LUFS

        // 增益上下限
        private const val MIN_GAIN = 0.25f
        private const val MAX_GAIN = 4.0f

        // 增益平滑时间常数（毫秒）：起音慢、释放快
        private const val ATTACK_MS = 800f
        private const val RELEASE_MS = 200f

        // LUFS 滑动均方窗口时长（毫秒），约等于 short-term 窗口量级
        private const val LOUDNESS_WINDOW_MS = 400f

        // 软限幅拐点与最终输出天花板（线性幅度，1.0 = 满刻度）
        private const val SOFT_KNEE_THRESHOLD = 0.75f
        private const val OUTPUT_CEILING = 0.98f

        // 反馈式限幅器释放时间常数（毫秒）：压制瞬时生效，恢复缓慢
        private const val LIMITER_RELEASE_MS = 150f
    }

    @Volatile
    private var enabled = false

    // 主线程置位、音频线程消费：请求在下一块处理开头重置全部 DSP 状态，
    // 避免主线程直接改写音频线程独占的滤波器/增益状态（参照 FFT 的代际模式）
    @Volatile
    private var resetRequested = false

    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    private var sampleRate = 44100
    private var channelCount = 2

    // 当前平滑后的施加增益
    private var currentGain = 1.0f

    // 反馈式限幅器的当前压制量（1.0 表示不压制）
    private var limiterGain = 1.0f

    // 每采样点平滑系数（configure 时按采样率换算）
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var limiterReleaseCoeff = 0f
    // LUFS 滑动均方窗口的单极点平滑系数
    private var loudnessCoeff = 0f

    // K 加权滤波器：每声道一个（内部含 stage1 搁架 + stage2 高通两级）
    private var kFilters: Array<KWeightingFilter> = emptyArray()

    // BS.1770 通道加权表（功率域），随 channelCount 在 configure 中重建
    private var channelWeights: DoubleArray = Bs1770.weights(2)

    // K 加权后信号的滑动均方（跨块累计）
    private var meanSquare = 0.0

    // 滑动窗口是否已装填过首块：未装填时直接取本块均方，避免从 0 起步导致
    // 头几百毫秒测量值偏低十几 dB、增益冲到 MAX_GAIN
    private var windowPrimed = false

    // 跨块的帧内相位：Media3 不保证每块都从帧边界开始，需要它把 K 加权滤波器
    // 稳定绑定到各自的声道上
    private var framePhase = 0

    // 复用输出缓冲区，避免每帧 allocateDirect
    private var cachedOutputBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate
        this.channelCount = if (inputAudioFormat.channelCount > 0) inputAudioFormat.channelCount else 2

        attackCoeff = smoothingCoeff(ATTACK_MS)
        releaseCoeff = smoothingCoeff(RELEASE_MS)
        limiterReleaseCoeff = smoothingCoeff(LIMITER_RELEASE_MS)
        loudnessCoeff = smoothingCoeff(LOUDNESS_WINDOW_MS)

        // 为每个声道构建 K 加权滤波器（与离线测量共用同一份实现）
        kFilters = Array(channelCount) { KWeightingFilter(sampleRate) }
        channelWeights = Bs1770.weights(channelCount)
        meanSquare = 0.0
        windowPrimed = false
        framePhase = 0
        limiterGain = 1.0f
        // configure 意味着换了一路新流，施加中的增益也必须归零重来，
        // 否则未测量的新曲会从上一首的增益（最高 MAX_GAIN）开始做 AGC 平滑。
        currentGain = 1.0f

        return inputAudioFormat
    }

    // 始终保持活跃（已配置即纳入管线），enabled 判断在 queueInput 内部处理
    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!enabled) {
            outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        if (cachedOutputBuffer.capacity() < size) {
            cachedOutputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        }
        cachedOutputBuffer.clear().limit(size)
        val output = cachedOutputBuffer

        // 若主线程请求过重置，在音频线程内执行，保证 DSP 状态只由音频线程写
        if (resetRequested) {
            resetRequested = false
            currentGain = 1.0f
            limiterGain = 1.0f
            meanSquare = 0.0
            windowPrimed = false
            framePhase = 0
            kFilters.forEach { it.reset() }
        }

        val totalSamples = size / 2
        val channels = channelCount
        val filters = kFilters
        val weights = channelWeights

        // 第一遍：K 加权测量本块响度与峰值（绝对读取，不移动读指针）
        var blockKSum = 0.0
        val startPos = inputBuffer.position()
        var frameCount = 0
        if (channels in 1..filters.size && channels <= weights.size && totalSamples >= channels) {
            var i = 0
            while (i + channels <= totalSamples) {
                var frameKSum = 0.0
                for (ch in 0 until channels) {
                    // Media3 不保证块起点落在帧边界，用 framePhase 纠正声道归属，
                    // 否则滤波器状态会长期跨声道错配
                    val filterCh = (framePhase + ch) % channels
                    val raw = inputBuffer.getShort(startPos + (i + ch) * 2) / 32768f
                    val g = weights[filterCh]
                    if (g == Bs1770.W_EXCLUDED) continue // LFE：不参与响度测量
                    // K 加权（stage1 -> stage2），仅用于测量
                    val w = filters[filterCh].process(raw)
                    // BS.1770 通道加权后按声道求和（不除以声道数，标准即为求和）
                    frameKSum += g * (w * w).toDouble()
                }
                blockKSum += frameKSum
                frameCount++
                i += channels
            }
        }
        framePhase = if (channels > 0) (framePhase + totalSamples) % channels else 0

        // 滑动均方（每帧一次单极点更新，得到窗口内 K 加权功率）
        if (frameCount > 0) {
            val blockMeanSquare = blockKSum / frameCount
            if (!windowPrimed) {
                // 首块直接装填，避免从 0 平滑上来造成十几 dB 的测量偏低
                meanSquare = blockMeanSquare
                windowPrimed = true
            } else {
                // 按帧数推进平滑，等效于对连续帧逐帧滤波
                // meanSquare = a^frameCount * meanSquare + (1 - a^frameCount) * blockMeanSquare
                val aPow = pow(loudnessCoeff, frameCount)
                meanSquare = aPow * meanSquare + (1.0 - aPow) * blockMeanSquare
            }
        }

        // 换算 LUFS：L = -0.691 + 10*log10(meanSquare)
        val loudnessLufs =
            if (meanSquare > 1e-12) {
                LUFS_OFFSET + 10.0 * log10(meanSquare)
            } else {
                SILENCE_LUFS
            }

        // 把响度差（dB）换算为线性增益；静音段维持当前增益避免放大噪声
        val targetGain =
            if (loudnessLufs > SILENCE_LUFS) {
                val gainDb = DEFAULT_TARGET_LUFS - loudnessLufs
                pow10(gainDb / 20.0).toFloat().coerceIn(MIN_GAIN, MAX_GAIN)
            } else {
                currentGain
            }
        // 峰值保护下放到逐采样的反馈式限幅器：
        // 输入峰值只是瞬时值，用它静态压制整块增益会把提升预算全部吃掉
        val smoothingCoeffForBlock =
            if (targetGain > currentGain) attackCoeff else releaseCoeff

        // 平滑：系数是「每帧」的，必须按本块推进的帧数取幂再施加（与上面 meanSquare 一致），
        // 否则每块只走 ~0.003%，增益永远收敛不到目标。
        // frameCount == 0（未测到完整帧）时保持当前增益不动。
        val newGain =
            if (frameCount > 0) {
                val c = pow(smoothingCoeffForBlock, frameCount)
                (currentGain * c + targetGain * (1.0 - c)).toFloat()
            } else {
                currentGain
            }

        // 第二遍：块内线性增益渐变，消费全部样本并施加广播级增益（非 K 加权信号）
        val gainStart = currentGain
        val gainEnd = newGain
        val rampFrames = if (frameCount > 1) frameCount - 1 else 1
        for (i in 0 until totalSamples) {
            val sample = inputBuffer.short
            // 按帧索引取渐变位置，保证同一帧的各声道共用同一增益（否则会轻微调制声场）
            val frameIdx = if (channels > 0) i / channels else i
            val t = if (frameCount > 1) (frameIdx.toFloat() / rampFrames).coerceAtMost(1f) else 1f
            val g = gainStart + (gainEnd - gainStart) * t
            var x = sample / 32768f * g

            // 反馈式限幅（无前瞻）：超过拐点立即压下，之后按释放系数缓慢恢复到 1.0
            val mag = abs(x)
            limiterGain =
                if (mag * limiterGain > SOFT_KNEE_THRESHOLD) {
                    SOFT_KNEE_THRESHOLD / mag
                } else {
                    limiterGain * limiterReleaseCoeff + (1f - limiterReleaseCoeff)
                }
            x *= limiterGain

            // 软削波兜底：限幅器是反馈式的，越界后的首个采样点会漏过去，这里保证 |y| <= OUTPUT_CEILING
            val processed = (softClip(x) * 32767f)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(processed.toShort())
        }

        // size 为奇数时会剩一个字节；原样透传，否则管线会以同一块反复重入造成活锁
        if (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }

        currentGain = newGain

        output.flip()
        outputBuffer = output
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        // 切歌/seek 时重置平滑与测量状态，避免上一首的增益/响度残留
        limiterGain = 1.0f
        meanSquare = 0.0
        windowPrimed = false
        framePhase = 0
        resetRequested = false
        kFilters.forEach { it.reset() }
        // flush() 在切歌和 seek 时都会触发，AGC 路径下从单位增益重新收敛
        currentGain = 1.0f
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        kFilters = emptyArray()
    }

    /**
     * 设置响度归一化开关
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            // 必须先发布 resetRequested 再发布 enabled：两个 @Volatile 写有先后顺序，
            // 反过来的话音频线程可能看到 enabled=true 而尚未看到重置请求，
            // 用上次关闭前的陈旧 DSP 状态处理一整块（错误增益 + 可能的爆音）。
            resetRequested = true
        }
        this.enabled = enabled
        Timber.tag(TAG).d("Loudness normalization enabled: $enabled")
    }

    fun isEnabled(): Boolean = enabled

    /** 一阶指数平滑系数：coeff 越接近 1 越平滑。coeff = exp(-1 / (timeSec * fs))。 */
    private fun smoothingCoeff(timeMs: Float): Float {
        val timeConstantSec = timeMs / 1000f
        val samples = max(1f, timeConstantSec * sampleRate)
        return exp(-1.0 / samples).toFloat().coerceIn(0f, 0.99999f)
    }

    /**
     * 软削波：拐点以下完全透明，拐点以上用 tanh 的三阶 Padé 近似平滑压缩到天花板。
     * 拐点处一阶导数为 1（连续可导，无接缝失真），t = 3 处近似值恰为 1.0，故输出上界即 OUTPUT_CEILING。
     */
    private fun softClip(x: Float): Float {
        val a = abs(x)
        if (a <= SOFT_KNEE_THRESHOLD) return x
        val range = OUTPUT_CEILING - SOFT_KNEE_THRESHOLD
        val t = ((a - SOFT_KNEE_THRESHOLD) / range).coerceAtMost(3f)
        val t2 = t * t
        val shaped = t * (27f + t2) / (27f + 9f * t2)
        val y = SOFT_KNEE_THRESHOLD + range * shaped
        return if (x < 0f) -y else y
    }

    /** a^n，n 为非负整数（frameCount 通常几百，直接快速幂）。 */
    private fun pow(a: Float, n: Int): Double {
        var result = 1.0
        var base = a.toDouble()
        var e = n
        while (e > 0) {
            if (e and 1 == 1) result *= base
            base *= base
            e = e shr 1
        }
        return result
    }

    private fun pow10(x: Double): Double = Math.pow(10.0, x)

}
