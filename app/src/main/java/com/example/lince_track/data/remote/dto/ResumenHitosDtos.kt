package com.example.lince_track.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Respuesta de `GET /api/hitos/resumen` del backend propio: el total y una fila por persona, la menos puntual primero
 * (ver "Resumen por recurso" en backend/README.md).
 */
@Serializable
data class ResumenHitosDto(
    val desde: String? = null,
    val hasta: String? = null,
    val total: ResumenRecursoDto = ResumenRecursoDto(),
    val recursos: List<ResumenRecursoDto> = emptyList()
)

/**
 * Cómo le fue a una persona (o al total) con sus check-in. Se cuenta el último check-in de cada servicio.
 *
 * @property uid cuenta de Firebase; null en el total (el nombre lo pone la app desde `usuarios/{uid}`).
 * @property rol "conductor" o "guia"; null en el total.
 * @property puntualidad porcentaje a tiempo (0 a 100); null si no hay servicios.
 * @property minutosAnticipacionPromedio minutos antes del pickup en promedio (negativo = después).
 */
@Serializable
data class ResumenRecursoDto(
    val uid: String? = null,
    val rol: String? = null,
    val servicios: Int = 0,
    val aTiempo: Int = 0,
    val tarde: Int = 0,
    val fueraDeZona: Int = 0,
    val gpsImpreciso: Int = 0,
    val sinGeocerca: Int = 0,
    val intentosCheckIn: Int = 0,
    val conCheckOut: Int = 0,
    val puntualidad: Double? = null,
    val minutosAnticipacionPromedio: Double? = null,
    val ultimoHito: String? = null
)
