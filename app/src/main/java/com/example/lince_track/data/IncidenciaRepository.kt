package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Incidencias de los servicios. Cada persona lee las suyas y el administrador todas. */
object IncidenciaRepository {

    private const val TAG = "INCIDENCIAS"
    private const val COLECCION = "incidencias"

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Con [uid] trae las de esa persona; con null, las de todos desde la fecha [desde] (AAAA-MM-DD),
     * lo que solo puede hacer el administrador.
     */
    fun observar(uid: String?, desde: String): Flow<List<IncidenciaServicio>> = callbackFlow {
        val coleccion = firestore.collection(COLECCION)
        val consulta = if (uid != null) {
            coleccion.whereEqualTo("uid", uid)
        } else {
            coleccion.whereGreaterThanOrEqualTo("fecha", desde)
        }
        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                LinceLogger.e(TAG, "No se pudieron leer las incidencias (${error.code})", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.aIncidencia() })
        }
        awaitClose { registro.remove() }
    }

    /** @return true si el servidor la confirmó; false si sigue en cola por falta de conexión. */
    suspend fun reportar(
        uid: String,
        rol: Rol,
        nombre: String,
        servicio: Servicio,
        tipo: TipoIncidencia,
        detalle: String,
        ubicacion: Ubicacion?
    ): Boolean = firestore.collection(COLECCION).document().set(
        buildMap<String, Any> {
            put("uid", uid)
            put("rol", rol.id)
            put("nombre", nombre)
            put("servicioId", servicio.id)
            put("titulo", servicio.titulo)
            put("tipo", tipo.codigo)
            put("detalle", detalle)
            put("fecha", servicio.fecha.toString())
            put("creadaEn", FieldValue.serverTimestamp())
            put("resuelta", false)
            if (ubicacion != null) {
                put("lat", ubicacion.lat)
                put("lon", ubicacion.lon)
            }
        }
    ).confirmada()

    /** @return true si el servidor lo confirmó; false si sigue en cola por falta de conexión. */
    suspend fun responder(id: String, adminUid: String, respuesta: String): Boolean =
        firestore.collection(COLECCION).document(id).update(
            mapOf(
                "enGestion" to true,
                "respuesta" to respuesta,
                "atendidaEn" to FieldValue.serverTimestamp(),
                "atendidaPor" to adminUid
            )
        ).confirmada()

    /**
     * Con [respuesta] vacía se mantiene la anterior.
     * @return true si el servidor lo confirmó; false si sigue en cola por falta de conexión.
     */
    suspend fun resolver(id: String, adminUid: String, respuesta: String): Boolean =
        firestore.collection(COLECCION).document(id).update(
            buildMap<String, Any> {
                put("resuelta", true)
                put("resueltaEn", FieldValue.serverTimestamp())
                put("resueltaPor", adminUid)
                if (respuesta.isNotBlank()) put("respuesta", respuesta)
            }
        ).confirmada()

    private fun DocumentSnapshot.aIncidencia(): IncidenciaServicio? {
        val servicioId = getString("servicioId") ?: return null
        val uid = getString("uid") ?: return null
        val tipo = TipoIncidencia.desde(getString("tipo")) ?: return null
        val creadaEn = getTimestamp("creadaEn", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: return null
        return IncidenciaServicio(
            id = id,
            servicioId = servicioId,
            titulo = getString("titulo").orEmpty(),
            uid = uid,
            nombre = getString("nombre").orEmpty(),
            tipo = tipo,
            detalle = getString("detalle").orEmpty(),
            creadaEn = creadaEn,
            resuelta = getBoolean("resuelta") == true,
            enGestion = getBoolean("enGestion") == true,
            respuesta = getString("respuesta").orEmpty(),
            ubicacion = Ubicacion.desde(getDouble("lat"), getDouble("lon"))
        )
    }
}
