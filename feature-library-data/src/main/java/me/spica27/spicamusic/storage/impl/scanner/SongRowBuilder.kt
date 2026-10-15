package me.spica27.spicamusic.storage.impl.scanner

import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.entity.TagStatus
import me.spica27.spicamusic.storage.impl.index.IndexText
import me.spica27.spicamusic.storage.impl.index.TagJson
import me.spica27.spicamusic.storage.impl.index.TagParser

/** MediaStore 提供的文件事实；title / artist / bitRate 只在 TagLib 读取失败时作回退 */
data class MediaFileRow(
    val mediaStoreId: Long,
    val path: String,
    val fileName: String,
    val size: Long,
    val mimeType: String,
    /** DATE_MODIFIED（秒） */
    val dateModified: Long,
    val durationMs: Long,
    val title: String?,
    val artist: String?,
    val bitRate: Int,
)

/** 合并文件事实、TagLib 结果与已有用户状态，生成待写入的歌曲行 */
object SongRowBuilder {
    /** 提取的字段或解析规则变化时 +1，下次扫描重读受影响的文件 */
    const val TAG_VERSION = 1

    private const val MEDIA_STORE_UNKNOWN = "<unknown>"

    /** 时间戳、大小与提取版本都没变才跳过；时间戳为 0（未知）时总是重读 */
    fun isUnchanged(
        file: MediaFileRow,
        existing: SongDao.SongScanInfo?,
    ): Boolean =
        existing != null &&
            file.dateModified != 0L &&
            existing.dateModified == file.dateModified &&
            existing.size == file.size &&
            existing.tagVersion >= TAG_VERSION

    fun build(
        file: MediaFileRow,
        extracted: ExtractedTags,
        existing: SongDao.SongScanInfo?,
        text: IndexText,
        isImported: Boolean = false,
    ): SongEntity {
        val tags = extracted.properties?.let(TagParser::parse)
        val audio = extracted.audio
        val fallbackArtist = file.artist?.trim()?.takeIf { it.isNotEmpty() && it != MEDIA_STORE_UNKNOWN }
        val title =
            tags?.title
                ?: file.title?.trim()?.takeIf { it.isNotEmpty() && it != MEDIA_STORE_UNKNOWN }
                ?: file.fileName.substringBeforeLast('.').ifBlank { file.fileName }
        val bitRate = audio?.bitRate?.takeIf { it > 0 } ?: file.bitRate.coerceAtLeast(0)
        // 只因提取版本升级而重读时文件没变，波形可以保留
        val fileChanged = existing != null && (existing.dateModified != file.dateModified || existing.size != file.size)
        return SongEntity(
            songId = existing?.songId ?: 0,
            mediaStoreId = file.mediaStoreId,
            path = file.path,
            size = file.size,
            mimeType = file.mimeType,
            dateModified = file.dateModified,
            displayName = title,
            artist = tags?.artistDisplay ?: fallbackArtist.orEmpty(),
            // 不回退 MediaStore ALBUM：系统会给没有专辑标签的文件填上目录名
            album = tags?.album.orEmpty(),
            artistTags = TagJson.encode(tags?.artists ?: listOfNotNull(fallbackArtist)),
            albumArtistTags = TagJson.encode(tags?.albumArtists.orEmpty()),
            composerTags = TagJson.encode(tags?.composers.orEmpty()),
            lyricistTags = TagJson.encode(tags?.lyricists.orEmpty()),
            trackNumber = tags?.trackNumber ?: 0,
            discNumber = tags?.discNumber ?: 0,
            year = tags?.year ?: 0,
            isCompilation = tags?.isCompilation ?: false,
            duration = audio?.durationMs?.takeIf { it > 0 } ?: file.durationMs.coerceAtLeast(0),
            sampleRate = audio?.sampleRate?.coerceAtLeast(0) ?: 0,
            bitRate = bitRate,
            channels = audio?.channels?.coerceAtLeast(0) ?: 0,
            // taglib 1.0.6 的 AudioProperties 不提供位深
            digit = 0,
            codec = resolveCodec(file.mimeType, bitRate),
            tagStatus = if (tags != null) TagStatus.TAGLIB else TagStatus.FALLBACK,
            tagVersion = TAG_VERSION,
            albumId = existing?.albumId,
            sortName = text.sortName(title),
            like = existing?.like ?: false,
            isIgnore = existing?.isIgnore ?: false,
            isImported = isImported || existing?.isImported == true,
            sort = existing?.sort ?: 0,
            // 文件内容变化后旧波形失效
            waveformData = if (fileChanged) null else existing?.waveformData,
        )
    }

    fun resolveCodec(
        mimeType: String,
        bitRate: Int,
    ): String {
        val codec =
            when {
                mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                mimeType.contains("aac", ignoreCase = true) -> "AAC"
                mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                mimeType.contains("alac", ignoreCase = true) -> "ALAC"
                mimeType.contains("opus", ignoreCase = true) -> "Opus"
                mimeType.contains("vorbis", ignoreCase = true) -> "Vorbis"
                mimeType.contains("wav", ignoreCase = true) -> "WAV"
                mimeType.contains("m4a", ignoreCase = true) -> "M4A"
                else -> mimeType.substringAfter("/").uppercase()
            }
        return if (codec == "M4A") {
            if (bitRate >= 700_000) "ALAC" else "AAC"
        } else {
            codec
        }
    }
}
