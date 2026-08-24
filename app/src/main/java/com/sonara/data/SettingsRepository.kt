package com.sonara.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sonara.ambient.AmbientVisualMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * User settings that actually change behavior: ambient visual intensity and
 * whether artwork colors drive the environment. Only options the backend
 * supports are exposed — no fake toggles.
 */
class SettingsRepository(context: Context, scope: CoroutineScope) {

    private val dataStore = context.applicationContext.ambientSettings

    val ambientMode: Flow<AmbientVisualMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_AMBIENT_MODE]) {
            AmbientVisualMode.Reduced.name -> AmbientVisualMode.Reduced
            AmbientVisualMode.Off.name -> AmbientVisualMode.Off
            else -> AmbientVisualMode.On
        }
    }

    val dynamicColors: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLORS] ?: true
    }

    suspend fun setAmbientMode(mode: AmbientVisualMode) {
        dataStore.edit { it[KEY_AMBIENT_MODE] = mode.name }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLORS] = enabled }
    }

    private companion object {
        val KEY_AMBIENT_MODE = stringPreferencesKey("ambient_mode")
        val KEY_DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }
}

private val Context.ambientSettings by androidx.datastore.preferences.preferencesDataStore(name = "sonara_settings")

/** Convenience snapshot for screens that need synchronous-ish reads. */
data class SettingsSnapshot(
    val ambientMode: AmbientVisualMode = AmbientVisualMode.On,
    val dynamicColors: Boolean = true,
)

fun SettingsRepository.snapshotIn(scope: CoroutineScope) = kotlinx.coroutines.flow.combine(
    ambientMode,
    dynamicColors,
) { mode, dynamic -> SettingsSnapshot(mode, dynamic) }
    .stateIn(scope, SharingStarted.Eagerly, SettingsSnapshot())
