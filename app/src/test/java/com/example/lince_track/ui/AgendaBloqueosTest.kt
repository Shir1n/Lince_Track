package com.example.lince_track.ui

import com.example.lince_track.data.Bloqueo
import com.example.lince_track.data.BloqueoRepository
import com.example.lince_track.data.Datos
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import com.example.lince_track.ui.agenda.AgendaViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

/**
 * Bloqueo de días en la agenda. La API del backend es un mock de MockK dentro de un BloqueoRepository real, así que
 * se prueban juntas la validación de la app y la respuesta del backend (la "validación doble" de la pauta).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgendaBloqueosTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private val hoy = LocalDate.of(2026, 11, 20)
    private val aTresDias = LocalDate.of(2026, 11, 23)
    private val diaConServicio = LocalDate.of(2026, 11, 28)
    private val diaLibre = LocalDate.of(2026, 11, 30)
    private val otroDia = LocalDate.of(2026, 12, 5)

    private val api = mockk<BloqueoApi>()
    private val servicio = Datos.servicio(3008).copy(fecha = diaConServicio)
    private val servicios = MutableStateFlow(listOf(servicio))
    private var resultado: Boolean? = null

    private fun agenda(autoCargar: Boolean = false) = AgendaViewModel(
        servicios = servicios,
        aceptarServicio = { Result.success(null) },
        rechazarServicio = { _, _, _ -> Result.success(null) },
        bloqueoRepository = BloqueoRepository(api),
        hoy = { hoy },
        autoCargarBloqueos = autoCargar
    )

    private fun dto(id: Long, fecha: LocalDate, motivo: String? = null) =
        BloqueoDto(id = id, uid = "uid1", fecha = fecha.toString(), motivo = motivo)

    private fun errorHttp(codigo: Int, code: String, message: String = "", requestId: String = "") = HttpException(
        Response.error<Any>(
            codigo,
            """{"error":{"code":"$code","message":"$message","requestId":"$requestId"}}"""
                .toResponseBody("application/json".toMediaType())
        )
    )

    /** Agenda que ya cargó del backend los parámetros y los bloqueos indicados. */
    private fun agendaConBloqueos(vararg bloqueos: BloqueoDto): AgendaViewModel {
        coEvery { api.obtenerParametros() } returns ParametrosDto(diasAnticipacionBloqueo = 7)
        coEvery { api.listar() } returns bloqueos.toList()
        return agenda(autoCargar = true)
    }

    @Test
    fun alCrearseCargaLosParametrosYLosBloqueos() = runTest(dispatcher) {
        coEvery { api.obtenerParametros() } returns ParametrosDto(diasAnticipacionBloqueo = 10)
        coEvery { api.listar() } returns listOf(dto(1, otroDia, "Descanso"))

        val vm = agenda(autoCargar = true)
        advanceUntilIdle()

        assertEquals(10, vm.diasAnticipacion.value)
        assertEquals(listOf(Bloqueo(1, "uid1", otroDia, "Descanso")), vm.bloqueos.value)
        assertFalse(vm.cargandoBloqueos.value)
        assertEquals(diaLibre, vm.primerDiaBloqueable())
        assertTrue(vm.estaBloqueado(otroDia))
        assertEquals(1L, vm.bloqueoDe(otroDia)?.id)
        assertNull(vm.bloqueoDe(diaLibre))
    }

    @Test
    fun siElBackendNoRespondeQuedanLosValoresPorDefecto() = runTest(dispatcher) {
        coEvery { api.obtenerParametros() } throws IOException("sin red")
        coEvery { api.listar() } throws IOException("sin red")

        val vm = agenda(autoCargar = true)
        advanceUntilIdle()

        assertEquals(7, vm.diasAnticipacion.value)
        assertTrue(vm.bloqueos.value.isEmpty())
        assertFalse(vm.cargandoBloqueos.value)
        assertNull(vm.message.value)
    }

    @Test
    fun indicaQueDiasSePuedenBloquear() = runTest(dispatcher) {
        val vm = agendaConBloqueos(dto(1, otroDia))
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 11, 27), vm.primerDiaBloqueable())
        assertFalse(vm.esDiaBloqueable(aTresDias))
        assertTrue(vm.esDiaBloqueable(LocalDate.of(2026, 11, 27)))
        assertFalse(vm.esDiaBloqueable(diaConServicio))
        assertFalse(vm.esDiaBloqueable(otroDia))
        assertTrue(vm.esDiaBloqueable(diaLibre))

        servicios.value = listOf(servicio.copy(cancelado = true))
        assertTrue(vm.esDiaBloqueable(diaConServicio))
    }

    @Test
    fun aMenosDeSieteDiasNoLlamaAlBackend() = runTest(dispatcher) {
        val vm = agenda()

        vm.bloquearDia(aTresDias) { resultado = it }
        advanceUntilIdle()

        assertEquals("Solo se puede bloquear desde el ${formatFecha(LocalDate.of(2026, 11, 27))}.", vm.message.value)
        assertEquals(false, resultado)
        coVerify(exactly = 0) { api.crear(any()) }
    }

    @Test
    fun unDiaConServicioNoSeBloquea() = runTest(dispatcher) {
        val vm = agenda()

        vm.bloquearDia(diaConServicio) { resultado = it }
        advanceUntilIdle()

        assertEquals("Ese día tienes un servicio asignado.", vm.message.value)
        assertEquals(false, resultado)
        coVerify(exactly = 0) { api.crear(any()) }
    }

    @Test
    fun unDiaYaBloqueadoNoSeVuelveABloquear() = runTest(dispatcher) {
        val vm = agendaConBloqueos(dto(1, diaLibre))
        advanceUntilIdle()

        vm.bloquearDia(diaLibre) { resultado = it }
        advanceUntilIdle()

        assertEquals("Ese día ya está bloqueado.", vm.message.value)
        assertEquals(false, resultado)
        coVerify(exactly = 0) { api.crear(any()) }
    }

    @Test
    fun bloquearUnDiaLibreLoAgregaEnOrden() = runTest(dispatcher) {
        val vm = agendaConBloqueos(dto(1, otroDia))
        advanceUntilIdle()
        val respuesta = CompletableDeferred<BloqueoDto>()
        coEvery { api.crear(any()) } coAnswers { respuesta.await() }

        vm.bloquearDia(diaLibre, "Descanso") { resultado = it }
        advanceUntilIdle()
        assertTrue(vm.operandoBloqueo.value)

        vm.bloquearDia(LocalDate.of(2026, 12, 1)) // mientras se envía, un segundo toque no hace nada
        respuesta.complete(dto(3, diaLibre, "Descanso"))
        advanceUntilIdle()

        assertFalse(vm.operandoBloqueo.value)
        assertEquals(listOf(3L, 1L), vm.bloqueos.value.map { it.id })
        assertEquals("Día bloqueado correctamente.", vm.message.value)
        assertEquals(true, resultado)
        coVerify(exactly = 1) { api.crear(any()) }
        coVerify { api.crear(BloqueoSolicitudDto("2026-11-30", "Descanso")) }
    }

    @Test
    fun siElBackendRechazaPorAnticipacionMuestraElIdYRecargaLosParametros() = runTest(dispatcher) {
        val vm = agendaConBloqueos()
        advanceUntilIdle()
        coEvery { api.crear(any()) } throws errorHttp(422, "SIN_ANTICIPACION", "Solo desde el 01-12-2026", "abc")
        coEvery { api.obtenerParametros() } returns ParametrosDto(diasAnticipacionBloqueo = 11)

        vm.bloquearDia(diaLibre) { resultado = it }
        advanceUntilIdle()

        assertEquals("Solo desde el 01-12-2026 (ID: abc)", vm.message.value)
        assertEquals(false, resultado)
        assertEquals(11, vm.diasAnticipacion.value)
        assertTrue(vm.bloqueos.value.isEmpty())
        coVerify(exactly = 2) { api.obtenerParametros() }
    }

    @Test
    fun siElBackendEncuentraUnServicioNoRecargaLosParametros() = runTest(dispatcher) {
        val vm = agendaConBloqueos()
        advanceUntilIdle()
        coEvery { api.crear(any()) } throws errorHttp(409, "DIA_CON_SERVICIO")

        vm.bloquearDia(diaLibre) { resultado = it }
        advanceUntilIdle()

        assertEquals("Ese día tienes un servicio asignado.", vm.message.value)
        assertEquals(false, resultado)
        coVerify(exactly = 1) { api.obtenerParametros() }
    }

    @Test
    fun desbloquearQuitaElDia() = runTest(dispatcher) {
        val vm = agendaConBloqueos(dto(3, diaLibre), dto(4, otroDia))
        advanceUntilIdle()
        val respuesta = CompletableDeferred<Unit>()
        coEvery { api.eliminar(3) } coAnswers { respuesta.await() }

        vm.desbloquearDia(3) { resultado = it }
        advanceUntilIdle()
        vm.desbloquearDia(4) // mientras se envía, un segundo toque no hace nada
        respuesta.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(4L), vm.bloqueos.value.map { it.id })
        assertEquals("Bloqueo eliminado correctamente.", vm.message.value)
        assertEquals(true, resultado)
        assertFalse(vm.operandoBloqueo.value)
        coVerify(exactly = 0) { api.eliminar(4) }
    }

    @Test
    fun siDesbloquearFallaElDiaSigueBloqueado() = runTest(dispatcher) {
        val vm = agendaConBloqueos(dto(3, diaLibre))
        advanceUntilIdle()
        coEvery { api.eliminar(3) } throws IOException("sin red")

        vm.desbloquearDia(3) { resultado = it }
        advanceUntilIdle()

        assertEquals("Sin conexión con el servidor. Revisa tu red.", vm.message.value)
        assertEquals(false, resultado)
        assertEquals(listOf(3L), vm.bloqueos.value.map { it.id })
    }
}
