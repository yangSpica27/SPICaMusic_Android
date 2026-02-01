package me.spcia.lyric_core.session

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.spcia.lyric_core.crypto.NetEaseEAPIEncryption
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import timber.log.Timber
import java.security.SecureRandom
import java.util.*

/**
 * 音乐会话管理
 *
 * 负责游客登录、Cookie管理和会话刷新
 */
class NetEaseSession(private val context: Context) {

  private val mutex = Mutex()
  private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    .followRedirects(false)
    .build()

  // 会话数据
  private var cookies: Map<String, String> = emptyMap()
  private var userId: Long = 0
  private var expireTime: Long = 0

  val isValid: Boolean
    get() = cookies.isNotEmpty() && System.currentTimeMillis() < expireTime

  /**
   * 获取当前会话（自动刷新）
   */
  suspend fun getSession(): SessionInfo = mutex.withLock {
    if (!isValid) {
      performGuestLogin()
    }
    SessionInfo(cookies, userId, expireTime)
  }

  /**
   * 执行游客登录
   */
  private suspend fun performGuestLogin() {
    try {
      Timber.tag("NetEaseSession").d("开始网易云游客登录")

      // 1. 生成设备信息
      val deviceId = generateDeviceId()
      val clientSign = generateClientSign()
      val osVersion = "Microsoft-Windows-10--build-${(200..300).random()}00-64bit"
      val deviceModel = listOf(
        "MS-iCraft B760M WIFI",
        "ASUS ROG STRIX Z790",
        "MSI MAG B550 TOMAHAWK",
        "ASRock X670E Taichi"
      ).random()

      // 2. 构造预登录Cookies
      val preCookies = mapOf(
        "os" to "pc",
        "deviceId" to deviceId,
        "osver" to osVersion,
        "clientSign" to clientSign,
        "channel" to "netease",
        "mode" to deviceModel,
        "appver" to "3.1.3.203419"
      )

      // 3. 构造登录参数
      val path = "/eapi/register/anonimous"
      val params = mapOf(
        "username" to NetEaseEAPIEncryption.getAnonymousUsername(deviceId),
        "e_r" to true,
        "header" to buildParamsHeader(preCookies)
      )

      // 4. 加密参数
      val encryptedParams = NetEaseEAPIEncryption.encryptParams(
        path.replace("/eapi", "/api"),
        params
      )

      // 5. 发送请求
      val request = Request.Builder()
        .url("https://interface.music.163.com$path")
        .post(encryptedParams.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .headers(buildHeaders(preCookies).toHeaders())
        .build()

      val response = client.newCall(request).execute()

      if (!response.isSuccessful) {
        throw IllegalStateException("游客登录失败: HTTP ${response.code}")
      }

      // 6. 解密响应
      val decrypted = NetEaseEAPIEncryption.decryptResponse(response.body!!.bytes())
      val jsonData = JSONObject(decrypted)

      Timber.tag("NetEaseSession").d("游客登录响应码: ${jsonData.getInt("code")}")

      if (jsonData.getInt("code") != 200) {
        throw IllegalStateException("游客登录失败: ${jsonData.getString("message")}")
      }

      // 7. 提取响应Cookies
      val responseCookies = response.headers.values("Set-Cookie")
        .associate { cookie ->
          val parts = cookie.split(";")[0].split("=", limit = 2)
          parts[0] to parts.getOrNull(1).orEmpty()
        }

      // 8. 合并Cookies
      val wnmcid = buildString {
        repeat(6) { append(('a'..'z').random()) }
        append(".")
        append(System.currentTimeMillis() - (1000..10000).random())
        append(".01.0")
      }

      cookies = mapOf(
        "WEVNSM" to "1.0.0",
        "os" to preCookies["os"]!!,
        "deviceId" to preCookies["deviceId"]!!,
        "osver" to preCookies["osver"]!!,
        "clientSign" to preCookies["clientSign"]!!,
        "channel" to "netease",
        "mode" to preCookies["mode"]!!,
        "NMTID" to responseCookies.getOrDefault("NMTID", ""),
        "MUSIC_A" to responseCookies.getOrDefault("MUSIC_A", ""),
        "__csrf" to responseCookies.getOrDefault("__csrf", ""),
        "appver" to preCookies["appver"]!!,
        "WNMCID" to wnmcid
      ).filterValues { it.isNotEmpty() }

      userId = jsonData.getLong("userId")
      expireTime = System.currentTimeMillis() + 864000000 // 10天

      Timber.tag("NetEaseSession").d("游客登录成功，userId=$userId")

    } catch (e: Exception) {
      Timber.tag("NetEaseSession").e(e, "游客登录失败")
      throw e
    }
  }

  /**
   * 生成设备ID
   */
  private fun generateDeviceId(): String {
    // 生成格式类似: 123456789ABCDEF123456789ABCDEF12
    return UUID.randomUUID().toString().replace("-", "").uppercase()
  }

  /**
   * 生成客户端签名
   */
  private fun generateClientSign(): String {
    val random = SecureRandom()

    // MAC地址部分
    val mac = (0..5).joinToString(":") { "%02X".format(random.nextInt(256)) }

    // 随机大写字母
    val randomStr = (0..7).map { ('A'..'Z').random() }.joinToString("")

    // 哈希部分
    val hashPart = (0..31).map { "%02x".format(random.nextInt(256)) }.joinToString("")

    return "$mac@@@$randomStr@@@@@@$hashPart"
  }

  /**
   * 构建参数头部JSON
   */
  private fun buildParamsHeader(cookies: Map<String, String>): String {
    val json = JSONObject().apply {
      put("clientSign", cookies["clientSign"])
      put("os", cookies["os"])
      put("appver", cookies["appver"])
      put("deviceId", cookies["deviceId"])
      put("requestId", 0)
      put("osver", cookies["osver"])
    }
    return json.toString()
  }

  /**
   * 构建请求头
   */
  private fun buildHeaders(cookies: Map<String, String>): Map<String, String> {
    return mapOf(
      "accept" to "*/*",
      "content-type" to "application/x-www-form-urlencoded",
      "cookie" to cookies.entries.joinToString("; ") { "${it.key}=${it.value}" },
      "mconfig-info" to """{"IuRPVVmc3WWul9fT":{"version":733184,"appver":"3.1.3.203419"}}""",
      "origin" to "orpheus://orpheus",
      "user-agent" to "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419",
      "sec-ch-ua" to """"Chromium";v="91"""",
      "sec-ch-ua-mobile" to "?0",
      "sec-fetch-site" to "cross-site",
      "sec-fetch-mode" to "cors",
      "sec-fetch-dest" to "empty",
      "accept-encoding" to "gzip, deflate, br",
      "accept-language" to "en-US,en;q=0.9"
    )
  }

  private fun Map<String, String>.toHeaders(): okhttp3.Headers {
    return okhttp3.Headers.Builder().apply {
      forEach { (key, value) -> add(key, value) }
    }.build()
  }

  /**
   * 会话信息
   */
  data class SessionInfo(
    val cookies: Map<String, String>,
    val userId: Long,
    val expireTime: Long
  ) {
    fun getCookieString(): String {
      return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    fun getParamsHeader(): String {
      val json = JSONObject().apply {
        put("clientSign", cookies["clientSign"])
        put("os", cookies["os"])
        put("appver", cookies["appver"])
        put("deviceId", cookies["deviceId"])
        put("requestId", 0)
        put("osver", cookies["osver"])
      }
      return json.toString()
    }
  }
}
