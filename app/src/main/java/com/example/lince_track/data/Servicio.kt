package com.example.lince_track.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class EstadoServicio {
    PENDIENTE,
    ACEPTADO,
    RECHAZADO
}

data class Servicio(
    val id: String,
    val titulo: String,
    val origen: String,
    val destino: String,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val pasajeros: Int,
    val nombresPasajeros: List<String>,
    val idiomaTour: String,
    val liderGrupo: String,
    val fonoLider: String,
    val notasGrupo: String,
    val guia: String,
    val conductor: String,
    val tarifa: Int,
    val estado: EstadoServicio,
    val reserva: String,
    /** TRANSFER, GUIDED_SERVICE o EXCURSION (vacío en datos antiguos). */
    val tipo: String = "",
    /** StatusPL de la API: CF, RQ, DL, CX o null si no hay staff asignado. */
    val statusPL: String? = null,
    /** true si horaFin fue estimada (la API solo entrega la hora de pickup). */
    val horaFinEstimada: Boolean = false
)

private val horaFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
private val fechaFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES"))
private val fechaCortaFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))

fun formatHora(hora: LocalTime): String = hora.format(horaFormatter)

fun formatFecha(fecha: LocalDate): String = fecha.format(fechaFormatter)

fun formatFechaCorta(fecha: LocalDate): String = fecha.format(fechaCortaFormatter)

fun formatPrecioCLP(monto: Int): String =
    "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()
