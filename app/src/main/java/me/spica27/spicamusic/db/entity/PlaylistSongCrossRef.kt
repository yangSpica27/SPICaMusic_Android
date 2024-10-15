package me.spica27.spicamusic.db.entity

import androidx.room.*


@Entity(primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    @ColumnInfo(index = true)
    val songId: Long
)


data class PlaylistWithSongs(
  @Embedded val playlist: Playlist,
  @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<Song>
)