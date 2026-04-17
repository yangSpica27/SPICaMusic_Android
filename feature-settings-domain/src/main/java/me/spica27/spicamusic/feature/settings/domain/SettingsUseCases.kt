package me.spica27.spicamusic.feature.settings.domain

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.core.preferences.PreferencesManager

class SettingsUseCases(
    private val preferencesManager: PreferencesManager,
) {
    object Keys {
        val DARK_MODE = PreferencesManager.Keys.DARK_MODE
        val KEEP_SCREEN_ON = PreferencesManager.Keys.KEEP_SCREEN_ON
        val DYNAMIC_SPECTRUM_BACKGROUND = PreferencesManager.Keys.DYNAMIC_SPECTRUM_BACKGROUND
        val DYNAMIC_COVER_TYPE = PreferencesManager.Keys.DYNAMIC_COVER_TYPE
        val EQ_ENABLED = PreferencesManager.Keys.EQ_ENABLED
        val EQ_BANDS = PreferencesManager.Keys.EQ_BANDS
        val REVERB_ENABLED = PreferencesManager.Keys.REVERB_ENABLED
        val REVERB_LEVEL = PreferencesManager.Keys.REVERB_LEVEL
        val REVERB_ROOM_SIZE = PreferencesManager.Keys.REVERB_ROOM_SIZE
    }

    fun getBoolean(
        key: Preferences.Key<Boolean>,
        defaultValue: Boolean = false,
    ): Flow<Boolean> = preferencesManager.getBoolean(key, defaultValue)

    suspend fun setBoolean(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        preferencesManager.setBoolean(key, value)
    }

    fun getString(
        key: Preferences.Key<String>,
        defaultValue: String = "",
    ): Flow<String> = preferencesManager.getString(key, defaultValue)

    suspend fun setString(
        key: Preferences.Key<String>,
        value: String,
    ) {
        preferencesManager.setString(key, value)
    }

    fun getFloat(
        key: Preferences.Key<String>,
        defaultValue: Float = 0f,
    ): Flow<Float> = preferencesManager.getFloat(key, defaultValue)

    suspend fun setFloat(
        key: Preferences.Key<String>,
        value: Float,
    ) {
        preferencesManager.setFloat(key, value)
    }

    fun getFloatList(
        key: Preferences.Key<String>,
        defaultValue: List<Float> = emptyList(),
    ): Flow<List<Float>> = preferencesManager.getFloatList(key, defaultValue)

    suspend fun setFloatList(
        key: Preferences.Key<String>,
        value: List<Float>,
    ) {
        preferencesManager.setFloatList(key, value)
    }
}
