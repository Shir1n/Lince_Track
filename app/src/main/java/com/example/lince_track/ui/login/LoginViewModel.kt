package com.example.lince_track.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.SinRolException
import com.example.lince_track.util.UiText
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginEstado(
    val cargando: Boolean = false,
    val error: UiText? = null,
    val rol: Rol? = null
)

/**
 * Estado del inicio de sesión. Las dependencias llegan por el constructor (con AuthRepository como valor por defecto)
 * para que los tests las reemplacen; Compose lo crea con `viewModel()`, que usa el constructor sin argumentos.
 */
class LoginViewModel(
    private val uidActual: () -> String? = { AuthRepository.usuarioActual?.uid },
    private val cargarRol: suspend (String) -> Rol? = AuthRepository::cargarRolActual,
    private val iniciar: suspend (String, String) -> Result<Rol> = AuthRepository::iniciarSesion
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginEstado(cargando = uidActual() != null))
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    init {
        // Si ya hay una sesión activa (app reabierta), recupera el rol automáticamente.
        val uid = uidActual()
        if (uid != null) {
            viewModelScope.launch {
                val rol = cargarRol(uid)
                _estado.value = if (rol != null) LoginEstado(rol = rol) else LoginEstado()
            }
        }
    }

    fun iniciarSesion(email: String, password: String) {
        if (_estado.value.cargando) return
        if (email.isBlank() || password.isBlank()) {
            _estado.value = LoginEstado(error = UiText.StringResource(R.string.login_error_campos_vacios))
            return
        }
        viewModelScope.launch {
            _estado.value = LoginEstado(cargando = true)
            val resultado = iniciar(email, password)
            _estado.value = resultado.fold(
                onSuccess = { LoginEstado(rol = it) },
                onFailure = { LoginEstado(error = it.mensajeAmigable()) }
            )
        }
    }

    fun limpiarError() {
        _estado.value = _estado.value.copy(error = null)
    }

    private fun Throwable.mensajeAmigable(): UiText = when (this) {
        is SinRolException -> message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.login_error_sin_rol)
        is FirebaseAuthInvalidUserException -> UiText.StringResource(R.string.login_error_usuario_no_existe)
        is FirebaseAuthInvalidCredentialsException -> UiText.StringResource(R.string.login_error_credenciales)
        is FirebaseNetworkException -> UiText.StringResource(R.string.login_error_red)
        else -> message?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.login_error_generico)
    }
}
