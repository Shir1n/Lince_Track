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

data class Ubicacion(val lat: Double, val lon: Double) {
    /** Enlace que abre este punto en la app de mapas. */
    val enlaceMapa: String get() = "geo:$lat,$lon?q=$lat,$lon"

    companion object {
        /** Null si falta alguna de las dos coordenadas. */
        fun desde(lat: Double?, lon: Double?): Ubicacion? =
            if (lat != null && lon != null) Ubicacion(lat, lon) else null
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
    val resuelta: Boolean = false,
    /** true desde que Coordinación respondió, hasta que la marca como resuelta. */
    val enGestion: Boolean = false,
    /** Lo último que Coordinación le dijo a quien reportó (vacío si aún no responde). */
    val respuesta: String = "",
    /** Dónde estaba quien reportó (null si no se pudo obtener). */
    val ubicacion: Ubicacion? = null
) {
    val estadoTexto: String
        get() = when {
            resuelta -> "Resuelta"
            enGestion -> "En gestión"
            else -> "Abierta"
        }
}

object ReglasIncidencia {

    const val MAX_DETALLE = 500

    /** Un tipo que exige texto no se acepta con el detalle vacío. */
    fun detalleValido(tipo: TipoIncidencia?, detalle: String): Boolean =
        tipo != null && detalle.length <= MAX_DETALLE && (!tipo.requiereTexto || detalle.isNotBlank())

    /** La respuesta de Coordinación es obligatoria al responder y opcional al resolver. */
    fun respuestaValida(texto: String, obligatoria: Boolean): Boolean =
        texto.length <= MAX_DETALLE && (!obligatoria || texto.isNotBlank())

    /** Las ya cerradas, de la más reciente a la más antigua. */
    fun resueltas(incidencias: List<IncidenciaServicio>): List<IncidenciaServicio> =
        incidencias.filter { it.resuelta }.sortedByDescending { it.creadaEn }

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
