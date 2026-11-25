package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.dto.HitoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant

/** Posición puntual durante un servicio en curso, con el backend simulado. */
class PosicionRepositoryTest {

    private val api = mockk<BloqueoApi>()
    private val repo = PosicionRepository(api)

    private fun errorHttp(codigo: Int, code: String) = HttpException(
        Response.error<Any>(
            codigo,
            """{"error":{"code":"$code","message":"no","requestId":"r1"}}""".toResponseBody("application/json".toMediaType())
        )
    )

    @Test
    fun soloSeEnviaEnCursoConGpsRealYCadaCincoMinutos() {
        val cinco = ReglasPosicion.INTERVALO_MS
        assertTrue(ReglasPosicion.debeEnviar(enCurso = true, gpsReal = true, ultimoEnvioMs = null, ahoraMs = 0))
        assertFalse(ReglasPosicion.debeEnviar(enCurso = true, gpsReal = true, ultimoEnvioMs = 0, ahoraMs = cinco - 1))
        assertTrue(ReglasPosicion.debeEnviar(enCurso = true, gpsReal = true, ultimoEnvioMs = 0, ahoraMs = cinco))
        assertFalse(ReglasPosicion.debeEnviar(enCurso = false, gpsReal = true, ultimoEnvioMs = null, ahoraMs = 0))
        assertFalse(ReglasPosicion.debeEnviar(enCurso = true, gpsReal = false, ultimoEnvioMs = null, ahoraMs = 0))
    }

    @Test
    fun minutosDesdeNuncaEsNegativo() {
        val t = Instant.parse("2026-11-24T12:00:00Z")
        assertEquals(3, ReglasPosicion.minutosDesde(t, t.plusSeconds(200)))
        assertEquals(0, ReglasPosicion.minutosDesde(t, t.minusSeconds(60)))
    }

    @Test
    fun enviarMandaUnHitoDePosicion() = runBlocking {
        coEvery { api.registrarHito(any()) } returns HitoDto(resultado = "REGISTRADO")

        assertTrue(repo.enviar("3166", -41.4, -72.9, 7.4f).isSuccess)

        coVerify {
            api.registrarHito(match { it.tipo == "POSICION" && it.servicioId == 3166L && it.precisionMetros == 7.4f.toDouble() })
        }
    }

    @Test
    fun sinSenalOSinCheckInLaPosicionSeOmite() = runBlocking {
        coEvery { api.registrarHito(any()) } throws IOException("sin red")
        assertTrue(repo.enviar("3166", 0.0, 0.0, 5f).isFailure)

        coEvery { api.registrarHito(any()) } throws errorHttp(409, "SIN_CHECK_IN")
        assertEquals("SIN_CHECK_IN", (repo.enviar("3166", 0.0, 0.0, 5f).exceptionOrNull() as BloqueoApiException).codigoApi)

        assertTrue(repo.enviar("demo", 0.0, 0.0, 5f).isFailure)
    }

    @Test
    fun laUltimaPosicionTraeCoordenadasPrecisionYHora() = runBlocking {
        coEvery { api.ultimaPosicion(3166) } returns HitoDto(
            tipo = "POSICION", lat = -41.45, lon = -72.91, precisionMetros = 8.6, registradoEn = "2026-11-24T23:55:16.318169Z"
        )

        assertEquals(
            PosicionServicio(-41.45, -72.91, 9, Instant.parse("2026-11-24T23:55:16.318169Z")),
            repo.ultima("3166").getOrThrow()
        )
    }

    @Test
    fun sinPosicionDevuelveNullYOtrosErroresSeInforman() = runBlocking {
        coEvery { api.ultimaPosicion(1) } throws errorHttp(404, "SIN_POSICION")
        assertNull(repo.ultima("1").getOrThrow())

        coEvery { api.ultimaPosicion(2) } returns HitoDto(lat = null, lon = null)
        assertNull(repo.ultima("2").getOrThrow())

        coEvery { api.ultimaPosicion(3) } returns HitoDto(lat = 1.0, lon = 2.0, registradoEn = "no es fecha")
        assertNull(repo.ultima("3").getOrThrow()!!.registradaEn)

        coEvery { api.ultimaPosicion(4) } throws errorHttp(403, "FORBIDDEN")
        assertTrue(repo.ultima("4").isFailure)
    }
}
