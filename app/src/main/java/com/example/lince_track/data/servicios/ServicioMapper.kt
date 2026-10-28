package com.example.lince_track.data.servicios

import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.remote.dto.ServicioDto
import java.time.LocalDate
import java.time.LocalTime

/**
 * Convierte un servicio de la API (ServicioDto) al modelo que usan las pantallas (Servicio).
 *
 * La API no trae todo lo que muestra la app hoy; estos campos se completan así:
 * - horaFin: la API solo trae horaPickup; se estima según el tipo de servicio.
 * - tarifa: no viene en la API; queda en 0.
 * - lista de pasajeros / teléfono del líder: solo viene reserva.nombre (pasajero principal).
 */
object ServicioMapper {

    const val SIN_GUIA = "Sin guía"
    const val SIN_TRANSPORTISTA = "Sin transportista"

    /** Duración estimada en minutos, usada para calcular horaFin. */
    fun duracionEstimadaMin(tipo: String): Long = when (tipo) {
        "TRANSFER" -> 60
        "GUIDED_SERVICE" -> 180
        "EXCURSION" -> 240
        else -> 60
    }

    /**
     * statusPL (asignación del staff) → estado de la app.
     * CF = confirmado → ACEPTADO; DL/CX o servicio cancelado (XX) → RECHAZADO;
     * RQ o sin staff (null) → PENDIENTE.
     */
    fun estadoDesde(statusPL: String?, estadoServicio: String = ""): EstadoServicio = when {
        estadoServicio == "XX" -> EstadoServicio.RECHAZADO
        statusPL == "CF" -> EstadoServicio.ACEPTADO
        statusPL == "DL" || statusPL == "CX" -> EstadoServicio.RECHAZADO
        else -> EstadoServicio.PENDIENTE
    }

    /** Devuelve null si la fecha no es válida (el servicio se descarta). */
    fun aDominio(dto: ServicioDto): Servicio? {
        val fecha = runCatching { LocalDate.parse(dto.fecha) }.getOrNull() ?: return null
        val horaInicio = runCatching { LocalTime.parse(dto.horaPickup) }.getOrDefault(LocalTime.MIDNIGHT)
        val horaFin = horaInicio.plusMinutes(duracionEstimadaMin(dto.tipo))

        val referencia = dto.reserva.referencia.ifBlank { dto.codigo }
        val titulo = dto.descripcion.ifBlank { dto.descripcionPadre ?: dto.codigo }

        return Servicio(
            id = dto.id.toString(),
            titulo = titulo,
            origen = dto.pickup,
            destino = dto.dropoff,
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            pasajeros = dto.pax.total,
            nombresPasajeros = listOf(dto.reserva.nombre).filter { it.isNotBlank() },
            idiomaTour = dto.idiomaRequerido?.nombre ?: "No especificado",
            liderGrupo = dto.reserva.nombre,
            fonoLider = "",
            notasGrupo = dto.notas.orEmpty(),
            guia = dto.guia?.nombre ?: SIN_GUIA,
            conductor = dto.transportista?.nombre ?: SIN_TRANSPORTISTA,
            tarifa = 0,
            estado = estadoDesde(dto.asignacion.statusPL, dto.estadoServicio),
            reserva = "#$referencia",
            tipo = dto.tipo,
            statusPL = dto.asignacion.statusPL,
            horaFinEstimada = true
        )
    }
}
