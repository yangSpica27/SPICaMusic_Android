package me.spica27.spicamusic.network

import com.skydoves.sandwich.ApiResponse
import me.spica27.spicamusic.network.bean.LyricResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LyricApi {


  /**
   * 获取歌词
   */
  @GET("advance")
  suspend fun fetchLyric(
    @Query("title") title: String,
    @Query("artist") artist: String?
  ): ApiResponse<List<LyricResponse>>

}