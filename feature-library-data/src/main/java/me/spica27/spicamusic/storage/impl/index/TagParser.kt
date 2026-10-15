package me.spica27.spicamusic.storage.impl.index

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 从 TagLib propertyMap 解析出的标签。列表字段保留拆分前的原始值。 */
data class ParsedTags(
    val title: String?,
    /** 艺人显示串：标签原文，多个原生值以 " / " 连接 */
    val artistDisplay: String,
    val artists: List<String>,
    val album: String,
    val albumArtists: List<String>,
    val composers: List<String>,
    val lyricists: List<String>,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val isCompilation: Boolean,
)

object TagParser {
    private val NUMBER = Regex("^\\s*(\\d+)")
    private val YEAR = Regex("(\\d{4})")

    fun parse(properties: Map<String, List<String>>): ParsedTags {
        /** 按顺序取第一个有非空值的字段 */
        fun values(vararg keys: String): List<String> =
            keys.firstNotNullOfOrNull { key ->
                properties[key]?.map(String::trim)?.filter(String::isNotEmpty)?.takeIf { it.isNotEmpty() }
            }.orEmpty()

        fun first(vararg keys: String): String? = values(*keys).firstOrNull()

        // 同一格式里 ARTISTS / ALBUMARTISTS 是规范的多值字段，优先于单值字段
        val artist = values("ARTIST")
        val artists = values("ARTISTS").ifEmpty { artist }
        val album = first("ALBUM").orEmpty()
        val discFromTag = parseNumber(first("DISCNUMBER"))
        return ParsedTags(
            title = first("TITLE"),
            artistDisplay = artist.ifEmpty { artists }.joinToString(" / "),
            artists = artists,
            album = album,
            albumArtists = values("ALBUMARTISTS").ifEmpty { values("ALBUMARTIST", "ALBUM ARTIST") },
            composers = values("COMPOSER"),
            lyricists = values("LYRICIST"),
            trackNumber = parseNumber(first("TRACKNUMBER")),
            discNumber = discFromTag.takeIf { it > 0 } ?: AlbumTitles.discMarker(album),
            year = parseYear(first("DATE", "YEAR", "ORIGINALDATE")),
            isCompilation = first("COMPILATION")?.lowercase() in setOf("1", "true", "yes"),
        )
    }

    /** "3/12" → 3；无法解析为 0 */
    fun parseNumber(value: String?): Int =
        value
            ?.let { NUMBER.find(it)?.groupValues?.get(1) }
            ?.toIntOrNull()
            ?.takeIf { it in 1..9999 }
            ?: 0

    fun parseYear(value: String?): Int =
        value
            ?.let { YEAR.find(it)?.groupValues?.get(1) }
            ?.toIntOrNull()
            ?.takeIf { it in 1000..2999 }
            ?: 0
}

/** 专辑名结尾的碟号标记，如 "(Disc 1)"、"[CD2]"、"- CD 2"、"Disc2" */
object AlbumTitles {
    private val DISC_MARKER =
        Regex("(?i)\\s*[-–—:]?\\s*[(\\[（【]?\\s*(?<![\\p{L}\\p{N}])(?:disc|disk|cd)\\s*\\.?\\s*(\\d{1,2})\\s*[)\\]）】]?\\s*$")

    fun discMarker(title: String): Int {
        val match = DISC_MARKER.find(title) ?: return 0
        if (title.substring(0, match.range.first).isBlank()) return 0
        return match.groupValues[1].toIntOrNull() ?: 0
    }

    /** 去掉碟号标记后的标题；去掉后为空则保留原文 */
    fun strip(title: String): String {
        val trimmed = title.trim()
        val match = DISC_MARKER.find(trimmed) ?: return trimmed
        return trimmed.substring(0, match.range.first).trim().ifEmpty { trimmed }
    }
}

/** 原始多值标签在 Song 表中以 JSON 字符串数组保存 */
object TagJson {
    private val serializer = ListSerializer(String.serializer())

    fun encode(values: List<String>): String = Json.encodeToString(serializer, values)

    fun decode(json: String): List<String> = runCatching { Json.decodeFromString(serializer, json) }.getOrDefault(emptyList())
}
