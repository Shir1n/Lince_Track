package com.example.lince_track.data.privacidad

import com.example.lince_track.data.PerfilUsuario
import com.example.lince_track.data.Reemplazable
import com.example.lince_track.data.await
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.util.LinceLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/** Estados de una solicitud; "completada" la escribe la Cloud Function tras borrar los datos. */
enum class EstadoSolicitud(val id: String) {
    PENDIENTE("pendiente"),
    APROBADA("aprobada"),
    RECHAZADA("rechazada"),
    COMPLETADA("completada");

    companion object {
        fun desde(valor: String?): EstadoSolicitud? = entries.firstOrNull { it.id == valor }
    }
}

/** Solicitud de eliminación de datos (`solicitudesPrivacidad/{uid}`). */
data class SolicitudPrivacidad(
    val uid: String,
    val nombre: String,
    val rol: String,
    val motivo: String,
    val estado: EstadoSolicitud,
    /** Milisegundos; null mientras llega la hora del servidor. */
    val creadaEn: Long?,
    val respuesta: String = ""
)

/**
 * Derechos de la Ley 21.719 en la app: pedir la eliminación de los datos (y cancelarla mientras esté pendiente),
 * descargar lo propio y, para Coordinación, aprobar o rechazar. Al aprobar se borra primero lo del backend
 * (hitos y bloqueos); lo de Firestore, Storage y Auth lo borra la Cloud Function `atenderSolicitudPrivacidad`.
 */
object SolicitudPrivacidadRepository {

    private const val TAG = "PRIVACIDAD"
    internal const val COLECCION = "solicitudesPrivacidad"
    const val MAX_MOTIVO = 500

    /** Firestore real; los tests lo reemplazan por uno simulado con MockK. */
    internal val firestoreReemplazable = Reemplazable { FirebaseFirestore.getInstance() }
    private val firestore get() = firestoreReemplazable.valor

    /** Backend propio; los tests lo reemplazan. */
    internal val apiReemplazable = Reemplazable<BloqueoApi> { BloqueoApiClient.api }
    private val api get() = apiReemplazable.valor

    private val json = Json { prettyPrint = true }

    /** La solicitud de [uid] (null si no hay). */
    fun observar(uid: String): Flow<SolicitudPrivacidad?> = callbackFlow {
        val registro = firestore.collection(COLECCION).document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                LinceLogger.e(TAG, "No se pudo leer la solicitud (${error.code})", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.aSolicitud())
        }
        awaitClose { registro.remove() }
    }

    /** Solicitudes pendientes, para Coordinación (la más antigua primero). */
    fun observarPendientes(): Flow<List<SolicitudPrivacidad>> = callbackFlow {
        val registro = firestore.collection(COLECCION)
            .whereEqualTo("estado", EstadoSolicitud.PENDIENTE.id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LinceLogger.e(TAG, "No se pudieron leer las solicitudes (${error.code})", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.aSolicitud() }.sortedBy { it.creadaEn ?: Long.MAX_VALUE })
            }
        awaitClose { registro.remove() }
    }

    suspend fun solicitar(uid: String, perfil: PerfilUsuario, motivo: String): Result<Unit> = intentar {
        firestore.collection(COLECCION).document(uid).set(
            mapOf(
                "uid" to uid,
                "nombre" to perfil.nombre.orEmpty(),
                "rol" to perfil.rol.id,
                "motivo" to motivo.trim().take(MAX_MOTIVO),
                "estado" to EstadoSolicitud.PENDIENTE.id,
                "creadaEn" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Solo mientras siga pendiente (lo revisan las reglas). */
    suspend fun cancelar(uid: String): Result<Unit> = intentar {
        firestore.collection(COLECCION).document(uid).delete().await()
    }

    /**
     * Coordinación aprueba: primero borra lo del backend (si falla, no se marca aprobada y se puede reintentar);
     * después la Cloud Function borra el resto y la cuenta.
     */
    suspend fun aprobar(uid: String, adminUid: String): Result<Unit> = intentar {
        try {
            api.borrarDatos(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.aBloqueoApiException()
        }
        responder(uid, adminUid, EstadoSolicitud.APROBADA, "")
    }

    suspend fun rechazar(uid: String, adminUid: String, respuesta: String): Result<Unit> = intentar {
        require(respuesta.isNotBlank()) { "Escribe el motivo del rechazo" }
        responder(uid, adminUid, EstadoSolicitud.RECHAZADA, respuesta.trim().take(MAX_MOTIVO))
    }

    private suspend fun responder(uid: String, adminUid: String, estado: EstadoSolicitud, respuesta: String) {
        firestore.collection(COLECCION).document(uid).update(
            mapOf(
                "estado" to estado.id,
                "respuesta" to respuesta,
                "atendidaEn" to FieldValue.serverTimestamp(),
                "atendidaPor" to adminUid
            )
        ).await()
    }

    /**
     * Todo lo que Lince Track guarda de la persona, en JSON (derecho de acceso y portabilidad): su cuenta,
     * respuestas, check-ins, incidencias y solicitud en Firestore, y sus hitos y bloqueos en el backend.
     */
    suspend fun exportar(uid: String): Result<String> = intentar {
        val propios = buildJsonObject {
            put("cuenta", documento("usuarios", uid))
            put("solicitudPrivacidad", documento(COLECCION, uid))
            for (coleccion in listOf("respuestas", "checkins", "incidencias")) {
                val consulta = firestore.collection(coleccion).whereEqualTo("uid", uid).get().await()
                put(coleccion, JsonArray(consulta.documents.map { aJson(it.data) }))
            }
        }
        val backend = try {
            api.misDatos()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.aBloqueoApiException()
        }
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("app", "Lince Track")
                put("descripcion", "Datos personales guardados por Lince Track (Ley 21.719: acceso y portabilidad)")
                put("generadoEn", Instant.now().toString())
                put("uid", uid)
                put("firestore", propios)
                put("backend", backend)
            }
        )
    }

    private suspend fun documento(coleccion: String, id: String): JsonElement {
        val snapshot = firestore.collection(coleccion).document(id).get().await()
        return if (snapshot.exists()) aJson(snapshot.data) else JsonNull
    }

    private suspend fun <T> intentar(accion: suspend () -> T): Result<T> = try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.w(TAG, "Operación de privacidad fallida: ${e.message}")
        Result.failure(e)
    }

    private fun DocumentSnapshot.aSolicitud(): SolicitudPrivacidad? {
        val estado = EstadoSolicitud.desde(getString("estado")) ?: return null
        return SolicitudPrivacidad(
            uid = getString("uid") ?: id,
            nombre = getString("nombre").orEmpty(),
            rol = getString("rol").orEmpty(),
            motivo = getString("motivo").orEmpty(),
            estado = estado,
            creadaEn = getTimestamp("creadaEn", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time,
            respuesta = getString("respuesta").orEmpty()
        )
    }

    /** Valores de Firestore a JSON (las fechas quedan en ISO 8601). */
    internal fun aJson(valor: Any?): JsonElement = when (valor) {
        null -> JsonNull
        is String -> JsonPrimitive(valor)
        is Number -> JsonPrimitive(valor)
        is Boolean -> JsonPrimitive(valor)
        is Timestamp -> JsonPrimitive(valor.toDate().toInstant().toString())
        is Map<*, *> -> JsonObject(valor.entries.associate { (k, v) -> k.toString() to aJson(v) }.toSortedMap())
        is List<*> -> JsonArray(valor.map { aJson(it) })
        else -> JsonPrimitive(valor.toString())
    }
}
