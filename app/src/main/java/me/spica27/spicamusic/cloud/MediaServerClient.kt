package me.spica27.spicamusic.cloud

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MediaServerClient(
    baseClient: OkHttpClient,
) {
    private val client =
        baseClient
            .newBuilder()
            .apply {
                interceptors().removeAll { it is HttpLoggingInterceptor }
                networkInterceptors().removeAll { it is HttpLoggingInterceptor }
            }.connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    suspend fun authenticate(
        type: MediaServerType,
        serverUrl: String,
        username: String,
        password: String,
    ): Result<MediaServerAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeAndValidateUrl(serverUrl)
                val requestBody =
                    JSONObject()
                        .put("Username", username.trim())
                        .put("Pw", password)
                        .toString()
                        .toRequestBody(JSON_MEDIA_TYPE)
                val request =
                    Request
                        .Builder()
                        .url("$normalized/Users/AuthenticateByName")
                        .header("Authorization", authorizationHeader(type))
                        .header("X-Emby-Authorization", authorizationHeader(type))
                        .post(requestBody)
                        .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code}: ${response.message}")
                    }
                    val json = JSONObject(response.body.string())
                    val token = json.optString("AccessToken")
                    val userId = json.optJSONObject("User")?.optString("Id").orEmpty()
                    if (token.isBlank() || userId.isBlank()) error("服务器未返回有效的登录令牌")
                    MediaServerAccount(
                        id = "",
                        type = type,
                        displayName = Uri.parse(normalized).host ?: type.name,
                        serverUrl = normalized,
                        username = username.trim(),
                        userId = userId,
                        accessToken = token,
                    )
                }
            }
        }

    suspend fun getSongs(
        account: MediaServerAccount,
        startIndex: Int,
        limit: Int = PAGE_SIZE,
        searchTerm: String = "",
    ): Result<CloudSongPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    "${account.normalizedServerUrl}/Users/${account.userId}/Items"
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("IncludeItemTypes", "Audio")
                        .addQueryParameter("Recursive", "true")
                        .addQueryParameter("Fields", "MediaSources,Genres,Path")
                        .addQueryParameter("StartIndex", startIndex.toString())
                        .addQueryParameter("Limit", limit.toString())
                        .addQueryParameter("SortBy", "SortName")
                        .addQueryParameter("SortOrder", "Ascending")
                        .addQueryParameter("EnableImages", "true")
                        .addQueryParameter("ImageTypeLimit", "1")
                        .apply {
                            if (searchTerm.isNotBlank()) {
                                addQueryParameter("SearchTerm", searchTerm.trim())
                            }
                        }.build()
                val json = executeJson(account, url.toString())
                val items = json.optJSONArray("Items")
                val songs =
                    buildList {
                        for (index in 0 until (items?.length() ?: 0)) {
                            val item = items?.optJSONObject(index) ?: continue
                            val id = item.optString("Id")
                            if (id.isBlank()) continue
                            val mediaSource =
                                item
                                    .optJSONArray("MediaSources")
                                    ?.optJSONObject(0)
                            val container =
                                mediaSource
                                    ?.optString("Container")
                                    .orEmpty()
                                    .substringBefore(',')
                                    .lowercase()
                            add(
                                CloudSong(
                                    id = id,
                                    title = item.optString("Name").ifBlank { "Unknown title" },
                                    artist =
                                        item
                                            .optJSONArray("Artists")
                                            ?.let { artists ->
                                                buildList {
                                                    for (artistIndex in 0 until artists.length()) {
                                                        artists.optString(artistIndex).takeIf(String::isNotBlank)?.let(::add)
                                                    }
                                                }.joinToString(", ")
                                            }.orEmpty()
                                            .ifBlank { item.optString("AlbumArtist") }
                                            .ifBlank { "Unknown artist" },
                                    album = item.optString("Album").ifBlank { "Unknown album" },
                                    durationMs = item.optLong("RunTimeTicks").coerceAtLeast(0L) / 10_000L,
                                    mimeType = mimeTypeFor(container),
                                    imageItemId =
                                        if (
                                            item
                                                .optJSONObject("ImageTags")
                                                ?.optString("Primary")
                                                .orEmpty()
                                                .isNotBlank()
                                        ) {
                                            id
                                        } else {
                                            item.optString("AlbumId").takeIf(String::isNotBlank)
                                        },
                                ),
                            )
                        }
                    }
                val total = json.optInt("TotalRecordCount", startIndex + songs.size)
                val next =
                    (startIndex + songs.size)
                        .takeIf { songs.isNotEmpty() && it < total }
                CloudSongPage(songs, total, next)
            }
        }

    fun streamUrl(
        account: MediaServerAccount,
        itemId: String,
    ): String =
        "${account.normalizedServerUrl}/Audio/${Uri.encode(itemId)}/stream"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("static", "true")
            .addQueryParameter("api_key", account.accessToken)
            .build()
            .toString()

    fun imageUrl(
        account: MediaServerAccount,
        itemId: String,
        maxWidth: Int = 420,
    ): String =
        "${account.normalizedServerUrl}/Items/${Uri.encode(itemId)}/Images/Primary"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("maxWidth", maxWidth.toString())
            .addQueryParameter("quality", "88")
            .addQueryParameter("api_key", account.accessToken)
            .build()
            .toString()

    private fun executeJson(
        account: MediaServerAccount,
        url: String,
    ): JSONObject {
        val request =
            Request
                .Builder()
                .url(url)
                .header("Authorization", authorizationHeader(account.type, account.accessToken))
                .header("X-Emby-Authorization", authorizationHeader(account.type, account.accessToken))
                .header("X-MediaBrowser-Token", account.accessToken)
                .get()
                .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
            JSONObject(response.body.string())
        }
    }

    private fun authorizationHeader(
        type: MediaServerType,
        token: String? = null,
    ): String {
        val clientName = if (type == MediaServerType.JELLYFIN) "SPICa Music" else "SPICa Music"
        val tokenPart = token?.let { ", Token=\"$it\"" }.orEmpty()
        return "MediaBrowser Client=\"$clientName\", Device=\"Android\", " +
            "DeviceId=\"$DEVICE_ID\", Version=\"1.0\"$tokenPart"
    }

    private fun normalizeAndValidateUrl(value: String): String {
        val normalized = value.trim().trimEnd('/')
        val url = normalized.toHttpUrl()
        require(url.scheme == "http" || url.scheme == "https") { "服务器地址必须以 http:// 或 https:// 开头" }
        return url.toString().trimEnd('/')
    }

    private fun mimeTypeFor(container: String): String =
        when (container) {
            "flac" -> "audio/flac"
            "m4a", "mp4" -> "audio/mp4"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            else -> "audio/mpeg"
        }

    companion object {
        const val PAGE_SIZE = 80
        private const val DEVICE_ID = "spica-music-android"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
