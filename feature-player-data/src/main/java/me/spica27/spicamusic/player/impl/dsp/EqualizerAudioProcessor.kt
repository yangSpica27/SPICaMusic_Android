package me.spica27.spicamusic.player.impl.dsp

import android.media.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 10段均衡器处理器
 * 使用简单的增益调节实现频段均衡
 * 
 * 频段: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16kHz
 */
@UnstableApi
class EqualizerAudioProcessor : AudioProcessor {

    companion object {
        private const val TAG = "EqualizerAudioProcessor"
        private const val BAND_COUNT = 10
    }

    // 主线程持有的"请求增益"，仅主线程读写 (-12dB 到 +12dB)
    private val requestedGains = FloatArray(BAND_COUNT) { 0f }

    // 主线程发布的不可变增益快照：每次变更新建数组并整体替换引用（@Volatile 安全发布）。
    // 音频线程通过引用比较检测变更，在本线程内重算系数，杜绝跨线程就地改写滤波器状态。
    @Volatile
    private var gainsSnapshot: FloatArray = FloatArray(BAND_COUNT)

    // 10段中心频率 (Hz)
    private val bandFrequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    // 每个频段的 Q 值（带宽）
    private val bandQ = 1.0f

    // ==== 以下均为音频线程独占状态（configure/queueInput/flush/reset 同一音频线程）====
    private var sampleRate = 44100f
    private var channelCount = 2

    // 当前实际应用的增益（从 gainsSnapshot 拷入，音频线程私有）
    private val appliedGains = FloatArray(BAND_COUNT)

    // 上次已应用的快照引用，用于检测主线程是否发布了新快照
    private var lastAppliedSnapshot: FloatArray? = null

    // 缓存：当前应用增益是否全为 0（全 0 可直接透传，省去逐样本处理，也避免热路径 lambda 分配）
    private var appliedAllZero = true

    // [band][channel] 滤波器（音频线程独占）
    private var filters: Array<Array<BiquadFilter>>? = null

    @Volatile
    private var enabled = false
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // 复用输出缓冲区，避免每帧 allocateDirect
    private var cachedOutputBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != AudioFormat.ENCODING_PCM_16BIT) {
            this.inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
            return AudioProcessor.AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate.toFloat()
        this.channelCount = inputAudioFormat.channelCount

        filters = Array(BAND_COUNT) {
            Array(channelCount) { BiquadFilter() }
        }
        // 强制在音频线程重新应用当前快照并重算系数
        lastAppliedSnapshot = null
        applySnapshotIfChanged()
        return inputAudioFormat
    }

    /**
     * 音频线程内：检测主线程是否发布了新增益快照，若变更则拷入 appliedGains、
     * 重算滤波器系数并重置状态。全部写操作都发生在音频线程，无跨线程竞争。
     */
    private fun applySnapshotIfChanged() {
        val snapshot = gainsSnapshot
        if (snapshot === lastAppliedSnapshot) return
        lastAppliedSnapshot = snapshot

        val filterBank = filters ?: return
        var allZero = true
        for (band in 0 until BAND_COUNT) {
            val gainDb = snapshot.getOrElse(band) { 0f }
            appliedGains[band] = gainDb
            if (gainDb != 0f) allZero = false
            for (ch in 0 until channelCount) {
                filterBank[band][ch].setPeakingEQ(sampleRate, bandFrequencies[band], bandQ, gainDb)
                filterBank[band][ch].reset()
            }
        }
        appliedAllZero = allZero
    }

    // 始终保持活跃（已配置即纳入管线），enabled 判断在 queueInput 内部处理
    // 这样切换开关可以即时生效，无需等待 ExoPlayer 重新 configure
    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        // 音频线程内应用主线程发布的最新增益快照（若有变更）
        applySnapshotIfChanged()

        if (!enabled || appliedAllZero) {
            outputBuffer = inputBuffer
            return
        }

        val size = inputBuffer.remaining()
        if (size == 0) {
            return
        }

        // 创建输出缓冲区（复用已分配内存，容量不足时才重新分配）
        if (cachedOutputBuffer.capacity() < size) {
            cachedOutputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        }
        cachedOutputBuffer.clear().limit(size)
        val output = cachedOutputBuffer

        val filterBank = filters
        if (filterBank == null) {
            outputBuffer = inputBuffer
            return
        }

        val totalSamples = size / 2

        // 处理音频数据 (16-bit PCM, interleaved)
        for (i in 0 until totalSamples) {
            val sample = inputBuffer.short
            val channel = if (channelCount > 0) i % channelCount else 0

            var x = sample / 32768f
            // 应用 10 段均衡器（peaking EQ）
            for (band in 0 until BAND_COUNT) {
                if (appliedGains[band] != 0f) {
                    x = filterBank[band][channel].process(x)
                }
            }

            val processed = (x * 32768f).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(processed.toShort())
        }

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
        // flush 由 ExoPlayer 在音频线程调用，可直接重置滤波器状态（seek/切歌清除残留）
        val filterBank = filters
        if (filterBank != null) {
            for (band in 0 until BAND_COUNT) {
                for (ch in 0 until channelCount) {
                    filterBank[band][ch].reset()
                }
            }
        }
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        filters = null
        lastAppliedSnapshot = null
    }

    /**
     * 设置均衡器开关（主线程）。开关本身即时生效；重新开启时通过强制重应用快照清零滤波器状态。
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) {
            // 重新发布当前增益的新快照，促使音频线程重置滤波器状态（避免在主线程碰 DSP 状态）
            publishSnapshot()
        }
        Timber.tag(TAG).d("EQ enabled: $enabled")
    }

    /**
     * 设置单个频段增益（主线程）
     * @param band 频段索引 (0-9)
     * @param gainDb 增益值 (-12.0 to +12.0 dB)
     */
    fun setBandGain(band: Int, gainDb: Float) {
        if (band !in 0 until BAND_COUNT) {
            Timber.tag(TAG).w("Invalid band index: $band")
            return
        }

        requestedGains[band] = gainDb.coerceIn(-12f, 12f)
        publishSnapshot()

        Timber.tag(TAG).d("Set band $band gain: ${requestedGains[band]}dB")
    }

    /**
     * 设置所有频段增益（主线程）：一次性构建并发布快照，避免逐段发布产生中间态。
     */
    fun setAllBands(gains: FloatArray) {
        if (gains.size != BAND_COUNT) {
            Timber.tag(TAG).w("Invalid bands array size: ${gains.size}, expected $BAND_COUNT")
            return
        }

        for (i in 0 until BAND_COUNT) {
            requestedGains[i] = gains[i].coerceIn(-12f, 12f)
        }
        publishSnapshot()
    }

    /**
     * 获取当前频段增益（主线程）
     */
    fun getBandGains(): FloatArray = requestedGains.copyOf()

    /** 主线程：把 requestedGains 拷成新数组并原子发布，音频线程在下一块检测并应用。 */
    private fun publishSnapshot() {
        gainsSnapshot = requestedGains.copyOf()
    }

    /**
     * 经典二阶 Biquad（Transposed Direct Form II）
     * 参考: Audio EQ Cookbook (peaking EQ)
     */
    private class BiquadFilter {
        private var b0 = 1f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f

        private var z1 = 0f
        private var z2 = 0f

        fun setPeakingEQ(
            sampleRate: Float,
            frequency: Float,
            q: Float,
            gainDb: Float,
        ) {
            val a = 10.0.pow((gainDb / 40.0).toDouble()).toFloat()
            val w0 = (2.0 * PI * frequency / sampleRate).toFloat()
            val alpha = (sin(w0) / (2.0 * q)).toFloat()
            val cosW0 = cos(w0)

            val b0 = 1 + alpha * a
            val b1 = -2 * cosW0
            val b2 = 1 - alpha * a
            val a0 = 1 + alpha / a
            val a1 = -2 * cosW0
            val a2 = 1 - alpha / a

            this.b0 = (b0 / a0).toFloat()
            this.b1 = (b1 / a0).toFloat()
            this.b2 = (b2 / a0).toFloat()
            this.a1 = (a1 / a0).toFloat()
            this.a2 = (a2 / a0).toFloat()
        }

        fun process(input: Float): Float {
            val output = b0 * input + z1
            z1 = b1 * input - a1 * output + z2
            z2 = b2 * input - a2 * output
            return output
        }

        fun reset() {
            z1 = 0f
            z2 = 0f
        }
    }
}
