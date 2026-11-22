package com.example.lince_track.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Idioma de la app, independiente del idioma del teléfono.
 *
 * Usa la API de idioma por app de Android (AppCompatDelegate.setApplicationLocales): cambia toda la app (pantallas,
 * mensajes y notificaciones), Android guarda la elección y, desde Android 13, también se puede cambiar en
 * Ajustes > Apps > Lince Track > Idioma. Al cambiarlo, la pantalla se recarga una vez.
 */
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ES("es-CL"),
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

        /** Traduce el idioma guardado por Android ("", "es-CL", "en-US"…) a una opción del selector. */
        fun fromLanguageTags(tags: String): AppLanguage {
            val idioma = tags.split(',').firstOrNull()?.trim()?.substringBefore('-')?.lowercase().orEmpty()
            return when (idioma) {
                "es" -> ES
                "en" -> EN
                else -> SYSTEM
            }
        }
    }
}

/** Idioma elegido en la app; SYSTEM si sigue al teléfono. */
fun idiomaActual(): AppLanguage =
    AppLanguage.fromLanguageTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

/** Cambia el idioma solo de esta app. SYSTEM vuelve a seguir el idioma del teléfono. */
fun cambiarIdioma(language: AppLanguage) {
    val locales = language.languageTag?.let { LocaleListCompat.forLanguageTags(it) }
        ?: LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(locales)
}

/**
 * Este contexto con el idioma elegido en la app. Sirve para lo que no es una pantalla (notificaciones): el contexto
 * de la aplicación puede seguir con el idioma con que se abrió la app y no se entera del cambio.
 */
fun Context.enIdiomaDeLaApp(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
    return createConfigurationContext(config)
}
