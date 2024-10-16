package me.spica27.spicamusic.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// 字典工具类
class DataStoreUtil(private val context: Context) {
  companion object {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    // 夜间模式
    val FORCE_DARK_THEME = booleanPreferencesKey("force_dark_theme")

    // 自动扫描
    val AUTO_SCANNER = booleanPreferencesKey("auto_scanner")

    // 自动播放
    val AUTO_PLAY = booleanPreferencesKey("auto_play")
  }

  val getAutoPlay: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[AUTO_PLAY] ?: true
    }.distinctUntilChanged()

  suspend fun saveAutoPlay(value: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[AUTO_PLAY] = value
    }
  }

  val getAutoScanner: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[AUTO_SCANNER] ?: true
    }.distinctUntilChanged()

  suspend fun saveAutoScanner(value: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[AUTO_SCANNER] = value
    }
  }

  val getForceDarkTheme: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[FORCE_DARK_THEME] ?: false
    }.distinctUntilChanged()

  suspend fun saveForceDarkTheme(value: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[FORCE_DARK_THEME] = value
    }
  }


}