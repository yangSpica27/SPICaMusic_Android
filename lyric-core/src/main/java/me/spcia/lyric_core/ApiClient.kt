package me.spcia.lyric_core

import android.content.Context
import com.skydoves.sandwich.getOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import timber.log.Timber

class ApiClient(
  private val context: Context,
  retrofit: Retrofit,
  okHttpClient: OkHttpClient
) {

  val api: Api by lazy {
    retrofit
      .create(Api::class.java)
  }
  
  // EAPI 客户端，用于获取逐字歌词
  private val eapiClient: NetEaseEAPIClient by lazy {
    NetEaseEAPIClient(context, okHttpClient)
  }

  suspend fun fetchExtInfo(displayName: String, artist: String?) = withContext(Dispatchers.IO) {
    Timber.tag("ApiClient").d("Fetching extra info for: $displayName - $artist")
    val extraInfoEntity = ExtraInfoEntity()
    
    try {
      // 1. 先使用普通API搜索歌曲
      val searchResponse = api.fetchMusicInfo("${displayName}").getOrThrow()
      Timber.tag("ApiClient").d("Search Response: $searchResponse")
      
      if (searchResponse.code != 200) {
        return@withContext null
      }
      
      // 2. 提取封面
      extraInfoEntity.cover = searchResponse.result.songs.firstOrNull { it.name == displayName }
        ?.album?.artist?.picUrl ?: ""
      
      // 3. 获取歌曲ID
      val songId =
        searchResponse.result.songs.firstOrNull { it.name == displayName }?.id
          ?: searchResponse.result.songs.firstOrNull()?.id
          ?: return@withContext extraInfoEntity
      
      // 4. 尝试使用 EAPI 获取逐字歌词
      val eapiLyricResponse = try {
        eapiClient.getLyric(songId)
      } catch (e: Exception) {
        Timber.tag("ApiClient").w(e, "EAPI 获取歌词失败，降级到普通API")
        null
      }
      
      if (eapiLyricResponse != null && eapiLyricResponse.code == 200) {
        Timber.tag("ApiClient").d("EAPI Lyric Response: $eapiLyricResponse")
        
        // 优先使用逐字歌词（YRC）
        if (!eapiLyricResponse.yrc?.lyric.isNullOrBlank()) {
          Timber.tag("ApiClient").d("✅ 成功获取逐字歌词 (YRC)")
          val yrcLines = YrcParser.parse(eapiLyricResponse.yrc!!.lyric)
          // 转换为标准 LRC 格式存储
          extraInfoEntity.lyrics = YrcParser.toLrc(yrcLines)
        } 
        // 其次使用普通歌词
        else if (!eapiLyricResponse.lrc?.lyric.isNullOrBlank()) {
          Timber.tag("ApiClient").d("使用普通歌词 (LRC)")
          extraInfoEntity.lyrics = eapiLyricResponse.lrc!!.lyric
        }
        // 检查翻译字段
        else if (!eapiLyricResponse.tlyric?.lyric.isNullOrBlank()) {
          Timber.tag("ApiClient").d("使用翻译字段歌词 (tlyric)")
          extraInfoEntity.lyrics = eapiLyricResponse.tlyric!!.lyric
        }
        // 检查罗马音字段
        else if (!eapiLyricResponse.romalrc?.lyric.isNullOrBlank()) {
          Timber.tag("ApiClient").d("使用罗马音字段歌词 (romalrc)")
          extraInfoEntity.lyrics = eapiLyricResponse.romalrc!!.lyric
        }
      } else {
        // 5. 降级：使用普通API获取歌词
        Timber.tag("ApiClient").d("降级到普通API获取歌词")
        val lyricResponse = api.fetchLyric(songId).getOrThrow()
        Timber.tag("ApiClient").d("普通API歌词响应: code=${lyricResponse}")
        
        // 尝试所有可能的歌词字段
        val lyrics = when {
          !lyricResponse.lrc?.lyric.isNullOrBlank() -> {
            Timber.tag("ApiClient").d("✅ 从 lrc 字段获取到歌词")
            lyricResponse.lrc!!.lyric
          }
          !lyricResponse.tlyric?.lyric.isNullOrBlank() -> {
            Timber.tag("ApiClient").d("✅ 从 tlyric 字段获取到歌词")
            lyricResponse.tlyric!!.lyric
          }
          !lyricResponse.romalrc?.lyric.isNullOrBlank() -> {
            Timber.tag("ApiClient").d("✅ 从 romalrc 字段获取到歌词")
            lyricResponse.romalrc!!.lyric
          }
          else -> {
            Timber.tag("ApiClient").w("⚠️ 普通API所有字段均未返回歌词内容")
            null
          }
        }
        
        lyrics?.let {
          Timber.tag("ApiClient").d("歌词长度: ${it.length}, 前200字符: ${it.take(200)}")
          extraInfoEntity.lyrics = it
        }
      }
      
    } catch (e: Exception) {
      Timber.tag("ApiClient").e(e, "获取歌曲信息失败")
    }
    
    return@withContext extraInfoEntity
  }


}