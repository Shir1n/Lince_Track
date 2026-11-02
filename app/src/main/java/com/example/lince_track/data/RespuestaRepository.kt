package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Respuestas a los servicios (aceptar/rechazar), guardadas en Firestore porque la API
 * de Southbound es de solo lectura. Cada persona tiene una por servicio.
 */
object RespuestaRepository {

    private const val TAG = "RESPUESTAS"
    private const val COLECCION = "respuestas"
    private const val ESPERA_CONFIRMACION_MS = 5_000L

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Respuestas del usuario: id del servicio → respuesta. Incluye al instante las que
     * aún no llegan al servidor (por ejemplo, sin conexión).
     */
    fun observar(uid: String): Flow<Map<String, RespuestaServicio>> = callbackFlow {
        val registro = firestore.collection(COLECCION)
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LinceLogger.e(TAG, "No se pudieron leer las respuestas (${error.code})", error)
                    return@addSnapshotListener
                }
                val respuestas = snapshot?.documents.orEmpty().mapNotNull { documento ->
                    val servicioId = documento.getString("servicioId")
                    val estado = when (documento.getString("estado")) {
                        "aceptado" -> EstadoServicio.ACEPTADO
                        "rechazado" -> EstadoServicio.RECHAZADO
                        else -> null
                    }
                    if (servicioId != null && estado != null) RespuestaServicio(servicioId, estado) else null
                }
                trySend(respuestas.associateBy { it.servicioId })
            }
        awaitClose { registro.remove() }
    }

    /**
     * Guarda la respuesta. Devuelve true si el servidor la confirmó y false si sigue en cola:
     * sin conexión, Firestore la envía al recuperarla, pero la tarea no termina hasta entonces.
     */
    suspend fun guardar(
        uid: String,
        rol: Rol,
        servicioId: String,
        estado: EstadoServicio,
        motivo: MotivoRechazo? = null,
        observaciones: String = ""
    ): Boolean {
        val datos = buildMap<String, Any> {
            put("uid", uid)
            put("servicioId", servicioId)
            put("rol", rol.id)
            put("estado", if (estado == EstadoServicio.RECHAZADO) "rechazado" else "aceptado")
            put("respondidoEn", FieldValue.serverTimestamp())
            if (motivo != null) {
                put("motivo", motivo.codigo)
                put("observaciones", observaciones)
            }
        }
        val tarea = firestore.collection(COLECCION).document("${uid}_$servicioId").set(datos)
        return withTimeoutOrNull(ESPERA_CONFIRMACION_MS) {
            tarea.await()
            true
        } ?: false
    }
}
