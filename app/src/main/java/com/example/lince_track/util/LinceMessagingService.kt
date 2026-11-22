package com.example.lince_track.util

import com.example.lince_track.data.TokenFcmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recibe las notificaciones que envían las Cloud Functions. Con la app cerrada o en segundo plano,
 * Android las muestra solo (con el canal "servicios") y al tocarlas abre la app con el `servicioId`.
 * Con la app abierta llegan aquí: no se muestran, porque [NotificadorLocal] ya avisa lo mismo.
 */
class LinceMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch {
            runCatching { TokenFcmRepository.guardar(uid, token) }
                .onFailure { LinceLogger.w("AVISOS", "No se pudo guardar el token nuevo: ${it.message}") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        LinceLogger.i("AVISOS", "Aviso recibido con la app abierta: ${message.data["tipo"] ?: "-"}")
    }
}
