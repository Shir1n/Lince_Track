package com.example.lince_track.data.remote

import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.GeocercaDto
import com.example.lince_track.data.remote.dto.HitoDto
import com.example.lince_track.data.remote.dto.HitoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del backend propio de Lince: bloqueos de disponibilidad, parámetros, geocercas e hitos.
 */
interface BloqueoApi {

    @GET("api/bloqueos")
    suspend fun listar(): List<BloqueoDto>

    @POST("api/bloqueos")
    suspend fun crear(@Body solicitud: BloqueoSolicitudDto): BloqueoDto

    @DELETE("api/bloqueos/{id}")
    suspend fun eliminar(@Path("id") id: Long)

    @GET("api/parametros")
    suspend fun obtenerParametros(): ParametrosDto

    @GET("api/geocercas")
    suspend fun geocercas(): List<GeocercaDto>

    /** Check-in o check-out: responde 201, o 200 si la clave de idempotencia ya existía. */
    @POST("api/hitos")
    suspend fun registrarHito(@Body solicitud: HitoSolicitudDto): HitoDto
}
