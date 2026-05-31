package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novabeat_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val ACTIVE_SPACE = stringPreferencesKey("active_space")
        val DSP_VOLUME_BOOST = intPreferencesKey("dsp_volume_boost") // 0 to 100 extra gain (translates to 100%-200% volume)
        val BOOST_WARNING_SHOWN = booleanPreferencesKey("boost_warning_shown")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BAND_VALUES = stringPreferencesKey("eq_band_values") // JSON representation of 10 sliders
        val BASS_BOOST_LEVEL = intPreferencesKey("bass_boost_level") // 0 to 1000
        val VIRTUALIZER_LEVEL = intPreferencesKey("virtualizer_level") // 0 to 1000 (3D Surround spatial)
        val COIL_BLUR_INTENSITY = floatPreferencesKey("coil_blur_intensity") // 0.1f to 1.0f (Glassmorphism card opacity)
        val ANIMATION_SPEED_SCALE = floatPreferencesKey("animation_speed_scale") // 0.5f to 2.0f
        val VISUALIZER_TYPE = stringPreferencesKey("visualizer_type") // circular, waveform, neon_particle, reactive_lighting
        val FAVORITE_SONGS = stringPreferencesKey("favorite_songs") // JSON string of IDs
        val PLAYBACK_HISTORY = stringPreferencesKey("playback_history") // JSON string of IDs
        val PLAYLISTS_DATA = stringPreferencesKey("playlists_data") // Custom playlists stored in JSON format
        val SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity") // Configurable shake gesture control: 0f (disabled) to 15f
    }

    val activeSpaceFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_SPACE] ?: "cyberpunk"
    }

    val dspVolumeBoostFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DSP_VOLUME_BOOST] ?: 0
    }

    val boostWarningShownFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BOOST_WARNING_SHOWN] ?: false
    }

    val eqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[EQ_ENABLED] ?: true
    }

    val eqPresetFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[EQ_PRESET] ?: "Hi-Fi"
    }

    val eqBandValuesFlow: Flow<List<Float>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[EQ_BAND_VALUES]
        if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Float>()
                for (i in 0 until array.length()) {
                    list.add(array.getDouble(i).toFloat())
                }
                list
            } catch (e: Exception) {
                listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            }
        } else {
            listOf(0f, 1f, 3f, 2f, -1f, 0f, 2f, 3f, 1f, 0f) // Custom curve default
        }
    }

    val bassBoostFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BASS_BOOST_LEVEL] ?: 300 // default 30% bass
    }

    val virtualizerFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[VIRTUALIZER_LEVEL] ?: 250 // default 25% 3D spatial
    }

    val blurIntensityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[COIL_BLUR_INTENSITY] ?: 0.35f
    }

    val animationSpeedFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[ANIMATION_SPEED_SCALE] ?: 1.0f
    }

    val visualizerTypeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[VISUALIZER_TYPE] ?: "circular"
    }

    val shakeSensitivityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[SHAKE_SENSITIVITY] ?: 12f // standard shake force
    }

    val favoriteSongsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[FAVORITE_SONGS] ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            val set = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
    }

    val historySongsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[PLAYBACK_HISTORY] ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveActiveSpace(spaceId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_SPACE] = spaceId
        }
    }

    suspend fun saveDspVolumeBoost(extraPercent: Int) {
        context.dataStore.edit { prefs ->
            prefs[DSP_VOLUME_BOOST] = extraPercent
        }
    }

    suspend fun saveBoostWarningShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BOOST_WARNING_SHOWN] = shown
        }
    }

    suspend fun saveEqEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[EQ_ENABLED] = enabled
        }
    }

    suspend fun saveEqPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[EQ_PRESET] = preset
        }
    }

    suspend fun saveEqBandValues(values: List<Float>) {
        context.dataStore.edit { prefs ->
            val array = JSONArray()
            values.forEach { array.put(it.toDouble()) }
            prefs[EQ_BAND_VALUES] = array.toString()
        }
    }

    suspend fun saveBassBoost(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[BASS_BOOST_LEVEL] = level
        }
    }

    suspend fun saveVirtualizer(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[VIRTUALIZER_LEVEL] = level
        }
    }

    suspend fun saveBlurIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[COIL_BLUR_INTENSITY] = intensity
        }
    }

    suspend fun saveAnimationSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[ANIMATION_SPEED_SCALE] = speed
        }
    }

    suspend fun saveVisualizerType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[VISUALIZER_TYPE] = type
        }
    }

    suspend fun saveShakeSensitivity(sensitivity: Float) {
        context.dataStore.edit { prefs ->
            prefs[SHAKE_SENSITIVITY] = sensitivity
        }
    }

    suspend fun toggleFavoriteSong(songId: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[FAVORITE_SONGS] ?: "[]"
            val array = JSONArray(jsonStr)
            val updatedList = mutableListOf<String>()
            var found = false
            for (i in 0 until array.length()) {
                val item = array.getString(i)
                if (item == songId) {
                    found = true
                } else {
                    updatedList.add(item)
                }
            }
            if (!found) {
                updatedList.add(songId)
            }
            val updatedJson = JSONArray(updatedList)
            prefs[FAVORITE_SONGS] = updatedJson.toString()
        }
    }

    suspend fun addSongToHistory(songId: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[PLAYBACK_HISTORY] ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            list.add(songId) // Add to top
            for (i in 0 until array.length()) {
                val item = array.getString(i)
                if (item != songId && list.size < 50) { // Keep last 50, distinct
                    list.add(item)
                }
            }
            val updated = JSONArray(list)
            prefs[PLAYBACK_HISTORY] = updated.toString()
        }
    }
}
