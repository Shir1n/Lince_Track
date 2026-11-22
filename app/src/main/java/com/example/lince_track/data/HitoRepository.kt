package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.data.remote.dto.HitoSolicitudDto

/** Lo que respondió el backend al registrar un hito. */
data class HitoRegistrado(
    /** null si el backend devolvió algo que la app no conoce. */
    val resultado: ResultadoCheckin?,
    val distanciaMetros: Int?,
    val radioMetros: Int?
)

/**
 * Geocercas y hitos (check-in y check-out) en el backend propio, que los valida otra vez en el servidor.
 * Recibe la API por el constructor para probarlo con MockK.
 */
class HitoRepository(
    private val api: BloqueoApi = BloqueoApiClient.api
) {
    /** Geocercas activas. */
    suspend fun geocercas(): Result<List<Geocerca>> = runCatching {
        api.geocercas()
            .filter { it.activa }
            .map { Geocerca(it.id, it.nombre, it.lat, it.lon, it.radioMetros) }
    }.recoverCatching { throw it.aBloqueoApiException() }

    /** Anticipación y precisión máxima que coordinación configuró. */
    suspend fun parametros(): Result<ParametrosCheckin> = runCatching {
        val p = api.obtenerParametros()
        ParametrosCheckin(p.anticipacionMinutos, p.precisionMaximaMetros)
    }.recoverCatching { throw it.aBloqueoApiException() }

    /**
     * Registra un check-in ([checkIn] = true) o un check-out. Si el backend ya tenía un check-in verificado
     * de esta persona (`HITO_DUPLICADO`), se toma como verificado: pasa si Firestore no alcanzó a guardarlo.
     */
    suspend fun registrar(
        servicioId: String,
        checkIn: Boolean,
        lat: Double,
        lon: Double,
        precisionMetros: Float,
        claveIdempotencia: String
    ): Result<HitoRegistrado> = runCatching {
        val id = servicioId.toLongOrNull()
            ?: throw BloqueoApiException("El servicio $servicioId no existe en el backend.")
        val hito = api.registrarHito(
            HitoSolicitudDto(
                servicioId = id,
                tipo = if (checkIn) "CHECK_IN" else "CHECK_OUT",
                lat = lat,
                lon = lon,
                precisionMetros = precisionMetros.toDouble().coerceAtLeast(0.0),
                claveIdempotencia = claveIdempotencia
            )
        )
        HitoRegistrado(
            resultado = ResultadoCheckin.desdeBackend(hito.resultado),
            distanciaMetros = hito.distanciaMetros?.let { Math.round(it).toInt() },
            radioMetros = hito.radioMetros
        )
    }.recoverCatching { error ->
        val traducido = error.aBloqueoApiException()
        if (checkIn && traducido.codigoApi == "HITO_DUPLICADO") {
            HitoRegistrado(ResultadoCheckin.VERIFICADO, null, null)
        } else {
            throw traducido
        }
    }
}
