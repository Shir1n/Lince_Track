package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Qué hacer con el token del teléfono cuando cambia la sesión. */
enum class AccionToken { GUARDAR, BORRAR, NADA }

/**
 * Token de notificaciones (Firebase Cloud Messaging) del teléfono, guardado en `tokensFcm/{uid}`.
 * Las Cloud Functions lo leen para avisar con la app cerrada. Se guarda un solo teléfono por cuenta:
 * el último donde inició sesión.
 */
object TokenFcmRepository {

    private const val TAG = "AVISOS"
    internal const val COLECCION = "tokensFcm"

    /** Firestore real; los tests lo reemplazan por uno simulado con MockK. */
    internal val firestoreReemplazable = Reemplazable { FirebaseFirestore.getInstance() }
    private val firestore get() = firestoreReemplazable.valor

    /** Pide y borra el token en Firebase Messaging; los tests lo reemplazan. */
    internal val mensajeriaReemplazable = Reemplazable<Mensajeria> { MensajeriaFirebase }
    private val mensajeria get() = mensajeriaReemplazable.valor

    /** Al iniciar sesión se guarda el token; al cerrarla se borra (así no llegan avisos de otra cuenta). */
    fun accion(uidAnterior: String?, uidActual: String?): AccionToken = when {
        uidActual != null && uidActual != uidAnterior -> AccionToken.GUARDAR
        uidActual == null && uidAnterior != null -> AccionToken.BORRAR
        else -> AccionToken.NADA
    }

    private var iniciado = false

    /** Sigue la sesión (perfil cargado o no) mientras la app está viva. */
    fun iniciar(scope: CoroutineScope) {
        if (iniciado) return
        iniciado = true
        scope.launch {
            var anterior: String? = null
            AuthRepository.perfilActual
                .map { perfil -> perfil?.let { AuthRepository.usuarioActual?.uid } }
                .distinctUntilChanged()
                .collect { uid ->
                    when (accion(anterior, uid)) {
                        AccionToken.GUARDAR -> registrar(uid!!)
                        AccionToken.BORRAR -> olvidarTelefono()
                        AccionToken.NADA -> Unit
                    }
                    anterior = uid
                }
        }
    }

    /** Pide el token del teléfono y lo guarda para [uid]. */
    suspend fun registrar(uid: String): Result<Unit> = try {
        guardar(uid, mensajeria.token())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.e(TAG, "No se pudo registrar el teléfono para notificaciones", e)
        Result.failure(e)
    }

    /** Guarda [token] para [uid]. Firebase lo llama también cuando cambia el token. */
    suspend fun guardar(uid: String, token: String) {
        firestore.collection(COLECCION).document(uid).set(
            mapOf(
                "token" to token,
                "actualizadoEn" to FieldValue.serverTimestamp()
            )
        ).await()
        LinceLogger.i(TAG, "Teléfono registrado para notificaciones")
    }

    /**
     * Al cerrar sesión ya no hay permiso para borrar el documento, así que se anula el token en Firebase:
     * la Cloud Function que intente usarlo lo recibe como inválido y borra el documento.
     */
    suspend fun olvidarTelefono(): Result<Unit> = try {
        mensajeria.borrarToken()
        LinceLogger.i(TAG, "Token de notificaciones anulado al cerrar sesión")
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.w(TAG, "No se pudo anular el token de notificaciones: ${e.message}")
        Result.failure(e)
    }
}

/** Lo que se usa de Firebase Messaging, separado para probar el repositorio sin Firebase. */
internal interface Mensajeria {
    suspend fun token(): String
    suspend fun borrarToken()
}

private object MensajeriaFirebase : Mensajeria {
    override suspend fun token(): String = FirebaseMessaging.getInstance().token.await()
    override suspend fun borrarToken() {
        FirebaseMessaging.getInstance().deleteToken().await()
    }
}
