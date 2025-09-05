package me.spica27.spicamusic.media.action

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

object MediaControl {
    /**
     * 替换播放列表，并播放目标歌曲
     */
    @OptIn(UnstableApi::class)
    fun playWithList(
        mediaIds: List<String>,
        mediaId: String,
        start: Boolean = true,
    ) {
        PlayerAction
            .UpdateList(mediaIds, mediaId, start)
            .action()
    }
}
