package com.example.lince_track.ui.ruta

import com.example.lince_track.ui.components.rememberPermisoUbicacion
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lince_track.BuildConfig
import com.example.lince_track.R
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EvaluacionCheckin
import com.example.lince_track.data.Geocerca
import com.example.lince_track.data.HitoRepository
import com.example.lince_track.data.ParametrosCheckin
import com.example.lince_track.data.ReglasGeocerca
import com.example.lince_track.data.ResultadoCheckin
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.ui.components.ReportarIncidenciaDialog
import com.example.lince_track.ui.components.rememberEnviarIncidencia
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.clima.ClimaInfo
import com.example.lince_track.data.clima.ClimaService
import com.example.lince_track.data.clima.PresetClima
import com.example.lince_track.data.clima.RecomendacionConduccion
import com.example.lince_track.data.clima.zonaClima
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.clima.TipoClima
import com.example.lince_track.data.esGuiado
import com.example.lince_track.data.responsable
import com.example.lince_track.data.remote.mensajeBloqueo
import com.example.lince_track.data.servicios.ServicioMapper
import com.example.lince_track.util.LinceLogger
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.os.Looper
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import com.example.lince_track.ui.theme.BarraSuperior

enum class RolMapa(val label: String) {
    TRANSPORTISTA("Transportista"),
    GUIA("Guía SERNATUR"),
    COORDINADOR("Coordinador");

    companion object {
        /** La vista del mapa sale del rol real de la cuenta. */
        fun desde(rol: Rol?): RolMapa = when (rol) {
            Rol.GUIA -> GUIA
            Rol.ADMINISTRADOR -> COORDINADOR
            else -> TRANSPORTISTA
        }
    }
}

enum class AppNavegacionExterna(val label: String, val subtitle: String) {
    GOOGLE_MAPS("Google Maps", "Navegación giro a giro por voz"),
    WAZE("Waze", "Ruta rápida con alertas de tráfico en vivo"),
    SELECTOR_SISTEMA("Otra app del teléfono", "Abrir selector nativo de Android")
}

private val PickupHotelCumbres = GeoPoint(-41.3195, -72.9854)
private val DestinoAeropuertoTepual = GeoPoint(-41.4389, -73.0940)
private val DestinoPetrohue = GeoPoint(-41.1731, -72.4468)
private val DestinoCentroPuertoVaras = GeoPoint(-41.3178, -72.9825)
private val PuntoEncuentroGuias = GeoPoint(-41.3182, -72.9838)
private val SimuladorCercaPickup = GeoPoint(-41.3208, -72.9869)

@Composable
fun RutaMapaScreen(
    servicioId: String,
    onBack: () -> Unit = {}
) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val servicio = remember(servicios, servicioId) { servicios.find { it.id == servicioId } }

    if (servicio == null) {
        RutaNoEncontrada(onBack = onBack)
    } else {
        RutaMapaContenido(servicio = servicio, onBack = onBack)
    }
}

@Composable
private fun RutaNoEncontrada(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.ruta_no_encontrado),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.cd_volver))
            }
        }
    }
}

@Composable
private fun RutaMapaContenido(
    servicio: Servicio,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val enviarIncidencia = rememberEnviarIncidencia(servicio.id, snackbarHostState)

    val rol by AuthRepository.rolActual.collectAsState()
    val activeRole = RolMapa.desde(rol)
    val esCoordinador = activeRole == RolMapa.COORDINADOR

    val checkins by ServicioRepository.checkins.collectAsState()
    val checkin = checkins[servicio.id]
    val isCheckInDone = checkin != null
    val isServiceFinished = checkin?.finalizado == true

    var procesando by remember { mutableStateOf(false) }
    var showExternalNavDialog by remember { mutableStateOf(false) }
    var showIncidenciaDialog by remember { mutableStateOf(false) }
    var recentrarMapa by remember { mutableIntStateOf(0) }

    var hasLocationPermission by remember {
        mutableStateOf(hasPermissions(context))
    }
    var userGeoPoint by remember { mutableStateOf(SimuladorCercaPickup) }
    var isUsingRealGps by remember { mutableStateOf(false) }
    var precisionGps by remember { mutableStateOf<Float?>(null) }
    var currentSpeedKmh by remember { mutableFloatStateOf(0f) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // El administrador no está en terreno: no se pide ni se sigue su ubicación.
    val seguirUbicacion = hasLocationPermission && !esCoordinador

    DisposableEffect(seguirUbicacion) {
        if (!seguirUbicacion) return@DisposableEffect onDispose {}

        // Cada lectura redibuja la pantalla: leer más seguido traba los gestos del mapa.
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val speed = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                currentSpeedKmh = speed
                userGeoPoint = GeoPoint(loc.latitude, loc.longitude)
                isUsingRealGps = true
                precisionGps = if (loc.hasAccuracy()) loc.accuracy else null
                // Sin coordenadas en el registro: la consola de logs se puede compartir y no debe guardar el
                // recorrido de la persona (aviso de privacidad).
                if (speed > 5f) {
                    LinceLogger.i("GPS", "Velocímetro: ${speed.roundToInt()} km/h")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            LinceLogger.i("GPS", "Suscripción activa de tracking GPS y velocímetro")
        } catch (e: SecurityException) {
            LinceLogger.w("GPS", "Error al iniciar tracking GPS: ${e.message}")
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            LinceLogger.i("GPS", "Tracking GPS pausado")
        }
    }

    // Antes del permiso de Android se explica para qué se usa la ubicación (aviso de privacidad).
    val pedirUbicacion = rememberPermisoUbicacion(
        motivo = "Mientras el mapa está abierto, el GPS muestra tu posición, la distancia al punto de encuentro y tu " +
            "velocidad (eso no se envía). En el check-in y el check-out se registra dónde estás."
    ) { granted ->
        hasLocationPermission = granted
        LinceLogger.i("GPS", "Respuesta de permisos de ubicación: granted=$granted")
        if (granted) {
            fetchCurrentGps(
                context = context,
                fusedClient = fusedLocationClient,
                onLocation = { point, real, precision ->
                    userGeoPoint = point
                    isUsingRealGps = real
                    precisionGps = precision
                    recentrarMapa++
                }
            )
        }
    }

    LaunchedEffect(esCoordinador) {
        Configuration.getInstance().userAgentValue = "LinceTrackChile/1.0 (cl.duoc.lince_track)"
        LinceLogger.i(
            tag = "MAPA",
            message = "Abriendo RutaMapaScreen para servicio ${servicio.id}",
            details = "titulo='${servicio.titulo}', rol=${activeRole.label}"
        )
        if (esCoordinador) return@LaunchedEffect
        if (hasLocationPermission) {
            fetchCurrentGps(
                context = context,
                fusedClient = fusedLocationClient,
                onLocation = { point, real, precision ->
                    userGeoPoint = point
                    isUsingRealGps = real
                    precisionGps = precision
                    recentrarMapa++
                }
            )
        } else {
            LinceLogger.w("GPS", "Sin permiso de ubicación activo; solicitando al usuario")
            pedirUbicacion()
        }
    }

    // Geocerca del pickup y reglas de coordinación, desde el backend propio.
    val hitos = remember { HitoRepository() }
    var geocercas by remember { mutableStateOf<List<Geocerca>?>(null) }
    var parametrosCheckin by remember { mutableStateOf<ParametrosCheckin?>(null) }
    var errorBackend by remember { mutableStateOf<String?>(null) }
    var cargarBackend by remember { mutableIntStateOf(0) }
    LaunchedEffect(cargarBackend) {
        errorBackend = null
        val porDefecto = context.getString(R.string.error_operacion)
        hitos.geocercas()
            .onSuccess { geocercas = it }
            .onFailure { errorBackend = it.mensajeBloqueo(porDefecto) }
        hitos.parametros()
            .onSuccess { parametrosCheckin = it }
            .onFailure { errorBackend = errorBackend ?: it.mensajeBloqueo(porDefecto) }
    }
    // La hora límite se revisa cada 30 s mientras el mapa está abierto.
    var ahora by remember { mutableStateOf(LocalDateTime.now(ReglasGeocerca.ZONA)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            ahora = LocalDateTime.now(ReglasGeocerca.ZONA)
        }
    }
    val geocercaPickup = remember(geocercas, servicio.origen) {
        geocercas?.let { ReglasGeocerca.buscar(it, servicio.origen) }
    }
    val pickupPoint = geocercaPickup?.let { GeoPoint(it.lat, it.lon) } ?: PickupHotelCumbres
    // Primera validación, en la app: bloquea el botón. El backend la repite con su propia hora.
    val evaluacion = parametrosCheckin?.takeIf { geocercas != null }?.let { parametros ->
        ReglasGeocerca.evaluar(
            geocerca = geocercaPickup,
            lat = userGeoPoint.latitude,
            lon = userGeoPoint.longitude,
            gpsReal = isUsingRealGps,
            precisionMetros = precisionGps,
            ahora = ahora,
            inicio = LocalDateTime.of(servicio.fecha, servicio.horaInicio),
            parametros = parametros
        )
    }
    val evaluacionVigente by rememberUpdatedState(evaluacion)
    // Se cambia solo cuando el backend respondió: si se reintenta tras un corte, no se duplica el check-in.
    var claveCheckin by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    // Con geocerca, el destino es su punto; sin ella, se deduce del texto (la API no trae coordenadas).
    val destinationPoint = remember(servicio, geocercas) {
        val geocercaDestino = geocercas?.let { ReglasGeocerca.buscar(it, servicio.destino) }
        if (geocercaDestino != null) return@remember GeoPoint(geocercaDestino.lat, geocercaDestino.lon)
        when {
            servicio.destino.contains("Petrohué", ignoreCase = true) ||
                servicio.destino.contains("Parque", ignoreCase = true) -> DestinoPetrohue
            servicio.destino.contains("Centro", ignoreCase = true) -> DestinoCentroPuertoVaras
            else -> DestinoAeropuertoTepual
        }
    }

    val referenceTarget = when {
        isCheckInDone -> destinationPoint
        geocercaPickup != null -> pickupPoint
        activeRole == RolMapa.GUIA -> PuntoEncuentroGuias
        else -> PickupHotelCumbres
    }

    val targetLabel = when {
        isCheckInDone -> servicio.destino
        activeRole == RolMapa.GUIA -> "Lobby ${servicio.origen} (Punto de Encuentro)"
        else -> "${servicio.origen} (Pickup)"
    }

    val distanceMeters = remember(userGeoPoint, referenceTarget) {
        calculateDistanceMeters(userGeoPoint, referenceTarget)
    }

    var presetClima by remember { mutableStateOf(PresetClima.REAL) }
    var climaInfo by remember { mutableStateOf(PresetClima.REAL.clima) }
    var cargandoClima by remember { mutableStateOf(false) }

    var ultimaUbicacionClima by remember { mutableStateOf<GeoPoint?>(null) }
    var ultimoTimestampClimaMs by remember { mutableStateOf(0L) }

    suspend fun refrescarClima(lat: Double, lon: Double, forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()
        val minutosTranscurridos = (ahora - ultimoTimestampClimaMs) / (1000 * 60)
        val distanciaKm = if (ultimaUbicacionClima != null) {
            calculateDistanceMeters(ultimaUbicacionClima!!, GeoPoint(lat, lon)).toFloat() / 1000f
        } else {
            Float.MAX_VALUE
        }

        if (forzar || minutosTranscurridos >= 15L || distanciaKm >= 5f) {
            cargandoClima = true
            LinceLogger.i(
                tag = "CLIMA",
                message = "Consultando Open-Meteo",
                details = "forzar=$forzar, minutos=$minutosTranscurridos, distanciaKm=${distanciaKm.roundToInt()}"
            )
            val res = ClimaService.consultarOpenMeteo(lat, lon)
            res.onSuccess {
                climaInfo = it
                ultimaUbicacionClima = GeoPoint(lat, lon)
                ultimoTimestampClimaMs = ahora
            }
            cargandoClima = false
        }
    }

    val zonaActual = remember(userGeoPoint) { zonaClima(userGeoPoint.latitude, userGeoPoint.longitude) }

    // La tarjeta de clima se muestra a transportistas (conducción) y guías (vestimenta de grupo).
    val mostrarClima = activeRole == RolMapa.TRANSPORTISTA || activeRole == RolMapa.GUIA

    LaunchedEffect(zonaActual, presetClima, activeRole) {
        if (!mostrarClima) return@LaunchedEffect
        if (presetClima == PresetClima.REAL) {
            refrescarClima(userGeoPoint.latitude, userGeoPoint.longitude, forzar = true)
        } else {
            climaInfo = presetClima.clima
        }
    }

    LaunchedEffect(userGeoPoint) {
        if (mostrarClima && presetClima == PresetClima.REAL) {
            refrescarClima(userGeoPoint.latitude, userGeoPoint.longitude, forzar = false)
        }
    }

    val recomendacionConduccion = remember(climaInfo, servicio) {
        ClimaService.generarRecomendacion(
            clima = climaInfo,
            nombreConductor = if (servicio.esGuiado) "" else servicio.conductor,
            destino = servicio.destino
        )
    }

    val recomendacionVestimenta = remember(climaInfo, servicio) {
        ClimaService.generarRecomendacionVestimenta(
            clima = climaInfo,
            nombreGuia = if (servicio.esGuiado) servicio.guia else "",
            destino = servicio.destino
        )
    }

    // En la versión final no se registra sin GPS real; en desarrollo se usa la posición simulada.
    val hayUbicacion = isUsingRealGps || BuildConfig.DEBUG

    fun registrar(mensajeExito: String, operacion: suspend () -> Result<String?>) {
        if (procesando) return
        scope.launch {
            if (!hayUbicacion) {
                snackbarHostState.showSnackbar(context.getString(R.string.ruta_sin_gps_senal))
                return@launch
            }
            procesando = true
            val resultado = operacion()
            procesando = false
            resultado.onSuccess { aviso ->
                snackbarHostState.showSnackbar(aviso ?: mensajeExito)
            }.onFailure { e ->
                snackbarHostState.showSnackbar(e.message ?: context.getString(R.string.error_operacion))
            }
        }
    }

    fun avisarBloqueo(rechazo: EvaluacionCheckin) {
        enviarIncidencia(
            ReglasGeocerca.tipoIncidencia(rechazo.resultado),
            ReglasGeocerca.detalleIncidencia(rechazo, servicio.origen)
        )
    }

    // Check-in: la app ya lo validó (el botón queda bloqueado si no cumple); el backend lo valida otra vez.
    fun registrarCheckInValidado(mensajeExito: String, guardar: suspend () -> Result<String?>) {
        val evaluacionActual = evaluacionVigente ?: return
        if (procesando || !evaluacionActual.resultado.permitido) return
        scope.launch {
            procesando = true
            val respuesta = hitos.registrar(
                servicioId = servicio.id,
                checkIn = true,
                lat = userGeoPoint.latitude,
                lon = userGeoPoint.longitude,
                precisionMetros = precisionGps ?: 0f,
                claveIdempotencia = claveCheckin
            )
            procesando = false
            respuesta.onSuccess { hito ->
                claveCheckin = UUID.randomUUID().toString()
                if (hito.resultado == ResultadoCheckin.VERIFICADO) {
                    registrar(mensajeExito, guardar)
                } else {
                    val rechazo = evaluacionActual.copy(
                        resultado = hito.resultado ?: ResultadoCheckin.SIN_GEOCERCA,
                        distanciaMetros = hito.distanciaMetros ?: evaluacionActual.distanciaMetros
                    )
                    LinceLogger.w("CHECKIN", "El backend rechazó el check-in de ${servicio.id}: ${rechazo.resultado}")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.ruta_checkin_rechazado,
                                motivoBloqueo(context, rechazo, servicio.origen)
                            )
                        )
                    }
                    avisarBloqueo(rechazo)
                }
            }.onFailure { e ->
                snackbarHostState.showSnackbar(e.mensajeBloqueo(context.getString(R.string.error_operacion)))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            RutaTopBar(
                activeRole = activeRole,
                hasPermission = hasLocationPermission,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp)
            ) {
                ServiceRouteHeaderCard(
                    servicio = servicio,
                    activeRole = activeRole,
                    isCheckInDone = isCheckInDone,
                    isServiceFinished = isServiceFinished
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    OsmMapContainer(
                        context = context,
                        userPoint = userGeoPoint,
                        destinationPoint = destinationPoint,
                        pickupPoint = pickupPoint,
                        radioGeocerca = geocercaPickup?.radioMetros,
                        originTitle = servicio.origen,
                        destinationTitle = servicio.destino,
                        activeRole = activeRole,
                        recentrar = recentrarMapa,
                        routeColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
                    )

                    if (!esCoordinador) {
                        GpsSpeedometerBadge(
                            speedProvider = { currentSpeedKmh },
                            tipoClima = climaInfo.tipo,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        )
                    }

                    ExtendedFloatingActionButton(
                        onClick = { showExternalNavDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = stringResource(R.string.cd_abrir_mapas),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                when {
                                    esCoordinador -> R.string.ruta_ver_maps_waze
                                    !isCheckInDone -> R.string.ruta_ir_pickup
                                    else -> R.string.ruta_ir_destino
                                }
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!esCoordinador) {
                        FloatingActionButton(
                            onClick = {
                                if (hasLocationPermission) {
                                    fetchCurrentGps(
                                        context = context,
                                        fusedClient = fusedLocationClient,
                                        onLocation = { point, real, precision ->
                                            userGeoPoint = point
                                            isUsingRealGps = real
                                            precisionGps = precision
                                            recentrarMapa++
                                            scope.launch {
                                                val msg = if (real) {
                                                    context.getString(R.string.ruta_gps_actualizado, textoPrecision(precision))
                                                } else {
                                                    context.getString(R.string.ruta_gps_simulado_aviso)
                                                }
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    )
                                } else {
                                    pedirUbicacion()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = stringResource(R.string.cd_centrar_ubicacion)
                            )
                        }
                    }
                }

                if (mostrarClima) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ClimaConduccionCard(
                            clima = climaInfo,
                            recomendacion = if (activeRole == RolMapa.TRANSPORTISTA) recomendacionConduccion else null,
                            recomendacionVestimenta = if (activeRole == RolMapa.GUIA) recomendacionVestimenta else null,
                            presetActual = presetClima,
                            cargando = cargandoClima,
                            onPresetSelected = { presetClima = it },
                            onRefrescarGps = {
                                scope.launch {
                                    refrescarClima(userGeoPoint.latitude, userGeoPoint.longitude, forzar = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            RoleBottomActionPanel(
                servicio = servicio,
                activeRole = activeRole,
                checkin = checkin,
                distanceMeters = distanceMeters,
                userPoint = userGeoPoint,
                isUsingRealGps = isUsingRealGps,
                precisionGps = precisionGps,
                procesando = procesando,
                evaluacion = evaluacion,
                errorBackend = errorBackend,
                onReintentar = { cargarBackend++ },
                onAvisarBloqueo = { evaluacion?.let { avisarBloqueo(it) } },
                onReportarIncidencia = { showIncidenciaDialog = true },
                onPrimaryAction = {
                    val esGuia = activeRole == RolMapa.GUIA
                    if (!isCheckInDone) {
                        val exito = if (esGuia) {
                            context.getString(R.string.ruta_guia_checkin_ok, servicio.pasajeros)
                        } else {
                            context.getString(R.string.ruta_conductor_checkin_ok, textoPrecision(precisionGps))
                        }
                        registrarCheckInValidado(exito) {
                            ServicioRepository.registrarCheckIn(
                                id = servicio.id,
                                lat = userGeoPoint.latitude,
                                lon = userGeoPoint.longitude,
                                distanciaM = evaluacionVigente?.distanciaMetros ?: distanceMeters,
                                gpsReal = isUsingRealGps
                            )
                        }
                    } else {
                        val exito = if (esGuia) {
                            context.getString(R.string.ruta_guia_checkout_ok)
                        } else {
                            context.getString(R.string.ruta_conductor_checkout_ok, servicio.destino)
                        }
                        registrar(exito) {
                            hitos.registrar(
                                servicioId = servicio.id,
                                checkIn = false,
                                lat = userGeoPoint.latitude,
                                lon = userGeoPoint.longitude,
                                precisionMetros = precisionGps ?: 0f,
                                claveIdempotencia = UUID.randomUUID().toString()
                            ).onFailure { e ->
                                LinceLogger.w("CHECKIN", "Check-out sin registro en el backend: ${e.message}")
                            }
                            ServicioRepository.registrarCheckOut(
                                id = servicio.id,
                                lat = userGeoPoint.latitude,
                                lon = userGeoPoint.longitude
                            )
                        }
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showIncidenciaDialog) {
        ReportarIncidenciaDialog(
            onDismiss = { showIncidenciaDialog = false },
            onConfirm = { tipo, detalle ->
                showIncidenciaDialog = false
                enviarIncidencia(tipo, detalle)
            }
        )
    }

    if (showExternalNavDialog) {
        ExternalNavigationDialog(
            targetName = targetLabel,
            targetPoint = referenceTarget,
            activeRole = activeRole,
            onDismiss = { showExternalNavDialog = false },
            onSelectApp = { appOption ->
                showExternalNavDialog = false
                launchExternalMapApp(
                    context = context,
                    option = appOption,
                    targetPoint = referenceTarget,
                    targetLabel = targetLabel,
                    onFallbackMessage = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
        )
    }
}

@Composable
private fun RutaTopBar(
    activeRole: RolMapa,
    hasPermission: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(BarraSuperior.fondo)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_volver),
                    tint = BarraSuperior.contenido
                )
            }
            Text(
                text = stringResource(
                    if (activeRole == RolMapa.COORDINADOR) R.string.ruta_titulo_coordinador
                    else R.string.ruta_titulo_operativo
                ),
                color = BarraSuperior.contenido,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val badgeText = stringResource(
            when {
                activeRole == RolMapa.COORDINADOR -> R.string.ruta_badge_supervision
                hasPermission -> R.string.ruta_badge_gps_activo
                else -> R.string.ruta_badge_sin_gps
            }
        )
        val badgeColor = when {
            activeRole == RolMapa.COORDINADOR -> MaterialTheme.colorScheme.primaryContainer
            hasPermission -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        }
        val badgeTextColor = if (activeRole == RolMapa.COORDINADOR) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            Color.White
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(end = 6.dp)
                .background(badgeColor, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (activeRole == RolMapa.COORDINADOR) Icons.Outlined.Person else Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = badgeTextColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                color = badgeTextColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ServiceRouteHeaderCard(
    servicio: Servicio,
    activeRole: RolMapa,
    isCheckInDone: Boolean,
    isServiceFinished: Boolean
) {
    val horaInicioTexto = formatHora(servicio.horaInicio)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = servicio.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = horaInicioTexto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val pasajerosTexto = stringResource(R.string.chip_pasajeros, servicio.pasajeros)
            val subtitle = when (activeRole) {
                RolMapa.TRANSPORTISTA ->
                    if (servicio.guia == ServicioMapper.SIN_GUIA) pasajerosTexto
                    else "${stringResource(R.string.chip_guia, servicio.guia)} • $pasajerosTexto"
                RolMapa.GUIA -> stringResource(R.string.ruta_reserva_pasajeros, servicio.reserva, pasajerosTexto)
                RolMapa.COORDINADOR -> stringResource(R.string.ruta_responsable, servicio.responsable, pasajerosTexto)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val statusText = when {
                isServiceFinished -> stringResource(R.string.ruta_estado_finalizado)
                isCheckInDone -> stringResource(R.string.ruta_estado_en_ruta)
                activeRole == RolMapa.COORDINADOR -> stringResource(R.string.ruta_estado_esperando)
                else -> stringResource(R.string.ruta_estado_programado, horaInicioTexto)
            }
            val statusColor = when {
                isServiceFinished -> MaterialTheme.colorScheme.primary
                isCheckInDone -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            }

            StatusBadge(text = statusText, color = statusColor)
        }
    }
}

@Composable
private fun GpsSpeedometerBadge(
    speedProvider: () -> Float,
    tipoClima: TipoClima,
    modifier: Modifier = Modifier
) {
    val speed = speedProvider()
    val limiteVelocidad = remember(tipoClima) {
        when (tipoClima) {
            TipoClima.LLUVIA_FUERTE, TipoClima.TORMENTA, TipoClima.NIEVE -> 50
            TipoClima.NIEBLA -> 45
            TipoClima.LLUVIA_MODERADA, TipoClima.VIENTO_FUERTE -> 70
            else -> 100
        }
    }
    val velocidadExcedida = speed.roundToInt() > limiteVelocidad

    Surface(
        shape = RoundedCornerShape(50),
        color = if (velocidadExcedida) Color(0xFFDC2626) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = stringResource(R.string.cd_velocidad_gps),
                tint = if (velocidadExcedida) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "${speed.roundToInt()} km/h",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (velocidadExcedida) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OsmMapContainer(
    context: Context,
    userPoint: GeoPoint,
    destinationPoint: GeoPoint,
    pickupPoint: GeoPoint,
    radioGeocerca: Int?,
    originTitle: String,
    destinationTitle: String,
    activeRole: RolMapa,
    recentrar: Int,
    routeColorArgb: Int
) {
    val esCoordinador = activeRole == RolMapa.COORDINADOR

    // osmdroid ya queda configurado en MainActivity: volver a cargarlo aquí traba la apertura.
    val mapView = remember {
        val linceTileSource = org.osmdroid.tileprovider.tilesource.XYTileSource(
            "LinceOsmDe",
            0,
            19,
            256,
            ".png",
            arrayOf("https://tile.openstreetmap.de/"),
            "© OpenStreetMap contributors"
        )
        LinceLogger.i(
            tag = "MAPA",
            message = "MapView inicializado con fuente ${linceTileSource.name()}",
            details = "url=https://tile.openstreetmap.de/ | UA=${Configuration.getInstance().userAgentValue}"
        )
        MapView(context).apply {
            setTileSource(linceTileSource)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(PickupHotelCumbres)
            // La pantalla tiene desplazamiento vertical: sin esto, se queda con los gestos del mapa.
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN -> vista.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        vista.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    // El mapa sigue las pausas de la app y se libera al salir de la pantalla.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val userMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(
                context,
                if (activeRole == RolMapa.GUIA) R.drawable.ic_mapa_persona else R.drawable.ic_mapa_vehiculo
            )
        }
    }

    // Ruta y marcadores fijos: solo se rehacen si cambia el servicio o el rol.
    LaunchedEffect(mapView, destinationPoint, pickupPoint, radioGeocerca, originTitle, destinationTitle, activeRole, routeColorArgb) {
        mapView.overlays.clear()

        val routeLine = Polyline().apply {
            setPoints(listOf(pickupPoint, destinationPoint))
            outlinePaint.color = routeColorArgb
            outlinePaint.strokeWidth = 10f
        }
        mapView.overlays.add(routeLine)

        // Zona donde se permite el check-in.
        if (radioGeocerca != null) {
            val zona = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(pickupPoint, radioGeocerca.toDouble())
                fillPaint.color = (routeColorArgb and 0x00FFFFFF) or 0x33000000
                outlinePaint.color = routeColorArgb
                outlinePaint.strokeWidth = 4f
                title = "Geocerca: $originTitle ($radioGeocerca m)"
            }
            mapView.overlays.add(zona)
        }

        val originMarker = Marker(mapView).apply {
            position = pickupPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_mapa_pickup)
            title = "Pickup: $originTitle"
        }
        mapView.overlays.add(originMarker)

        val destinationMarker = Marker(mapView).apply {
            position = destinationPoint
            // La bandera se apoya en la base del asta, no en el centro.
            setAnchor(0.25f, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_mapa_destino)
            title = "Destino: $destinationTitle"
        }
        mapView.overlays.add(destinationMarker)

        if (activeRole == RolMapa.GUIA && radioGeocerca == null) {
            val guideMeetingMarker = Marker(mapView).apply {
                position = PuntoEncuentroGuias
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_mapa_encuentro)
                title = "Punto de Encuentro Guía / Recepción Grupo"
            }
            mapView.overlays.add(guideMeetingMarker)
        }

        if (!esCoordinador) {
            userMarker.title = if (activeRole == RolMapa.GUIA) "Tu Ubicación" else "Tu Vehículo"
            mapView.overlays.add(userMarker)
        }
        mapView.invalidate()
    }

    // Solo se mueve el marcador del usuario, sin tocar lo que está explorando en el mapa.
    LaunchedEffect(userMarker, userPoint) {
        userMarker.position = userPoint
        mapView.invalidate()
    }

    // Solo se centra al llegar la primera ubicación y al pulsar el botón de ubicación.
    LaunchedEffect(recentrar) {
        if (recentrar > 0 && !esCoordinador) mapView.controller.animateTo(userPoint)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView }
    )
}

@Composable
private fun RoleBottomActionPanel(
    servicio: Servicio,
    activeRole: RolMapa,
    checkin: CheckinServicio?,
    distanceMeters: Int,
    userPoint: GeoPoint,
    isUsingRealGps: Boolean,
    precisionGps: Float?,
    procesando: Boolean,
    evaluacion: EvaluacionCheckin?,
    errorBackend: String?,
    onReintentar: () -> Unit,
    onAvisarBloqueo: () -> Unit,
    onReportarIncidencia: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val isCheckInDone = checkin != null
    val isServiceFinished = checkin?.finalizado == true
    val context = LocalContext.current
    // El check-in se valida con la geocerca; el check-out solo queda registrado.
    val validarCheckin = activeRole != RolMapa.COORDINADOR && !isCheckInDone
    val bloqueo = evaluacion?.takeIf { validarCheckin && !it.resultado.permitido }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            val locationTitle = when (activeRole) {
                RolMapa.TRANSPORTISTA -> stringResource(
                    if (isCheckInDone) R.string.ruta_destino else R.string.ruta_punto_recogida,
                    if (isCheckInDone) servicio.destino else servicio.origen
                )
                RolMapa.GUIA -> stringResource(
                    if (isCheckInDone) R.string.ruta_destino else R.string.ruta_recepcion_pasajeros,
                    if (isCheckInDone) servicio.destino else servicio.origen
                )
                RolMapa.COORDINADOR -> servicio.responsable
            }
            Text(
                text = locationTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Con la geocerca cargada, la distancia ya va en la línea de validación (sin mostrar coordenadas).
            val conGeocerca = validarCheckin && evaluacion?.geocerca != null
            val detalle = when {
                conGeocerca -> null
                activeRole == RolMapa.COORDINADOR ->
                    checkin?.let { ReglasCheckin.resumen(it) } ?: stringResource(R.string.ruta_sin_checkin)
                isUsingRealGps ->
                    stringResource(
                        R.string.ruta_distancia_gps_real,
                        ReglasCheckin.formatoDistancia(distanceMeters),
                        textoPrecision(precisionGps)
                    )
                BuildConfig.DEBUG ->
                    stringResource(
                        R.string.ruta_distancia_gps_simulado,
                        ReglasCheckin.formatoDistancia(distanceMeters)
                    )
                else -> stringResource(R.string.ruta_esperando_gps)
            }
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (validarCheckin) {
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    evaluacion != null -> {
                        val geocerca = evaluacion.geocerca
                        if (geocerca != null) {
                            val distancia = ReglasCheckin.formatoDistancia(evaluacion.distanciaMetros ?: 0)
                            val radio = ReglasCheckin.formatoDistancia(geocerca.radioMetros)
                            val precision = evaluacion.precisionMetros?.takeIf { isUsingRealGps }
                            Text(
                                text = if (precision != null) {
                                    stringResource(R.string.ruta_geocerca_datos, distancia, radio, "$precision m")
                                } else {
                                    stringResource(R.string.ruta_geocerca_datos_sin_gps, distancia, radio)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(
                                    R.string.ruta_geocerca_limite,
                                    horaCorta(evaluacion.limite),
                                    evaluacion.parametros.anticipacionMinutos
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (bloqueo != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = motivoBloqueo(context, bloqueo, servicio.origen),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    errorBackend != null -> {
                        Text(
                            text = stringResource(R.string.ruta_geocerca_error, errorBackend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onReintentar) {
                            Text(stringResource(R.string.ruta_reintentar), fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> Text(
                        text = stringResource(R.string.ruta_geocerca_cargando),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (activeRole != RolMapa.COORDINADOR) {
                Spacer(modifier = Modifier.height(10.dp))

                val primaryButtonText = stringResource(
                    if (bloqueo != null) R.string.ruta_btn_checkin_bloqueado else when (activeRole) {
                        RolMapa.GUIA -> when {
                            isServiceFinished -> R.string.ruta_btn_excursion_finalizada
                            !isCheckInDone -> R.string.ruta_btn_confirmar_presentacion
                            else -> R.string.ruta_btn_finalizar_guia
                        }
                        else -> when {
                            isServiceFinished -> R.string.ruta_btn_completado
                            !isCheckInDone -> R.string.ruta_btn_checkin
                            else -> R.string.ruta_btn_checkout
                        }
                    }
                )

                Button(
                    onClick = onPrimaryAction,
                    enabled = !isServiceFinished && !procesando &&
                        (!validarCheckin || evaluacion?.resultado?.permitido == true),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isCheckInDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isServiceFinished) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = primaryButtonText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (bloqueo != null && ReglasIncidencia.puedeReportar(servicio, checkin)) {
                    TextButton(onClick = onAvisarBloqueo) {
                        Text(stringResource(R.string.ruta_avisar_bloqueo), fontWeight = FontWeight.Bold)
                    }
                }

                if (ReglasIncidencia.puedeReportar(servicio, checkin)) {
                    TextButton(onClick = onReportarIncidencia) {
                        Text(stringResource(R.string.detalle_reportar_incidencia), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalNavigationDialog(
    targetName: String,
    targetPoint: GeoPoint,
    activeRole: RolMapa,
    onDismiss: () -> Unit,
    onSelectApp: (AppNavegacionExterna) -> Unit
) {
    var selectedApp by remember { mutableIntStateOf(0) }
    val options = AppNavegacionExterna.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                text = stringResource(
                    if (activeRole == RolMapa.COORDINADOR) R.string.ruta_nav_inspeccionar
                    else R.string.ruta_nav_iniciar
                ),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.ruta_nav_destino_coords,
                        targetName,
                        formatCoord(targetPoint)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedApp = index }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedApp == index,
                            onClick = { selectedApp = index }
                        )
                        Column {
                            Text(
                                text = option.labelLocalizado(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.subtituloLocalizado(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectApp(options[selectedApp]) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    text = stringResource(R.string.ruta_nav_abrir),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.accion_cancelar),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun AppNavegacionExterna.labelLocalizado(): String = when (this) {
    AppNavegacionExterna.SELECTOR_SISTEMA -> stringResource(R.string.ruta_nav_otro)
    else -> label
}

@Composable
private fun AppNavegacionExterna.subtituloLocalizado(): String = when (this) {
    AppNavegacionExterna.GOOGLE_MAPS -> stringResource(R.string.ruta_nav_gmaps_desc)
    AppNavegacionExterna.WAZE -> stringResource(R.string.ruta_nav_waze_desc)
    AppNavegacionExterna.SELECTOR_SISTEMA -> stringResource(R.string.ruta_nav_otro_desc)
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color.White, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun launchExternalMapApp(
    context: Context,
    option: AppNavegacionExterna,
    targetPoint: GeoPoint,
    targetLabel: String,
    onFallbackMessage: (String) -> Unit
) {
    val lat = targetPoint.latitude
    val lon = targetPoint.longitude
    val encodedLabel = Uri.encode(targetLabel)

    try {
        when (option) {
            AppNavegacionExterna.GOOGLE_MAPS -> {
                val gmmUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    LinceLogger.i("NAV", "Abriendo Google Maps nativo hacia $targetLabel", gmmUri.toString())
                    context.startActivity(mapIntent)
                } else {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon&travelmode=driving")
                    LinceLogger.w("NAV", "Google Maps no instalado; usando fallback web", webUri.toString())
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }
            AppNavegacionExterna.WAZE -> {
                val wazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes")
                val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                    setPackage("com.waze")
                }
                if (wazeIntent.resolveActivity(context.packageManager) != null) {
                    LinceLogger.i("NAV", "Abriendo Waze nativo hacia $targetLabel", wazeUri.toString())
                    context.startActivity(wazeIntent)
                } else {
                    LinceLogger.w("NAV", "Waze no instalado; abriendo ruta web", wazeUri.toString())
                    onFallbackMessage(context.getString(R.string.ruta_nav_waze_fallback))
                    context.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
                }
            }
            AppNavegacionExterna.SELECTOR_SISTEMA -> {
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
                LinceLogger.i("NAV", "Abriendo selector nativo geo: hacia $targetLabel", geoUri.toString())
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, geoUri),
                    context.getString(R.string.ruta_nav_con)
                )
                context.startActivity(chooser)
            }
        }
    } catch (ex: Exception) {
        LinceLogger.e("NAV", "Error al lanzar aplicación de mapas externa (${option.label})", ex)
        onFallbackMessage(context.getString(R.string.ruta_nav_sin_app))
    }
}

private fun hasPermissions(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun fetchCurrentGps(
    context: Context,
    fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocation: (GeoPoint, Boolean, Float?) -> Unit
) {
    if (!hasPermissions(context)) return
    // La posición simulada solo existe en compilaciones de desarrollo.
    val usarRespaldo = { if (BuildConfig.DEBUG) onLocation(SimuladorCercaPickup, false, null) }
    fusedClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            val isInsideChile = location.latitude in -56.0..-17.0 && location.longitude in -76.0..-66.0
            if (isInsideChile) {
                LinceLogger.i(
                    tag = "GPS",
                    message = "Ubicación GPS real obtenida",
                    details = "precisión=${location.accuracy.roundToInt()} m"
                )
                onLocation(
                    GeoPoint(location.latitude, location.longitude),
                    true,
                    if (location.hasAccuracy()) location.accuracy else null
                )
            } else {
                LinceLogger.w(
                    tag = "GPS",
                    message = "Coordenada fuera de Chile (emulador); usando punto simulado en Puerto Varas"
                )
                usarRespaldo()
            }
        } else {
            LinceLogger.w("GPS", "FusedLocation devolvió null; sin ubicación disponible")
            usarRespaldo()
        }
    }.addOnFailureListener { ex ->
        LinceLogger.e("GPS", "Error al consultar FusedLocationProviderClient", ex)
        usarRespaldo()
    }
}

private fun calculateDistanceMeters(from: GeoPoint, to: GeoPoint): Int {
    val results = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        results
    )
    return results[0].roundToInt()
}

/** Por qué no se permite el check-in, para la persona en terreno. */
private fun motivoBloqueo(context: Context, evaluacion: EvaluacionCheckin, lugar: String): String {
    val geocerca = evaluacion.geocerca
    return when (evaluacion.resultado) {
        ResultadoCheckin.VERIFICADO -> ""
        ResultadoCheckin.SIN_GEOCERCA -> context.getString(R.string.ruta_bloqueo_sin_geocerca, lugar)
        ResultadoCheckin.SIN_GPS_REAL -> context.getString(R.string.ruta_bloqueo_sin_gps)
        ResultadoCheckin.GPS_IMPRECISO -> context.getString(
            R.string.ruta_bloqueo_impreciso,
            "${evaluacion.precisionMetros ?: 0} m",
            "${evaluacion.parametros.precisionMaximaMetros} m"
        )
        ResultadoCheckin.FUERA_DE_ZONA -> context.getString(
            R.string.ruta_bloqueo_fuera,
            ReglasCheckin.formatoDistancia(evaluacion.distanciaMetros ?: 0),
            geocerca?.nombre ?: lugar,
            ReglasCheckin.formatoDistancia(geocerca?.radioMetros ?: 0)
        )
        ResultadoCheckin.TARDE -> context.getString(R.string.ruta_bloqueo_tarde, horaCorta(evaluacion.limite))
    }
}

private fun horaCorta(hora: LocalDateTime): String = "%02d:%02d".format(Locale.US, hora.hour, hora.minute)

/** Precisión del GPS para mostrar; en pantalla no van coordenadas (quedan en capturas y videos). */
private fun textoPrecision(precision: Float?): String = precision?.let { "± ${it.roundToInt()} m" } ?: "—"

private fun formatCoord(point: GeoPoint): String {
    val lat = "%.4f".format(Locale.US, point.latitude)
    val lon = "%.4f".format(Locale.US, point.longitude)
    return "$lat, $lon"
}
