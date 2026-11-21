package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import com.example.lince_track.data.remote.mensajeBloqueo
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/** El repositorio recibe la API del backend por el constructor, así que aquí se reemplaza por un mock de MockK. */
class BloqueoRepositoryTest {

    private val api = mockk<BloqueoApi>()
    private val repo = BloqueoRepository(api)
    private val fecha = LocalDate.of(2026, 11, 28)

    private fun errorHttp(codigo: Int, cuerpo: String) =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    private fun cuerpo(code: String, message: String = "", requestId: String = "req-1") =
        """{"error":{"code":"$code","message":"$message","requestId":"$requestId"}}"""

    private fun errorAlCrear(error: Throwable): BloqueoApiException = runBlocking {
        coEvery { api.crear(any()) } throws error
        repo.crear(fecha).exceptionOrNull() as BloqueoApiException
    }

    @Test
    fun listarConvierteLosBloqueosAlModelo() = runBlocking {
        coEvery { api.listar() } returns listOf(
            BloqueoDto(id = 5, uid = "uid1", fecha = "2026-11-28", motivo = "Descanso", creadoEn = "2026-11-20T10:00:00Z")
        )

        val lista = repo.listar().getOrThrow()

        assertEquals(listOf(Bloqueo(5, "uid1", fecha, "Descanso", "2026-11-20T10:00:00Z")), lista)
    }

    @Test
    fun crearEnviaLaFechaYElMotivoSinEspacios() = runBlocking {
        coEvery { api.crear(any()) } returns BloqueoDto(id = 7, uid = "uid1", fecha = "2026-11-28", motivo = "Trámite")

        val creado = repo.crear(fecha, "  Trámite  ").getOrThrow()
        repo.crear(fecha, "   ")
        repo.crear(fecha)

        assertEquals(7L, creado.id)
        assertEquals(fecha, creado.fecha)
        coVerify(exactly = 1) { api.crear(BloqueoSolicitudDto("2026-11-28", "Trámite")) }
        coVerify(exactly = 2) { api.crear(BloqueoSolicitudDto("2026-11-28", null)) }
    }

    @Test
    fun eliminarYParametrosLlamanALaApi() = runBlocking {
        coEvery { api.eliminar(9) } just Runs
        coEvery { api.obtenerParametros() } returns ParametrosDto(diasAnticipacionBloqueo = 10)

        assertTrue(repo.eliminar(9).isSuccess)
        assertEquals(10, repo.obtenerParametros().getOrThrow().diasAnticipacionBloqueo)
        coVerify { api.eliminar(9) }
    }

    @Test
    fun losErroresDelBackendLleganConMensajeClaroYRequestId() {
        val sinAnticipacion = errorAlCrear(errorHttp(422, cuerpo("SIN_ANTICIPACION", "Solo desde el 27-11-2026")))
        assertEquals("Solo desde el 27-11-2026", sinAnticipacion.message)
        assertEquals(422, sinAnticipacion.codigoHttp)
        assertEquals("SIN_ANTICIPACION", sinAnticipacion.codigoApi)
        assertEquals("req-1", sinAnticipacion.requestId)
        assertEquals("Solo desde el 27-11-2026 (ID: req-1)", sinAnticipacion.mensajeConRequestId)

        assertEquals(
            "Ese día no cumple la anticipación mínima para bloquear.",
            errorAlCrear(errorHttp(422, cuerpo("SIN_ANTICIPACION"))).message
        )
        assertEquals(
            "Ese día tienes un servicio asignado.",
            errorAlCrear(errorHttp(409, cuerpo("DIA_CON_SERVICIO"))).message
        )
        assertEquals("Ese día ya está bloqueado.", errorAlCrear(errorHttp(409, cuerpo("YA_BLOQUEADO"))).message)
        assertEquals(
            "Tu cuenta no tiene permisos para bloquear días.",
            errorAlCrear(errorHttp(403, cuerpo("ACCESS_DENIED"))).message
        )
        assertEquals("Sin ID de la API", errorAlCrear(errorHttp(403, cuerpo("FORBIDDEN", "Sin ID de la API"))).message)
        assertEquals(
            "No se encontró el bloqueo solicitado.",
            errorAlCrear(errorHttp(404, cuerpo("NOT_FOUND", "Bloqueo 9 no existe"))).message
        )
        assertEquals("Falla interna", errorAlCrear(errorHttp(500, cuerpo("INTERNAL_ERROR", "Falla interna"))).message)
    }

    @Test
    fun unErrorSinCuerpoJsonNoTraeRequestId() {
        val error = errorAlCrear(errorHttp(502, "<html>Bad Gateway</html>"))

        assertEquals("Error 502 del servidor.", error.message)
        assertNull(error.codigoApi)
        assertNull(error.requestId)
        assertEquals("Error 502 del servidor.", error.mensajeConRequestId)

        // Un requestId vacío tampoco se muestra.
        assertNull(errorAlCrear(errorHttp(409, cuerpo("YA_BLOQUEADO", requestId = ""))).requestId)
    }

    @Test
    fun losErroresDeRedYOtrosTambienSeTraducen() = runBlocking {
        coEvery { api.listar() } throws IOException("timeout")
        assertEquals("Sin conexión con el servidor. Revisa tu red.", repo.listar().exceptionOrNull()?.message)

        coEvery { api.obtenerParametros() } throws IllegalStateException("raro")
        assertEquals("raro", repo.obtenerParametros().exceptionOrNull()?.message)

        coEvery { api.eliminar(any()) } throws IllegalStateException()
        assertEquals("Error inesperado al gestionar el bloqueo.", repo.eliminar(1).exceptionOrNull()?.message)

        val yaTraducida = BloqueoApiException("Ya traducida", requestId = "r")
        coEvery { api.listar() } throws yaTraducida
        assertSame(yaTraducida, repo.listar().exceptionOrNull())
    }

    @Test
    fun mensajeBloqueoAgregaElRequestIdOUsaElTextoPorDefecto() {
        assertEquals("Falló (ID: r1)", BloqueoApiException("Falló", requestId = "r1").mensajeBloqueo("x"))
        assertEquals("Falló", BloqueoApiException("Falló").mensajeBloqueo("x"))
        assertEquals("Otro error", RuntimeException("Otro error").mensajeBloqueo("x"))
        assertEquals("Por defecto", RuntimeException().mensajeBloqueo("Por defecto"))
    }
}
