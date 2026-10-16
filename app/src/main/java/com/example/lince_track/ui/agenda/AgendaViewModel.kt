package com.example.lince_track.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class AgendaViewModel : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _processingIds = MutableStateFlow<Set<String>>(emptySet())
    val processingIds: StateFlow<Set<String>> = _processingIds.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val servicios: StateFlow<List<Servicio>> = ServicioRepository.servicios

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
        val hoy = LocalDate.now()
        _selectedDate.value = hoy
        _month.value = YearMonth.from(hoy)
    }

    fun aceptar(servicio: Servicio) = operar(servicio) { ServicioRepository.aceptar(it) }

    fun rechazar(servicio: Servicio) = operar(servicio) { ServicioRepository.rechazar(it) }

    fun limpiarMensaje() {
        _message.value = null
    }

    private fun operar(servicio: Servicio, op: suspend (String) -> Result<Unit>) {
        if (servicio.id in _processingIds.value) return
        viewModelScope.launch {
            _processingIds.update { it + servicio.id }
            val resultado = op(servicio.id)
            _processingIds.update { it - servicio.id }
            resultado.onFailure { e ->
                _message.value = e.message ?: "No se pudo completar la operación"
            }
        }
    }
}
