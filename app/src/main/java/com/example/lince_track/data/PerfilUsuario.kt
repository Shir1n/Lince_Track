package com.example.lince_track.data

/**
 * Datos del usuario con sesión iniciada, leídos desde Firestore (`usuarios/{uid}`).
 *
 * @property transportistaId / guiaId id de la persona en la API de Southbound. Solo se usa
 *   el que corresponde al rol; null significa que la cuenta aún no está vinculada.
 */
data class PerfilUsuario(
    val rol: Rol,
    val nombre: String? = null,
    val transportistaId: Long? = null,
    val guiaId: Long? = null
)
