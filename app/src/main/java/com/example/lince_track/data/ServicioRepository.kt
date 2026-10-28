package com.example.lince_track.data

import com.example.lince_track.data.remote.LinceApiClient
import com.example.lince_track.data.remote.aLinceApiException
import com.example.lince_track.data.servicios.ApiServiciosDataSource
import com.example.lince_track.data.servicios.FakeServiciosDataSource
import com.example.lince_track.data.servicios.FiltroServicios
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.data.servicios.ServiciosDataSource
import com.example.lince_track.util.LinceLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Fuente de datos única de servicios para las pantallas.
 *
 * - Los servicios vienen de la API de Southbound (solo lectura). Si no hay clave en
 *   local.properties (LINCE_API_KEY), se usan datos de ejemplo con la misma forma.
 * - Aceptar/rechazar se guarda por ahora solo en memoria (la API no permite escribir).
 *   El siguiente paso es guardar esas respuestas en Firestore.
 */
object ServicioRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fuente: ServiciosDataSource by lazy {
        if (LinceApiClient.estaConfigurada) {
            ApiServiciosDataSource(LinceApiClient.api)
        } else {
            FakeServiciosDataSource()
        }
    }

    /** Servicios tal como llegan de la fuente (ya convertidos al modelo de la app). */
    private val serviciosFuente = MutableStateFlow<List<Servicio>>(emptyList())

    /** Respuestas dadas en esta sesión: id del servicio → estado elegido. */
    private val respuestasLocales = MutableStateFlow<Map<String, EstadoServicio>>(emptyMap())

    val servicios: StateFlow<List<Servicio>> =
        combine(serviciosFuente, respuestasLocales) { lista, respuestas ->
            lista.map { s -> respuestas[s.id]?.let { s.copy(estado = it) } ?: s }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    /** Último error de carga, listo para mostrar en pantalla (null si no hay). */
    val error: StateFlow<String?> = _error.asStateFlow()

    private var filtroActual = FiltroServicios.porDefecto()
    private var cargaEnCurso: Job? = null

    init {
        refrescar()
    }

    /** Vuelve a pedir los servicios. Si ya hay una carga en curso, no lanza otra. */
    fun refrescar(filtro: FiltroServicios = filtroActual): Job {
        cargaEnCurso?.takeIf { it.isActive }?.let { return it }
        filtroActual = filtro
        return scope.launch {
            _cargando.value = true
            try {
                val dtos = fuente.obtener(filtro)
                val convertidos = dtos.mapNotNull { ServicioMapper.aDominio(it) }
                serviciosFuente.value = convertidos
                _error.value = null
                LinceLogger.i(
                    tag = "SERVICIOS",
                    message = "${convertidos.size} servicios cargados desde ${fuente.nombre}",
                    details = "rango=${filtro.desde}..${filtro.hasta} oficina=${filtro.oficina ?: "todas"}" +
                        if (dtos.size != convertidos.size) " | descartados=${dtos.size - convertidos.size}" else ""
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val error = e.aLinceApiException()
                _error.value = error.message
                LinceLogger.e("SERVICIOS", "No se pudieron cargar los servicios (${fuente.nombre})", error)
            } finally {
                _cargando.value = false
            }
        }.also { cargaEnCurso = it }
    }

    fun buscarPorId(id: String): Servicio? = servicios.value.find { it.id == id }

    suspend fun aceptar(id: String): Result<Unit> =
        responder(id, EstadoServicio.ACEPTADO, desde = setOf(EstadoServicio.PENDIENTE))

    suspend fun rechazar(id: String): Result<Unit> =
        responder(id, EstadoServicio.RECHAZADO, desde = setOf(EstadoServicio.PENDIENTE, EstadoServicio.ACEPTADO))

    private suspend fun responder(
        id: String,
        nuevoEstado: EstadoServicio,
        desde: Set<EstadoServicio>
    ): Result<Unit> {
        delay(300)
        val actual = buscarPorId(id)
            ?: return Result.failure(IllegalStateException("Asignación no encontrada"))
        if (actual.estado !in desde) {
            return Result.failure(IllegalStateException("Esta asignación ya fue respondida"))
        }
        respuestasLocales.update { it + (id to nuevoEstado) }
        return Result.success(Unit)
    }
}
