package me.spica27.spicamusic.media.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.media.common.PlayMode
import org.koin.java.KoinJavaComponent.getKoin

internal class PlayerKVUtils(
    context: Context,
) {
    private val sharedPreferences = context.getSharedPreferences("player", Context.MODE_PRIVATE)

    private val songDao = getKoin().get<SongDao>()

    companion object {
        private val KEY_HISTORY_IDS = "history_ids"
        private val KEY_HISTORY_POSITION = "history_position"
        private val KEY_PLAY_MODE = "play_mode"
    }

    /**
     * 历史播放的id
     */
    fun setHistoryIds(ids: List<Long>) {
        sharedPreferences.edit { putString("history_ids", ids.joinToString(",")) }
    }

    @WorkerThread
    fun getHistoryItems(): List<Song> =
        getHistoryIds().mapNotNull {
            songDao.getSongWithMediaStoreId(it)
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
                        trySend(PlayMode.from(getPlayMode()))
                    }
                }
            if (sharedPreferences.contains(KEY_PLAY_MODE)) {
                trySend(PlayMode.from(getPlayMode()))
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }.buffer(Channel.UNLIMITED)
}
