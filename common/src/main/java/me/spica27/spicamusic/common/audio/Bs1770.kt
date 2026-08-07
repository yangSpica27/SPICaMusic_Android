package me.spica27.spicamusic.common.audio

/**
 * ITU-R BS.1770-4 / EBU R128 的共用常量与声道加权表。
 *
 * 纯 Kotlin，无 Android / Media3 依赖，便于在实时播放（feature-player-data）
 * 与离线扫描测量（feature-library-data）两侧共用同一套算法，避免两边实现漂移。
 */
object Bs1770 {

    /** 响度换算常数：L = -0.691 + 10*log10(加权功率和) */
    const val LUFS_OFFSET = -0.691

    /** 绝对门限（LUFS）：低于此值的块视为静音，不计入积分 */
    const val ABSOLUTE_GATE_LUFS = -70.0

    /** 相对门限相对量（LU）：绝对门控后整体响度再降 10 LU */
    const val RELATIVE_GATE_LU = 10.0

    // 通道加权系数（功率域）：L/R/C = 1.0，Ls/Rs = 1.41（+1.5dB），LFE 完全排除。
    // 注意 1.41 是功率域系数（10^(1.5/10)），不是振幅域的 1.5dB，用错会差 3dB。
    const val W_MAIN = 1.0
    const val W_SURROUND = 1.41
    const val W_EXCLUDED = 0.0

    /**
     * 按声道数推断 BS.1770 权重表。
     *
     * 交织顺序按 Android/Media3 惯例：0=L 1=R 2=C 3=LFE 4=Ls 5=Rs (6=SL 7=SR)。
     * Media3 的 AudioFormat 只带 sampleRate/channelCount/encoding、没有声道掩码，
     * 因此布局只能由声道数推断；未知布局一律退回全 1.0（不会放大误差）。
     */
    fun weights(channelCount: Int): DoubleArray =
        when (channelCount) {
            // 单声道：BS.1770 未定义，业界（libebur128）按 centre 处理，权重 1.0
            1 -> doubleArrayOf(W_MAIN)
            2 -> doubleArrayOf(W_MAIN, W_MAIN)
            // 3.0 = L R C
            3 -> doubleArrayOf(W_MAIN, W_MAIN, W_MAIN)
            // 4.0 quad = L R Ls Rs
            4 -> doubleArrayOf(W_MAIN, W_MAIN, W_SURROUND, W_SURROUND)
            // 5.0 = L R C Ls Rs
            5 -> doubleArrayOf(W_MAIN, W_MAIN, W_MAIN, W_SURROUND, W_SURROUND)
            // 5.1 = L R C LFE Ls Rs，LFE(index 3) 排除
            6 -> doubleArrayOf(W_MAIN, W_MAIN, W_MAIN, W_EXCLUDED, W_SURROUND, W_SURROUND)
            // 7.1 = L R C LFE Lb Rb Ls Rs，LFE(index 3) 排除，四个环绕均按 1.41
            8 ->
                doubleArrayOf(
                    W_MAIN, W_MAIN, W_MAIN, W_EXCLUDED,
                    W_SURROUND, W_SURROUND, W_SURROUND, W_SURROUND,
                )
            else -> DoubleArray(channelCount.coerceAtLeast(1)) { W_MAIN }
        }
}
