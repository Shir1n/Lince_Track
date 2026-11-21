package com.example.lince_track.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ES("es"),
    EN("en");

    fun resolveLocale(systemLocale: Locale = Locale.getDefault()): Locale {
        return when (this) {
            SYSTEM -> systemLocale
            ES -> Locale.forLanguageTag("es-CL")
            EN -> Locale.ENGLISH
        }
    }

    companion object {
        fun fromStored(value: String?): AppLanguage {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")

val Context.appLanguageFlow: Flow<AppLanguage>
    get() = settingsDataStore.data.map { preferences ->
        AppLanguage.fromStored(preferences[APP_LANGUAGE_KEY])
    }

suspend fun Context.setAppLanguage(language: AppLanguage) {
    settingsDataStore.edit { preferences ->
        preferences[APP_LANGUAGE_KEY] = language.name
    }
}

fun Context.localizedContext(language: AppLanguage, systemLocale: Locale): Context {
    val targetLocale = language.resolveLocale(systemLocale)
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(targetLocale))
    val configContext = createConfigurationContext(config)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = configContext.resources
        override fun getAssets(): AssetManager = configContext.assets
    }
}
