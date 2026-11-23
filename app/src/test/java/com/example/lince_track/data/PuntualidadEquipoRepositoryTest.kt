package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.dto.ResumenHitosDto
import com.example.lince_track.data.remote.dto.ResumenRecursoDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/** La API del backend es un mock de MockK; los nombres de las cuentas se pasan como una lambda. */
class PuntualidadEquipoRepositoryTest {

    private val api = mockk<BloqueoApi>()
    private val semana = LocalDate.of(2026, 11, 14)..LocalDate.of(2026, 11, 20)
    private var nombres: suspend () -> Map<String, String> = {
        mapOf("uid-carlos" to "Carlos Morales R.", "uid-sin-nombre" to "  ")
    }
    private val repo = PuntualidadEquipoRepository(api) { nombres() }

    private val respuesta = ResumenHitosDto(
        desde = "2026-11-14",
        hasta = "2026-11-20",
        total = ResumenRecursoDto(servicios = 4, aTiempo = 2, puntualidad = 50.4),
        recursos = listOf(
            ResumenRecursoDto(
                uid = "uid-carlos", rol = "conductor", servicios = 2, aTiempo = 0, tarde = 1, fueraDeZona = 1,
                gpsImpreciso = 0, puntualidad = 0.0, minutosAnticipacionPromedio = -5.6
            ),
            ResumenRecursoDto(uid = "uid-alfredo", rol = "guia", servicios = 1, aTiempo = 1, puntualidad = 100.0),
            ResumenRecursoDto(uid = "uid-sin-nombre", rol = "otro", servicios = 1, aTiempo = 1, puntualidad = 66.6),
            ResumenRecursoDto(uid = null, servicios = 9) // sin uid no es una persona
        )
    )

    @Test
    fun pideElRangoYLePoneNombreACadaPersona() = runBlocking {
        coEvery { api.resumenHitos(any(), any()) } returns respuesta

        val equipo = repo.obtener(semana).getOrThrow()

        coVerify { api.resumenHitos("2026-11-14", "2026-11-20") }
        assertEquals(4, equipo.servicios)
        assertEquals(50, equipo.puntualidad)
        // Mismo orden del backend (la menos puntual primero) y sin la fila sin uid.
        assertEquals(listOf("uid-carlos", "uid-alfredo", "uid-sin-nombre"), equipo.personas.map { it.uid })
        assertEquals(
            PuntualidadPersona("uid-carlos", "Carlos Morales R.", Rol.CONDUCTOR, 2, 0, 1, 1, 0, 0, -6),
            equipo.personas[0]
        )
        // Si la cuenta no está o su nombre está en blanco, queda sin nombre ("Sin nombre" en pantalla).
        assertNull(equipo.personas[1].nombre)
        assertEquals(Rol.GUIA, equipo.personas[1].rol)
        assertNull(equipo.personas[1].minutosAnticipacion)
        assertNull(equipo.personas[2].nombre)
        assertNull(equipo.personas[2].rol)
        assertEquals(67, equipo.personas[2].puntualidad)
    }

    @Test
    fun sinLosNombresIgualSeMuestraLaPuntualidad() = runBlocking {
        coEvery { api.resumenHitos(any(), any()) } returns respuesta
        nombres = { throw IllegalStateException("sin permiso en Firestore") }

        val equipo = repo.obtener(semana).getOrThrow()

        assertEquals(3, equipo.personas.size)
        assertTrue(equipo.personas.all { it.nombre == null })
    }

    @Test
    fun sinServiciosLaPuntualidadQuedaVacia() = runBlocking {
        coEvery { api.resumenHitos(any(), any()) } returns ResumenHitosDto()

        val equipo = repo.obtener(semana).getOrThrow()

        assertEquals(0, equipo.servicios)
        assertNull(equipo.puntualidad)
        assertTrue(equipo.personas.isEmpty())
    }

    @Test
    fun unErrorDelBackendTraeSuCodigoYElRequestId() = runBlocking {
        val cuerpo = """{"error":{"code":"FORBIDDEN","message":"Tu rol no permite esta acción.","requestId":"req-7"}}"""
        coEvery { api.resumenHitos(any(), any()) } throws HttpException(
            Response.error<Any>(403, cuerpo.toResponseBody("application/json".toMediaType()))
        )

        val error = repo.obtener(semana).exceptionOrNull() as BloqueoApiException

        assertEquals(403, error.codigoHttp)
        assertEquals("FORBIDDEN", error.codigoApi)
        assertEquals("req-7", error.requestId)
    }

    @Test
    fun sinConexionElErrorGuardaLaCausa() = runBlocking {
        coEvery { api.resumenHitos(any(), any()) } throws IOException("sin red")

        val error = repo.obtener(semana).exceptionOrNull() as BloqueoApiException

        assertTrue(error.cause is IOException)
    }

    @Test
    fun laCancelacionNoSeConvierteEnError() {
        coEvery { api.resumenHitos(any(), any()) } throws CancellationException("cancelado")
        assertTrue(runCatching { runBlocking { repo.obtener(semana) } }.exceptionOrNull() is CancellationException)

        coEvery { api.resumenHitos(any(), any()) } returns respuesta
        nombres = { throw CancellationException("cancelado") }
        assertTrue(runCatching { runBlocking { repo.obtener(semana) } }.exceptionOrNull() is CancellationException)
    }
}
