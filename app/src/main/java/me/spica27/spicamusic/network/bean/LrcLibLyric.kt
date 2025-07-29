package me.spica27.spicamusic.network.bean

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID


@JsonClass(generateAdapter = true)
data class LrcLibLyric(
  @Json(name = "albumName")
  val albumName: String?,
  @Json(name = "artistName")
  val artistName: String?,
  @Json(name = "duration")
  val duration: Double,
  @Json(name = "id")
  val id: Int,
  @Json(name = "instrumental")
  val instrumental: Boolean,
  @Json(name = "name")
  val name: String,
  @Json(name = "plainLyrics")
  val plainLyrics: String?,
  @Json(name = "syncedLyrics")
  val syncedLyrics: String?,
  @Json(name = "trackName")
  val trackName: String?
) {


  fun toLyricResponse(): LyricResponse {
    return LyricResponse(
      id = UUID.randomUUID().toString(),
      album = albumName,
      artist = artistName,
      cover = "",
      lyrics = syncedLyrics?:"",
      title = name,
    )
  }

}


