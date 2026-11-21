package com.example.lince_track.data

import java.time.LocalDate

/**
 * Reglas de negocio para el bloqueo de días (disponibilidad), alineadas con el backend:
 * - Un conductor o guía solo puede bloquear con la anticipación mínima configurada (7 días por defecto).
 * - No puede bloquear días donde tenga un servicio asignado (se excluyen los cancelados y reasignados).
 * - No puede bloquear un día que ya esté bloqueado.
 */
object ReglasBloqueo {

    /**
     * Calcula la primera fecha permitida para bloquear:
     * hoy + diasAnticipacion.
     * Ejemplo: si hoy es 20-11 y la anticipación es 7 días, el primer día permitido es el 27-11.
     */
    fun primerDiaBloqueable(hoy: LocalDate = LocalDate.now(), diasAnticipacion: Int = 7): LocalDate {
        return hoy.plusDays(diasAnticipacion.toLong())
    }

    /**
     * Valida si una fecha cumple con la anticipación mínima requerida.
     */
    fun tieneAnticipacion(
        fecha: LocalDate,
        hoy: LocalDate = LocalDate.now(),
        diasAnticipacion: Int = 7
    ): Boolean {
        return !fecha.isBefore(primerDiaBloqueable(hoy, diasAnticipacion))
    }

    /**
     * Comprueba si el usuario tiene un servicio asignado activo en esa fecha.
     * No cuentan los servicios cancelados (XX) ni los que se reasignaron a otra persona.
     */
    fun tieneServicioAsignado(servicios: List<Servicio>, fecha: LocalDate): Boolean {
        return servicios.any { s ->
            s.fecha == fecha && !s.cancelado && !s.reasignado
        }
    }

    /**
     * Comprueba si la fecha ya cuenta con un bloqueo activo.
     */
    fun estaBloqueado(fecha: LocalDate, bloqueos: List<Bloqueo>): Boolean {
        return bloqueos.any { it.fecha == fecha }
    }

    /**
     * Validación completa en el cliente antes de solicitar un bloqueo al servidor:
     * Requiere anticipación suficiente, sin servicios asignados y sin bloqueo previo.
     */
    fun esDiaBloqueable(
        fecha: LocalDate,
        servicios: List<Servicio>,
        bloqueos: List<Bloqueo>,
        hoy: LocalDate = LocalDate.now(),
        diasAnticipacion: Int = 7
    ): Boolean {
        if (!tieneAnticipacion(fecha, hoy, diasAnticipacion)) return false
        if (tieneServicioAsignado(servicios, fecha)) return false
        if (estaBloqueado(fecha, bloqueos)) return false
        return true
    }
}
