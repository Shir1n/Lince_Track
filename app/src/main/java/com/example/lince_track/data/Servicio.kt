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
    val vehiculo: VehiculoServicio? = null,
    /** true si la API lo marca cancelado (XX): ya no se puede reasignar. */
    val cancelado: Boolean = false,
    /** Id en la API de quien trae asignado: transportista o guía según el tipo. */
    val responsableId: Long? = null,
    /** true si el administrador eligió a otra persona (ver ReglasAsignacion). */
    val reasignado: Boolean = false,
    /** Teléfono que publicó quien aceptó este servicio (vacío si no hay). */
    val telefonoResponsable: String = "",
    /** Teléfono de la otra persona del tour, una vez que aceptó (vacío si no hay). */
    val telefonoCompanero: String = "",
    /** Agencia (cliente de Southbound) que vendió el servicio; vacío si la API no la trae. */
    val agencia: String = "",
    val agenciaPais: String = "",
    val agenciaId: Long? = null
)

data class VehiculoServicio(
    val patente: String,
    /** SEDAN, VAN, MINIBUS o BUS. */
    val tipo: String,
    val capacidad: Int
)

private val LOCALE_CHILE = Locale.forLanguageTag("es-CL")
private val horaFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_CHILE)
private val fechaFormatterEs = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_CHILE)
private val fechaCortaFormatterEs = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_CHILE)
private val fechaFormatterEn = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
private val fechaCortaFormatterEn = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

/** Quién atiende el servicio: los guiados llevan solo guía; el resto, solo transportista. */
val Servicio.responsable: String
    get() = if (esGuiado) guia else conductor

val Servicio.esGuiado: Boolean
    get() = tipo == "GUIDED_SERVICE"

fun formatHora(hora: LocalTime): String = hora.format(horaFormatter)

fun formatFecha(fecha: LocalDate, locale: Locale = LOCALE_CHILE): String =
    fecha.format(if (locale.language == "en") fechaFormatterEn else fechaFormatterEs)

fun formatFechaCorta(fecha: LocalDate, locale: Locale = LOCALE_CHILE): String =
    fecha.format(if (locale.language == "en") fechaCortaFormatterEn else fechaCortaFormatterEs)

/** Nombre del mes con mayúscula inicial, por ejemplo "Noviembre" o "November". */
fun formatNombreMes(mes: Month, locale: Locale = LOCALE_CHILE): String =
    mes.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }

fun formatPrecioCLP(monto: Int): String =
    "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()
