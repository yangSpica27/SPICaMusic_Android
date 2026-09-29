package me.spica27.spicamusic.feature.transfer.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val PackageJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

@Serializable
data class PackageManifest(
    val format: String = "spica-library",
    val version: Int = 1,
    val archiveId: String,
    val createdAt: Long,
    val requiredFeatures: List<String> = listOf("complete-audio", "selected-lyrics"),
    val assets: List<PackageAsset>,
)

@Serializable
data class PackageAsset(
    val path: String,
    val sha256: String,
    val size: Long,
    val kind: String,
)

/** 平台 ID 使用字符串并保留来源作用域，扩展字段随包保存。 */
@Serializable
data class SourceReference(
    val provider: String,
    val itemId: String,
    val instanceScope: String? = null,
    val extensions: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class PackageTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val fileName: String,
    val mimeType: String,
    val audio: String,
    val favorite: Boolean,
    val ignored: Boolean = false,
    val trackNumber: Int = 0,
    val sampleRate: Int = 0,
    val bitRate: Int = 0,
    val channels: Int = 0,
    val bitDepth: Int = 0,
    val codec: String = "",
    val lyrics: PackageLyrics? = null,
    val sources: List<SourceReference> = emptyList(),
)

@Serializable
data class PackageLyrics(
    val asset: String?,
    val format: String,
    val offsetMs: Long,
    val sourceType: String,
    val sourceName: String,
    val manual: Boolean,
    val suppressed: Boolean = false,
)

@Serializable
data class PackagePlaylist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val entries: List<PackagePlaylistEntry>,
    val artwork: String? = null,
)

@Serializable
data class PackagePlaylistEntry(
    val id: String,
    val trackId: String,
)

data class LibraryPackage(
    val manifest: PackageManifest,
    val tracks: List<PackageTrack>,
    val playlists: List<PackagePlaylist>,
) {
    fun validate() {
        require(manifest.format == "spica-library" && manifest.version == 1) { "不支持的完整包格式或版本" }
        require(manifest.requiredFeatures.all { it in setOf("complete-audio", "selected-lyrics", LYRICS_SUPPRESSION) }) { "完整包需要更新版本的应用" }
        require(tracks.none { it.lyrics?.suppressed == true } || LYRICS_SUPPRESSION in manifest.requiredFeatures) {
            "完整包未声明无匹配歌词功能"
        }
        require(validId(manifest.archiveId)) { "完整包标识无效" }
        require(tracks.size <= 100_000 && playlists.size <= 20_000) { "完整包条目过多" }
        require(tracks.map { it.id }.toSet().size == tracks.size) { "完整包包含重复歌曲标识" }
        require(playlists.map { it.id }.toSet().size == playlists.size) { "完整包包含重复歌单标识" }
        val assets = manifest.assets.associateBy { it.path }
        require(assets.size == manifest.assets.size && assets.size <= 300_002) { "完整包包含重复或过多资源" }
        manifest.assets.forEach {
            require(it.sha256.matches(Regex("[a-f0-9]{64}")) && it.size >= 0) { "资源校验信息无效" }
            require(
                it.path in setOf("tracks.json", "playlists.json") ||
                    it.path.matches(Regex("(audio|lyrics|artwork)/[a-f0-9]{64}\\.[a-z0-9]+")),
            ) { "资源路径无效" }
            require(it.kind in setOf("audio", "lyrics", "artwork", "metadata")) { "资源类型无效" }
            require(
                it.size <=
                    when (it.kind) {
                        "metadata" -> MAX_METADATA_BYTES
                        "lyrics" -> MAX_LYRIC_BYTES
                        "artwork" -> MAX_ARTWORK_BYTES
                        else -> MAX_AUDIO_BYTES
                    },
            ) { "资源超过大小限制" }
            require(
                it.kind == "metadata" && it.path in setOf("tracks.json", "playlists.json") || it.path.startsWith("${it.kind}/"),
            ) { "资源类型与路径不符" }
        }
        require(assets["tracks.json"]?.kind == "metadata" && assets["playlists.json"]?.kind == "metadata") { "完整包缺少歌曲或歌单目录" }
        tracks.forEach { track ->
            require(validId(track.id) && track.durationMs >= 0) { "歌曲信息无效" }
            require(assets[track.audio]?.let { it.kind == "audio" && it.size > 0 } == true) { "歌曲缺少完整音频：${track.title}" }
            require(listOf(track.title, track.artist, track.album, track.fileName, track.mimeType).all { it.length <= 16_384 }) { "歌曲信息过长" }
            track.lyrics?.asset?.let { require(assets[it]?.kind == "lyrics") { "歌曲缺少歌词资源" } }
        }
        val trackIds = tracks.map { it.id }.toSet()
        playlists.forEach { playlist ->
            require(validId(playlist.id) && playlist.name.length <= 16_384 && playlist.entries.size <= 100_000) { "歌单信息无效" }
            require(
                playlist.entries
                    .map { it.id }
                    .toSet()
                    .size == playlist.entries.size,
            ) { "歌单包含重复条目标识" }
            // 当前歌单不支持重复曲目，明确报错，避免静默丢失。
            require(
                playlist.entries
                    .map { it.trackId }
                    .toSet()
                    .size == playlist.entries.size,
            ) { "此版本暂不支持同一歌单内的重复歌曲" }
            require(playlist.entries.all { validId(it.id) && it.trackId in trackIds }) { "歌单引用了缺失歌曲" }
            playlist.artwork?.let { require(assets[it]?.kind == "artwork") { "歌单缺少封面资源" } }
        }
    }

    companion object {
        const val LYRICS_SUPPRESSION = "lyrics-suppression"
        const val MAX_METADATA_BYTES = 32L * 1024 * 1024
        const val MAX_LYRIC_BYTES = 8L * 1024 * 1024
        const val MAX_ARTWORK_BYTES = 32L * 1024 * 1024
        const val MAX_AUDIO_BYTES = 32L * 1024 * 1024 * 1024

        private fun validId(value: String) = value.matches(Regex("[a-zA-Z0-9_-]{1,128}"))
    }
}
