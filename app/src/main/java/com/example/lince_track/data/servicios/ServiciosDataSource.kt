package com.example.lince_track.data.servicios

import com.example.lince_track.data.remote.LinceApi
import com.example.lince_track.data.remote.LinceApiException
import com.example.lince_track.data.remote.aLinceApiException
import com.example.lince_track.data.remote.dto.ServicioDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Qué servicios pedir.
 *
 * @property oficina código de oficina (SCL, PUX, PUQ). Por defecto PUX (Puerto Varas),
 *   que es donde opera la app hoy; null = todas las oficinas.
 * @property guiaId / transportistaId id de la persona en la API. Cuando cada usuario
 *   tenga su id guardado en Firestore (usuarios/{uid}), se filtrará por aquí.
 */
data class FiltroServicios(
    val desde: LocalDate,
    val hasta: LocalDate,
    val oficina: String? = OFICINA_POR_DEFECTO,
    val guiaId: Long? = null,
    val transportistaId: Long? = null
) {
    companion object {
        const val OFICINA_POR_DEFECTO = "PUX"

        /** Ventana por defecto: una semana atrás y un mes adelante. */
        fun porDefecto(hoy: LocalDate = LocalDate.now()) = FiltroServicios(
            desde = hoy.minusDays(7),
            hasta = hoy.plusDays(30)
        )
    }
}

/** Origen de los servicios: la API real o datos de ejemplo. */
interface ServiciosDataSource {
    /** Nombre para mostrar en logs ("API Southbound" / "Datos de ejemplo"). */
    val nombre: String

    /** @throws LinceApiException con un mensaje legible si algo falla. */
    suspend fun obtener(filtro: FiltroServicios): List<ServicioDto>
}

/**
 * Lee los servicios desde la API de Southbound, recorriendo todas las páginas.
 * Si la API responde 429, espera lo que indica Retry-After (hasta 10 s) y reintenta una vez.
 */
class ApiServiciosDataSource(
    private val api: LinceApi,
    private val maxPaginas: Int = 10
) : ServiciosDataSource {

    override val nombre = "API Southbound"

    override suspend fun obtener(filtro: FiltroServicios): List<ServicioDto> {
        val resultado = mutableListOf<ServicioDto>()
        var pagina = 1
        while (true) {
            val respuesta = conReintento {
                api.servicios(
                    fechaDesde = filtro.desde.toString(),
                    fechaHasta = filtro.hasta.toString(),
                    oficina = filtro.oficina,
                    guiaId = filtro.guiaId,
                    transportistaId = filtro.transportistaId,
                    orden = "fecha",
                    page = pagina,
                    pageSize = LinceApi.PAGE_SIZE_MAX
                )
            }
            resultado += respuesta.data
            val ultimaPagina = respuesta.meta.totalPages
            if (pagina >= ultimaPagina || respuesta.data.isEmpty() || pagina >= maxPaginas) break
            pagina++
        }
        return resultado
    }

    private suspend fun <T> conReintento(bloque: suspend () -> T): T {
        return try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val error = e.aLinceApiException()
            val espera = error.reintentarEnSegundos
            if (error.codigoHttp == 429 && espera != null && espera <= 10) {
                delay(espera * 1000)
                try {
                    bloque()
                } catch (e2: CancellationException) {
                    throw e2
                } catch (e2: Throwable) {
                    throw e2.aLinceApiException()
                }
            } else {
                throw error
            }
        }
    }
}
