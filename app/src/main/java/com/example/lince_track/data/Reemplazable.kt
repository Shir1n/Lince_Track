package com.example.lince_track.data

/**
 * Dependencia de un repositorio (Firestore, FirebaseAuth, la fuente de servicios) que se obtiene recién al usarla y
 * que los tests pueden reemplazar por una simulada con MockK.
 *
 * En la app nunca se reemplaza: [valor] devuelve siempre la real. Así los repositorios siguen siendo `object` y las
 * pantallas los usan igual que antes, pero los tests de JVM no necesitan Firebase ni conexión.
 */
internal class Reemplazable<T : Any>(private val real: () -> T) {

    private var reemplazo: T? = null

    val valor: T
        get() = reemplazo ?: real()

    /** Usa [nuevo] en vez de la real; con null vuelve a la real. */
    fun reemplazar(nuevo: T?) {
        reemplazo = nuevo
    }
}
