package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Respuestas a los servicios (aceptar/rechazar), guardadas en Firestore porque la API
 * de Southbound es de solo lectura. Cada persona tiene una por servicio.
 */
object RespuestaRepository {

    private const val TAG = "RESPUESTAS"
    private const val COLECCION = "respuestas"

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Respuestas por servicio: id del servicio → respuesta. Con [uid] trae solo las de esa
     * persona; con null, todas (solo el administrador tiene permiso). Incluye al instante las
     * que aún no llegan al servidor (por ejemplo, sin conexión).
     */
    fun observar(uid: String?): Flow<Map<String, RespuestaServicio>> = callbackFlow {
        val coleccion = firestore.collection(COLECCION)
        val consulta = if (uid != null) coleccion.whereEqualTo("uid", uid) else coleccion
        val registro = consulta
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
                    if (servicioId != null && estado != null) {
                        RespuestaServicio(
                            servicioId = servicioId,
                            estado = estado,
                            motivo = MotivoRechazo.desde(documento.getString("motivo")),
                            observaciones = documento.getString("observaciones").orEmpty(),
                            respondidoEn = documento.getTimestamp("respondidoEn")?.toDate()?.time
                        )
                    } else {
                        null
                    }
                }
                // Si un servicio tuviera más de una respuesta, queda la más reciente.
                trySend(respuestas.sortedBy { it.respondidoEn ?: Long.MAX_VALUE }.associateBy { it.servicioId })
            }
        awaitClose { registro.remove() }
    }

    /**
     * Guarda la respuesta.
     * @return true si el servidor la confirmó; false si sigue en cola por falta de conexión.
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
        return firestore.collection(COLECCION).document("${uid}_$servicioId").set(datos).confirmada()
    }
}
