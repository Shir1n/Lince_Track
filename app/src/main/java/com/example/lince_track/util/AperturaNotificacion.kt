package com.example.lince_track.util

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio que hay que abrir porque se tocó una notificación. Lo deja MainActivity y lo consume la navegación
 * cuando ya hay sesión (pantalla principal).
 */
object AperturaNotificacion {

    /** Extra del intent: lo pone NotificadorLocal y lo envían las Cloud Functions en `data`. */
    const val EXTRA_SERVICIO = "servicioId"

    private val _servicioId = MutableStateFlow<String?>(null)
    val servicioId: StateFlow<String?> = _servicioId.asStateFlow()

    fun desde(intent: Intent?) {
        intent?.getStringExtra(EXTRA_SERVICIO)?.takeIf { it.isNotBlank() }?.let { _servicioId.value = it }
        intent?.removeExtra(EXTRA_SERVICIO)
    }

    fun consumir() {
        _servicioId.value = null
    }
}
