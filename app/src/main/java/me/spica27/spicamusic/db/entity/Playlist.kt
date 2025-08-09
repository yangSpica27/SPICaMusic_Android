package me.spica27.spicamusic.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Playlist(
  @PrimaryKey(autoGenerate = true)
  var playlistId: Long? = null,
  var playlistName: String = "自定义播放列表${Date().time}",
  var cover: String? = null,
  var createTimestamp: Long = Date().time,
  var playTimes: Int = 0,
) {

//    @Ignore
//    constructor():this(0,"",null)

}