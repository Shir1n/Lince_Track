package com.example.lince_track.data

import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.Date

/**
 * Piezas de Firebase simuladas con MockK, para probar los repositorios en la JVM sin conexión ni emulador.
 * Firebase entrega los resultados con "listeners"; aquí se llaman al tiro.
 */
object FirebaseSimulado {

    /** Tarea que ya terminó bien: await() devuelve [valor] y confirmada() devuelve true. */
    @Suppress("UNCHECKED_CAST")
    fun <T> tareaExitosa(valor: T? = null): Task<T> {
        val tarea = mockk<Task<T>>()
        every { tarea.addOnSuccessListener(any<OnSuccessListener<in T>>()) } answers {
            firstArg<OnSuccessListener<in T>>().onSuccess(valor as T); tarea
        }
        every { tarea.addOnFailureListener(any<OnFailureListener>()) } returns tarea
        every { tarea.addOnCanceledListener(any<OnCanceledListener>()) } returns tarea
        return tarea
    }

    /** Tarea que falló con [error]. */
    fun <T> tareaFallida(error: Exception): Task<T> {
        val tarea = mockk<Task<T>>()
        every { tarea.addOnSuccessListener(any<OnSuccessListener<in T>>()) } returns tarea
        every { tarea.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error); tarea
        }
        every { tarea.addOnCanceledListener(any<OnCanceledListener>()) } returns tarea
        return tarea
    }

    /** Tarea que nunca termina: lo que pasa sin conexión (la escritura queda en cola). */
    fun <T> tareaPendiente(): Task<T> {
        val tarea = mockk<Task<T>>()
        every { tarea.addOnSuccessListener(any<OnSuccessListener<in T>>()) } returns tarea
        every { tarea.addOnFailureListener(any<OnFailureListener>()) } returns tarea
        every { tarea.addOnCanceledListener(any<OnCanceledListener>()) } returns tarea
        return tarea
    }

    /** Documento con esos [campos]. Las fechas se pasan como [Date] y se leen como Timestamp. */
    fun documento(id: String, campos: Map<String, Any?>, existe: Boolean = true): DocumentSnapshot {
        val doc = mockk<DocumentSnapshot>()
        every { doc.id } returns id
        every { doc.exists() } returns existe
        every { doc.get(any<String>()) } answers { campos[firstArg()] }
        every { doc.getString(any()) } answers { campos[firstArg()] as? String }
        every { doc.getBoolean(any()) } answers { campos[firstArg()] as? Boolean }
        every { doc.getDouble(any()) } answers { (campos[firstArg()] as? Number)?.toDouble() }
        every { doc.getLong(any()) } answers { (campos[firstArg()] as? Number)?.toLong() }
        every { doc.getTimestamp(any()) } answers { (campos[firstArg()] as? Date)?.let { Timestamp(it) } }
        every { doc.getTimestamp(any(), any()) } answers { (campos[firstArg()] as? Date)?.let { Timestamp(it) } }
        return doc
    }

    /**
     * FirebaseAuth sin nadie conectado, para dejar en AuthRepository al terminar un test. No se vuelve al real porque
     * en la JVM no existe: ServicioRepository sigue vivo entre tests y, si lo consultara, su escucha se detendría.
     */
    fun authSinSesion(): FirebaseAuth {
        val auth = mockk<FirebaseAuth>()
        every { auth.currentUser } returns null
        every { auth.signOut() } returns Unit
        return auth
    }

    fun instantanea(vararg documentos: DocumentSnapshot): QuerySnapshot {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns documentos.toList()
        return snapshot
    }

    fun errorFirestore(codigo: FirebaseFirestoreException.Code = FirebaseFirestoreException.Code.PERMISSION_DENIED) =
        FirebaseFirestoreException("simulado", codigo)

    /**
     * Escucha de una consulta: guarda el listener que registra el repositorio para que el test le mande
     * instantáneas o errores, y la [registro] para comprobar que se quita al dejar de escuchar.
     */
    class Escucha(consulta: Query) {
        private val listener = slot<EventListener<QuerySnapshot>>()
        val registro: ListenerRegistration = mockk(relaxed = true)

        init {
            every { consulta.addSnapshotListener(capture(listener)) } returns registro
        }

        val registrada: Boolean get() = listener.isCaptured

        fun enviar(snapshot: QuerySnapshot) = listener.captured.onEvent(snapshot, null)

        fun fallar(error: FirebaseFirestoreException = errorFirestore()) = listener.captured.onEvent(null, error)
    }
}
