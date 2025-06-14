package me.spica27.spicamusic.db.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.spica27.spicamusic.utils.toAudioUri
import me.spica27.spicamusic.utils.toCoverUri

@kotlinx.parcelize.Parcelize
@Entity(
  indices = [
    Index("displayName"),
    Index("mediaStoreId", unique = true),
  ]
)
data class Song constructor(
  @ColumnInfo(index = true)
  @PrimaryKey(autoGenerate = true)
  var songId: Long? = null,
  var mediaStoreId: Long,
  var path: String,
  var displayName: String,
  var artist: String,
  var size: Long,
  var like: Boolean,
  val duration: Long,
  var sort: Int,
  var playTimes: Int,
  var lastPlayTime: Int,
  var mimeType: String,
  var albumId: Long,
  var sampleRate: Int, // 采样率
  var bitRate: Int, // 比特率
  var channels: Int, // 声道数
  var digit: Int // 位深度
) : Parcelable {

  fun getFormatMimeType(): String {
    if (mimeType.contains("audio/")) {
      return mimeType.replace("audio/", "")
    }
    return mimeType
  }

  fun getCoverUri(): Uri {
    return albumId.toCoverUri()
  }

  fun getSongUri(): Uri {
    return mediaStoreId.toAudioUri()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Song

    if (songId != other.songId) return false
    if (mediaStoreId != other.mediaStoreId) return false
    if (path != other.path) return false
    if (displayName != other.displayName) return false
    if (artist != other.artist) return false
    if (size != other.size) return false
    if (like != other.like) return false
    if (duration != other.duration) return false
    if (sort != other.sort) return false
    if (playTimes != other.playTimes) return false
    if (lastPlayTime != other.lastPlayTime) return false

    return true
  }

  override fun hashCode(): Int {
    var result = songId?.hashCode() ?: 0
    result = 31 * result + mediaStoreId.hashCode()
    result = 31 * result + path.hashCode()
    result = 31 * result + displayName.hashCode()
    result = 31 * result + artist.hashCode()
    result = 31 * result + size.hashCode()
    result = 31 * result + like.hashCode()
    result = 31 * result + duration.hashCode()
    result = 31 * result + sort
    result = 31 * result + playTimes
    result = 31 * result + lastPlayTime
    return result
  }

  override fun toString(): String {
    return "Song(songId=$songId, mediaStoreId=$mediaStoreId, path='$path', displayName='$displayName', artist='$artist', size=$size, like=$like, duration=$duration, sort=$sort, playTimes=$playTimes, lastPlayTime=$lastPlayTime)"
  }


}