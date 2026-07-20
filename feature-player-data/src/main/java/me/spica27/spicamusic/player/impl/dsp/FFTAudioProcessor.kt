package me.spica27.spicamusic.player.impl.dsp

import be.tarsos.dsp.util.fft.FFT
import be.tarsos.dsp.util.fft.HammingWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.player.api.IFFTProcessor
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * FFT 音频处理器实现
 * 使用 TarsosDSP 进行实时傅里叶变换，分析 31 个频段的响度
 * 使用独立线程异步处理，避免阻塞音频流
 *
 * 高采样率（>48kHz，如 Hi-Res 88.2/96/176.4/192kHz）音源会先做整数倍
 * 均值抽取，将有效采样率压回 ≤48kHz：既保证 4096 点 FFT 的低频分辨率
 * （避免 20-40Hz 频段落入直流 bin 产生异常数据），也等比例降低计算量。
 *
 * 结果通过 [bands] StateFlow 发布，无监听器注册机制。
 * 实际采样需同时满足两个条件（见 [isSamplingActive]）：
 * - 应用在前台：由前后台生命周期通过 [enable]/[disable] 驱动；
 * - 正在播放：由播放器通过 [setPlaybackActive] 驱动
 *   （暂停时 ExoPlayer 仍会向管线预缓冲数据，不应采样）。
 */
class FFTAudioProcessor : IFFTProcessor {

    companion object {
        private const val TAG = "FFTAudioProcessor"

        // FFT 窗口大小 (2的幂次)
        private const val FFT_SIZE = 4096

        // 最小分贝值
        private const val MIN_DB = 0f

        // 最大分贝值
        private const val MAX_DB = 60f

        // 抽取后的最大有效采样率：44.1/48kHz 原样处理，Hi-Res 按整数倍抽取
        private const val MAX_EFFECTIVE_SAMPLE_RATE = 48000
    }

    private val _bands = MutableStateFlow(FloatArray(IFFTProcessor.BAND_COUNT))
    override val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    // 默认关闭：应用进入前台后由生命周期观察者调用 enable() 开启，
    // 避免进程在后台被拉起（如媒体恢复播放）时白白采样耗电
    private val _isEnabled = MutableStateFlow(false)
    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // 播放状态门控：暂停时 ExoPlayer 仍会通过管线预缓冲音频（暂停中 seek/切歌
    // 会预填充约 250ms 数据），不加此门控这些数据会推翻暂停时的清零
    @Volatile
    private var isPlaybackActive = false

    /**
     * 当前是否实际采样（应用在前台且正在播放）
     * 供包装器在拷贝音频数据前快速判断，避免无谓拷贝
     */
    val isSamplingActive: Boolean
        get() = _isEnabled.value && isPlaybackActive

    /**
     * 播放状态变化时由播放器调用；暂停/停止时停止采样
     */
    fun setPlaybackActive(active: Boolean) {
        isPlaybackActive = active
    }

    private val stateLock = Any()

    // FFT 实例与采样率无关，仅取决于窗口大小，全程复用
    private val fft = FFT(FFT_SIZE, HammingWindow())

    // ==== 以下采样缓冲状态由音频线程（process 调用方）独占读写 ====
    // reset() 不直接触碰这些字段（避免跨线程数据竞争），而是递增
    // processingGeneration，由音频线程在下次 process() 时检测到代际变化后自行清零

    // 输入采样率与抽取参数
    private var inputSampleRate = 44100
    private var decimationFactor = 1
    private var effectiveSampleRate = 44100

    // 抽取分组的跨调用累计状态
    private var decimAccum = 0f
    private var decimCount = 0

    // 音频缓冲区（抽取后的单声道样本环形填充）
    private val audioBuffer = FloatArray(FFT_SIZE)
    private var bufferIndex = 0

    // 音频线程最后一次观察到的 reset 代际
    private var lastSeenGeneration = 0

    // 预分配复用缓冲区，避免热路径中重复分配 FloatArray
    // 用于 PCM 解码的转换输出（最大可容纳一次 process 调用的样本数）
    private var convertBuffer = FloatArray(FFT_SIZE)

    // 用于传递给 performFFT 的独立拷贝（防止 audioBuffer 被下次 process 覆盖）
    private val processBuffer = FloatArray(FFT_SIZE)

    // 预分配 FFT 幅度缓冲区，避免 performFFT 每次分配（只在 FFT 线程中使用，无需同步）
    private val magnitudesBuffer = FloatArray(FFT_SIZE / 2)

    // 预分配频段映射结果缓冲区
    private val bandValuesBuffer = FloatArray(IFFTProcessor.BAND_COUNT)

    // 预分配 _bands 发射缓冲区（双缓冲避免外部持有引用时被覆盖）
    private val bandResultBuffers = arrayOf(
        FloatArray(IFFTProcessor.BAND_COUNT),
        FloatArray(IFFTProcessor.BAND_COUNT),
    )
    private var activeBandResultIndex = 0

    // 处理标志 - 使用原子布尔值确保线程安全
    private val isProcessing = AtomicBoolean(false)

    // reset/flush 后递增，用于阻止旧 FFT 任务发布过期数据
    private val processingGeneration = AtomicInteger(0)

    /**
     * 检查是否正在处理，用于外部快速判断
     */
    fun isBusy(): Boolean = isProcessing.get()

    // 独立的FFT处理线程
    private val fftExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "FFT-Processor").apply {
            priority = Thread.MIN_PRIORITY // 设置为最低优先级，避免影响音频播放
        }
    }
    private val fftDispatcher = fftExecutor.asCoroutineDispatcher()
    private val fftScope = CoroutineScope(fftDispatcher + SupervisorJob())

    override fun enable() {
        _isEnabled.value = true
    }

    override fun disable() {
        _isEnabled.value = false
        reset()
    }

    /**
     * 释放资源
     */
    fun release() {
        fftScope.cancel()
        fftExecutor.shutdown()
    }

    /**
     * 处理音频数据
     * 如果正在处理FFT，丢弃当前音频数据，避免阻塞
     */
    override fun process(
        audioData: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        audioDataSize: Int
    ) {
        if (!isSamplingActive) {
            return
        }

        // 如果正在处理FFT，丢弃当前音频数据，避免阻塞
        if (isProcessing.get()) {
            return
        }

        val bytesPerSample = PcmMonoDecoder.bytesPerSample(encoding)
        if (bytesPerSample == 0 || channelCount <= 0 || sampleRate <= 0) {
            return
        }

        // 在填充数据之前读取代际快照：
        // 1. 检测到 reset() 发生过时，先清空本线程独占的采样缓冲状态；
        // 2. 快照传给 FFT 任务，发布前在 stateLock 下复验，保证本次调用期间
        //    发生的 reset 一定能使过期频谱的发布失败（不会覆盖刚清零的 bands）
        val generation = processingGeneration.get()
        if (generation != lastSeenGeneration) {
            lastSeenGeneration = generation
            bufferIndex = 0
            decimAccum = 0f
            decimCount = 0
        }

        // 采样率变化时更新抽取参数并重置累计状态
        if (sampleRate != inputSampleRate || decimationFactor == 0) {
            inputSampleRate = sampleRate
            decimationFactor = ((sampleRate + MAX_EFFECTIVE_SAMPLE_RATE - 1) / MAX_EFFECTIVE_SAMPLE_RATE)
                .coerceAtLeast(1)
            effectiveSampleRate = sampleRate / decimationFactor
            decimAccum = 0f
            decimCount = 0
            bufferIndex = 0
        }

        // 解码第一个声道为浮点样本，复用 convertBuffer 避免分配
        val maxSampleCount = audioDataSize / (bytesPerSample * channelCount)
        if (maxSampleCount <= 0) {
            return
        }
        if (convertBuffer.size < maxSampleCount) {
            convertBuffer = FloatArray(maxSampleCount)
        }
        val sampleCount = PcmMonoDecoder.decodeFirstChannel(
            data = audioData,
            sizeBytes = audioDataSize,
            channelCount = channelCount,
            encoding = encoding,
            out = convertBuffer,
        )

        // 均值抽取后填充缓冲区
        val factor = decimationFactor
        for (i in 0 until sampleCount) {
            decimAccum += convertBuffer[i]
            decimCount++
            if (decimCount < factor) {
                continue
            }
            val sample = decimAccum / factor
            decimAccum = 0f
            decimCount = 0

            audioBuffer[bufferIndex] = sample
            bufferIndex++

            // 缓冲区满时异步执行 FFT 分析
            if (bufferIndex >= FFT_SIZE) {
                bufferIndex = 0

                // 将 audioBuffer 拷贝到 processBuffer，避免异步执行时被主路径覆盖
                if (isProcessing.compareAndSet(false, true)) {
                    audioBuffer.copyInto(processBuffer)
                    // 使用 process() 入口处的代际快照（而非此刻重读），
                    // 本次调用期间发生的 reset 会使发布校验失败
                    val fftSampleRate = effectiveSampleRate
                    // 在独立线程中异步处理FFT
                    fftScope.launch {
                        try {
                            performFFT(
                                fftData = processBuffer,
                                sampleRate = fftSampleRate,
                                generation = generation,
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "FFT processing error")
                        } finally {
                            // 处理完成，允许接收新数据
                            isProcessing.set(false)
                        }
                    }
                }
            }
        }
    }

    override fun reset() {
        synchronized(stateLock) {
            // 递增代际：使在途 FFT 任务的发布校验失败；采样缓冲状态由音频线程
            // 在下次 process() 时检测到代际变化后自行清空（不跨线程写共享字段）
            processingGeneration.incrementAndGet()
            _bands.value = FloatArray(IFFTProcessor.BAND_COUNT)
        }
    }

    /**
     * 执行 FFT 分析（在独立线程中执行）
     * 所有中间缓冲区均为预分配字段，零堆分配热路径。
     * @param fftData 已填充的音频数据
     * @param sampleRate 本次任务对应的有效采样率快照（抽取后）
     * @param generation 本次任务启动时的 reset 代际
     */
    private fun performFFT(
        fftData: FloatArray,
        sampleRate: Int,
        generation: Int,
    ) {
        // 执行 FFT
        fft.forwardTransform(fftData)

        // 计算频谱幅度（复用预分配的 magnitudesBuffer，零分配）
        for (i in 0 until FFT_SIZE / 2) {
            val real = fftData[2 * i]
            val imag = fftData[2 * i + 1]
            magnitudesBuffer[i] = sqrt(real * real + imag * imag)
        }

        // 映射到 31 个频段（结果写入 bandValuesBuffer，零分配）
        mapToBands(magnitudesBuffer, sampleRate, bandValuesBuffer)

        synchronized(stateLock) {
            if (generation != processingGeneration.get()) {
                return
            }

            // 双缓冲：切换到下一个结果缓冲区，避免外部持有引用时被覆盖
            val nextIndex = activeBandResultIndex xor 1
            val nextResult = bandResultBuffers[nextIndex]
            bandValuesBuffer.copyInto(nextResult)
            activeBandResultIndex = nextIndex

            // 更新状态（发出已复制的缓冲区引用，零额外分配）
            _bands.value = nextResult
        }
    }

    /**
     * 将 FFT 结果映射到 31 个频段，结果写入 result（复用，零分配）
     */
    private fun mapToBands(magnitudes: FloatArray, sampleRate: Int, result: FloatArray) {
        val frequencyResolution = sampleRate.toFloat() / FFT_SIZE
        val nyquist = sampleRate / 2f

        for (bandIndex in 0 until IFFTProcessor.BAND_COUNT) {
            val centerFreq = IFFTProcessor.FREQUENCY_BANDS[bandIndex]

            // 计算频段范围 (对数间隔)
            val lowFreq = if (bandIndex == 0) {
                16f // 最低频率
            } else {
                sqrt(centerFreq * IFFTProcessor.FREQUENCY_BANDS[bandIndex - 1])
            }

            val highFreq = if (bandIndex == IFFTProcessor.BAND_COUNT - 1) {
                22000f // 最高频率
            } else {
                sqrt(centerFreq * IFFTProcessor.FREQUENCY_BANDS[bandIndex + 1])
            }

            // 超出奈奎斯特频率的频段无有效数据（低采样率音源）
            if (lowFreq >= nyquist) {
                result[bandIndex] = 0f
                continue
            }

            // 计算 FFT bin 范围；跳过 bin 0（直流分量），避免直流偏置污染低频段
            val lowBin = max(1, (lowFreq / frequencyResolution).toInt())
            val highBin = (min(highFreq, nyquist) / frequencyResolution).toInt()
                .coerceIn(lowBin, magnitudes.size - 1)

            // 计算该频段的平均能量
            var sum = 0f
            var count = 0
            for (bin in lowBin..highBin) {
                sum += magnitudes[bin]
                count++
            }

            val avgMagnitude = if (count > 0) sum / count else 0f

            // 转换为分贝并归一化到 0-1
            val db = if (avgMagnitude > 0) {
                20 * kotlin.math.log10(avgMagnitude.toDouble()).toFloat()
            } else {
                MIN_DB
            }

            // 归一化到 0-1 范围
            val normalized = (db - MIN_DB) / (MAX_DB - MIN_DB)
            result[bandIndex] = max(0f, min(1f, normalized))
        }
    }
}
