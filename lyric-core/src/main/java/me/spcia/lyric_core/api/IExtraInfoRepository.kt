package me.spcia.lyric_core.api

import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Lyric

/**
 * 歌词仓库接口
 * 提供歌词数据的存储和查询操作
 */
interface IExtraInfoRepository {
    /**
     * 根据歌曲ID获取歌词 Flow
     */
    fun getLyricFlowBySongId(songId: Long): Flow<Lyric?>

    /**
     * 保存或更新歌词
     */
    suspend fun saveLyric(lyric: Lyric)

    /**
     * 删除歌词
     */
    suspend fun deleteLyric(songId: Long)

    /**
     * 批量保存歌词
     */
    suspend fun saveLyrics(lyrics: List<Lyric>)
}