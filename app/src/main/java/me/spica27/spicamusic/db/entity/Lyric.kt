package me.spica27.spicamusic.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Lyric(
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0,
  var mediaId: Long = 0,
  var lyrics: String = "",
  var cover: String= "",
  var delay: Long = 0
)