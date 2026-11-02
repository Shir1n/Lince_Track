package com.example.lince_track.ui.ruta

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
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.CheckinServicio
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
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
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
                text = "Servicio no encontrado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Volver")
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
                if (speed > 5f) {
                    LinceLogger.i(
                        "GPS",
                        "Tracking continuo: ${speed.roundToInt()} km/h",
                        "lat=${loc.latitude}, lon=${loc.longitude}"
                    )
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        LinceLogger.i("GPS", "Respuesta de permisos de ubicación: granted=$granted")
        if (granted) {
            fetchCurrentGps(
                context = context,
                fusedClient = fusedLocationClient,
                onLocation = { point, real ->
                    userGeoPoint = point
                    isUsingRealGps = real
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
                onLocation = { point, real ->
                    userGeoPoint = point
                    isUsingRealGps = real
                    recentrarMapa++
                }
            )
        } else {
            LinceLogger.w("GPS", "Sin permiso de ubicación activo; solicitando al usuario")
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val destinationPoint = remember(servicio) {
        when {
            servicio.destino.contains("Petrohué", ignoreCase = true) ||
                servicio.destino.contains("Parque", ignoreCase = true) -> DestinoPetrohue
            servicio.destino.contains("Centro", ignoreCase = true) -> DestinoCentroPuertoVaras
            else -> DestinoAeropuertoTepual
        }
    }

    val referenceTarget = when {
        isCheckInDone -> destinationPoint
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
                snackbarHostState.showSnackbar("Aún no hay señal GPS. Espera unos segundos e intenta nuevamente.")
                return@launch
            }
            procesando = true
            val resultado = operacion()
            procesando = false
            resultado.onSuccess { aviso ->
                snackbarHostState.showSnackbar(aviso ?: mensajeExito)
            }.onFailure { e ->
                snackbarHostState.showSnackbar(e.message ?: "No se pudo completar la operación")
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
                            contentDescription = "Abrir en app de mapas",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (esCoordinador) {
                                "Ver en Maps / Waze"
                            } else if (!isCheckInDone) {
                                "Ir al Pickup"
                            } else {
                                "Ir al Destino"
                            },
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
                                        onLocation = { point, real ->
                                            userGeoPoint = point
                                            isUsingRealGps = real
                                            recentrarMapa++
                                            scope.launch {
                                                val msg = if (real) {
                                                    "GPS actualizado (${formatCoord(point)})"
                                                } else {
                                                    "Sin señal GPS: usando posición simulada en Puerto Varas"
                                                }
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
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
                                contentDescription = "Centrar en mi ubicación"
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
                procesando = procesando,
                onReportarIncidencia = { showIncidenciaDialog = true },
                onPrimaryAction = {
                    val esGuia = activeRole == RolMapa.GUIA
                    if (!isCheckInDone) {
                        val exito = if (esGuia) {
                            "Presentación y grupo de ${servicio.pasajeros} pasajeros confirmados"
                        } else {
                            "Check-in registrado (${formatCoord(userGeoPoint)})"
                        }
                        registrar(exito) {
                            ServicioRepository.registrarCheckIn(
                                id = servicio.id,
                                lat = userGeoPoint.latitude,
                                lon = userGeoPoint.longitude,
                                distanciaM = distanceMeters,
                                gpsReal = isUsingRealGps
                            )
                        }
                    } else {
                        val exito = if (esGuia) {
                            "Servicio de guía finalizado"
                        } else {
                            "Check-out registrado en ${servicio.destino}"
                        }
                        registrar(exito) {
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
                    contentDescription = "Volver",
                    tint = BarraSuperior.contenido
                )
            }
            Text(
                text = if (activeRole == RolMapa.COORDINADOR) "Ruta del Servicio" else "Ruta y Check-in",
                color = BarraSuperior.contenido,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val badgeText = when {
            activeRole == RolMapa.COORDINADOR -> "Supervisión"
            hasPermission -> "GPS Activo"
            else -> "Sin GPS"
        }
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

            val pasajerosTexto = "${servicio.pasajeros} Pasajeros"
            val subtitle = when (activeRole) {
                RolMapa.TRANSPORTISTA ->
                    if (servicio.guia == ServicioMapper.SIN_GUIA) pasajerosTexto else "Guía: ${servicio.guia} • $pasajerosTexto"
                RolMapa.GUIA -> "Reserva ${servicio.reserva} • $pasajerosTexto"
                RolMapa.COORDINADOR -> "Responsable: ${servicio.responsable} • $pasajerosTexto"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val statusText = when {
                isServiceFinished -> "Servicio Finalizado"
                isCheckInDone -> "En Ruta — Check-in registrado"
                activeRole == RolMapa.COORDINADOR -> "Esperando check-in"
                else -> "Inicio programado: $horaInicioTexto"
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
                contentDescription = "Velocidad GPS",
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
        }
    }

    // Ruta y marcadores fijos: solo se rehacen si cambia el servicio o el rol.
    LaunchedEffect(mapView, destinationPoint, originTitle, destinationTitle, activeRole, routeColorArgb) {
        mapView.overlays.clear()

        val routeLine = Polyline().apply {
            setPoints(listOf(PickupHotelCumbres, destinationPoint))
            outlinePaint.color = routeColorArgb
            outlinePaint.strokeWidth = 10f
        }
        mapView.overlays.add(routeLine)

        val originMarker = Marker(mapView).apply {
            position = PickupHotelCumbres
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Pickup: $originTitle"
        }
        mapView.overlays.add(originMarker)

        val destinationMarker = Marker(mapView).apply {
            position = destinationPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destino: $destinationTitle"
        }
        mapView.overlays.add(destinationMarker)

        if (activeRole == RolMapa.GUIA) {
            val guideMeetingMarker = Marker(mapView).apply {
                position = PuntoEncuentroGuias
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
    procesando: Boolean,
    onReportarIncidencia: () -> Unit,
    onPrimaryAction: () -> Unit
) {
    val isCheckInDone = checkin != null
    val isServiceFinished = checkin?.finalizado == true

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
                RolMapa.TRANSPORTISTA ->
                    if (isCheckInDone) "Destino: ${servicio.destino}" else "Punto de recogida: ${servicio.origen}"
                RolMapa.GUIA ->
                    if (isCheckInDone) "Destino: ${servicio.destino}" else "Recepción de pasajeros: Lobby ${servicio.origen}"
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

            val detalle = when {
                activeRole == RolMapa.COORDINADOR ->
                    checkin?.let { ReglasCheckin.resumen(it) } ?: "Sin check-in registrado"
                isUsingRealGps ->
                    "A ${ReglasCheckin.formatoDistancia(distanceMeters)} del punto • GPS: ${formatCoord(userPoint)}"
                BuildConfig.DEBUG ->
                    "A ${ReglasCheckin.formatoDistancia(distanceMeters)} del punto • GPS simulado"
                else -> "Esperando señal GPS..."
            }
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (activeRole != RolMapa.COORDINADOR) {
                Spacer(modifier = Modifier.height(10.dp))

                val primaryButtonText = when (activeRole) {
                    RolMapa.GUIA -> when {
                        isServiceFinished -> "EXCURSIÓN FINALIZADA"
                        !isCheckInDone -> "CONFIRMAR PRESENTACIÓN Y GRUPO"
                        else -> "FINALIZAR SERVICIO DE GUÍA"
                    }
                    else -> when {
                        isServiceFinished -> "SERVICIO COMPLETADO"
                        !isCheckInDone -> "REGISTRAR CHECK-IN (INICIO)"
                        else -> "REGISTRAR CHECK-OUT (FIN)"
                    }
                }

                Button(
                    onClick = onPrimaryAction,
                    enabled = !isServiceFinished && !procesando,
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

                if (ReglasIncidencia.puedeReportar(servicio, checkin)) {
                    TextButton(onClick = onReportarIncidencia) {
                        Text("Reportar incidencia", fontWeight = FontWeight.Bold)
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
                text = if (activeRole == RolMapa.COORDINADOR) {
                    "Inspeccionar en Mapa Externo"
                } else {
                    "Iniciar Navegación Externa"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Destino: $targetName\nCoordenadas: ${formatCoord(targetPoint)}",
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
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.subtitle,
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
                    text = "ABRIR NAVEGACIÓN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCELAR",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
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
                    onFallbackMessage("Waze no instalado en este dispositivo; abriendo ruta web")
                    context.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
                }
            }
            AppNavegacionExterna.SELECTOR_SISTEMA -> {
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
                LinceLogger.i("NAV", "Abriendo selector nativo geo: hacia $targetLabel", geoUri.toString())
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, geoUri),
                    "Navegar con:"
                )
                context.startActivity(chooser)
            }
        }
    } catch (ex: Exception) {
        LinceLogger.e("NAV", "Error al lanzar aplicación de mapas externa (${option.label})", ex)
        onFallbackMessage("No se encontró una aplicación de mapas compatible")
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
    onLocation: (GeoPoint, Boolean) -> Unit
) {
    if (!hasPermissions(context)) return
    // La posición simulada solo existe en compilaciones de desarrollo.
    val usarRespaldo = { if (BuildConfig.DEBUG) onLocation(SimuladorCercaPickup, false) }
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
                    details = "lat=${location.latitude}, lon=${location.longitude}, acc=${location.accuracy}m"
                )
                onLocation(GeoPoint(location.latitude, location.longitude), true)
            } else {
                LinceLogger.w(
                    tag = "GPS",
                    message = "Coordenada fuera de Chile (emulador); usando punto simulado en Puerto Varas",
                    details = "raw=${location.latitude},${location.longitude} -> sim=${SimuladorCercaPickup.latitude},${SimuladorCercaPickup.longitude}"
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

private fun formatCoord(point: GeoPoint): String {
    val lat = "%.4f".format(Locale.US, point.latitude)
    val lon = "%.4f".format(Locale.US, point.longitude)
    return "$lat, $lon"
}
