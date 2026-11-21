package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import com.example.lince_track.data.remote.dto.aModelo
import java.time.LocalDate

/**
 * Repositorio para la gestión de bloqueos de disponibilidad y consulta de parámetros.
 * Recibe la API mediante el constructor para facilitar pruebas unitarias con MockK.
 */
class BloqueoRepository(
    private val api: BloqueoApi = BloqueoApiClient.api
) {

    /** Obtiene los bloqueos vigentes (los del usuario o todos si es admin). */
    suspend fun listar(): Result<List<Bloqueo>> = runCatching {
        api.listar().map { it.aModelo() }
    }.recoverCatching { throw it.aBloqueoApiException() }

    /** Bloquea un día determinado con motivo opcional. */
    suspend fun crear(fecha: LocalDate, motivo: String? = null): Result<Bloqueo> = runCatching {
        val solicitud = BloqueoSolicitudDto(
            fecha = fecha.toString(),
            motivo = motivo?.trim()?.ifBlank { null }
        )
        api.crear(solicitud).aModelo()
    }.recoverCatching { throw it.aBloqueoApiException() }

    /** Elimina un bloqueo existente por su identificador. */
    suspend fun eliminar(id: Long): Result<Unit> = runCatching {
        api.eliminar(id)
    }.recoverCatching { throw it.aBloqueoApiException() }

    /** Consulta los parámetros configurados en el sistema (ej. días de anticipación). */
    suspend fun obtenerParametros(): Result<ParametrosDto> = runCatching {
        api.obtenerParametros()
    }.recoverCatching { throw it.aBloqueoApiException() }
}
