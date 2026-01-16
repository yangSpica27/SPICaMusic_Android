package me.spica27.spicamusic.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.spica27.spicamusic.player.api.IFFTProcessor
import me.spica27.spicamusic.player.api.IMusicPlayer

/**
 * FFT 数据插值器
 *
 * 负责将 FFT 原始数据平滑插值为适合每帧绘制的数据
 * 特性:
 * - 自动插值，保证动画流畅（60fps）
 * - 支持订阅管理，无订阅时自动停止计算节约性能
 * - 线程安全的数据访问
 */
class FFTInterpolator(
    private val player: IMusicPlayer,
    private val fftProcessor: IFFTProcessor,
    private val scope: CoroutineScope,
) {
    // 频段数量
    private val bandCount = IFFTProcessor.BAND_COUNT

    // 当前 FFT 原始数据
    private val currentBands = FloatArray(bandCount)

    // 上一帧 FFT 数据
    private val lastBands = FloatArray(bandCount)

    // 插值后的绘制数据
    private val _interpolatedData = MutableStateFlow(FloatArray(bandCount))

    /**
     * 插值后的绘制数据
     * 适合直接用于 UI 绘制，60fps 更新
     */
    val interpolatedData: StateFlow<FloatArray> = _interpolatedData.asStateFlow()

    // 上次 FFT 更新时间
    private var lastUpdateTime = 0L

    // 两次更新的时间间隔
    private var updateInterval = 16L

    // 数据锁，保证线程安全
    private val dataLock = Mutex()

    // 插值计算任务
    private var interpolationJob: Job? = null

    // FFT 数据监听任务
    private var fftListenerJob: Job? = null

    // 订阅数量
    private var subscriberCount = 0
    private val subscriberLock = Mutex()

    /**
     * 订阅插值数据
     * 当有订阅者时，开始计算；所有订阅者取消后，停止计算以节约性能
     */
    suspend fun subscribe() {
        subscriberLock.withLock {
            subscriberCount++
            if (subscriberCount == 1) {
                // 第一个订阅者，启动计算
                startInterpolation()
            }
        }
    }

    /**
     * 取消订阅
     * 当所有订阅者都取消后，自动停止计算
     */
    suspend fun unsubscribe() {
        subscriberLock.withLock {
            subscriberCount = (subscriberCount - 1).coerceAtLeast(0)
            if (subscriberCount == 0) {
                // 最后一个订阅者取消，停止计算
                stopInterpolation()
            }
        }
    }

    /**
     * 启动插值计算
     */
    private fun startInterpolation() {
        // 启动 FFT 数据监听
        fftListenerJob =
            scope.launch(Dispatchers.Default) {
                fftProcessor.bands.collect { newBands ->
                    dataLock.withLock {
                        // 保存上一帧数据
                        currentBands.copyInto(lastBands)
                        // 更新当前数据
                        newBands.copyInto(currentBands)
                        // 更新时间间隔
                        val currentTime = System.currentTimeMillis()
                        updateInterval = (currentTime - lastUpdateTime).coerceAtLeast(16L)
                        lastUpdateTime = currentTime
                    }
                }
            }

        // 启动插值任务（60fps）
        interpolationJob =
            scope.launch(Dispatchers.Default) {
                val frameInterval = 8L // 约 60fps

                while (isActive) {
                    dataLock.withLock {
                        val currentTime = System.currentTimeMillis()
                        val safeInterval = updateInterval.coerceAtLeast(16L)

                        // 计算插值进度 (0.0 - 1.0)
                        val progress =
                            ((currentTime - lastUpdateTime) / safeInterval.toFloat())
                                .coerceIn(0f, 1f)

                        // 对每个频段进行线性插值
                        val newData =
                            FloatArray(bandCount) { index ->
                                lastBands[index] + (currentBands[index] - lastBands[index]) * progress
                            }

                        // 更新插值数据
                        _interpolatedData.value = newData
                    }

                    delay(frameInterval)
                }
            }
    }

    /**
     * 停止插值计算
     */
    private fun stopInterpolation() {
        interpolationJob?.cancel()
        interpolationJob = null

        fftListenerJob?.cancel()
        fftListenerJob = null

        // 重置数据为全0
        _interpolatedData.value = FloatArray(bandCount)
    }

    /**
     * 清理资源
     */
    fun dispose() {
        stopInterpolation()
    }
}
