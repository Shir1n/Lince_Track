package com.example.lince_track.ui.servicios

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lince_track.data.AuthRepository
import com.example.lince_track.data.EstadoServicio
import com.example.lince_track.data.ReglasCheckin
import com.example.lince_track.data.Rol
import com.example.lince_track.data.Servicio
import com.example.lince_track.data.ServicioRepository
import com.example.lince_track.data.formatFechaCorta
import com.example.lince_track.data.formatHora
import com.example.lince_track.data.formatPrecioCLP
import com.example.lince_track.ui.components.AvisoServiciosCard
import com.example.lince_track.ui.components.EstadoServicioBadge
import com.example.lince_track.ui.components.RejectServicioDialog
import com.example.lince_track.ui.historial.VerTerminadosBoton
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.lince_track.ui.theme.BarraSuperior

@Composable
fun ServiciosScreen(
    onVerTerminados: () -> Unit = {},
    onServiceClick: (String) -> Unit = {},
    onRouteClick: (String) -> Unit = {}
) {
    val servicios by ServicioRepository.servicios.collectAsState()
    val error by ServicioRepository.error.collectAsState()
    val hoy = LocalDate.now()
    val checkins by ServicioRepository.checkins.collectAsState()
    val serviciosHoy = remember(servicios, checkins, hoy) {
        ReglasCheckin.porRealizar(servicios, checkins).filter { it.fecha == hoy }.sortedBy { it.horaInicio }
    }

    val scope = rememberCoroutineScope()
    var procesandoIds by remember { mutableStateOf(setOf<String>()) }
    var rechazoPendiente by remember { mutableStateOf<Servicio?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun operar(servicio: Servicio, op: suspend (String) -> Result<String?>) {
        if (servicio.id in procesandoIds) return
        scope.launch {
            procesandoIds = procesandoIds + servicio.id
            val resultado = op(servicio.id)
            procesandoIds = procesandoIds - servicio.id
            resultado.onSuccess { aviso ->
                if (aviso != null) snackbarHostState.showSnackbar(aviso)
            }.onFailure { e ->
                snackbarHostState.showSnackbar(e.message ?: "No se pudo completar la operación")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryKpiCard(serviciosHoy, ReglasCheckin.terminados(servicios, checkins).size, onVerTerminados)

                Text(
                    text = "Servicios de Hoy - ${formatFechaCorta(hoy)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                error?.let { AvisoServiciosCard(it) }

                if (serviciosHoy.isEmpty() && error == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tienes servicios para hoy",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    serviciosHoy.forEach { servicio ->
                        ServicioCard(
                            servicio = servicio,
                            procesando = servicio.id in procesandoIds,
                            onAceptar = { operar(servicio, ServicioRepository::aceptar) },
                            onRechazar = { rechazoPendiente = servicio },
                            onClick = { onServiceClick(servicio.id) },
                            onRouteClick = { onRouteClick(servicio.id) }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    rechazoPendiente?.let { servicio ->
        RejectServicioDialog(
            servicio = servicio,
            onDismiss = { rechazoPendiente = null },
            onConfirm = { motivo, observaciones ->
                operar(servicio) { ServicioRepository.rechazar(it, motivo, observaciones) }
                rechazoPendiente = null
            }
        )
    }
}

@Composable
private fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(BarraSuperior.fondo)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lince Track",
                color = BarraSuperior.contenido,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Transportista",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryKpiCard(servicios: List<Servicio>, terminados: Int, onVerTerminados: () -> Unit) {
    val aceptados = servicios.filter { it.estado == EstadoServicio.ACEPTADO }
    val pendientes = servicios.filter { it.estado == EstadoServicio.PENDIENTE }
    val saldo = aceptados.sumOf { it.tarifa }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                if (saldo > 0) {
                    Text(
                        text = "Saldo acumulado hoy:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatPrecioCLP(saldo)} CLP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Resumen de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (aceptados.size + pendientes.size).toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (pendientes.size == 1) "1 pendiente" else "${pendientes.size} pendientes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        VerTerminadosBoton(terminados, onVerTerminados)
    }
}

@Composable
private fun ServicioCard(
    servicio: Servicio,
    procesando: Boolean,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit,
    onClick: () -> Unit,
    onRouteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                EstadoServicioBadge(servicio.estado)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatHora(servicio.horaInicio)} – ${formatHora(servicio.horaFin)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("${servicio.pasajeros} Pasajeros")
                MetaChip("Guía: ${servicio.guia}")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // La API aún no trae tarifa: sin ella no se muestra. El espacio mantiene los botones a la derecha.
                if (servicio.tarifa > 0) {
                    Text(
                        text = formatPrecioCLP(servicio.tarifa),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                val rol by AuthRepository.rolActual.collectAsState()
                when (servicio.estado) {
                    EstadoServicio.PENDIENTE -> {
                        if (procesando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (rol != Rol.ADMINISTRADOR) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = onAceptar,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Confirmar", fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = onRechazar) {
                                    Text(
                                        text = "Rechazar",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    EstadoServicio.ACEPTADO -> {
                        OutlinedButton(
                            onClick = onRouteClick,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Ver Ruta", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    EstadoServicio.RECHAZADO -> {
                        OutlinedButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Ver Detalle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
