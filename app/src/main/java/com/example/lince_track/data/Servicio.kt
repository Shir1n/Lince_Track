package com.example.lince_track.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    val horaFinEstimada: Boolean = false,
    /** Vehículo asignado; la API lo trae solo en TRANSFER y EXCURSION. */
    val vehiculo: VehiculoServicio? = null
)

data class VehiculoServicio(
    val patente: String,
    /** SEDAN, VAN, MINIBUS o BUS. */
    val tipo: String,
    val capacidad: Int
)

private val LOCALE_CHILE = Locale.forLanguageTag("es-CL")
private val horaFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_CHILE)
private val fechaFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_CHILE)
private val fechaCortaFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_CHILE)

/** Quién atiende el servicio: los guiados llevan solo guía; el resto, solo transportista. */
val Servicio.responsable: String
    get() = if (esGuiado) guia else conductor

val Servicio.esGuiado: Boolean
    get() = tipo == "GUIDED_SERVICE"

fun formatHora(hora: LocalTime): String = hora.format(horaFormatter)

fun formatFecha(fecha: LocalDate): String = fecha.format(fechaFormatter)

fun formatFechaCorta(fecha: LocalDate): String = fecha.format(fechaCortaFormatter)

/** Nombre del mes con mayúscula inicial, por ejemplo "Noviembre". */
fun formatNombreMes(mes: Month): String =
    mes.getDisplayName(TextStyle.FULL, LOCALE_CHILE).replaceFirstChar { it.titlecase(LOCALE_CHILE) }

fun formatPrecioCLP(monto: Int): String =
    "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()
