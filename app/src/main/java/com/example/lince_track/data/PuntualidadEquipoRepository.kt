package com.example.lince_track.data

import com.example.lince_track.data.remote.BloqueoApi
import com.example.lince_track.data.remote.BloqueoApiClient
import com.example.lince_track.data.remote.aBloqueoApiException
import com.example.lince_track.data.remote.dto.ResumenRecursoDto
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Cómo le fue a una persona con sus check-in en el periodo (último check-in de cada servicio).
 *
 * @property nombre el de su cuenta (`usuarios/{uid}`); null si no se encontró (la pantalla muestra "Sin nombre").
 * @property puntualidad porcentaje a tiempo, redondeado; null si no tuvo servicios.
 * @property minutosAnticipacion minutos antes del pickup en promedio (negativo = después); null si no hay dato.
 */
data class PuntualidadPersona(
    val uid: String,
    val nombre: String?,
    val rol: Rol?,
    val servicios: Int,
    val aTiempo: Int,
    val tarde: Int,
    val fueraDeZona: Int,
    val gpsImpreciso: Int,
    val puntualidad: Int?,
    val minutosAnticipacion: Int?
)

/** Puntualidad del equipo: el total y una fila por persona, la menos puntual primero (orden del backend). */
data class PuntualidadEquipo(
    val servicios: Int,
    val puntualidad: Int?,
    val personas: List<PuntualidadPersona>
)

/**
 * Pide al backend propio la puntualidad por persona (`/api/hitos/resumen`) y le pone a cada uid el nombre de su
 * cuenta. Recibe la API y la lectura de nombres por el constructor para probarlo con MockK; la app usa las reales.
 */
class PuntualidadEquipoRepository(
    private val api: BloqueoApi = BloqueoApiClient.api,
    private val nombres: suspend () -> Map<String, String> = {
        AsignacionRepository.personal().associate { it.uid to it.nombre }
    }
) {

    /** Si falla el backend, el error es un BloqueoApiException (con el requestId si vino). */
    suspend fun obtener(rango: ClosedRange<LocalDate>): Result<PuntualidadEquipo> = try {
        val dto = api.resumenHitos(rango.start.toString(), rango.endInclusive.toString())
        val porUid = leerNombres()
        Result.success(
            PuntualidadEquipo(
                servicios = dto.total.servicios,
                puntualidad = dto.total.puntualidad?.roundToInt(),
                personas = dto.recursos.mapNotNull { aPersona(it, porUid) }
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.aBloqueoApiException())
    }

    /** Sin los nombres la tarjeta igual sirve: cada fila queda "Sin nombre". */
    private suspend fun leerNombres(): Map<String, String> = try {
        nombres()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.w("PUNTUALIDAD", "No se pudieron leer los nombres del equipo: ${e.message}")
        emptyMap()
    }

    companion object {
        fun aPersona(dto: ResumenRecursoDto, nombres: Map<String, String>): PuntualidadPersona? {
            val uid = dto.uid ?: return null
            return PuntualidadPersona(
                uid = uid,
                nombre = nombres[uid]?.takeIf { it.isNotBlank() },
                rol = Rol.desde(dto.rol),
                servicios = dto.servicios,
                aTiempo = dto.aTiempo,
                tarde = dto.tarde,
                fueraDeZona = dto.fueraDeZona,
                gpsImpreciso = dto.gpsImpreciso,
                puntualidad = dto.puntualidad?.roundToInt(),
                minutosAnticipacion = dto.minutosAnticipacionPromedio?.roundToInt()
            )
        }
    }
}
