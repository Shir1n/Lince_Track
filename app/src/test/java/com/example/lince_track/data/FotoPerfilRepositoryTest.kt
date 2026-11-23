package com.example.lince_track.data

import android.net.Uri
import com.example.lince_track.data.FirebaseSimulado.tareaExitosa
import com.example.lince_track.data.FirebaseSimulado.tareaFallida
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Foto de perfil con Firebase Storage simulado. */
class FotoPerfilRepositoryTest {

    private val storage = mockk<FirebaseStorage>()
    private val raiz = mockk<StorageReference>()
    private val foto = mockk<StorageReference>()

    @Before
    fun preparar() {
        every { storage.reference } returns raiz
        every { raiz.child("perfil/u1/foto.jpg") } returns foto
        FotoPerfilRepository.storageReemplazable.reemplazar(storage)
    }

    @After
    fun limpiar() {
        FotoPerfilRepository.storageReemplazable.reemplazar(null)
    }

    private fun enlace(texto: String): Uri = mockk<Uri>().also { every { it.toString() } returns texto }

    private fun errorStorage(codigo: Int): StorageException =
        mockk<StorageException>(relaxed = true).also { every { it.errorCode } returns codigo }

    /** UploadTask que termina bien (await usa los mismos listeners que cualquier Task). */
    private fun subidaExitosa(): UploadTask {
        val tarea = mockk<UploadTask>()
        val resultado = mockk<UploadTask.TaskSnapshot>()
        every { tarea.addOnSuccessListener(any<OnSuccessListener<in UploadTask.TaskSnapshot>>()) } answers {
            firstArg<OnSuccessListener<in UploadTask.TaskSnapshot>>().onSuccess(resultado); tarea
        }
        every { tarea.addOnFailureListener(any<OnFailureListener>()) } returns tarea
        every { tarea.addOnCanceledListener(any<OnCanceledListener>()) } returns tarea
        return tarea
    }

    @Test
    fun laFotoSeAchicaSinDeformarseYNuncaSeAgranda() {
        assertEquals(512 to 384, FotoPerfilRepository.medidas(4000, 3000))
        assertEquals(288 to 512, FotoPerfilRepository.medidas(1080, 1920))
        assertEquals(300 to 200, FotoPerfilRepository.medidas(300, 200))
        assertEquals(512 to 1, FotoPerfilRepository.medidas(10_000, 5))
        assertEquals(0 to 0, FotoPerfilRepository.medidas(0, 0))
        assertEquals("perfil/abc/foto.jpg", FotoPerfilRepository.ruta("abc"))
    }

    @Test
    fun leeElEnlaceDeLaFotoONullSiNoHay() = runBlocking {
        every { foto.downloadUrl } returns tareaExitosa(enlace("https://storage/u1.jpg"))
        assertEquals("https://storage/u1.jpg", FotoPerfilRepository.url("u1"))

        every { foto.downloadUrl } returns tareaFallida(errorStorage(StorageException.ERROR_OBJECT_NOT_FOUND))
        assertNull(FotoPerfilRepository.url("u1"))

        every { foto.downloadUrl } returns tareaFallida(errorStorage(StorageException.ERROR_NOT_AUTHORIZED))
        assertNull(FotoPerfilRepository.url("u1"))

        every { foto.downloadUrl } returns tareaFallida(IOException("sin red"))
        assertNull(FotoPerfilRepository.url("u1"))
    }

    @Test
    fun subirGuardaElJpegYDevuelveElEnlaceNuevo() = runBlocking {
        val bytes = ByteArray(2048)
        every { foto.putBytes(bytes, any<StorageMetadata>()) } returns subidaExitosa()
        every { foto.downloadUrl } returns tareaExitosa(enlace("https://storage/u1.jpg?v=2"))

        assertEquals("https://storage/u1.jpg?v=2", FotoPerfilRepository.subir("u1", bytes).getOrThrow())
        verify { foto.putBytes(bytes, any<StorageMetadata>()) }
    }

    @Test
    fun siLaSubidaFallaSeInforma() = runBlocking {
        every { foto.putBytes(any(), any<StorageMetadata>()) } throws IllegalStateException("sin permiso")
        assertTrue(FotoPerfilRepository.subir("u1", ByteArray(1)).isFailure)
    }

    @Test
    fun borrarQuitaLaFoto() = runBlocking {
        every { foto.delete() } returns tareaExitosa(null)
        assertTrue(FotoPerfilRepository.borrar("u1").isSuccess)

        every { foto.delete() } returns tareaFallida(IOException("sin red"))
        assertTrue(FotoPerfilRepository.borrar("u1").isFailure)
    }
}
