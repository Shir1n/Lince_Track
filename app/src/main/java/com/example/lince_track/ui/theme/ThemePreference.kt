package com.example.lince_track.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

val Context.themeModeFlow: Flow<ThemeMode>
    get() = settingsDataStore.data.map { preferences ->
        when (preferences[THEME_MODE_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

suspend fun Context.setThemeMode(mode: ThemeMode) {
    settingsDataStore.edit { preferences ->
        preferences[THEME_MODE_KEY] = mode.name
    }
}
