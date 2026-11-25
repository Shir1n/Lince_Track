package com.example.lince_track.data

import com.example.lince_track.util.LinceLogger
import com.example.lince_track.data.remote.LinceApi
import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.data.remote.aLinceApiException
import com.example.lince_track.data.remote.dto.GuiaDto
import com.example.lince_track.data.remote.dto.PaginaDto
import com.example.lince_track.data.remote.dto.TransportistaDto
import kotlinx.coroutines.CancellationException

/** Guía o transportista según la API de Southbound. */
enum class TipoMiembro { GUIA, TRANSPORTISTA }

data class VehiculoEquipo(val patente: String, val tipo: String, val capacidad: Int)

/**
 * Una persona del equipo de la oficina (`/guias` o `/transportistas`).
 *
 * @property idiomas códigos de idioma (ES, EN…); solo guías.
 * @property vehiculos solo transportistas.
 */
data class MiembroEquipo(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val tipo: TipoMiembro,
    val activo: Boolean,
    val evaluacion: Double,
    val fechaIngreso: String,
    val idiomas: List<String> = emptyList(),
    val vehiculos: List<VehiculoEquipo> = emptyList()
)

/** Guías y transportistas de la oficina: primero los activos y, dentro de cada grupo, por nombre. */
/** Agencia que vende servicios de Southbound (`/clientes`). */
data class Agencia(val id: Long, val codigo: String, val nombre: String, val pais: String)

data class EquipoSouthbound(
    val guias: List<MiembroEquipo>,
    val transportistas: List<MiembroEquipo>,
    val agencias: List<Agencia> = emptyList()
)

/**
 * Lee de la API de Southbound el equipo de la oficina y los tipos de servicio de `/catalogos`. Recibe la API por el
 * constructor para probarlo con MockK; la app usa la real.
 */
class EquipoSouthboundRepository(
    private val api: LinceApi = LinceApiClient.api,
    private val oficina: String = OFICINA
) {

    /** Guías y transportistas (todas las páginas). Si falla, el error es un LinceApiException. */
    suspend fun equipo(): Result<EquipoSouthbound> = conErrores {
        val guias = todasLasPaginas { pagina -> api.guias(oficina = oficina, page = pagina, pageSize = PAGINA) }
        val transportistas =
            todasLasPaginas { pagina -> api.transportistas(oficina = oficina, page = pagina, pageSize = PAGINA) }
        // Las agencias son un extra: si /clientes falla, el equipo igual se muestra.
        val agencias = try {
            todasLasPaginas { pagina -> api.clientes(page = pagina, pageSize = PAGINA) }
                .map { Agencia(it.id, it.codigo, it.nombre, it.pais) }
                .sortedBy { it.nombre.lowercase() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LinceLogger.w("EQUIPO", "No se pudieron leer las agencias de /clientes: ${e.message}")
            emptyList()
        }
        EquipoSouthbound(
            guias = ordenar(guias.map { aMiembro(it) }),
            transportistas = ordenar(transportistas.map { aMiembro(it) }),
            agencias = agencias
        )
    }

    /** Tipos de servicio de `/catalogos` (TRANSFER, GUIDED_SERVICE, EXCURSION…), en el orden de la API. */
    suspend fun tiposServicio(): Result<List<String>> = conErrores {
        api.catalogos().data.tiposServicio.map { it.tipo }.filter { it.isNotBlank() }.distinct()
    }

    private suspend fun <T> conErrores(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.aLinceApiException())
    }

    /** Pide páginas hasta la última que informa la API (con un tope por si la respuesta viniera mal). */
    private suspend fun <T> todasLasPaginas(pedir: suspend (Int) -> PaginaDto<T>): List<T> {
        val todos = mutableListOf<T>()
        var pagina = 1
        do {
            val respuesta = pedir(pagina)
            todos += respuesta.data
            pagina++
        } while (pagina <= respuesta.meta.totalPages && pagina <= MAX_PAGINAS)
        return todos
    }

    companion object {
        const val OFICINA = "PUX"
        const val PAGINA = 100
        const val MAX_PAGINAS = 10

        /** Si `/catalogos` no responde, el filtro igual ofrece los tres tipos que usa la API hoy. */
        val TIPOS_SERVICIO_CONOCIDOS = listOf("TRANSFER", "GUIDED_SERVICE", "EXCURSION")

        fun aMiembro(dto: GuiaDto) = MiembroEquipo(
            id = dto.id,
            codigo = dto.codigo,
            nombre = dto.nombre,
            tipo = TipoMiembro.GUIA,
            activo = dto.activo,
            evaluacion = dto.evaluacionPromedio,
            fechaIngreso = dto.fechaIngreso,
            idiomas = dto.idiomas.map { it.codigo }.filter { it.isNotBlank() }
        )

        fun aMiembro(dto: TransportistaDto) = MiembroEquipo(
            id = dto.id,
            codigo = dto.codigo,
            nombre = dto.nombre,
            tipo = TipoMiembro.TRANSPORTISTA,
            activo = dto.activo,
            evaluacion = dto.evaluacionPromedio,
            fechaIngreso = dto.fechaIngreso,
            vehiculos = dto.vehiculos.map { VehiculoEquipo(it.patente, it.tipo, it.capacidad) }
        )

        fun ordenar(miembros: List<MiembroEquipo>): List<MiembroEquipo> =
            miembros.sortedWith(compareBy<MiembroEquipo> { !it.activo }.thenBy { it.nombre })

        /** Servicios de un tipo; con [tipo] null, todos. */
        fun filtrarPorTipo(servicios: List<Servicio>, tipo: String?): List<Servicio> =
            if (tipo == null) servicios else servicios.filter { it.tipo == tipo }
    }
}
