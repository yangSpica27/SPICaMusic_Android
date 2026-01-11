package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.PlayHistory

/**
 * 播放历史仓库接口
 * 提供播放历史记录的存储和查询操作
 */
interface IPlayHistoryRepository {
    /**
     * 获取所有播放历史 Flow
     */
    fun getAllPlayHistoryFlow(): Flow<List<PlayHistory>>

    /**
     * 获取最近播放的歌曲列表
     * @param limit 限制数量
     */
    fun getRecentPlayHistoryFlow(limit: Int = 50): Flow<List<PlayHistory>>

    /**
     * 添加播放记录
     */
    suspend fun addPlayHistory(songId: Long)

    /**
     * 清空播放历史
     */
    suspend fun clearPlayHistory()

    /**
     * 删除指定歌曲的播放历史
     */
    suspend fun deletePlayHistory(songId: Long)


  fun insertPlayHistory(item: PlayHistory)
}
