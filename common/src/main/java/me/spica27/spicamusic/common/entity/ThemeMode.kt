package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable

/** 应用主题模式。SYSTEM 会实时跟随 Android 深色 / 浅色模式。 */
@Immutable
enum class ThemeMode(
    val value: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    fun resolve(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

    companion object {
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
