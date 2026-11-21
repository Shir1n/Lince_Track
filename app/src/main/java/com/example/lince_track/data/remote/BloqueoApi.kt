package com.example.lince_track.data.remote

import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del backend propio de Lince para bloqueo de disponibilidad y parámetros del sistema.
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
}
