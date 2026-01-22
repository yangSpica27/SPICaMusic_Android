package me.spcia.lyric_core


import com.skydoves.sandwich.ApiResponse
import me.spcia.lyric_core.entity.LyricResponse
import me.spcia.lyric_core.entity.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface Api {


  @Headers(
    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.64",
    "Referer: https://music.163.com/"
  )
  @GET("https://music.163.com/api/search/get/?type=1&limit=2")
  suspend fun fetchMusicInfo(@Query("s") songName: String): ApiResponse<SearchResponse>


  @Headers(
    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.64",
    "Referer: https://music.163.com/"
  )
  @GET("https://music.163.com/api/song/lyric?os=pc&tv=-1")
  suspend fun fetchLyric(@Query("id") songId: Long): ApiResponse<LyricResponse>

}