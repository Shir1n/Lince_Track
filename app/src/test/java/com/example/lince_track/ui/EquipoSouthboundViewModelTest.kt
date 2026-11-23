package com.example.lince_track.ui

import com.example.lince_track.R
import com.example.lince_track.data.EquipoSouthbound
import com.example.lince_track.data.EquipoSouthboundRepository
import com.example.lince_track.data.MiembroEquipo
import com.example.lince_track.data.TipoMiembro
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.ui.admin.EquipoSouthboundViewModel
import com.example.lince_track.ui.admin.EstadoEquipo
import com.example.lince_track.util.UiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Sección "Equipo" y filtro por tipo: se le pasan lambdas en vez del repositorio real. */
@OptIn(ExperimentalCoroutinesApi::class)
class EquipoSouthboundViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private val conocidos = EquipoSouthboundRepository.TIPOS_SERVICIO_CONOCIDOS
    private val equipo = EquipoSouthbound(
        guias = listOf(MiembroEquipo(33, "GUI-033", "Benjamín", TipoMiembro.GUIA, true, 4.5, "2023-03-08")),
        transportistas = emptyList()
    )
    private var pedidosEquipo = 0
    private var pedidosTipos = 0
    private var respuestaEquipo: suspend () -> Result<EquipoSouthbound> = { Result.success(equipo) }
    private var respuestaTipos: suspend () -> Result<List<String>> = { Result.success(listOf("TRANSFER", "BOAT")) }

    private fun viewModel(disponible: Boolean = true) = EquipoSouthboundViewModel(
        cargarEquipoApi = { pedidosEquipo++; respuestaEquipo() },
        cargarTiposApi = { pedidosTipos++; respuestaTipos() },
        disponible = disponible
    )

    @Test
    fun sinClaveDeLaApiNoPideNadaYElFiltroUsaLosTiposConocidos() = runTest(dispatcher) {
        val vm = viewModel(disponible = false)

        vm.cargarEquipo()
        vm.reintentar()
        vm.cargarTipos()
        advanceUntilIdle()

        assertFalse(vm.disponible)
        assertEquals(0, pedidosEquipo + pedidosTipos)
        assertEquals(EstadoEquipo(), vm.estado.value)
        assertEquals(conocidos, vm.tiposServicio.value)
    }

    @Test
    fun cargaElEquipoUnaSolaVez() = runTest(dispatcher) {
        val pendiente = CompletableDeferred<Result<EquipoSouthbound>>()
        respuestaEquipo = { pendiente.await() }
        val vm = viewModel()

        vm.cargarEquipo()
        advanceUntilIdle()
        assertTrue(vm.estado.value.cargando)
        vm.cargarEquipo() // mientras carga
        vm.reintentar() // mientras carga tampoco
        pendiente.complete(Result.success(equipo))
        advanceUntilIdle()
        vm.cargarEquipo() // ya cargado

        assertEquals(1, pedidosEquipo)
        assertEquals(EstadoEquipo(equipo = equipo), vm.estado.value)
    }

    @Test
    fun losErroresSeMuestranYSePuedeReintentar() = runTest(dispatcher) {
        val texto = UiText.StringResource(R.string.api_bd_no_disponible)
        respuestaEquipo = { Result.failure(LinceApiException("No disponible", 503, requestId = "abc", texto = texto)) }
        val vm = viewModel()

        vm.cargarEquipo()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.error_con_id, texto, "abc"), vm.estado.value.error)

        respuestaEquipo = { Result.failure(IllegalStateException()) }
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.equipo_error), vm.estado.value.error)

        respuestaEquipo = { Result.success(equipo) }
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(EstadoEquipo(equipo = equipo), vm.estado.value)
        assertEquals(3, pedidosEquipo)
    }

    @Test
    fun losTiposDelCatalogoReemplazanALosConocidos() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(conocidos, vm.tiposServicio.value)

        vm.cargarTipos()
        vm.cargarTipos() // una sola vez
        advanceUntilIdle()

        assertEquals(listOf("TRANSFER", "BOAT"), vm.tiposServicio.value)
        assertEquals(1, pedidosTipos)
    }

    @Test
    fun siElCatalogoFallaOVieneVacioQuedanLosConocidos() = runTest(dispatcher) {
        respuestaTipos = { Result.failure(IllegalStateException("caída")) }
        val conError = viewModel()
        conError.cargarTipos()
        advanceUntilIdle()
        assertEquals(conocidos, conError.tiposServicio.value)

        respuestaTipos = { Result.success(emptyList()) }
        val vacio = viewModel()
        vacio.cargarTipos()
        advanceUntilIdle()
        assertEquals(conocidos, vacio.tiposServicio.value)
    }
}
