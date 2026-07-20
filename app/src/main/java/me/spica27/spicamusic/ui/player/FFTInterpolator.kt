package me.spica27.spicamusic.ui.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.player.api.IFFTProcessor

/**
 * FFT 数据插值器
 *
 * 将低刷新率的 FFT 原始数据平滑插值为约120fps 的绘制数据。
 *
 * 生命周期完全由 Flow 订阅驱动，无需手动订阅/解绑：
 * - UI 通过 collectAsStateWithLifecycle 收集 [interpolatedData] 时自动开始计算
 * - 页面不可见或应用进入后台时收集停止，计算随之自动结束
 * - 输出已收敛且无新 FFT 帧时（暂停/静音），循环挂起等待，不空转耗电
 */
class FFTInterpolator(
    private val fftProcessor: IFFTProcessor,
    scope: CoroutineScope,
) {
    companion object {
        // 绘制帧间隔（约 60fps）
        private const val FRAME_INTERVAL_MS = 8L

        // FFT 帧间隔的插值时长上下限
        private const val MIN_TRANSITION_MS = 8L
        private const val MAX_TRANSITION_MS = 250L

        // 无订阅者后延迟停止，避免页面切换瞬间反复重启
        private const val STOP_TIMEOUT_MS = 2_000L
    }

    private val bandCount = IFFTProcessor.BAND_COUNT

    /**
     * 插值后的绘制数据 (31个频段, 0.0-1.0)
     * 适合直接用于 UI 绘制，约120fps 平滑更新
     */
    val interpolatedData: StateFlow<FloatArray> =
        flow {
            // 全部状态为流内局部变量，单协程执行，无需加锁
            val fromBands = FloatArray(bandCount)
            val toBands = FloatArray(bandCount)
            var lastOutput = FloatArray(bandCount)
            var lastFrame: FloatArray? = null
            var transitionStart = 0L
            var transitionDuration = 100L

            while (true) {
                val now = SystemClock.elapsedRealtime()

                val target = fftProcessor.bands.value
                if (target !== lastFrame) {
                    // 收到新 FFT 帧：以当前输出为起点，向新数据过渡
                    lastOutput.copyInto(fromBands)
                    target.copyInto(toBands)
                    if (lastFrame != null) {
                        transitionDuration =
                            (now - transitionStart)
                                .coerceIn(MIN_TRANSITION_MS, MAX_TRANSITION_MS)
                    }
                    transitionStart = now
                    lastFrame = target
                }

                val progress =
                    ((now - transitionStart) / transitionDuration.toFloat()).coerceIn(0f, 1f)

                // 每帧发射全新数组：TextureView 渲染线程等消费方会跨帧持有引用，
                // 复用缓冲区会被本协程并发改写导致撕裂帧（31 个 float 的分配开销可忽略）
                val frame = FloatArray(bandCount)
                for (i in 0 until bandCount) {
                    frame[i] = fromBands[i] + (toBands[i] - fromBands[i]) * progress
                }
                lastOutput = frame
                emit(frame)

                if (progress >= 1f) {
                    // 已收敛且暂无新数据：挂起等待下一帧 FFT，避免空转
                    fftProcessor.bands.first { it !== lastFrame }
                } else {
                    delay(FRAME_INTERVAL_MS)
                }
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = STOP_TIMEOUT_MS,
                    // 停止后清除缓存值，重新订阅时从全 0 开始
                    replayExpirationMillis = 0,
                ),
                FloatArray(bandCount),
            )
}
