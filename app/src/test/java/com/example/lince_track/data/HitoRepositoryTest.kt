package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.dto.GeocercaDto
import com.example.lince_track.data.remote.dto.HitoDto
import com.example.lince_track.data.remote.dto.HitoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** El repositorio recibe la API del backend por el constructor: aquí es un mock de MockK. */
class HitoRepositoryTest {

    private val api = mockk<BloqueoApi>()
    private val repo = HitoRepository(api)

    private fun errorHttp(codigo: Int, code: String, message: String = "") = HttpException(
        Response.error<Any>(
            codigo,
            """{"error":{"code":"$code","message":"$message","requestId":"req-9"}}"""
                .toResponseBody("application/json".toMediaType())
        )
    )

    private suspend fun registrar(checkIn: Boolean = true, servicioId: String = "3008", precision: Float = 12f) =
        repo.registrar(servicioId, checkIn, -41.3192, -72.9858, precision, "clave-1")

    @Test
    fun geocercasDejaSoloLasActivas() = runBlocking {
        coEvery { api.geocercas() } returns listOf(
            GeocercaDto(1, "Hotel Centro Puerto Varas", -41.3192, -72.9858, 150),
            GeocercaDto(2, "Hotel Antiguo", -41.0, -72.0, 100, activa = false)
        )

        val lista = repo.geocercas().getOrThrow()

        assertEquals(listOf(Geocerca(1, "Hotel Centro Puerto Varas", -41.3192, -72.9858, 150)), lista)
    }

    @Test
    fun parametrosTraeAnticipacionYPrecision() = runBlocking {
        coEvery { api.obtenerParametros() } returns ParametrosDto(anticipacionMinutos = 15, precisionMaximaMetros = 50)

        assertEquals(ParametrosCheckin(15, 50), repo.parametros().getOrThrow())
    }

    @Test
    fun registrarEnviaElHitoYLeeLaRespuesta() = runBlocking {
        coEvery { api.registrarHito(any()) } returns HitoDto(
            id = 4, servicioId = 3008, tipo = "CHECK_IN", resultado = "FUERA_DE_ZONA",
            geocercaNombre = "Hotel Centro Puerto Varas", radioMetros = 150, distanciaMetros = 230.6
        )

        val hito = registrar().getOrThrow()

        assertEquals(HitoRegistrado(ResultadoCheckin.FUERA_DE_ZONA, 231, 150), hito)
        coVerify {
            api.registrarHito(HitoSolicitudDto(3008, "CHECK_IN", -41.3192, -72.9858, 12.0, "clave-1"))
        }
    }

    @Test
    fun elCheckOutVaComoCheckOutYSinPrecisionNegativa() = runBlocking {
        coEvery { api.registrarHito(any()) } returns HitoDto(resultado = "REGISTRADO")

        val hito = registrar(checkIn = false, precision = -1f).getOrThrow()

        assertEquals(HitoRegistrado(null, null, null), hito)
        coVerify {
            api.registrarHito(HitoSolicitudDto(3008, "CHECK_OUT", -41.3192, -72.9858, 0.0, "clave-1"))
        }
    }

    @Test
    fun unCheckInYaVerificadoEnElBackendCuentaComoVerificado() = runBlocking {
        coEvery { api.registrarHito(any()) } throws errorHttp(409, "HITO_DUPLICADO", "Ya tienes un check-in verificado.")

        assertEquals(ResultadoCheckin.VERIFICADO, registrar().getOrThrow().resultado)
        // En el check-out el mismo código es un error.
        assertTrue(registrar(checkIn = false).isFailure)
    }

    @Test
    fun losErroresTraenElMensajeYElRequestIdDelBackend() = runBlocking {
        coEvery { api.registrarHito(any()) } throws errorHttp(403, "FORBIDDEN", "El servicio no está asignado a tu cuenta.")

        val error = registrar().exceptionOrNull() as BloqueoApiException

        assertEquals("El servicio no está asignado a tu cuenta.", error.message)
        assertEquals("req-9", error.requestId)
        assertEquals(403, error.codigoHttp)
    }

    @Test
    fun sinConexionYConIdNoNumericoFalla() = runBlocking {
        coEvery { api.geocercas() } throws IOException("sin red")
        coEvery { api.obtenerParametros() } throws IOException("sin red")

        assertEquals("Sin conexión con el servidor. Revisa tu red.", repo.geocercas().exceptionOrNull()?.message)
        assertTrue(repo.parametros().isFailure)
        assertEquals(
            "El servicio demo-1 no existe en el backend.",
            registrar(servicioId = "demo-1").exceptionOrNull()?.message
        )
    }
}
