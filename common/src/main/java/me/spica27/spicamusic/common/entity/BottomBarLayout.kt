package me.spica27.spicamusic.common.entity

/** 首页底栏布局，未知的持久化值回退到传统样式。 */
enum class BottomBarLayout(val value: String) {
    Traditional("traditional"),
    Floating("floating"),
    ;

    companion object {
        fun fromString(value: String): BottomBarLayout = entries.firstOrNull { it.value == value } ?: Traditional
    }
}
