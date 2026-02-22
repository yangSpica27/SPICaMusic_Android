package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "PlayHistory",
    indices = [
        Index("mediaId"),
        Index("time"),
        Index(value = ["mediaId", "time"]),
        Index("actionType"),
        Index("contextType"),
        Index("sessionId"),
        Index("isCompleted"),
    ],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var mediaId: Long = 0,
    var title: String = "",
    var artist: String = "",
    var album: String = "",
    // total duration of the track in milliseconds
    var duration: Long = 0L,
    // how long the user actually listened in this event (ms)
    var playedDuration: Long = 0L,
    // last playback position in milliseconds
    var position: Long = 0L,
    // action type: 0=play,1=pause,2=skip,3=complete,4=like,...
    var actionType: Int = 0,
    // context information for where the playback started (playlist, album, radio, search...)
    var contextType: String = "",
    var contextId: Long = 0,
    // grouping id for the user session
    var sessionId: String = "",
    // device identifier (optional)
    var deviceId: String = "",
    // source label for recommendations or routing
    var source: String = "",
    // whether the track was listened to sufficiently to be considered completed
    var isCompleted: Boolean = false,
    // extensible JSON blob for additional metadata
    var extra: String = "",
    // event timestamp
    var time: Long = System.currentTimeMillis(),
)
