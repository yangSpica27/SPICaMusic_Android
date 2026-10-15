package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** 艺人类型：普通艺人、合辑用的群星、标签缺失时的未知艺人 */
enum class ArtistKind {
    NORMAL,
    VARIOUS,
    UNKNOWN,
}

/** 艺人在一首歌里的署名角色 */
enum class ArtistRole {
    ARTIST,
    COMPOSER,
    LYRICIST,
}

/**
 * 应用按文件标签建立的艺人。未知艺人的 [name] 为空，由界面显示本地化文案。
 * 封面取自艺人名下的一首代表曲本体。
 */
@Immutable
@Serializable
data class Artist(
    val artistId: Long,
    val name: String,
    val songCount: Int,
    val coverMediaStoreId: Long = 0L,
    val kind: ArtistKind = ArtistKind.NORMAL,
)

/** 一首歌的一条署名 */
@Immutable
@Serializable
data class ArtistCredit(
    val artist: Artist,
    val role: ArtistRole,
)
