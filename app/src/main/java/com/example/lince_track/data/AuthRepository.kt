package com.example.lince_track.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Se lanza cuando una cuenta existe en Authentication pero su rol no se puede
 * determinar desde Firestore. Solo el administrador crea cuentas y asigna roles.
 */
class SinRolException(message: String) : IllegalStateException(message)

/**
 * Fuente de datos de autenticación y roles.
 *
 * El administrador crea las cuentas manualmente en Firebase Console (Authentication)
 * y asigna el rol en Firestore (colección `usuarios`). Aquí no existe registro de
 * usuarios: solo inicio de sesión.
 */
object AuthRepository {

    private const val COLECCION_USUARIOS = "usuarios"

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _rolActual = MutableStateFlow<Rol?>(null)
    val rolActual: StateFlow<Rol?> = _rolActual.asStateFlow()

    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    /**
     * Inicia sesión con correo y contraseña y devuelve el rol del usuario.
     * Falla si las credenciales son incorrectas o si el rol no está bien configurado.
     */
    suspend fun iniciarSesion(email: String, password: String): Result<Rol> = runCatching {
        val resultado = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = resultado.user?.uid ?: throw SinRolException("No se pudo obtener tu usuario.")
        val rol = obtenerRol(uid)
        _rolActual.value = rol
        rol
    }

    /**
     * Carga el rol de un usuario ya autenticado (útil al reabrir la app).
     * Devuelve null (sin lanzar) si no se pudo recuperar el rol.
     */
    suspend fun cargarRolActual(uid: String): Rol? = runCatching { obtenerRol(uid) }
        .onSuccess { _rolActual.value = it }
        .getOrNull()

    fun cerrarSesion() {
        _rolActual.value = null
        auth.signOut()
    }

    private suspend fun obtenerRol(uid: String): Rol {
        val documento = try {
            firestore.collection(COLECCION_USUARIOS).document(uid).get().await()
        } catch (e: FirebaseFirestoreException) {
            throw when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    SinRolException("Sin permiso para leer tu cuenta. Publica las reglas de Firestore (firestore.rules).")
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    SinRolException("Sin conexión con la base de datos. Revisa tu internet.")
                else ->
                    SinRolException("Error al leer tu cuenta: ${e.message ?: "desconocido"}")
            }
        }

        if (!documento.exists()) {
            throw SinRolException(
                "No existe tu documento en la colección 'usuarios'. " +
                    "Verifica que el ID del documento sea exactamente tu UID de Authentication."
            )
        }

        val valorRol = documento.getString("rol")
        return Rol.desde(valorRol)
            ?: throw SinRolException(
                "El campo 'rol' no es válido (valor actual: \"$valorRol\"). " +
                    "Debe ser: conductor, guia o administrador."
            )
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
}
