package com.example.lince_track.data

import com.example.lince_track.data.FirebaseSimulado.documento
import com.example.lince_track.data.FirebaseSimulado.errorFirestore
import com.example.lince_track.data.FirebaseSimulado.instantanea
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaFallida
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.justRun
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** AsignacionRepository y AuthRepository con Firestore y FirebaseAuth simulados. */
@OptIn(ExperimentalCoroutinesApi::class)
class AsignacionYAuthRepositoryTest {

    private val firestore = mockk<FirebaseFirestore>()
    private val auth = mockk<FirebaseAuth>()
    private val asignaciones = mockk<CollectionReference>()
    private val usuarios = mockk<CollectionReference>()
    private val estados = mockk<CollectionReference>()

    @Before
    fun preparar() {
        every { firestore.collection("asignaciones") } returns asignaciones
        every { firestore.collection("usuarios") } returns usuarios
        every { firestore.collection("estadosServicio") } returns estados
        justRun { auth.signOut() }
        every { auth.currentUser } returns null
        AsignacionRepository.firestoreReemplazable.reemplazar(firestore)
        AuthRepository.firestoreReemplazable.reemplazar(firestore)
        AuthRepository.authReemplazable.reemplazar(auth)
    }

    @After
    fun limpiar() {
        AuthRepository.cerrarSesion()
        AsignacionRepository.firestoreReemplazable.reemplazar(null)
        AuthRepository.firestoreReemplazable.reemplazar(null)
        AuthRepository.authReemplazable.reemplazar(FirebaseSimulado.authSinSesion())
    }

    // ---------- Asignaciones ----------

    @Test
    fun observarAsignacionesIgnoraLasQueNoTienenUid() = runTest {
        val desde = mockk<Query>()
        every { asignaciones.whereGreaterThanOrEqualTo("fecha", "2026-11-13") } returns desde
        val escucha = FirebaseSimulado.Escucha(desde)
        val recibidas = mutableListOf<Map<String, AsignacionServicio>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            AsignacionRepository.observar("2026-11-13").toList(recibidas)
        }

        escucha.fallar()
        escucha.enviar(
            instantanea(
                documento("3008", mapOf("uid" to "patricio", "nombre" to "Patricio Lagos")),
                documento("3009", mapOf("uid" to "otro")),
                documento("3010", mapOf("nombre" to "Sin uid"))
            )
        )
        job.cancel()

        val mapa = recibidas.single()
        assertEquals(AsignacionServicio("3008", "patricio", "Patricio Lagos"), mapa["3008"])
        assertEquals("", mapa.getValue("3009").nombre)
        assertEquals(setOf("3008", "3009"), mapa.keys)
    }

    @Test
    fun personalSoloIncluyeConductoresYGuias() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { usuarios.get() } returns tareaExitosa(snapshot)
        every { snapshot.documents } returns listOf(
            documento("c1", mapOf("rol" to "conductor", "nombre" to "Carlos", "transportistaId" to 37L, "telefono" to "+569")),
            documento("g1", mapOf("rol" to "guia", "nombre" to "Alfredo", "guiaId" to 35L)),
            documento("a1", mapOf("rol" to "administrador", "nombre" to "Admin")),
            documento("x1", mapOf("rol" to "pasajero")),
            documento("c2", mapOf("rol" to "conductor", "nombre" to "Sin id"))
        )

        val personal = AsignacionRepository.personal()

        assertEquals(listOf("c1", "g1", "c2"), personal.map { it.uid })
        assertEquals(37L, personal[0].idApi)
        assertEquals("+569", personal[0].telefono)
        assertEquals(Rol.GUIA, personal[1].rol)
        assertNull(personal[2].idApi)
    }

    @Test
    fun reasignarGuardaElReemplazoYBorraElEstadoAnterior() = runTest {
        val lote = mockk<WriteBatch>()
        val docAsignacion = mockk<DocumentReference>()
        val docEstado = mockk<DocumentReference>()
        val datos = slot<Any>()
        every { firestore.batch() } returns lote
        every { asignaciones.document("3008") } returns docAsignacion
        every { estados.document("3008") } returns docEstado
        every { lote.set(docAsignacion, capture(datos)) } returns lote
        every { lote.delete(docEstado) } returns lote
        every { lote.commit() } returns tareaExitosa()
        val persona = PersonaDisponible("patricio", "Patricio Lagos", Rol.CONDUCTOR, 43L)

        assertTrue(AsignacionRepository.reasignar(Datos.servicio(3008), persona, "admin"))

        val guardado = datos.captured as Map<*, *>
        assertEquals("patricio", guardado["uid"])
        assertEquals("conductor", guardado["rol"])
        assertEquals("admin", guardado["asignadoPor"])
        verify { lote.delete(docEstado) }
    }

    // ---------- Autenticación ----------

    private fun usuarioEnFirestore(uid: String, campos: Map<String, Any?>, existe: Boolean = true) {
        val ref = mockk<DocumentReference>()
        every { usuarios.document(uid) } returns ref
        every { ref.get() } returns tareaExitosa(documento(uid, campos, existe))
    }

    private fun loginExitoso(uid: String) {
        val resultado = mockk<AuthResult>()
        val usuario = mockk<FirebaseUser>()
        every { usuario.uid } returns uid
        every { resultado.user } returns usuario
        every { auth.signInWithEmailAndPassword("carlos@lince.cl", "clave") } returns tareaExitosa(resultado)
    }

    @Test
    fun iniciarSesionCargaElPerfilCompleto() = runTest {
        loginExitoso("carlos")
        usuarioEnFirestore("carlos", mapOf("rol" to "conductor", "nombre" to "Carlos", "telefono" to "+569",
            "transportistaId" to 37L, "perfilNuevo" to true))

        val rol = AuthRepository.iniciarSesion("  carlos@lince.cl ", "clave")

        assertEquals(Rol.CONDUCTOR, rol.getOrNull())
        val perfil = AuthRepository.perfilActual.value!!
        assertEquals(37L, perfil.transportistaId)
        assertEquals("Carlos", perfil.nombre)
        assertTrue(perfil.perfilNuevo)
        assertEquals(Rol.CONDUCTOR, AuthRepository.rolActual.value)
    }

    @Test
    fun unaCuentaSinDocumentoOSinRolValidoNoEntraYCierraSesion() = runTest {
        loginExitoso("x")
        usuarioEnFirestore("x", emptyMap(), existe = false)
        val sinDocumento = AuthRepository.iniciarSesion("carlos@lince.cl", "clave")
        assertTrue(sinDocumento.exceptionOrNull() is SinRolException)

        usuarioEnFirestore("x", mapOf("rol" to "pasajero"))
        val rolInvalido = AuthRepository.iniciarSesion("carlos@lince.cl", "clave")
        assertEquals("Tu cuenta no está configurada. Contacta al administrador.", rolInvalido.exceptionOrNull()?.message)

        verify(exactly = 2) { auth.signOut() }
        assertNull(AuthRepository.perfilActual.value)
    }

    @Test
    fun unIdQueNoEsNumeroQuedaSinVincular() = runTest {
        loginExitoso("g")
        usuarioEnFirestore("g", mapOf("rol" to "guia", "guiaId" to "35"))
        AuthRepository.iniciarSesion("carlos@lince.cl", "clave")
        assertNull(AuthRepository.perfilActual.value!!.guiaId)
    }

    @Test
    fun credencialesMalasDevuelvenElErrorDeFirebase() = runTest {
        val error = IllegalArgumentException("clave mala")
        every { auth.signInWithEmailAndPassword(any(), any()) } returns tareaFallida<AuthResult>(error)
        // Las corrutinas pueden relanzar una copia de la excepción: se compara el tipo y el mensaje.
        val recibido = AuthRepository.iniciarSesion("carlos@lince.cl", "x").exceptionOrNull()
        assertTrue(recibido is IllegalArgumentException)
        assertEquals("clave mala", recibido?.message)
        verify { auth.signOut() }
    }

    @Test
    fun sinUsuarioEnElResultadoEsErrorDeCuenta() = runTest {
        val resultado = mockk<AuthResult>()
        every { resultado.user } returns null
        every { auth.signInWithEmailAndPassword(any(), any()) } returns tareaExitosa(resultado)
        assertTrue(AuthRepository.iniciarSesion("a@b.cl", "x").exceptionOrNull() is SinRolException)
    }

    private fun errorAlLeer(codigo: Code): String? {
        val ref = mockk<DocumentReference>()
        every { usuarios.document("c") } returns ref
        every { ref.get() } returns tareaFallida<DocumentSnapshot>(errorFirestore(codigo))
        return kotlinx.coroutines.runBlocking { AuthRepository.cargarRolActual("c") }
            .let { AuthRepository.errorPerfil.value }
    }

    @Test
    fun cadaErrorDeFirestoreTieneSuMensaje() {
        assertEquals("Tu cuenta no tiene permiso para ingresar. Contacta al administrador.", errorAlLeer(Code.PERMISSION_DENIED))
        assertEquals("Sin conexión con la base de datos. Revisa tu internet.", errorAlLeer(Code.UNAVAILABLE))
        assertEquals("No se pudo leer tu cuenta. Intenta nuevamente.", errorAlLeer(Code.INTERNAL))
    }

    @Test
    fun cargarRolActualRecuperaLaSesionAbierta() = runTest {
        usuarioEnFirestore("admin", mapOf("rol" to "administrador", "nombre" to "Admin 1"))
        assertEquals(Rol.ADMINISTRADOR, AuthRepository.cargarRolActual("admin"))
        assertNull(AuthRepository.errorPerfil.value)
    }

    @Test
    fun unErrorInesperadoAlRecuperarLaSesionDaMensajeGenerico() = runTest {
        val ref = mockk<DocumentReference>()
        every { usuarios.document("c") } returns ref
        every { ref.get() } returns tareaFallida<DocumentSnapshot>(IllegalStateException("raro"))
        assertNull(AuthRepository.cargarRolActual("c"))
        assertEquals("No se pudo leer tu cuenta. Intenta nuevamente.", AuthRepository.errorPerfil.value)
    }

    @Test
    fun usuarioActualEsElDeFirebaseYCerrarSesionLimpiaTodo() = runTest {
        val usuario = mockk<FirebaseUser>()
        every { auth.currentUser } returns usuario
        assertEquals(usuario, AuthRepository.usuarioActual)

        usuarioEnFirestore("admin", mapOf("rol" to "administrador"))
        AuthRepository.cargarRolActual("admin")
        AuthRepository.cerrarSesion()

        assertNull(AuthRepository.perfilActual.value)
        assertNull(AuthRepository.rolActual.value)
        verify { auth.signOut() }
    }
}
