package com.example.lince_track.ui.detalle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.CheckinServicio
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.IncidenciaServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.ReglasIncidencia
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.formatPrecioCLP
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.IncidenciaCard
import com.example.lince_track.ui.components.RejectServicioDialog
import com.example.lince_track.ui.components.ReportarIncidenciaDialog
import com.example.lince_track.util.abrirWhatsApp
import kotlinx.coroutines.launch
import com.example.lince_track.ui.theme.BarraSuperior

@Composable
fun DetalleServicioScreen(
    servicioId: String,
    onBack: () -> Unit = {},
    onOpenRoute: (String) -> Unit = {}
) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val servicio = remember(servicios, servicioId) { servicios.find { it.id == servicioId } }
    val checkins by ServicioRepository.checkins.collectAsState()
    val incidencias by ServicioRepository.incidencias.collectAsState()
    val rolActual by AuthRepository.rolActual.collectAsState()
    var showIncidenciaDialog by remember { mutableStateOf(false) }
 
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Al aceptar se queda aquí, con "Ver Ruta" a mano; al rechazar vuelve de inmediato,
    // salvo que haya que avisar algo primero (por ejemplo, que no hay conexión).
    fun operar(volverAlTerminar: Boolean, op: suspend (String) -> Result<String?>) {
        if (servicio == null || procesando) return
        scope.launch {
            procesando = true
            val resultado = op(servicio.id)
            procesando = false
            resultado.onSuccess { aviso ->
                if (volverAlTerminar) {
                    if (aviso != null) snackbarHostState.showSnackbar(aviso)
                    onBack()
                } else {
                    snackbarHostState.showSnackbar(aviso ?: "Asignación confirmada")
                }
            }.onFailure { e ->
                snackbarHostState.showSnackbar(e.message ?: "No se pudo completar la operación")
            }
        }
    }

    if (servicio == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Asignación no encontrada",
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
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            DetalleTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceHeaderCard(servicio)
                RouteScheduleCard(servicio)
                PassengersCard(servicio)
                GuideCard(servicio)
                IncidenciasSeccion(
                    servicio = servicio,
                    incidencias = ReglasIncidencia.deServicio(incidencias, servicio.id),
                    puedeReportar = rolActual != Rol.ADMINISTRADOR &&
                        ReglasIncidencia.puedeReportar(servicio, checkins[servicio.id]),
                    onReportar = { showIncidenciaDialog = true },
                    onResolver = { incidencia: IncidenciaServicio ->
                        scope.launch {
                            ServicioRepository.resolverIncidencia(incidencia)
                                .onSuccess { aviso -> if (aviso != null) snackbarHostState.showSnackbar(aviso) }
                                .onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "No se pudo marcar como resuelta") }
                        }
                        Unit
                    }.takeIf { rolActual == Rol.ADMINISTRADOR }
                )
            }

            BottomActions(
                servicio = servicio,
                checkin = checkins[servicio.id],
                procesando = procesando,
                onConfirm = { operar(volverAlTerminar = false, ServicioRepository::aceptar) },
                onReject = { showRejectDialog = true },
                onOpenRoute = { onOpenRoute(servicio.id) }
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
                scope.launch {
                    ServicioRepository.reportarIncidencia(servicio.id, tipo, detalle)
                        .onSuccess { aviso -> snackbarHostState.showSnackbar(aviso ?: "Incidencia enviada a Coordinación") }
                        .onFailure { e -> snackbarHostState.showSnackbar(e.message ?: "No se pudo enviar la incidencia") }
                }
            }
        )
    }

    if (showRejectDialog) {
        RejectServicioDialog(
            servicio = servicio,
            onDismiss = { showRejectDialog = false },
            onConfirm = { motivo, observaciones ->
                showRejectDialog = false
                operar(volverAlTerminar = true) { ServicioRepository.rechazar(it, motivo, observaciones) }
            }
        )
    }
}

@Composable
private fun DetalleTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .background(BarraSuperior.fondo)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = BarraSuperior.contenido
            )
        }
        Text(
            text = "Detalle de Servicio",
            color = BarraSuperior.contenido,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ServiceHeaderCard(servicio: Servicio) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = servicio.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reserva: ${servicio.reserva}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            EstadoServicioBadge(servicio.estado)
            // La API aún no trae tarifa: sin ella no se muestra.
            if (servicio.tarifa > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Tarifa: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatPrecioCLP(servicio.tarifa)} CLP",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteScheduleCard(servicio: Servicio) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Ruta y Horario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "${servicio.origen}  •  ${formatHora(servicio.horaInicio)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "${servicio.destino}  •  ${formatHora(servicio.horaFin)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PassengersCard(servicio: Servicio) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pasajeros (${servicio.pasajeros})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Idioma del tour: ${servicio.idiomaTour}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Lista del grupo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            servicio.nombresPasajeros.forEachIndexed { index, nombre ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${index + 1}. $nombre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Líder del grupo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = servicio.liderGrupo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = servicio.fonoLider,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${servicio.fonoLider}"))
                    context.startActivity(intent)
                }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = "Llamar al líder del grupo",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = {
                    abrirWhatsApp(context, servicio.fonoLider)
                }) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = "Abrir WhatsApp del líder del grupo",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (servicio.notasGrupo.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = servicio.notasGrupo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun IncidenciasSeccion(
    servicio: Servicio,
    incidencias: List<IncidenciaServicio>,
    puedeReportar: Boolean,
    onReportar: () -> Unit,
    onResolver: ((IncidenciaServicio) -> Unit)?
) {
    if (incidencias.isEmpty() && !puedeReportar) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (incidencias.isNotEmpty()) {
            Text(
                text = "Incidencias de ${servicio.reserva}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        incidencias.forEach { incidencia ->
            IncidenciaCard(
                incidencia = incidencia,
                onResolver = onResolver?.let { resolver -> { resolver(incidencia) } }
            )
        }
        if (puedeReportar) {
            OutlinedButton(
                onClick = onReportar,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reportar incidencia", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GuideCard(servicio: Servicio) {
    val rolActual by AuthRepository.rolActual.collectAsState()
    val esGuia = rolActual == Rol.GUIA
    val etiqueta = if (esGuia) "Conductor Asignado" else "Guía Asignado"
    val nombre = if (esGuia) servicio.conductor else servicio.guia

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$etiqueta: $nombre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {}) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = if (esGuia) "Llamar al conductor" else "Llamar al guía",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActions(
    servicio: Servicio,
    checkin: CheckinServicio?,
    procesando: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onOpenRoute: () -> Unit
) {
    val rol by AuthRepository.rolActual.collectAsState()
    val puedeResponder = rol != Rol.ADMINISTRADOR

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Un servicio terminado ya no se responde ni se vuelve a registrar.
        if (checkin != null && ReglasCheckin.estaTerminado(servicio, checkin)) {
            Text(
                text = "Servicio terminado: ${ReglasCheckin.horario(checkin)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            return@Row
        }
        when (servicio.estado) {
            EstadoServicio.PENDIENTE -> {
                if (procesando) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                } else if (puedeResponder) {
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("CONFIRMAR ASIGNACIÓN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            EstadoServicio.ACEPTADO -> {
                Button(
                    onClick = onOpenRoute,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (puedeResponder) "VER RUTA Y CHECK-IN" else "VER RUTA Y CHECK-IN EN MAPA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (puedeResponder) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !procesando,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            EstadoServicio.RECHAZADO -> {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Asignación rechazada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
