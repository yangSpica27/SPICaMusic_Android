package me.spica27.spicamusic.feature.transfer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import me.spica27.spicamusic.feature.transfer.domain.ImportPreview
import me.spica27.spicamusic.feature.transfer.domain.LibraryPackage
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.PackageTrack
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.SelectedLyricsResolver
import me.spica27.spicamusic.storage.api.StoredLyrics
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.entity.TransferIdentityEntity
import me.spica27.spicamusic.storage.impl.entity.TransferReceiptEntity
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

internal data class PreparedImport(
    val library: LibraryPackage,
    val matches: Map<String, SongEntity>,
    val localLyrics: Map<String, StoredLyrics>,
    val preview: ImportPreview,
)

internal class LibraryPackageImporter(
    private val context: Context,
    private val database: AppDatabase,
    private val lyricReader: ILyricSourceReader,
) {
    private val resolver = context.contentResolver

    suspend fun prepare(
        file: File,
        progress: (TransferState.Running) -> Unit,
    ): PreparedImport {
        val library = PackageArchive().inspect(file) { done, total -> progress(TransferState.Running("校验完整包", done, total)) }
        val local = database.readTransferSnapshot()
        val assets = library.manifest.assets.associateBy { it.path }
        val songsById = local.songs.associateBy { it.songId }
        val identities = local.identities.filter { it.kind == TRACK_IDENTITY }.associateBy { it.stableId }
        val bySize = local.songs.groupBy { it.size }
        val hashes = HashMap<Long, String?>()
        val usedSongs = HashSet<Long>()
        val matches = LinkedHashMap<String, SongEntity>()
        val selectedLyrics = HashMap<String, StoredLyrics>()
        var conflicts = 0

        suspend fun hash(song: SongEntity): String? {
            val id = requireNotNull(song.songId)
            if (hashes.containsKey(id)) return hashes[id]
            val result =
                try {
                    resolver.openInputStream(mediaUri(song.mediaStoreId))?.use {
                        PackageArchive.copyAndHash(it, null, LibraryPackage.MAX_AUDIO_BYTES).second
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null // 本地文件不可读时，从包内恢复。
                }
            hashes[id] = result
            return result
        }
        ZipFile(file).use { zip ->
            library.tracks.forEachIndexed { index, track ->
                currentCoroutineContext().ensureActive()
                progress(TransferState.Running("匹配已有歌曲：${track.title}", index.toLong(), library.tracks.size.toLong()))
                val asset = assets.getValue(track.audio)
                val identitySong = identities[track.id]?.localId?.let(songsById::get)
                val candidates = (listOfNotNull(identitySong) + bySize[asset.size].orEmpty()).distinctBy { it.songId }
                val match = candidates.firstOrNull { requireNotNull(it.songId) !in usedSongs && hash(it) == asset.sha256 }
                if (match != null) {
                    matches[track.id] = match
                    usedSongs.add(requireNotNull(match.songId))
                    val lyrics =
                        SelectedLyricsResolver.resolve(match.mediaStoreId, local.lyrics[match.mediaStoreId]?.stored()) {
                            lyricReader.readEmbedded(match.mediaStoreId)
                        }
                    if (lyrics != null) selectedLyrics[track.id] = lyrics
                    val incoming = track.lyrics
                    val incomingRaw = incoming?.asset?.let { PackageArchive.readSmall(zip, it, LibraryPackage.MAX_LYRIC_BYTES) }.orEmpty()
                    if (hasLyricsConflict(lyrics, incoming, incomingRaw)) {
                        conflicts++
                    }
                }
            }
        }
        val playlistIds = local.playlists.map { it.playlistId }.toSet()
        val existingPlaylists =
            local.identities
                .filter { it.kind == PLAYLIST_IDENTITY && it.localId in playlistIds }
                .map { it.stableId }
                .toSet()
        return PreparedImport(
            library,
            matches,
            selectedLyrics,
            ImportPreview(
                songs = library.tracks.size,
                playlists = library.playlists.size,
                newSongs = library.tracks.size - matches.size,
                reusedSongs = matches.size,
                newPlaylists = library.playlists.count { it.id !in existingPlaylists },
                lyricConflicts = conflicts,
                bytesToCopy = library.tracks.filterNot { it.id in matches }.sumOf { assets.getValue(it.audio).size },
            ),
        )
    }

    /** 调用前须持有曲库访问锁，直到导入或清理完成。 */
    suspend fun commit(
        taskId: String,
        file: File,
        plan: PreparedImport,
        preferImportedLyrics: Boolean,
        progress: (TransferState.Running) -> Unit,
    ) {
        val library = plan.library
        val assets = library.manifest.assets.associateBy { it.path }
        val free = Environment.getExternalStorageDirectory().usableSpace
        require(plan.preview.bytesToCopy + 32L * 1024 * 1024 < free) { "存储空间不足，无法恢复完整音频" }
        val newSongs = HashMap<String, SongEntity>()
        val artworkDir = File(context.filesDir, "library-artwork/$taskId")
        val artworkPaths = HashMap<String, String>()
        try {
            ZipFile(file).use { zip ->
                var copied = 0L
                library.tracks.filterNot { it.id in plan.matches }.forEach { track ->
                    currentCoroutineContext().ensureActive()
                    val asset = assets.getValue(track.audio)
                    val values =
                        ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, safeAudioName(track))
                            put(MediaStore.Audio.Media.RELATIVE_PATH, taskFolder(taskId))
                            put(MediaStore.Audio.Media.MIME_TYPE, track.mimeType.takeIf { it.startsWith("audio/") } ?: "audio/mpeg")
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                            put(MediaStore.Audio.Media.IS_MUSIC, 1)
                            put(MediaStore.Audio.Media.TITLE, track.title)
                            put(MediaStore.Audio.Media.ARTIST, track.artist)
                            put(MediaStore.Audio.Media.ALBUM, track.album)
                        }
                    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    val uri = resolver.insert(collection, values) ?: error("无法创建音频：${track.title}")
                    zip.getInputStream(zip.getEntry(track.audio)).use { input ->
                        val output = resolver.openOutputStream(uri, "w") ?: error("无法写入音频：${track.title}")
                        output.use {
                            val actual =
                                PackageArchive.copyAndHash(input, it, asset.size) { bytes ->
                                    progress(TransferState.Running("恢复音频：${track.title}", copied + bytes, plan.preview.bytesToCopy))
                                }
                            require(actual.first == asset.size && actual.second == asset.sha256) { "音频校验失败：${track.title}" }
                        }
                    }
                    check(
                        resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null) == 1,
                    ) { "无法完成音频写入" }
                    newSongs[track.id] = restoredSong(uri, track, asset.size)
                    copied += asset.size
                }
                library.playlists.mapNotNull { it.artwork }.distinct().forEach { path ->
                    val asset = assets.getValue(path)
                    check(artworkDir.exists() || artworkDir.mkdirs()) { "无法创建封面目录" }
                    val target = File(artworkDir, "${asset.sha256}.image")
                    zip.getInputStream(zip.getEntry(path)).use { input ->
                        target.outputStream().use { output ->
                            val actual = PackageArchive.copyAndHash(input, output, asset.size)
                            require(actual.first == asset.size && actual.second == asset.sha256) { "封面校验失败" }
                        }
                    }
                    artworkPaths[path] = target.absolutePath
                }
                progress(TransferState.Running("保存歌单、收藏和歌词"))
                database.withTransaction {
                    val dao = database.transferDao()
                    val current = database.readTransferSnapshot()
                    val currentSongs = current.songs.associateBy { it.songId }
                    val localSongs = HashMap<String, SongEntity>()
                    library.tracks.forEach { track ->
                        val matched = plan.matches[track.id]
                        val song =
                            if (matched != null) {
                                currentSongs[matched.songId] ?: error("曲库已发生变化，请重新预览导入")
                            } else {
                                val restored = newSongs.getValue(track.id)
                                restored.copy(songId = dao.insertSong(restored))
                            }
                        localSongs[track.id] = song
                        dao.putIdentity(
                            TransferIdentityEntity(
                                TRACK_IDENTITY,
                                track.id,
                                requireNotNull(song.songId),
                                PackageJson.encodeToString(track.sources),
                            ),
                        )
                        // 明确导入的歌曲不受本机扫描筛选规则影响。
                        dao.putIdentity(TransferIdentityEntity("imported_track", track.id, requireNotNull(song.songId)))
                        if (track.favorite && !song.like) database.songDao().likeSongs(requireNotNull(song.songId), true)
                        val existing = current.lyrics[song.mediaStoreId]
                        val localSelection =
                            if (existing?.stored().hasLyricsSelection()) {
                                requireNotNull(existing).stored()
                            } else {
                                plan.localLyrics[track.id] ?: existing?.stored()
                            }
                        val incoming = track.lyrics
                        if (incoming != null && shouldImportLyrics(localSelection, preferImportedLyrics)) {
                            val raw = incoming.asset?.let { PackageArchive.readSmall(zip, it, LibraryPackage.MAX_LYRIC_BYTES) }.orEmpty()
                            database.lyricDao().insertLyric(
                                ExtraInfoEntity(
                                    id = existing?.id ?: 0L,
                                    mediaId = song.mediaStoreId,
                                    lyrics = raw,
                                    delay = incoming.offsetMs,
                                    lyricSourceName = incoming.sourceName,
                                    sourceType = incoming.sourceType,
                                    isManual = incoming.manual,
                                    restoredSnapshot = raw.isNotBlank(),
                                    lyricsSuppressed = incoming.suppressed,
                                ),
                            )
                        }
                    }
                    val existingPlaylists = current.playlists.associateBy { it.playlistId }
                    val playlistIdentities = current.identities.filter { it.kind == PLAYLIST_IDENTITY }.associateBy { it.stableId }
                    library.playlists.forEach { playlist ->
                        val existing = playlistIdentities[playlist.id]?.localId?.let(existingPlaylists::get)
                        val playlistId =
                            existing?.playlistId ?: database.playlistDao().insertPlaylistAndGetId(
                                PlaylistEntity(
                                    playlistName = playlist.name,
                                    createTimestamp = playlist.createdAt,
                                    cover = playlist.artwork?.let(artworkPaths::get),
                                ),
                            )
                        requireNotNull(playlistId)
                        dao.putIdentity(TransferIdentityEntity(PLAYLIST_IDENTITY, playlist.id, playlistId))
                        val oldEntries = current.entries.filter { it.playlistId == playlistId }
                        val oldMediaIds = oldEntries.map { it.mediaId }.toSet()
                        val added = playlist.entries.map { localSongs.getValue(it.trackId).mediaStoreId }.filterNot { it in oldMediaIds }
                        // 保留已有顺序，新曲目按包内顺序追加。
                        val ordered = oldEntries.map { it.mediaId } + added
                        oldEntries.forEachIndexed { index, entry ->
                            database.playlistDao().updateSortOrder(playlistId, entry.mediaId, (ordered.size - index).toLong() * 1_000_000)
                        }
                        database.playlistDao().insertListItems(
                            added.mapIndexed { index, mediaId ->
                                PlaylistSongCrossRefEntity(playlistId, mediaId, sortOrder = (added.size - index).toLong() * 1_000_000)
                            },
                        )
                        database.playlistDao().setNeedUpdate(playlistId)
                    }
                    val allSongs = dao.songs()
                    val affectedAlbums = newSongs.values.map { it.albumId }.toSet()
                    database.albumDao().insertAll(
                        allSongs.filter { it.albumId in affectedAlbums }.groupBy { it.albumId }.map { (id, songs) ->
                            AlbumEntity(id.toString(), songs.first().album, songs.first().artist, numberOfSongs = songs.size)
                        },
                    )
                    dao.putReceipt(TransferReceiptEntity(taskId, library.manifest.archiveId, library.tracks.size, library.playlists.size))
                }
            }
        } catch (e: Exception) {
            withContext(NonCancellable) {
                if (database.transferDao().receipt(taskId) == null) {
                    cleanup(taskId)
                    artworkDir.deleteRecursively()
                }
            }
            throw e
        }
    }

    /** 清理任务目录中可能在进程中断前创建的音频。 */
    @Suppress("DEPRECATION")
    fun cleanup(taskId: String) {
        val collection = MediaStore.setIncludePending(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY))
        val ids = mutableListOf<Long>()
        resolver
            .query(
                collection,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.RELATIVE_PATH} = ? AND ${MediaStore.Audio.Media.OWNER_PACKAGE_NAME} = ?",
                arrayOf(taskFolder(taskId), context.packageName),
                null,
            )?.use { cursor -> while (cursor.moveToNext()) ids.add(cursor.getLong(0)) }
        ids.forEach { resolver.delete(ContentUris.withAppendedId(collection, it), null, null) }
        File(context.filesDir, "library-artwork/$taskId").deleteRecursively()
    }

    private fun restoredSong(
        uri: Uri,
        track: PackageTrack,
        size: Long,
    ): SongEntity {
        var path = ""
        var albumId = 0L
        var modified = 0L
        resolver
            .query(
                uri,
                arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATE_MODIFIED),
                null,
                null,
                null,
            )?.use {
                check(it.moveToFirst()) { "恢复的音频无法访问" }
                path = it.getString(0).orEmpty()
                albumId = it.getLong(1)
                modified = it.getLong(2)
            } ?: error("无法读取恢复的音频信息")
        return SongEntity(
            mediaStoreId = ContentUris.parseId(uri),
            path = path,
            displayName = track.title,
            artist = track.artist,
            size = size,
            like = track.favorite,
            duration = track.durationMs,
            sort = 0,
            sortName = track.title.uppercase(Locale.ROOT),
            mimeType = track.mimeType,
            albumId = albumId,
            album = track.album,
            sampleRate = track.sampleRate,
            bitRate = track.bitRate,
            channels = track.channels,
            digit = track.bitDepth,
            isIgnore = track.ignored,
            dateModified = modified,
            codec = track.codec,
            trackNumber = track.trackNumber,
        )
    }

    private fun taskFolder(taskId: String): String {
        require(taskId.matches(Regex("[a-f0-9-]{36}")))
        return "${Environment.DIRECTORY_MUSIC}/SPICaMusic/Imports/$taskId/"
    }

    private fun safeAudioName(track: PackageTrack): String {
        val extension =
            track.fileName
                .substringAfterLast('.', "")
                .lowercase(Locale.ROOT)
                .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
                ?: android.webkit.MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(track.mimeType) ?: "audio"
        val base =
            track.title
                .replace(Regex("[\\p{Cntrl}/\\\\:*?\"<>|]"), "_")
                .take(100)
                .trim()
                .ifBlank { "audio" }
        return "$base-${track.id.takeLast(8)}.$extension"
    }
}
