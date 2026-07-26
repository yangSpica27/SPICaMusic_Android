package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable

/**
 * 主题色风格。
 */
@Immutable
sealed class ThemeColorStyle(
    val value: String,
    val name: String,
) {
    object Textured : ThemeColorStyle(
        "textured",
        "质感化",
    )

    object Flat : ThemeColorStyle(
        "flat",
        "扁平化",
    )

    override fun toString(): String = name

    companion object {
        fun fromString(value: String): ThemeColorStyle =
            when (value) {
                Textured.value -> Textured
                Flat.value -> Flat
                else -> Textured
            }

        val presets: List<ThemeColorStyle>
            get() = listOf(Textured, Flat)
    }
}
