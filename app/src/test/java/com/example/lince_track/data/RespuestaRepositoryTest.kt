package com.example.lince_track.data

import com.example.lince_track.data.FirebaseSimulado.documento
import com.example.lince_track.data.FirebaseSimulado.instantanea
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaPendiente
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RespuestaRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val respuestas = mockk<CollectionReference>()
    private val estados = mockk<CollectionReference>()

    @Before
    fun preparar() {
        every { firestore.collection("respuestas") } returns respuestas
        every { firestore.collection("estadosServicio") } returns estados
        RespuestaRepository.firestoreReemplazable.reemplazar(firestore)
    }

    @After
    fun limpiar() = RespuestaRepository.firestoreReemplazable.reemplazar(null)

    @Test
    fun observarTraeLasRespuestasDeLaPersonaOrdenadasYDescartaLasInvalidas() = runTest {
        val porUid = mockk<Query>()
        every { respuestas.whereEqualTo("uid", "u1") } returns porUid
        val escucha = FirebaseSimulado.Escucha(porUid)
        val recibidas = mutableListOf<List<RespuestaServicio>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            RespuestaRepository.observar("u1").toList(recibidas)
        }

        escucha.fallar() // un error no corta la escucha ni manda nada
        escucha.enviar(
            instantanea(
                documento("a", mapOf("servicioId" to "2", "estado" to "rechazado", "motivo" to "otro",
                    "observaciones" to "Enfermo", "respondidoEn" to Date(2_000), "uid" to "u1")),
                documento("b", mapOf("servicioId" to "1", "estado" to "aceptado", "respondidoEn" to Date(1_000))),
                documento("c", mapOf("servicioId" to "3", "estado" to "aceptado")), // sin hora del servidor: al final
                documento("d", mapOf("servicioId" to "4", "estado" to "raro")),
                documento("e", mapOf("estado" to "aceptado"))
            )
        )
        job.cancel()

        assertEquals(1, recibidas.size)
        val lista = recibidas.single()
        assertEquals(listOf("1", "2", "3"), lista.map { it.servicioId })
        assertEquals(MotivoRechazo.OTRO, lista[1].motivo)
        assertEquals("Enfermo", lista[1].observaciones)
        assertEquals(EstadoServicio.RECHAZADO, lista[1].estado)
        verify { escucha.registro.remove() }
    }

    @Test
    fun sinUidElAdministradorEscuchaTodaLaColeccion() = runTest {
        val escucha = FirebaseSimulado.Escucha(respuestas)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { RespuestaRepository.observar(null).collect {} }
        assertTrue(escucha.registrada)
        job.cancel()
    }

    @Test
    fun observarEstadosTraeLoPublicadoPorServicio() = runTest {
        val desde = mockk<Query>()
        every { estados.whereGreaterThanOrEqualTo("fecha", "2026-11-13") } returns desde
        val escucha = FirebaseSimulado.Escucha(desde)
        val recibidos = mutableListOf<Map<String, RespuestaServicio>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            RespuestaRepository.observarEstados("2026-11-13").toList(recibidos)
        }

        escucha.fallar()
        escucha.enviar(
            instantanea(
                documento("3009", mapOf("estado" to "aceptado", "nombre" to "Alfredo", "telefono" to "+569")),
                documento("3010", mapOf("estado" to "rechazado")),
                documento("3011", mapOf("estado" to "otro"))
            )
        )
        job.cancel()

        val mapa = recibidos.single()
        assertEquals(setOf("3009", "3010"), mapa.keys)
        assertEquals("Alfredo", mapa.getValue("3009").nombre)
        assertEquals("+569", mapa.getValue("3009").telefono)
        assertEquals(EstadoServicio.RECHAZADO, mapa.getValue("3010").estado)
    }

    private fun prepararLote(): Triple<WriteBatch, io.mockk.CapturingSlot<Any>, io.mockk.CapturingSlot<Any>> {
        val lote = mockk<WriteBatch>()
        val docRespuesta = mockk<DocumentReference>()
        val docEstado = mockk<DocumentReference>()
        val datos = slot<Any>()
        val publicado = slot<Any>()
        every { firestore.batch() } returns lote
        every { respuestas.document("u1_3008") } returns docRespuesta
        every { estados.document("3008") } returns docEstado
        every { lote.set(docRespuesta, capture(datos)) } returns lote
        every { lote.set(docEstado, capture(publicado)) } returns lote
        return Triple(lote, datos, publicado)
    }

    @Test
    fun guardarUnRechazoEscribeLaRespuestaYElEstadoEnUnSoloLote() = runTest {
        val (lote, datos, publicado) = prepararLote()
        every { lote.commit() } returns tareaExitosa()

        val confirmada = RespuestaRepository.guardar(
            "u1", Rol.CONDUCTOR, "Carlos", "+56911", "3008", "2026-11-20",
            EstadoServicio.RECHAZADO, MotivoRechazo.FALLA_MECANICA, "Sin frenos"
        )

        assertTrue(confirmada)
        val respuesta = datos.captured as Map<*, *>
        assertEquals("rechazado", respuesta["estado"])
        assertEquals("falla_mecanica", respuesta["motivo"])
        assertEquals("Sin frenos", respuesta["observaciones"])
        assertEquals("conductor", respuesta["rol"])
        assertTrue(respuesta["respondidoEn"] is FieldValue)
        val estado = publicado.captured as Map<*, *>
        assertEquals("Carlos", estado["nombre"])
        assertEquals("+56911", estado["telefono"])
        assertEquals("2026-11-20", estado["fecha"])
        assertFalse(estado.containsKey("motivo")) // la otra persona no ve el motivo
    }

    @Test
    fun aceptarSinConexionQuedaEnColaSinMotivo() = runTest {
        val (lote, datos, _) = prepararLote()
        every { lote.commit() } returns tareaPendiente()

        val confirmada = RespuestaRepository.guardar(
            "u1", Rol.GUIA, "Alfredo", "", "3008", "2026-11-20", EstadoServicio.ACEPTADO
        )

        assertFalse(confirmada)
        val respuesta = datos.captured as Map<*, *>
        assertEquals("aceptado", respuesta["estado"])
        assertFalse(respuesta.containsKey("motivo"))
    }
}
