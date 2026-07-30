package me.spica27.spicamusic.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.InflaterInputStream

class RemoteMusicClientRegistry(
    private val subsonic: SubsonicClient,
    private val netease: NeteaseClient,
    private val qqMusic: QqMusicClient,
) {
    suspend fun authenticateSubsonic(
        serverUrl: String,
        username: String,
        password: String,
    ): Result<RemoteMusicAccount> = subsonic.authenticate(serverUrl, username, password)

    suspend fun authenticateCookies(
        provider: RemoteMusicProvider,
        cookieHeader: String,
    ): Result<RemoteMusicAccount> =
        when (provider) {
            RemoteMusicProvider.NETEASE -> netease.authenticate(cookieHeader)
            RemoteMusicProvider.QQ_MUSIC -> qqMusic.authenticate(cookieHeader)
            RemoteMusicProvider.SUBSONIC ->
                Result.failure(IllegalArgumentException("Subsonic uses server credentials"))
        }

    suspend fun listSongs(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage =
        when (account.provider) {
            RemoteMusicProvider.SUBSONIC -> subsonic.listSongs(account, query, offset, limit)
            RemoteMusicProvider.NETEASE -> netease.listSongs(account, query, offset, limit)
            RemoteMusicProvider.QQ_MUSIC -> qqMusic.listSongs(account, query, offset, limit)
        }

    suspend fun resolveStreamUrl(
        account: RemoteMusicAccount,
        songId: String,
    ): String =
        when (account.provider) {
            RemoteMusicProvider.SUBSONIC -> subsonic.streamUrl(account, songId)
            RemoteMusicProvider.NETEASE -> netease.resolveStreamUrl(account, songId)
            RemoteMusicProvider.QQ_MUSIC -> qqMusic.resolveStreamUrl(account, songId)
        }

    fun clearCache(accountId: String) {
        netease.clearCache(accountId)
        qqMusic.clearCache(accountId)
    }
}

class SubsonicClient(
    baseClient: OkHttpClient,
) {
    private val client =
        baseClient
            .newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String,
    ): Result<RemoteMusicAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeServerUrl(serverUrl)
                require(username.isNotBlank()) { "Username is required" }
                require(password.isNotBlank()) { "Password is required" }
                val account =
                    RemoteMusicAccount(
                        id = "",
                        provider = RemoteMusicProvider.SUBSONIC,
                        displayName = "$username @ ${normalized.toHttpUrl().host}",
                        serverUrl = normalized,
                        username = username.trim(),
                        secret = password,
                    )
                requireSuccessfulResponse(request(account, "ping"))
                account
            }
        }

    suspend fun listSongs(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage =
        withContext(Dispatchers.IO) {
            val response =
                requireSuccessfulResponse(
                    request(
                        account,
                        "search3",
                        mapOf(
                            "query" to query,
                            "artistCount" to "0",
                            "albumCount" to "0",
                            "songCount" to limit.toString(),
                            "songOffset" to offset.toString(),
                        ),
                    ),
                )
            val songArray = response.optJSONObject("searchResult3")?.optJSONArray("song")
            val songs = parseSongs(account, songArray)
            if (songs.isEmpty() && query.isBlank() && offset == 0) {
                val randomResponse =
                    requireSuccessfulResponse(
                        request(account, "getRandomSongs", mapOf("size" to limit.toString())),
                    )
                val randomSongs =
                    parseSongs(account, randomResponse.optJSONObject("randomSongs")?.optJSONArray("song"))
                return@withContext RemoteSongPage(randomSongs, null)
            }
            RemoteSongPage(songs, (offset + songs.size).takeIf { songs.size == limit })
        }

    fun streamUrl(
        account: RemoteMusicAccount,
        songId: String,
    ): String = apiUrl(account, "stream", mapOf("id" to songId, "format" to "raw")).toString()

    private fun parseSongs(
        account: RemoteMusicAccount,
        array: JSONArray?,
    ): List<RemoteSong> =
        buildList {
            for (index in 0 until (array?.length() ?: 0)) {
                val item = array?.optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val coverArt = item.optString("coverArt").takeIf(String::isNotBlank)
                add(
                    RemoteSong(
                        id = id,
                        title = item.optString("title", "Unknown title"),
                        artist = item.optString("artist", "Unknown artist"),
                        album = item.optString("album", "Unknown album"),
                        durationMs = item.optLong("duration").coerceAtLeast(0L) * 1_000L,
                        mimeType = item.optString("contentType", "audio/mpeg"),
                        artworkUrl =
                            coverArt?.let {
                                apiUrl(
                                    account,
                                    "getCoverArt",
                                    mapOf("id" to it, "size" to "400"),
                                ).toString()
                            },
                    ),
                )
            }
        }

    private fun request(
        account: RemoteMusicAccount,
        endpoint: String,
        parameters: Map<String, String> = emptyMap(),
    ): JSONObject {
        val request =
            Request
                .Builder()
                .url(apiUrl(account, endpoint, parameters + ("f" to "json")))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Subsonic HTTP ${response.code}" }
            JSONObject(response.body.string())
        }
    }

    private fun apiUrl(
        account: RemoteMusicAccount,
        endpoint: String,
        parameters: Map<String, String> = emptyMap(),
    ): HttpUrl {
        val salt =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(12)
        val token = md5(account.secret + salt)
        val builder =
            "${account.normalizedServerUrl}/rest/$endpoint.view"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("u", account.username)
                .addQueryParameter("t", token)
                .addQueryParameter("s", salt)
                .addQueryParameter("v", API_VERSION)
                .addQueryParameter("c", CLIENT_ID)
        parameters.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build()
    }

    private fun requireSuccessfulResponse(root: JSONObject): JSONObject {
        val response =
            root.optJSONObject("subsonic-response")
                ?: error("Invalid Subsonic response")
        if (response.optString("status") != "ok") {
            val error = response.optJSONObject("error")
            throw IllegalStateException(
                error?.optString("message")?.takeIf(String::isNotBlank)
                    ?: "Subsonic authentication failed",
            )
        }
        return response
    }

    private fun normalizeServerUrl(value: String): String {
        val url =
            value.trim().trimEnd('/').toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid server URL")
        require(url.scheme == "http" || url.scheme == "https") { "Only HTTP or HTTPS is supported" }
        return url.toString().trimEnd('/')
    }

    @Suppress("DEPRECATION")
    private fun md5(value: String): String =
        MessageDigest
            .getInstance("MD5")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_ID = "SPICaMusic"
        const val USER_AGENT = "SPICaMusic/Subsonic"
    }
}

class NeteaseClient(
    baseClient: OkHttpClient,
) {
    private val client =
        baseClient
            .newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    private val libraryCache = ConcurrentHashMap<String, List<RemoteSong>>()

    suspend fun authenticate(cookieHeader: String): Result<RemoteMusicAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(cookieValue(cookieHeader, "MUSIC_U").isNotBlank()) {
                    "NetEase MUSIC_U login cookie was not found"
                }
                val root = executeJson(getRequest(ACCOUNT_URL, cookieHeader))
                val profile = root.optJSONObject("profile")
                val account = root.optJSONObject("account")
                val userId =
                    profile?.optLong("userId")?.takeIf { it > 0L }
                        ?: account?.optLong("id")?.takeIf { it > 0L }
                        ?: error("NetEase login session could not be verified")
                RemoteMusicAccount(
                    id = "",
                    provider = RemoteMusicProvider.NETEASE,
                    displayName =
                        profile?.optString("nickname")?.takeIf(String::isNotBlank)
                            ?: "NetEase $userId",
                    secret = cookieHeader,
                    userId = userId.toString(),
                )
            }
        }

    suspend fun listSongs(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage =
        withContext(Dispatchers.IO) {
            if (query.isNotBlank()) {
                search(account, query, offset, limit)
            } else {
                val library =
                    libraryCache[account.id] ?: loadLibrary(account).also {
                        libraryCache[account.id] = it
                    }
                val songs = library.drop(offset).take(limit)
                RemoteSongPage(songs, (offset + songs.size).takeIf { it < library.size })
            }
        }

    suspend fun resolveStreamUrl(
        account: RemoteMusicAccount,
        songId: String,
    ): String =
        withContext(Dispatchers.IO) {
            val form =
                FormBody
                    .Builder()
                    .add("ids", "[$songId]")
                    .add("br", "999000")
                    .build()
            val request =
                requestBuilder(STREAM_URL, account.secret)
                    .post(form)
                    .build()
            val root = executeJson(request)
            root
                .optJSONArray("data")
                ?.optJSONObject(0)
                ?.optString("url")
                ?.takeIf(String::isNotBlank)
                ?: "https://music.163.com/song/media/outer/url?id=$songId.mp3"
        }

    fun clearCache(accountId: String) {
        libraryCache.remove(accountId)
    }

    private fun search(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage {
        val form =
            FormBody
                .Builder()
                .add("s", query)
                .add("type", "1")
                .add("offset", offset.toString())
                .add("limit", limit.toString())
                .build()
        val root =
            executeJson(
                requestBuilder(SEARCH_URL, account.secret)
                    .post(form)
                    .build(),
            )
        val result = root.optJSONObject("result")
        val songs = parseSongs(result?.optJSONArray("songs"))
        val total = result?.optInt("songCount", offset + songs.size) ?: (offset + songs.size)
        return RemoteSongPage(songs, (offset + songs.size).takeIf { it < total && songs.isNotEmpty() })
    }

    private fun loadLibrary(account: RemoteMusicAccount): List<RemoteSong> {
        val userId = account.userId.toLongOrNull() ?: error("NetEase user id is missing")
        val playlistUrl =
            PLAYLISTS_URL
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("uid", userId.toString())
                .addQueryParameter("limit", "12")
                .addQueryParameter("offset", "0")
                .build()
        val playlists =
            executeJson(getRequest(playlistUrl.toString(), account.secret))
                .optJSONArray("playlist")
        val result = LinkedHashMap<String, RemoteSong>()
        for (index in 0 until minOf(playlists?.length() ?: 0, MAX_LIBRARY_PLAYLISTS)) {
            val playlistId = playlists?.optJSONObject(index)?.optLong("id") ?: continue
            val detailUrl =
                PLAYLIST_DETAIL_URL
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("id", playlistId.toString())
                    .addQueryParameter("n", "1000")
                    .addQueryParameter("s", "0")
                    .build()
            val tracks =
                executeJson(getRequest(detailUrl.toString(), account.secret))
                    .optJSONObject("playlist")
                    ?.optJSONArray("tracks")
            parseSongs(tracks).forEach { result.putIfAbsent(it.id, it) }
        }
        return result.values.toList()
    }

    private fun parseSongs(array: JSONArray?): List<RemoteSong> =
        buildList {
            for (index in 0 until (array?.length() ?: 0)) {
                val item = array?.optJSONObject(index) ?: continue
                val id = item.optLong("id").takeIf { it > 0L } ?: continue
                val artists = item.optJSONArray("ar") ?: item.optJSONArray("artists")
                val artistNames =
                    buildList {
                        for (artistIndex in 0 until (artists?.length() ?: 0)) {
                            artists
                                ?.optJSONObject(artistIndex)
                                ?.optString("name")
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }
                val albumObject = item.optJSONObject("al") ?: item.optJSONObject("album")
                add(
                    RemoteSong(
                        id = id.toString(),
                        title = item.optString("name", "Unknown title"),
                        artist = artistNames.joinToString(" / ").ifBlank { "Unknown artist" },
                        album = albumObject?.optString("name").orEmpty().ifBlank { "Unknown album" },
                        durationMs =
                            item.optLong("dt").takeIf { it > 0L }
                                ?: item.optLong("duration").coerceAtLeast(0L),
                        mimeType = "audio/mpeg",
                        artworkUrl = albumObject?.optString("picUrl")?.takeIf(String::isNotBlank),
                    ),
                )
            }
        }

    private fun getRequest(
        url: String,
        cookies: String,
    ): Request = requestBuilder(url, cookies).get().build()

    private fun requestBuilder(
        url: String,
        cookies: String,
    ): Request.Builder =
        Request
            .Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", "https://music.163.com/")
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Cookie", cookies)

    private fun executeJson(request: Request): JSONObject =
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "NetEase HTTP ${response.code}" }
            JSONObject(response.body.string())
        }

    private companion object {
        const val ACCOUNT_URL = "https://music.163.com/api/nuser/account/get"
        const val PLAYLISTS_URL = "https://music.163.com/api/user/playlist/"
        const val PLAYLIST_DETAIL_URL = "https://music.163.com/api/v6/playlist/detail"
        const val SEARCH_URL = "https://music.163.com/api/search/get/web"
        const val STREAM_URL = "https://music.163.com/api/song/enhance/player/url"
        const val MAX_LIBRARY_PLAYLISTS = 6
    }
}

class QqMusicClient(
    baseClient: OkHttpClient,
) {
    private val client =
        baseClient
            .newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    private val libraryCache = ConcurrentHashMap<String, List<RemoteSong>>()

    suspend fun authenticate(cookieHeader: String): Result<RemoteMusicAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uin = extractUin(cookieHeader)
                require(uin != "0") { "QQ Music login account was not found" }
                require(
                    cookieValue(cookieHeader, "qm_keyst").isNotBlank() ||
                        cookieValue(cookieHeader, "qqmusic_key").isNotBlank(),
                ) { "QQ Music session key was not found" }
                RemoteMusicAccount(
                    id = "",
                    provider = RemoteMusicProvider.QQ_MUSIC,
                    displayName = "QQ Music $uin",
                    secret = cookieHeader,
                    userId = uin,
                )
            }
        }

    suspend fun listSongs(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage =
        withContext(Dispatchers.IO) {
            if (query.isNotBlank()) {
                search(account, query, offset, limit)
            } else {
                val library =
                    libraryCache[account.id] ?: loadLibrary(account).also {
                        libraryCache[account.id] = it
                    }
                val songs = library.drop(offset).take(limit)
                RemoteSongPage(songs, (offset + songs.size).takeIf { it < library.size })
            }
        }

    suspend fun resolveStreamUrl(
        account: RemoteMusicAccount,
        songId: String,
    ): String =
        withContext(Dispatchers.IO) {
            val uin = extractUin(account.secret)
            val key =
                cookieValue(account.secret, "qm_keyst")
                    .ifBlank { cookieValue(account.secret, "qqmusic_key") }
            val payload =
                JSONObject(
                    mapOf(
                        "req_0" to
                            mapOf(
                                "module" to "music.vkey.GetEVkey",
                                "method" to "GetUrl",
                                "param" to
                                    mapOf(
                                        "guid" to "327783793guid",
                                        "songmid" to listOf(songId),
                                        "songtype" to listOf(0),
                                        "uin" to uin,
                                        "loginflag" to 1,
                                        "platform" to "20",
                                        "xcdn" to 1,
                                    ),
                            ),
                        "comm" to
                            mapOf(
                                "uin" to uin,
                                "format" to "json",
                                "ct" to 19,
                                "cv" to 1602,
                                "authst" to key,
                            ),
                    ),
                )
            val request =
                requestBuilder(VKEY_URL, account.secret)
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
            val root = JSONObject(executeText(request))
            val data = root.optJSONObject("req_0")?.optJSONObject("data")
            val purl =
                data
                    ?.optJSONArray("midurlinfo")
                    ?.optJSONObject(0)
                    ?.optString("purl")
                    ?.takeIf(String::isNotBlank)
                    ?: error("QQ Music did not return a playable URL")
            if (purl.startsWith("http")) {
                purl
            } else {
                val sip = data.optJSONArray("sip")?.optString(0).orEmpty()
                (sip.ifBlank { "https://ws.stream.qqmusic.qq.com/" }).trimEnd('/') +
                    "/" +
                    purl.trimStart('/')
            }
        }

    fun clearCache(accountId: String) {
        libraryCache.remove(accountId)
    }

    private fun search(
        account: RemoteMusicAccount,
        query: String,
        offset: Int,
        limit: Int,
    ): RemoteSongPage {
        val page = offset / limit + 1
        val url =
            SEARCH_URL
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("format", "json")
                .addQueryParameter("w", query)
                .addQueryParameter("p", page.toString())
                .addQueryParameter("n", limit.toString())
                .addQueryParameter("t", "0")
                .build()
        val root = JSONObject(executeText(requestBuilder(url.toString(), account.secret).get().build()))
        val songData = root.optJSONObject("data")?.optJSONObject("song")
        val songs = parseSongs(songData?.optJSONArray("list"))
        val total = songData?.optInt("totalnum", offset + songs.size) ?: (offset + songs.size)
        return RemoteSongPage(songs, (offset + songs.size).takeIf { it < total && songs.isNotEmpty() })
    }

    private fun loadLibrary(account: RemoteMusicAccount): List<RemoteSong> {
        val uin = extractUin(account.secret)
        val gtk = gtk(account.secret)
        val createdUrl =
            CREATED_PLAYLISTS_URL
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("format", "json")
                .addQueryParameter("platform", "yqq.json")
                .addQueryParameter("uin", uin)
                .addQueryParameter("hostuin", uin)
                .addQueryParameter("g_tk", gtk.toString())
                .addQueryParameter("sin", "0")
                .addQueryParameter("size", "12")
                .build()
        val created =
            JSONObject(executeText(requestBuilder(createdUrl.toString(), account.secret).get().build()))
                .optJSONObject("data")
                ?.optJSONArray("disslist")
        val playlistIds = LinkedHashSet<Long>()
        for (index in 0 until (created?.length() ?: 0)) {
            created
                ?.optJSONObject(index)
                ?.optLong("tid")
                ?.takeIf { it > 0L }
                ?.let(playlistIds::add)
        }
        val result = LinkedHashMap<String, RemoteSong>()
        playlistIds.take(MAX_LIBRARY_PLAYLISTS).forEach { playlistId ->
            val detailUrl =
                PLAYLIST_DETAIL_URL
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("type", "1")
                    .addQueryParameter("json", "1")
                    .addQueryParameter("utf8", "1")
                    .addQueryParameter("onlysong", "0")
                    .addQueryParameter("disstid", playlistId.toString())
                    .addQueryParameter("song_begin", "0")
                    .addQueryParameter("song_num", "1000")
                    .addQueryParameter("g_tk", gtk.toString())
                    .addQueryParameter("format", "json")
                    .build()
            val songs =
                JSONObject(executeText(requestBuilder(detailUrl.toString(), account.secret).get().build()))
                    .optJSONArray("cdlist")
                    ?.optJSONObject(0)
                    ?.optJSONArray("songlist")
            parseSongs(songs).forEach { result.putIfAbsent(it.id, it) }
        }
        return result.values.toList()
    }

    private fun parseSongs(array: JSONArray?): List<RemoteSong> =
        buildList {
            for (index in 0 until (array?.length() ?: 0)) {
                val item = array?.optJSONObject(index) ?: continue
                val mid = item.optString("songmid", item.optString("mid"))
                if (mid.isBlank()) continue
                val singers = item.optJSONArray("singer")
                val artistNames =
                    buildList {
                        for (singerIndex in 0 until (singers?.length() ?: 0)) {
                            singers
                                ?.optJSONObject(singerIndex)
                                ?.optString("name")
                                ?.takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }
                val albumObject = item.optJSONObject("album")
                val albumMid =
                    item.optString("albummid").ifBlank {
                        albumObject?.optString("mid").orEmpty()
                    }
                add(
                    RemoteSong(
                        id = mid,
                        title =
                            item.optString("songname").ifBlank {
                                item.optString("title", "Unknown title")
                            },
                        artist = artistNames.joinToString(" / ").ifBlank { "Unknown artist" },
                        album =
                            item
                                .optString("albumname")
                                .ifBlank {
                                    albumObject?.optString("name").orEmpty()
                                }.ifBlank { "Unknown album" },
                        durationMs = item.optLong("interval").coerceAtLeast(0L) * 1_000L,
                        mimeType = "audio/mp4",
                        artworkUrl =
                            albumMid
                                .takeIf(String::isNotBlank)
                                ?.let { "https://y.qq.com/music/photo_new/T002R300x300M000$it.jpg" },
                    ),
                )
            }
        }

    private fun requestBuilder(
        url: String,
        cookies: String,
    ): Request.Builder =
        Request
            .Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", "https://y.qq.com/")
            .header("Origin", "https://y.qq.com")
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Cookie", cookies)

    private fun executeText(request: Request): String =
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "QQ Music HTTP ${response.code}" }
            decodePossiblyCompressed(response.body.bytes())
        }

    private fun decodePossiblyCompressed(data: ByteArray): String {
        val direct = String(data, StandardCharsets.UTF_8).trim()
        if (direct.startsWith("{") || direct.startsWith("[")) return direct
        val zlibOffset =
            (0 until minOf(10, data.size - 1))
                .firstOrNull { data[it] == 0x78.toByte() }
                ?: return direct.substringAfter('(').substringBeforeLast(')')
        return runCatching {
            InflaterInputStream(ByteArrayInputStream(data, zlibOffset, data.size - zlibOffset)).use { input ->
                ByteArrayOutputStream().use { output ->
                    input.copyTo(output)
                    output.toString(StandardCharsets.UTF_8.name())
                }
            }
        }.getOrDefault(direct)
    }

    private fun gtk(cookies: String): Long {
        val key =
            cookieValue(cookies, "p_skey")
                .ifBlank { cookieValue(cookies, "skey") }
        var hash = 5381L
        key.forEach { hash += (hash shl 5) + it.code }
        return hash and 0x7fffffff
    }

    private companion object {
        const val CREATED_PLAYLISTS_URL =
            "https://c6.y.qq.com/rsc/fcgi-bin/fcg_user_created_diss"
        const val PLAYLIST_DETAIL_URL =
            "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg"
        const val SEARCH_URL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp"
        const val VKEY_URL = "https://u6.y.qq.com/cgi-bin/musics.fcg"
        const val MAX_LIBRARY_PLAYLISTS = 6
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

internal fun cookieValue(
    header: String,
    name: String,
): String =
    header
        .split(';')
        .asSequence()
        .map(String::trim)
        .firstOrNull { it.substringBefore('=').trim() == name }
        ?.substringAfter('=', "")
        .orEmpty()

internal fun extractUin(cookieHeader: String): String =
    sequenceOf("uin", "p_uin", "luin", "wxuin")
        .map { cookieValue(cookieHeader, it) }
        .firstOrNull(String::isNotBlank)
        .orEmpty()
        .filter(Char::isDigit)
        .ifBlank { "0" }
