package com.example.lince_track.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.Rol
import com.example.lince_track.data.SinRolException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginEstado(
    val cargando: Boolean = false,
    val error: String? = null,
    val rol: Rol? = null
)

class LoginViewModel : ViewModel() {

    private val _estado = MutableStateFlow(
        LoginEstado(cargando = AuthRepository.usuarioActual != null)
    )
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    init {
        // Si ya hay una sesión activa (app reabierta), recupera el rol automáticamente.
        val usuario = AuthRepository.usuarioActual
        if (usuario != null) {
            viewModelScope.launch {
                val rol = AuthRepository.cargarRolActual(usuario.uid)
                _estado.value = if (rol != null) LoginEstado(rol = rol) else LoginEstado()
            }
        }
    }

    fun iniciarSesion(email: String, password: String) {
        if (_estado.value.cargando) return
        if (email.isBlank() || password.isBlank()) {
            _estado.value = LoginEstado(error = "Ingresa tu correo y contraseña")
            return
        }
        viewModelScope.launch {
            _estado.value = LoginEstado(cargando = true)
            val resultado = AuthRepository.iniciarSesion(email, password)
            _estado.value = resultado.fold(
                onSuccess = { LoginEstado(rol = it) },
                onFailure = { LoginEstado(error = it.mensajeAmigable()) }
            )
        }
    }

    fun limpiarError() {
        _estado.value = _estado.value.copy(error = null)
    }

    private fun Throwable.mensajeAmigable(): String = when (this) {
        is SinRolException -> message ?: "Sin rol asignado"
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet."
        else -> message ?: "No se pudo iniciar sesión. Intenta nuevamente."
    }
}
