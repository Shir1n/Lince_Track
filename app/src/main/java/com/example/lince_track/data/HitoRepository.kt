package com.example.lince_track.data

import com.example.lince_track.data.local.BaseLocal
import com.example.lince_track.data.local.GeocercaGuardada
import com.example.lince_track.data.local.LinceDao
import com.example.lince_track.data.local.ParametrosGuardados
import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.BloqueoApiException
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.data.remote.dto.HitoSolicitudDto
import com.example.lince_track.util.LinceLogger
import java.io.IOException

/** Lo que respondió el backend al registrar un hito. */
data class HitoRegistrado(
    /** null si el backend devolvió algo que la app no conoce. */
    val resultado: ResultadoCheckin?,
    val distanciaMetros: Int?,
    val radioMetros: Int?
)

/**
 * true si el error es pasajero (sin señal, servidor caído o demasiadas solicitudes): conviene guardar y reintentar.
 * Un 4xx es una respuesta del backend (prohibido, no existe, orden incorrecto) y reintentar no la cambia.
 */
val BloqueoApiException.esSinConexion: Boolean
    get() = cause is IOException || (codigoHttp ?: 0) >= 500 || codigoHttp == 429

/**
 * Geocercas y hitos (check-in y check-out) en el backend propio, que los valida otra vez en el servidor.
 * Las geocercas y las reglas quedan guardadas en Room ([dao]) para validar el check-in sin conexión.
 * Recibe la API y la base por el constructor para probarlo con MockK.
 */
class HitoRepository(
    private val api: BloqueoApi = BloqueoApiClient.api,
    private val dao: () -> LinceDao? = { BaseLocal.dao }
) {
    /** Geocercas activas. Sin conexión, las últimas guardadas (si hay). */
    suspend fun geocercas(): Result<List<Geocerca>> = conCache(
        remoto = {
            val lista = api.geocercas()
                .filter { it.activa }
                .map { Geocerca(it.id, it.nombre, it.lat, it.lon, it.radioMetros) }
            dao()?.reemplazarGeocercas(lista.map { GeocercaGuardada(it.id, it.nombre, it.lat, it.lon, it.radioMetros) })
            lista
        },
        local = {
            dao()?.geocercas()?.takeIf { it.isNotEmpty() }
                ?.map { Geocerca(it.id, it.nombre, it.lat, it.lon, it.radioMetros) }
        }
    )

    /** Anticipación y precisión máxima que coordinación configuró. Sin conexión, las últimas guardadas. */
    suspend fun parametros(): Result<ParametrosCheckin> = conCache(
        remoto = {
            val p = api.obtenerParametros()
            dao()?.guardarParametros(ParametrosGuardados(1, p.anticipacionMinutos, p.precisionMaximaMetros))
            ParametrosCheckin(p.anticipacionMinutos, p.precisionMaximaMetros)
        },
        local = { dao()?.parametros()?.let { ParametrosCheckin(it.anticipacionMinutos, it.precisionMaximaMetros) } }
    )

    /** Pide al backend; si no hay conexión y hay copia local, usa la copia. */
    private suspend fun <T> conCache(remoto: suspend () -> T, local: suspend () -> T?): Result<T> =
        runCatching { remoto() }.recoverCatching { error ->
            val traducido = error.aBloqueoApiException()
            val guardado = if (traducido.esSinConexion) runCatching { local() }.getOrNull() else null
            if (guardado == null) throw traducido
            LinceLogger.w("CHECKIN", "Sin conexión con el backend: se usan los datos guardados")
            guardado
        }

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
