package me.spica27.spicamusic.common.audio

import kotlin.math.tan

/**
 * BS.1770 K 加权双级 biquad（Direct Form I）。
 *
 * 系数在 48kHz 处按 BS.1770-4 定义，对其它采样率用双线性变换 + 频率预畸变（pre-warp）
 * 重新推导，做法与 libebur128 一致。stage1 为高架搁架（≈ +4dB @ high shelf），
 * stage2 为二阶高通（≈ 38Hz，即 RLB 加权）。
 *
 * 纯 Kotlin，实时播放与离线测量共用，确保两侧测得同一个值。
 * 非线程安全：每个声道各持一份，且只由单一线程推进。
 */
class KWeightingFilter(sampleRate: Int) {

    private val stage1 = Biquad().apply { setStage1(sampleRate) }
    private val stage2 = Biquad().apply { setStage2(sampleRate) }

    /** 对单个采样点做完整 K 加权（stage1 -> stage2） */
    fun process(input: Float): Float = stage2.process(stage1.process(input))

    fun reset() {
        stage1.reset()
        stage2.reset()
    }

    private class Biquad {
        private var b0 = 1.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0

        // Direct Form I 状态
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        /** Stage 1：高架搁架滤波（head/pre-filter） */
        fun setStage1(sampleRate: Int) {
            // BS.1770-4 在 48kHz 的模拟原型参数
            val f0 = 1681.9744509555319
            val g = 3.99984385397 // dB
            val q = 0.7071752369554193

            val k = tan(Math.PI * f0 / sampleRate)
            val vh = pow10(g / 20.0)
            val vb = pow10(g / 40.0)

            val a0 = 1.0 + k / q + k * k
            b0 = (vh + vb * k / q + k * k) / a0
            b1 = 2.0 * (k * k - vh) / a0
            b2 = (vh - vb * k / q + k * k) / a0
            a1 = 2.0 * (k * k - 1.0) / a0
            a2 = (1.0 - k / q + k * k) / a0
        }

        /** Stage 2：二阶高通滤波（RLB 加权，≈38Hz） */
        fun setStage2(sampleRate: Int) {
            val f0 = 38.13547087602444
            val q = 0.5003270373238773

            val k = tan(Math.PI * f0 / sampleRate)
            val a0 = 1.0 + k / q + k * k
            b0 = 1.0 / a0
            b1 = -2.0 / a0
            b2 = 1.0 / a0
            a1 = 2.0 * (k * k - 1.0) / a0
            a2 = (1.0 - k / q + k * k) / a0
        }

        fun process(input: Float): Float {
            val x0 = input.toDouble()
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            return y0.toFloat()
        }

        fun reset() {
            x1 = 0.0
            x2 = 0.0
            y1 = 0.0
            y2 = 0.0
        }

        private fun pow10(x: Double): Double = Math.pow(10.0, x)
    }
}
