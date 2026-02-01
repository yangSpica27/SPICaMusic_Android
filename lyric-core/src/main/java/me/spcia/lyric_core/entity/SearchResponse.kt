package me.spcia.lyric_core.entity


import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class SearchResponse(
    val code: Int,
    val result: Result
) {
    @Keep
    data class Result(
        val hasMore: Boolean,
        val songCount: Int,
        val songs: List<Song>
    ) {
        @Keep
        data class Song(
            val album: Album,
            val artists: List<Artist>,
            val copyrightId: Int,
            val duration: Int,
            val fee: Int,
            val ftype: Int,
            val id: Long,
            val mark: Long,
            val mvid: Int,
            val name: String,
            val rUrl: String?,
            val rtype: Int,
            val status: Int,
            val transNames: List<String>?
        ) {
            @Keep
            data class Album(
                val artist: Artist,
                val copyrightId: Int,
                val id: Int,
                val mark: Int,
                val name: String,
                val picId: Long,
                val publishTime: Long,
                val size: Int,
                val status: Int,
                val transNames: List<String>?
            )

            @Keep
            data class Artist(
              val albumSize: Int,
              val appendRecText: String?,
              val fansGroup: String?,
              val fansSize: String?,
              val id: Int,
              val img1v1: Int,
              val img1v1Url: String,
              val musicSize: Int,
              val name: String,
              val picId: Int,
              val picUrl: String?,
              val recommendText: String?,
              val trans: String?
            )
        }
    }
}