package com.example.lince_track.data.privacidad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacidadDataStore by preferencesDataStore(name = "privacidad_prefs")

/**
 * Recuerda qué versión del aviso de privacidad vio la persona en este teléfono. Si el aviso cambia, se sube
 * [VERSION_AVISO] y se vuelve a mostrar al abrir la app.
 *
 * Igual que ControlVersionRepository, cada función tiene una versión que recibe el DataStore para los tests.
 */
object PrivacidadRepository {

    /** Súbela cuando cambie lo que dice el aviso (qué datos, para qué o quién los ve). */
    const val VERSION_AVISO = 4

    private val VERSION_VISTA = intPreferencesKey("aviso_privacidad_visto")

    fun debeMostrarAviso(context: Context): Flow<Boolean> = debeMostrarAviso(context.privacidadDataStore)

    fun debeMostrarAviso(datos: DataStore<Preferences>): Flow<Boolean> =
        datos.data.map { prefs -> (prefs[VERSION_VISTA] ?: 0) < VERSION_AVISO }

    suspend fun marcarAvisoVisto(context: Context) = marcarAvisoVisto(context.privacidadDataStore)

    suspend fun marcarAvisoVisto(datos: DataStore<Preferences>) {
        datos.edit { prefs -> prefs[VERSION_VISTA] = VERSION_AVISO }
    }
}
