package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
    val year: Int = 0,
    val numberOfSongs: Int = 0,
    val dateModified: Long = System.currentTimeMillis()
)