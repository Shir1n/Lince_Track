package com.example.lince_track.data.remote

import com.example.lince_track.data.remote.dto.GuiaDto
import com.example.lince_track.data.remote.dto.HealthDto
import com.example.lince_track.data.remote.dto.PaginaDto
import com.example.lince_track.data.remote.dto.RespuestaDto
import com.example.lince_track.data.remote.dto.ServicioDto
import com.example.lince_track.data.remote.dto.TransportistaDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de la API de Southbound (solo lectura).
 * Los parámetros null no se envían: la API rechaza con 400 los parámetros vacíos o desconocidos.
 */
interface LinceApi {

    /** No requiere API key. Responde 503 si la base de datos no está disponible. */
    @GET("health")
    suspend fun health(): HealthDto

    @GET("api/v1/servicios")
    suspend fun servicios(
        @Query("fechaDesde") fechaDesde: String? = null,
        @Query("fechaHasta") fechaHasta: String? = null,
        @Query("oficina") oficina: String? = null,
        @Query("tipo") tipo: String? = null,
        @Query("estadoServicio") estadoServicio: String? = null,
        @Query("statusPL") statusPL: String? = null,
        @Query("guiaId") guiaId: Long? = null,
        @Query("transportistaId") transportistaId: Long? = null,
        @Query("orden") orden: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null
    ): PaginaDto<ServicioDto>

    @GET("api/v1/servicios/{id}")
    suspend fun servicio(@Path("id") id: Long): RespuestaDto<ServicioDto>

    @GET("api/v1/guias/{id}")
    suspend fun guia(@Path("id") id: Long): RespuestaDto<GuiaDto>

    @GET("api/v1/transportistas/{id}")
    suspend fun transportista(@Path("id") id: Long): RespuestaDto<TransportistaDto>

    companion object {
        /** Máximo permitido por la API para pageSize. */
        const val PAGE_SIZE_MAX = 100
    }
}
