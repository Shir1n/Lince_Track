package com.example.lince_track.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.R
import com.example.lince_track.data.PuntualidadEquipo
import com.example.lince_track.data.PuntualidadEquipoRepository
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.util.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

/** Lo que muestra la tarjeta "Puntualidad del equipo". */
data class EstadoPuntualidadEquipo(
    val cargando: Boolean = false,
    val equipo: PuntualidadEquipo? = null,
    val error: UiText? = null
)

/**
 * Pide al backend propio la puntualidad por persona para el periodo elegido en el Resumen (Hoy o Últimos 7 días).
 * Las dependencias llegan por el constructor, con la app real por defecto, para que los tests pasen lambdas.
 */
class PuntualidadEquipoViewModel(
    private val cargarEquipo: suspend (ClosedRange<LocalDate>) -> Result<PuntualidadEquipo> =
        { rango -> PuntualidadEquipoRepository().obtener(rango) },
    private val hoy: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoPuntualidadEquipo())
    val estado: StateFlow<EstadoPuntualidadEquipo> = _estado.asStateFlow()

    private var periodoActual: PeriodoResumen? = null
    private var carga: Job? = null

    /** Pide la puntualidad del periodo. Si ya se cargó o se está cargando ese mismo periodo, no la vuelve a pedir. */
    fun cargar(periodo: PeriodoResumen) {
        if (periodo == periodoActual && (_estado.value.equipo != null || _estado.value.cargando)) return
        pedir(periodo)
    }

    /** Vuelve a pedir el último periodo (por ejemplo, tras un error). */
    fun reintentar() {
        periodoActual?.let { pedir(it) }
    }

    private fun pedir(periodo: PeriodoResumen) {
        periodoActual = periodo
        carga?.cancel()
        _estado.value = EstadoPuntualidadEquipo(cargando = true)
        carga = viewModelScope.launch {
            cargarEquipo(ResumenOperacion.rango(periodo, hoy()))
                .onSuccess { _estado.value = EstadoPuntualidadEquipo(equipo = it) }
                .onFailure { _estado.value = EstadoPuntualidadEquipo(error = textoError(it)) }
        }
    }

    /** El error en el idioma de la app, con el requestId del backend si vino (para buscarlo en sus registros). */
    private fun textoError(error: Throwable): UiText {
        val e = error as? BloqueoApiException
        val texto = when {
            e?.codigoHttp == 403 -> UiText.StringResource(R.string.puntualidad_sin_permiso)
            e?.cause is IOException -> UiText.StringResource(R.string.error_sin_conexion_servidor)
            else -> UiText.StringResource(R.string.puntualidad_error)
        }
        val requestId = e?.requestId
        return if (requestId.isNullOrBlank()) texto else UiText.StringResource(R.string.error_con_id, texto, requestId)
    }
}
