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
    private const val COLECCION_ESTADOS = "estadosServicio"

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
     * Estado publicado de cada servicio desde la fecha [desde] (AAAA-MM-DD): id del servicio →
     * estado y nombre. Es lo que ve la otra persona del tour; no incluye motivo ni observaciones.
     */
    fun observarEstados(desde: String): Flow<Map<String, RespuestaServicio>> = callbackFlow {
        val registro = firestore.collection(COLECCION_ESTADOS)
            .whereGreaterThanOrEqualTo("fecha", desde)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LinceLogger.e(TAG, "No se pudieron leer los estados (${error.code})", error)
                    return@addSnapshotListener
                }
                val estados = snapshot?.documents.orEmpty().mapNotNull { documento ->
                    val estado = when (documento.getString("estado")) {
                        "aceptado" -> EstadoServicio.ACEPTADO
                        "rechazado" -> EstadoServicio.RECHAZADO
                        else -> null
                    }
                    estado?.let {
                        RespuestaServicio(servicioId = documento.id, estado = it, nombre = documento.getString("nombre"))
                    }
                }
                trySend(estados.associateBy { it.servicioId })
            }
        awaitClose { registro.remove() }
    }

    /**
     * Guarda la respuesta y publica el estado para la otra persona del tour, ambos o ninguno.
     * @return true si el servidor la confirmó; false si sigue en cola por falta de conexión.
     */
    suspend fun guardar(
        uid: String,
        rol: Rol,
        nombre: String,
        servicioId: String,
        fecha: String,
        estado: EstadoServicio,
        motivo: MotivoRechazo? = null,
        observaciones: String = ""
    ): Boolean {
        val codigoEstado = if (estado == EstadoServicio.RECHAZADO) "rechazado" else "aceptado"
        val datos = buildMap<String, Any> {
            put("uid", uid)
            put("servicioId", servicioId)
            put("rol", rol.id)
            put("estado", codigoEstado)
            put("respondidoEn", FieldValue.serverTimestamp())
            if (motivo != null) {
                put("motivo", motivo.codigo)
                put("observaciones", observaciones)
            }
        }
        val estadoCompartido = mapOf(
            "uid" to uid,
            "servicioId" to servicioId,
            "rol" to rol.id,
            "nombre" to nombre,
            "estado" to codigoEstado,
            "fecha" to fecha,
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        return firestore.batch()
            .set(firestore.collection(COLECCION).document("${uid}_$servicioId"), datos)
            .set(firestore.collection(COLECCION_ESTADOS).document(servicioId), estadoCompartido)
            .commit()
            .confirmada()
    }
}
