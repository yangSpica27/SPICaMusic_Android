package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Lyric")
data class LyricEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var mediaId: Long = 0,
    var lyrics: String = "",
    var cover: String = "",
    var delay: Long = 0,
)
