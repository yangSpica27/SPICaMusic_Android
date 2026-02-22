package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.TopSong

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
     * 添加播放记录（只指定 songId 的便捷方法）
     */
    suspend fun addPlayHistory(songId: Long)

    /**
     * 添加播放记录（完整对象）
     */
    suspend fun addPlayHistory(item: PlayHistory)

    /**
     * 清空播放历史
     */
    suspend fun clearPlayHistory()

    /**
     * 删除指定歌曲的播放历史
     */
    suspend fun deletePlayHistory(songId: Long)

    /**
     * 同步插入历史（同步包装，尽量使用 suspend 版本）
     */
    fun insertPlayHistory(item: PlayHistory)

    /**
     * 本周统计：时长、播放次数、去重歌曲数
     */
    suspend fun getWeeklyStats(): PlayStats

    /**
     * 全部时间统计：时长、播放次数、去重歌曲数
     */
    suspend fun getAllTimeStats(): PlayStats

    /**
     * 时长最长的歌曲（按时长排序）
     */
    suspend fun getTopSongsByDuration(limit: Int = 10): List<TopSong>
}
