package me.spica27.spicamusic.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.App
import me.spica27.spicamusic.dsp.Equalizer
import me.spica27.spicamusic.dsp.EqualizerBand
import me.spica27.spicamusic.dsp.NyquistBand
import me.spica27.spicamusic.dsp.toNyquistBand

// 字典工具类
class DataStoreUtil(private val context: Context = App.getInstance()) {
  companion object {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    // 夜间模式
    val FORCE_DARK_THEME = booleanPreferencesKey("force_dark_theme")

    // 自动扫描
    val AUTO_SCANNER = booleanPreferencesKey("auto_scanner")

    // 自动播放
    val AUTO_PLAY = booleanPreferencesKey("auto_play")

    // 响度增益
    val REPLAY_GAIN = intPreferencesKey("REPLAY_GAIN")


  }


  suspend fun saveEq(
    eq: List<EqualizerBand>
  ) {
    context.dataStore.edit { preferences ->
      for (equalizerBand in eq) {
        preferences[doublePreferencesKey("EQ-${equalizerBand.centerFrequency}")] =
          equalizerBand.gain
      }
    }
  }

  fun getEqualizerBand(): Flow<List<EqualizerBand>> {
    return context.dataStore.data.map { preferences ->
      Equalizer.centerFrequency.map {
        val gain: Double = preferences[doublePreferencesKey("EQ-${it}")] ?: 0.0
        EqualizerBand(
          it,
          gain
        )
      }
    }.distinctUntilChanged()
  }


  fun getNyquistBand(): Flow<List<NyquistBand>> {
    return context.dataStore.data.map { preferences ->
      Equalizer.centerFrequency.map {
        val gain: Double = preferences[doublePreferencesKey("EQ-${it}")] ?: 0.0
        EqualizerBand(
          it,
          gain
        ).toNyquistBand()
      }
    }.distinctUntilChanged()
  }


  suspend fun saveReplayGain(
    replayGain: Int
  ) {
    context.dataStore.edit { preferences ->
      preferences[REPLAY_GAIN] = replayGain
    }
  }

  val getReplayGain: Flow<Int>
    get() = context.dataStore.data.map { preferences ->
      preferences[REPLAY_GAIN] ?: 0
    }.distinctUntilChanged()


  val isForceDarkTheme: Boolean
    get() = runBlocking { context.dataStore.data.first()[FORCE_DARK_THEME] ?: false }

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