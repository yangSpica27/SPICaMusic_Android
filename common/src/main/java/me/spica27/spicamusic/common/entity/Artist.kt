package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 歌手。封面取自歌手名下的一首「代表曲」本体，而不是按专辑 id 取图：
 * MediaStore 会把无专辑标签的文件按所在目录名并成同一个伪专辑（如 "Music"），
 * 按 albumId 取图会串到别的歌手的歌上。
 */
@Immutable
@Serializable
data class Artist(
    val name: String,
    val songCount: Int,
    val coverAlbumId: Long,
    val coverMediaStoreId: Long = 0L,
)

/**
 * 由同一歌手的歌曲构造 [Artist]：代表曲取 mediaStoreId 最小的一首，
 * 与 SongDao.getArtistsPaging 同一规则，保证从歌手列表、播放页、歌曲菜单进入时头像一致。
 */
fun artistOf(
    name: String,
    songs: List<Song>,
): Artist {
    val cover = songs.minByOrNull { it.mediaStoreId }
    return Artist(
        name = name,
        songCount = songs.size,
        coverAlbumId = cover?.albumId ?: 0L,
        coverMediaStoreId = cover?.mediaStoreId ?: 0L,
    )
}
