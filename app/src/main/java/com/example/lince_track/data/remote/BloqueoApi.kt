package com.example.lince_track.data.remote

import com.example.lince_track.data.remote.dto.BloqueoDto
import com.example.lince_track.data.remote.dto.BloqueoSolicitudDto
import com.example.lince_track.data.remote.dto.GeocercaDto
import com.example.lince_track.data.remote.dto.HitoDto
import com.example.lince_track.data.remote.dto.HitoSolicitudDto
import com.example.lince_track.data.remote.dto.ParametrosDto
import com.example.lince_track.data.remote.dto.ResumenHitosDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    /** Último hito informado en el servicio (solo administrador); 404 si nadie ha informado. */
    @GET("api/hitos/ultima-posicion")
    suspend fun ultimaPosicion(@Query("servicioId") servicioId: Long): HitoDto

    /** Lo que el backend guarda de la persona del token (Ley 21.719: acceso y portabilidad). */
    @GET("api/privacidad/mis-datos")
    suspend fun misDatos(): JsonElement

    /** Solo el administrador, al aprobar una solicitud de eliminación. */
    @DELETE("api/privacidad/{uid}")
    suspend fun borrarDatos(@Path("uid") uid: String): JsonElement

    /**
     * Puntualidad por persona (solo administrador). Fechas AAAA-MM-DD, ambas incluidas; sin fechas, los últimos 30 días.
     */
    @GET("api/hitos/resumen")
    suspend fun resumenHitos(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null
    ): ResumenHitosDto
}
