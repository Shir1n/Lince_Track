package com.example.lince_track.data

import com.example.lince_track.R
import com.example.lince_track.data.remote.LinceApi
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.data.remote.dto.CatalogosDto
import com.example.lince_track.data.remote.dto.GuiaDto
import com.example.lince_track.data.remote.dto.IdiomaDto
import com.example.lince_track.data.remote.dto.PaginaDto
import com.example.lince_track.data.remote.dto.PaginacionMetaDto
import com.example.lince_track.data.remote.dto.RespuestaDto
import com.example.lince_track.data.remote.dto.TipoServicioDto
import com.example.lince_track.data.remote.dto.TransportistaDto
import com.example.lince_track.data.remote.dto.VehiculoDto
import com.example.lince_track.util.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** La API de Southbound es un mock de MockK. */
class EquipoSouthboundRepositoryTest {

    private val api = mockk<LinceApi>()
    private val repo = EquipoSouthboundRepository(api)

    private fun <T> pagina(datos: List<T>, pagina: Int, totalPaginas: Int) =
        PaginaDto(datos, PaginacionMetaDto(page = pagina, pageSize = 100, total = datos.size, totalPages = totalPaginas))

    private fun guia(id: Long, nombre: String, activo: Boolean = true) = GuiaDto(
        id = id, codigo = "GUI-0$id", nombre = nombre, activo = activo, evaluacionPromedio = 4.5,
        fechaIngreso = "2023-03-08", idiomas = listOf(IdiomaDto("ES", "Español"), IdiomaDto("", ""), IdiomaDto("EN", "Inglés"))
    )

    private val andres = TransportistaDto(
        id = 37, codigo = "TRA-037", nombre = "Andrés Valdés Ibáñez", evaluacionPromedio = 4.99,
        vehiculos = listOf(VehiculoDto(44, "ZZ-1044", "VAN", 8))
    )

    @Test
    fun traeTodasLasPaginasYOrdenaActivosPrimero() = runBlocking {
        coEvery { api.guias("PUX", null, 1, 100) } returns pagina(listOf(guia(33, "Benjamín"), guia(23, "Ana", activo = false)), 1, 2)
        coEvery { api.guias("PUX", null, 2, 100) } returns pagina(listOf(guia(31, "Cristóbal")), 2, 2)
        coEvery { api.transportistas("PUX", null, 1, 100) } returns pagina(listOf(andres), 1, 1)

        val equipo = repo.equipo().getOrThrow()

        // Activos por nombre y los inactivos al final.
        assertEquals(listOf("Benjamín", "Cristóbal", "Ana"), equipo.guias.map { it.nombre })
        assertEquals(listOf("ES", "EN"), equipo.guias.first().idiomas)
        assertEquals(TipoMiembro.GUIA, equipo.guias.first().tipo)
        assertEquals(
            MiembroEquipo(
                id = 37, codigo = "TRA-037", nombre = "Andrés Valdés Ibáñez", tipo = TipoMiembro.TRANSPORTISTA,
                activo = true, evaluacion = 4.99, fechaIngreso = "", vehiculos = listOf(VehiculoEquipo("ZZ-1044", "VAN", 8))
            ),
            equipo.transportistas.single()
        )
        coVerify(exactly = 2) { api.guias(any(), any(), any(), any()) }
        coVerify(exactly = 1) { api.transportistas(any(), any(), any(), any()) }
    }

    @Test
    fun noPideMasDeDiezPaginas() = runBlocking {
        coEvery { api.guias(any(), any(), any(), any()) } returns pagina(listOf(guia(1, "Uno")), 1, 50)
        coEvery { api.transportistas(any(), any(), any(), any()) } returns pagina(emptyList(), 1, 0)

        val equipo = repo.equipo().getOrThrow()

        assertEquals(EquipoSouthboundRepository.MAX_PAGINAS, equipo.guias.size)
        coVerify(exactly = EquipoSouthboundRepository.MAX_PAGINAS) { api.guias(any(), any(), any(), any()) }
        assertTrue(equipo.transportistas.isEmpty())
    }

    @Test
    fun losTiposDeServicioVienenDeLosCatalogos() = runBlocking {
        coEvery { api.catalogos() } returns RespuestaDto(
            CatalogosDto(
                tiposServicio = listOf(
                    TipoServicioDto("TRANSFER", "TR"), TipoServicioDto("", ""),
                    TipoServicioDto("EXCURSION", "EX"), TipoServicioDto("TRANSFER", "TR")
                )
            )
        )

        assertEquals(listOf("TRANSFER", "EXCURSION"), repo.tiposServicio().getOrThrow())
    }

    @Test
    fun losErroresSeTraducenYLaCancelacionNo() = runBlocking {
        coEvery { api.guias(any(), any(), any(), any()) } throws IOException("sin red")
        val error = repo.equipo().exceptionOrNull() as LinceApiException
        assertEquals(UiText.StringResource(R.string.api_sin_conexion), error.texto)

        coEvery { api.catalogos() } throws IllegalStateException("raro")
        assertTrue(repo.tiposServicio().exceptionOrNull() is LinceApiException)

        coEvery { api.catalogos() } throws CancellationException("cancelado")
        assertTrue(runCatching { repo.tiposServicio() }.exceptionOrNull() is CancellationException)
    }

    @Test
    fun filtraLosServiciosPorTipo() {
        val transfer = Datos.servicio(1).copy(tipo = "TRANSFER")
        val excursion = Datos.servicio(2).copy(tipo = "EXCURSION")
        val todos = listOf(transfer, excursion)

        assertEquals(todos, EquipoSouthboundRepository.filtrarPorTipo(todos, null))
        assertEquals(listOf(excursion), EquipoSouthboundRepository.filtrarPorTipo(todos, "EXCURSION"))
        assertTrue(EquipoSouthboundRepository.filtrarPorTipo(todos, "GUIDED_SERVICE").isEmpty())
    }
}
