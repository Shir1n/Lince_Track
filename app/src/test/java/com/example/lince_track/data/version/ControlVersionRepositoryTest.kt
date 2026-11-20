package com.example.lince_track.data.version

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetSocketAddress

/**
 * ControlVersionRepository con un DataStore real en un archivo temporal y un servidor HTTP local que hace de
 * GitHub, así que no necesita Android ni internet.
 */
class ControlVersionRepositoryTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var datos: DataStore<Preferences>
    private lateinit var servidor: HttpServer

    /** Ruta del servidor local → (código HTTP, cuerpo). */
    private val respuestas = mutableMapOf<String, Pair<Int, String>>()

    @Before
    fun preparar() {
        datos = PreferenceDataStoreFactory.create(scope = scope) { carpeta.newFile("version.preferences_pb") }
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            val (codigo, cuerpo) = respuestas[intercambio.requestURI.path] ?: (404 to "")
            val bytes = cuerpo.toByteArray()
            intercambio.sendResponseHeaders(codigo, if (intercambio.requestMethod == "HEAD" || bytes.isEmpty()) -1 else bytes.size.toLong())
            intercambio.responseBody.use { if (intercambio.requestMethod != "HEAD") it.write(bytes) }
        }
        servidor.start()
    }

    @After
    fun limpiar() {
        servidor.stop(0)
        scope.cancel()
    }

    private fun url(ruta: String) = "http://127.0.0.1:${servidor.address.port}$ruta"

    @Test
    fun losAvisosVienenActivosYSePuedenApagar() = runBlocking {
        assertTrue(ControlVersionRepository.avisosActivosFlow(datos).first())
        ControlVersionRepository.setAvisosActivos(datos, false)
        assertFalse(ControlVersionRepository.avisosActivosFlow(datos).first())
        assertEquals(EstadoAvisoVersion.Ninguno, ControlVersionRepository.evaluarAlIniciar(datos) { error("no debe consultar") })
    }

    @Test
    fun unaVersionNuevaEnMasterAvisaQueEstaDesactualizada() = runBlocking {
        val local = ControlVersionRepository.versionLocal
        val remota = local.copy(versionCode = local.versionCode + 1)
        val estado = ControlVersionRepository.evaluarAlIniciar(datos) { remota }
        assertEquals(EstadoAvisoVersion.Desactualizada(local, remota), estado)
    }

    @Test
    fun despuesDeVerLaVersionYaNoSeAvisa() = runBlocking {
        val local = ControlVersionRepository.versionLocal
        assertEquals(EstadoAvisoVersion.RecienActualizada(local), ControlVersionRepository.evaluarAlIniciar(datos) { null })
        ControlVersionRepository.marcarVersionVista(datos, local.versionCode)
        assertEquals(EstadoAvisoVersion.Ninguno, ControlVersionRepository.evaluarAlIniciar(datos) { null })
    }

    @Test
    fun leeLaVersionRemotaDeVersionJson() = runBlocking {
        respuestas["/version.json"] = 200 to """{"versionCode": 99, "versionName": "9.9", "novedades": ["Algo"], "extra": 1}"""
        val remota = ControlVersionRepository.consultarVersionRemota(url("/version.json"))
        assertEquals(99, remota?.versionCode)
        assertEquals(listOf("Algo"), remota?.novedades)
    }

    @Test
    fun unErrorHttpOSinConexionNoHayVersionRemota() = runBlocking {
        assertNull(ControlVersionRepository.consultarVersionRemota(url("/no-existe.json")))
        respuestas["/malo.json"] = 200 to "esto no es json"
        assertNull(ControlVersionRepository.consultarVersionRemota(url("/malo.json")))
        assertNull(ControlVersionRepository.consultarVersionRemota("http://127.0.0.1:1/version.json"))
    }

    @Test
    fun laDescargaSoloEstaDisponibleSiElApkResponde() = runBlocking {
        respuestas["/lince-track.apk"] = 200 to ""
        assertTrue(ControlVersionRepository.descargaDisponible(url("/lince-track.apk")))
        assertFalse(ControlVersionRepository.descargaDisponible(url("/otro.apk")))
        assertFalse(ControlVersionRepository.descargaDisponible("http://127.0.0.1:1/lince-track.apk"))
    }
}
