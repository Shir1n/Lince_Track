package com.example.lince_track.data

import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaFallida
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** TokenFcmRepository con Firestore y Firebase Messaging simulados. */
class TokenFcmRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val coleccion = mockk<CollectionReference>()
    private val documento = mockk<DocumentReference>()

    /** Firebase Messaging de mentira: entrega [token] o falla, y cuenta los borrados. */
    private class MensajeriaSimulada(var token: String? = "token-1", var fallaBorrar: Boolean = false) : Mensajeria {
        var borrados = 0
        override suspend fun token(): String = token ?: throw IllegalStateException("sin Play Services")
        override suspend fun borrarToken() {
            if (fallaBorrar) throw IllegalStateException("sin red")
            borrados++
        }
    }

    private val mensajeria = MensajeriaSimulada()

    @Before
    fun preparar() {
        every { firestore.collection("tokensFcm") } returns coleccion
        every { coleccion.document("u1") } returns documento
        TokenFcmRepository.firestoreReemplazable.reemplazar(firestore)
        TokenFcmRepository.mensajeriaReemplazable.reemplazar(mensajeria)
    }

    @After
    fun limpiar() {
        TokenFcmRepository.firestoreReemplazable.reemplazar(null)
        TokenFcmRepository.mensajeriaReemplazable.reemplazar(null)
    }

    @Test
    fun alIniciarSesionSeGuardaYAlCerrarlaSeBorra() {
        assertEquals(AccionToken.GUARDAR, TokenFcmRepository.accion(null, "u1"))
        assertEquals(AccionToken.GUARDAR, TokenFcmRepository.accion("u1", "u2"))
        assertEquals(AccionToken.BORRAR, TokenFcmRepository.accion("u1", null))
        assertEquals(AccionToken.NADA, TokenFcmRepository.accion("u1", "u1"))
        assertEquals(AccionToken.NADA, TokenFcmRepository.accion(null, null))
    }

    @Test
    fun registrarGuardaElTokenDelTelefono() = runBlocking {
        val datos = slot<Map<String, Any>>()
        every { documento.set(capture(datos)) } returns tareaExitosa(null)

        assertTrue(TokenFcmRepository.registrar("u1").isSuccess)

        assertEquals("token-1", datos.captured["token"])
        assertEquals(setOf("token", "actualizadoEn"), datos.captured.keys)
    }

    @Test
    fun sinTokenOSinPermisoElRegistroFallaSinRomperLaApp() = runBlocking {
        mensajeria.token = null
        assertTrue(TokenFcmRepository.registrar("u1").isFailure)
        verify(exactly = 0) { documento.set(any()) }

        mensajeria.token = "token-2"
        every { documento.set(any()) } returns tareaFallida(FirebaseSimulado.errorFirestore())
        assertTrue(TokenFcmRepository.registrar("u1").isFailure)
    }

    @Test
    fun alCerrarSesionSeAnulaElToken() = runBlocking {
        assertTrue(TokenFcmRepository.olvidarTelefono().isSuccess)
        assertEquals(1, mensajeria.borrados)

        mensajeria.fallaBorrar = true
        assertTrue(TokenFcmRepository.olvidarTelefono().isFailure)
    }
}
