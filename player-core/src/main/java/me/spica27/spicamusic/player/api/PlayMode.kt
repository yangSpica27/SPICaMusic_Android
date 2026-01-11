package me.spica27.spicamusic.player.api

/**
 * 播放模式
 */
sealed class PlayMode(val name: String) {
    /**
     * 列表循环
     */
    data object LOOP : PlayMode("LOOP")

    /**
     * 单曲循环
     */
    data object LIST : PlayMode("LIST")

    /**
     * 随机播放
     */
    data object SHUFFLE : PlayMode("SHUFFLE")

    companion object {
        fun from(string: String?): PlayMode =
            when (string) {
                LOOP.name -> LOOP
                LIST.name -> LIST
                SHUFFLE.name -> SHUFFLE
                else -> LOOP
            }
    }
}
