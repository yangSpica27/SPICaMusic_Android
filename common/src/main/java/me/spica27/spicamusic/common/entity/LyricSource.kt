package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable

/** 可供用户选择的歌词来源类型。 */
enum class LyricSourceType {
    EMBEDDED,
    LOCAL_FILE,
    ONLINE,
    NONE,
}

/**
 * 与具体获取方式无关的歌词候选。
 * rawLyrics 保留原始文本，解析和展示由上层统一完成。
 */
@Immutable
sealed class LyricSource(
    val type: LyricSourceType,
    open val rawLyrics: String,
    open val title: String,
    open val subtitle: String,
    open val stableKey: String,
) {
    @Immutable
    data class Embedded(
        override val rawLyrics: String,
        override val title: String = "歌曲内嵌歌词",
        override val subtitle: String = "来自音频文件",
        override val stableKey: String = "embedded",
    ) : LyricSource(
        type = LyricSourceType.EMBEDDED,
        rawLyrics = rawLyrics,
        title = title,
        subtitle = subtitle,
        stableKey = stableKey,
    )

    @Immutable
    data class LocalFile(
        val uri: String,
        val fileName: String,
        override val rawLyrics: String,
        override val title: String = fileName,
        override val subtitle: String = "本地歌词文件",
        override val stableKey: String = "local:$uri",
    ) : LyricSource(
        type = LyricSourceType.LOCAL_FILE,
        rawLyrics = rawLyrics,
        title = title,
        subtitle = subtitle,
        stableKey = stableKey,
    )

    @Immutable
    data class Online(
        val id: Long,
        override val title: String,
        override val subtitle: String,
        val album: String = "",
        val albumArt: String = "",
        val duration: Int = 0,
        override val rawLyrics: String,
        override val stableKey: String = "online:$id",
    ) : LyricSource(
        type = LyricSourceType.ONLINE,
        rawLyrics = rawLyrics,
        title = title,
        subtitle = subtitle,
        stableKey = stableKey,
    )
}
