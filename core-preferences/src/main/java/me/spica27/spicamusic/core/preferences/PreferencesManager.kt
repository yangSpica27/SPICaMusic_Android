package me.spica27.spicamusic.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class PreferencesManager(
    private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    /**
     * 仅用于首帧渲染所需的小型同步缓存。
     *
     * DataStore 仍然负责正式设置；这里保存播放器上一次封面 URI 与主题色，避免冷启动时
     * 先使用默认蓝色、随后再解码封面并重建整套动态色板造成明显卡顿和闪色。
     */
    private val renderCache by lazy {
        context.getSharedPreferences("render_cache", Context.MODE_PRIVATE)
    }

    object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_COLOR_STYLE = stringPreferencesKey("theme_color_style")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DYNAMIC_SPECTRUM_BACKGROUND = stringPreferencesKey("dynamic_spectrum_background")
        val DYNAMIC_COVER_TYPE = stringPreferencesKey("dynamic_cover_type")
        val PROGRESS_BAR_STYLE = stringPreferencesKey("progress_bar_style")
        val WAVY_PROGRESS_DEFAULT_APPLIED = booleanPreferencesKey("wavy_progress_default_applied")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        val REVERB_LEVEL = stringPreferencesKey("reverb_level")
        val REVERB_ROOM_SIZE = stringPreferencesKey("reverb_room_size")
        val SCAN_MIN_DURATION_SEC = stringPreferencesKey("scan_min_duration_sec")
        val SCAN_MAX_DURATION_SEC = stringPreferencesKey("scan_max_duration_sec")
        val SCAN_MIN_FILE_SIZE_KB = stringPreferencesKey("scan_min_file_size_kb")
        val SCAN_ENABLED_FORMATS = stringPreferencesKey("scan_enabled_formats")
        val SCAN_LAST_COMPLETED_AT = stringPreferencesKey("scan_last_completed_at")
    }

    data class CachedPlayerTheme(
        val artworkUri: String,
        val argb: Int,
    )

    fun getCachedPlayerTheme(): CachedPlayerTheme? {
        val artworkUri = renderCache.getString(RENDER_CACHE_ARTWORK_URI, null).orEmpty()
        if (artworkUri.isBlank() || !renderCache.contains(RENDER_CACHE_THEME_ARGB)) return null
        return CachedPlayerTheme(
            artworkUri = artworkUri,
            argb = renderCache.getInt(RENDER_CACHE_THEME_ARGB, DEFAULT_PLAYER_THEME_ARGB),
        )
    }

    fun setCachedPlayerTheme(
        artworkUri: String,
        argb: Int,
    ) {
        if (artworkUri.isBlank()) return
        renderCache
            .edit()
            .putString(RENDER_CACHE_ARTWORK_URI, artworkUri)
            .putInt(RENDER_CACHE_THEME_ARGB, argb)
            .apply()
    }

    /**
     * Theme mode is needed before DataStore can emit its first value; otherwise a fixed light app
     * on a dark system renders one dark frame during cold start. The SharedPreferences entry is a
     * first-frame cache only, while DataStore remains the source of truth.
     */
    fun getInitialThemeMode(): String {
        renderCache.getString(RENDER_CACHE_THEME_MODE, null)?.let { return it }
        val storedMode =
            runBlocking {
                context.dataStore.data.first()[Keys.THEME_MODE].orEmpty()
            }
        if (storedMode.isNotBlank()) {
            renderCache.edit().putString(RENDER_CACHE_THEME_MODE, storedMode).commit()
        }
        return storedMode
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
        if (key == Keys.THEME_MODE) {
            renderCache.edit().putString(RENDER_CACHE_THEME_MODE, value).apply()
        }
    }

    fun getFloat(
        key: Preferences.Key<String>,
        defaultValue: Float = 0f,
    ): Flow<Float> =
        context.dataStore.data.map { preferences ->
            preferences[key]?.toFloatOrNull() ?: defaultValue
        }

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

    private companion object {
        const val RENDER_CACHE_ARTWORK_URI = "player_theme_artwork_uri"
        const val RENDER_CACHE_THEME_ARGB = "player_theme_argb"
        const val RENDER_CACHE_THEME_MODE = "theme_mode"
        const val DEFAULT_PLAYER_THEME_ARGB = 0xFF2196F3.toInt()
    }
}
