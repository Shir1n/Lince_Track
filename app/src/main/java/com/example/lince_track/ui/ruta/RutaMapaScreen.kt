package com.example.lince_track.ui.ruta

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatHora
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

enum class RolMapa(val label: String) {
    TRANSPORTISTA("Transportista"),
    GUIA("Guía SERNATUR"),
    COORDINADOR("Coordinador")
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
    servicioId: String = "SB-2026-8890",
    onBack: () -> Unit = {},
    initialRole: RolMapa = RolMapa.TRANSPORTISTA
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val servicios by ServicioRepository.servicios.collectAsState()
    val servicio = remember(servicios, servicioId) {
        servicios.find { it.id == servicioId } ?: servicios.firstOrNull()
    }

    var activeRole by remember { mutableStateOf(initialRole) }
    var isCheckInDone by remember { mutableStateOf(false) }
    var isServiceFinished by remember { mutableStateOf(false) }
    var assignedDriver by remember { mutableStateOf("Carlos Morales (Sprinter LK-VF-84)") }
    var showReportDialog by remember { mutableStateOf(false) }
    var showReassignDialog by remember { mutableStateOf(false) }
    var showExternalNavDialog by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(hasPermissions(context))
    }
    var userGeoPoint by remember { mutableStateOf(SimuladorCercaPickup) }
    var isUsingRealGps by remember { mutableStateOf(false) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            fetchCurrentGps(
                context = context,
                fusedClient = fusedLocationClient,
                onLocation = { point, real ->
                    userGeoPoint = point
                    isUsingRealGps = real
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        if (hasLocationPermission) {
            fetchCurrentGps(
                context = context,
                fusedClient = fusedLocationClient,
                onLocation = { point, real ->
                    userGeoPoint = point
                    isUsingRealGps = real
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
    }

    val destinationPoint = remember(servicio) {
        when {
            servicio?.destino?.contains("Petrohué", ignoreCase = true) == true ||
                servicio?.destino?.contains("Parque", ignoreCase = true) == true -> DestinoPetrohue
            servicio?.destino?.contains("Centro", ignoreCase = true) == true -> DestinoCentroPuertoVaras
            else -> DestinoAeropuertoTepual
        }
    }

    val referenceTarget = when {
        activeRole == RolMapa.COORDINADOR -> PickupHotelCumbres
        isCheckInDone -> destinationPoint
        activeRole == RolMapa.GUIA -> PuntoEncuentroGuias
        else -> PickupHotelCumbres
    }

    val targetLabel = when {
        activeRole == RolMapa.COORDINADOR -> "Posición Unidad en Ruta"
        isCheckInDone -> servicio?.destino ?: "Aeropuerto El Tepual"
        activeRole == RolMapa.GUIA -> "Lobby ${servicio?.origen ?: "Hotel Cumbres"} (Punto de Encuentro)"
        else -> "${servicio?.origen ?: "Hotel Cumbres Puerto Varas"} (Pickup)"
    }

    val distanceMeters = remember(userGeoPoint, referenceTarget) {
        calculateDistanceMeters(userGeoPoint, referenceTarget)
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

            RoleSelectorRow(
                activeRole = activeRole,
                onRoleSelected = { activeRole = it }
            )

            ServiceRouteHeaderCard(
                servicio = servicio,
                activeRole = activeRole,
                isCheckInDone = isCheckInDone,
                isServiceFinished = isServiceFinished,
                assignedDriver = assignedDriver
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                OsmMapContainer(
                    context = context,
                    userPoint = userGeoPoint,
                    destinationPoint = destinationPoint,
                    originTitle = servicio?.origen ?: "Hotel Cumbres Puerto Varas",
                    destinationTitle = servicio?.destino ?: "Aeropuerto El Tepual",
                    activeRole = activeRole,
                    routeColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
                )

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
                        text = if (activeRole == RolMapa.COORDINADOR) {
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

                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            fetchCurrentGps(
                                context = context,
                                fusedClient = fusedLocationClient,
                                onLocation = { point, real ->
                                    userGeoPoint = point
                                    isUsingRealGps = real
                                    scope.launch {
                                        val msg = if (real) {
                                            "GPS actualizado (${formatCoord(point)})"
                                        } else {
                                            "GPS de emulador fuera de Chile: centrando en Puerto Varas"
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
                        contentDescription = "Actualizar ubicación GPS"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RoleBottomActionPanel(
                servicio = servicio,
                activeRole = activeRole,
                distanceMeters = distanceMeters,
                userPoint = userGeoPoint,
                isCheckInDone = isCheckInDone,
                isServiceFinished = isServiceFinished,
                assignedDriver = assignedDriver,
                onPrimaryAction = {
                    when (activeRole) {
                        RolMapa.TRANSPORTISTA -> {
                            if (!isCheckInDone) {
                                isCheckInDone = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Check-in registrado en GPS (${formatCoord(userGeoPoint)})"
                                    )
                                }
                            } else {
                                isServiceFinished = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Check-out finalizado en ${servicio?.destino ?: "destino"}"
                                    )
                                }
                            }
                        }
                        RolMapa.GUIA -> {
                            if (!isCheckInDone) {
                                isCheckInDone = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Presentación SERNATUR y grupo de ${servicio?.pasajeros ?: 4} pasajeros confirmados"
                                    )
                                }
                            } else {
                                isServiceFinished = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Servicio de guía finalizado sin incidencias"
                                    )
                                }
                            }
                        }
                        RolMapa.COORDINADOR -> {
                            showReassignDialog = true
                        }
                    }
                },
                onSecondaryAction = {
                    when (activeRole) {
                        RolMapa.TRANSPORTISTA, RolMapa.GUIA -> showReportDialog = true
                        RolMapa.COORDINADOR -> {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+56987654321"))
                            context.startActivity(dialIntent)
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

    if (showReportDialog) {
        ReportIncidentDialog(
            activeRole = activeRole,
            onDismiss = { showReportDialog = false },
            onConfirm = { motivo ->
                showReportDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Alerta enviada a Coordinación: $motivo")
                }
            }
        )
    }

    if (showReassignDialog) {
        ReassignDriverDialog(
            currentDriver = assignedDriver,
            onDismiss = { showReassignDialog = false },
            onConfirm = { newDriver ->
                assignedDriver = newDriver
                showReassignDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Servicio reasignado a $newDriver")
                }
            }
        )
    }

    if (showExternalNavDialog) {
        val navTargetPoint = if (activeRole == RolMapa.COORDINADOR) userGeoPoint else referenceTarget
        ExternalNavigationDialog(
            targetName = targetLabel,
            targetPoint = navTargetPoint,
            activeRole = activeRole,
            onDismiss = { showExternalNavDialog = false },
            onSelectApp = { appOption ->
                showExternalNavDialog = false
                launchExternalMapApp(
                    context = context,
                    option = appOption,
                    targetPoint = navTargetPoint,
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
            .height(64.dp)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = if (activeRole == RolMapa.COORDINADOR) "Monitoreo GPS" else "Ruta y Check-in",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        val badgeText = when {
            activeRole == RolMapa.COORDINADOR -> "Modo Supervisión"
            hasPermission -> "GPS Activo"
            else -> "GPS Sin Permiso"
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
                .padding(end = 8.dp)
                .background(badgeColor, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (activeRole == RolMapa.COORDINADOR) Icons.Outlined.Person else Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = badgeTextColor,
                modifier = Modifier.size(16.dp)
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
private fun RoleSelectorRow(
    activeRole: RolMapa,
    onRoleSelected: (RolMapa) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RolMapa.entries.forEach { role ->
            val selected = role == activeRole
            FilterChip(
                selected = selected,
                onClick = { onRoleSelected(role) },
                label = {
                    Text(
                        text = role.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun ServiceRouteHeaderCard(
    servicio: Servicio?,
    activeRole: RolMapa,
    isCheckInDone: Boolean,
    isServiceFinished: Boolean,
    assignedDriver: String
) {
    val horaInicioTexto = servicio?.let { formatHora(it.horaInicio) } ?: "08:30 AM"
    val margenInicioTexto = servicio?.let { formatHora(it.horaInicio.minusMinutes(15)) } ?: "08:15 AM"
    val margenFinTexto = servicio?.let { formatHora(it.horaInicio.plusMinutes(15)) } ?: "08:45 AM"

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
                    text = servicio?.titulo ?: "Transfer Hotel Cumbres → Aeropuerto Tepual",
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

            val guiaNombre = servicio?.guia ?: "Andrea Soto"
            val pasajeros = servicio?.pasajeros ?: 4
            val reserva = servicio?.reserva ?: "#SB-2026-8890"

            val subtitle = when (activeRole) {
                RolMapa.TRANSPORTISTA -> "Guía: $guiaNombre (SERNATUR) • $pasajeros Pasajeros"
                RolMapa.GUIA -> "Chofer: $assignedDriver • Reserva $reserva"
                RolMapa.COORDINADOR -> "Unidad: $assignedDriver • Guía: $guiaNombre"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val statusText = when {
                isServiceFinished -> "Servicio Finalizado (Check-out OK)"
                isCheckInDone -> "En Ruta — Check-in validado por GPS"
                activeRole == RolMapa.COORDINADOR -> "Margen 15 min: $margenInicioTexto - $margenFinTexto (Esperando Check-in)"
                else -> "Margen 15 min activo: $margenInicioTexto - $margenFinTexto (En horario)"
            }
            val statusColor = when {
                isServiceFinished -> MaterialTheme.colorScheme.primary
                isCheckInDone -> MaterialTheme.colorScheme.secondary
                activeRole == RolMapa.COORDINADOR -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }

            StatusBadge(text = statusText, color = statusColor)
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
    routeColorArgb: Int
) {
    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "${context.packageName}/1.0 (LinceTrackApp)"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(PickupHotelCumbres)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            val routeLine = Polyline().apply {
                setPoints(
                    listOf(
                        PickupHotelCumbres,
                        destinationPoint
                    )
                )
                outlinePaint.color = routeColorArgb
                outlinePaint.strokeWidth = 10f
            }
            map.overlays.add(routeLine)

            val originMarker = Marker(map).apply {
                position = PickupHotelCumbres
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Pickup: $originTitle"
            }
            map.overlays.add(originMarker)

            val destinationMarker = Marker(map).apply {
                position = destinationPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Destino: $destinationTitle"
            }
            map.overlays.add(destinationMarker)

            if (activeRole == RolMapa.GUIA) {
                val guideMeetingMarker = Marker(map).apply {
                    position = PuntoEncuentroGuias
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Punto de Encuentro Guía / Recepción Grupo"
                }
                map.overlays.add(guideMeetingMarker)
            }

            val currentMarker = Marker(map).apply {
                position = userPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = when (activeRole) {
                    RolMapa.TRANSPORTISTA -> "Tu Vehículo (GPS en vivo)"
                    RolMapa.GUIA -> "Tu Ubicación Guía SERNATUR"
                    RolMapa.COORDINADOR -> "Telemetría Vehículo en Ruta"
                }
            }
            map.overlays.add(currentMarker)

            map.controller.animateTo(userPoint)
            map.invalidate()
        }
    )
}

@Composable
private fun RoleBottomActionPanel(
    servicio: Servicio?,
    activeRole: RolMapa,
    distanceMeters: Int,
    userPoint: GeoPoint,
    isCheckInDone: Boolean,
    isServiceFinished: Boolean,
    assignedDriver: String,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit
) {
    val origenNombre = servicio?.origen ?: "Hotel Cumbres, Puerto Varas"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val locationTitle = when (activeRole) {
                RolMapa.TRANSPORTISTA -> "Punto de recogida: $origenNombre"
                RolMapa.GUIA -> "Recepción de pasajeros: Lobby $origenNombre"
                RolMapa.COORDINADOR -> "Monitoreando: $assignedDriver"
            }
            Text(
                text = locationTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "A $distanceMeters m del punto • GPS: ${formatCoord(userPoint)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            val primaryButtonText = when (activeRole) {
                RolMapa.TRANSPORTISTA -> when {
                    isServiceFinished -> "SERVICIO COMPLETADO"
                    !isCheckInDone -> "REGISTRAR CHECK-IN (INICIO)"
                    else -> "REGISTRAR CHECK-OUT (FIN)"
                }
                RolMapa.GUIA -> when {
                    isServiceFinished -> "EXCURSIÓN FINALIZADA"
                    !isCheckInDone -> "CONFIRMAR PRESENTACIÓN Y GRUPO"
                    else -> "FINALIZAR SERVICIO DE GUÍA"
                }
                RolMapa.COORDINADOR -> "REASIGNAR SERVICIO"
            }

            val primaryButtonColor = when (activeRole) {
                RolMapa.COORDINADOR -> MaterialTheme.colorScheme.primary
                else -> if (!isCheckInDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            }

            Button(
                onClick = onPrimaryAction,
                enabled = !isServiceFinished || activeRole == RolMapa.COORDINADOR,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isServiceFinished && activeRole != RolMapa.COORDINADOR) {
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

            Spacer(modifier = Modifier.height(6.dp))

            if (activeRole == RolMapa.COORDINADOR) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contactar Equipo en Ruta",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                TextButton(onClick = onSecondaryAction) {
                    Text(
                        text = if (activeRole == RolMapa.TRANSPORTISTA) {
                            "Reportar retraso a Coordinación"
                        } else {
                            "Reportar incidencia de grupo a Coordinación"
                        },
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportIncidentDialog(
    activeRole: RolMapa,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableIntStateOf(0) }
    var observations by remember { mutableStateOf("") }

    val reasons = if (activeRole == RolMapa.TRANSPORTISTA) {
        listOf(
            "Tráfico denso / Corte en ruta",
            "Falla mecánica o neumático",
            "Espera de pasajeros en lobby",
            "Condición climática adversa"
        )
    } else {
        listOf(
            "Pasajeros atrasados en recepción",
            "Documentación / Voucher incompleto",
            "Fila o demora en acceso CONAF",
            "Cambio de itinerario solicitado"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = if (activeRole == RolMapa.TRANSPORTISTA) {
                    "Reportar Retraso en Ruta"
                } else {
                    "Reportar Incidencia de Grupo"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Se enviará tu ubicación GPS actual y el motivo a Coordinación Logística.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                reasons.forEachIndexed { index, reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == index,
                            onClick = { selectedReason = index }
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    placeholder = {
                        Text(
                            "Detalle adicional...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reasons[selectedReason]) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = "ENVIAR ALERTA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
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
private fun ReassignDriverDialog(
    currentDriver: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReplacement by remember { mutableIntStateOf(0) }
    val replacements = listOf(
        "Miguel Soto (Mercedes Sprinter • A 5 min)",
        "Jorge Valdivia (Hyundai H1 • Disponible)",
        "Patricio Lagos (Ford Transit • En base)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Reasignar Servicio en Ruta",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "Actual: $currentDriver\nSelecciona unidad disponible cercana en el mapa:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                replacements.forEachIndexed { index, candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReplacement = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReplacement == index,
                            onClick = { selectedReplacement = index }
                        )
                        Text(
                            text = candidate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replacements[selectedReplacement]) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    text = "CONFIRMAR REEMPLAZO",
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
                    context.startActivity(mapIntent)
                } else {
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon&travelmode=driving")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            }
            AppNavegacionExterna.WAZE -> {
                val wazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes")
                val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                    setPackage("com.waze")
                }
                if (wazeIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(wazeIntent)
                } else {
                    onFallbackMessage("Waze no instalado en este dispositivo; abriendo ruta web")
                    context.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
                }
            }
            AppNavegacionExterna.SELECTOR_SISTEMA -> {
                val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, geoUri),
                    "Navegar con:"
                )
                context.startActivity(chooser)
            }
        }
    } catch (_: Exception) {
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
    fusedClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            val isInsideChile = location.latitude in -56.0..-17.0 && location.longitude in -76.0..-66.0
            if (isInsideChile) {
                onLocation(GeoPoint(location.latitude, location.longitude), true)
            } else {
                onLocation(SimuladorCercaPickup, false)
            }
        } else {
            onLocation(SimuladorCercaPickup, false)
        }
    }.addOnFailureListener {
        onLocation(SimuladorCercaPickup, false)
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
    val lat = "%.4f".format(point.latitude)
    val lon = "%.4f".format(point.longitude)
    return "$lat, $lon"
}
