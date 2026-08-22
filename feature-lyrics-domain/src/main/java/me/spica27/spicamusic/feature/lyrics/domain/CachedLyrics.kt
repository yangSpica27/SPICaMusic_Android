package me.spica27.spicamusic.feature.lyrics.domain

data class CachedLyrics(
    val mediaId: Long,
    val lyrics: String,
    val delay: Long,
    val lyricSourceName: String,
    val cover: String,
    /** 来源类型名，对应 LyricSourceType（EMBEDDED/LOCAL_FILE/ONLINE/NONE） */
    val sourceType: String,
    /** 手动锁定：为 true 时跳过自动优先级，直接使用这份快照 */
    val isManual: Boolean,
    /** 本地文件来源出处，仅展示用 */
    val sourceUri: String,
)
