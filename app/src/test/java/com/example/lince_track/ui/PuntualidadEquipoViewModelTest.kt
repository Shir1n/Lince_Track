package com.example.lince_track.ui

import com.example.lince_track.R
import com.example.lince_track.data.PuntualidadEquipo
import com.example.lince_track.data.PuntualidadPersona
import com.example.lince_track.data.Rol
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.ui.admin.EstadoPuntualidadEquipo
import com.example.lince_track.ui.admin.PeriodoResumen
import com.example.lince_track.ui.admin.PuntualidadEquipoViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/** La tarjeta "Puntualidad del equipo": se le pasan lambdas en vez del repositorio real. */
@OptIn(ExperimentalCoroutinesApi::class)
class PuntualidadEquipoViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private val hoy = LocalDate.of(2026, 11, 20)
    private val equipo = PuntualidadEquipo(
        servicios = 2,
        puntualidad = 50,
        personas = listOf(PuntualidadPersona("uid-carlos", "Carlos Morales R.", Rol.CONDUCTOR, 2, 1, 1, 0, 0, 50, 3))
    )
    private val pedidos = mutableListOf<ClosedRange<LocalDate>>()
    private var respuesta: suspend () -> Result<PuntualidadEquipo> = { Result.success(equipo) }

    private fun viewModel() = PuntualidadEquipoViewModel(
        cargarEquipo = { rango -> pedidos += rango; respuesta() },
        hoy = { hoy }
    )

    @Test
    fun cargaElPeriodoElegido() = runTest(dispatcher) {
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        assertTrue(vm.estado.value.cargando)
        advanceUntilIdle()

        assertEquals(listOf(hoy..hoy), pedidos)
        assertEquals(EstadoPuntualidadEquipo(equipo = equipo), vm.estado.value)

        vm.cargar(PeriodoResumen.SIETE_DIAS)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 14)..hoy, pedidos.last())
    }

    @Test
    fun elMismoPeriodoNoSePideDosVeces() = runTest(dispatcher) {
        val pendiente = CompletableDeferred<Result<PuntualidadEquipo>>()
        respuesta = { pendiente.await() }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        vm.cargar(PeriodoResumen.HOY) // mientras carga
        pendiente.complete(Result.success(equipo))
        advanceUntilIdle()
        vm.cargar(PeriodoResumen.HOY) // ya cargado

        assertEquals(1, pedidos.size)
    }

    @Test
    fun cambiarDePeriodoDescartaLaCargaAnterior() = runTest(dispatcher) {
        val lenta = CompletableDeferred<Result<PuntualidadEquipo>>()
        val otro = equipo.copy(servicios = 9)
        var llamada = 0
        respuesta = { if (++llamada == 1) lenta.await() else Result.success(otro) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.SIETE_DIAS)
        advanceUntilIdle()
        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()
        lenta.complete(Result.success(equipo))
        advanceUntilIdle()

        assertEquals(otro, vm.estado.value.equipo)
    }

    @Test
    fun sinPermisoSeAvisaConElRequestId() = runTest(dispatcher) {
        respuesta = { Result.failure(BloqueoApiException("Prohibido", codigoHttp = 403, requestId = "req-7")) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()

        assertEquals(
            UiText.StringResource(R.string.error_con_id, UiText.StringResource(R.string.puntualidad_sin_permiso), "req-7"),
            vm.estado.value.error
        )
    }

    @Test
    fun sinConexionYOtrosErroresTienenSuTexto() = runTest(dispatcher) {
        respuesta = { Result.failure(BloqueoApiException("Sin red", causa = IOException("sin red"))) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.error_sin_conexion_servidor), vm.estado.value.error)

        respuesta = { Result.failure(BloqueoApiException("Falla interna", codigoHttp = 500, requestId = "")) }
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.puntualidad_error), vm.estado.value.error)

        respuesta = { Result.failure(IllegalStateException("raro")) }
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.puntualidad_error), vm.estado.value.error)
    }

    @Test
    fun reintentarVuelveAPedirElUltimoPeriodo() = runTest(dispatcher) {
        val vm = viewModel()
        vm.reintentar() // aún no hay periodo: no hace nada
        advanceUntilIdle()
        assertTrue(pedidos.isEmpty())

        respuesta = { Result.failure(IllegalStateException("caída")) }
        vm.cargar(PeriodoResumen.SIETE_DIAS)
        advanceUntilIdle()

        respuesta = { Result.success(equipo) }
        vm.reintentar()
        advanceUntilIdle()

        assertEquals(2, pedidos.size)
        assertEquals(pedidos[0], pedidos[1])
        assertEquals(EstadoPuntualidadEquipo(equipo = equipo), vm.estado.value)
    }
}
