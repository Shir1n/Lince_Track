package com.example.lince_track.data.remote.dto

import kotlinx.serialization.Serializable

/** Punto de pickup o dropoff del backend. El nombre coincide con el texto que trae la API de Southbound. */
@Serializable
data class GeocercaDto(
    val id: Long,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val radioMetros: Int,
    val activa: Boolean = true
)

/** Lo que la app envía al marcar. La hora no va: la pone el servidor. */
@Serializable
data class HitoSolicitudDto(
    val servicioId: Long,
    /** CHECK_IN, CHECK_OUT o POSICION. */
    val tipo: String,
    val lat: Double,
    val lon: Double,
    val precisionMetros: Double,
    /** UUID de la app: si se reenvía, el backend devuelve el mismo hito en vez de duplicarlo. */
    val claveIdempotencia: String
)

/** Hito guardado por el backend con su validación. */
@Serializable
data class HitoDto(
    val id: Long? = null,
    val servicioId: Long = 0,
    val tipo: String = "",
    /** VERIFICADO, FUERA_DE_ZONA, TARDE, GPS_IMPRECISO, SIN_GEOCERCA o REGISTRADO. */
    val resultado: String = "",
    val geocercaNombre: String? = null,
    val radioMetros: Int? = null,
    val distanciaMetros: Double? = null
)
