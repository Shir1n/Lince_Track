package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.data.remote.dto.HitoSolicitudDto
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.UUID

/** Última posición informada en un servicio, para Coordinación. */
data class PosicionServicio(
    val lat: Double,
    val lon: Double,
    val precisionMetros: Int,
    /** Hora del servidor; null si no se pudo leer. */
    val registradaEn: Instant?
)

/**
 * Cuándo se envía la posición: solo con el servicio en curso (check-in hecho y sin check-out), con GPS real y cada
 * [INTERVALO_MS]. Sin rastreo 24/7: fuera de eso no se envía nada.
 */
object ReglasPosicion {

    const val INTERVALO_MS = 5 * 60 * 1000L

    fun debeEnviar(enCurso: Boolean, gpsReal: Boolean, ultimoEnvioMs: Long?, ahoraMs: Long): Boolean =
        enCurso && gpsReal && (ultimoEnvioMs == null || ahoraMs - ultimoEnvioMs >= INTERVALO_MS)

    /** Minutos desde que se registró la posición (nunca negativo). */
    fun minutosDesde(registradaEn: Instant, ahora: Instant): Long =
        ((ahora.toEpochMilli() - registradaEn.toEpochMilli()) / 60_000L).coerceAtLeast(0)
}

/**
 * Posición puntual del conductor o guía durante un servicio en curso (hitos de tipo POSICION del backend) y la
 * última informada, que solo ve Coordinación. Recibe la API por el constructor para probarlo con MockK.
 */
class PosicionRepository(
    private val api: BloqueoApi = BloqueoApiClient.api
) {

    /** El backend exige un check-in previo; si no hay conexión, esta posición simplemente se omite. */
    suspend fun enviar(servicioId: String, lat: Double, lon: Double, precisionMetros: Float): Result<Unit> = try {
        val id = servicioId.toLong()
        api.registrarHito(
            HitoSolicitudDto(
                servicioId = id,
                tipo = "POSICION",
                lat = lat,
                lon = lon,
                precisionMetros = precisionMetros.toDouble().coerceAtLeast(0.0),
                claveIdempotencia = UUID.randomUUID().toString()
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.aBloqueoApiException())
    }

    /** Última posición (o check-in/check-out) del servicio; null si nadie ha informado una. */
    suspend fun ultima(servicioId: String): Result<PosicionServicio?> = try {
        val hito = api.ultimaPosicion(servicioId.toLong())
        val lat = hito.lat
        val lon = hito.lon
        Result.success(
            if (lat == null || lon == null) null
            else PosicionServicio(
                lat = lat,
                lon = lon,
                precisionMetros = Math.round(hito.precisionMetros ?: 0.0).toInt(),
                registradaEn = hito.registradoEn?.let { runCatching { Instant.parse(it) }.getOrNull() }
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = e.aBloqueoApiException()
        if (error.codigoHttp == 404) Result.success(null) else Result.failure(error)
    }
}
