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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Fuente de datos única de servicios para las pantallas.
 *
 * - Los servicios vienen de la API de Southbound (solo lectura). Si no hay clave en
 *   local.properties (LINCE_API_KEY), se usan datos de ejemplo con la misma forma.
 * - Solo se cargan con sesión iniciada y según el perfil del usuario (AuthRepository);
 *   al cerrar sesión se borra todo.
 * - Aceptar/rechazar se guarda en Firestore (la API no permite escribir); ver RespuestaRepository.
 */
object ServicioRepository {

    private const val CUENTA_SIN_VINCULAR =
        "Tu cuenta aún no está vinculada. Contacta al administrador."
    private const val RESPUESTA_EN_COLA =
        "Sin conexión: se enviará al recuperar la señal."

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

    /** Servicios de la otra persona en los tours del usuario (ver ReglasCompanero). */
    private val serviciosCompaneros = MutableStateFlow<List<Servicio>>(emptyList())

    /** Lo que publicó cada persona sobre sus servicios: id del servicio → estado y nombre. */
    private val estadosCompartidos = MutableStateFlow<Map<String, RespuestaServicio>>(emptyMap())
    private var escuchaEstados: Job? = null

    private val _respuestas = MutableStateFlow<Map<String, RespuestaServicio>>(emptyMap())
    /**
     * Respuestas guardadas en Firestore: id del servicio → respuesta.
     * Las del usuario o, para el administrador, las de todos.
     */
    val respuestas: StateFlow<Map<String, RespuestaServicio>> = _respuestas.asStateFlow()
    private var escuchaRespuestas: Job? = null

    private val _checkins = MutableStateFlow<Map<String, CheckinServicio>>(emptyMap())
    /**
     * Check-ins guardados en Firestore: id del servicio → check-in.
     * Los del usuario o, para el administrador, los de todos.
     */
    val checkins: StateFlow<Map<String, CheckinServicio>> = _checkins.asStateFlow()
    private var escuchaCheckins: Job? = null

    val servicios: StateFlow<List<Servicio>> =
        combine(serviciosFuente, _respuestas, serviciosCompaneros, estadosCompartidos, AuthRepository.perfilActual) { lista, porServicio, companeros, estados, perfil ->
            val companerosConEstado = companeros.map { ReglasCompanero.aplicarPublicado(it, estados[it.id]) }
            lista.map { servicio ->
                val propio = ReglasCompanero.conNombrePropio(ReglasRespuesta.aplicar(servicio, porServicio[servicio.id]), perfil)
                ReglasCompanero.completar(propio, companerosConEstado)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val errorCarga = MutableStateFlow<String?>(null)
    /** Último error (de carga o de la cuenta), listo para mostrar en pantalla (null si no hay). */
    val error: StateFlow<String?> =
        combine(errorCarga, AuthRepository.errorPerfil) { carga, perfil -> perfil ?: carga }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** Qué pedir a la fuente; null si no hay nada que cargar para el usuario actual. */
    private var filtroActual: FiltroServicios? = null
    private var cargaEnCurso: Job? = null

    init {
        scope.launch {
            AuthRepository.perfilActual.collect { aplicarPerfil(it) }
        }
    }

    /** Descarta los datos del usuario anterior y carga los del nuevo. */
    private suspend fun aplicarPerfil(perfil: PerfilUsuario?) {
        // Se espera a que termine para que una carga antigua no escriba sobre los datos nuevos.
        cargaEnCurso?.cancelAndJoin()
        escuchaRespuestas?.cancelAndJoin()
        escuchaCheckins?.cancelAndJoin()
        escuchaEstados?.cancelAndJoin()
        serviciosFuente.value = emptyList()
        serviciosCompaneros.value = emptyList()
        _respuestas.value = emptyMap()
        _checkins.value = emptyMap()
        estadosCompartidos.value = emptyMap()
        errorCarga.value = null
        filtroActual = perfil?.let { FiltroServicios.paraPerfil(it) }

        val uid = AuthRepository.usuarioActual?.uid
        if (perfil != null && uid != null) {
            val soloDe = if (perfil.rol == Rol.ADMINISTRADOR) null else uid
            escuchaRespuestas = scope.launch {
                RespuestaRepository.observar(soloDe).collect { _respuestas.value = it }
            }
            escuchaCheckins = scope.launch {
                CheckinRepository.observar(soloDe).collect { _checkins.value = it }
            }
            val filtro = filtroActual
            if (filtro != null && soloDe != null) {
                escuchaEstados = scope.launch {
                    RespuestaRepository.observarEstados(filtro.desde.toString()).collect { estadosCompartidos.value = it }
                }
            }
        }

        if (perfil != null && filtroActual == null) {
            errorCarga.value = CUENTA_SIN_VINCULAR
            LinceLogger.w("SERVICIOS", "Cuenta de ${perfil.rol.etiqueta} sin id de la API")
        }
        refrescar()
    }

    /** Vuelve a pedir los servicios. Si ya hay una carga en curso, no lanza otra. */
    fun refrescar(): Job? {
        cargaEnCurso?.takeIf { it.isActive }?.let { return it }
        val filtro = filtroActual ?: return null
        return scope.launch {
            _cargando.value = true
            try {
                val dtos = fuente.obtener(filtro)
                val convertidos = dtos.mapNotNull { ServicioMapper.aDominio(it) }
                serviciosFuente.value = convertidos
                serviciosCompaneros.value = cargarCompaneros(filtro, convertidos)
                errorCarga.value = null
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
                errorCarga.value = error.message
                LinceLogger.e("SERVICIOS", "No se pudieron cargar los servicios (${fuente.nombre})", error)
            } finally {
                _cargando.value = false
            }
        }.also { cargaEnCurso = it }
    }

    /**
     * Servicios de otras personas en las mismas reservas que [propios]: el filtro por persona no
     * los trae. Si fallan, la lista propia sigue sirviendo, así que solo se registra el error.
     */
    private suspend fun cargarCompaneros(filtro: FiltroServicios, propios: List<Servicio>): List<Servicio> {
        if (propios.isEmpty() || (filtro.guiaId == null && filtro.transportistaId == null)) return emptyList()
        return try {
            val reservas = propios.map { it.reserva }.toSet()
            val idsPropios = propios.map { it.id }.toSet()
            fuente.obtener(filtro.copy(guiaId = null, transportistaId = null))
                .mapNotNull { ServicioMapper.aDominio(it) }
                .filter { it.reserva in reservas && it.id !in idsPropios }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LinceLogger.e("SERVICIOS", "No se pudieron cargar los servicios relacionados", e.aLinceApiException())
            serviciosCompaneros.value
        }
    }

    fun buscarPorId(id: String): Servicio? = servicios.value.find { it.id == id }

    /** @return en caso de éxito, un aviso para mostrar al usuario (null si no hay nada que avisar). */
    suspend fun aceptar(id: String): Result<String?> =
        responder(id, EstadoServicio.ACEPTADO, desde = setOf(EstadoServicio.PENDIENTE))

    /** @return en caso de éxito, un aviso para mostrar al usuario (null si no hay nada que avisar). */
    suspend fun rechazar(id: String, motivo: MotivoRechazo, observaciones: String): Result<String?> {
        val texto = observaciones.trim()
        if (!ReglasRespuesta.motivoValido(motivo, texto)) {
            return Result.failure(IllegalArgumentException("Indica el motivo del rechazo"))
        }
        return responder(
            id,
            EstadoServicio.RECHAZADO,
            desde = setOf(EstadoServicio.PENDIENTE, EstadoServicio.ACEPTADO),
            motivo = motivo,
            observaciones = texto
        )
    }

    /** @return en caso de éxito, un aviso para mostrar al usuario (null si no hay nada que avisar). */
    suspend fun registrarCheckIn(
        id: String,
        lat: Double,
        lon: Double,
        distanciaM: Int,
        gpsReal: Boolean
    ): Result<String?> = registrarEtapaRuta(id, esCheckIn = true) { uid, rol ->
        CheckinRepository.registrarCheckIn(uid, rol, id, lat, lon, distanciaM, gpsReal)
    }

    /** @return en caso de éxito, un aviso para mostrar al usuario (null si no hay nada que avisar). */
    suspend fun registrarCheckOut(id: String, lat: Double, lon: Double): Result<String?> =
        registrarEtapaRuta(id, esCheckIn = false) { uid, _ ->
            CheckinRepository.registrarCheckOut(uid, id, lat, lon)
        }

    private suspend fun registrarEtapaRuta(
        id: String,
        esCheckIn: Boolean,
        guardar: suspend (uid: String, rol: Rol) -> Boolean
    ): Result<String?> {
        val uid = AuthRepository.usuarioActual?.uid
        val perfil = AuthRepository.perfilActual.value
        if (uid == null || perfil == null) {
            return Result.failure(IllegalStateException("Tu sesión no está lista. Intenta nuevamente."))
        }
        if (perfil.rol == Rol.ADMINISTRADOR) {
            return Result.failure(IllegalStateException("El administrador no registra check-in"))
        }
        val servicio = buscarPorId(id)
            ?: return Result.failure(IllegalStateException("Asignación no encontrada"))
        if (servicio.estado != EstadoServicio.ACEPTADO) {
            return Result.failure(IllegalStateException("Solo se registra en servicios aceptados"))
        }
        val checkin = _checkins.value[id]
        if (esCheckIn && checkin != null) {
            return Result.failure(IllegalStateException("El check-in ya está registrado"))
        }
        if (!esCheckIn && (checkin == null || checkin.finalizado)) {
            return Result.failure(IllegalStateException("No hay un check-in pendiente de cerrar"))
        }
        return try {
            Result.success(if (guardar(uid, perfil.rol)) null else RESPUESTA_EN_COLA)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LinceLogger.e("SERVICIOS", "No se pudo guardar el check-in", e)
            Result.failure(IllegalStateException("No se pudo guardar el registro. Intenta nuevamente."))
        }
    }

    private suspend fun responder(
        id: String,
        nuevoEstado: EstadoServicio,
        desde: Set<EstadoServicio>,
        motivo: MotivoRechazo? = null,
        observaciones: String = ""
    ): Result<String?> {
        val uid = AuthRepository.usuarioActual?.uid
        val perfil = AuthRepository.perfilActual.value
        if (uid == null || perfil == null) {
            return Result.failure(IllegalStateException("Tu sesión no está lista. Intenta nuevamente."))
        }
        if (perfil.rol == Rol.ADMINISTRADOR) {
            return Result.failure(IllegalStateException("El administrador no responde servicios"))
        }
        val actual = buscarPorId(id)
            ?: return Result.failure(IllegalStateException("Asignación no encontrada"))
        if (actual.estado !in desde) {
            return Result.failure(IllegalStateException("Esta asignación ya fue respondida"))
        }
        return try {
            val confirmada = RespuestaRepository.guardar(uid, perfil.rol, perfil.nombre.orEmpty(), id, actual.fecha.toString(), nuevoEstado, motivo, observaciones)
            Result.success(if (confirmada) null else RESPUESTA_EN_COLA)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LinceLogger.e("SERVICIOS", "No se pudo guardar la respuesta", e)
            Result.failure(IllegalStateException("No se pudo guardar tu respuesta. Intenta nuevamente."))
        }
    }
}
