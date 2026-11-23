package com.example.lince_track.data.privacidad

import com.example.lince_track.data.FirebaseSimulado
import com.example.lince_track.data.FirebaseSimulado.documento
import com.example.lince_track.data.FirebaseSimulado.instantanea
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaFallida
import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.Rol
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

/** Solicitudes de eliminación y descarga de datos (Ley 21.719) con Firestore y el backend simulados. */
@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudPrivacidadRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val solicitudes = mockk<CollectionReference>()
    private val documentoU1 = mockk<DocumentReference>()
    private val api = mockk<BloqueoApi>()
    private val repo = SolicitudPrivacidadRepository

    @Before
    fun preparar() {
        every { firestore.collection("solicitudesPrivacidad") } returns solicitudes
        every { solicitudes.document("u1") } returns documentoU1
        repo.firestoreReemplazable.reemplazar(firestore)
        repo.apiReemplazable.reemplazar(api)
    }

    @After
    fun limpiar() {
        repo.firestoreReemplazable.reemplazar(null)
        repo.apiReemplazable.reemplazar(null)
    }

    private fun campos(estado: String = "pendiente") = mapOf(
        "uid" to "u1", "nombre" to "Patricio", "rol" to "conductor", "motivo" to "Me cambio",
        "estado" to estado, "creadaEn" to Date(1_000), "respuesta" to null
    )

    @Test
    fun observarEntregaLaSolicitudONull() = runTest {
        val listener = slot<EventListener<DocumentSnapshot>>()
        val registro = mockk<ListenerRegistration>(relaxed = true)
        every { documentoU1.addSnapshotListener(capture(listener)) } returns registro
        val recibidas = mutableListOf<SolicitudPrivacidad?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { repo.observar("u1").toList(recibidas) }

        listener.captured.onEvent(documento("u1", campos()), null)
        listener.captured.onEvent(documento("u1", emptyMap(), existe = false), null)
        listener.captured.onEvent(documento("u1", campos("desconocido")), null)
        listener.captured.onEvent(null, FirebaseSimulado.errorFirestore())
        job.cancel()

        assertEquals(
            listOf(SolicitudPrivacidad("u1", "Patricio", "conductor", "Me cambio", EstadoSolicitud.PENDIENTE, 1_000), null, null),
            recibidas
        )
        verify { registro.remove() }
    }

    @Test
    fun coordinacionVeLasPendientesLaMasAntiguaPrimero() = runTest {
        val consulta = mockk<Query>()
        every { solicitudes.whereEqualTo("estado", "pendiente") } returns consulta
        val escucha = FirebaseSimulado.Escucha(consulta)
        val recibidas = mutableListOf<List<SolicitudPrivacidad>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { repo.observarPendientes().toList(recibidas) }

        escucha.fallar()
        escucha.enviar(
            instantanea(
                documento("b", campos() + mapOf("uid" to "b", "creadaEn" to Date(2_000))),
                documento("a", campos() + mapOf("uid" to "a", "creadaEn" to null)),
                documento("c", campos() + mapOf("uid" to "c"))
            )
        )
        job.cancel()

        assertEquals(listOf(listOf("c", "b", "a")), recibidas.map { lista -> lista.map { it.uid } })
    }

    @Test
    fun solicitarYCancelarEscribenElDocumentoDeLaPersona() = runBlocking {
        val datos = slot<Map<String, Any>>()
        every { documentoU1.set(capture(datos)) } returns tareaExitosa(null)
        every { documentoU1.delete() } returns tareaExitosa(null)

        assertTrue(repo.solicitar("u1", PerfilUsuario(Rol.GUIA, nombre = "Alfredo"), "  " + "x".repeat(600)).isSuccess)
        assertTrue(repo.cancelar("u1").isSuccess)

        assertEquals("guia", datos.captured["rol"])
        assertEquals("Alfredo", datos.captured["nombre"])
        assertEquals("pendiente", datos.captured["estado"])
        assertEquals(500, (datos.captured["motivo"] as String).length)
        assertEquals(setOf("uid", "nombre", "rol", "motivo", "estado", "creadaEn"), datos.captured.keys)
    }

    @Test
    fun siFirestoreFallaSeInforma() = runBlocking {
        every { documentoU1.delete() } returns tareaFallida(FirebaseSimulado.errorFirestore())
        assertTrue(repo.cancelar("u1").isFailure)
    }

    @Test
    fun aprobarBorraPrimeroElBackendYDespuesMarcaLaSolicitud() = runBlocking {
        val cambios = slot<Map<String, Any>>()
        coEvery { api.borrarDatos("u1") } returns JsonObject(mapOf("hitos" to JsonPrimitive(3)))
        every { documentoU1.update(capture(cambios)) } returns tareaExitosa(null)

        assertTrue(repo.aprobar("u1", "admin1").isSuccess)

        assertEquals("aprobada", cambios.captured["estado"])
        assertEquals("", cambios.captured["respuesta"])
        assertEquals("admin1", cambios.captured["atendidaPor"])
    }

    @Test
    fun siElBackendNoBorraNoSeApruebaYSePuedeReintentar() = runBlocking {
        coEvery { api.borrarDatos("u1") } throws IOException("sin red")

        val error = repo.aprobar("u1", "admin1").exceptionOrNull()

        assertTrue(error is BloqueoApiException)
        verify(exactly = 0) { documentoU1.update(any<Map<String, Any>>()) }
    }

    @Test
    fun rechazarExigeMotivo() = runBlocking {
        val cambios = slot<Map<String, Any>>()
        every { documentoU1.update(capture(cambios)) } returns tareaExitosa(null)

        assertTrue(repo.rechazar("u1", "admin1", "   ").isFailure)
        assertTrue(repo.rechazar("u1", "admin1", "  Tienes servicios esta semana ").isSuccess)

        assertEquals("rechazada", cambios.captured["estado"])
        assertEquals("Tienes servicios esta semana", cambios.captured["respuesta"])
    }

    @Test
    fun exportarJuntaFirestoreYElBackendEnUnJson() = runBlocking {
        val usuarios = mockk<CollectionReference>()
        val cuenta = mockk<DocumentReference>()
        every { firestore.collection("usuarios") } returns usuarios
        every { usuarios.document("u1") } returns cuenta
        every { cuenta.get() } returns tareaExitosa(conDatos(mapOf("nombre" to "Patricio", "rol" to "conductor")))
        every { documentoU1.get() } returns tareaExitosa(conDatos(null, existe = false))
        for (coleccion in listOf("respuestas", "checkins", "incidencias")) {
            val col = mockk<CollectionReference>()
            val consulta = mockk<Query>()
            every { firestore.collection(coleccion) } returns col
            every { col.whereEqualTo("uid", "u1") } returns consulta
            val docs = if (coleccion == "checkins") {
                listOf(conDatos(mapOf("servicioId" to "3166", "checkInEn" to Timestamp(Date(0)), "lat" to -41.4)))
            } else {
                emptyList()
            }
            val snapshot = mockk<QuerySnapshot>()
            every { snapshot.documents } returns docs
            every { consulta.get() } returns tareaExitosa(snapshot)
        }
        coEvery { api.misDatos() } returns JsonObject(mapOf("uid" to JsonPrimitive("u1")))

        val texto = repo.exportar("u1").getOrThrow()
        val json = Json.parseToJsonElement(texto).jsonObject

        assertEquals("u1", json["uid"]!!.jsonPrimitive.content)
        val firestoreJson = json["firestore"]!!.jsonObject
        assertEquals("Patricio", firestoreJson["cuenta"]!!.jsonObject["nombre"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, firestoreJson["solicitudPrivacidad"])
        val checkin = firestoreJson["checkins"]!!.jsonArray.single().jsonObject
        assertEquals("1970-01-01T00:00:00Z", checkin["checkInEn"]!!.jsonPrimitive.content)
        assertEquals("u1", json["backend"]!!.jsonObject["uid"]!!.jsonPrimitive.content)
        coVerify { api.misDatos() }
    }

    @Test
    fun siElBackendNoRespondeLaDescargaFalla() = runBlocking {
        val usuarios = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection(any()) } answers {
            if (firstArg<String>() == "solicitudesPrivacidad") solicitudes else usuarios
        }
        every { usuarios.document(any()).get() } returns tareaExitosa(conDatos(null, existe = false))
        every { documentoU1.get() } returns tareaExitosa(conDatos(null, existe = false))
        val vacia = mockk<QuerySnapshot>()
        every { vacia.documents } returns emptyList()
        every { usuarios.whereEqualTo("uid", "u1").get() } returns tareaExitosa(vacia)
        coEvery { api.misDatos() } throws IOException("sin red")

        assertTrue(repo.exportar("u1").exceptionOrNull() is BloqueoApiException)
    }

    @Test
    fun losValoresDeFirestoreSePasanAJson() {
        val json = repo.aJson(
            mapOf("b" to listOf(1, true, null), "a" to "texto", "c" to mapOf("x" to 2.5), "d" to Rol.GUIA)
        ).jsonObject
        assertEquals(listOf("a", "b", "c", "d"), json.keys.toList())
        assertEquals("GUIA", json["d"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, json["b"]!!.jsonArray[2])
        assertNull(EstadoSolicitud.desde("otro"))
    }

    private fun conDatos(datos: Map<String, Any?>?, existe: Boolean = true): DocumentSnapshot {
        val doc = mockk<DocumentSnapshot>()
        every { doc.exists() } returns existe
        every { doc.data } returns datos
        return doc
    }
}
