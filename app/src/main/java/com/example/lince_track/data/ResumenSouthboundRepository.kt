package com.example.lince_track.data

import com.example.lince_track.data.remote.LinceApi
import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.data.remote.aLinceApiException
import com.example.lince_track.data.remote.dto.ConteoPorClaveDto
import com.example.lince_track.data.remote.dto.ResumenDto
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

/** Cuántos servicios y pasajeros hay con un statusPL (CF, RQ, DL, CX, SIN_ASIGNAR u otro que agregue la API). */
data class ConteoStatusPL(val clave: String, val servicios: Int, val pax: Int)

/** Totales de Southbound (`/api/v1/resumen`) para la tarjeta del Resumen del administrador. */
data class ResumenSouthbound(
    val totalServicios: Int,
    val totalPax: Int,
    val porStatusPL: List<ConteoStatusPL>
)

/**
 * Pide a la API de Southbound el resumen de la oficina en un rango de fechas. Recibe la API por el constructor para
 * probarlo con MockK; la app usa la real.
 */
class ResumenSouthboundRepository(
    private val api: LinceApi = LinceApiClient.api,
    private val oficina: String = OFICINA
) {

    /** Si falla, el error es un LinceApiException (con su texto traducible y el requestId de la API). */
    suspend fun obtener(rango: ClosedRange<LocalDate>): Result<ResumenSouthbound> = try {
        val dto = api.resumen(oficina, rango.start.toString(), rango.endInclusive.toString()).data
        Result.success(aModelo(dto))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.aLinceApiException())
    }

    companion object {
        /** La app trabaja con la oficina de Puerto Varas. */
        const val OFICINA = "PUX"

        /** Orden de la tarjeta: los cinco de siempre (aunque vengan en 0) y después los que la API agregue. */
        val ORDEN_STATUS_PL = listOf("CF", "RQ", "DL", "CX", "SIN_ASIGNAR")

        fun aModelo(dto: ResumenDto): ResumenSouthbound =
            ResumenSouthbound(dto.totalServicios, dto.totalPax, ordenar(dto.porStatusPL))

        fun ordenar(lista: List<ConteoPorClaveDto>): List<ConteoStatusPL> {
            val porClave = lista.associateBy { it.clave }
            val conocidos = ORDEN_STATUS_PL.map { clave ->
                porClave[clave]?.let { ConteoStatusPL(clave, it.servicios, it.pax) } ?: ConteoStatusPL(clave, 0, 0)
            }
            val nuevos = lista.filter { it.clave !in ORDEN_STATUS_PL }.map { ConteoStatusPL(it.clave, it.servicios, it.pax) }
            return conocidos + nuevos
        }
    }
}
