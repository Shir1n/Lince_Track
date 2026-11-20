package com.example.lince_track.data.privacidad

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.lince_track.ui.components.SECCIONES_AVISO_PRIVACIDAD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** El aviso de privacidad se muestra hasta que se ve, y otra vez si cambia su versión. */
class PrivacidadRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var datos: DataStore<Preferences>

    @Before
    fun preparar() {
        datos = PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("privacidad.preferences_pb") }
    }

    @After
    fun limpiar() = scope.cancel()

    @Test
    fun laPrimeraVezSeMuestraYDespuesDeVerloNo() = runBlocking {
        assertTrue(PrivacidadRepository.debeMostrarAviso(datos).first())
        PrivacidadRepository.marcarAvisoVisto(datos)
        assertFalse(PrivacidadRepository.debeMostrarAviso(datos).first())
    }

    @Test
    fun siElAvisoCambiaDeVersionSeVuelveAMostrar() = runBlocking {
        datos.edit { it[intPreferencesKey("aviso_privacidad_visto")] = PrivacidadRepository.VERSION_AVISO - 1 }
        assertTrue(PrivacidadRepository.debeMostrarAviso(datos).first())
    }

    @Test
    fun elAvisoCubreLoQuePideLaLey() {
        val titulos = SECCIONES_AVISO_PRIVACIDAD.map { it.first }
        listOf("Qué datos usamos", "Para qué", "Quién los ve", "Tu ubicación", "Dónde se guardan", "Tus derechos")
            .forEach { assertTrue("Falta la sección \"$it\"", it in titulos) }
        // Debe decir que la ubicación del mapa no se envía y que no hay seguimiento en segundo plano.
        val ubicacion = SECCIONES_AVISO_PRIVACIDAD.first { it.first == "Tu ubicación" }.second
        assertTrue(ubicacion.contains("no se") && ubicacion.contains("segundo plano"))
    }
}
