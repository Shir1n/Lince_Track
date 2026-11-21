package com.example.lince_track.data

import com.example.lince_track.data.FirebaseSimulado.documento
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.servicios.FiltroServicios
import com.example.lince_track.data.servicios.ServiciosDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * ServicioRepository coordina la API de Southbound con lo guardado en Firestore. Aquí la fuente de servicios es
 * simulada, los repositorios de Firestore se reemplazan con mockkObject y la sesión se abre con AuthRepository real
 * sobre un FirebaseAuth simulado.
 */
class ServicioRepositoryTest {

    private val auth = mockk<FirebaseAuth>()
    private val firestore = mockk<FirebaseFirestore>()
    private val usuarios = mockk<CollectionReference>()
    private val fuente = mockk<ServiciosDataSource>()

    private val respuestas = MutableStateFlow<List<RespuestaServicio>>(emptyList())
    private val checkins = MutableStateFlow<Map<String, CheckinServicio>>(emptyMap())
    private val estados = MutableStateFlow<Map<String, RespuestaServicio>>(emptyMap())
    private val asignaciones = MutableStateFlow<Map<String, AsignacionServicio>>(emptyMap())
    private val incidencias = MutableStateFlow<List<IncidenciaServicio>>(emptyList())

    private val hoy = LocalDate.now()
    private val propio = Datos.dto(3008, fecha = hoy, transportistaId = 37, referencia = "TOUR1")
    private val delGuia = Datos.dto(3009, fecha = hoy, tipo = "GUIDED_SERVICE", transportistaId = null, guiaId = 35, referencia = "TOUR1")

    @Before
    fun preparar() {
        // Antes de tocar ServicioRepository: al crearse lee el usuario actual, y sin respuesta su escucha se detiene.
        every { auth.currentUser } returns null
        justRun { auth.signOut() }
        every { firestore.collection("usuarios") } returns usuarios
        AuthRepository.authReemplazable.reemplazar(auth)
        AuthRepository.firestoreReemplazable.reemplazar(firestore)
        ServicioRepository.fuenteReemplazable.reemplazar(fuente)

        mockkObject(RespuestaRepository, CheckinRepository, AsignacionRepository, IncidenciaRepository)
        every { RespuestaRepository.observar(any()) } returns respuestas
        every { RespuestaRepository.observarEstados(any()) } returns estados
        every { CheckinRepository.observar(any()) } returns checkins
        every { AsignacionRepository.observar(any()) } returns asignaciones
        every { IncidenciaRepository.observar(any(), any()) } returns incidencias
    }

    @After
    fun limpiar() = runBlocking {
        AuthRepository.cerrarSesion()
        esperar { !ServicioRepository.datosListos.value && ServicioRepository.servicios.value.isEmpty() }
        unmockkObject(RespuestaRepository, CheckinRepository, AsignacionRepository, IncidenciaRepository)
        ServicioRepository.fuenteReemplazable.reemplazar(null)
        AuthRepository.authReemplazable.reemplazar(FirebaseSimulado.authSinSesion())
        AuthRepository.firestoreReemplazable.reemplazar(null)
    }

    private suspend fun esperar(condicion: () -> Boolean) = withTimeout(5_000) {
        while (!condicion()) delay(10)
    }

    /** Abre sesión como [uid] con esos datos en usuarios/{uid} y espera a que lleguen la carga y las escuchas. */
    private fun entrar(uid: String, campos: Map<String, Any?>, propios: List<ServicioDto>, oficina: List<ServicioDto> = propios) =
        runBlocking {
            val usuario = mockk<FirebaseUser>()
            every { usuario.uid } returns uid
            every { auth.currentUser } returns usuario
            val ref = mockk<DocumentReference>()
            every { usuarios.document(uid) } returns ref
            every { ref.get() } returns tareaExitosa(documento(uid, campos))
            coEvery { fuente.obtener(any()) } answers {
                val filtro = firstArg<FiltroServicios>()
                if (filtro.transportistaId == null && filtro.guiaId == null) oficina else propios
            }
            every { fuente.nombre } returns "Simulada"
            AuthRepository.cargarRolActual(uid)
        }

    private fun entrarComoCarlos() {
        entrar("carlos", mapOf("rol" to "conductor", "nombre" to "Carlos", "telefono" to "+569", "transportistaId" to 37L),
            propios = listOf(propio), oficina = listOf(propio, delGuia))
        // datosListos llega un instante antes que la lista armada: se esperan ambas.
        runBlocking {
            withTimeout(5_000) { ServicioRepository.datosListos.first { it } }
            esperar { ServicioRepository.buscarPorId("3008") != null }
        }
    }

    private fun entrarComoAdmin(lista: List<ServicioDto> = listOf(propio, delGuia)) {
        entrar("admin", mapOf("rol" to "administrador", "nombre" to "Admin 1"), propios = lista)
        runBlocking {
            withTimeout(5_000) { ServicioRepository.datosListos.first { it } }
            esperar { ServicioRepository.servicios.value.size == lista.size }
        }
    }

    /** Hace que la respuesta de Carlos aparezca como aceptada. */
    private fun carlosAcepta(id: String = "3008") = runBlocking {
        respuestas.value = listOf(RespuestaServicio(id, EstadoServicio.ACEPTADO, respondidoEn = 1L, uid = "carlos"))
        esperar { ServicioRepository.buscarPorId(id)?.estado == EstadoServicio.ACEPTADO }
    }

    // ---------- Carga ----------

    @Test
    fun elConductorVeLoSuyoYLaOficinaSirveParaElCompanero() {
        entrarComoCarlos()

        assertEquals(listOf("3008"), ServicioRepository.servicios.value.map { it.id })
        assertEquals(EstadoServicio.PENDIENTE, ServicioRepository.buscarPorId("3008")?.estado)
        assertEquals(false, ServicioRepository.cargando.value)
        assertNull(ServicioRepository.error.value)
        coVerify { fuente.obtener(match { it.transportistaId == 37L }) }
        coVerify { fuente.obtener(match { it.transportistaId == null && it.guiaId == null }) }
    }

    @Test
    fun unaCuentaSinIdDeLaApiNoCargaNadaYLoAvisa() = runBlocking {
        entrar("sinid", mapOf("rol" to "conductor"), propios = listOf(propio))
        esperar { ServicioRepository.error.value != null }
        assertEquals("Tu cuenta aún no está vinculada. Contacta al administrador.", ServicioRepository.error.value)
        assertTrue(ServicioRepository.servicios.value.isEmpty())
    }

    @Test
    fun siLaApiFallaSeMuestraElError() = runBlocking {
        entrar("carlos", mapOf("rol" to "conductor", "transportistaId" to 37L), propios = emptyList())
        withTimeout(5_000) { ServicioRepository.datosListos.first { it } }
        coEvery { fuente.obtener(any()) } throws LinceApiException("La API no responde", 503)
        ServicioRepository.refrescar()?.join()
        esperar { ServicioRepository.error.value != null }
        assertEquals("La API no responde", ServicioRepository.error.value)
    }

    @Test
    fun siFallaSoloLaOficinaSeMantieneLaListaPropia() = runBlocking {
        entrar("carlos", mapOf("rol" to "conductor", "transportistaId" to 37L), propios = listOf(propio))
        withTimeout(5_000) { ServicioRepository.datosListos.first { it } }
        coEvery { fuente.obtener(match { it.transportistaId == null }) } throws IllegalStateException("caída")
        ServicioRepository.refrescar()?.join()
        esperar { ServicioRepository.servicios.value.isNotEmpty() }
        assertEquals("3008", ServicioRepository.servicios.value.single().id)
        assertNull(ServicioRepository.error.value)
    }

    // ---------- Aceptar y rechazar ----------

    @Test
    fun aceptarGuardaLaRespuestaConLosDatosDelPerfil() = runBlocking {
        entrarComoCarlos()
        coEvery { RespuestaRepository.guardar(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        assertEquals(Result.success(null), ServicioRepository.aceptar("3008"))
        coVerify {
            RespuestaRepository.guardar("carlos", Rol.CONDUCTOR, "Carlos", "+569", "3008", hoy.toString(),
                EstadoServicio.ACEPTADO, null, "")
        }
    }

    @Test
    fun sinConexionLaRespuestaQuedaEnColaYUnErrorSeInforma() = runBlocking {
        entrarComoCarlos()
        coEvery { RespuestaRepository.guardar(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns false
        assertEquals("Sin conexión: se enviará al recuperar la señal.", ServicioRepository.aceptar("3008").getOrNull())

        coEvery { RespuestaRepository.guardar(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws IllegalStateException("x")
        assertEquals("No se pudo guardar tu respuesta. Intenta nuevamente.", ServicioRepository.aceptar("3008").exceptionOrNull()?.message)
    }

    @Test
    fun rechazarExigeMotivoValido() = runBlocking {
        entrarComoCarlos()
        coEvery { RespuestaRepository.guardar(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        assertEquals("Indica el motivo del rechazo",
            ServicioRepository.rechazar("3008", MotivoRechazo.OTRO, "  ").exceptionOrNull()?.message)
        assertTrue(ServicioRepository.rechazar("3008", MotivoRechazo.FALLA_MECANICA, " Sin frenos ").isSuccess)
        coVerify {
            RespuestaRepository.guardar("carlos", any(), any(), any(), "3008", any(), EstadoServicio.RECHAZADO,
                MotivoRechazo.FALLA_MECANICA, "Sin frenos")
        }
    }

    @Test
    fun noSePuedeResponderLoQueNoExisteOYaFueRespondido() = runBlocking {
        entrarComoCarlos()
        assertEquals("Asignación no encontrada", ServicioRepository.aceptar("9999").exceptionOrNull()?.message)
        carlosAcepta()
        assertEquals("Esta asignación ya fue respondida", ServicioRepository.aceptar("3008").exceptionOrNull()?.message)
    }

    @Test
    fun sinSesionNadaSeGuarda() = runBlocking {
        every { auth.currentUser } returns null
        val mensaje = "Tu sesión no está lista. Intenta nuevamente."
        assertEquals(mensaje, ServicioRepository.aceptar("3008").exceptionOrNull()?.message)
        assertEquals(mensaje, ServicioRepository.registrarCheckIn("3008", 0.0, 0.0, 0, true).exceptionOrNull()?.message)
        assertEquals(mensaje, ServicioRepository.reportarIncidencia("3008", TipoIncidencia.ATRASO, "", null).exceptionOrNull()?.message)
    }

    // ---------- Check-in y check-out ----------

    @Test
    fun elRecorridoCheckInCheckOutRespetaElOrden() = runBlocking {
        entrarComoCarlos()
        coEvery { CheckinRepository.registrarCheckIn(any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { CheckinRepository.registrarCheckOut(any(), any(), any(), any()) } returns false

        assertEquals("Solo se registra en servicios aceptados",
            ServicioRepository.registrarCheckIn("3008", -41.3, -72.9, 40, true).exceptionOrNull()?.message)
        assertEquals("Asignación no encontrada",
            ServicioRepository.registrarCheckIn("1", -41.3, -72.9, 40, true).exceptionOrNull()?.message)
        carlosAcepta()
        assertEquals("No hay un check-in pendiente de cerrar",
            ServicioRepository.registrarCheckOut("3008", -41.3, -72.9).exceptionOrNull()?.message)

        assertEquals(Result.success(null), ServicioRepository.registrarCheckIn("3008", -41.3, -72.9, 40, true))
        coVerify { CheckinRepository.registrarCheckIn("carlos", Rol.CONDUCTOR, "3008", -41.3, -72.9, 40, true) }

        checkins.value = mapOf("3008" to CheckinServicio("3008", 1L, 40, true, uid = "carlos"))
        esperar { ServicioRepository.checkins.value.containsKey("3008") }
        assertEquals("El check-in ya está registrado",
            ServicioRepository.registrarCheckIn("3008", -41.3, -72.9, 40, true).exceptionOrNull()?.message)
        assertEquals("Sin conexión: se enviará al recuperar la señal.",
            ServicioRepository.registrarCheckOut("3008", -41.31, -72.91).getOrNull())

        checkins.value = mapOf("3008" to CheckinServicio("3008", 1L, 40, true, checkOutEn = 2L, uid = "carlos"))
        esperar { ServicioRepository.checkins.value["3008"]?.finalizado == true }
        assertEquals("Este servicio ya terminó", ServicioRepository.rechazar("3008", MotivoRechazo.FUERZA_MAYOR, "").exceptionOrNull()?.message)
        assertEquals("Este servicio ya no admite incidencias",
            ServicioRepository.reportarIncidencia("3008", TipoIncidencia.ATRASO, "", null).exceptionOrNull()?.message)
    }

    @Test
    fun unErrorAlGuardarElCheckInSeInforma() = runBlocking {
        entrarComoCarlos()
        carlosAcepta()
        coEvery { CheckinRepository.registrarCheckIn(any(), any(), any(), any(), any(), any(), any()) } throws IllegalStateException("x")
        assertEquals("No se pudo guardar el registro. Intenta nuevamente.",
            ServicioRepository.registrarCheckIn("3008", 0.0, 0.0, 0, false).exceptionOrNull()?.message)
    }

    // ---------- Incidencias del conductor ----------

    @Test
    fun reportarUnaIncidenciaValida() = runBlocking {
        entrarComoCarlos()
        carlosAcepta()
        coEvery { IncidenciaRepository.reportar(any(), any(), any(), any(), any(), any(), any()) } returns true
        val ubicacion = Ubicacion(-41.3, -72.9)

        assertEquals("Describe lo que pasó",
            ServicioRepository.reportarIncidencia("3008", TipoIncidencia.OTRO, " ", null).exceptionOrNull()?.message)
        assertEquals(Result.success(null),
            ServicioRepository.reportarIncidencia("3008", TipoIncidencia.FALLA_MECANICA, " Pana ", ubicacion))
        coVerify {
            IncidenciaRepository.reportar("carlos", Rol.CONDUCTOR, "Carlos", any(), TipoIncidencia.FALLA_MECANICA, "Pana", ubicacion)
        }

        coEvery { IncidenciaRepository.reportar(any(), any(), any(), any(), any(), any(), any()) } throws IllegalStateException("x")
        assertEquals("No se pudo enviar la incidencia. Intenta nuevamente.",
            ServicioRepository.reportarIncidencia("3008", TipoIncidencia.ATRASO, "", null).exceptionOrNull()?.message)
        assertEquals("Asignación no encontrada",
            ServicioRepository.reportarIncidencia("1", TipoIncidencia.ATRASO, "", null).exceptionOrNull()?.message)
    }

    @Test
    fun elConductorNoGestionaIncidenciasNiReasigna() = runBlocking {
        entrarComoCarlos()
        val incidencia = IncidenciaServicio("i1", "3008", "Tour", "carlos", "Carlos", TipoIncidencia.ATRASO, "", 1L)
        assertEquals("Solo el administrador gestiona incidencias",
            ServicioRepository.resolverIncidencia(incidencia).exceptionOrNull()?.message)
        assertEquals("Solo el administrador puede reasignar",
            ServicioRepository.reasignar(ServicioRepository.buscarPorId("3008")!!,
                PersonaDisponible("p", "Patricio", Rol.CONDUCTOR, 43L)).exceptionOrNull()?.message)
    }

    // ---------- Administrador ----------

    @Test
    fun elAdministradorVeTodaLaOficinaYNoRespondeNiMarca() = runBlocking {
        entrarComoAdmin()
        assertEquals(setOf("3008", "3009"), ServicioRepository.servicios.value.map { it.id }.toSet())
        assertEquals("El administrador no responde servicios", ServicioRepository.aceptar("3008").exceptionOrNull()?.message)
        assertEquals("El administrador no registra check-in",
            ServicioRepository.registrarCheckIn("3008", 0.0, 0.0, 0, true).exceptionOrNull()?.message)
        assertEquals("Tu sesión no está lista. Intenta nuevamente.",
            ServicioRepository.reportarIncidencia("3008", TipoIncidencia.ATRASO, "", null).exceptionOrNull()?.message)
    }

    @Test
    fun elAdministradorRespondeYResuelveIncidencias() = runBlocking {
        entrarComoAdmin()
        val incidencia = IncidenciaServicio("i1", "3008", "Tour", "carlos", "Carlos", TipoIncidencia.ATRASO, "", 1L)
        coEvery { IncidenciaRepository.responder(any(), any(), any()) } returns true
        coEvery { IncidenciaRepository.resolver(any(), any(), any()) } returns false

        assertEquals("Escribe una respuesta", ServicioRepository.responderIncidencia(incidencia, "  ").exceptionOrNull()?.message)
        assertEquals(Result.success(null), ServicioRepository.responderIncidencia(incidencia, " Va el reemplazo "))
        coVerify { IncidenciaRepository.responder("i1", "admin", "Va el reemplazo") }
        assertEquals("Sin conexión: se enviará al recuperar la señal.", ServicioRepository.resolverIncidencia(incidencia).getOrNull())
        assertEquals("El mensaje es demasiado largo",
            ServicioRepository.resolverIncidencia(incidencia, "x".repeat(5_000)).exceptionOrNull()?.message)

        coEvery { IncidenciaRepository.resolver(any(), any(), any()) } throws IllegalStateException("x")
        assertEquals("No se pudo marcar como resuelta. Intenta nuevamente.",
            ServicioRepository.resolverIncidencia(incidencia, "Listo").exceptionOrNull()?.message)
    }

    @Test
    fun reasignarSoloLoQueAunSePuede() = runBlocking {
        entrarComoAdmin()
        val patricio = PersonaDisponible("patricio", "Patricio Lagos", Rol.CONDUCTOR, 43L, "+56943")
        coEvery { AsignacionRepository.reasignar(any(), any(), any()) } returns true

        assertEquals(Result.success(null), ServicioRepository.reasignar(ServicioRepository.buscarPorId("3008")!!, patricio))
        coVerify { AsignacionRepository.reasignar(any(), patricio, "admin") }

        // Aceptado y sin incidencia abierta: ya no se reasigna.
        respuestas.value = listOf(RespuestaServicio("3008", EstadoServicio.ACEPTADO, respondidoEn = 1L, uid = "carlos"))
        esperar { ServicioRepository.buscarPorId("3008")?.estado == EstadoServicio.ACEPTADO }
        assertEquals("Este servicio ya no se puede reasignar",
            ServicioRepository.reasignar(ServicioRepository.buscarPorId("3008")!!, patricio).exceptionOrNull()?.message)

        // Con una incidencia abierta sí se puede enviar reemplazo.
        incidencias.value = listOf(IncidenciaServicio("i1", "3008", "Tour", "carlos", "Carlos", TipoIncidencia.FALLA_MECANICA, "", 1L))
        esperar { ServicioRepository.incidencias.value.isNotEmpty() }
        coEvery { AsignacionRepository.reasignar(any(), any(), any()) } throws IllegalStateException("x")
        assertEquals("No se pudo reasignar. Intenta nuevamente.",
            ServicioRepository.reasignar(ServicioRepository.buscarPorId("3008")!!, patricio).exceptionOrNull()?.message)
    }

    @Test
    fun candidatosYTelefonosSalenDelPersonal() = runBlocking {
        entrarComoAdmin()
        val personal = listOf(
            PersonaDisponible("carlos", "Carlos", Rol.CONDUCTOR, 37L, "+56937"),
            PersonaDisponible("patricio", "Patricio Lagos", Rol.CONDUCTOR, 43L, "+56943"),
            PersonaDisponible("alfredo", "Alfredo", Rol.GUIA, 35L, "")
        )
        coEvery { AsignacionRepository.personal() } returns personal

        // Carlos ya tiene el 3008 según la API, así que no aparece como candidato.
        assertEquals(listOf("patricio"), ServicioRepository.candidatosPara(ServicioRepository.buscarPorId("3008")!!).getOrThrow().map { it.uid })
        assertEquals(mapOf("carlos" to "+56937", "patricio" to "+56943"), ServicioRepository.telefonosDelPersonal())

        coEvery { AsignacionRepository.personal() } throws IllegalStateException("sin permiso")
        assertEquals(emptyMap<String, String>(), ServicioRepository.telefonosDelPersonal())
        assertEquals("No se pudo cargar el personal. Intenta nuevamente.",
            ServicioRepository.candidatosPara(ServicioRepository.buscarPorId("3008")!!).exceptionOrNull()?.message)
    }

    @Test
    fun loReasignadoAOtraPersonaYaNoLoVeQuienLoTenia() = runBlocking {
        entrarComoCarlos()
        asignaciones.value = mapOf("3008" to AsignacionServicio("3008", "patricio", "Patricio Lagos"))
        esperar { ServicioRepository.servicios.value.isEmpty() }
        assertEquals("patricio", ServicioRepository.asignaciones.value.getValue("3008").uid)
    }
}
