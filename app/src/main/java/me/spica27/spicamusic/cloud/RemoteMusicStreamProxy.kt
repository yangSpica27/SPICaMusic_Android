package me.spica27.spicamusic.cloud

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ServerSocket

/**
 * Keeps provider credentials and short-lived upstream URLs out of Media3's queue.
 * Only loopback clients can reach this proxy; each request resolves a fresh provider URL and
 * forwards byte ranges without buffering the whole song.
 */
class RemoteMusicStreamProxy(
    baseClient: OkHttpClient,
    private val accountStore: CloudAccountStore,
    private val clients: RemoteMusicClientRegistry,
) {
    private val upstreamClient = baseClient.newBuilder().build()
    private val startMutex = Mutex()

    @Volatile
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    @Volatile
    private var port: Int = 0

    suspend fun streamUrl(
        account: RemoteMusicAccount,
        song: RemoteSong,
    ): String {
        ensureStarted()
        return "http://127.0.0.1:$port/remote/${account.id}/${song.id}"
    }

    private suspend fun ensureStarted() {
        if (server != null) return
        startMutex.withLock {
            if (server != null) return
            val selectedPort =
                withContext(Dispatchers.IO) {
                    ServerSocket(0).use { it.localPort }
                }
            val newServer =
                embeddedServer(CIO, port = selectedPort, host = "127.0.0.1") {
                    routing {
                        get("/remote/{accountId}/{songId}") {
                            val accountId = call.parameters["accountId"].orEmpty()
                            val songId = call.parameters["songId"].orEmpty()
                            if (!SAFE_ID.matches(accountId) || !SAFE_ID.matches(songId)) {
                                call.respond(HttpStatusCode.BadRequest, "Invalid cloud stream id")
                                return@get
                            }
                            val account =
                                accountStore
                                    .getRemoteAccounts()
                                    .firstOrNull { it.id == accountId }
                            if (account == null) {
                                call.respond(HttpStatusCode.NotFound, "Cloud account not found")
                                return@get
                            }
                            val upstreamUrl =
                                runCatching { clients.resolveStreamUrl(account, songId) }
                                    .getOrElse {
                                        call.respond(
                                            HttpStatusCode.BadGateway,
                                            it.message ?: "Unable to resolve cloud stream",
                                        )
                                        return@get
                                    }
                            val requestBuilder =
                                Request
                                    .Builder()
                                    .url(upstreamUrl)
                                    .header("Accept-Encoding", "identity")
                            call.request.headers["Range"]?.let {
                                if (!SAFE_RANGE.matches(it)) {
                                    call.respond(
                                        HttpStatusCode(416, "Range Not Satisfiable"),
                                        "Invalid byte range",
                                    )
                                    return@get
                                }
                                requestBuilder.header("Range", it)
                            }
                            withContext(Dispatchers.IO) {
                                upstreamClient.newCall(requestBuilder.build()).execute()
                            }.use { response ->
                                if (!response.isSuccessful && response.code != 206) {
                                    call.respond(
                                        HttpStatusCode.fromValue(
                                            if (response.code in 400..599) response.code else 502,
                                        ),
                                        "Upstream stream failed",
                                    )
                                    return@get
                                }
                                response.header("Accept-Ranges")?.let {
                                    call.response.header("Accept-Ranges", it)
                                }
                                response.header("Content-Length")?.let {
                                    call.response.header("Content-Length", it)
                                }
                                response.header("Content-Range")?.let {
                                    call.response.header("Content-Range", it)
                                }
                                val type =
                                    response
                                        .header("Content-Type")
                                        ?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                                        ?: ContentType.Audio.Any
                                call.respondOutputStream(
                                    contentType = type,
                                    status = HttpStatusCode.fromValue(response.code),
                                ) {
                                    response.body.byteStream().use { input ->
                                        input.copyTo(this, BUFFER_SIZE)
                                    }
                                }
                            }
                        }
                    }
                }
            withContext(Dispatchers.IO) { newServer.start(wait = false) }
            port = selectedPort
            server = newServer
        }
    }

    private companion object {
        val SAFE_ID = Regex("^[A-Za-z0-9_.:-]{1,160}$")
        val SAFE_RANGE = Regex("^bytes=\\d*-\\d*$")
        const val BUFFER_SIZE = 64 * 1024
    }
}
