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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private const val SIN_SESION = Int.MAX_VALUE
    /** Primera carga, respuestas, estados y asignaciones. */
    private const val FUENTES_INICIALES = 4

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

    /**
     * Toda la oficina, para encontrar a la otra persona de cada tour y lo que se le reasignó al
     * usuario: el filtro por persona no trae esos servicios. Vacía para el administrador.
     */
    private val serviciosOficina = MutableStateFlow<List<Servicio>>(emptyList())

    private val _asignaciones = MutableStateFlow<Map<String, AsignacionServicio>>(emptyMap())
    /** Reemplazos elegidos por el administrador: id del servicio → reemplazo. */
    val asignaciones: StateFlow<Map<String, AsignacionServicio>> = _asignaciones.asStateFlow()
    private var escuchaAsignaciones: Job? = null

    private val rechazaron = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

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
        combine(
            combine(serviciosFuente, serviciosOficina, ::Pair),
            _asignaciones,
            _respuestas,
            estadosCompartidos,
            AuthRepository.perfilActual
        ) { (propios, oficina), asignaciones, respuestas, estados, perfil ->
            ReglasAsignacion.armar(propios, oficina, asignaciones, respuestas, estados, perfil, AuthRepository.usuarioActual?.uid)
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Lo que falta por llegar tras iniciar sesión: la primera carga y cada escucha. */
    private val fuentesPendientes = MutableStateFlow(SIN_SESION)
    private var cargaInicialHecha = false

    /** true cuando llegó todo lo inicial: desde aquí, un cambio en [servicios] es una novedad. */
    val datosListos: StateFlow<Boolean> =
        fuentesPendientes.map { it == 0 }.stateIn(scope, SharingStarted.Eagerly, false)

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
        fuentesPendientes.value = SIN_SESION
        // Se espera a que termine para que una carga antigua no escriba sobre los datos nuevos.
        cargaEnCurso?.cancelAndJoin()
        escuchaRespuestas?.cancelAndJoin()
        escuchaCheckins?.cancelAndJoin()
        escuchaEstados?.cancelAndJoin()
        escuchaAsignaciones?.cancelAndJoin()
        serviciosFuente.value = emptyList()
        serviciosOficina.value = emptyList()
        _respuestas.value = emptyMap()
        rechazaron.value = emptyMap()
        _checkins.value = emptyMap()
        estadosCompartidos.value = emptyMap()
        _asignaciones.value = emptyMap()
        errorCarga.value = null
        cargaInicialHecha = false
        filtroActual = perfil?.let { FiltroServicios.paraPerfil(it) }

        val uid = AuthRepository.usuarioActual?.uid
        if (perfil != null && uid != null) {
            val soloDe = if (perfil.rol == Rol.ADMINISTRADOR) null else uid
            if (filtroActual != null) fuentesPendientes.value = FUENTES_INICIALES
            escuchaRespuestas = scope.launch {
                RespuestaRepository.observar(soloDe).marcandoPrimera().collect { lista ->
                    // Si un servicio tiene más de una respuesta, queda la más reciente.
                    _respuestas.value = lista.associateBy { it.servicioId }
                    rechazaron.value = ReglasAsignacion.rechazos(lista)
                }
            }
            escuchaCheckins = scope.launch {
                CheckinRepository.observar(soloDe).collect { _checkins.value = it }
            }
            filtroActual?.let { filtro ->
                val desde = filtro.desde.toString()
                escuchaEstados = scope.launch {
                    RespuestaRepository.observarEstados(desde).marcandoPrimera().collect { estadosCompartidos.value = it }
                }
                escuchaAsignaciones = scope.launch {
                    AsignacionRepository.observar(desde).marcandoPrimera().collect { _asignaciones.value = it }
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
                serviciosOficina.value = cargarOficina(filtro)
                errorCarga.value = null
                if (!cargaInicialHecha) {
                    cargaInicialHecha = true
                    fuentesPendientes.update { it - 1 }
                }
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

    /** Si falla, la lista propia sigue sirviendo, así que solo se registra el error. */
    private suspend fun cargarOficina(filtro: FiltroServicios): List<Servicio> {
        if (filtro.guiaId == null && filtro.transportistaId == null) return emptyList()
        return try {
            fuente.obtener(filtro.copy(guiaId = null, transportistaId = null))
                .mapNotNull { ServicioMapper.aDominio(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LinceLogger.e("SERVICIOS", "No se pudieron cargar los servicios de la oficina", e.aLinceApiException())
            serviciosOficina.value
        }
    }

    /** Descuenta esta escucha de [fuentesPendientes] cuando llega su primer resultado. */
    private fun <T> Flow<T>.marcandoPrimera(): Flow<T> {
        var primera = true
        return onEach {
            if (primera) {
                primera = false
                fuentesPendientes.update { it - 1 }
            }
        }
    }

    /** Cuentas a las que se les puede reasignar el servicio. */
    suspend fun candidatosPara(servicio: Servicio): Result<List<PersonaDisponible>> = try {
        Result.success(
            ReglasAsignacion.candidatos(
                servicio,
                AsignacionRepository.personal(),
                _asignaciones.value[servicio.id],
                rechazaron.value[servicio.id].orEmpty()
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LinceLogger.e("SERVICIOS", "No se pudo leer el personal", e)
        Result.failure(IllegalStateException("No se pudo cargar el personal. Intenta nuevamente."))
    }

    /** @return en caso de éxito, un aviso para mostrar al usuario (null si no hay nada que avisar). */
    suspend fun reasignar(servicio: Servicio, persona: PersonaDisponible): Result<String?> {
        val uid = AuthRepository.usuarioActual?.uid
        if (uid == null || AuthRepository.perfilActual.value?.rol != Rol.ADMINISTRADOR) {
            return Result.failure(IllegalStateException("Solo el administrador puede reasignar"))
        }
        if (!ReglasAsignacion.puedeReasignar(servicio, _checkins.value[servicio.id])) {
            return Result.failure(IllegalStateException("Este servicio ya no se puede reasignar"))
        }
        return try {
            Result.success(if (AsignacionRepository.reasignar(servicio, persona, uid)) null else RESPUESTA_EN_COLA)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LinceLogger.e("SERVICIOS", "No se pudo reasignar el servicio", e)
            Result.failure(IllegalStateException("No se pudo reasignar. Intenta nuevamente."))
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
