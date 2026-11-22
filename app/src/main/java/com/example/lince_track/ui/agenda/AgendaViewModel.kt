package com.example.lince_track.ui.agenda

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.R
import com.example.lince_track.data.Bloqueo
import com.example.lince_track.data.BloqueoRepository
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.ReglasBloqueo
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFecha
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.util.LinceLogger
import com.example.lince_track.util.UiText
import com.example.lince_track.util.textoUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Estado del calendario y gestión de disponibilidad (bloqueo de días).
 * Las dependencias llegan por el constructor (con la app real como valor por defecto) para
 * que los tests las reemplacen; Compose lo crea con `viewModel()`, que usa el constructor sin argumentos.
 */
class AgendaViewModel(
    val servicios: StateFlow<List<Servicio>> = ServicioRepository.servicios,
    private val aceptarServicio: suspend (String) -> Result<String?> = ServicioRepository::aceptar,
    private val rechazarServicio: suspend (String, MotivoRechazo, String) -> Result<String?> =
        ServicioRepository::rechazar,
    private val bloqueoRepository: BloqueoRepository = BloqueoRepository(),
    private val hoy: () -> LocalDate = LocalDate::now,
    autoCargarBloqueos: Boolean = true
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(hoy())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.from(hoy()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _processingIds = MutableStateFlow<Set<String>>(emptySet())
    val processingIds: StateFlow<Set<String>> = _processingIds.asStateFlow()

    // UiText y no String: la pantalla lo traduce al idioma elegido en la app.
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    // --- Bloqueo de días (disponibilidad) ---
    private val _bloqueos = MutableStateFlow<List<Bloqueo>>(emptyList())
    val bloqueos: StateFlow<List<Bloqueo>> = _bloqueos.asStateFlow()

    private val _diasAnticipacion = MutableStateFlow(7)
    val diasAnticipacion: StateFlow<Int> = _diasAnticipacion.asStateFlow()

    private val _cargandoBloqueos = MutableStateFlow(false)
    val cargandoBloqueos: StateFlow<Boolean> = _cargandoBloqueos.asStateFlow()

    private val _operandoBloqueo = MutableStateFlow(false)
    val operandoBloqueo: StateFlow<Boolean> = _operandoBloqueo.asStateFlow()

    init {
        if (autoCargarBloqueos) {
            cargarBloqueosYParametros()
        }
    }

    fun seleccionarFecha(fecha: LocalDate) {
        _selectedDate.value = fecha
        _month.value = YearMonth.from(fecha)
    }

    fun mesAnterior() {
        _month.update { it.minusMonths(1) }
    }

    fun mesSiguiente() {
        _month.update { it.plusMonths(1) }
    }

    fun irHoy() {
        val fecha = hoy()
        _selectedDate.value = fecha
        _month.value = YearMonth.from(fecha)
    }

    fun aceptar(servicio: Servicio) = operar(servicio) { aceptarServicio(it) }

    fun rechazar(servicio: Servicio, motivo: MotivoRechazo, observaciones: String) =
        operar(servicio) { rechazarServicio(it, motivo, observaciones) }

    fun limpiarMensaje() {
        _message.value = null
    }

    // --- Disponibilidad ---

    fun cargarBloqueosYParametros() {
        viewModelScope.launch {
            _cargandoBloqueos.value = true
            bloqueoRepository.obtenerParametros().onSuccess { params ->
                _diasAnticipacion.value = params.diasAnticipacionBloqueo
            }.onFailure { e ->
                LinceLogger.w("AGENDA", "No se pudieron cargar parámetros: ${e.message}")
            }

            bloqueoRepository.listar().onSuccess { lista ->
                _bloqueos.value = lista
            }.onFailure { e ->
                LinceLogger.w("AGENDA", "No se pudieron listar bloqueos: ${e.message}")
            }
            _cargandoBloqueos.value = false
        }
    }

    fun primerDiaBloqueable(): LocalDate =
        ReglasBloqueo.primerDiaBloqueable(hoy(), _diasAnticipacion.value)

    fun esDiaBloqueable(fecha: LocalDate): Boolean =
        ReglasBloqueo.esDiaBloqueable(fecha, servicios.value, _bloqueos.value, hoy(), _diasAnticipacion.value)

    fun estaBloqueado(fecha: LocalDate): Boolean =
        ReglasBloqueo.estaBloqueado(fecha, _bloqueos.value)

    fun bloqueoDe(fecha: LocalDate): Bloqueo? =
        _bloqueos.value.find { it.fecha == fecha }

    /**
     * Solicita el bloqueo de un día.
     * Realiza doble validación: primero en cliente con las reglas de negocio, y luego en el backend.
     */
    fun bloquearDia(fecha: LocalDate, motivo: String, onResultado: ((Boolean) -> Unit)? = null) {
        if (_operandoBloqueo.value) return

        // Validación en cliente (doble validación)
        val fechaHoy = hoy()
        val anticipacion = _diasAnticipacion.value
        if (!ReglasBloqueo.tieneAnticipacion(fecha, fechaHoy, anticipacion)) {
            val primerDia = ReglasBloqueo.primerDiaBloqueable(fechaHoy, anticipacion)
            _message.value = UiText.StringResource(R.string.bloqueo_desde, formatFecha(primerDia))
            onResultado?.invoke(false)
            return
        }

        if (ReglasBloqueo.tieneServicioAsignado(servicios.value, fecha)) {
            _message.value = UiText.StringResource(R.string.bloqueo_dia_con_servicio)
            onResultado?.invoke(false)
            return
        }

        if (ReglasBloqueo.estaBloqueado(fecha, _bloqueos.value)) {
            _message.value = UiText.StringResource(R.string.bloqueo_ya_bloqueado)
            onResultado?.invoke(false)
            return
        }

        // El motivo es obligatorio (el diálogo tampoco deja confirmar sin él).
        if (motivo.isBlank()) {
            _message.value = UiText.StringResource(R.string.bloqueo_motivo_obligatorio)
            onResultado?.invoke(false)
            return
        }

        viewModelScope.launch {
            _operandoBloqueo.value = true
            val res = bloqueoRepository.crear(fecha, motivo)
            _operandoBloqueo.value = false
            res.onSuccess { nuevoBloqueo ->
                _bloqueos.update { lista -> (lista.filterNot { it.id == nuevoBloqueo.id } + nuevoBloqueo).sortedBy { it.fecha } }
                _message.value = UiText.StringResource(R.string.bloqueo_ok)
                onResultado?.invoke(true)
            }.onFailure { error ->
                _message.value = textoErrorBloqueo(error, R.string.bloqueo_error_crear)
                // Si coordinación cambió la anticipación, la app tenía un valor viejo: se vuelve a pedir.
                if ((error as? BloqueoApiException)?.codigoApi == "SIN_ANTICIPACION") cargarBloqueosYParametros()
                onResultado?.invoke(false)
            }
        }
    }

    /**
     * Elimina el bloqueo de un día en el backend y actualiza el estado local.
     */
    fun desbloquearDia(bloqueoId: Long, onResultado: ((Boolean) -> Unit)? = null) {
        if (_operandoBloqueo.value) return
        viewModelScope.launch {
            _operandoBloqueo.value = true
            val res = bloqueoRepository.eliminar(bloqueoId)
            _operandoBloqueo.value = false
            res.onSuccess {
                _bloqueos.update { lista -> lista.filterNot { it.id == bloqueoId } }
                _message.value = UiText.StringResource(R.string.bloqueo_eliminado)
                onResultado?.invoke(true)
            }.onFailure { error ->
                _message.value = textoErrorBloqueo(error, R.string.bloqueo_error_eliminar)
                onResultado?.invoke(false)
            }
        }
    }

    /**
     * Error del backend de bloqueos en el idioma de la app: los códigos conocidos usan textos propios; los demás, el
     * mensaje del servidor. Si el backend mandó un requestId, se agrega para poder buscarlo en sus logs.
     */
    private fun textoErrorBloqueo(error: Throwable, @StringRes porDefecto: Int): UiText {
        val e = error as? BloqueoApiException
        val texto = when {
            e?.codigoApi == "SIN_ANTICIPACION" -> UiText.StringResource(R.string.bloqueo_sin_anticipacion)
            e?.codigoApi == "DIA_CON_SERVICIO" -> UiText.StringResource(R.string.bloqueo_dia_con_servicio)
            e?.codigoApi == "YA_BLOQUEADO" -> UiText.StringResource(R.string.bloqueo_ya_bloqueado)
            e?.codigoApi == "MOTIVO_OBLIGATORIO" -> UiText.StringResource(R.string.bloqueo_motivo_obligatorio)
            e?.codigoHttp == 403 -> UiText.StringResource(R.string.bloqueo_sin_permiso)
            e?.codigoHttp == 404 -> UiText.StringResource(R.string.bloqueo_no_encontrado)
            e?.cause is IOException -> UiText.StringResource(R.string.error_sin_conexion_servidor)
            e?.codigoHttp != null && !e.message.isNullOrBlank() -> UiText.DynamicString(e.message!!)
            else -> UiText.StringResource(porDefecto)
        }
        val requestId = e?.requestId
        return if (requestId.isNullOrBlank()) texto else UiText.StringResource(R.string.error_con_id, texto, requestId)
    }

    private fun operar(servicio: Servicio, op: suspend (String) -> Result<String?>) {
        if (servicio.id in _processingIds.value) return
        viewModelScope.launch {
            _processingIds.update { it + servicio.id }
            val resultado = op(servicio.id)
            _processingIds.update { it - servicio.id }
            resultado.onSuccess { aviso ->
                if (aviso != null) _message.value = ServicioRepository.textoAviso(aviso)
            }.onFailure { e ->
                _message.value = e.textoUi(R.string.agenda_operacion_fallida)
            }
        }
    }
}
