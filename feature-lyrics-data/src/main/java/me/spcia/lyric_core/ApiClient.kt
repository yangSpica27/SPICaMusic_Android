package me.spcia.lyric_core

import android.content.Context
import com.skydoves.sandwich.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import retrofit2.Retrofit
import timber.log.Timber

/**
 * 歌词API客户端
 * 通过新的歌词API获取歌曲信息和歌词
 */
class ApiClient(
    private val context: Context,
    retrofit: Retrofit
) {

    private val api: LyricsApi by lazy {
        retrofit.create(LyricsApi::class.java)
    }

    /**
     * 根据歌曲名和艺术家获取歌词信息
     * @param displayName 歌曲标题
     * @param artist 艺术家（可选，暂未使用）
     */
    suspend fun fetchExtInfo(displayName: String, artist: String?) = withContext(Dispatchers.IO) {
        Timber.tag("ApiClient").d("搜索歌词: $displayName - $artist")
        
        try {
            // 调用新的歌词API
            val response = api.searchLyrics(displayName).getOrNull()
            
            if (response == null || response.code != 200) {
                Timber.tag("ApiClient").w("API请求失败: ${response?.message}")
                return@withContext null
            }
            
            if (response.data.isNullOrEmpty()) {
                Timber.tag("ApiClient").w("未找到歌词数据")
                return@withContext null
            }
            
            // 获取第一个匹配结果
            val songLyrics = response.data.first()
            Timber.tag("ApiClient").d("✅ 找到歌曲: ${songLyrics.name} - ${songLyrics.artist}")
            
            // 构建返回实体
            val extraInfoEntity = ExtraInfoEntity(
                lyrics = songLyrics.lyrics,
                cover = songLyrics.albumArt
            )
            
            Timber.tag("ApiClient").d("歌词长度: ${songLyrics.lyrics.length}")
            
            return@withContext extraInfoEntity
            
        } catch (e: Exception) {
            Timber.tag("ApiClient").e(e, "获取歌词失败")
            return@withContext null
        }
    }

    /**
     * 搜索歌词并返回所有匹配结果
     * @param displayName 歌曲标题
     * @return 所有匹配的歌词结果列表，失败时返回空列表
     */
    suspend fun searchAllLyrics(displayName: String): List<SongLyrics> = withContext(Dispatchers.IO) {
        Timber.tag("ApiClient").d("搜索所有歌词: $displayName")

        try {
            val response = api.searchLyrics(displayName).getOrNull()

            if (response == null || response.code != 200) {
                Timber.tag("ApiClient").w("API请求失败: ${response?.message}")
                return@withContext emptyList()
            }

            val results = response.data ?: emptyList()
            Timber.tag("ApiClient").d("找到 ${results.size} 个歌词结果")
            return@withContext results
        } catch (e: Exception) {
            Timber.tag("ApiClient").e(e, "搜索歌词失败")
            return@withContext emptyList()
        }
    }
}
