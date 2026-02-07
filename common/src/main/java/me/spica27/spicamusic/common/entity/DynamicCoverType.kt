package me.spica27.spicamusic.common.entity

/**
 * 动态封面类型
 * 控制播放器封面翻转后的背面显示效果
 */
sealed class DynamicCoverType(
    val value: String,
    val name: String,
) {
    /** 动态方格 - 3D 音频可视化器 */
    object DynamicGrid : DynamicCoverType(
        "dynamic_grid",
        "动态方格",
    )

    /** 闪耀星光 - 分形 + 调色板音频可视化器 */
    object ShiningStars : DynamicCoverType(
        "shining_stars",
        "闪耀星光",
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
                DynamicGrid.value -> DynamicGrid
                ShiningStars.value -> ShiningStars
                OFF.value -> OFF
                else -> DynamicGrid
            }

        val presets: List<DynamicCoverType>
            get() = listOf(DynamicGrid, ShiningStars, OFF)
    }
}
