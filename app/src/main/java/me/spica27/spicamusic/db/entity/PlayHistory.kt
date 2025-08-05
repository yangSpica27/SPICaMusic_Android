package me.spica27.spicamusic.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class PlayHistory(
  @PrimaryKey(autoGenerate = true)
  var id: Long = 0,
  var mediaId: Long = 0,
  var title: String = "",
  var artist: String = "",
  var album: String = "",
  var time: Long = System.currentTimeMillis(),
)