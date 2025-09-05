package me.spica27.spicamusic.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Stable
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    var playlistId: Long? = null,
    var playlistName: String = "自定义播放列表${Date().time}",
    var cover: String? = null,
    var createTimestamp: Long = Date().time,
    var playTimes: Int = 0,
    var needUpdate: Boolean = true, // 标记用于是否需要更新封面
) {
    //    @Ignore
//    constructor():this(0,"",null)
}
