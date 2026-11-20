package com.example.lince_track.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.data.MotivoRechazo
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Estado del calendario. Las dependencias llegan por el constructor (con la app real como valor por defecto) para
 * que los tests las reemplacen; Compose lo crea con `viewModel()`, que usa el constructor sin argumentos.
 */
class AgendaViewModel(
    val servicios: StateFlow<List<Servicio>> = ServicioRepository.servicios,
    private val aceptarServicio: suspend (String) -> Result<String?> = ServicioRepository::aceptar,
    private val rechazarServicio: suspend (String, MotivoRechazo, String) -> Result<String?> =
        ServicioRepository::rechazar,
    private val hoy: () -> LocalDate = LocalDate::now
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(hoy())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.from(hoy()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _processingIds = MutableStateFlow<Set<String>>(emptySet())
    val processingIds: StateFlow<Set<String>> = _processingIds.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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

    private fun operar(servicio: Servicio, op: suspend (String) -> Result<String?>) {
        if (servicio.id in _processingIds.value) return
        viewModelScope.launch {
            _processingIds.update { it + servicio.id }
            val resultado = op(servicio.id)
            _processingIds.update { it - servicio.id }
            resultado.onSuccess { aviso ->
                if (aviso != null) _message.value = aviso
            }.onFailure { e ->
                _message.value = e.message ?: "No se pudo completar la operación"
            }
        }
    }
}
