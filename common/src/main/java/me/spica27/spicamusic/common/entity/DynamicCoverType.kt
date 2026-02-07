package me.spica27.spicamusic.common.entity

/**
 * 动态封面类型
 * 控制播放器封面翻转后的背面显示效果
 */
sealed class DynamicCoverType(
    val value: String,
    val name: String,
) {
    /** 闪耀星光 - 分形 + 调色板音频可视化器 */
    object ShiningStars : DynamicCoverType(
        "shining_stars",
        "闪耀星光",
    )

    /** 音频城市 - 3D 俯瞰柱体网格可视化器 */
    object AudioCity : DynamicCoverType(
        "audio_city",
        "音频城市",
    )

    /** 关闭 - 禁用封面翻转 */
    object OFF : DynamicCoverType(
        "off",
        "关闭",
    )

    override fun toString(): String = name

    companion object {
        fun fromString(value: String): DynamicCoverType =
            when (value) {
                ShiningStars.value -> ShiningStars
                AudioCity.value -> AudioCity
                OFF.value -> OFF
                else -> ShiningStars
            }

        val presets: List<DynamicCoverType>
            get() = listOf(ShiningStars, AudioCity, OFF)
    }
}
