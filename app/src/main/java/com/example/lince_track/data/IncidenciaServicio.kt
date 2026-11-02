package com.example.lince_track.data

import java.time.Instant
import java.time.ZoneId

/** Tipos de incidencia. [codigo] es el valor que se guarda en Firestore. */
enum class TipoIncidencia(val codigo: String, val etiqueta: String, val requiereTexto: Boolean) {
    ATRASO("atraso", "Atraso", false),
    FALLA_MECANICA("falla_mecanica", "Falla mecánica", false),
    PASAJEROS("pasajeros", "Problema con pasajeros", false),
    CAMINO("camino", "Camino o clima", false),
    OTRO("otro", "Otro", true);

    companion object {
        fun desde(codigo: String?): TipoIncidencia? = entries.firstOrNull { it.codigo == codigo }
    }
}

/**
 * Problema reportado durante un servicio, guardado en `incidencias/{id}`.
 *
 * @property creadaEn hora en milisegundos (estimada hasta que llega al servidor).
 */
data class IncidenciaServicio(
    val id: String,
    val servicioId: String,
    val titulo: String,
    val uid: String,
    val nombre: String,
    val tipo: TipoIncidencia,
    val detalle: String,
    val creadaEn: Long,
    val resuelta: Boolean = false
)

object ReglasIncidencia {

    const val MAX_DETALLE = 500

    /** Un tipo que exige texto no se acepta con el detalle vacío. */
    fun detalleValido(tipo: TipoIncidencia?, detalle: String): Boolean =
        tipo != null && detalle.length <= MAX_DETALLE && (!tipo.requiereTexto || detalle.isNotBlank())

    /** Se reporta mientras el servicio está a cargo de la persona: aceptado y sin check-out. */
    fun puedeReportar(servicio: Servicio, checkin: CheckinServicio?): Boolean =
        servicio.estado == EstadoServicio.ACEPTADO && checkin?.finalizado != true

    /** Las que falta resolver, de la más reciente a la más antigua. */
    fun abiertas(incidencias: List<IncidenciaServicio>): List<IncidenciaServicio> =
        incidencias.filterNot { it.resuelta }.sortedByDescending { it.creadaEn }

    /** Las de un servicio, primero las abiertas y luego de la más reciente a la más antigua. */
    fun deServicio(incidencias: List<IncidenciaServicio>, servicioId: String): List<IncidenciaServicio> =
        incidencias
            .filter { it.servicioId == servicioId }
            .sortedWith(compareBy<IncidenciaServicio> { it.resuelta }.thenByDescending { it.creadaEn })

    /** Ejemplo: "Atraso • 10:32". */
    fun resumen(incidencia: IncidenciaServicio, zona: ZoneId = ZoneId.systemDefault()): String {
        val hora = formatHora(Instant.ofEpochMilli(incidencia.creadaEn).atZone(zona).toLocalTime())
        return "${incidencia.tipo.etiqueta} • $hora"
    }
}
