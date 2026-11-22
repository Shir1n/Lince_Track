package com.example.lince_track.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.R
import com.example.lince_track.data.ResumenSouthbound
import com.example.lince_track.data.ResumenSouthboundRepository
import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.util.UiText
import com.example.lince_track.util.textoUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Lo que muestra la tarjeta "Datos de Southbound". */
data class EstadoResumenSouthbound(
    val cargando: Boolean = false,
    val resumen: ResumenSouthbound? = null,
    val error: UiText? = null
)

/**
 * Pide a Southbound el resumen de la oficina para el periodo elegido en el Resumen (Hoy o Últimos 7 días).
 * Las dependencias llegan por el constructor, con la app real por defecto, para que los tests pasen lambdas.
 *
 * @param disponible false sin clave de la API: con los datos de ejemplo no hay `/resumen` y la tarjeta no se muestra.
 */
class ResumenSouthboundViewModel(
    private val cargarResumen: suspend (ClosedRange<LocalDate>) -> Result<ResumenSouthbound> =
        { rango -> ResumenSouthboundRepository().obtener(rango) },
    val disponible: Boolean = LinceApiClient.estaConfigurada,
    private val hoy: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoResumenSouthbound())
    val estado: StateFlow<EstadoResumenSouthbound> = _estado.asStateFlow()

    private var periodoActual: PeriodoResumen? = null
    private var carga: Job? = null

    /** Pide el resumen del periodo. Si ya se cargó o se está cargando ese mismo periodo, no lo vuelve a pedir. */
    fun cargar(periodo: PeriodoResumen) {
        if (!disponible) return
        if (periodo == periodoActual && (_estado.value.resumen != null || _estado.value.cargando)) return
        pedir(periodo)
    }

    /** Vuelve a pedir el último periodo (por ejemplo, tras un error). */
    fun reintentar() {
        periodoActual?.let { pedir(it) }
    }

    private fun pedir(periodo: PeriodoResumen) {
        periodoActual = periodo
        carga?.cancel()
        _estado.value = EstadoResumenSouthbound(cargando = true)
        carga = viewModelScope.launch {
            cargarResumen(ResumenOperacion.rango(periodo, hoy()))
                .onSuccess { _estado.value = EstadoResumenSouthbound(resumen = it) }
                .onFailure { _estado.value = EstadoResumenSouthbound(error = textoError(it)) }
        }
    }

    /** El error en el idioma de la app, con el requestId de la API si vino (para buscarlo en sus registros). */
    private fun textoError(error: Throwable): UiText {
        val texto = error.textoUi(R.string.resumen_southbound_error)
        val requestId = (error as? LinceApiException)?.requestId
        return if (requestId.isNullOrBlank()) texto else UiText.StringResource(R.string.error_con_id, texto, requestId)
    }
}
