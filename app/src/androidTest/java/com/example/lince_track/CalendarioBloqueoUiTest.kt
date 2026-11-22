package com.example.lince_track

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lince_track.R
import com.example.lince_track.data.BloqueoRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.dto.AsignacionDto
import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import com.example.lince_track.data.remote.dto.PersonaDto
import com.example.lince_track.data.remote.dto.ReservaDto
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.ui.agenda.AgendaViewModel
import com.example.lince_track.ui.calendario.CalendarioScreen
import com.example.lince_track.ui.theme.Lince_TrackTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Bloqueo de días en el calendario, con hoy fijo en el 20-11-2026 (anticipación de 7 días: se bloquea desde el 27-11)
 * y un backend falso que no sale del celular.
 */
class CalendarioBloqueoUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hoy = LocalDate.of(2026, 11, 20)
    private val api = BloqueoApiFalsa()

    // Los textos se leen de los recursos: así el test pasa con la app en español o en inglés.
    private val contexto get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val idioma get() = contexto.resources.configuration.locales[0]
    private fun texto(id: Int, vararg args: Any): String = contexto.getString(id, *args)

    /** Responde como el backend, sin red: guarda lo que se bloquea. */
    private class BloqueoApiFalsa : BloqueoApi {
        val creados = mutableListOf<BloqueoSolicitudDto>()
        override suspend fun listar(): List<BloqueoDto> = emptyList()
        override suspend fun crear(solicitud: BloqueoSolicitudDto): BloqueoDto {
            creados += solicitud
            return BloqueoDto(id = creados.size.toLong(), uid = "uid", fecha = solicitud.fecha, motivo = solicitud.motivo)
        }
        override suspend fun eliminar(id: Long) = Unit
        override suspend fun obtenerParametros(): ParametrosDto = ParametrosDto(diasAnticipacionBloqueo = 7)
    }

    private fun servicioEl(fecha: LocalDate) = ServicioMapper.aDominio(
        ServicioDto(
            id = 3008,
            fecha = fecha.toString(),
            horaPickup = "08:30",
            tipo = "EXCURSION",
            descripcion = "Tour Frutillar y Lago Llanquihue",
            estadoServicio = "CF",
            reserva = ReservaDto(referencia = "REF3008", nombre = "Pasajero", estado = "CF"),
            pickup = "Hotel Cumbres Puerto Varas",
            dropoff = "Hotel Cumbres Puerto Varas",
            transportista = PersonaDto(37, nombre = "Carlos Morales R."),
            asignacion = AsignacionDto(statusPL = "CF")
        )
    )!!

    private fun abrirCalendario(rol: Rol = Rol.CONDUCTOR) {
        val viewModel = AgendaViewModel(
            servicios = MutableStateFlow(listOf(servicioEl(LocalDate.of(2026, 11, 28)))),
            aceptarServicio = { Result.success(null) },
            rechazarServicio = { _, _, _ -> Result.success(null) },
            bloqueoRepository = BloqueoRepository(api),
            hoy = { hoy },
            autoCargarBloqueos = false
        )
        composeRule.setContent {
            Lince_TrackTheme {
                CalendarioScreen(viewModel = viewModel, rol = rol)
            }
        }
    }

    private fun tocarDia(fecha: LocalDate) {
        composeRule.onNodeWithTag("day_cell_$fecha").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun unDiaATresDiasDeHoyNoSePuedeBloquear() {
        abrirCalendario()

        val aTresDias = LocalDate.of(2026, 11, 23)
        tocarDia(aTresDias)
        composeRule.onNodeWithTag("btn_bloquear_dia").performScrollTo().assertIsNotEnabled()
        composeRule
            .onNodeWithText(texto(R.string.bloqueo_anticipacion_min, 7, formatFecha(LocalDate.of(2026, 11, 27), idioma)))
            .assertExists()

        // Tocar de nuevo el día elegido tampoco abre el diálogo.
        tocarDia(aTresDias)
        composeRule.onNodeWithTag("campo_motivo_bloqueo").assertDoesNotExist()
        assertEquals(0, api.creados.size)
    }

    @Test
    fun elAdministradorNoGestionaDisponibilidad() {
        abrirCalendario(Rol.ADMINISTRADOR)

        val diaLibre = LocalDate.of(2026, 11, 30)
        tocarDia(diaLibre)
        tocarDia(diaLibre)

        composeRule.onNodeWithText(texto(R.string.bloqueo_disponibilidad)).assertDoesNotExist()
        composeRule.onNodeWithTag("btn_bloquear_dia").assertDoesNotExist()
        composeRule.onNodeWithTag("campo_motivo_bloqueo").assertDoesNotExist()
    }

    @Test
    fun unDiaConServicioNoMuestraElBotonDeBloquear() {
        abrirCalendario()

        tocarDia(LocalDate.of(2026, 11, 28))

        composeRule.onNodeWithText(texto(R.string.bloqueo_tiene_servicios)).assertExists()
        composeRule.onNodeWithTag("btn_bloquear_dia").assertDoesNotExist()
    }

    @Test
    fun unDiaLibreConAnticipacionSeBloquea() {
        abrirCalendario()

        tocarDia(LocalDate.of(2026, 11, 30))
        composeRule.onNodeWithTag("btn_bloquear_dia").performScrollTo().assertIsEnabled().performClick()
        // El motivo es obligatorio: sin escribirlo, no se puede confirmar.
        composeRule.onNodeWithTag("btn_confirmar_bloqueo").assertIsNotEnabled()
        composeRule.onNodeWithTag("campo_motivo_bloqueo").performTextInput("Descanso")
        composeRule.onNodeWithTag("btn_confirmar_bloqueo").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) { api.creados.isNotEmpty() }
        assertEquals(listOf(BloqueoSolicitudDto("2026-11-30", "Descanso")), api.creados)
        composeRule.onNodeWithText(texto(R.string.bloqueo_badge_corto)).assertExists()
        composeRule.onNodeWithText(texto(R.string.bloqueo_dia_bloqueado)).assertExists()
    }
}
