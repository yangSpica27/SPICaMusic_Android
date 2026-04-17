package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation

@Entity(tableName = "PlaylistSongCrossRef", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRefEntity(
    val playlistId: Long,
    @ColumnInfo(index = true)
    val songId: Long,
    val insertTime: Long = System.currentTimeMillis(),
)

data class PlaylistWithSongsEntity(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRefEntity::class),
    )
    val songs: List<SongEntity>,
)
