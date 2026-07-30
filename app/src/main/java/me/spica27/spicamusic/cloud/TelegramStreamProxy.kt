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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.ServerSocket

/**
 * 把 TDLib 的渐进下载文件暴露为仅监听 127.0.0.1 的 Range HTTP 流。
 * 页面列表不会下载音频；只有真正播放或拖动进度时才申请对应字节区间。
 */
class TelegramStreamProxy(
    private val repository: TelegramRepository,
) {
    private val startMutex = Mutex()

    @Volatile
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    @Volatile
    private var port: Int = 0

    suspend fun streamUrl(song: TelegramSong): String {
        ensureStarted()
        return "http://127.0.0.1:$port/telegram/${song.fileId}?size=${song.fileSize}"
    }

    suspend fun artworkUrl(song: TelegramSong): String? {
        val coverFileId = song.coverFileId ?: return null
        ensureStarted()
        return "http://127.0.0.1:$port/telegram-art/$coverFileId"
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
                        get("/telegram/{fileId}") {
                            val fileId = call.parameters["fileId"]?.toIntOrNull()
                            val knownSize = call.request.queryParameters["size"]?.toLongOrNull()
                            if (fileId == null || fileId <= 0 || knownSize == null || knownSize <= 0L) {
                                call.respond(HttpStatusCode.BadRequest, "Invalid Telegram file")
                                return@get
                            }

                            val range = parseRange(call.request.headers["Range"], knownSize)
                            if (range == null) {
                                call.respond(HttpStatusCode(416, "Range Not Satisfiable"), "Invalid byte range")
                                return@get
                            }
                            val (start, end) = range
                            val length = end - start + 1L
                            repository.download(fileId, start, length)

                            val isPartial = start != 0L || end != knownSize - 1L
                            call.response.header("Accept-Ranges", "bytes")
                            call.response.header("Content-Length", length.toString())
                            if (isPartial) {
                                call.response.header("Content-Range", "bytes $start-$end/$knownSize")
                            }

                            call.respondOutputStream(
                                contentType = ContentType.Audio.Any,
                                status = if (isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK,
                            ) {
                                var current = start
                                var randomAccessFile: RandomAccessFile? = null
                                try {
                                    while (current <= end) {
                                        val info = repository.getFile(fileId)
                                        val path = info.local.path
                                        val availableStart = info.local.downloadOffset
                                        val availableEnd = availableStart + info.local.downloadedPrefixSize
                                        if (path.isBlank() || !File(path).isFile || current !in availableStart until availableEnd) {
                                            delay(POLL_DELAY_MS)
                                            continue
                                        }
                                        if (randomAccessFile == null) {
                                            randomAccessFile = RandomAccessFile(path, "r")
                                        }
                                        val available = (availableEnd - current).coerceAtMost(end - current + 1L)
                                        if (available <= 0L) {
                                            delay(POLL_DELAY_MS)
                                            continue
                                        }
                                        val buffer = ByteArray(minOf(BUFFER_SIZE.toLong(), available).toInt())
                                        randomAccessFile.seek(current)
                                        val read = randomAccessFile.read(buffer)
                                        if (read <= 0) {
                                            delay(POLL_DELAY_MS)
                                            continue
                                        }
                                        write(buffer, 0, read)
                                        flush()
                                        current += read
                                    }
                                } finally {
                                    randomAccessFile?.close()
                                }
                            }
                        }
                        get("/telegram-art/{fileId}") {
                            val fileId = call.parameters["fileId"]?.toIntOrNull()
                            if (fileId == null || fileId <= 0) {
                                call.respond(HttpStatusCode.BadRequest, "Invalid Telegram artwork")
                                return@get
                            }
                            val artwork = awaitDownloadedFile(fileId)
                            if (artwork == null) {
                                call.respond(HttpStatusCode.ServiceUnavailable, "Artwork is not available")
                                return@get
                            }
                            call.response.header("Cache-Control", "private, max-age=86400")
                            call.response.header("Content-Length", artwork.length().toString())
                            call.respondOutputStream(contentType = ContentType.Image.Any) {
                                artwork.inputStream().buffered().use { input ->
                                    input.copyTo(this)
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

    private suspend fun awaitDownloadedFile(fileId: Int): File? {
        repository.download(fileId, 0L, 0L)
        repeat(ARTWORK_POLL_LIMIT) {
            val info = repository.getFile(fileId)
            val candidate =
                info.local.path
                    .takeIf(String::isNotBlank)
                    ?.let(::File)
            if (info.local.isDownloadingCompleted && candidate?.isFile == true && candidate.length() > 0L) {
                return candidate
            }
            delay(POLL_DELAY_MS)
        }
        return null
    }

    private fun parseRange(
        header: String?,
        total: Long,
    ): Pair<Long, Long>? {
        if (header.isNullOrBlank()) return 0L to (total - 1L)
        if (!header.startsWith("bytes=") || header.contains(',')) return null
        val parts = header.removePrefix("bytes=").split('-', limit = 2)
        if (parts.size != 2) return null
        val start = parts[0].toLongOrNull() ?: return null
        val end = parts[1].toLongOrNull() ?: (total - 1L)
        if (start < 0L || end < start || start >= total) return null
        return start to end.coerceAtMost(total - 1L)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val POLL_DELAY_MS = 40L
        const val ARTWORK_POLL_LIMIT = 250
    }
}
