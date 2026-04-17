package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    var playlistId: Long? = null,
    var playlistName: String = "自定义播放列表${System.currentTimeMillis()}",
    var cover: String? = null,
    var createTimestamp: Long = System.currentTimeMillis(),
    var playTimes: Int = 0,
    var needUpdate: Boolean = true,
)
