package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Reemplazos elegidos por el administrador. Los servicios viven en la API, de solo lectura. */
object AsignacionRepository {

    private const val TAG = "ASIGNACIONES"
    private const val COLECCION = "asignaciones"
    private const val COLECCION_ESTADOS = "estadosServicio"
    private const val COLECCION_USUARIOS = "usuarios"

    private val firestore = FirebaseFirestore.getInstance()

    /** Reemplazos de los servicios desde la fecha [desde] (AAAA-MM-DD): id del servicio → reemplazo. */
    fun observar(desde: String): Flow<Map<String, AsignacionServicio>> = callbackFlow {
        val registro = firestore.collection(COLECCION)
            .whereGreaterThanOrEqualTo("fecha", desde)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LinceLogger.e(TAG, "No se pudieron leer las asignaciones (${error.code})", error)
                    return@addSnapshotListener
                }
                val asignaciones = snapshot?.documents.orEmpty().mapNotNull { documento ->
                    val uid = documento.getString("uid")
                    uid?.let { AsignacionServicio(documento.id, it, documento.getString("nombre").orEmpty()) }
                }
                trySend(asignaciones.associateBy { it.servicioId })
            }
        awaitClose { registro.remove() }
    }

    /** Cuentas de conductores y guías. Solo el administrador tiene permiso para listarlas. */
    suspend fun personal(): List<PersonaDisponible> =
        firestore.collection(COLECCION_USUARIOS).get().await().documents.mapNotNull { aPersona(it) }

    private fun aPersona(documento: DocumentSnapshot): PersonaDisponible? {
        val rol = Rol.desde(documento.getString("rol")) ?: return null
        val campoId = when (rol) {
            Rol.CONDUCTOR -> "transportistaId"
            Rol.GUIA -> "guiaId"
            Rol.ADMINISTRADOR -> return null
        }
        return PersonaDisponible(
            uid = documento.id,
            nombre = documento.getString("nombre").orEmpty(),
            rol = rol,
            idApi = (documento.get(campoId) as? Number)?.toLong()
        )
    }

    /**
     * Guarda el reemplazo y borra el estado publicado, que era de la persona anterior.
     * @return true si el servidor lo confirmó; false si sigue en cola por falta de conexión.
     */
    suspend fun reasignar(servicio: Servicio, persona: PersonaDisponible, adminUid: String): Boolean {
        val datos = mapOf(
            "servicioId" to servicio.id,
            "uid" to persona.uid,
            "nombre" to persona.nombre,
            "rol" to persona.rol.id,
            "fecha" to servicio.fecha.toString(),
            "asignadoPor" to adminUid,
            "asignadoEn" to FieldValue.serverTimestamp()
        )
        return firestore.batch()
            .set(firestore.collection(COLECCION).document(servicio.id), datos)
            .delete(firestore.collection(COLECCION_ESTADOS).document(servicio.id))
            .commit()
            .confirmada()
    }
}
