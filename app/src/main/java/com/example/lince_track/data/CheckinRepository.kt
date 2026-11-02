package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Check-in y check-out de cada persona por servicio, guardados en Firestore. */
object CheckinRepository {

    private const val TAG = "CHECKIN"
    private const val COLECCION = "checkins"

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Check-ins por servicio: id del servicio → check-in. Con [uid] trae solo los de esa
     * persona; con null, todos (solo el administrador tiene permiso).
     */
    fun observar(uid: String?): Flow<Map<String, CheckinServicio>> = callbackFlow {
        val coleccion = firestore.collection(COLECCION)
        val consulta = if (uid != null) coleccion.whereEqualTo("uid", uid) else coleccion
        val registro = consulta.addSnapshotListener { snapshot, error ->
            if (error != null) {
                LinceLogger.e(TAG, "No se pudieron leer los check-ins (${error.code})", error)
                return@addSnapshotListener
            }
            val checkins = snapshot?.documents.orEmpty().mapNotNull { it.aCheckin() }
            trySend(checkins.sortedBy { it.checkInEn }.associateBy { it.servicioId })
        }
        awaitClose { registro.remove() }
    }

    /** @return true si el servidor lo confirmó; false si sigue en cola por falta de conexión. */
    suspend fun registrarCheckIn(
        uid: String,
        rol: Rol,
        servicioId: String,
        lat: Double,
        lon: Double,
        distanciaM: Int,
        gpsReal: Boolean
    ): Boolean = documento(uid, servicioId).set(
        mapOf(
            "uid" to uid,
            "servicioId" to servicioId,
            "rol" to rol.id,
            "checkInEn" to FieldValue.serverTimestamp(),
            "lat" to lat,
            "lon" to lon,
            "distanciaM" to distanciaM,
            "gpsReal" to gpsReal
        )
    ).confirmada()

    /** @return true si el servidor lo confirmó; false si sigue en cola por falta de conexión. */
    suspend fun registrarCheckOut(uid: String, servicioId: String, lat: Double, lon: Double): Boolean =
        documento(uid, servicioId).update(
            mapOf(
                "checkOutEn" to FieldValue.serverTimestamp(),
                "latFin" to lat,
                "lonFin" to lon
            )
        ).confirmada()

    private fun documento(uid: String, servicioId: String) =
        firestore.collection(COLECCION).document("${uid}_$servicioId")

    private fun DocumentSnapshot.aCheckin(): CheckinServicio? {
        // Mientras la hora del servidor no llega, se usa una estimada para mostrar el estado al instante.
        val estimada = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val servicioId = getString("servicioId") ?: return null
        val checkInEn = getTimestamp("checkInEn", estimada)?.toDate()?.time ?: return null
        return CheckinServicio(
            servicioId = servicioId,
            checkInEn = checkInEn,
            distanciaM = getLong("distanciaM")?.toInt() ?: 0,
            gpsReal = getBoolean("gpsReal") ?: false,
            checkOutEn = getTimestamp("checkOutEn", estimada)?.toDate()?.time
        )
    }
}
