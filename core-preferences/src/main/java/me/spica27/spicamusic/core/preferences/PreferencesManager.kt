package me.spica27.spicamusic.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PreferencesManager(
    private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_COLOR_STYLE = stringPreferencesKey("theme_color_style")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DYNAMIC_SPECTRUM_BACKGROUND = stringPreferencesKey("dynamic_spectrum_background")
        val DYNAMIC_COVER_TYPE = stringPreferencesKey("dynamic_cover_type")
        val PROGRESS_BAR_STYLE = stringPreferencesKey("progress_bar_style")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        val REVERB_LEVEL = stringPreferencesKey("reverb_level")
        val REVERB_ROOM_SIZE = stringPreferencesKey("reverb_room_size")
        val LOUDNESS_NORMALIZATION_ENABLED = booleanPreferencesKey("loudness_normalization_enabled")

        // 目标响度（LUFS）。本项目 float 一律以字符串存储，见 getFloat/setFloat
        val LOUDNESS_TARGET_LUFS = stringPreferencesKey("loudness_target_lufs")
        val SCAN_MIN_DURATION_SEC = stringPreferencesKey("scan_min_duration_sec")
        val SCAN_MIN_FILE_SIZE_KB = stringPreferencesKey("scan_min_file_size_kb")
        val SCAN_ENABLED_FORMATS = stringPreferencesKey("scan_enabled_formats")
        val SCAN_LAST_COMPLETED_AT = stringPreferencesKey("scan_last_completed_at")

        // 已完成补扫的扫描 schema 版本号（用于新增字段时的启动静默重扫）
        val SCAN_SCHEMA_VERSION = intPreferencesKey("scan_schema_version")
    }

    fun getBoolean(
        key: Preferences.Key<Boolean>,
        defaultValue: Boolean = false,
    ): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.distinctUntilChanged()

    suspend fun setBoolean(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getInt(
        key: Preferences.Key<Int>,
        defaultValue: Int = 0,
    ): Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.distinctUntilChanged()

    suspend fun setInt(
        key: Preferences.Key<Int>,
        value: Int,
    ) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getString(
        key: Preferences.Key<String>,
        defaultValue: String = "",
    ): Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }

    suspend fun setString(
        key: Preferences.Key<String>,
        value: String,
    ) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getFloat(
        key: Preferences.Key<String>,
        defaultValue: Float = 0f,
    ): Flow<Float> =
        context.dataStore.data.map { preferences ->
            preferences[key]?.toFloatOrNull() ?: defaultValue
        }.distinctUntilChanged()

    suspend fun setFloat(
        key: Preferences.Key<String>,
        value: Float,
    ) {
        context.dataStore.edit { preferences ->
            preferences[key] = value.toString()
        }
    }

    fun getFloatList(
        key: Preferences.Key<String>,
        defaultValue: List<Float> = emptyList(),
    ): Flow<List<Float>> =
        context.dataStore.data.map { preferences ->
            val serialized = preferences[key]
            if (serialized.isNullOrEmpty()) {
                defaultValue
            } else {
                serialized.split(",").mapNotNull { it.toFloatOrNull() }.ifEmpty { defaultValue }
            }
        }

    suspend fun setFloatList(
        key: Preferences.Key<String>,
        value: List<Float>,
    ) {
        context.dataStore.edit { preferences ->
            preferences[key] = value.joinToString(",")
        }
    }
}
