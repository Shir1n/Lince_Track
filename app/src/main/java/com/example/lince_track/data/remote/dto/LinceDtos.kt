package com.example.lince_track.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Modelos del JSON de la API de Southbound (Lince API académica, OpenAPI 1.0.0).
 * Documentación: https://ws.southbound.travel/lince-api/api-docs/
 *
 * Los códigos (tipo, estados, statusPL...) se guardan como String a propósito:
 * si la API agrega un valor nuevo, la app no se cae al leerlo.
 * Los valores por defecto permiten leer respuestas aunque falte algún campo.
 */

/** Configuración JSON compartida por el cliente y los tests. */
val LinceJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// ---------- Envoltorios de respuesta ----------

@Serializable
data class PaginaDto<T>(
    val data: List<T> = emptyList(),
    val meta: PaginacionMetaDto = PaginacionMetaDto()
)

@Serializable
data class RespuestaDto<T>(
    val data: T
)

@Serializable
data class PaginacionMetaDto(
    val page: Int = 1,
    val pageSize: Int = 0,
    val total: Int = 0,
    val totalPages: Int = 0
)

// ---------- Servicios ----------

@Serializable
data class ServicioDto(
    val id: Long,
    val codigo: String = "",
    /** Columna Srv: TR, GS o EX. */
    val srv: String = "",
    /** TRANSFER, GUIDED_SERVICE o EXCURSION. */
    val tipo: String = "",
    /** SrvDate, formato YYYY-MM-DD. */
    val fecha: String,
    /** PUTime, formato HH:mm (hora local de Chile). */
    val horaPickup: String = "",
    val descripcion: String = "",
    /** Tour al que pertenece el servicio. */
    val descripcionPadre: String? = null,
    /** SrvStatus: CF, RQ, WL o XX. */
    val estadoServicio: String = "",
    val reserva: ReservaDto = ReservaDto(),
    val pax: PaxDto = PaxDto(),
    val pickup: String = "",
    val dropoff: String = "",
    val notas: String? = null,
    val oficina: OficinaDto? = null,
    val cliente: ClienteRefDto? = null,
    /** Solo los GUIDED_SERVICE exigen idioma. */
    val idiomaRequerido: IdiomaDto? = null,
    /** Solo GUIDED_SERVICE; null si no corresponde o aún no hay. */
    val guia: PersonaDto? = null,
    /** Solo TRANSFER y EXCURSION; null si no corresponde o aún no hay. */
    val transportista: PersonaDto? = null,
    val vehiculo: VehiculoDto? = null,
    val asignacion: AsignacionDto = AsignacionDto()
)

@Serializable
data class ReservaDto(
    /** Reference, número interno de la reserva. */
    val referencia: String = "",
    /** BkgName, pasajero principal y cantidad de pax. */
    val nombre: String = "",
    /** BkgStatus: CF, RQ, WL o XX. */
    val estado: String = ""
)

@Serializable
data class PaxDto(
    val adultos: Int = 0,
    val ninos: Int = 0,
    val total: Int = 0,
    /** Formato de la planilla: "adultos+niños". */
    val texto: String = ""
)

@Serializable
data class AsignacionDto(
    /** CF, RQ, DL, CX o null si aún no hay staff asignado. */
    val statusPL: String? = null,
    /** ISO 8601 sin zona (hora local de Chile). */
    val actualizadoEn: String? = null
)

@Serializable
data class PersonaDto(
    val id: Long,
    val codigo: String = "",
    val nombre: String = ""
)

@Serializable
data class OficinaDto(
    /** SCL, PUX o PUQ. */
    val codigo: String = "",
    val nombre: String = ""
)

@Serializable
data class IdiomaDto(
    /** ES, EN, PT, DE o FR. */
    val codigo: String = "",
    val nombre: String = ""
)

@Serializable
data class ClienteRefDto(
    val id: Long,
    val codigo: String = "",
    val nombre: String = "",
    val pais: String = ""
)

@Serializable
data class VehiculoDto(
    val id: Long,
    val patente: String = "",
    /** SEDAN, VAN, MINIBUS o BUS. */
    val tipo: String = "",
    val capacidad: Int = 0
)

// ---------- Guías y transportistas ----------

@Serializable
data class GuiaDto(
    val id: Long,
    val codigo: String = "",
    val nombre: String = "",
    val oficina: OficinaDto? = null,
    val idiomas: List<IdiomaDto> = emptyList(),
    val evaluacionPromedio: Double = 0.0,
    val fechaIngreso: String = "",
    val activo: Boolean = true,
    /** Solo viene en GET /guias/{id}. */
    val totalServicios: Int? = null
)

@Serializable
data class TransportistaDto(
    val id: Long,
    val codigo: String = "",
    val nombre: String = "",
    val oficina: OficinaDto? = null,
    val vehiculos: List<VehiculoDto> = emptyList(),
    val evaluacionPromedio: Double = 0.0,
    val fechaIngreso: String = "",
    val activo: Boolean = true,
    /** Solo viene en GET /transportistas/{id}. */
    val totalServicios: Int? = null
)

// ---------- Sistema y errores ----------

@Serializable
data class HealthDto(
    /** ok o degraded. */
    val status: String = "",
    /** up o down. */
    val db: String = "",
    val version: String = "",
    val time: String = ""
)

@Serializable
data class ErrorRespuestaDto(
    val error: ErrorCuerpoDto = ErrorCuerpoDto()
)

@Serializable
data class ErrorCuerpoDto(
    /** VALIDATION_ERROR, UNAUTHORIZED, KEY_EXPIRED, NOT_FOUND, RATE_LIMITED, DB_UNAVAILABLE o INTERNAL_ERROR. */
    val code: String = "",
    val message: String = "",
    val details: List<ErrorDetalleDto> = emptyList(),
    val requestId: String = ""
)

@Serializable
data class ErrorDetalleDto(
    val field: String = "",
    val message: String = ""
)
