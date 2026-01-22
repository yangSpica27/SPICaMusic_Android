package me.spcia.lyric_core

import com.skydoves.sandwich.getOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import retrofit2.Retrofit
import timber.log.Timber

class ApiClient(
  retrofit: Retrofit
) {

  val api: Api by lazy {
    retrofit
      .create(Api::class.java)
  }

  suspend fun fetchExtInfo(displayName: String, artist: String?) = withContext(Dispatchers.IO) {
    Timber.tag("ApiClient").e("Fetching extra info for: $displayName - $artist")
    val extraInfoEntity = ExtraInfoEntity()
    val searchResponse = api.fetchMusicInfo("${displayName};${artist}").getOrThrow()
    Timber.e("Search Response: $searchResponse")
    if (searchResponse.code != 200) {
      return@withContext null
    }
    extraInfoEntity.cover = searchResponse.result.songs.firstOrNull { it.name == displayName }
      ?.album?.artist?.picUrl ?: ""
    val songId =
      searchResponse.result.songs.firstOrNull { it.name == displayName }?.id
        ?: searchResponse.result.songs.firstOrNull()?.id
        ?: return@withContext extraInfoEntity
    val lyricResponse = api.fetchLyric(songId).getOrThrow()
    Timber.e("Lyric Response: $lyricResponse")
    lyricResponse.lrc?.lyric?.let {
      extraInfoEntity.lyrics = it
    }
    return@withContext extraInfoEntity
  }


}