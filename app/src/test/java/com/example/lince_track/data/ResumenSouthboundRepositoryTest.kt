package com.example.lince_track.data

import com.example.lince_track.R
import com.example.lince_track.data.remote.LinceApi
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.data.remote.dto.ConteoPorClaveDto
import com.example.lince_track.data.remote.dto.RespuestaDto
import com.example.lince_track.data.remote.dto.ResumenDto
import com.example.lince_track.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/** El repositorio recibe la API de Southbound por el constructor, así que aquí es un mock de MockK. */
class ResumenSouthboundRepositoryTest {

    private val api = mockk<LinceApi>()
    private val repo = ResumenSouthboundRepository(api)
    private val semana = LocalDate.of(2026, 11, 14)..LocalDate.of(2026, 11, 20)

    private fun respuesta(porStatusPL: List<ConteoPorClaveDto>) = RespuestaDto(
        ResumenDto(totalServicios = 102, totalPax = 525, porStatusPL = porStatusPL)
    )

    @Test
    fun pideLaOficinaYElRangoYDevuelveLosTotales() = runBlocking {
        coEvery { api.resumen(any(), any(), any()) } returns respuesta(
            listOf(
                ConteoPorClaveDto("SIN_ASIGNAR", 62, 288),
                ConteoPorClaveDto("CF", 7, 21),
                ConteoPorClaveDto("RQ", 25, 165),
                ConteoPorClaveDto("DL", 1, 4),
                ConteoPorClaveDto("CX", 7, 47)
            )
        )

        val resumen = repo.obtener(semana).getOrThrow()

        coVerify { api.resumen("PUX", "2026-11-14", "2026-11-20") }
        assertEquals(102, resumen.totalServicios)
        assertEquals(525, resumen.totalPax)
        // Siempre en el mismo orden, aunque la API los mande en otro.
        assertEquals(listOf("CF", "RQ", "DL", "CX", "SIN_ASIGNAR"), resumen.porStatusPL.map { it.clave })
        assertEquals(ConteoStatusPL("SIN_ASIGNAR", 62, 288), resumen.porStatusPL.last())
    }

    @Test
    fun losEstadosQueFaltanVanEnCeroYLosNuevosAlFinal() {
        val lista = ResumenSouthboundRepository.ordenar(
            listOf(ConteoPorClaveDto("WL", 3, 9), ConteoPorClaveDto("CF", 2, 5))
        )

        assertEquals(
            listOf(
                ConteoStatusPL("CF", 2, 5),
                ConteoStatusPL("RQ", 0, 0),
                ConteoStatusPL("DL", 0, 0),
                ConteoStatusPL("CX", 0, 0),
                ConteoStatusPL("SIN_ASIGNAR", 0, 0),
                ConteoStatusPL("WL", 3, 9)
            ),
            lista
        )
    }

    @Test
    fun unErrorDeLaApiTraeSuTextoYElRequestId() = runBlocking {
        val cuerpo = """{"error":{"code":"RATE_LIMITED","message":"Demasiadas","requestId":"req-9"}}"""
        coEvery { api.resumen(any(), any(), any()) } throws HttpException(
            Response.error<Any>(429, cuerpo.toResponseBody("application/json".toMediaType()))
        )

        val error = repo.obtener(semana).exceptionOrNull() as LinceApiException

        assertEquals(429, error.codigoHttp)
        assertEquals("req-9", error.requestId)
        assertEquals(UiText.StringResource(R.string.api_demasiadas_solicitudes, 60), error.texto)
    }

    @Test
    fun sinConexionSeAvisaConSuTexto() = runBlocking {
        coEvery { api.resumen(any(), any(), any()) } throws IOException("sin red")

        val error = repo.obtener(semana).exceptionOrNull() as LinceApiException

        assertEquals(UiText.StringResource(R.string.api_sin_conexion), error.texto)
    }

    @Test
    fun laCancelacionNoSeConvierteEnError() {
        coEvery { api.resumen(any(), any(), any()) } throws CancellationException("cancelado")

        val lanzada = runCatching { runBlocking { repo.obtener(semana) } }.exceptionOrNull()

        assertTrue(lanzada is CancellationException)
    }
}
