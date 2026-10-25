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

    suspend fun aceptar(id: String): Result<Unit> =
        mutar(id, EstadoServicio.ACEPTADO, from = setOf(EstadoServicio.PENDIENTE))

    suspend fun rechazar(id: String): Result<Unit> =
        mutar(id, EstadoServicio.RECHAZADO, from = setOf(EstadoServicio.PENDIENTE, EstadoServicio.ACEPTADO))

    private suspend fun mutar(
        id: String,
        nuevoEstado: EstadoServicio,
        from: Set<EstadoServicio>
    ): Result<Unit> {
        delay(500)
        val actual = _servicios.value.find { it.id == id }
            ?: return Result.failure(IllegalStateException("Asignación no encontrada"))
        if (actual.estado !in from) {
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
                nombresPasajeros = listOf(
                    "John Miller", "Sarah Miller", "Emily Miller", "Jake Thompson"
                ),
                idiomaTour = "Inglés",
                liderGrupo = "John Miller",
                fonoLider = "+1 415 555 0132",
                notasGrupo = "Vuelo LA-281 sale 11:45 AM. Equipaje: 4 maletas grandes.",
                conductor = "Carlos Morales",
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
                nombresPasajeros = listOf(
                    "Marcela Fuentes", "Jorge Fuentes", "Camila Rojas",
                    "Diego Rojas", "Valentina Paredes", "Tomás Paredes"
                ),
                idiomaTour = "Español",
                liderGrupo = "Marcela Fuentes",
                fonoLider = "+56 9 6112 4455",
                notasGrupo = "Dos niños menores de 8 años. Preferencia asientos delanteros.",
                conductor = "Carlos Morales",
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
                nombresPasajeros = listOf(
                    "Michael Brown", "Laura Brown", "Kevin Smith", "Anna Smith",
                    "Peter Jones", "Julia Jones", "Mark Wilson", "Emma Wilson"
                ),
                idiomaTour = "Inglés",
                liderGrupo = "Michael Brown",
                fonoLider = "+44 7700 900123",
                notasGrupo = "Llevar comprobantes CONAF. Un pasajero con movilidad reducida (bastón).",
                conductor = "Miguel Soto",
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
                nombresPasajeros = listOf(
                    "Carlos Souza", "Beatriz Souza"
                ),
                idiomaTour = "Portugués",
                liderGrupo = "Carlos Souza",
                fonoLider = "+55 11 91234 5678",
                notasGrupo = "Llegan en vuelo SKY-114. Esperar en puerta 2 de arribos.",
                conductor = "Jorge Valdivia",
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
                nombresPasajeros = listOf(
                    "Rodrigo González", "María González", "Josefa González", "Martín González",
                    "Luis Herrera", "Carmen Herrera", "Sofía Herrera",
                    "Andrés Mora", "Paula Mora", "Felipe Mora"
                ),
                idiomaTour = "Español",
                liderGrupo = "Rodrigo González",
                fonoLider = "+56 9 8334 2211",
                notasGrupo = "Grupo familiar. Almuerzo no incluido: sugerir restaurante en Ancud.",
                conductor = "Patricio Lagos",
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
                nombresPasajeros = listOf(
                    "Patricia Lagos", "Hernán Vega", "Rosa Vega"
                ),
                idiomaTour = "Español",
                liderGrupo = "Patricia Lagos",
                fonoLider = "+56 9 4556 7788",
                notasGrupo = "Sin observaciones.",
                conductor = "Jorge Valdivia",
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
                nombresPasajeros = listOf(
                    "David Clark", "Nancy Clark", "Robert Clark",
                    "Alice Johnson", "Tom Johnson"
                ),
                idiomaTour = "Inglés",
                liderGrupo = "David Clark",
                fonoLider = "+1 303 555 0177",
                notasGrupo = "Interés en fotografía de volcanes. Adulto mayor (78 años), ritmo lento.",
                conductor = "Miguel Soto",
                tarifa = 48000,
                estado = EstadoServicio.ACEPTADO,
                reserva = "#SB-2026-8605"
            )
        )
    }
}
