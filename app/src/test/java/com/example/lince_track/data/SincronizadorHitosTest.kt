package com.example.lince_track.data

import com.example.lince_track.data.local.GeocercaGuardada
import com.example.lince_track.data.local.HitoPendiente
import com.example.lince_track.data.local.LinceDao
import com.example.lince_track.data.local.ParametrosGuardados
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.dto.GeocercaDto
import com.example.lince_track.data.remote.dto.HitoDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Cola de check-ins sin conexión: Room reemplazado por una lista en memoria y el backend por MockK. */
class SincronizadorHitosTest {

    /** Lo mismo que hace Room, en memoria. */
    private class DaoEnMemoria : LinceDao() {
        val hitos = MutableStateFlow<List<HitoPendiente>>(emptyList())
        val intentos = mutableMapOf<String, String?>()
        var geocercasGuardadas: List<GeocercaGuardada> = emptyList()
        var parametrosGuardados: ParametrosGuardados? = null

        override suspend fun guardarHito(hito: HitoPendiente) {
            if (hitos.value.none { it.clave == hito.clave }) hitos.value = hitos.value + hito
        }
        override fun observarHitos(): Flow<List<HitoPendiente>> = hitos
        override suspend fun hitosPendientes() = hitos.value.sortedBy { it.creadoEn }
        override suspend fun borrarHito(clave: String) {
            hitos.value = hitos.value.filterNot { it.clave == clave }
        }
        override suspend fun marcarIntento(clave: String, error: String?) {
            intentos[clave] = error
        }
        override suspend fun geocercas() = geocercasGuardadas
        override suspend fun borrarGeocercas() {
            geocercasGuardadas = emptyList()
        }
        override suspend fun insertarGeocercas(geocercas: List<GeocercaGuardada>) {
            geocercasGuardadas = geocercasGuardadas + geocercas
        }
        override suspend fun parametros() = parametrosGuardados
        override suspend fun guardarParametros(parametros: ParametrosGuardados) {
            parametrosGuardados = parametros
        }
    }

    private val dao = DaoEnMemoria()
    private val api = mockk<BloqueoApi>()
    private val checkIns = mutableListOf<String>()
    private val checkOuts = mutableListOf<String>()
    private val rechazos = mutableListOf<Pair<String, ResultadoCheckin>>()

    private val sincronizador = SincronizadorHitos(
        dao = dao,
        hitos = HitoRepository(api) { dao },
        guardarCheckIn = { checkIns += it.servicioId },
        guardarCheckOut = { checkOuts += it.servicioId },
        reportarRechazo = { h, resultado, _ -> rechazos += h.servicioId to resultado }
    )

    private fun hito(clave: String, servicio: String = "3067", checkIn: Boolean = true, creado: Long = 1) = HitoPendiente(
        clave = clave, uid = "u1", rol = "conductor", nombre = "Carlos", servicioId = servicio, titulo = "Tour",
        fecha = "2026-11-22", lugar = "Hotel Costanera Puerto Varas", checkIn = checkIn, lat = -41.3, lon = -72.9,
        precisionMetros = 8f, distanciaMetros = 20, creadoEn = creado
    )

    private fun errorHttp(codigo: Int, code: String) = HttpException(
        Response.error<Any>(
            codigo,
            """{"error":{"code":"$code","message":"no","requestId":"r1"}}""".toResponseBody("application/json".toMediaType())
        )
    )

    @Test
    fun conColaVaciaNoHaceNada() = runBlocking {
        assertEquals(ResultadoSincronizacion(), sincronizador.sincronizar())
        assertFalse(ResultadoSincronizacion().reintentar)
    }

    @Test
    fun enviaEnOrdenConLaMismaClaveYVaciaLaCola() = runBlocking {
        dao.guardarHito(hito("b", checkIn = false, creado = 2))
        dao.guardarHito(hito("a", creado = 1))
        dao.guardarHito(hito("a", creado = 1)) // doble toque: no se duplica
        coEvery { api.registrarHito(any()) } answers {
            HitoDto(resultado = if (firstArg<com.example.lince_track.data.remote.dto.HitoSolicitudDto>().tipo == "CHECK_IN") "VERIFICADO" else "REGISTRADO")
        }

        val resultado = sincronizador.sincronizar()

        assertEquals(ResultadoSincronizacion(enviados = 2, servicios = listOf("3067")), resultado)
        assertEquals(listOf("3067"), checkIns)
        assertEquals(listOf("3067"), checkOuts)
        assertTrue(dao.hitos.value.isEmpty())
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            api.registrarHito(match { it.claveIdempotencia == "a" && it.tipo == "CHECK_IN" })
            api.registrarHito(match { it.claveIdempotencia == "b" && it.tipo == "CHECK_OUT" })
        }
    }

    @Test
    fun sinConexionSeDetieneYQuedaParaReintentar() = runBlocking {
        dao.guardarHito(hito("a", creado = 1))
        dao.guardarHito(hito("b", checkIn = false, creado = 2))
        coEvery { api.registrarHito(any()) } throws IOException("sin red")

        val resultado = sincronizador.sincronizar()

        assertEquals(2, resultado.pendientes)
        assertTrue(resultado.reintentar)
        assertEquals(2, dao.hitos.value.size)
        assertEquals(setOf("a"), dao.intentos.keys)
        coVerify(exactly = 1) { api.registrarHito(any()) }
    }

    @Test
    fun unErrorDelServidorTambienSeReintenta() = runBlocking {
        dao.guardarHito(hito("a"))
        coEvery { api.registrarHito(any()) } throws errorHttp(503, "DB_UNAVAILABLE")

        assertEquals(1, sincronizador.sincronizar().pendientes)
        assertEquals(1, dao.hitos.value.size)
    }

    @Test
    fun siElBackendLoRechazaSeCreaLaIncidenciaYSeDescartaSuCheckOut() = runBlocking {
        dao.guardarHito(hito("a", creado = 1))
        dao.guardarHito(hito("b", checkIn = false, creado = 2))
        coEvery { api.registrarHito(any()) } returns HitoDto(resultado = "TARDE", distanciaMetros = 30.0, radioMetros = 150)

        val resultado = sincronizador.sincronizar()

        assertEquals(ResultadoSincronizacion(rechazados = 1, descartados = 1, servicios = listOf("3067")), resultado)
        assertEquals(listOf("3067" to ResultadoCheckin.TARDE), rechazos)
        assertTrue(checkIns.isEmpty())
        assertTrue(checkOuts.isEmpty())
        assertTrue(dao.hitos.value.isEmpty())
        coVerify(exactly = 1) { api.registrarHito(any()) }
    }

    @Test
    fun unRechazoDefinitivoSeDescartaYSigueConElResto() = runBlocking {
        dao.guardarHito(hito("a", servicio = "1", creado = 1))
        dao.guardarHito(hito("b", servicio = "2", creado = 2))
        coEvery { api.registrarHito(match { it.servicioId == 1L }) } throws errorHttp(403, "FORBIDDEN")
        coEvery { api.registrarHito(match { it.servicioId == 2L }) } throws errorHttp(409, "HITO_DUPLICADO")

        val resultado = sincronizador.sincronizar()

        // El 409 de un check-in ya verificado cuenta como enviado.
        assertEquals(ResultadoSincronizacion(enviados = 1, descartados = 1, servicios = listOf("2")), resultado)
        assertEquals(listOf("2"), checkIns)
        assertTrue(dao.hitos.value.isEmpty())
    }

    @Test
    fun siFirestoreFallaElHitoIgualSaleDeLaCola() = runBlocking {
        dao.guardarHito(hito("a"))
        coEvery { api.registrarHito(any()) } returns HitoDto(resultado = "VERIFICADO")
        val conFalla = SincronizadorHitos(
            dao = dao,
            hitos = HitoRepository(api) { dao },
            guardarCheckIn = { throw IllegalStateException("sin permiso") }
        )

        assertEquals(1, conFalla.sincronizar().enviados)
        assertTrue(dao.hitos.value.isEmpty())
    }

    @Test
    fun lasGeocercasYReglasQuedanGuardadasYSeUsanSinConexion() = runBlocking {
        val repo = HitoRepository(api) { dao }
        coEvery { api.geocercas() } returns listOf(
            GeocercaDto(1, "Hotel Centro Puerto Varas", -41.3, -72.9, 150),
            GeocercaDto(2, "Inactiva", 0.0, 0.0, 100, activa = false)
        )
        coEvery { api.obtenerParametros() } returns ParametrosDto(anticipacionMinutos = 15, precisionMaximaMetros = 50)

        val enLinea = repo.geocercas().getOrThrow()
        repo.parametros().getOrThrow()
        assertEquals(1, dao.geocercasGuardadas.size)

        coEvery { api.geocercas() } throws IOException("sin red")
        coEvery { api.obtenerParametros() } throws IOException("sin red")

        assertEquals(enLinea, repo.geocercas().getOrThrow())
        assertEquals(ParametrosCheckin(15, 50), repo.parametros().getOrThrow())
    }

    @Test
    fun sinCopiaGuardadaOConUnErrorDelBackendSeInformaElError() = runBlocking {
        val repo = HitoRepository(api) { dao }
        coEvery { api.geocercas() } throws IOException("sin red")
        coEvery { api.obtenerParametros() } throws errorHttp(403, "FORBIDDEN")
        dao.parametrosGuardados = ParametrosGuardados(1, 15, 50)

        assertTrue(repo.geocercas().isFailure)
        // Un 403 no es falta de conexión: no se esconde con la copia guardada.
        assertTrue(repo.parametros().isFailure)
    }
}
