package com.example.lince_track.data

import com.example.lince_track.R
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.util.UiText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Se lanza cuando una cuenta existe en Authentication pero su rol no se puede
 * determinar desde Firestore. Solo el administrador crea cuentas y asigna roles.
 * El mensaje se muestra al usuario; el detalle técnico va a LinceLogger.
 */
/** El mensaje queda en español (registro); [texto] es lo que se muestra, en el idioma de la app. */
class SinRolException(message: String, val texto: UiText = UiText.DynamicString(message)) : IllegalStateException(message)

/**
 * Fuente de datos de autenticación y roles.
 *
 * El administrador crea las cuentas manualmente en Firebase Console (Authentication)
 * y asigna el rol y el id de la API en Firestore (colección `usuarios`). Aquí no existe
 * registro de usuarios: solo inicio de sesión.
 */
object AuthRepository {

    private const val TAG = "AUTH"
    private const val COLECCION_USUARIOS = "usuarios"
    private const val MENSAJE_CUENTA_SIN_CONFIGURAR =
        "Tu cuenta no está configurada. Contacta al administrador."
    private const val MENSAJE_ERROR_CUENTA = "No se pudo leer tu cuenta. Intenta nuevamente."

    /** FirebaseAuth real; los tests lo reemplazan por uno simulado con MockK. */
    internal val authReemplazable = Reemplazable { FirebaseAuth.getInstance() }
    private val auth get() = authReemplazable.valor
    /** Firestore real; los tests lo reemplazan por uno simulado con MockK. */
    internal val firestoreReemplazable = Reemplazable { FirebaseFirestore.getInstance() }
    private val firestore get() = firestoreReemplazable.valor

    private val _rolActual = MutableStateFlow<Rol?>(null)
    val rolActual: StateFlow<Rol?> = _rolActual.asStateFlow()

    private val _perfilActual = MutableStateFlow<PerfilUsuario?>(null)
    /** Perfil del usuario con sesión iniciada (null si no hay sesión o aún no se carga). */
    val perfilActual: StateFlow<PerfilUsuario?> = _perfilActual.asStateFlow()

    private val _errorPerfil = MutableStateFlow<String?>(null)
    /** Motivo por el que no se pudo recuperar el perfil de una sesión ya abierta. */
    val errorPerfil: StateFlow<String?> = _errorPerfil.asStateFlow()

    private val _errorPerfilTexto = MutableStateFlow<UiText?>(null)
    /** El mismo motivo, para mostrarlo en el idioma de la app. */
    val errorPerfilTexto: StateFlow<UiText?> = _errorPerfilTexto.asStateFlow()

    val usuarioActual: FirebaseUser?
        get() = auth.currentUser

    /**
     * Inicia sesión con correo y contraseña y devuelve el rol del usuario.
     * Falla si las credenciales son incorrectas o si el rol no está bien configurado;
     * en ese caso la sesión queda cerrada.
     */
    suspend fun iniciarSesion(email: String, password: String): Result<Rol> = try {
        val resultado = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val uid = resultado.user?.uid ?: throw errorLeerCuenta()
        val perfil = obtenerPerfil(uid)
        establecerPerfil(perfil)
        Result.success(perfil.rol)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        auth.signOut()
        Result.failure(e)
    }

    /**
     * Carga el perfil de un usuario ya autenticado (útil al reabrir la app) y devuelve su rol.
     * Devuelve null (sin lanzar) si no se pudo recuperar; el motivo queda en [errorPerfil].
     */
    suspend fun cargarRolActual(uid: String): Rol? = try {
        val perfil = obtenerPerfil(uid)
        establecerPerfil(perfil)
        perfil.rol
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = e as? SinRolException ?: errorLeerCuenta()
        _errorPerfil.value = error.message
        _errorPerfilTexto.value = error.texto
        LinceLogger.e(TAG, "No se pudo recuperar el perfil de la sesión", e)
        null
    }

    fun cerrarSesion() {
        _rolActual.value = null
        _perfilActual.value = null
        _errorPerfil.value = null
        _errorPerfilTexto.value = null
        auth.signOut()
    }

    private fun establecerPerfil(perfil: PerfilUsuario) {
        _rolActual.value = perfil.rol
        _perfilActual.value = perfil
        _errorPerfil.value = null
        _errorPerfilTexto.value = null
    }

    private fun errorLeerCuenta() =
        SinRolException(MENSAJE_ERROR_CUENTA, UiText.StringResource(R.string.error_leer_cuenta))

    private fun errorCuentaSinConfigurar() =
        SinRolException(MENSAJE_CUENTA_SIN_CONFIGURAR, UiText.StringResource(R.string.error_cuenta_sin_configurar))

    private suspend fun obtenerPerfil(uid: String): PerfilUsuario {
        val documento = try {
            firestore.collection(COLECCION_USUARIOS).document(uid).get().await()
        } catch (e: FirebaseFirestoreException) {
            LinceLogger.e(TAG, "Error al leer el documento del usuario (${e.code})", e)
            throw when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    SinRolException(
                        "Tu cuenta no tiene permiso para ingresar. Contacta al administrador.",
                        UiText.StringResource(R.string.error_cuenta_sin_permiso)
                    )
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    SinRolException(
                        "Sin conexión con la base de datos. Revisa tu internet.",
                        UiText.StringResource(R.string.error_sin_conexion_bd)
                    )
                else ->
                    errorLeerCuenta()
            }
        }

        if (!documento.exists()) {
            LinceLogger.w(TAG, "No existe el documento del usuario en '$COLECCION_USUARIOS'")
            throw errorCuentaSinConfigurar()
        }

        val valorRol = documento.getString("rol")
        val rol = Rol.desde(valorRol)
        if (rol == null) {
            LinceLogger.w(TAG, "Campo 'rol' no válido", details = "valor=\"$valorRol\"")
            throw errorCuentaSinConfigurar()
        }

        return PerfilUsuario(
            rol = rol,
            nombre = documento.getString("nombre"),
            telefono = documento.getString("telefono"),
            transportistaId = documento.idPersona("transportistaId"),
            guiaId = documento.idPersona("guiaId"),
            perfilNuevo = documento.get("perfilNuevo") == true
        )
    }

    /** Lee un id numérico. Un valor de otro tipo cuenta como cuenta sin vincular. */
    private fun DocumentSnapshot.idPersona(campo: String): Long? {
        val valor = get(campo) ?: return null
        val id = (valor as? Number)?.toLong()
        if (id == null) LinceLogger.w(TAG, "El campo '$campo' debe ser un número")
        return id
    }
}
