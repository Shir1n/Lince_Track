package com.example.lince_track.data

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Inicio y fin de un servicio, guardados en `checkins/{uid}_{servicioId}`.
 *
 * @property checkInEn / checkOutEn hora en milisegundos (estimada hasta que llega al servidor).
 * @property distanciaM distancia al punto de inicio al hacer el check-in. No se bloquea por
 *   distancia ni por hora: se registra para que Coordinación la vea.
 * @property gpsReal false si se usó la posición simulada (solo en compilaciones de desarrollo).
 */
data class CheckinServicio(
    val servicioId: String,
    val checkInEn: Long,
    val distanciaM: Int,
    val gpsReal: Boolean,
    val checkOutEn: Long? = null
) {
    val finalizado: Boolean get() = checkOutEn != null
}

object ReglasCheckin {

    private val LOCALE_CHILE = Locale.forLanguageTag("es-CL")

    /** Ejemplos: "850 m", "2,5 km", "980 km". */
    fun formatoDistancia(metros: Int): String = when {
        metros < 1_000 -> "$metros m"
        metros < 10_000 -> String.format(LOCALE_CHILE, "%.1f km", metros / 1_000.0)
        else -> "${(metros / 1_000.0).roundToInt()} km"
    }

    /** Texto del estado para Coordinación: "Check-in 10:32 • a 120 m" o "Finalizado 12:40". */
    fun resumen(checkin: CheckinServicio, zona: ZoneId = ZoneId.systemDefault()): String {
        val base = checkin.checkOutEn?.let { "Finalizado ${hora(it, zona)}" }
            ?: "Check-in ${hora(checkin.checkInEn, zona)} • a ${formatoDistancia(checkin.distanciaM)}"
        return if (checkin.gpsReal) base else "$base (GPS simulado)"
    }

    /** true si ya se registró el check-out, salvo que la API lo haya cancelado después. */
    fun estaTerminado(servicio: Servicio, checkin: CheckinServicio?): Boolean =
        checkin?.finalizado == true && servicio.estado == EstadoServicio.ACEPTADO

    /** Los que aún no terminan: lo que muestra la lista de trabajo del día. */
    fun porRealizar(servicios: List<Servicio>, checkins: Map<String, CheckinServicio>): List<Servicio> =
        servicios.filterNot { estaTerminado(it, checkins[it.id]) }

    /** Del más reciente al más antiguo, según la hora del check-out. */
    fun terminados(servicios: List<Servicio>, checkins: Map<String, CheckinServicio>): List<Servicio> =
        servicios
            .filter { estaTerminado(it, checkins[it.id]) }
            .sortedByDescending { checkins[it.id]?.checkOutEn }

    /** Hora real de inicio y de término: "10:32 a 12:40". */
    fun horario(checkin: CheckinServicio, zona: ZoneId = ZoneId.systemDefault()): String {
        val inicio = hora(checkin.checkInEn, zona)
        return checkin.checkOutEn?.let { "$inicio a ${hora(it, zona)}" } ?: inicio
    }

    private fun hora(milisegundos: Long, zona: ZoneId): String =
        formatHora(Instant.ofEpochMilli(milisegundos).atZone(zona).toLocalTime())
}
