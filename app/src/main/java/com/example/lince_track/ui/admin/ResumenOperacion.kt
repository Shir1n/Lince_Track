package com.example.lince_track.ui.admin

import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.RespuestaServicio
import com.example.lince_track.data.Servicio
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

enum class PeriodoResumen(val etiqueta: String) {
    HOY("Hoy"),
    SIETE_DIAS("Últimos 7 días")
}

/**
 * Lo que se resume.
 *
 * @property respuestas todas las respuestas, no solo la vigente: el rechazo de quien fue
 *   reemplazado también cuenta.
 * @property reemplazos ids de los servicios reasignados.
 */
data class DatosResumen(
    val servicios: List<Servicio> = emptyList(),
    val respuestas: List<RespuestaServicio> = emptyList(),
    val checkins: Map<String, CheckinServicio> = emptyMap(),
    val incidencias: List<IncidenciaServicio> = emptyList(),
    val reemplazos: Set<String> = emptySet()
) {
    operator fun plus(otros: DatosResumen) = DatosResumen(
        servicios = servicios + otros.servicios,
        respuestas = respuestas + otros.respuestas,
        checkins = checkins + otros.checkins,
        incidencias = incidencias + otros.incidencias,
        reemplazos = reemplazos + otros.reemplazos
    )
}

data class CantidadResumen(val etiqueta: String, val cantidad: Int)

/**
 * Cifras de un periodo.
 *
 * @property sinCheckIn aceptados sin check-in cuyo margen de puntualidad ya pasó.
 * @property distanciaPromedioM null si no hay check-ins con GPS real.
 */
data class Resumen(
    val conCheckIn: Int = 0,
    val aTiempo: Int = 0,
    val sinCheckIn: Int = 0,
    val aceptados: Int = 0,
    val terminados: Int = 0,
    val rechazos: List<CantidadResumen> = emptyList(),
    val conGpsReal: Int = 0,
    val cercanos: Int = 0,
    val distanciaPromedioM: Int? = null,
    val incidenciasAbiertas: Int = 0,
    val incidenciasResueltas: Int = 0,
    val incidenciasPorTipo: List<CantidadResumen> = emptyList(),
    val reemplazos: Int = 0
) {
    val totalRechazos: Int get() = rechazos.sumOf { it.cantidad }
}

/** Cálculos del resumen para los dueños, sin dependencias de Android ni Firebase. */
object ResumenOperacion {

    const val MARGEN_PUNTUALIDAD_MIN = 15L
    const val DISTANCIA_CERCANA_M = 500

    /** Hoy, o los 7 días que terminan hoy. */
    fun rango(periodo: PeriodoResumen, hoy: LocalDate): ClosedRange<LocalDate> = when (periodo) {
        PeriodoResumen.HOY -> hoy..hoy
        PeriodoResumen.SIETE_DIAS -> hoy.minusDays(6)..hoy
    }

    /** Porcentaje redondeado; null si no hay sobre qué calcularlo. */
    fun porcentaje(parte: Int, total: Int): Int? =
        if (total == 0) null else (parte * 100.0 / total).roundToInt()

    fun calcular(
        datos: DatosResumen,
        rango: ClosedRange<LocalDate>,
        ahora: LocalDateTime,
        zona: ZoneId = ZoneId.systemDefault()
    ): Resumen {
        val servicios = datos.servicios.filter { it.fecha in rango }
        val aceptados = servicios.filter { it.estado == EstadoServicio.ACEPTADO }
        val conCheckIn = servicios.filter { datos.checkins[it.id] != null }
        val gpsReal = conCheckIn.mapNotNull { datos.checkins[it.id] }.filter { it.gpsReal }
        val incidencias = datos.incidencias.filter { fechaDe(it.creadaEn, zona) in rango }
        return Resumen(
            conCheckIn = conCheckIn.size,
            aTiempo = conCheckIn.count { aTiempo(it, datos.checkins.getValue(it.id), zona) },
            sinCheckIn = aceptados.count { datos.checkins[it.id] == null && limite(it).isBefore(ahora) },
            aceptados = aceptados.size,
            terminados = aceptados.count { ReglasCheckin.estaTerminado(it, datos.checkins[it.id]) },
            rechazos = rechazos(servicios, datos.respuestas),
            conGpsReal = gpsReal.size,
            cercanos = gpsReal.count { it.distanciaM <= DISTANCIA_CERCANA_M },
            distanciaPromedioM = gpsReal.takeIf { it.isNotEmpty() }?.map { it.distanciaM }?.average()?.roundToInt(),
            incidenciasAbiertas = incidencias.count { !it.resuelta },
            incidenciasResueltas = incidencias.count { it.resuelta },
            incidenciasPorTipo = ordenar(incidencias.map { it.tipo.etiqueta }),
            reemplazos = servicios.count { it.id in datos.reemplazos }
        )
    }

    /** Hora tope para llegar a tiempo. */
    private fun limite(servicio: Servicio): LocalDateTime =
        LocalDateTime.of(servicio.fecha, servicio.horaInicio).plusMinutes(MARGEN_PUNTUALIDAD_MIN)

    private fun aTiempo(servicio: Servicio, checkin: CheckinServicio, zona: ZoneId): Boolean =
        !Instant.ofEpochMilli(checkin.checkInEn).atZone(zona).toLocalDateTime().isAfter(limite(servicio))

    /** Lo rechazado sin respuesta de nadie viene de la API. */
    private fun rechazos(servicios: List<Servicio>, respuestas: List<RespuestaServicio>): List<CantidadResumen> {
        val rechazosPorServicio = respuestas
            .filter { it.estado == EstadoServicio.RECHAZADO }
            .groupBy { it.servicioId }
        val etiquetas = servicios.flatMap { servicio ->
            val propios = rechazosPorServicio[servicio.id].orEmpty()
            when {
                propios.isNotEmpty() -> propios.map { it.motivo?.etiqueta ?: PanelAdmin.MOTIVO_DESCONOCIDO }
                servicio.estado == EstadoServicio.RECHAZADO -> listOf(PanelAdmin.RECHAZADO_EN_API)
                else -> emptyList()
            }
        }
        return ordenar(etiquetas)
    }

    /** De mayor a menor cantidad; los empates, por nombre. */
    private fun ordenar(etiquetas: List<String>): List<CantidadResumen> =
        etiquetas.groupingBy { it }.eachCount()
            .map { (etiqueta, cantidad) -> CantidadResumen(etiqueta, cantidad) }
            .sortedWith(compareBy({ -it.cantidad }, { it.etiqueta }))

    private fun fechaDe(milisegundos: Long, zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(milisegundos).atZone(zona).toLocalDate()
}
