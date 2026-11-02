package com.example.lince_track.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val ESPERA_CONFIRMACION_MS = 5_000L

/** Espera el resultado de una tarea de Firebase desde una corrutina. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

/**
 * Espera una escritura de Firestore. Devuelve true si el servidor la confirmó y false si sigue
 * en cola: sin conexión, Firestore la envía al recuperarla, pero la tarea no termina hasta entonces.
 */
internal suspend fun Task<*>.confirmada(): Boolean =
    withTimeoutOrNull(ESPERA_CONFIRMACION_MS) {
        await()
        true
    } ?: false
