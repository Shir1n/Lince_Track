package com.example.lince_track.ui

import com.example.lince_track.R
import com.example.lince_track.data.ConteoStatusPL
import com.example.lince_track.data.ResumenSouthbound
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.ui.admin.EstadoResumenSouthbound
import com.example.lince_track.ui.admin.PeriodoResumen
import com.example.lince_track.ui.admin.ResumenSouthboundViewModel
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
import java.time.LocalDate

/** La tarjeta "Datos de Southbound": se le pasan lambdas en vez del repositorio real (CLAUDE.md, tests de ViewModel). */
@OptIn(ExperimentalCoroutinesApi::class)
class ResumenSouthboundViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private val hoy = LocalDate.of(2026, 11, 20)
    private val resumen = ResumenSouthbound(102, 525, listOf(ConteoStatusPL("CF", 7, 21)))
    private val pedidos = mutableListOf<ClosedRange<LocalDate>>()
    private var respuesta: suspend () -> Result<ResumenSouthbound> = { Result.success(resumen) }

    private fun viewModel(disponible: Boolean = true) = ResumenSouthboundViewModel(
        cargarResumen = { rango -> pedidos += rango; respuesta() },
        disponible = disponible,
        hoy = { hoy }
    )

    @Test
    fun sinClaveDeLaApiNoPideNada() = runTest(dispatcher) {
        val vm = viewModel(disponible = false)

        vm.cargar(PeriodoResumen.HOY)
        vm.reintentar()
        advanceUntilIdle()

        assertFalse(vm.disponible)
        assertTrue(pedidos.isEmpty())
        assertEquals(EstadoResumenSouthbound(), vm.estado.value)
    }

    @Test
    fun cargaElPeriodoElegido() = runTest(dispatcher) {
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        assertTrue(vm.estado.value.cargando)
        advanceUntilIdle()

        assertEquals(listOf(hoy..hoy), pedidos)
        assertEquals(EstadoResumenSouthbound(resumen = resumen), vm.estado.value)

        vm.cargar(PeriodoResumen.SIETE_DIAS)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 14)..hoy, pedidos.last())
    }

    @Test
    fun elMismoPeriodoNoSePideDosVeces() = runTest(dispatcher) {
        val pendiente = CompletableDeferred<Result<ResumenSouthbound>>()
        respuesta = { pendiente.await() }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        vm.cargar(PeriodoResumen.HOY) // mientras carga
        pendiente.complete(Result.success(resumen))
        advanceUntilIdle()
        vm.cargar(PeriodoResumen.HOY) // ya cargado

        assertEquals(1, pedidos.size)
    }

    @Test
    fun cambiarDePeriodoDescartaLaCargaAnterior() = runTest(dispatcher) {
        val lenta = CompletableDeferred<Result<ResumenSouthbound>>()
        val otro = resumen.copy(totalServicios = 15)
        var llamada = 0
        respuesta = { if (++llamada == 1) lenta.await() else Result.success(otro) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.SIETE_DIAS)
        advanceUntilIdle()
        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()
        lenta.complete(Result.success(resumen))
        advanceUntilIdle()

        assertEquals(otro, vm.estado.value.resumen)
    }

    @Test
    fun unErrorDeLaApiSeMuestraConElRequestId() = runTest(dispatcher) {
        val texto = UiText.StringResource(R.string.api_bd_no_disponible)
        respuesta = { Result.failure(LinceApiException("No disponible", 503, requestId = "abc", texto = texto)) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()

        assertEquals(
            EstadoResumenSouthbound(error = UiText.StringResource(R.string.error_con_id, texto, "abc")),
            vm.estado.value
        )
    }

    @Test
    fun unErrorSinRequestIdSeMuestraSolo() = runTest(dispatcher) {
        respuesta = { Result.failure(LinceApiException("Sin red", texto = UiText.StringResource(R.string.api_sin_conexion))) }
        val vm = viewModel()

        vm.cargar(PeriodoResumen.HOY)
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.api_sin_conexion), vm.estado.value.error)

        respuesta = { Result.failure(IllegalStateException()) }
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.resumen_southbound_error), vm.estado.value.error)
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
        assertEquals(UiText.DynamicString("caída"), vm.estado.value.error)

        respuesta = { Result.success(resumen) }
        vm.reintentar()
        advanceUntilIdle()

        assertEquals(2, pedidos.size)
        assertEquals(pedidos[0], pedidos[1])
        assertEquals(EstadoResumenSouthbound(resumen = resumen), vm.estado.value)
    }
}
