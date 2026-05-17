package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation

@Entity(tableName = "PlaylistSongCrossRef", primaryKeys = ["playlistId", "mediaId"])
data class PlaylistSongCrossRefEntity(
    val playlistId: Long,
    @ColumnInfo(index = true)
    val mediaId: Long,
    val insertTime: Long = System.currentTimeMillis(),
)

data class PlaylistWithSongsEntity(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "mediaStoreId",
        associateBy = Junction(
            PlaylistSongCrossRefEntity::class,
            parentColumn = "playlistId",
            entityColumn = "mediaId",
        ),
    )
    val songs: List<SongEntity>,
)
