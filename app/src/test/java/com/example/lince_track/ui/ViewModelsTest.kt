package com.example.lince_track.ui

import com.example.lince_track.R
import com.example.lince_track.data.BloqueoRepository
import com.example.lince_track.data.Datos
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.SinRolException
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.ui.agenda.AgendaViewModel
import com.example.lince_track.ui.login.LoginEstado
import com.example.lince_track.ui.login.LoginViewModel
import com.example.lince_track.util.UiText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Los ViewModel reciben sus dependencias por el constructor, así que aquí se les pasan funciones simuladas con MockK
 * y no se toca Firebase. viewModelScope usa Dispatchers.Main, que en la JVM se reemplaza por uno de prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(dispatcher)

    @After
    fun limpiar() = Dispatchers.resetMain()

    // ---------- AgendaViewModel ----------

    private val hoy = LocalDate.of(2026, 11, 20)
    private val servicio: Servicio = Datos.servicio(3008)
    // Lambdas de prueba en vez de mocks: registran las llamadas y devuelven lo que diga cada test.
    private val aceptados = mutableListOf<String>()
    private var respuestaAceptar: suspend (String) -> Result<String?> = { Result.success(null) }
    private var respuestaRechazar: suspend (MotivoRechazo) -> Result<String?> = { Result.success(null) }

    private fun agenda() = AgendaViewModel(
        servicios = MutableStateFlow(listOf(servicio)),
        aceptarServicio = { id -> aceptados += id; respuestaAceptar(id) },
        rechazarServicio = { _, motivo, _ -> respuestaRechazar(motivo) },
        // Sin backend real: el repositorio de bloqueos recibe una API simulada y no carga nada al crearse.
        bloqueoRepository = BloqueoRepository(mockk<BloqueoApi>(relaxed = true)),
        hoy = { hoy },
        autoCargarBloqueos = false
    )

    @Test
    fun laAgendaPartEnHoyYNavegaEntreMeses() {
        val vm = agenda()
        assertEquals(hoy, vm.selectedDate.value)
        assertEquals(YearMonth.of(2026, 11), vm.month.value)
        assertEquals(listOf(servicio), vm.servicios.value)

        vm.mesSiguiente()
        vm.mesSiguiente()
        assertEquals(YearMonth.of(2027, 1), vm.month.value)
        vm.mesAnterior()
        assertEquals(YearMonth.of(2026, 12), vm.month.value)

        vm.seleccionarFecha(LocalDate.of(2026, 9, 3))
        assertEquals(YearMonth.of(2026, 9), vm.month.value)
        vm.irHoy()
        assertEquals(hoy, vm.selectedDate.value)
        assertEquals(YearMonth.of(2026, 11), vm.month.value)
    }

    @Test
    fun aceptarMarcaElServicioEnProcesoYMuestraElAviso() = runTest(dispatcher) {
        val respuesta = CompletableDeferred<Result<String?>>()
        respuestaAceptar = { respuesta.await() }
        val vm = agenda()

        vm.aceptar(servicio)
        advanceUntilIdle()
        assertEquals(setOf("3008"), vm.processingIds.value)

        vm.aceptar(servicio) // mientras está en proceso, un segundo toque no hace nada
        respuesta.complete(Result.success("Sin conexión: se enviará al recuperar la señal."))
        advanceUntilIdle()

        assertTrue(vm.processingIds.value.isEmpty())
        assertEquals(UiText.StringResource(R.string.aviso_en_cola), vm.message.value)
        assertEquals(listOf("3008"), aceptados)
        vm.limpiarMensaje()
        assertNull(vm.message.value)
    }

    @Test
    fun aceptarSinAvisoNoMuestraMensaje() = runTest(dispatcher) {
        val vm = agenda()
        vm.aceptar(servicio)
        advanceUntilIdle()
        assertNull(vm.message.value)
    }

    @Test
    fun unRechazoFallidoMuestraElError() = runTest(dispatcher) {
        respuestaRechazar = { motivo ->
            if (motivo == MotivoRechazo.OTRO) Result.failure(IllegalArgumentException("Indica el motivo del rechazo"))
            else Result.failure(IllegalStateException())
        }
        val vm = agenda()

        vm.rechazar(servicio, MotivoRechazo.OTRO, "")
        advanceUntilIdle()
        assertEquals(UiText.DynamicString("Indica el motivo del rechazo"), vm.message.value)

        vm.rechazar(servicio, MotivoRechazo.FUERZA_MAYOR, "")
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.agenda_operacion_fallida), vm.message.value)
    }

    // ---------- LoginViewModel ----------

    private var rolGuardado: Rol? = null
    private val intentos = mutableListOf<String>()
    private var respuestaLogin: suspend () -> Result<Rol> = { Result.success(Rol.CONDUCTOR) }

    private fun login(uid: String? = null) = LoginViewModel(
        uidActual = { uid },
        cargarRol = { rolGuardado },
        iniciar = { correo, _ -> intentos += correo; respuestaLogin() }
    )

    @Test
    fun sinSesionAbiertaEmpiezaVacio() {
        val vm = login()
        assertEquals(LoginEstado(), vm.estado.value)
    }

    @Test
    fun conSesionAbiertaRecuperaElRol() = runTest(dispatcher) {
        rolGuardado = Rol.CONDUCTOR
        val vm = login("carlos")
        assertTrue(vm.estado.value.cargando)
        advanceUntilIdle()
        assertEquals(LoginEstado(rol = Rol.CONDUCTOR), vm.estado.value)
    }

    @Test
    fun siNoSePuedeRecuperarLaSesionVuelveAlFormulario() = runTest(dispatcher) {
        rolGuardado = null
        val vm = login("carlos")
        advanceUntilIdle()
        assertEquals(LoginEstado(), vm.estado.value)
    }

    @Test
    fun camposVaciosNoLlamanAFirebase() {
        val vm = login()
        vm.iniciarSesion(" ", "clave")
        assertEquals(UiText.StringResource(R.string.login_error_campos_vacios), vm.estado.value.error)
        vm.limpiarError()
        assertNull(vm.estado.value.error)
        assertTrue(intentos.isEmpty())
    }

    @Test
    fun correoConFormatoInvalidoNoLlamaAFirebase() {
        val vm = login()
        for (correo in listOf("carlos", "carlos@", "carlos@southbound", "car los@southbound.cl")) {
            vm.iniciarSesion(correo, "clave")
            assertEquals(correo, UiText.StringResource(R.string.login_error_correo_invalido), vm.estado.value.error)
        }
        assertTrue(intentos.isEmpty())
    }

    @Test
    fun iniciarSesionEntregaElRolYNoSeRepiteMientrasCarga() = runTest(dispatcher) {
        val resultado = CompletableDeferred<Result<Rol>>()
        respuestaLogin = { resultado.await() }
        val vm = login()

        vm.iniciarSesion("admin@lince.cl", "clave")
        advanceUntilIdle()
        assertTrue(vm.estado.value.cargando)
        vm.iniciarSesion("admin@lince.cl", "clave")
        resultado.complete(Result.success(Rol.ADMINISTRADOR))
        advanceUntilIdle()

        assertEquals(Rol.ADMINISTRADOR, vm.estado.value.rol)
        assertFalse(vm.estado.value.cargando)
        assertEquals(listOf("admin@lince.cl"), intentos)
    }

    private fun mensajePara(error: Throwable): UiText? {
        respuestaLogin = { Result.failure(error) }
        val vm = login()
        vm.iniciarSesion("a@b.cl", "x")
        dispatcher.scheduler.advanceUntilIdle()
        return vm.estado.value.error
    }

    @Test
    fun cadaErrorDeFirebaseTieneUnMensajeClaro() {
        assertEquals(UiText.DynamicString("Tu cuenta no está configurada."), mensajePara(SinRolException("Tu cuenta no está configurada.")))
        assertEquals(UiText.StringResource(R.string.login_error_usuario_no_existe), mensajePara(mockk<FirebaseAuthInvalidUserException>()))
        assertEquals(UiText.StringResource(R.string.login_error_credenciales), mensajePara(mockk<FirebaseAuthInvalidCredentialsException>()))
        assertEquals(UiText.StringResource(R.string.login_error_red), mensajePara(FirebaseNetworkException("sin red")))
        assertEquals(UiText.DynamicString("Algo raro"), mensajePara(IllegalStateException("Algo raro")))
        assertEquals(UiText.StringResource(R.string.login_error_generico), mensajePara(IllegalStateException()))
    }

    @Test
    fun losDosViewModelsSePuedenCrearSinArgumentos() {
        // Compose los crea con viewModel(), que necesita el constructor sin argumentos.
        assertSame(0, AgendaViewModel::class.java.getConstructor().parameterCount)
        assertSame(0, LoginViewModel::class.java.getConstructor().parameterCount)
    }
}
