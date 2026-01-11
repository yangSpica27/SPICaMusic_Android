package me.spica27.spicamusic.player.impl.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.storage.api.ISongRepository
import org.koin.java.KoinJavaComponent.getKoin

class PlayerKVUtils(
    context: Context,
) {
    private val sharedPreferences = context.getSharedPreferences("player", Context.MODE_PRIVATE)

    private val songRepository = getKoin().get<ISongRepository>()

    companion object {
      private const val KEY_HISTORY_IDS = "history_ids"
      private const val KEY_HISTORY_POSITION = "history_position"
      private const val KEY_PLAY_MODE = "play_mode"
    }

    /**
     * 历史播放的id
     */
    fun setHistoryIds(ids: List<Long>) {
        sharedPreferences.edit { putString("history_ids", ids.joinToString(",")) }
    }

    @WorkerThread
    suspend fun getHistoryItems(): List<Song> =
        getHistoryIds().mapNotNull {
            songRepository.getSongByMediaStoreId(it)
        }

    /**
     * 获取历史播放的id
     */
    fun getHistoryIds(): List<Long> {
        val ids = sharedPreferences.getString(KEY_HISTORY_IDS, "")
        return ids?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    /**
     * 播放到第一个
     */
    fun setHistoryPosition(position: Int) {
        sharedPreferences.edit { putInt(KEY_HISTORY_POSITION, position) }
    }

    /**
     * 设置播放的到第几个的index到缓存
     */
    fun getHistoryPosition(position: Int) {
        sharedPreferences.getInt(KEY_HISTORY_POSITION, 0)
    }

    /**
     * 播放模式
     */
    fun setPlayMode(mode: String) {
        sharedPreferences.edit { putString(KEY_PLAY_MODE, mode) }
    }

    /**
     * 获取播放模式
     */
    fun getPlayMode(): String = sharedPreferences.getString(KEY_PLAY_MODE, null) ?: PlayMode.LOOP.name

    fun getPlayModeFlow(): Flow<PlayMode> =
        callbackFlow {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == KEY_PLAY_MODE) {
                        trySend(parsePlayMode(getPlayMode()))
                    }
                }
            if (sharedPreferences.contains(KEY_PLAY_MODE)) {
                trySend(parsePlayMode(getPlayMode()))
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }.buffer(Channel.UNLIMITED)

    private fun parsePlayMode(mode: String): PlayMode =
        when (mode) {
            "LOOP" -> PlayMode.LOOP
            "SHUFFLE" -> PlayMode.SHUFFLE
            else -> PlayMode.LIST
        }
}
