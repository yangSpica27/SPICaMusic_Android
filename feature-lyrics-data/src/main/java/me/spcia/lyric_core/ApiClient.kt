package me.spcia.lyric_core

import android.content.Context
import com.skydoves.sandwich.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.entity.SongLyrics
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
