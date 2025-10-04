package me.spica27.spicamusic.common

sealed class VisaulizerMode(
    val value: Int,
    val name: String,
) {
    // 圆形模式
    data object CIRCLE : VisaulizerMode(0, "RING MODE")

    // 底部线条模式
    data object BOTTOM : VisaulizerMode(1, "BOTTOM LINE MODE")
}
