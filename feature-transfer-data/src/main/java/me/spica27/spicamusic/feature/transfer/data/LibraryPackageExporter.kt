package me.spica27.spicamusic.feature.transfer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import me.spica27.spicamusic.feature.transfer.domain.LibraryPackage
import me.spica27.spicamusic.feature.transfer.domain.PackageAsset
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.PackageLyrics
import me.spica27.spicamusic.feature.transfer.domain.PackageManifest
import me.spica27.spicamusic.feature.transfer.domain.PackagePlaylist
import me.spica27.spicamusic.feature.transfer.domain.PackagePlaylistEntry
import me.spica27.spicamusic.feature.transfer.domain.PackageTrack
import me.spica27.spicamusic.feature.transfer.domain.SourceReference
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.SelectedLyricsResolver
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

internal class LibraryPackageExporter(
    private val context: Context,
    private val database: AppDatabase,
    private val lyricReader: ILyricSourceReader,
) {
    suspend fun export(
        uri: Uri,
        progress: (TransferState.Running) -> Unit,
    ): Pair<Int, Int> {
        progress(TransferState.Running("读取曲库与歌单"))
        val snapshot = database.readTransferSnapshot(assignIdentities = true)
        require(snapshot.songs.isNotEmpty() || snapshot.playlists.isNotEmpty()) { "曲库为空，没有可导出的内容" }
        val assets = LinkedHashMap<String, PackageAsset>()
        val openers = HashMap<String, () -> InputStream>()
        val sourcesBySong = snapshot.identities.filter { it.kind == TRACK_IDENTITY }.groupBy { it.localId }

        fun addText(
            path: String,
            value: String,
            kind: String,
        ): String {
            val bytes = value.toByteArray(Charsets.UTF_8)
            assets[path] = PackageArchive.asset(path, bytes, kind)
            openers[path] = { ByteArrayInputStream(bytes) }
            return path
        }
        val tracks =
            snapshot.songs.mapIndexed { index, song ->
                currentCoroutineContext().ensureActive()
                progress(TransferState.Running("检查音频：${song.displayName}", index.toLong(), snapshot.songs.size.toLong()))
                val audioUri = mediaUri(song.mediaStoreId)
                val fingerprint =
                    try {
                        open(audioUri).use { PackageArchive.copyAndHash(it, null, LibraryPackage.MAX_AUDIO_BYTES) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error("无法读取音频「${song.displayName}」，完整包尚未导出：${e.message}")
                    }
                require(fingerprint.first > 0) { "音频为空：${song.displayName}" }
                val audioPath = "audio/${fingerprint.second}.bin"
                assets[audioPath] = PackageAsset(audioPath, fingerprint.second, fingerprint.first, "audio")
                openers[audioPath] = { open(audioUri) }
                val cached = snapshot.lyrics[song.mediaStoreId]?.stored()
                val selected =
                    SelectedLyricsResolver.resolve(song.mediaStoreId, cached) {
                        lyricReader.readEmbeddedForSnapshot(song.mediaStoreId)
                    }
                val lyrics =
                    selected?.let {
                        val raw = it.lyrics
                        val format = lyricFormat(raw)
                        val path =
                            if (raw.isBlank()) {
                                null
                            } else {
                                val bytes = raw.toByteArray(Charsets.UTF_8)
                                require(bytes.size <= LibraryPackage.MAX_LYRIC_BYTES) { "歌词过大：${song.displayName}" }
                                addText("lyrics/${PackageArchive.digest(bytes)}.$format", raw, "lyrics")
                            }
                        PackageLyrics(path, format, it.delay, it.sourceType, it.sourceName, it.isManual, it.lyricsSuppressed)
                    }
                val sources =
                    sourcesBySong[song.songId]
                        .orEmpty()
                        .flatMap { PackageJson.decodeFromString<List<SourceReference>>(it.sourcesJson) }
                        .distinct()
                PackageTrack(
                    id = snapshot.id(TRACK_IDENTITY, requireNotNull(song.songId)),
                    title = song.displayName,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.duration,
                    fileName = File(song.path).name.ifBlank { "${song.displayName}.audio" },
                    mimeType = song.mimeType,
                    audio = audioPath,
                    favorite = song.like,
                    ignored = song.isIgnore,
                    trackNumber = song.trackNumber,
                    sampleRate = song.sampleRate,
                    bitRate = song.bitRate,
                    channels = song.channels,
                    bitDepth = song.digit,
                    codec = song.codec,
                    lyrics = lyrics,
                    sources = sources,
                )
            }
        val trackIds = snapshot.songs.associate { it.mediaStoreId to snapshot.id(TRACK_IDENTITY, requireNotNull(it.songId)) }
        val entriesByPlaylist = snapshot.entries.groupBy { it.playlistId }
        val playlists =
            snapshot.playlists.map { playlist ->
                val id = snapshot.id(PLAYLIST_IDENTITY, requireNotNull(playlist.playlistId))
                val entries =
                    entriesByPlaylist[playlist.playlistId].orEmpty().map { entry ->
                        val trackId = trackIds[entry.mediaId] ?: error("歌单「${playlist.playlistName}」包含已丢失的歌曲，请先移除失效条目")
                        PackagePlaylistEntry(UUID.nameUUIDFromBytes("$id:$trackId".toByteArray(Charsets.UTF_8)).toString(), trackId)
                    }
                val artwork =
                    playlist.cover?.takeIf { it.isNotBlank() }?.let { cover ->
                        val coverUri = if (cover.startsWith("/")) Uri.fromFile(File(cover)) else Uri.parse(cover)
                        val bytes = open(coverUri).use { it.readBounded(LibraryPackage.MAX_ARTWORK_BYTES) }
                        val path = "artwork/${PackageArchive.digest(bytes)}.bin"
                        assets[path] = PackageArchive.asset(path, bytes, "artwork")
                        openers[path] = { ByteArrayInputStream(bytes) }
                        path
                    }
                PackagePlaylist(id, playlist.playlistName, playlist.createTimestamp, entries, artwork)
            }
        addText("tracks.json", PackageJson.encodeToString(tracks), "metadata")
        addText("playlists.json", PackageJson.encodeToString(playlists), "metadata")
        val library =
            LibraryPackage(
                PackageManifest(
                    archiveId = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    assets = assets.values.toList(),
                    requiredFeatures =
                        buildList {
                            add("complete-audio")
                            add("selected-lyrics")
                            if (tracks.any { it.lyrics?.suppressed == true }) add(LibraryPackage.LYRICS_SUPPRESSION)
                        },
                ),
                tracks,
                playlists,
            )
        library.validate()
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("无法打开导出文件")
        output.use {
            PackageArchive().write(library, it, { name -> openers.getValue(name).invoke() }) { done, total ->
                progress(TransferState.Running("正在写入完整包", done, total))
            }
        }
        return tracks.size to playlists.size
    }

    private fun open(uri: Uri): InputStream = context.contentResolver.openInputStream(uri) ?: error("无法读取文件：$uri")
}

internal fun mediaUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

internal fun lyricFormat(raw: String): String =
    when {
        raw.contains("<tt", ignoreCase = true) -> "ttml"
        raw.lineSequence().any { it.startsWith("[") && it.contains("](") } -> "yrc"
        Regex("\\[\\d+:\\d+").containsMatchIn(raw) -> "lrc"
        else -> "txt"
    }

internal fun InputStream.readBounded(limit: Long): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count == -1) break
        require(output.size().toLong() + count <= limit) { "文件超过大小限制" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
