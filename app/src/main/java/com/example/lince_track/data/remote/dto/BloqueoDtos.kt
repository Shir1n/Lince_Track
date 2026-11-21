package com.example.lince_track.data.remote.dto

import com.example.lince_track.data.Bloqueo
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BloqueoDto(
    val id: Long,
    val uid: String = "",
    val fecha: String,
    val motivo: String? = null,
    val creadoEn: String? = null
)

@Serializable
data class BloqueoSolicitudDto(
    val fecha: String,
    val motivo: String? = null
)

@Serializable
data class ParametrosDto(
    val id: Long = 1L,
    val anticipacionMinutos: Int = 60,
    val precisionMaximaMetros: Int = 100,
    val radioPorDefectoMetros: Int = 200,
    val diasAnticipacionBloqueo: Int = 7,
    val actualizadoEn: String? = null,
    val actualizadoPor: String? = null
)

/** Convierte el DTO recibido del backend al modelo de dominio. */
fun BloqueoDto.aModelo(): Bloqueo = Bloqueo(
    id = id,
    uid = uid,
    fecha = LocalDate.parse(fecha),
    motivo = motivo,
    creadoEn = creadoEn
)
