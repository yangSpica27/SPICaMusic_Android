package me.spica27.spicamusic.network

import com.skydoves.sandwich.ApiResponse
import me.spica27.spicamusic.network.bean.LrcLibLyric
import me.spica27.spicamusic.network.bean.LyricResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface LyricApi {


  /**
   * 获取歌词
   */
  @Headers("User-Agent: SPICaMusic_Android betaVersion (https://github.com/yangSpica27/SPICaMusic_Android)")
  @GET("advance")
  suspend fun fetchLyric(
    @Query("title") title: String,
    @Query("artist") artist: String?
  ): ApiResponse<List<LyricResponse>>


  @GET("https://lrclib.net/api/search")
  suspend fun fetchLyric2(
    @Query("q") title: String,
    @Query("artist_name")
    artist: String?
  ): ApiResponse<List<LrcLibLyric>>

}