package me.spcia.lyric_core

import com.skydoves.sandwich.ApiResponse
import me.spcia.lyric_core.entity.LyricsApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 歌词API接口
 */
interface LyricsApi {

    /**
     * 根据歌曲标题搜索歌词
     * @param title 歌曲标题
     */
    @GET("http://106.54.25.152:4141/api/lyrics")
    suspend fun searchLyrics(@Query("title") title: String): ApiResponse<LyricsApiResponse>
}
