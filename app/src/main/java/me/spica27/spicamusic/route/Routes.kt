package me.spica27.spicamusic.route

import android.net.Uri
import android.os.Parcelable
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import kotlinx.serialization.Serializable
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.utils.MoshiUtil

/// App的导航

object Routes {


  @Serializable
  data object Splash

  @Serializable
  data object Main

  @Serializable
  data class AddSong(val playlistId: Long)

  @Serializable
  data class PlaylistDetail(val playlistId: Long)


  @Serializable
  data class Translate(val pointX: Float, val pointY: Float, val fromLight: Boolean)

  /**
   * 搜索
   */
  @Serializable
  data object SearchAll

  /**
   * 收藏列表
   */
  @Serializable
  data object LikeList

  /**
   * 最近播放列表
   */
  @Serializable
  data object RecentlyList

  @Serializable
  data object EQ

  @Serializable
  data object Scanner

  @Serializable
  data class PlayListItemDetail(val playlistId: Long, val songId: Long)

  @Serializable
  data object AgreePrivacy

  @Serializable
  data object IgnoreList

  /**
   * 歌词搜索
   */
  @Serializable
  data class LyricsSearch(val song: Song)


  inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
  ) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {


    override fun put(bundle: SavedState, key: String, value: T) {
      bundle.putParcelable(key, value)
    }

    override fun get(bundle: SavedState, key: String): T? {
      return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): T = MoshiUtil.fromJson(Uri.decode(value))!!

    override fun serializeAsValue(value: T): String {
      return Uri.encode(
        MoshiUtil.toJson(value)
      )
    }
  }

}


