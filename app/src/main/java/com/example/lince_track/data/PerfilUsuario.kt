package com.example.lince_track.data

/**
 * Datos del usuario con sesión iniciada, leídos desde Firestore (`usuarios/{uid}`).
 *
 * @property transportistaId / guiaId id de la persona en la API de Southbound. Solo se usa
 *   el que corresponde al rol; null significa que la cuenta aún no está vinculada.
 * @property perfilNuevo un conductor con true ve `PerfilRealScreen`; el resto de los conductores
 *   sigue con `PerfilScreen`, que queda como placeholder con datos de ejemplo.
 */
data class PerfilUsuario(
    val rol: Rol,
    val nombre: String? = null,
    val transportistaId: Long? = null,
    val guiaId: Long? = null,
    val perfilNuevo: Boolean = false
)
