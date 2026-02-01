package me.spcia.lyric_core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.crypto.NetEaseEAPIEncryption
import me.spcia.lyric_core.entity.LyricResponse
import me.spcia.lyric_core.entity.SearchResponse
import me.spcia.lyric_core.session.NetEaseSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

/**
 *  EAPI 客户端
 * 
 * 使用加密的 EAPI 接口获取逐字歌词等高级功能
 */
class NetEaseEAPIClient(
    context: Context,
    private val okHttpClient: OkHttpClient
) {
    
    private val session = NetEaseSession(context)
    
    /**
     * 搜索歌曲（EAPI）
     */
    suspend fun searchSong(keyword: String, limit: Int = 2): SearchResponse? = withContext(Dispatchers.IO) {
        try {
            val sessionInfo = session.getSession()
            
            val path = "/eapi/search/song/list/page"
            val params = mutableMapOf<String, Any>(
                "keyword" to keyword,
                "scene" to "NORMAL",
                "needCorrect" to "true",
                "limit" to limit.toString(),
                "offset" to "0",
                "e_r" to true,
                "header" to sessionInfo.getParamsHeader()
            )
            
            val encryptedParams = NetEaseEAPIEncryption.encryptParams(
                path.replace("/eapi", "/api"),
                params
            )
            
            val request = Request.Builder()
                .url("https://interface.music.163.com$path")
                .post(encryptedParams.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Cookie", sessionInfo.getCookieString())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.tag("NetEaseEAPIClient").e("搜索失败: HTTP ${response.code}")
                return@withContext null
            }
            
            val decrypted = NetEaseEAPIEncryption.decryptResponse(response.body!!.bytes())
            val json = JSONObject(decrypted)
            
            if (json.getInt("code") != 200) {
                Timber.tag("NetEaseEAPIClient").e("搜索失败: ${json.optString("message")}")
                return@withContext null
            }
            
            // 转换为 SearchResponse (简化版，只提取必要字段)
            val dataJson = json.optJSONObject("data")
            val resourcesArray = dataJson?.optJSONArray("resources") ?: return@withContext null
            
            val songs = mutableListOf<SearchResponse.Result.Song>()
            for (i in 0 until resourcesArray.length()) {
                val resource = resourcesArray.getJSONObject(i)
                val baseInfo = resource.optJSONObject("baseInfo") ?: continue
                val simpleSongData = baseInfo.optJSONObject("simpleSongData") ?: continue
                
                // 这里需要根据实际的 JSON 结构进行映射
                // 暂时返回 null，后续完善
            }
            
            null // 暂时返回 null，需要完整的 JSON 映射
            
        } catch (e: Exception) {
            Timber.tag("NetEaseEAPIClient").e(e, "搜索异常")
            null
        }
    }
    
    /**
     * 获取歌词（EAPI，支持逐字歌词）
     */
    suspend fun getLyric(songId: Long): LyricResponse? = withContext(Dispatchers.IO) {
        try {
            val sessionInfo = session.getSession()
            
            val path = "/eapi/song/lyric/v1"
            val params = mutableMapOf<String, Any>(
                "id" to songId.toInt(),
                "lv" to "-1",     // 普通歌词版本
                "tv" to "-1",     // 翻译版本  
                "rv" to "-1",     // 罗马音版本
                "yv" to "-1",     // 逐字歌词版本（关键！）
                "e_r" to true,
                "header" to sessionInfo.getParamsHeader()
            )
            
            val encryptedParams = NetEaseEAPIEncryption.encryptParams(
                path.replace("/eapi", "/api"),
                params
            )
            
            val request = Request.Builder()
                .url("https://interface.music.163.com$path")
                .post(encryptedParams.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .header("Cookie", sessionInfo.getCookieString())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Timber.tag("NetEaseEAPIClient").e("获取歌词失败: HTTP ${response.code}")
                return@withContext null
            }
            
            val decrypted = NetEaseEAPIEncryption.decryptResponse(response.body!!.bytes())
            Timber.tag("NetEaseEAPIClient").d("歌词响应: $decrypted")
            
            val json = JSONObject(decrypted)
            
            if (json.getInt("code") != 200) {
                Timber.tag("NetEaseEAPIClient").e("获取歌词失败: ${json.optString("message")}")
                return@withContext null
            }
            
            // 手动构造 LyricResponse
            val lrcObj = json.optJSONObject("lrc")
            val yrcObj = json.optJSONObject("yrc")
            val tlyricObj = json.optJSONObject("tlyric")
            val romalrcObj = json.optJSONObject("romalrc")
            val lyricUserObj = json.optJSONObject("lyricUser")
            val transUserObj = json.optJSONObject("transUser")
            
            LyricResponse(
                code = json.getInt("code"),
                lrc = lrcObj?.let {
                    LyricResponse.Lrc(
                        lyric = it.optString("lyric", ""),
                        version = it.optInt("version", 0)
                    )
                },
                yrc = yrcObj?.let {
                    LyricResponse.Yrc(
                        lyric = it.optString("lyric", ""),
                        version = it.optInt("version", 0)
                    )
                },
                tlyric = tlyricObj?.let {
                    LyricResponse.Lrc(
                        lyric = it.optString("lyric", ""),
                        version = it.optInt("version", 0)
                    )
                },
                romalrc = romalrcObj?.let {
                    LyricResponse.Lrc(
                        lyric = it.optString("lyric", ""),
                        version = it.optInt("version", 0)
                    )
                },
                lyricUser = lyricUserObj?.let {
                    LyricResponse.LyricUser(
                        demand = it.optInt("demand", 0),
                        id = it.optInt("id", 0),
                        nickname = it.optString("nickname", ""),
                        status = it.optInt("status", 0),
                        uptime = it.optLong("uptime", 0),
                        userid = it.optInt("userid", 0)
                    )
                },
                qfy = json.optBoolean("qfy", false),
                sfy = json.optBoolean("sfy", false),
                sgc = json.optBoolean("sgc", false),
                transUser = transUserObj?.let {
                    LyricResponse.TransUser(
                        demand = it.optInt("demand", 0),
                        id = it.optInt("id", 0),
                        nickname = it.optString("nickname", ""),
                        status = it.optInt("status", 0),
                        uptime = it.optLong("uptime", 0),
                        userid = it.optInt("userid", 0)
                    )
                }
            )
            
        } catch (e: Exception) {
            Timber.tag("NetEaseEAPIClient").e(e, "获取歌词异常")
            null
        }
    }
}
