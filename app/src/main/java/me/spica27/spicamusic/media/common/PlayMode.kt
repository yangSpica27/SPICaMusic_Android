package me.spica27.spicamusic.media.common

import androidx.media3.common.Player

/**
 * 循环模式
 */
sealed class PlayMode(
    val name: String,
) {
    /**
     * 列表循环
     */
    object LOOP : PlayMode("LOOP")

    /**
     * 单循环
     */
    object LIST : PlayMode("LIST")

    /**
     * 随机模式
     */
    object SHUFFLE : PlayMode("SHUFFLE")

    companion object {
        fun of(
            repeatMode: Int,
            shuffleModeEnabled: Boolean,
        ): PlayMode {
            if (shuffleModeEnabled) return SHUFFLE
            if (repeatMode == Player.REPEAT_MODE_ONE) return LIST
            return LOOP
        }

        fun from(string: String?): PlayMode =
            when (string) {
                LOOP.name -> LOOP
                LIST.name -> LIST
                SHUFFLE.name -> SHUFFLE
                else -> LOOP
            }
    }
}

var Player.playMode
    get() = PlayMode.of(repeatMode, shuffleModeEnabled)
    set(value) {
        shuffleModeEnabled = value == PlayMode.SHUFFLE
        repeatMode =
            if (value == PlayMode.LIST) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_ALL
            }
    }
