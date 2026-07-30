package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable

/**
 * 播放器进度条样式。
 */
@Immutable
sealed class ProgressBarStyle(
    val value: String,
    val name: String,
) {
    object ExpressiveWavy : ProgressBarStyle(
        "expressive_wavy",
        "流动波浪",
    )

    object DynamicWaveform : ProgressBarStyle(
        "dynamic_waveform",
        "动态波形",
    )

    object TimeDomainWaveform : ProgressBarStyle(
        "time_domain_waveform",
        "时域波形",
    )

    override fun toString(): String = name

    companion object {
        fun fromString(value: String): ProgressBarStyle =
            when (value) {
                ExpressiveWavy.value -> ExpressiveWavy
                DynamicWaveform.value -> DynamicWaveform
                TimeDomainWaveform.value -> TimeDomainWaveform
                else -> ExpressiveWavy
            }

        val presets: List<ProgressBarStyle>
            get() = listOf(ExpressiveWavy, DynamicWaveform, TimeDomainWaveform)
    }
}
