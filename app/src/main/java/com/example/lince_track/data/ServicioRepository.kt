package com.example.lince_track.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

/**
 * Fuente de datos única de asignaciones/servicios.
 *
 * Hoy es un repositorio en memoria porque el proyecto aún no cuenta con backend.
 * Es el punto de integración para conectar una API real: basta reemplazar esta
 * implementación por un repositorio que consulte los endpoints, sin tocar la UI.
 */
object ServicioRepository {

    private val _servicios = MutableStateFlow(seedServicios())
    val servicios: StateFlow<List<Servicio>> = _servicios.asStateFlow()

    fun buscarPorId(id: String): Servicio? = _servicios.value.find { it.id == id }

    suspend fun aceptar(id: String): Result<Unit> = mutar(id, EstadoServicio.ACEPTADO)

    suspend fun rechazar(id: String): Result<Unit> = mutar(id, EstadoServicio.RECHAZADO)

    private suspend fun mutar(id: String, nuevoEstado: EstadoServicio): Result<Unit> {
        delay(500)
        val actual = _servicios.value.find { it.id == id }
            ?: return Result.failure(IllegalStateException("Asignación no encontrada"))
        if (actual.estado != EstadoServicio.PENDIENTE) {
            return Result.failure(IllegalStateException("Esta asignación ya fue respondida"))
        }
        _servicios.update { lista ->
            lista.map { if (it.id == id) it.copy(estado = nuevoEstado) else it }
        }
        return Result.success(Unit)
    }

    private fun seedServicios(): List<Servicio> {
        val hoy = LocalDate.now()
        return listOf(
            Servicio(
                id = "SB-2026-8890",
                titulo = "Transfer Hotel Cumbres → Aeropuerto Tepual",
                origen = "Hotel Cumbres, Puerto Varas",
                destino = "Aeropuerto El Tepual",
                fecha = hoy,
                horaInicio = LocalTime.of(8, 30),
                horaFin = LocalTime.of(9, 30),
                pasajeros = 4,
                guia = "Andrea Soto",
                tarifa = 28000,
                estado = EstadoServicio.ACEPTADO,
                reserva = "#SB-2026-8890"
            ),
            Servicio(
                id = "SB-2026-8901",
                titulo = "City Tour Puerto Varas",
                origen = "Hotel Cumbres, Puerto Varas",
                destino = "Centro de Puerto Varas",
                fecha = hoy,
                horaInicio = LocalTime.of(9, 0),
                horaFin = LocalTime.of(10, 30),
                pasajeros = 6,
                guia = "Andrea Soto",
                tarifa = 35000,
                estado = EstadoServicio.ACEPTADO,
                reserva = "#SB-2026-8901"
            ),
            Servicio(
                id = "SB-2026-8941",
                titulo = "Excursión Saltos del Petrohué",
                origen = "Hotel Cumbres, Puerto Varas",
                destino = "Parque Nacional Vicente Pérez Rosales",
                fecha = hoy,
                horaInicio = LocalTime.of(13, 30),
                horaFin = LocalTime.of(18, 30),
                pasajeros = 8,
                guia = "Roberto Díaz",
                tarifa = 40000,
                estado = EstadoServicio.PENDIENTE,
                reserva = "#SB-2026-8941"
            ),
            Servicio(
                id = "SB-2026-9020",
                titulo = "Transfer Aeropuerto → Puerto Montt",
                origen = "Aeropuerto El Tepual",
                destino = "Puerto Montt",
                fecha = hoy.plusDays(2),
                horaInicio = LocalTime.of(15, 0),
                horaFin = LocalTime.of(16, 0),
                pasajeros = 2,
                guia = "Roberto Díaz",
                tarifa = 22000,
                estado = EstadoServicio.PENDIENTE,
                reserva = "#SB-2026-9020"
            ),
            Servicio(
                id = "SB-2026-8950",
                titulo = "Excursión Isla de Chiloé",
                origen = "Puerto Montt",
                destino = "Ancud, Chiloé",
                fecha = hoy.plusDays(5),
                horaInicio = LocalTime.of(9, 0),
                horaFin = LocalTime.of(13, 0),
                pasajeros = 10,
                guia = "Andrea Soto",
                tarifa = 65000,
                estado = EstadoServicio.ACEPTADO,
                reserva = "#SB-2026-8950"
            ),
            Servicio(
                id = "SB-2026-8702",
                titulo = "Transfer Frutillar → Puerto Varas",
                origen = "Frutillar",
                destino = "Puerto Varas",
                fecha = hoy.minusDays(1),
                horaInicio = LocalTime.of(12, 0),
                horaFin = LocalTime.of(13, 0),
                pasajeros = 3,
                guia = "Roberto Díaz",
                tarifa = 20000,
                estado = EstadoServicio.RECHAZADO,
                reserva = "#SB-2026-8702"
            ),
            Servicio(
                id = "SB-2026-8605",
                titulo = "Excursión Volcán Osorno",
                origen = "Puerto Varas",
                destino = "Volcán Osorno",
                fecha = hoy.minusDays(3),
                horaInicio = LocalTime.of(8, 0),
                horaFin = LocalTime.of(14, 0),
                pasajeros = 5,
                guia = "Andrea Soto",
                tarifa = 48000,
                estado = EstadoServicio.ACEPTADO,
                reserva = "#SB-2026-8605"
            )
        )
    }
}
