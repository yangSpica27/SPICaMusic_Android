package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 歌单与歌曲关联实体
 */
@Parcelize
@Serializable
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val insertTime: Long = System.currentTimeMillis(),
) : Parcelable

/**
 * 歌单详情（包含歌曲列表）
 */
@Parcelize
@Serializable
data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>,
) : Parcelable
