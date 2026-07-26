package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * 可收录的音频格式目录。
 * key 持久化到偏好设置；extensions 用于 SAF 目录遍历过滤；mimeTypes 用于 MediaStore 行过滤。
 */
enum class ScanFormat(
    val key: String,
    val extensions: Set<String>,
    val mimeTypes: Set<String>,
) {
    MP3("mp3", setOf("mp3"), setOf("audio/mpeg", "audio/mp3")),
    M4A("m4a", setOf("m4a"), setOf("audio/mp4", "audio/x-m4a", "audio/alac")),
    AAC("aac", setOf("aac"), setOf("audio/aac")),
    FLAC("flac", setOf("flac"), setOf("audio/flac", "audio/x-flac")),
    OGG("ogg", setOf("ogg"), setOf("audio/ogg", "audio/vorbis")),
    OPUS("opus", setOf("opus"), setOf("audio/opus")),
    WAV("wav", setOf("wav"), setOf("audio/wav", "audio/x-wav", "audio/vnd.wave")),
    AIFF("aiff", setOf("aiff", "aif"), setOf("audio/x-aiff", "audio/aiff")),
    ;

    companion object {
        val allKeys: Set<String> = entries.map { it.key }.toSet()

        fun fromKeys(keys: Set<String>): Set<ScanFormat> = entries.filter { it.key in keys }.toSet()
    }
}

/**
 * 扫描规则：决定哪些音频会被收录进曲库。
 * 对全量扫描、MediaStore 观察者触发的局部同步和额外目录扫描统一生效。
 */
data class ScanRules(
    val minDurationMs: Long,
    val minFileSizeBytes: Long,
    val enabledFormatKeys: Set<String>,
) {
    fun allowedMimeTypes(): Set<String> =
        ScanFormat.fromKeys(enabledFormatKeys).flatMapTo(mutableSetOf()) { it.mimeTypes }

    fun allowedExtensions(): Set<String> =
        ScanFormat.fromKeys(enabledFormatKeys).flatMapTo(mutableSetOf()) { it.extensions }

    companion object {
        const val DEFAULT_MIN_DURATION_SEC = 10

        val DEFAULT =
            ScanRules(
                minDurationMs = DEFAULT_MIN_DURATION_SEC * 1000L,
                minFileSizeBytes = 0L,
                enabledFormatKeys = ScanFormat.allKeys,
            )
    }
}

interface IScanRulesRepository {
    fun getRulesFlow(): Flow<ScanRules>

    suspend fun getRulesSync(): ScanRules

    suspend fun setMinDurationSec(seconds: Int)

    suspend fun setMinFileSizeKb(kb: Int)

    suspend fun setEnabledFormats(keys: Set<String>)
}
