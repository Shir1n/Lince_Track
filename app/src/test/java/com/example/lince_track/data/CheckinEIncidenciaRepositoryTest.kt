package com.example.lince_track.data

import com.example.lince_track.data.FirebaseSimulado.documento
import com.example.lince_track.data.FirebaseSimulado.instantanea
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaPendiente
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/** CheckinRepository e IncidenciaRepository con Firestore simulado. */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckinEIncidenciaRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val checkins = mockk<CollectionReference>()
    private val incidencias = mockk<CollectionReference>()

    @Before
    fun preparar() {
        every { firestore.collection("checkins") } returns checkins
        every { firestore.collection("incidencias") } returns incidencias
        CheckinRepository.firestoreReemplazable.reemplazar(firestore)
        IncidenciaRepository.firestoreReemplazable.reemplazar(firestore)
    }

    @After
    fun limpiar() {
        CheckinRepository.firestoreReemplazable.reemplazar(null)
        IncidenciaRepository.firestoreReemplazable.reemplazar(null)
    }

    // ---------- Check-in ----------

    @Test
    fun observarCheckinsLosAgrupaPorServicio() = runTest {
        val porUid = mockk<Query>()
        every { checkins.whereEqualTo("uid", "u1") } returns porUid
        val escucha = FirebaseSimulado.Escucha(porUid)
        val recibidos = mutableListOf<Map<String, CheckinServicio>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { CheckinRepository.observar("u1").toList(recibidos) }

        escucha.fallar()
        escucha.enviar(
            instantanea(
                documento("u1_1", mapOf("servicioId" to "1", "checkInEn" to Date(1_000), "distanciaM" to 40L,
                    "gpsReal" to true, "checkOutEn" to Date(5_000), "uid" to "u1")),
                documento("u1_2", mapOf("servicioId" to "2", "checkInEn" to Date(2_000))),
                documento("u1_3", mapOf("servicioId" to "3")), // sin hora de check-in: se descarta
                documento("u1_4", mapOf("checkInEn" to Date(3_000)))
            )
        )
        job.cancel()

        val mapa = recibidos.single()
        assertEquals(setOf("1", "2"), mapa.keys)
        assertTrue(mapa.getValue("1").finalizado)
        assertEquals(40, mapa.getValue("1").distanciaM)
        assertFalse(mapa.getValue("2").gpsReal)
        assertEquals(0, mapa.getValue("2").distanciaM)
    }

    @Test
    fun elAdministradorEscuchaTodosLosCheckins() = runTest {
        val escucha = FirebaseSimulado.Escucha(checkins)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { CheckinRepository.observar(null).collect {} }
        assertTrue(escucha.registrada)
        job.cancel()
    }

    @Test
    fun registrarCheckInYCheckOutEscribenElMismoDocumento() = runTest {
        val doc = mockk<DocumentReference>()
        every { checkins.document("u1_3008") } returns doc
        val entrada = slot<Any>()
        val salida = slot<Map<String, Any>>()
        every { doc.set(capture(entrada)) } returns tareaExitosa()
        every { doc.update(capture(salida)) } returns tareaPendiente()

        assertTrue(CheckinRepository.registrarCheckIn("u1", Rol.CONDUCTOR, "3008", -41.3, -72.9, 35, true))
        assertFalse(CheckinRepository.registrarCheckOut("u1", "3008", -41.31, -72.91)) // sin conexión: en cola

        val datos = entrada.captured as Map<*, *>
        assertEquals("conductor", datos["rol"])
        assertEquals(35, datos["distanciaM"])
        assertEquals(true, datos["gpsReal"])
        assertEquals(-41.31, salida.captured["latFin"])
    }

    // ---------- Incidencias ----------

    @Test
    fun observarIncidenciasDeLaPersonaYDescartaLasIncompletas() = runTest {
        val porUid = mockk<Query>()
        every { incidencias.whereEqualTo("uid", "u1") } returns porUid
        val escucha = FirebaseSimulado.Escucha(porUid)
        val recibidas = mutableListOf<List<IncidenciaServicio>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            IncidenciaRepository.observar("u1", "2026-11-13").toList(recibidas)
        }

        escucha.fallar()
        escucha.enviar(
            instantanea(
                documento("i1", mapOf("servicioId" to "3008", "uid" to "u1", "tipo" to "falla_mecanica",
                    "creadaEn" to Date(1_000), "titulo" to "Tour", "nombre" to "Carlos", "detalle" to "Pana",
                    "resuelta" to false, "enGestion" to true, "respuesta" to "Va el reemplazo",
                    "lat" to -41.3, "lon" to -72.9)),
                documento("i2", mapOf("servicioId" to "3008", "uid" to "u1", "tipo" to "atraso", "creadaEn" to Date(2_000))),
                documento("i3", mapOf("servicioId" to "3008", "uid" to "u1", "tipo" to "desconocido", "creadaEn" to Date(3_000))),
                documento("i4", mapOf("servicioId" to "3008", "uid" to "u1", "tipo" to "atraso")),
                documento("i5", mapOf("uid" to "u1", "tipo" to "atraso", "creadaEn" to Date(4_000))),
                documento("i6", mapOf("servicioId" to "3008", "tipo" to "atraso", "creadaEn" to Date(4_000)))
            )
        )
        job.cancel()

        val lista = recibidas.single()
        assertEquals(listOf("i1", "i2"), lista.map { it.id })
        assertEquals(TipoIncidencia.FALLA_MECANICA, lista[0].tipo)
        assertTrue(lista[0].enGestion)
        assertEquals(Ubicacion(-41.3, -72.9), lista[0].ubicacion)
        assertNull(lista[1].ubicacion)
        assertEquals("", lista[1].titulo)
    }

    @Test
    fun elAdministradorVeLasIncidenciasDesdeUnaFecha() = runTest {
        val desde = mockk<Query>()
        every { incidencias.whereGreaterThanOrEqualTo("fecha", "2026-11-13") } returns desde
        val escucha = FirebaseSimulado.Escucha(desde)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            IncidenciaRepository.observar(null, "2026-11-13").collect {}
        }
        assertTrue(escucha.registrada)
        job.cancel()
    }

    @Test
    fun reportarGuardaLaUbicacionSoloSiLaHay() = runTest {
        val conUbicacion = mockk<DocumentReference>()
        val sinUbicacion = mockk<DocumentReference>()
        every { incidencias.document() } returnsMany listOf(conUbicacion, sinUbicacion)
        val datos1 = slot<Any>()
        val datos2 = slot<Any>()
        every { conUbicacion.set(capture(datos1)) } returns tareaExitosa()
        every { sinUbicacion.set(capture(datos2)) } returns tareaExitosa()
        val servicio = Datos.servicio(3008)

        assertTrue(IncidenciaRepository.reportar("u1", Rol.CONDUCTOR, "Carlos", servicio, TipoIncidencia.ATRASO, "Taco", Ubicacion(-41.3, -72.9)))
        assertTrue(IncidenciaRepository.reportar("u1", Rol.CONDUCTOR, "Carlos", servicio, TipoIncidencia.ATRASO, "Taco", null))

        val primera = datos1.captured as Map<*, *>
        assertEquals(-41.3, primera["lat"])
        assertEquals("atraso", primera["tipo"])
        assertEquals(false, primera["resuelta"])
        assertEquals(servicio.fecha.toString(), primera["fecha"])
        assertFalse((datos2.captured as Map<*, *>).containsKey("lat"))
    }

    @Test
    fun responderYResolverActualizanLaIncidencia() = runTest {
        val doc = mockk<DocumentReference>()
        every { incidencias.document("i1") } returns doc
        val cambios = mutableListOf<Map<String, Any>>()
        every { doc.update(capture(cambios)) } returns tareaExitosa()

        assertTrue(IncidenciaRepository.responder("i1", "admin", "Va el reemplazo"))
        assertTrue(IncidenciaRepository.resolver("i1", "admin", "Listo"))
        assertTrue(IncidenciaRepository.resolver("i1", "admin", ""))

        assertEquals(true, cambios[0]["enGestion"])
        assertEquals("admin", cambios[0]["atendidaPor"])
        assertEquals("Listo", cambios[1]["respuesta"])
        assertEquals(true, cambios[1]["resuelta"])
        assertFalse(cambios[2].containsKey("respuesta")) // vacía: se mantiene la anterior
    }
}
