package me.spica27.spicamusic.player.impl

/**
 * 过滤过期的播放进度采样，同时保留明确的跳转操作。
 *
 * 媒体控制器读取的位置可能暂时落后于上一次读取，或者在播放器重连时读取失败。
 * 歌词渲染器不能把这两种情况当成真实的跳转，否则逐字高亮和歌词滚动都会回退。
 * 播放器会在位置不连续回调中通过 [resetTo] 处理明确的跳转，普通播放采样则通过
 * [sample] 处理。
 */
internal class PlaybackPositionSmoother {
    private val lock = Any()
    private var currentMediaId: String? = null
    private var lastPositionMs = 0L
    private var acceptNextSample = false

    fun sample(mediaId: String?, rawPositionMs: Long): Long = synchronized(lock) {
        val positionMs = rawPositionMs.coerceAtLeast(0L)
        if (acceptNextSample || mediaId != currentMediaId) {
            currentMediaId = mediaId
            lastPositionMs = positionMs
            acceptNextSample = false
        } else if (positionMs >= lastPositionMs) {
            lastPositionMs = positionMs
        }
        lastPositionMs
    }

    fun resetTo(mediaId: String?, positionMs: Long) = synchronized(lock) {
        currentMediaId = mediaId
        lastPositionMs = positionMs.coerceAtLeast(0L)
        acceptNextSample = false
    }

    /** 断开连接时保持当前显示位置，重连后的首个采样重新建立可信位置。 */
    fun markDisconnected() = synchronized(lock) {
        acceptNextSample = true
    }

    fun lastPosition(mediaId: String?): Long = synchronized(lock) {
        // 媒体控制器重连期间媒体 ID 可能暂时为空。在观察到新的媒体 ID 前，
        // 保留上一次位置，避免歌词界面短暂闪回 0。
        if (mediaId == null || mediaId == currentMediaId) lastPositionMs else 0L
    }

    fun clear() = synchronized(lock) {
        currentMediaId = null
        lastPositionMs = 0L
        acceptNextSample = false
    }
}
