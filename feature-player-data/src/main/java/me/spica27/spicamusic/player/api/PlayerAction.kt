package me.spica27.spicamusic.player.api

/**
 * 播放器操作基类
 */
sealed class PlayerAction {
    /**
     * 播放
     */
    data object Play : PlayerAction()

    /**
     * 暂停
     */
    data object Pause : PlayerAction()

    /**
     * 播放或暂停切换
     */
    data object PlayOrPause : PlayerAction()

    /**
     * 下一曲
     */
    data object SkipToNext : PlayerAction()

    /**
     * 上一曲
     */
    data object SkipToPrevious : PlayerAction()

    /**
     * 根据媒体ID播放
     */
    data class PlayById(val mediaId: String) : PlayerAction()

    /**
     * 跳转到指定位置
     */
    data class SeekTo(val positionMs: Long) : PlayerAction()

    /**
     * 播放完成后暂停
     */
    data class PauseWhenCompletion(val cancel: Boolean = false) : PlayerAction()

    /**
     * 设置播放模式
     */
    data class SetPlayMode(val playMode: PlayMode) : PlayerAction()

    /**
     * 添加到下一曲播放
     */
    data class AddToNext(val mediaId: String) : PlayerAction()

    /**
     * 从播放列表移除
     */
    data class RemoveWithMediaId(val mediaId: String) : PlayerAction()

    /**
     * 按索引批量移除播放列表项
     */
    data class RemoveAtIndices(
        val indices: List<Int>,
    ) : PlayerAction()

    /**
     * 在播放列表内移动一项（索引均为移动前的时间轴位置）
     */
    data class MoveItem(
        val fromIndex: Int,
        val toIndex: Int,
    ) : PlayerAction()

    /**
     * 按时间轴索引播放（重复歌曲也能精确命中）
     */
    data class PlayAtIndex(
        val index: Int,
    ) : PlayerAction()

    /**
     * 添加到队列末尾
     */
    data class AddToQueue(
        val mediaIds: List<String>,
    ): PlayerAction()


    /**
     * 更新播放列表
     */
    data class UpdateList(
        val mediaIds: List<String>,
        val mediaId: String? = null,
        val start: Boolean = false,
    ) : PlayerAction()

    /**
     * 从头开始播放
     */
    data object ReloadAndPlay : PlayerAction()
}

internal fun normalizedRemovalIndices(
    indices: List<Int>,
    itemCount: Int,
): List<Int> =
    indices
        .asSequence()
        .distinct()
        .filter { it in 0 until itemCount }
        .sortedDescending()
        .toList()

internal fun isValidQueueMove(
    fromIndex: Int,
    toIndex: Int,
    itemCount: Int,
): Boolean = fromIndex != toIndex && fromIndex in 0 until itemCount && toIndex in 0 until itemCount
