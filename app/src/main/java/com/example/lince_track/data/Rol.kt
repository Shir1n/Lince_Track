package com.example.lince_track.data

/**
 * Tipos de usuario de la aplicación.
 *
 * El valor [id] es el que se guarda como texto en Firestore (colección `usuarios`),
 * y [etiqueta] es el texto legible para mostrar en pantalla.
 */
enum class Rol(val id: String, val etiqueta: String) {
    ADMINISTRADOR("administrador", "Administrador"),
    CONDUCTOR("conductor", "Conductor"),
    GUIA("guia", "Guía");

    companion object {
        fun desde(valor: String?): Rol? =
            entries.firstOrNull { it.id.equals(valor, ignoreCase = true) }
    }
}
