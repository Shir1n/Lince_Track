package com.example.lince_track.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.R
import com.example.lince_track.data.EquipoSouthbound
import com.example.lince_track.data.EquipoSouthboundRepository
import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.util.UiText
import com.example.lince_track.util.textoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lo que muestra la sección "Equipo". */
data class EstadoEquipo(
    val cargando: Boolean = false,
    val equipo: EquipoSouthbound? = null,
    val error: UiText? = null
)

/**
 * Equipo de la oficina (`/guias` y `/transportistas`) y tipos de servicio (`/catalogos`) para el panel del
 * administrador. Las dependencias llegan por el constructor, con la app real por defecto, para que los tests pasen
 * lambdas.
 *
 * @param disponible false sin clave de la API: no hay equipo que mostrar y el filtro usa los tipos conocidos.
 */
class EquipoSouthboundViewModel(
    private val cargarEquipoApi: suspend () -> Result<EquipoSouthbound> =
        { EquipoSouthboundRepository().equipo() },
    private val cargarTiposApi: suspend () -> Result<List<String>> =
        { EquipoSouthboundRepository().tiposServicio() },
    val disponible: Boolean = LinceApiClient.estaConfigurada
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoEquipo())
    val estado: StateFlow<EstadoEquipo> = _estado.asStateFlow()

    private val _tiposServicio = MutableStateFlow(EquipoSouthboundRepository.TIPOS_SERVICIO_CONOCIDOS)
    /** Opciones del filtro por tipo de servicio. */
    val tiposServicio: StateFlow<List<String>> = _tiposServicio.asStateFlow()

    private var tiposCargados = false

    /** Pide el equipo una vez; si ya está o se está pidiendo, no hace nada. */
    fun cargarEquipo() {
        if (!disponible) return
        if (_estado.value.equipo != null || _estado.value.cargando) return
        pedirEquipo()
    }

    /** Vuelve a pedir el equipo (por ejemplo, tras un error). */
    fun reintentar() {
        if (disponible && !_estado.value.cargando) pedirEquipo()
    }

    /** Pide los tipos de servicio a `/catalogos` una vez; si falla, quedan los conocidos. */
    fun cargarTipos() {
        if (!disponible || tiposCargados) return
        tiposCargados = true
        viewModelScope.launch {
            cargarTiposApi()
                .onSuccess { tipos -> if (tipos.isNotEmpty()) _tiposServicio.value = tipos }
                .onFailure { e -> LinceLogger.w("EQUIPO", "Sin tipos de /catalogos; se usan los conocidos: ${e.message}") }
        }
    }

    private fun pedirEquipo() {
        _estado.value = EstadoEquipo(cargando = true)
        viewModelScope.launch {
            cargarEquipoApi()
                .onSuccess { _estado.value = EstadoEquipo(equipo = it) }
                .onFailure { _estado.value = EstadoEquipo(error = textoError(it)) }
        }
    }

    /** El error en el idioma de la app, con el requestId de la API si vino. */
    private fun textoError(error: Throwable): UiText {
        val texto = error.textoUi(R.string.equipo_error)
        val requestId = (error as? LinceApiException)?.requestId
        return if (requestId.isNullOrBlank()) texto else UiText.StringResource(R.string.error_con_id, texto, requestId)
    }
}
