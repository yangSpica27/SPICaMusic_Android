package me.spica27.spicamusic.player.api

/**
 * 当前睡眠定时器状态。
 *
 * [deadlineElapsedRealtimeMs] 使用 [android.os.SystemClock.elapsedRealtime] 的时钟，
 * 不受用户修改系统时间影响。UI 通常只需要展示 [remainingMs]；deadline 暴露出来
 * 是为了让状态消费者可以在需要时基于单调时钟重新计算精确剩余时间。
 */
data class SleepTimerState(
    val durationMs: Long,
    val remainingMs: Long,
    val deadlineElapsedRealtimeMs: Long,
) {
    init {
        require(durationMs > 0) { "Sleep timer duration must be greater than zero" }
        require(remainingMs in 0..durationMs) {
            "Sleep timer remaining time must be between zero and duration"
        }
    }

    /** 根据当前单调时钟生成刷新后的状态；结果会限制在 [0, durationMs]。 */
    fun updatedAt(nowElapsedRealtimeMs: Long): SleepTimerState =
        copy(
            remainingMs =
                (deadlineElapsedRealtimeMs - nowElapsedRealtimeMs)
                    .coerceIn(0L, durationMs),
        )
}
